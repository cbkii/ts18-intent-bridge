#!/usr/bin/env python3
"""Build a bounded static string index for installed TS18 panel-contract APKs."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path

TOKENS = re.compile(
    rb"(?:com\.(?:tw|dofun|android)\.[A-Za-z0-9_.$:-]{2,}|"
    rb"android\.(?:intent|media|appwidget)\.[A-Za-z0-9_.$:-]{2,}|"
    rb"[A-Za-z0-9_.$-]*(?:music|media|track|artist|album|duration|progress|seek|play|pause|next|prev|binder|aidl)[A-Za-z0-9_.$:-]*)",
    re.IGNORECASE,
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def strings(data: bytes) -> set[str]:
    found = {match.group().decode("utf-8", "replace") for match in TOKENS.finditer(data)}
    # DEX/resources may contain UTF-16LE literals.
    compact = data.replace(b"\x00", b"")
    found.update(match.group().decode("utf-8", "replace") for match in TOKENS.finditer(compact))
    return {value[:300] for value in found if 3 <= len(value) <= 300}


def analyze(apk: Path) -> dict[str, object]:
    entry_hits: dict[str, list[str]] = {}
    remaining_bytes = 256 * 1024 * 1024
    with zipfile.ZipFile(apk) as archive:
        for entry in archive.infolist():
            compression_ratio = entry.file_size / max(entry.compress_size, 1)
            if (
                entry.is_dir()
                or entry.file_size > 64 * 1024 * 1024
                or entry.file_size > remaining_bytes
                or compression_ratio > 250
            ):
                continue
            data = archive.read(entry)
            remaining_bytes -= len(data)
            values = sorted(strings(data))
            if values:
                entry_hits[entry.filename] = values[:1000]
    return {
        "apk": apk.name,
        "sha256": sha256(apk),
        "classification": "Static APK observation; field types and runtime use — Requires device validation",
        "entries": entry_hits,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path, help="APK file or directory containing captured APKs")
    parser.add_argument("--output", type=Path, default=Path("panel-static-index.json"))
    args = parser.parse_args()
    apks = [args.input] if args.input.is_file() else sorted(args.input.glob("*.apk"))
    if not apks:
        parser.error("no APK files found")
    report = {"schema": 1, "apks": [analyze(apk) for apk in apks]}
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
