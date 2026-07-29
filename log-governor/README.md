# TS18 Vendor Log Governor

**Observed (repository source):** This is a separately installable Magisk module. It controls
vendor diagnostic collectors only and does not modify the TS18 Intent Bridge APK.

**Observed (repository source and fixtures):** The default profile is `stock`, so installation
alone changes no service state. The first profile command records the baseline before changing any
service. First run:

```sh
su -c /data/adb/modules/ts18_log_governor/bin/ts18-logctl probe
su -c /data/adb/modules/ts18_log_governor/bin/ts18-logctl status
```

**Observed (repository source and fixtures):** Profiles:

- `stock`: restore the service state captured at first boot.
- `quiet`: stop ylog/yloglite/ylogw plus explicitly approved candidates.
- `ylog-window`: start ylog collectors for 10 minutes, then return to `quiet`.
- `full-diagnostics`: start ylog defaults plus explicitly approved vendor logging candidates for
  an intentional campaign.

**Requires device validation:** Non-ylog modem/vendor services are candidates rather than
pre-approved controls. Inspect the probe, test SIM/data, GNSS, Bluetooth, Wi-Fi, audio, reverse
camera, ACC sleep/wake and affected system-app first launch, then approve one service at a time:

```sh
su -c /data/adb/modules/ts18_log_governor/bin/ts18-logctl approve vendor.mlogservice
```

**Observed (repository source and fixtures):** Core Android `logd`, crash/tombstone, audit, and
watchdog infrastructure is never targeted. Uninstall reports success only when captured-baseline
restoration succeeds.

**Requires device validation:** To recover from a device-specific boot problem, create the module
`disable` marker from Magisk recovery (or remove the module directory), reboot, and verify all
vehicle functions before re-enabling a profile.
