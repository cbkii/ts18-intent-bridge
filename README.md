# TS18 Intent Bridge

LSPosed/Vector module scaffold for TS18 / Topway / DoFun Android 10 head units.

The goal is to make hardcoded stock-launcher and picker references more tolerant of replacement apps without deleting stock packages, patching paid APKs, or pretending root equals platform signing.

## Implemented scope

Defaults are TS18-specific, but all replacement package names are user-configurable in the module app UI.

- `com.tw.radio` → configurable radio replacement, default `com.navimods.radio`.
- `com.tw.music` / `com.tw.music.MusicActivity` → configurable music replacement, default `com.tw.media` / `com.tw.music.MusicActivity`.
- SAF / DocumentsUI picker intents → configurable file-manager package candidates, default `com.mixplorer,com.mixplorer.silver`.

There is **no** `com.tw.radio → com.tw.media` rule.

## Implemented hook layers

- Caller-side intent rewriting for `startActivity`, `startActivityForResult`, `startService`, `bindService`, and broadcast sender paths.
- SAF picker rewriting for `ACTION_OPEN_DOCUMENT`, `ACTION_OPEN_DOCUMENT_TREE`, `ACTION_GET_CONTENT`, `ACTION_CREATE_DOCUMENT`, and `ACTION_PICK` when the intent is implicit or explicitly/restrictively aimed at DocumentsUI.
- Caller-side `PackageManager` compatibility hooks for selected package, component, signature, and resolver calls.
- Optional system/framework hook points for Android 10, intentionally documented as high risk and not part of the first validation pass.

## What this cannot do

The module can rewrite intents and spoof selected caller-side `PackageManager` views inside scoped processes. It cannot actually transfer package identity, installed signing certificates, Linux UID, SELinux domain, privileged permission grants, provider authorities, Binder service ownership, or private Topway service authority from one app to another.

## Target device baseline

Exact-device baseline to validate against before changing scope:

- Platform family: TS18 / Topway / DoFun / TWTHEME, `UIS8581A` / `sp9863a` references.
- Android 10 / API 29.
- DoFun launcher package observed as `com.dofun.variety`.
- Stock radio package observed as `com.tw.radio` with `com.tw.radio/.RadioActivity`.
- NavRadio+ package observed as `com.navimods.radio` with `.RadioActivity`.
- Music replacement package observed as `com.tw.media` with `com.tw.music.MusicActivity`.
- DocumentsUI absent or unreliable on the TS18 evidence set; SAF must have direct-path/manual fallback in apps even when this module is enabled.

## Build

```bash
./gradlew assembleDebug
```

Install the APK, enable it in Vector/LSPosed, open **TS18 Intent Bridge**, confirm or edit package names, then save.

## Recommended first scope

Start with the launcher only:

```text
com.dofun.variety
```

Add caller packages one at a time only after captures prove where intents originate:

```text
com.tw.service
com.tw.core
com.tw.coreservice
com.tw.carinfoservice
apps that need SAF picker redirection
```

Scope `android` / System Framework only after app-side hooks are proven and rollback is ready.

## Validation entry points

```bash
scripts/ts18-preflight.sh
scripts/capture-ts18-intents.sh
scripts/show-current-config.sh
```

See `docs/TS18_VALIDATION_RUNBOOK.md` and `docs/CONFIGURATION.md`.

## Key references

- ReZygisk: https://github.com/PerformanC/ReZygisk
- Vector: https://github.com/JingMatrix/Vector
- LSPosed: https://github.com/LSPosed/LSPosed
- LSPosed modern API wiki: https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API
- Legacy Xposed API tutorial: https://github.com/rovo89/XposedBridge/wiki/Development-tutorial
- Android intents: https://developer.android.com/guide/components/intents-filters
- Android `Intent`: https://developer.android.com/reference/android/content/Intent
- Android `PackageManager`: https://developer.android.com/reference/android/content/pm/PackageManager
- Android SAF: https://developer.android.com/training/data-storage/shared/documents-files
- MiXplorer package reference: https://mixplorer.com/
- CorePatch precedent: https://github.com/LSPosed/CorePatch
- XSpoofSignatures precedent: https://github.com/Xposed-Modules-Repo/dev.rushii.xspoofsignatures
