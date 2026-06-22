#!/usr/bin/env bash
# Capture filtered logs while reproducing a launcher/picker action.

warning_count=0
error_count=0
out_dir="/sdcard/TS18IntentBridge/logs"
duration_seconds=${1:-90}
pattern=${TS18_INTENT_BRIDGE_PATTERN:-'TS18IntentBridge|ActivityTaskManager|ActivityManager|PackageManager|com\.tw\.radio|com\.navimods\.radio|com\.tw\.music|com\.tw\.media|documentsui|mixplorer|ACTION_OPEN_DOCUMENT|ACTION_GET_CONTENT|ACTION_OPEN_DOCUMENT_TREE'}

log() { printf '[INFO] %s\n' "$*" >&2; }
warn() { printf '[WARN] %s\n' "$*" >&2; warning_count=$((warning_count + 1)); }
error() { printf '[ERROR] %s\n' "$*" >&2; error_count=$((error_count + 1)); }

main() {
  case "$duration_seconds" in
    ''|*[!0-9]*) error "duration must be seconds as an integer"; return 2 ;;
  esac
  mkdir -p -- "$out_dir" || { error "Cannot create $out_dir"; return 1; }
  stamp=$(date +%Y%m%d-%H%M%S)
  log_file="$out_dir/intent-bridge-$stamp.log"

  log "Clearing logcat"
  logcat -c || warn "Could not clear logcat"
  log "Reproduce the issue now. Capturing ${duration_seconds}s to $log_file"
  sleep "$duration_seconds"
  if logcat -d | grep -iE "$pattern" > "$log_file"; then
    log "Captured matching log lines"
  else
    warn "No matching log lines captured; raw logcat may still contain useful data"
    : > "$log_file"
  fi
  printf '\nRESULT: %s\nWARNINGS: %s\nERRORS: %s\nOUTPUT: %s\n' \
    "$([ "$error_count" -eq 0 ] && echo SUCCESS || echo FAILED)" "$warning_count" "$error_count" "$log_file" >&2
  [ "$error_count" -eq 0 ]
}

main "$@"
