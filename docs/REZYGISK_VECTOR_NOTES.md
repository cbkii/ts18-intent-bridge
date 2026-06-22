# ReZygisk, Vector, and module API notes

## Current repo position

This APK is a legacy Xposed API module. It is intended to be enabled in Vector/LSPosed, while ReZygisk supplies the lower Zygisk injection layer on the rooted TS18.

## Why not native Zygisk first?

The required first-pass behaviour is Java-level:

- rewrite `Intent` objects before launch/bind/send calls;
- alter selected `PackageManager` return values in scoped caller processes;
- capture log evidence from app-side callers.

Native Zygisk would add complexity without proving a benefit for these first targets.

## Why not modern libxposed first?

Vector advertises both legacy and modern API support. LSPosed modern API provides better framework communication, remote preferences, and future-proof module metadata. However, this project needs a small, easily-debugged Android 10 validation build first.

Modern API migration should be a separate branch after:

- current legacy hooks validate on TS18;
- preferences are proven readable in scoped processes;
- radio/music/SAF rules each work separately;
- a rollback procedure is tested.

## Modern migration checklist

- Add `src/main/resources/META-INF/xposed/java_init.list`.
- Add `src/main/resources/META-INF/xposed/module.prop`.
- Add `src/main/resources/META-INF/xposed/scope.list`.
- Implement `io.github.libxposed.api.XposedModule`.
- Replace `XSharedPreferences` with Remote Preferences if Vector exposes them reliably on TS18.
- Retain the legacy branch until the modern branch has equal validation evidence.

## Compatibility expectation

Vector/LSPosed scope remains the main safety boundary. Do not implement broad internal allowlists that silently hook every app.

## Final release relationship

**Precedent:** ReZygisk is treated as the lower Zygisk environment provider on rooted devices. Vector/LSPosed is the ART/Xposed framework that loads this Java module.

**Observed:** This APK is not a native Zygisk module: it ships no `zygisk/` shared library and does not implement Magisk native module entry points. The release remains legacy Xposed-compatible for Vector/LSPosed.
