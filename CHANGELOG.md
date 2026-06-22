# Changelog

## Unreleased

- Implemented configurable radio, music, and SAF defaults in code and UI.
- Added caller allowlist/blocklist settings, verbose logging toggle, and PackageManager compatibility master toggle.
- Kept PackageManager compatibility off by default and removed signature-spoofing hooks; the module does not claim UID, signature, privileged permission, provider authority, or package identity transfer.
- Added unit tests for configuration parsing and CI checks for build, tests, and shell syntax.
- Added manual release workflow at `.github/workflows/manual-release.yml` with APK artifact collection and SHA-256 checksums.
- Removed the incorrect disabled radio-to-media fallback entirely; no code, rules, or docs implement it.
