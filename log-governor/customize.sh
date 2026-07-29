#!/system/bin/sh
set -eu

ui_print "- Installing TS18 Vendor Log Governor"
ui_print "- Default profile: stock (no service changes)"
ui_print "- Use the module Action or bin/ts18-logctl after reviewing probe output"
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
set_perm "$MODPATH/bin/ts18-logctl" 0 0 0755
