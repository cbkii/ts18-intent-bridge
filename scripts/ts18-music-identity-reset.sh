#!/usr/bin/env bash
# Audit-first recovery for stock com.tw.music identity after exact-package module experiments.
set -euo pipefail

script_dir=$(CDPATH='' cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
# shellcheck source=scripts/lib/ts18-toolkit-common.sh
source "$script_dir/lib/ts18-toolkit-common.sh"

mode=${1:-audit}
shift || true
confirm_clear=false
output_root=/sdcard/TS18Toolkit
while (($#)); do
  case $1 in
    --confirm-clear-data)
      confirm_clear=true
      shift
      ;;
    --output)
      (($# >= 2)) || ts18_die '--output needs a value'
      output_root=$2
      shift 2
      ;;
    *)
      ts18_die "Unknown argument: $1"
      ;;
  esac
done
case $mode in
  audit|prepare-stock-restore|verify-stock|reset-user-state|bundle) ;;
  *) ts18_die 'Mode must be audit, prepare-stock-restore, verify-stock, reset-user-state, or bundle.' ;;
esac

ts18_require_android
stamp=$(date +%Y%m%d-%H%M%S)
stage="$output_root/music-identity-$stamp-$mode"
mkdir -p "$stage"
results="$stage/command-results.tsv"
printf 'label\texit_status\tstarted_epoch\tfinished_epoch\n' >"$results"

audit() {
  ts18_capture "$results" "$stage/id.txt" id id || true
  ts18_capture "$results" "$stage/pm-path.txt" pm-path pm path com.tw.music || true
  ts18_capture "$results" "$stage/package.txt" package dumpsys package com.tw.music || true
  ts18_capture "$results" "$stage/packages-u.txt" packages-u pm list packages -f -U -u com.tw.music || true
  ts18_capture "$results" "$stage/notification.txt" notification dumpsys notification || true
  : >"$stage/stock-apks.sha256"
  while IFS= read -r apk_path; do
    apk_path=${apk_path#package:}
    [[ -n $apk_path ]] || continue
    if [[ -r $apk_path ]]; then
      printf '%s  %s\n' "$(ts18_sha256 "$apk_path")" "$apk_path" >>"$stage/stock-apks.sha256"
    elif ts18_root_available; then
      ts18_root sha256sum "$apk_path" >>"$stage/stock-apks.sha256" 2>/dev/null || true
    fi
  done < <(sed -n 's/^package://p' "$stage/pm-path.txt")
  if ts18_root_available; then
    # The quoted program is intentionally evaluated by the root-side shell.
    # shellcheck disable=SC2016
    ts18_capture_root "$results" "$stage/magisk-modules.txt" magisk-modules \
      sh -c 'for d in /data/adb/modules/*; do [ -d "$d" ] || continue; printf "%s disabled=%s remove=%s\n" "$d" "$([ -e "$d/disable" ] && echo true || echo false)" "$([ -e "$d/remove" ] && echo true || echo false)"; find "$d" -type f -o -type l; done' || true
  else
    printf '%s\n' 'root unavailable' >"$stage/magisk-modules.txt"
  fi
}

candidate_modules() {
  # The quoted program is intentionally evaluated by the root-side shell.
  # shellcheck disable=SC2016
  ts18_root sh -c '
    for d in /data/adb/modules/*; do
      [ -d "$d" ] || continue
      if find "$d" \( -type f -o -type l \) 2>/dev/null |
        grep -Eq "/(com\.tw\.music|Music|TW.?Music)[^/]*\.apk$|/data/app/com\.tw\.music"; then
        printf "%s\n" "$d"
      fi
    done
  '
}

enabled_candidate_modules() {
  # The quoted program is intentionally evaluated by the root-side shell.
  # shellcheck disable=SC2016
  ts18_root sh -c '
    for d in /data/adb/modules/*; do
      [ -d "$d" ] || continue
      [ ! -e "$d/disable" ] || continue
      if find "$d" \( -type f -o -type l \) 2>/dev/null |
        grep -Eq "/(com\.tw\.music|Music|TW.?Music)[^/]*\.apk$|/data/app/com\.tw\.music"; then
        printf "%s\n" "$d"
      fi
    done
  '
}

verify_stock_identity() {
  local package_dump=$1 path_dump=$2
  local -a enabled_modules=()
  grep -Eq 'userId=1000|sharedUserId=1000|sharedUser=android\.uid\.system|sharedUserId=android\.uid\.system' "$package_dump" ||
    ts18_die 'com.tw.music is not proven to own the recorded stock privileged/shared identity.'
  grep -Eq '^package:/(system|product|vendor|system_ext)/' "$path_dump" ||
    ts18_die 'com.tw.music source path is not a stock read-only partition path.'
  if grep -Eq '^package:/data/adb/modules/' "$path_dump"; then
    ts18_die 'A Magisk overlay still owns the visible com.tw.music APK path.'
  fi
  if ts18_root_available; then
    mapfile -t enabled_modules < <(enabled_candidate_modules)
    ((${#enabled_modules[@]} == 0)) ||
      ts18_die "An enabled exact-package candidate module remains: ${enabled_modules[*]}"
  fi
}

audit
case $mode in
  audit|bundle) ;;
  prepare-stock-restore)
    ts18_root_available || ts18_die 'Root is required to disable an exact-package module.'
    mapfile -t modules < <(candidate_modules)
    ((${#modules[@]} == 1)) ||
      ts18_die "Expected exactly one candidate exact-package module; found ${#modules[@]}. No mutation performed."
    module=${modules[0]}
    [[ $module == /data/adb/modules/* && $module != *'..'* ]] ||
      ts18_die "Unsafe module path: $module"
    ts18_root touch "$module/disable"
    printf '%s\n' "$module" >"$stage/disabled-module.txt"
    printf '%s\n' 'REBOOT_REQUIRED: reboot before verify-stock or reset-user-state.' >"$stage/NEXT_STEP.txt"
    ;;
  verify-stock)
    verify_stock_identity "$stage/package.txt" "$stage/pm-path.txt"
    printf '%s\n' 'Stock package identity verification passed.' >"$stage/verification.txt"
    ;;
  reset-user-state)
    verify_stock_identity "$stage/package.txt" "$stage/pm-path.txt"
    ts18_root pm install-existing --user 0 com.tw.music >"$stage/install-existing.txt" 2>&1
    ts18_root pm enable --user 0 com.tw.music >"$stage/enable.txt" 2>&1
    if $confirm_clear; then
      ts18_root pm clear --user 0 com.tw.music >"$stage/clear-data.txt" 2>&1
    else
      printf '%s\n' 'Data retained. Re-run with --confirm-clear-data only if a backed-up clean reset is required.' \
        >"$stage/clear-data.txt"
    fi
    ts18_capture "$results" "$stage/package-after.txt" package-after dumpsys package com.tw.music || true
    ts18_capture "$results" "$stage/pm-path-after.txt" pm-path-after pm path com.tw.music || true
    verify_stock_identity "$stage/package-after.txt" "$stage/pm-path-after.txt"
    ;;
esac

cat >"$stage/SAFETY.txt" <<'EOF'
This tool never edits packages.xml, package databases, notification-policy XML, system APKs, or
firmware partitions. A Magisk module is disabled only when exactly one candidate is identified.
Stale notification UIDs remain evidence and are not repaired by XML surgery.
EOF
checksum_tmp="$output_root/.music-identity-checksums-$stamp-$$.tmp"
(
  cd "$stage"
  find . -type f ! -name checksums.sha256 -print0 | LC_ALL=C sort -z |
    xargs -0 sha256sum
) >"$checksum_tmp"
mv "$checksum_tmp" "$stage/checksums.sha256"
zip_path="$output_root/ts18-music-identity-$stamp-$mode.zip"
ts18_make_zip "$stage" "$zip_path"
printf 'output=%s\nsha256=%s\n' "$zip_path" "$(ts18_sha256 "$zip_path")"
