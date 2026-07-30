#!/system/bin/sh
set -eu

MODDIR=${0%/*}
restored=false
if [ -x "$MODDIR/bin/ts18-logctl" ]; then
  if "$MODDIR/bin/ts18-logctl" stock; then
    restored=true
  fi
fi
if [ "$restored" = true ]; then
  printf '%s\n' 'Baseline service state restored where the recorded init services still exist.'
else
  printf '%s\n' 'WARNING: baseline service state was not restored; ts18-logctl was missing or failed.' >&2
fi
