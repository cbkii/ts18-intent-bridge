# AI agent development guide

## Mission

Develop this module as a TS18-specific compatibility bridge, not as a generic Android spoofing toolkit.

## Priorities

1. Preserve bootability and stock launcher stability.
2. Keep hooks narrow, logged and reversible.
3. Do not impersonate private Topway services without evidence.
4. Prefer caller-side hooks before system-server hooks.
5. Validate on Android 10/API 29; newer Android behaviour is precedent only.

## Current implementation gaps

- No runtime UI to toggle rules.
- No LSPosed modern remote preferences yet.
- No NavRadio+ exact component discovery beyond `getLaunchIntentForPackage()`.
- No SAF result validation helper app.
- No automated on-device instrumentation tests.
- No native Zygisk component; LSPosed/Vector is the correct first implementation path.

## Recommended next tasks

1. Add a diagnostic screen that shows installed package status for all source/destination packages.
2. Add a TS18-only `logcat` capture helper with start/stop/export buttons.
3. Add LSPosed modern API support while retaining legacy compatibility for Vector.
4. Add settings backed by LSPosed remote preferences.
5. Add a test app that triggers all picker intents and records result URIs.
6. Add per-caller rule matching, so `com.tw.radio` can route differently depending on caller process or action.
7. Add component-discovery command output parser for NavRadio+.

## Do not do

- Do not patch or rename NavRadio+.
- Do not globally disable Android signature checks.
- Do not hook every installed package by default.
- Do not enable framework/system-server scope by default.
- Do not hide crashes or claim tests passed without device logs.
