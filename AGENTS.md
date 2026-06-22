# Agent Instructions

Work as a senior Android automotive, embedded, Xposed/LSPosed, and Magisk engineer.

## Hard boundaries

- Do not add a `com.tw.radio -> com.tw.media` rule.
- Do not claim full package identity spoofing is possible through LSPosed alone.
- Do not patch, rename, re-sign, or redistribute NavRadio+, MiXplorer, MiXplorer Silver, or stock TS18 APKs.
- Do not disable or delete protected TS18 packages as part of this repo.
- Do not add system/framework scope as the default.
- Do not claim tests passed unless they ran.

## Current functional requirements

1. Rewrite stock radio launches from configurable source package, default `com.tw.radio`, to configurable target package, default `com.navimods.radio`.
2. Rewrite stock music launches from configurable source package/component, default `com.tw.music/com.tw.music.MusicActivity`, to configurable target package/component, default `com.tw.media/com.tw.music.MusicActivity`.
3. Rewrite SAF/DocumentsUI picker intents to configurable package candidates, default `com.mixplorer,com.mixplorer.silver`.
4. Provide caller-side `PackageManager` compatibility only as scoped, reversible, best-effort compatibility.
5. Keep all changes reversible by disabling the module and rebooting.

## Evidence labels

Use these labels in docs, issues, commits, and PRs:

- **Observed**: directly captured on the exact TS18 or from attached APK/diagnostic inspection.
- **Inferred**: strongly implied by observed TS18 state or Android platform behaviour.
- **Precedent**: from LSPosed, Vector, ReZygisk, CorePatch, Android docs, MiXplorer docs, or related head-unit work.
- **Hypothesis**: plausible but not proven on the exact device.
- **Requires device validation**: must be tested on the TS18 before enabling broadly.
- **Unsupported**: should not be implemented without new evidence.

## Development sequence

1. Inspect latest TS18 diagnostics and target APK manifests before changing rules.
2. Keep app-side hooks first: `com.dofun.variety` only.
3. Validate radio, music, and SAF separately.
4. Add PackageManager spoofing only when a caller demonstrably queries package identity before launching.
5. Add more scoped packages one by one.
6. Treat System Framework scope as a last resort.

## Sources to read before major changes

- `docs/SECOND_PASS_RESEARCH.md`
- `docs/CONFIGURATION.md`
- `docs/TS18_SPECIFICATIONS.md`
- `docs/PACKAGE_IDENTITY_SPOOFING.md`
- `docs/TS18_VALIDATION_RUNBOOK.md`
- https://github.com/PerformanC/ReZygisk
- https://github.com/JingMatrix/Vector
- https://github.com/LSPosed/LSPosed/wiki/Develop-Xposed-Modules-Using-Modern-Xposed-API
- https://github.com/rovo89/XposedBridge/wiki/Development-tutorial
- https://developer.android.com/guide/components/intents-filters
- https://developer.android.com/training/data-storage/shared/documents-files

## Release architecture note

- This repo is a legacy-compatible LSPosed/Vector Xposed module APK unless a future task proves a native Zygisk requirement.
- Default scope metadata and runtime allowlist must remain `com.dofun.variety` only.
- Service, bind, broadcast, PendingIntent, System Framework, UID, signature, provider, Binder, and private Topway authority experiments are not release defaults.
