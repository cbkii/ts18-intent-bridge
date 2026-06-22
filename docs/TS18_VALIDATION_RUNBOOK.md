# TS18 validation runbook

## Phase 0: backup and rollback

Confirm you can disable the module in Vector/LSPosed and reboot. Keep a known-good boot image and Magisk recovery method available.

## Phase 1: preflight

Run:

```bash
scripts/ts18-preflight.sh
```

Required captures:

- `pm path com.dofun.variety`
- `pm path com.tw.radio`
- `pm path com.navimods.radio` or your configured radio replacement
- `pm path com.tw.music`
- `pm path com.tw.media` or your configured music replacement
- `pm path com.mixplorer` / `com.mixplorer.silver` or your configured file manager

## Phase 2: app-side scope only

Enable the module only for:

```text
com.dofun.variety
```

Reboot or force-stop DoFun.

Test exactly one action at a time:

1. Press stock launcher radio affordance.
2. Press stock launcher music affordance.
3. Trigger an app’s file picker that normally opens DocumentsUI.

Capture logs after each separate test:

```bash
scripts/capture-ts18-intents.sh
```

## Phase 3: package-manager compatibility

If a launch does not redirect, inspect logs for:

- hardcoded component names;
- package existence checks;
- `resolveActivity` returning stock package;
- signature, UID, permission, or provider-authority checks;
- target package not resolving from the caller.

Only then adjust PackageManager spoofing or add a caller package to scope.

## Phase 4: Topway service scope

Add one package at a time only if logs prove it is the caller:

```text
com.tw.service
com.tw.core
com.tw.coreservice
com.tw.carinfoservice
```

Reboot between changes. Validate radio, music, reverse camera, SystemUI, Bluetooth, and launcher after each scope change.

## Phase 5: system framework scope

System Framework / `android` scope is last resort only.

STOP unless:

- app-side hooks failed;
- logs show the relevant intent reaches framework before any caller-side hook can catch it;
- rollback is ready;
- only one rule is enabled for the test.

## Expected success

- Radio launcher action opens the configured radio replacement.
- Music launcher action opens the configured music replacement.
- SAF picker attempts open the configured file manager candidate, and the calling app receives a usable result.
- No launcher, SystemUI, reverse camera, Topway service, radio service, or boot regressions occur.

## Failure capture

If anything fails, capture:

```bash
logcat -d | grep -iE 'TS18IntentBridge|ActivityTaskManager|ActivityManager|PackageManager|com.tw.radio|com.navimods.radio|com.tw.music|com.tw.media|documentsui|mixplorer' > /sdcard/TS18IntentBridge/failure.log
dumpsys package com.dofun.variety > /sdcard/TS18IntentBridge/dofun-package.txt
dumpsys activity top > /sdcard/TS18IntentBridge/activity-top.txt
```

Then disable the module and reboot.

## Result levels for SAF validation

Record SAF outcomes separately:

1. picker opens;
2. user can select a file/folder;
3. caller receives a usable result URI/path;
4. persisted URI grants work, if the caller requires them;
5. direct-path/manual fallback remains available.

**Requires device validation:** A replacement file manager opening does not prove full SAF/DocumentsUI compatibility.
