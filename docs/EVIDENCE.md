# Evidence and assumptions

## Observed from attached DoFun APK assets

`com.dofun.variety_V9.7.2.367.260312.apk` contains launcher matching data in `assets/apps_config.json` and `assets/apps_match_config.json`.

Relevant observed entries:

```json
{
  "compare_name": "com.tw.radio",
  "image_name": "app_radio",
  "link_image_name": "link_icon_radio",
  "name": "收音机"
}
```

```json
{
  "soft_name": "hotseat_app_radio",
  "icon_name": "link_icon_radio",
  "compare_name": "com.tw.radio",
  "compare_soft_name": "收音,电台,電台,Radio",
  "function": "program",
  "behavior": ["follow_installation"]
}
```

```json
{
  "soft_name": "hotseat_app_music",
  "icon_name": "link_icon_music",
  "compare_soft_name": "音乐,音樂,Music",
  "more_packages": [
    {
      "package_name": "com.tw.media",
      "class_name": "com.tw.music.MusicActivity"
    },
    {
      "package_name": "com.tw.music",
      "class_name": "com.tw.music.MusicActivity"
    }
  ],
  "function": "music_set",
  "behavior": ["fixed"]
}
```

This supports the `com.tw.music` → `com.tw.media/com.tw.music.MusicActivity` rule more strongly than a generic music redirect.

## Inferred

- DoFun may launch radio by package name, explicit component, or an internal `program` function. The module therefore hooks both intent starts and package-manager lookups.
- SAF redirection is only reliable when MiXplorer exposes a matching picker activity for the original action/type/category. The module uses `resolveActivity()` before rewriting where a `Context` is available.

## Requires device validation

- Exact NavRadio+ launch activity on the TS18-compatible build.
- Whether DoFun invokes `com.tw.radio` directly, performs package discovery, or uses a vendor Binder/Topway service.
- Whether MiXplorer on TS18 returns a usable URI result for `ACTION_OPEN_DOCUMENT`, `ACTION_GET_CONTENT`, and `ACTION_OPEN_DOCUMENT_TREE`.
- Whether Vector/LSPosed on this Android 10 build permits scoped system-server hooks without boot instability.

## Unsupported as a first version

- Real UID reassignment.
- Real platform signing.
- Real provider-authority ownership transfer.
- Private Topway radio/audio service impersonation.
- Patching NavRadio+ APK identity or licence state.
