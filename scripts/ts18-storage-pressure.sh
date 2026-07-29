#!/usr/bin/env bash
# Audit and apply only reviewed cleanup/debloat policy on writable TS18 storage.
set -euo pipefail

script_dir=$(CDPATH='' cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
# shellcheck source=scripts/lib/ts18-toolkit-common.sh
source "$script_dir/lib/ts18-toolkit-common.sh"

mode=${1:-audit}
shift || true
policy=
confirmation=
output_root=/sdcard/TS18Toolkit
state_root=/sdcard/TS18Toolkit/storage-state
while (($#)); do
  case $1 in
    --policy)
      (($# >= 2)) || ts18_die '--policy needs a file'
      policy=$2
      shift 2
      ;;
    --confirm)
      (($# >= 2)) || ts18_die '--confirm needs a value'
      confirmation=$2
      shift 2
      ;;
    --output)
      (($# >= 2)) || ts18_die '--output needs a directory'
      output_root=$2
      shift 2
      ;;
    *) ts18_die "Unknown argument: $1" ;;
  esac
done
case $mode in
  audit|plan|apply|rollback|compare) ;;
  *) ts18_die 'Mode must be audit, plan, apply, rollback, or compare.' ;;
esac

protected_packages='com.android.systemui com.android.settings com.android.phone com.android.providers.media com.android.externalstorage com.android.documentsui com.tw.service com.tw.service.xt com.tw.carinfoservice com.dofun.variety com.tw.music'
is_protected_package() {
  case " $protected_packages " in
    *" $1 "*) return 0 ;;
    *) return 1 ;;
  esac
}

validate_cleanup_path() {
  local path=$1
  [[ $path == /* && $path != *'..'* ]] || return 1
  case $path in
    /data/vendor/log/*|/data/vendor/ylog/*|/data/ylog/*|/data/log/*|/data/tombstones/*|/data/anr/*|/data/system/dropbox/*|/sdcard/TS18Logs/*)
      ;;
    *) return 1 ;;
  esac
  case $path in
    /|/data|/vendor|/system|/product|/system_ext|/sdcard|/storage|*/Music|*/Music/*|*/DCIM|*/DCIM/*)
      return 1
      ;;
  esac
}

ts18_require_android
stamp=$(date +%Y%m%d-%H%M%S)
stage="$output_root/storage-pressure-$stamp-$mode"
mkdir -p "$stage"
results="$stage/command-results.tsv"
printf 'label\texit_status\tstarted_epoch\tfinished_epoch\n' >"$results"

audit_state() {
  local suffix=${1:-}
  ts18_capture "$results" "$stage/df${suffix}.txt" "df${suffix}" df -k || true
  ts18_capture "$results" "$stage/df-inodes${suffix}.txt" "df-inodes${suffix}" df -i || true
  ts18_capture "$results" "$stage/mount${suffix}.txt" "mount${suffix}" mount || true
  ts18_capture "$results" "$stage/packages${suffix}.txt" "packages${suffix}" pm list packages -f -U -u || true
  ts18_capture "$results" "$stage/processes${suffix}.txt" "processes${suffix}" ps -A -o USER,PID,PPID,NAME,ARGS || true
  ts18_capture "$results" "$stage/diskstats${suffix}.txt" "diskstats${suffix}" dumpsys diskstats || true
  if ts18_root_available; then
    ts18_capture_root "$results" "$stage/block${suffix}.txt" "block${suffix}" \
      sh -c 'cat /proc/partitions; printf "\n-- dm --\n"; ls -l /dev/block/by-name /dev/block/mapper 2>&1' || true
    ts18_capture_root "$results" "$stage/largest-writable${suffix}.txt" "largest-writable${suffix}" \
      sh -c 'du -k -d 2 /data /cache /sdcard/Android/data 2>/dev/null' || true
    LC_ALL=C sort -nr "$stage/largest-writable${suffix}.txt" \
      >"$stage/largest-writable${suffix}-sorted.txt" || true
    ts18_capture_root "$results" "$stage/log-services${suffix}.txt" "log-services${suffix}" \
      sh -c 'getprop | grep -Ei "init\.svc\..*(ylog|slog|modem|mlog|cplog|log_service)"' || true
  fi
}

audit_state
case $mode in
  audit|compare) ;;
  plan)
    cat >"$stage/reviewed-policy.conf" <<'EOF'
# Audit output only. Copy this file outside the ZIP, review each target, then uncomment exact lines.
# Allowed actions:
# DISABLE_PACKAGE=com.example.noncritical
# ARCHIVE_DELETE=/data/vendor/log/example-old-log
#
# Read-only /, /system, /vendor, /product, and /system_ext files are never eligible.
# User media, current vehicle services, WebView providers, and arbitrary app data are never eligible.
EOF
    ;;
  apply)
    [[ -n $policy && -f $policy ]] || ts18_die 'apply requires --policy FILE'
    [[ $confirmation == APPLY_REVIEWED_POLICY ]] ||
      ts18_die 'apply requires --confirm APPLY_REVIEWED_POLICY'
    ts18_root_available || ts18_die 'Root is required for reviewed cleanup.'
    mkdir -p "$state_root"
    state_file="$state_root/applied-$stamp.tsv"
    : >"$state_file"
    archive_dir="$stage/pre-delete-archive"
    mkdir -p "$archive_dir"
    while IFS= read -r line || [[ -n $line ]]; do
      line=${line%%#*}
      [[ -n ${line//[[:space:]]/} ]] || continue
      case $line in
        DISABLE_PACKAGE=*)
          package_id=${line#DISABLE_PACKAGE=}
          [[ $package_id =~ ^[A-Za-z0-9_.]+$ ]] || ts18_die "Invalid package ID: $package_id"
          is_protected_package "$package_id" &&
            ts18_die "Protected package cannot be disabled: $package_id"
          pm path "$package_id" >/dev/null 2>&1 || ts18_die "Package not installed: $package_id"
          ts18_root pm disable-user --user 0 "$package_id" \
            >"$stage/disable-$(ts18_safe_name "$package_id").txt" 2>&1
          printf 'DISABLED\t%s\n' "$package_id" >>"$state_file"
          ;;
        ARCHIVE_DELETE=*)
          target=${line#ARCHIVE_DELETE=}
          validate_cleanup_path "$target" || ts18_die "Cleanup path is outside the allowlist: $target"
          ts18_root test -e "$target" || ts18_die "Cleanup target does not exist: $target"
          ts18_root test ! -L "$target" || ts18_die "Cleanup target is a symlink: $target"
          mount_point=$(ts18_mount_point_for_path "$target")
          [[ -z $mount_point ]] || ts18_die "Cleanup target is a mount point: $target"
          archive_name=$(ts18_safe_name "$target")
          ts18_root tar -czf "$archive_dir/$archive_name.tar.gz" -C / "${target#/}"
          tar -tzf "$archive_dir/$archive_name.tar.gz" >/dev/null
          ts18_root rm -rf -- "$target"
          printf 'ARCHIVED_DELETED\t%s\t%s\n' "$target" "$archive_name.tar.gz" >>"$state_file"
          ;;
        *) ts18_die "Unsupported policy line: $line" ;;
      esac
    done <"$policy"
    cp "$state_file" "$stage/applied-state.tsv"
    audit_state -after
    ;;
  rollback)
    ts18_root_available || ts18_die 'Root is required for rollback.'
    latest=$(find "$state_root" -maxdepth 1 -type f -name 'applied-*.tsv' -print 2>/dev/null |
      LC_ALL=C sort | tail -n 1)
    [[ -n $latest ]] || ts18_die 'No prior applied-state file was found.'
    while IFS=$'\t' read -r action value _; do
      case $action in
        DISABLED) ts18_root pm enable --user 0 "$value" ;;
        ARCHIVED_DELETED)
          ts18_warn "File deletion rollback requires the archived ZIP from the apply run: $value"
          ;;
      esac
    done <"$latest"
    cp "$latest" "$stage/rollback-source.tsv"
    audit_state -after
    ;;
esac

cat >"$stage/SAFETY.txt" <<'EOF'
The reported utilisation of read-only dm-backed /system or /vendor images cannot be reduced by
this live tool. It never remounts or deletes firmware partitions. Cleanup is limited to exact,
reviewed writable log paths; debloat uses reversible pm disable-user and rejects protected packages.
EOF
ts18_write_checksums "$stage" "$stage/checksums.sha256"
zip_path="$output_root/ts18-storage-pressure-$stamp-$mode.zip"
ts18_make_zip "$stage" "$zip_path"
printf 'output=%s\nsha256=%s\n' "$zip_path" "$(ts18_sha256 "$zip_path")"
