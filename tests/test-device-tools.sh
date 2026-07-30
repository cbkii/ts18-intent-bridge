#!/usr/bin/env bash
set -euo pipefail

repo_root=$(CDPATH='' cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

fake_bin="$tmp/bin"
state="$tmp/state"
mkdir -p "$fake_bin" "$state"

cat >"$fake_bin/getprop" <<'EOF'
#!/usr/bin/env bash
case ${1:-} in
  init.svc.*) printf '%s\n' running ;;
  '') printf '%s\n' '[init.svc.ylog]: [running]' ;;
esac
EOF
cat >"$fake_bin/setprop" <<'EOF'
#!/usr/bin/env bash
printf '%s %s\n' "$1" "$2" >>"${TS18_FAKE_SETPROP_LOG:?}"
EOF
chmod +x "$fake_bin/getprop" "$fake_bin/setprop"

export PATH="$fake_bin:$PATH"
export TS18_FAKE_SETPROP_LOG="$tmp/setprop.log"
export TS18_LOG_GOVERNOR_MODDIR="$repo_root/log-governor"
export TS18_LOG_GOVERNOR_STATE_DIR="$state"
ctl="$repo_root/log-governor/bin/ts18-logctl"

sh "$ctl" quiet
test -s "$state/baseline.tsv"
grep -Fq 'ctl.stop ylog' "$TS18_FAKE_SETPROP_LOG"
if grep -Fq 'ctl.stop vendor.mlogservice' "$TS18_FAKE_SETPROP_LOG"; then
  printf '%s\n' 'vendor service was quieted before explicit approval' >&2
  exit 1
fi
sh "$ctl" approve vendor.mlogservice
sh "$ctl" quiet
grep -Fq 'ctl.stop vendor.mlogservice' "$TS18_FAKE_SETPROP_LOG"
sh "$ctl" stock
grep -Fq 'ctl.start ylog' "$TS18_FAKE_SETPROP_LOG"

: >"$TS18_FAKE_SETPROP_LOG"
sh "$ctl" full-diagnostics
grep -Fq 'ctl.start ylog' "$TS18_FAKE_SETPROP_LOG"
grep -Fq 'ctl.start vendor.mlogservice' "$TS18_FAKE_SETPROP_LOG"
if grep -Fq 'ctl.start slogmodem' "$TS18_FAKE_SETPROP_LOG"; then
  printf '%s\n' 'full diagnostics started an unapproved vendor service' >&2
  exit 1
fi

TS18_YLOG_WINDOW_SECONDS=60 sh "$ctl" ylog-window
window_token=$(cat "$state/ylog-window-token")
window_pid=$(cut -f1 "$state/ylog-window-pid")
window_pid_token=$(cut -f2 "$state/ylog-window-pid")
[[ $window_pid_token == "$window_token" ]]
sh "$ctl" stock
sh "$ctl" expire-ylog-window "$window_token"
[[ $(sh "$ctl" current) == stock ]]
cancelled_cmdline=''
if [[ -r /proc/$window_pid/cmdline ]]; then
  cancelled_cmdline=$(tr '\000' ' ' <"/proc/$window_pid/cmdline")
fi
if kill -0 "$window_pid" 2>/dev/null &&
  [[ $cancelled_cmdline == *ylog-window-timer*"$window_token"* ]]; then
  printf '%s\n' 'cancelled ylog timer is still running' >&2
  exit 1
fi

# Simulate a reboot killing the userspace timer while the bounded window is
# still active. apply-current must recreate a timer for the remaining duration.
TS18_YLOG_WINDOW_SECONDS=60 sh "$ctl" ylog-window
window_token=$(cat "$state/ylog-window-token")
window_pid=$(cut -f1 "$state/ylog-window-pid")
kill -TERM "$window_pid" 2>/dev/null || true
for _ in 1 2 3 4 5; do
  kill -0 "$window_pid" 2>/dev/null || break
  sleep 1
done
sh "$ctl" apply-current
rearmed_pid=$(cut -f1 "$state/ylog-window-pid")
rearmed_token=$(cut -f2 "$state/ylog-window-pid")
[[ $rearmed_token == "$window_token" ]]
[[ $rearmed_pid != "$window_pid" ]]
kill -0 "$rearmed_pid"
sh "$ctl" quiet

panel_error=''
if panel_error=$(bash "$repo_root/scripts/probe-ts18-panel-contract.sh" \
  --duration 000400 --output "$tmp/panel-invalid" 2>&1); then
  printf '%s\n' 'panel probe accepted an out-of-range decimal duration' >&2
  exit 1
fi
grep -Fq 'Duration must be 5..300 seconds.' <<<"$panel_error"

# The storage deletion mount-point guard must pass awk field references intact through root.
# shellcheck source=scripts/lib/ts18-toolkit-common.sh
source "$repo_root/scripts/lib/ts18-toolkit-common.sh"
ts18_root() { "$@"; }
[[ $(ts18_mount_point_for_path /proc) == /proc ]]

checksum_source="$tmp/checksum-source"
mkdir -p "$checksum_source/sub" "$tmp/archives"
printf 'alpha\n' >"$checksum_source/a file.txt"
printf 'beta\n' >"$checksum_source/sub/b.txt"
printf 'nested checksum evidence\n' >"$checksum_source/sub/checksums.sha256"
printf 'captured payload\n' >"$checksum_source/captured-file"
failing_find_bin="$tmp/failing-find-bin"
mkdir -p "$failing_find_bin"
cat >"$failing_find_bin/find" <<'EOF'
#!/usr/bin/env bash
printf './captured-file\0'
exit 42
EOF
chmod +x "$failing_find_bin/find"
printf 'existing manifest\n' >"$checksum_source/checksums.sha256"
if (
  export PATH="$failing_find_bin:$PATH"
  ts18_write_checksums "$checksum_source" "$checksum_source/checksums.sha256"
) >/dev/null 2>&1; then
  printf '%s\n' 'checksum enumeration failure was not propagated' >&2
  exit 1
fi
grep -Fxq 'existing manifest' "$checksum_source/checksums.sha256"
[[ $(ts18_root_sha256 "$checksum_source/a file.txt") == \
  "$(sha256sum "$checksum_source/a file.txt" | awk '{print $1}')" ]]
ts18_write_checksums "$checksum_source" "$checksum_source/checksums.sha256"
grep -Fq './sub/checksums.sha256' "$checksum_source/checksums.sha256"
(cd "$checksum_source" && sha256sum -c checksums.sha256 >/dev/null)
(cd "$tmp" && ts18_make_zip checksum-source archives/relative-output.zip)
unzip -t "$tmp/archives/relative-output.zip" >/dev/null

governor_zip="$tmp/log-governor.zip"
governor_zip_second="$tmp/log-governor-second.zip"
bash "$repo_root/tools/build-log-governor.sh" "$governor_zip" >/dev/null
bash "$repo_root/tools/build-log-governor.sh" "$governor_zip_second" >/dev/null
cmp "$governor_zip" "$governor_zip_second"
unzip -t "$governor_zip" >/dev/null
unzip -p "$governor_zip" config/candidates.list | grep -Fq yloglite
if unzip -p "$governor_zip" config/candidates.list | grep -Eq '^(logd|tombstoned|watchdog)$'; then
  printf '%s\n' 'core Android logging service entered governor candidates' >&2
  exit 1
fi

apk_dir="$tmp/apks"
mkdir -p "$apk_dir/payload"
printf '%s\n' 'com.tw.music.action.cmd musicTitle musicaArtist musicAlbum' \
  >"$apk_dir/payload/strings.txt"
(cd "$apk_dir/payload" && zip -q -X "$apk_dir/reference.apk" strings.txt)
python3 "$repo_root/tools/analyze-ts18-panel-contract.py" \
  "$apk_dir/reference.apk" --output "$tmp/panel.json" >/dev/null
grep -Fq 'com.tw.music.action.cmd' "$tmp/panel.json"
grep -Fq 'Requires device validation' "$tmp/panel.json"

printf '%s\n' 'device toolkit fixture tests: PASS'
