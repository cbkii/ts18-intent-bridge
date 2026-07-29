#!/usr/bin/env bash
# Capture static and dynamic evidence for the private DoFun/Topway panel contract.
set -euo pipefail

script_dir=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
# shellcheck source=scripts/lib/ts18-toolkit-common.sh
source "$script_dir/lib/ts18-toolkit-common.sh"

action=observe
duration=20
repeats=2
output_root=/sdcard/TS18Toolkit
writer=

cleanup() {
  if [[ -n ${writer:-} ]]; then
    ts18_stop_writer "$writer" || ts18_warn "Log writer $writer did not stop cleanly"
    writer=
  fi
}
trap cleanup EXIT INT TERM

while (($#)); do
  case $1 in
    --action)
      (($# >= 2)) || ts18_die '--action needs a value'
      action=$2
      shift 2
      ;;
    --duration)
      (($# >= 2)) || ts18_die '--duration needs seconds'
      duration=$2
      shift 2
      ;;
    --repeats)
      (($# >= 2)) || ts18_die '--repeats needs a number'
      repeats=$2
      shift 2
      ;;
    --output)
      (($# >= 2)) || ts18_die '--output needs a directory'
      output_root=$2
      shift 2
      ;;
    --help|-h)
      printf '%s\n' \
        'Usage: probe-ts18-panel-contract.sh [--action observe|play|pause|play-pause|next|previous]' \
        '       [--duration SECONDS] [--repeats 2] [--output DIRECTORY]'
      exit 0
      ;;
    *) ts18_die "Unknown argument: $1" ;;
  esac
done
case $action in
  observe|play|pause|play-pause|next|previous) ;;
  *) ts18_die 'Unsupported action.' ;;
esac
[[ $duration =~ ^[0-9]+$ ]] && ((duration >= 5 && duration <= 300)) ||
  ts18_die 'Duration must be 5..300 seconds.'
[[ $repeats =~ ^[0-9]+$ ]] && ((repeats >= 1 && repeats <= 5)) ||
  ts18_die 'Repeats must be 1..5.'

keycode_for_action() {
  case $1 in
    play) printf '126\n' ;;
    pause) printf '127\n' ;;
    play-pause) printf '85\n' ;;
    next) printf '87\n' ;;
    previous) printf '88\n' ;;
    observe) printf '\n' ;;
  esac
}

ts18_require_android
stamp=$(date +%Y%m%d-%H%M%S)
stage="$output_root/panel-contract-$stamp-$action"
mkdir -p "$stage"/{apks,packages,runs,static}
results="$stage/command-results.tsv"
printf 'label\texit_status\tstarted_epoch\tfinished_epoch\n' >"$results"
packages=(
  com.tw.music com.dofun.variety com.tw.service com.tw.service.xt
  com.tw.carinfoservice com.navimods.radio com.android.systemui com.tw.media
)

for package_id in "${packages[@]}"; do
  safe=$(ts18_safe_name "$package_id")
  path_file="$stage/packages/$safe-path.txt"
  ts18_capture "$results" "$path_file" "pm-path:$package_id" pm path "$package_id" || true
  ts18_capture "$results" "$stage/packages/$safe-package.txt" \
    "package:$package_id" dumpsys package "$package_id" || true
  while IFS= read -r path; do
    path=${path#package:}
    [[ -n $path ]] || continue
    destination="$stage/apks/$safe-$(basename "$path")"
    if cp -- "$path" "$destination" 2>/dev/null ||
      { ts18_root_available && ts18_root cp "$path" "$destination"; }; then
      printf '%s  %s\n' "$(ts18_sha256 "$destination")" "$(basename "$destination")" \
        >>"$stage/apks/SHA256SUMS.txt"
    fi
  done < <(sed -n 's/^package://p' "$path_file")
done

ts18_capture "$results" "$stage/static/service-list.txt" service-list service list || true
ts18_capture "$results" "$stage/static/binder-calls.txt" binder-calls dumpsys binder_calls_stats || true
ts18_capture "$results" "$stage/static/appwidget.txt" appwidget dumpsys appwidget || true
ts18_capture "$results" "$stage/static/notification.txt" notification dumpsys notification || true

printf '%s\n' \
  'action,direction,sender,receiver,component,code_or_transaction,fields_and_types,null_default,frequency,evidence,capture' \
  >"$stage/contract-matrix.csv"

for repeat in $(seq 1 "$repeats"); do
  run="$stage/runs/$(printf '%02d' "$repeat")"
  mkdir -p "$run"
  ts18_capture "$results" "$run/media-before.txt" "media-before:$repeat" dumpsys media_session || true
  ts18_capture "$results" "$run/audio-before.txt" "audio-before:$repeat" dumpsys audio || true
  ts18_capture "$results" "$run/activity-before.txt" "activity-before:$repeat" dumpsys activity activities || true
  # Preserve global log history and follow only from this action boundary.
  logcat -v threadtime -b all -T 1 >"$run/logcat.txt" 2>&1 &
  writer=$!
  printf '%s\tbefore\t%s\t%s\n' "$(date +%s)" "$action" "$repeat" >"$run/timeline.tsv"
  keycode=$(keycode_for_action "$action")
  if [[ -n $keycode ]]; then
    input keyevent "$keycode"
    printf '%s\taction-keyevent-%s\t%s\t%s\n' "$(date +%s)" "$keycode" "$action" "$repeat" \
      >>"$run/timeline.tsv"
  else
    ts18_info "Repeat $repeat: operate the fixed DoFun panel now; capture remains active for ${duration}s."
  fi
  sleep "$duration"
  printf '%s\tafter\t%s\t%s\n' "$(date +%s)" "$action" "$repeat" >>"$run/timeline.tsv"
  ts18_stop_writer "$writer" || ts18_warn "Log writer $writer required forced termination"
  writer=
  sync
  ts18_capture "$results" "$run/media-after.txt" "media-after:$repeat" dumpsys media_session || true
  ts18_capture "$results" "$run/audio-after.txt" "audio-after:$repeat" dumpsys audio || true
  ts18_capture "$results" "$run/activity-after.txt" "activity-after:$repeat" dumpsys activity activities || true
  ts18_capture "$results" "$run/notification-after.txt" "notification-after:$repeat" dumpsys notification || true
  ts18_capture "$results" "$run/broadcast-history.txt" "broadcasts:$repeat" dumpsys activity broadcasts || true
  {
    printf '%s\n' "action=$action"
    printf '%s\n' "repeat=$repeat"
    if cmp -s "$run/media-before.txt" "$run/media-after.txt"; then
      printf '%s\n' 'media_session_changed=false'
    else
      printf '%s\n' 'media_session_changed=true'
    fi
    if cmp -s "$run/audio-before.txt" "$run/audio-after.txt"; then
      printf '%s\n' 'audio_changed=false'
    else
      printf '%s\n' 'audio_changed=true'
    fi
    printf '%s\n' 'contract_claim=Requires device validation; raw evidence only'
  } >"$run/result.txt"
done

cat >"$stage/README.txt" <<'EOF'
This bundle is an evidence campaign, not an Auxio private-contract implementation.

Evidence labels:
- Observed: raw package/APK/session/log state in this bundle.
- Inferred: a correlation that still needs repeat/reboot confirmation.
- Requires device validation: field names, types, defaults, Binder transactions, and senders that
  are not consistently observed in at least two runs.

Run once with stock com.tw.music stable, repeat after reboot, and use fixture tracks with normal,
blank, long, and Unicode metadata. Capture panel actions separately from Android media-key actions.
Do not promote a field into Auxio until two consistent physical runs prove sender, receiver, type,
null/default behaviour, and update frequency.

The modified developer reference com.tw.music_ac.apk is intentionally excluded. It is not an
installable authority and must not be repaired, re-signed, or installed.
EOF
(
  cd "$stage"
  find . -type f ! -name checksums.sha256 -print0 | LC_ALL=C sort -z |
    xargs -0 sha256sum >checksums.sha256
)
zip_path="$output_root/ts18-panel-contract-$stamp-$action.zip"
ts18_make_zip "$stage" "$zip_path"
printf 'output=%s\nsha256=%s\n' "$zip_path" "$(ts18_sha256 "$zip_path")"
