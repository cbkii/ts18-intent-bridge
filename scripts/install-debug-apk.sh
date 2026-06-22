#!/usr/bin/env bash
# Build and install debug APK over adb. Run from repo root.

apk="app/build/outputs/apk/debug/app-debug.apk"

if [[ -x ./gradlew ]]; then
  build_cmd=(./gradlew assembleDebug)
elif command -v gradle >/dev/null 2>&1; then
  build_cmd=(gradle assembleDebug --no-daemon)
else
  echo "STOP: no Gradle available. Install Gradle or add a Gradle wrapper." >&2
  exit 1
fi

"${build_cmd[@]}" || exit 1
[[ -f "$apk" ]] || { echo "STOP: APK not found: $apk" >&2; exit 1; }
adb install -r "$apk"
