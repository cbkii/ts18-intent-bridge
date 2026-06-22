# Second-pass research notes

Updated scope: radio, music, and SAF redirects only. The former radio-to-media fallback is not part of this repo.

## ReZygisk

**Precedent:** ReZygisk is a fork of Zygisk Next and describes itself as a standalone implementation of Zygisk that provides Zygisk API support for KernelSU, APatch, and Magisk.

URL: https://github.com/PerformanC/ReZygisk

For this project, ReZygisk is treated as the zygote injection provider. The current module is not a native Zygisk module. It is an Xposed/LSPosed-style Java hook module intended to run under Vector/LSPosed, with ReZygisk providing the lower injection layer on the rooted TS18.

Practical implication:

- Do not add native Zygisk code unless Java hooks cannot reach the target call path.
- If native Zygisk becomes necessary, use a separate module or flavour based on `topjohnwu/zygisk-module-sample` and keep it isolated.

URL: https://github.com/topjohnwu/zygisk-module-sample

## Vector / JingMatrix

**Precedent:** Vector describes itself as a modern Xposed framework and states it supports both legacy and modern hooking standards, including the legacy Xposed API and modern libxposed API.

URL: https://github.com/JingMatrix/Vector

Current scaffold uses the legacy API because:

- it is smaller and easier to validate on Android 10 / API 29;
- Vector advertises legacy compatibility;
- the required first-pass hooks are ordinary Java method hooks;
- modern API remote preferences and service APIs are useful but add moving parts that should be validated after core behaviour works.

Future migration path:

- add a `modern` branch/flavour using `io.github.libxposed.api.XposedModule`;
- add `src/main/resources/META-INF/xposed/java_init.list`;
- add `META-INF/xposed/module.prop` and `META-INF/xposed/scope.list`;
- replace legacy `XSharedPreferences` with modern Remote Preferences where Vector/LSPosed exposes them reliably.

## LSPosed modern API

**Precedent:** LSPosed modern API documentation says modern modules use `META-INF/xposed/java_init.list` instead of `assets/xposed_init`, use `META-INF/xposed/scope.list`, and can communicate with the framework for scope management, shared preferences, remote files, and framework/version checks.

URL: https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API

The same page documents preference options. It distinguishes legacy `XSharedPreferences`, new `XSharedPreferences`, Remote Preferences, and Remote Files. This scaffold uses legacy `XSharedPreferences` for the first TS18 validation build, plus a simple module UI that writes `bridge_config` preferences. Agents should migrate to Remote Preferences only after proving Vector support on the actual head unit.

## Legacy Xposed API

**Precedent:** the legacy API uses `IXposedHookLoadPackage`, `XC_MethodHook`, `XposedBridge.hookMethod`, and `assets/xposed_init`.

URL: https://github.com/rovo89/XposedBridge/wiki/Development-tutorial

This scaffold remains legacy-compatible. It should be scoped narrowly in Vector/LSPosed. Do not depend on hidden framework internals until app-side hooks fail and logs prove why.

## Android intent model

**Precedent:** Android documents explicit intents as specifying the component/package that satisfies the intent. This is exactly why hardcoded `com.tw.radio/.RadioActivity` and `com.tw.music/.MusicActivity` calls need rewriting before the framework resolves them.

URLs:

- https://developer.android.com/guide/components/intents-filters
- https://developer.android.com/reference/android/content/Intent

## Android PackageManager model

**Precedent:** `PackageManager` is the Android API for retrieving installed package information. This module only spoofs selected caller-side results inside hooked processes; it does not alter the real package database.

URL: https://developer.android.com/reference/android/content/pm/PackageManager

## SAF / DocumentsUI

**Precedent:** Android SAF uses picker intents such as `ACTION_OPEN_DOCUMENT`, `ACTION_OPEN_DOCUMENT_TREE`, and `ACTION_CREATE_DOCUMENT` to let users select files or directories through document providers.

URL: https://developer.android.com/training/data-storage/shared/documents-files

On the TS18 evidence set, DocumentsUI is absent/unreliable, so this module attempts a practical user-space redirect to a configured file manager. This is not equivalent to providing real persisted `content://` URI permissions unless the replacement app returns compatible URI results.

## MiXplorer

**Precedent:** the MiXplorer website lists the free package name as `com.mixplorer`; the Play Store page for MiXplorer Silver uses package ID `com.mixplorer.silver`.

URLs:

- https://mixplorer.com/
- https://play.google.com/store/apps/details?id=com.mixplorer.silver

Defaults therefore use `com.mixplorer,com.mixplorer.silver`, but the module UI lets users replace that list with any package candidates.

## Existing hook precedents

Use these as concepts, not code to copy blindly:

- CorePatch: signature and package-manager hook precedent. URL: https://github.com/LSPosed/CorePatch
- XSpoofSignatures: caller-side signature spoof precedent. URL: https://github.com/Xposed-Modules-Repo/dev.rushii.xspoofsignatures
- XPrivacyLua: broad app-scoped Xposed hook precedent. URL: https://github.com/M66B/XPrivacyLua
- App Settings Reborn: scoped per-app Xposed module precedent. URL: https://github.com/BlueCat300/App-Settings-Reborn

## Design conclusion

**Implemented:** legacy Xposed Java hooks, scoped to caller packages, with configurable replacement package names and best-effort PackageManager compatibility.

**Deferred:** native Zygisk hooks, modern libxposed migration, system_server default scope, provider authority spoofing, UID/signature impersonation, and any stock APK replacement.
