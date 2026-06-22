# Architecture

## Layers

1. **Intent rewrite layer**
   Hooks caller-side Android APIs that pass `Intent` objects. It rewrites known TS18/DoFun package references before Android resolves them.

2. **PackageManager compatibility layer**
   Hooks selected `ApplicationPackageManager` methods inside scoped caller processes. This is a best-effort illusion for launcher checks such as “is `com.tw.radio` installed?” or “what launch intent should I use?”.

3. **Optional framework catch layer**
   Hooks Android 10 system-server classes (`ActivityTaskManagerService`, `ActivityManagerService`). This can catch calls missed by caller-side hooks, but it has higher boot-loop risk and should be disabled by scope until validated.

## Why this uses LSPosed/Vector rather than a pure native Zygisk module

The required changes are primarily Java framework object mutations: `Intent`, `PackageManager`, `ResolveInfo`, `PackageInfo` and related classes. LSPosed/Vector is a direct fit for this because it hooks Java methods in target app processes. ReZygisk supplies injection support underneath Vector, but a standalone native Zygisk module would be overkill for the first implementation.

## Configuration model

The module app writes configurable defaults to private SharedPreferences. Hooked legacy Xposed processes read those settings through `XSharedPreferences` with a short cache interval. Built-in defaults remain conservative and TS18-specific so disabling the module and rebooting is the rollback path.

## Release architecture decision

**Observed:** The release APK is legacy Xposed-compatible. Runtime loading uses `assets/xposed_init` and `IXposedHookLoadPackage`; the legacy Xposed API remains `compileOnly`.

**Observed:** `META-INF/xposed/module.prop` and `META-INF/xposed/scope.list` are present for manager/scope discovery. There is intentionally no `META-INF/xposed/java_init.list`, no `XposedModule` subclass, and no native `zygisk/*.so`; this avoids a confusing half-migration.

**Precedent:** ReZygisk can provide the rooted Zygisk environment, while Vector/LSPosed provides ART/Xposed hook loading. This module stays an Android/Xposed module APK for Java Intent and PackageManager interception.

## Operation-typed hooks

**Observed:** release hooks are bounded to activity launch/result paths by default. Radio, music, and SAF rules do not rewrite service starts, service binds, broadcasts, or PendingIntent creation by default. System Framework scope remains a last resort and is not in default scope metadata.
