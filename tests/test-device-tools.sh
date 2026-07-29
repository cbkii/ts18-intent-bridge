#!/usr/bin/env bash
set -euo pipefail

repo_root=$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
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

sh "$ctl" init-baseline
sh "$ctl" quiet
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

governor_zip="$tmp/log-governor.zip"
bash "$repo_root/tools/build-log-governor.sh" "$governor_zip" >/dev/null
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
