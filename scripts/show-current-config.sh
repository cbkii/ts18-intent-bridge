#!/usr/bin/env bash
# Show module package and likely SharedPreferences state on a rooted Android/Termux shell.

warning_count=0
error_count=0
module_pkg=${1:-dev.cbkii.ts18intentbridge}
prefs_name="bridge_config"

log() { printf '[INFO] %s\n' "$*" >&2; }
warn() { printf '[WARN] %s\n' "$*" >&2; warning_count=$((warning_count + 1)); }
error() { printf '[ERROR] %s\n' "$*" >&2; error_count=$((error_count + 1)); }

main() {
  log "Module package: $module_pkg"
  pm path "$module_pkg" || warn "pm path failed for $module_pkg"
  pref_path="/data/data/$module_pkg/shared_prefs/$prefs_name.xml"
  log "Preference path: $pref_path"
  if command -v su >/dev/null 2>&1; then
    su -c "if [ -f '$pref_path' ]; then cat '$pref_path'; else echo 'missing'; fi" || warn "Could not read prefs through su"
  else
    warn "su not available; cannot read app-private preferences"
  fi
  printf '\nRESULT: %s\nWARNINGS: %s\nERRORS: %s\n' \
    "$([ "$error_count" -eq 0 ] && echo SUCCESS || echo FAILED)" "$warning_count" "$error_count" >&2
  [ "$error_count" -eq 0 ]
}

main "$@"
