# TS18 specifications and project evidence

## Exact-device baseline

**Observed from project diagnostics:** target work is for a TS18/Topway/DoFun/TWTHEME Android 10 head unit, not a generic Android phone or generic TS10/TS10M unit.

Latest exact-device baseline used in this repo:

```text
Device/build family: s9863a1h10_Natv / uis8581a2h10 / sp9863a
Build: TS18.2.2_20241210.165912_WINDOW-THEME1
FOTA identity: WINDOW-THEME1_1000
Android: 10 / API 29
Display: 1280x720 physical, app area approximately 1225x720 with top/right system regions
Root: Magisk 28.1 style environment, SELinux permissive in captured root diagnostics
```

## Board specification precedent

**Precedent from attached Toparea TS18 4G DSP specifications:** TS18 4G DSP board documentation describes a Spreadtrum/UIS8581A-based Android 10 design with an 8-core A55 CPU up to 1.6 GHz, PowerVR GE8322 GPU, LPDDR4X RAM options, eMMC storage options, two USB outputs with one Host-only and one OTG-capable port, Bluetooth 5.0, and SI4755 digital radio with optional QN8035 radio.

Treat this as board-family precedent unless PCB, build, panel, and package captures match the exact unit.

## Radio-specific relevance

**Observed from diagnostics:** stock radio is a real TS18 package and component:

```text
/system/priv-app/com.tw.radio_78cc/com.tw.radio_78cc.apk = com.tw.radio
com.tw.radio/.RadioActivity
```

**Observed from diagnostics:** NavRadio+ is installed separately as:

```text
com.navimods.radio
com.navimods.radio/.RadioActivity
```

This repo rewrites caller intents. It does not replace the SI4755/QN8035 radio chip, MCU configuration, radio region, antenna power, or vendor DSP/radio services.

## Music-specific relevance

**Observed from project work:** the stock DoFun/Topway music integration uses `com.tw.music` contracts, while the replacement app path uses a `com.tw.media` package exposing `com.tw.music.MusicActivity` for DoFun compatibility.

This repo keeps those contracts configurable because Auxio-TS or another app may later use a different package/component.

## Storage / SAF relevance

**Observed from project diagnostics:** TS18 storage and SAF support differ from normal phones. USB paths such as `/storage/usbdisk0` and `/storage/usbdisk1` are important, while DocumentsUI may be absent or unreliable. Apps still need direct-path and manual fallbacks.

This repo redirects picker intents only as a convenience layer. It cannot make a replacement file manager become Android DocumentsUI or grant persisted URI permissions that the replacement does not return.

## Protected packages and services

Treat these as protected until proven otherwise:

```text
com.tw.service
com.tw.core
com.tw.coreservice
com.tw.carinfoservice
com.tw.bt
com.tw.eq
com.tw.reverse
com.android.systemui
com.android.providers.media
com.dofun.variety
ZLink / SLink / GOCSDK / ylog / updater packages
```

Scope the module narrowly and add packages one at a time.
