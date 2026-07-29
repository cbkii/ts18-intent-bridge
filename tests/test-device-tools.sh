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
cat >"$fake_bin/su" <<'EOF'
#!/usr/bin/env bash
[[ ${1:-} == -c && $# -eq 2 ]] || exit 2
bash -c "$2"
EOF
chmod +x "$fake_bin/getprop" "$fake_bin/setprop" "$fake_bin/su"

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
window_pid=$(cat "$state/ylog-window-pid")
sh "$ctl" stock
sh "$ctl" expire-ylog-window "$window_token"
[[ $(sh "$ctl" current) == stock ]]
kill "$window_pid" 2>/dev/null || true

# The storage deletion mount-point guard must pass awk field references intact through root.
# shellcheck source=scripts/lib/ts18-toolkit-common.sh
source "$repo_root/scripts/lib/ts18-toolkit-common.sh"
[[ $(ts18_mount_point_for_path /proc) == /proc ]]

checksum_source="$tmp/checksum-source"
mkdir -p "$checksum_source/sub" "$tmp/archives"
printf 'alpha\n' >"$checksum_source/a file.txt"
printf 'beta\n' >"$checksum_source/sub/b.txt"
[[ $(ts18_root_sha256 "$checksum_source/a file.txt") == \
  "$(sha256sum "$checksum_source/a file.txt" | awk '{print $1}')" ]]
ts18_write_checksums "$checksum_source" "$checksum_source/checksums.sha256"
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
