# Magisk/Zygisk note

This repo intentionally does not ship a native Magisk/Zygisk module yet.

The first implementation is an LSPosed/Vector APK module because the required surfaces are Java Android framework APIs. ReZygisk is expected to provide the underlying Zygisk injection environment for Vector.

Only add a native Zygisk component if on-device logs prove a required TS18 integration path cannot be reached through LSPosed/Vector Java hooks.
