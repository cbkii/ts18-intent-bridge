# TS18 device toolkit

**Observed (repository source):** The toolkit is separate from the bridge runtime. Installing or
enabling the LSPosed module is not required to run these scripts.

## Safety and execution order

1. Run the universal collector against the currently installed Auxio package.
2. Run `ts18-music-identity-reset.sh audit`.
3. If an exact-package Magisk payload is active, use `prepare-stock-restore`, reboot, then
   `verify-stock`. Never proceed when more than one candidate module is found.
4. Stabilise DocumentsUI/SAF before the panel campaign.
5. Run the panel probe twice per action and once again after reboot while stock music is stable.
6. Run `ts18-storage-pressure.sh audit`, review its ZIP, then generate a `plan`. `apply` is allowed
   only with an edited reviewed policy and the literal confirmation.
7. Install the separate log-governor module in its default `stock` profile. Probe it, approve
   non-ylog services one at a time, validate vehicle functions, then select `quiet`.

## Universal, version-agnostic capture

```sh
bash scripts/collect-ts18-evidence.sh \
  --package com.tw.media.debug \
  --scenario clean-folder-index \
  --duration 180
```

**Observed (repository source and fixtures):** Omit `--package` to discover candidates through
installed music-player components. The bundle records application ID, version/code, UID/shared
identity evidence, base/split APK paths and hashes, processes, components, critical-package state,
Magisk inventory, one full log lane and per-UID lanes when supported. It starts logging before the
scenario, stops and waits for every writer, copies immutable staging data, writes checksums, and
requires `unzip -t` success.

**Observed (collector acceptance rule):** An Auxio capture is not accepted as app-logging proof
unless it includes `AUXIO_TS_CAPTURE_CANARY`. Package, version, flavour and hash are evidence—not
preconditions.

**Requires device validation:** Run these focused campaigns:

- installation/package identity;
- folder-picker return and source generation;
- bounded library indexing plus no-progress watchdog;
- MediaSession, audio focus and metadata;
- fixed DoFun panel controls;
- notification/channel health;
- SAF/persisted grants across two reboots;
- USB removal/reinsert;
- cold boot, ACC sleep/wake and playback recovery.

**Requires device validation:** Start capture before each causal action. A result marker is
successful only after the relevant MediaSession, audio, provider or indexing state changes.
Capture a thread/bundle snapshot before cancelling a stalled scan.

## Stock music identity recovery

```sh
bash scripts/ts18-music-identity-reset.sh audit
bash scripts/ts18-music-identity-reset.sh prepare-stock-restore
# reboot
bash scripts/ts18-music-identity-reset.sh verify-stock
```

**Observed (repository source and fixtures):** `reset-user-state` requires root and first proves a
stock read-only APK path, recorded privileged/shared identity, and absence of an enabled candidate
Magisk overlay. It retains app data unless `--confirm-clear-data` is supplied. The tool never edits
Android package databases, policy XML, or a system APK. The modified `com.tw.music_ac.apk`
reference is excluded: do not install, repair, or re-sign it.

## Private panel contract campaign

```sh
bash scripts/probe-ts18-panel-contract.sh --action observe --duration 30 --repeats 2
bash scripts/probe-ts18-panel-contract.sh --action next --duration 15 --repeats 2
python tools/analyze-ts18-panel-contract.py extracted-bundle/apks
```

**Requires device validation:** Use fixture tracks with normal, blank, long and Unicode metadata.
The probe is designed to capture before/action/after MediaSession, audio, activity, notification
and broadcast evidence plus installed base/split APKs. Static strings are not runtime contracts.
Only sender, receiver, field/type, null/default and frequency evidence repeated in two physical
runs can enter an Auxio adapter. Until then the adapter remains disabled.

## Storage pressure

```sh
bash scripts/ts18-storage-pressure.sh audit
bash scripts/ts18-storage-pressure.sh plan
bash scripts/ts18-storage-pressure.sh apply \
  --policy /sdcard/reviewed-policy.conf \
  --confirm APPLY_REVIEWED_POLICY
```

**Observed (supplied TS18 capture):** `/system` and `/vendor` are read-only dm-backed filesystems;
their utilisation percentages cannot be lowered through live deletion.

**Observed (repository source and fixtures):** The tool only inventories those partitions.
Application is limited to exact reviewed writable log paths and reversible `pm disable-user`;
protected vehicle/system packages (including `com.tw.music`) and user media are rejected. Every
deleted log target is archived first. Package rollback is supported; deleted-path rollback
requires the apply-run archive.

## Vendor log governor

Build or download `ts18-log-governor.zip`, install it in Magisk, reboot, then follow
`log-governor/README.md`.

**Observed (repository source and fixtures):** Installation defaults to `stock`. `ylog-window` is
bounded to ten minutes and returns to quiet only if its generation token is still current.
Unapproved non-ylog services are not changed by `quiet` or `full-diagnostics`. Core Android
logging, crash, audit and watchdog services are never candidates.
