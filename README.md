# TS18 Device Toolkit

This repository now maintains the audit-first TS18 device toolkit and the separately installable
Vendor Log Governor.

## Runtime bridge retirement

The standalone Xposed intent-rewriting APK is retired and is no longer built, uploaded, or released
from this repository. Its source remains only as historical reference.

For Auxio-TS, use the signed LSPosed addon shipped by
[Auxio-TS](https://github.com/cbkii/Auxio-TS). That addon is purpose-built for the stock
`com.tw.music` process, is statically scoped only to `com.tw.music`, and forwards media control
to the separately signed `com.tw.media` app. Do not install a legacy standalone bridge APK or the
retired exact-package Magisk overlay.

## Maintained outputs

Canonical CI validates and publishes two short-lived workflow artifacts:

- `ts18-device-toolkit.zip`: evidence collection, stock-music identity recovery, private-panel
  contract probing, and reviewed storage-pressure tooling.
- `ts18-log-governor.zip`: a reversible Magisk governor for bounded vendor-log campaigns.

Both artifacts include self-verifying SHA-256 sidecars. See
[docs/DEVICE_TOOLKIT.md](docs/DEVICE_TOOLKIT.md) and
[log-governor/README.md](log-governor/README.md) for the safety boundaries and device order.

## Safety boundary

The toolkit does not patch paid APKs, edit PackageManager databases, remount read-only firmware
partitions, delete user media, or treat root as platform signing. Physical TS18 validation remains
required before applying any reviewed mutation or enabling a non-stock logging profile.
