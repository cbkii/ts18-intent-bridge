# Package identity spoofing limits

## What this module can spoof

Inside scoped caller processes, this module can intercept selected Java calls and return adjusted values:

- intent targets before `startActivity`, `startService`, `bindService`, or broadcast calls;
- `PackageManager.getPackageInfo()` results;
- `PackageManager.getApplicationInfo()` results;
- `PackageManager.getLaunchIntentForPackage()` results;
- component info returned by `getActivityInfo`, `getServiceInfo`, and `getProviderInfo`;
- selected `checkSignatures(String, String)` results;
- selected `resolveActivity` and query results.

This is caller-side compatibility only. It is useful when a launcher checks "does `com.tw.radio` exist?" before launching a hardcoded component.

## What it cannot spoof

It cannot truly change:

- installed package names in the system package database;
- signing certificates;
- Linux UID/appId;
- SELinux domain;
- priv-app allowlist grants;
- sharedUserId membership;
- provider authorities owned by another APK;
- real Binder service endpoints;
- native checks performed outside hooked Java methods;
- checks in unscoped processes.

## PackageManager hook policy

PackageManager compatibility is enabled per rule because TS18 launcher integration may query package identity before launching. Keep it enabled only for the scoped caller packages that need it.

Disable PackageManager spoofing if:

- the target app launches correctly with pure intent rewrite;
- the caller performs signature or UID-sensitive work;
- logs show signature mismatch, provider authority confusion, or repeated crashes.

## Provider and service authority policy

Do not claim providers or services are bridged until explicit evidence exists.

Redirecting an activity launch from `com.tw.music` to `com.tw.media` is a different layer from binding to `com.tw.music.MusicService`, sending Topway media broadcasts, or satisfying private `com.tw.*` service permissions.

Redirecting SAF picker intents to MiXplorer is different from implementing `com.android.documentsui` or a real `DocumentsProvider` with persisted URI grants.
