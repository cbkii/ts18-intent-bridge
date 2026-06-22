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

## Why rules are compiled-in initially

TS18 storage and permissions are constrained, and app-side hooks run inside arbitrary target processes. Reading config from `/data/adb` or shared storage from every hooked process can fail because of SELinux and app sandboxing. The first validation build uses static rules; once stable, move settings to LSPosed modern remote preferences or a small companion service.
