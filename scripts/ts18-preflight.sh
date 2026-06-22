#!/usr/bin/env bash
# Collect minimal package and environment state before testing TS18 Intent Bridge.
# Designed for Android/Termux or adb shell with root available.

warning_count=0
error_count=0
out_dir="/sdcard/TS18IntentBridge/preflight-$(date +%Y%m%d-%H%M%S)"
package_list=${TS18_INTENT_BRIDGE_PACKAGES:-'com.dofun.variety com.tw.radio com.navimods.radio com.tw.music com.tw.media com.mixplorer com.mixplorer.silver com.android.documentsui dev.cbkii.ts18intentbridge dev.cbkii.ts18intentbridge.debug'}

log() { printf '[INFO] %s\n' "$*" >&2; }
warn() { printf '[WARN] %s\n' "$*" >&2; warning_count=$((warning_count + 1)); }
error() { printf '[ERROR] %s\n' "$*" >&2; error_count=$((error_count + 1)); }

run_capture() {
  local name=$1
  shift
  if "$@" > "$out_dir/$name.txt" 2>&1; then
    log "captured $name"
  else
    warn "failed to capture $name"
  fi
}

main() {
  mkdir -p -- "$out_dir" || { error "Cannot create $out_dir"; return 1; }
  run_capture id id
  run_capture getprop getprop
  run_capture packages pm list packages -f
  for pkg in $package_list; do
    safe_name=$(printf '%s' "$pkg" | tr -c 'A-Za-z0-9._-' '_')
    run_capture "pm-path-$safe_name" pm path "$pkg"
    run_capture "dumpsys-package-$safe_name" dumpsys package "$pkg"
  done
  run_capture current-top dumpsys activity top
  run_capture activity-intents dumpsys activity intents
  printf '\nRESULT: %s\nWARNINGS: %s\nERRORS: %s\nOUTPUT: %s\n' \
    "$([ "$error_count" -eq 0 ] && echo SUCCESS || echo FAILED)" "$warning_count" "$error_count" "$out_dir" >&2
  [ "$error_count" -eq 0 ]
}

main "$@"
