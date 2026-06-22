# References

## Injection / framework layer

- ReZygisk: https://github.com/PerformanC/ReZygisk
- Zygisk Next: https://github.com/Dr-TSNG/ZygiskNext
- Zygisk native module sample: https://github.com/topjohnwu/zygisk-module-sample
- Vector / JingMatrix: https://github.com/JingMatrix/Vector
- LSPosed: https://github.com/LSPosed/LSPosed
- LSPosed website: https://lsposed.org/
- LSPosed modern Xposed API: https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API
- Legacy Xposed API tutorial: https://github.com/rovo89/XposedBridge/wiki/Development-tutorial
- XposedBridge API reference: https://api.xposed.info/reference/de/robv/android/xposed/XposedBridge.html
- XSharedPreferences API reference: https://api.xposed.info/reference/de/robv/android/xposed/XSharedPreferences.html

## Android platform docs

- Intents and intent filters: https://developer.android.com/guide/components/intents-filters
- `Intent` API: https://developer.android.com/reference/android/content/Intent
- `PackageManager` API: https://developer.android.com/reference/android/content/pm/PackageManager
- Storage Access Framework: https://developer.android.com/training/data-storage/shared/documents-files
- Document provider overview: https://developer.android.com/guide/topics/providers/document-provider
- `DocumentsContract`: https://developer.android.com/reference/android/provider/DocumentsContract

## Hooking precedents

- CorePatch: https://github.com/LSPosed/CorePatch
- XSpoofSignatures: https://github.com/Xposed-Modules-Repo/dev.rushii.xspoofsignatures
- XPrivacyLua: https://github.com/M66B/XPrivacyLua
- App Settings Reborn: https://github.com/BlueCat300/App-Settings-Reborn
- Hide My Applist: https://github.com/Dr-TSNG/Hide-My-Applist

## Replacement app references

- MiXplorer official site: https://mixplorer.com/
- MiXplorer Silver Play Store package: https://play.google.com/store/apps/details?id=com.mixplorer.silver
- MiXplorer XDA thread: https://xdaforums.com/t/app-2-3-mixplorer-v6-x-released-fully-featured-file-manager.1523691/

## TS18 / Topway evidence bundled outside repo

- `combined-small.pdf`: attached TS18/Toparea board and upgrade documentation.
- `TS18-root-diagnostics-20260618-151636Z.tar.gz`: exact-device runtime diagnostics.
- `TS18-props.zip`: exact-device property captures.
- `com.dofun.variety_V9.7.2.367.260312.apk`: launcher APK for static inspection.
- `com.tw.music_TW_THEME.20240715.apk` and `com.tw.music_ac.apk`: stock/related music APKs.
- `NavRadio_Plus_v4_00_PREMIUM (1).apk`: reference APK only; validate minSdk/target compatibility before using on TS18.

## 2026 release-readiness references

- ReZygisk repository: https://github.com/PerformanC/ReZygisk — **Precedent** for the device-side Zygisk-compatible injection environment; use release notes before recommending a specific device build.
- ReZygisk releases: https://github.com/PerformanC/ReZygisk/releases — **Precedent** for packaging/version checks before TS18 install.
- JingMatrix/Vector repository: https://github.com/JingMatrix/Vector — **Precedent** for the modern LSPosed-compatible runtime expected on the target device.
- JingMatrix/Vector releases: https://github.com/JingMatrix/Vector/releases — **Precedent** for Vector/LSPosed API compatibility notes, including Android 10 fixes when present.
- LSPosed modern API wiki: https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API — **Precedent** for `META-INF/xposed/scope.list` and `module.prop` metadata.
- Legacy Xposed tutorial: https://github.com/rovo89/XposedBridge/wiki/Development-tutorial — **Precedent** for the legacy `IXposedHookLoadPackage` entrypoint retained by this module.
- Android intents and filters: https://developer.android.com/guide/components/intents-filters — **Precedent** for preserving action, data, MIME type, categories, extras, and flags while redirecting launches.
- Android `PackageManager`: https://developer.android.com/reference/android/content/pm/PackageManager — **Precedent** for the bounded caller-visible metadata APIs this module can shim.
- Android Storage Access Framework overview: https://developer.android.com/guide/topics/providers/document-provider — **Precedent** for SAF/DocumentsProvider limitations, persisted URI grants, and why a generic file manager is not automatically a full DocumentsUI replacement.
- Android documents/files training: https://developer.android.com/training/data-storage/shared/documents-files — **Precedent** for user-facing picker flows and result expectations.
- MiXplorer official site: https://mixplorer.com/ — **Precedent** only for package-reference validation; this repo must not redistribute or modify MiXplorer/MiXplorer Silver APKs.
- LSPosed/CorePatch: https://github.com/LSPosed/CorePatch — **Precedent** for PackageManager hook boundaries and why global signature/package spoofing is not appropriate here.
- XSpoofSignatures: https://github.com/Xposed-Modules-Repo/dev.rushii.xspoofsignatures — **Precedent** demonstrating that signature spoofing is a separate high-risk problem space; this module intentionally does not implement it.
