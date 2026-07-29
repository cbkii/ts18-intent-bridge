#!/usr/bin/env bash
# Version/variant-agnostic TS18 application and integration evidence collector.
set -euo pipefail

script_dir=$(CDPATH='' cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
# shellcheck source=scripts/lib/ts18-toolkit-common.sh
source "$script_dir/lib/ts18-toolkit-common.sh"

duration=120
scenario=general
output_root=/sdcard/TS18Toolkit
declare -a requested_packages=()
declare -a writers=()

usage() {
  printf '%s\n' \
    'Usage: collect-ts18-evidence.sh [--package ID]... [--duration SECONDS]' \
    '       [--scenario LABEL] [--output DIRECTORY]' \
    '' \
    'If --package is omitted, candidates are discovered from installed music-player components.' \
    'The collector never assumes an Auxio version, variant, package ID, or APK hash.'
}

while (($#)); do
  case $1 in
    --package)
      (($# >= 2)) || ts18_die '--package needs a value'
      requested_packages+=("$2")
      shift 2
      ;;
    --duration)
      (($# >= 2)) || ts18_die '--duration needs a value'
      duration=$2
      shift 2
      ;;
    --scenario)
      (($# >= 2)) || ts18_die '--scenario needs a value'
      scenario=$2
      shift 2
      ;;
    --output)
      (($# >= 2)) || ts18_die '--output needs a value'
      output_root=$2
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      ts18_die "Unknown argument: $1"
      ;;
  esac
done
if [[ ! $duration =~ ^[0-9]+$ ]] || ((duration < 5 || duration > 7200)); then
  ts18_die 'Duration must be 5..7200 seconds.'
fi

ts18_require_android
stamp=$(date +%Y%m%d-%H%M%S)
scenario_safe=$(ts18_safe_name "$scenario")
work_dir="$output_root/capture-$stamp-$scenario_safe"
stage_dir="$work_dir/stage"
results_file="$stage_dir/command-results.tsv"
mkdir -p "$stage_dir"/{logs,packages,system,apks,markers}
printf 'label\texit_status\tstarted_epoch\tfinished_epoch\n' >"$results_file"

cleanup() {
  local pid
  for pid in "${writers[@]:-}"; do
    ts18_stop_writer "$pid" || ts18_warn "Writer $pid did not stop cleanly"
  done
}
trap cleanup EXIT INT TERM

discover_packages() {
  local discovery="$stage_dir/system/music-player-discovery.txt"
  {
    cmd package query-activities -a android.intent.action.MUSIC_PLAYER 2>&1 || true
    cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.APP_MUSIC 2>&1 || true
    pm list packages -f 2>&1
  } >"$discovery"

  if ((${#requested_packages[@]})); then
    printf '%s\n' "${requested_packages[@]}"
    return
  fi
  {
    printf '%s\n' com.tw.music com.tw.media org.oxycblt.auxio
    sed -nE 's/.*package:([^[:space:]/]+).*/\1/p' "$discovery"
    sed -nE 's/.*([A-Za-z0-9_.]+)\\/[A-Za-z0-9_.$]+.*/\1/p' "$discovery"
  } | awk 'NF && !seen[$0]++'
}

mapfile -t packages < <(discover_packages)
critical_packages=(
  com.dofun.variety com.tw.service com.tw.service.xt com.tw.carinfoservice
  com.android.documentsui com.android.externalstorage
  com.cbkii.tsdocsui.rootprovider com.navimods.radio
)
packages+=("${critical_packages[@]}")
mapfile -t packages < <(printf '%s\n' "${packages[@]}" | awk 'NF && !seen[$0]++')

{
  printf 'schema=1\n'
  printf 'scenario=%s\n' "$scenario"
  printf 'capture_started=%s\n' "$(date -u +%FT%TZ)"
  printf 'duration_seconds=%s\n' "$duration"
  printf 'root_available=%s\n' "$(ts18_root_available && echo true || echo false)"
  printf 'packages=%s\n' "${packages[*]}"
} >"$stage_dir/manifest.txt"

ts18_capture "$results_file" "$stage_dir/system/id.txt" id id || true
ts18_capture "$results_file" "$stage_dir/system/getprop.txt" getprop getprop || true
ts18_capture "$results_file" "$stage_dir/system/df.txt" df df -k || true
ts18_capture "$results_file" "$stage_dir/system/mount.txt" mount mount || true
ts18_capture "$results_file" "$stage_dir/system/packages.txt" packages pm list packages -f -U -u || true
ts18_capture "$results_file" "$stage_dir/system/processes.txt" processes ps -A -o USER,PID,PPID,NAME,ARGS || true
if ts18_root_available; then
  # The quoted program is intentionally evaluated by the root-side shell.
  # shellcheck disable=SC2016
  ts18_capture_root "$results_file" "$stage_dir/system/magisk-modules.txt" magisk-modules \
    sh -c 'for d in /data/adb/modules/*; do [ -d "$d" ] || continue; printf "%s disabled=%s remove=%s\n" "$d" "$([ -e "$d/disable" ] && echo true || echo false)" "$([ -e "$d/remove" ] && echo true || echo false)"; done' || true
else
  printf '%s\n' 'root unavailable' >"$stage_dir/system/magisk-modules.txt"
fi

for package_id in "${packages[@]}"; do
  safe=$(ts18_safe_name "$package_id")
  path_file="$stage_dir/packages/$safe-path.txt"
  ts18_capture "$results_file" "$path_file" "pm-path:$package_id" pm path "$package_id" || true
  if ! grep -q '^package:' "$path_file"; then
    continue
  fi
  ts18_capture "$results_file" "$stage_dir/packages/$safe-package.txt" \
    "dumpsys-package:$package_id" dumpsys package "$package_id" || true
  uid=$(sed -nE 's/.*userId=([0-9]+).*/\1/p' "$stage_dir/packages/$safe-package.txt" | head -n 1)
  printf '%s\n' "${uid:-unknown}" >"$stage_dir/packages/$safe-uid.txt"
  while IFS= read -r apk_path; do
    apk_path=${apk_path#package:}
    [[ -n $apk_path ]] || continue
    apk_name="$safe-$(basename "$apk_path")"
    if cp -- "$apk_path" "$stage_dir/apks/$apk_name" 2>/dev/null ||
      { ts18_root_available && ts18_root cp "$apk_path" "$stage_dir/apks/$apk_name"; }; then
      printf '%s  %s\n' "$(ts18_sha256 "$stage_dir/apks/$apk_name")" "$apk_name" \
        >>"$stage_dir/apks/SHA256SUMS.txt"
    else
      ts18_warn "Could not copy $apk_path"
    fi
  done < <(sed -n 's/^package://p' "$path_file")
done

for service in activity activities media_session audio notification appwidget package providers uri_grants; do
  safe=$(ts18_safe_name "$service")
  # Capture complete raw output. Filtering is deliberately deferred until writers have stopped.
  ts18_capture "$results_file" "$stage_dir/system/dumpsys-$safe.txt" "dumpsys:$service" \
    dumpsys "$service" || true
done
ts18_capture "$results_file" "$stage_dir/system/dumpsys-activity-intents.txt" \
  dumpsys:activity-intents dumpsys activity intents || true

printf '%s\tbefore\t%s\n' "$(date +%s)" "$scenario" >"$stage_dir/markers/timeline.tsv"
# `-T 1` begins at the collection boundary without erasing the device's global buffers.
logcat -v threadtime -b all -T 1 >"$stage_dir/logs/logcat-full.txt" 2>&1 &
writers+=("$!")
for package_id in "${packages[@]}"; do
  safe=$(ts18_safe_name "$package_id")
  uid_file="$stage_dir/packages/$safe-uid.txt"
  [[ -f $uid_file ]] || continue
  uid=$(tr -d '\r\n' <"$uid_file")
  [[ $uid =~ ^[0-9]+$ ]] || continue
  if logcat --help 2>&1 | grep -q -- '--uid'; then
    logcat -v threadtime -b all -T 1 --uid="$uid" >"$stage_dir/logs/logcat-$safe.txt" 2>&1 &
    writers+=("$!")
  fi
done

ts18_info "Capture is active for ${duration}s. Perform scenario: $scenario"
sleep "$duration"
printf '%s\tafter\t%s\n' "$(date +%s)" "$scenario" >>"$stage_dir/markers/timeline.tsv"
cleanup
writers=()
sync

if command -v grep >/dev/null 2>&1; then
  grep -Ei 'Auxio|com\.tw\.music|com\.tw\.media|MusicRepository|AuxioPerf|AUXIO_TS_CAPTURE_CANARY|MediaSession|DoFun|Topway' \
    "$stage_dir/logs/logcat-full.txt" >"$stage_dir/logs/logcat-target-derived.txt" || true
fi
auxio_detected=false
if grep -RqsE 'org\.oxycblt\.auxio|AUXIO_TS_CAPTURE_CANARY' "$stage_dir/packages"; then
  auxio_detected=true
fi
if $auxio_detected &&
  ! grep -Rq 'AUXIO_TS_CAPTURE_CANARY' "$stage_dir/logs"; then
  printf '%s\n' 'WARNING: no Auxio capture canary was observed; app-side logging could not be validated.' \
    >>"$stage_dir/manifest.txt"
fi

ts18_capture "$results_file" "$stage_dir/system/media-session-after.txt" \
  media-session-after dumpsys media_session || true
ts18_capture "$results_file" "$stage_dir/system/audio-after.txt" audio-after dumpsys audio || true
printf 'capture_finished=%s\n' "$(date -u +%FT%TZ)" >>"$stage_dir/manifest.txt"

ts18_write_checksums "$stage_dir" "$stage_dir/checksums.sha256"
immutable="$work_dir/immutable"
cp -a "$stage_dir" "$immutable"
output_zip="$output_root/ts18-evidence-$stamp-$scenario_safe.zip"
ts18_make_zip "$immutable" "$output_zip"
printf 'bundle_sha256=%s\n' "$(ts18_sha256 "$output_zip")"
printf 'output=%s\n' "$output_zip"
