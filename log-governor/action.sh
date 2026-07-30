#!/system/bin/sh
set -eu

MODDIR=${0%/*}
CTL="$MODDIR/bin/ts18-logctl"
current=$("$CTL" current)
case "$current" in
  stock) next=quiet ;;
  quiet) next=ylog-window ;;
  ylog-window|full-diagnostics) next=stock ;;
  *) next=stock ;;
esac
printf 'Current profile: %s\nApplying profile: %s\n' "$current" "$next"
"$CTL" "$next"
"$CTL" status
