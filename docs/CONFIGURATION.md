# Configuration

Open the module app **TS18 Intent Bridge** after installation. Defaults are written on first launch.

## Defaults

| Rule | Source | Default target | User configurable |
| --- | --- | --- | --- |
| Radio | `com.tw.radio` | `com.navimods.radio` | yes |
| Music | `com.tw.music/com.tw.music.MusicActivity` | `com.tw.media/com.tw.music.MusicActivity` | yes |
| SAF / DocumentsUI | picker actions | `com.mixplorer,com.mixplorer.silver` | yes |

## Radio settings

- Source package: default `com.tw.radio`.
- Replacement package: default `com.navimods.radio`.
- Replacement class: blank by default. Blank means use `PackageManager.getLaunchIntentForPackage()` for the replacement app.
- Use replacement launch intent: enabled by default.
- Spoof selected PackageManager answers: enabled by default, but still only caller-side.

Set a replacement class only after confirming it exists, for example:

```text
com.navimods.radio.RadioActivity
```

## Music settings

- Source package: default `com.tw.music`.
- Source class: default `com.tw.music.MusicActivity`.
- Replacement package: default `com.tw.media`.
- Replacement class: default `com.tw.music.MusicActivity`.
- Use replacement launch intent: disabled by default because this redirect is component-specific.
- Spoof selected PackageManager answers: enabled by default.

## SAF settings

Provide a comma or whitespace-separated list of package candidates. Earlier candidates win when they resolve:

```text
com.mixplorer,com.mixplorer.silver
```

You can use another file manager package if it accepts compatible picker intents and returns usable results.

## Applying changes

After saving settings:

1. Force-stop and restart the scoped caller app, or reboot.
2. Reproduce exactly one use case.
3. Capture logs with `scripts/capture-ts18-intents.sh`.

Hooked processes cache settings for a short interval. A reboot is the cleanest way to prove a configuration change.

## STOP conditions

Stop and disable the module if any of these occur:

- DoFun launcher repeatedly crashes.
- SystemUI, Topway core services, reverse camera, radio hardware, or boot are affected.
- PackageManager hooks create signature or UID errors.
- MiXplorer opens but the caller receives no usable result.
- A target package does not resolve from the scoped caller.
