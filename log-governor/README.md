# TS18 Vendor Log Governor

This is a separately installable Magisk module. It controls vendor diagnostic collectors only and
does not modify the TS18 Intent Bridge APK.

The default profile is `stock`, so installation alone changes no service state. First run:

```sh
su -c /data/adb/modules/ts18_log_governor/bin/ts18-logctl probe
su -c /data/adb/modules/ts18_log_governor/bin/ts18-logctl status
```

Profiles:

- `stock`: restore the service state captured at first boot.
- `quiet`: stop ylog/yloglite/ylogw plus explicitly approved candidates.
- `ylog-window`: start ylog collectors for 10 minutes, then return to `quiet`.
- `full-diagnostics`: start every known vendor logging candidate for an intentional campaign.

Non-ylog modem/vendor services are deliberately candidates rather than quiet defaults. Inspect the
probe, test SIM/data, GNSS, Bluetooth, Wi-Fi, audio, reverse camera, ACC sleep/wake and affected
system-app first launch, then approve one service at a time:

```sh
su -c /data/adb/modules/ts18_log_governor/bin/ts18-logctl approve vendor.mlogservice
```

Core Android `logd`, crash/tombstone, audit, and watchdog infrastructure is never targeted.
Uninstall attempts to restore the captured baseline. Boot recovery: create the module `disable`
marker from Magisk recovery or remove the module directory, then reboot.
