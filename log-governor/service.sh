#!/system/bin/sh
set -eu

MODDIR=${0%/*}
STATE_DIR=/data/adb/ts18-log-governor
mkdir -p "$STATE_DIR"
[ -f "$STATE_DIR/profile" ] || printf '%s\n' stock >"$STATE_DIR/profile"
"$MODDIR/bin/ts18-logctl" init-baseline
"$MODDIR/bin/ts18-logctl" apply-current
