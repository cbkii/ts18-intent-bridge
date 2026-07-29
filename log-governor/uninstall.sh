#!/system/bin/sh
set -eu

MODDIR=${0%/*}
if [ -x "$MODDIR/bin/ts18-logctl" ]; then
  "$MODDIR/bin/ts18-logctl" stock || true
fi
printf '%s\n' 'Baseline service state restored where the recorded init services still exist.'
