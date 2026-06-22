#!/usr/bin/env python3
"""Extract DoFun launcher package-matching evidence from an APK.

Usage:
  python tools/extract-dofun-evidence.py /path/to/com.dofun.variety.apk
"""
from __future__ import annotations

import json
import sys
import zipfile
from pathlib import Path

KEYS = ["com.tw.radio", "com.tw.music", "com.tw.media", "com.tw.twfileexplore", "com.navimods.radio"]
ASSETS = ["assets/apps_config.json", "assets/apps_match_config.json"]


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2
    apk = Path(argv[1])
    if not apk.is_file():
        print(f"ERROR: not a file: {apk}", file=sys.stderr)
        return 1
    with zipfile.ZipFile(apk) as zf:
        for asset in ASSETS:
            try:
                text = zf.read(asset).decode("utf-8", "replace")
            except KeyError:
                print(f"WARN: missing {asset}", file=sys.stderr)
                continue
            print(f"\n## {asset}\n")
            for key in KEYS:
                idx = text.find(key)
                if idx < 0:
                    continue
                start = max(0, idx - 600)
                end = min(len(text), idx + 900)
                print(f"### {key}\n")
                print("```text")
                print(text[start:end])
                print("```")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
