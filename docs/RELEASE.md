# Release workflow

**Inferred / Requires device validation:** CI can build and test the APK without a TS18. A real TS18 validation pass must still be performed before broad enablement.

## Manual GitHub release

Workflow path: `.github/workflows/manual-release.yml`.

Trigger it from **Actions → Manual release → Run workflow** with:

- `version_name`: semantic version string, for example `0.3.0`.
- `version_code`: optional integer override used by Gradle through `VERSION_CODE`.
- `tag_name`: optional; blank defaults to `v<version_name>`.
- `prerelease`: keep enabled until TS18 validation evidence is attached.
- `generate_notes`: lets GitHub produce release notes.
- `release_title` / `release_body`: optional human-readable release text.

The workflow checks out the repo, installs JDK 17 and Android tooling, runs `gradle clean test assembleRelease lint`, validates shell scripts with `bash -n scripts/*.sh`, collects release APKs/reports, writes SHA-256 checksums, uploads a workflow artifact, and creates or updates the GitHub release.

Unsigned release APKs are labelled by `ARTIFACT_NOTICE.txt`. Future signing support may use repository/environment secrets named `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`; those secrets are documented for future maintainers but are not required by the current unsigned build.

## Pre-release checklist

1. Build and unit-test in CI.
2. Install on a TS18 Android 10 / SDK 29 head unit with ReZygisk + Vector/LSPosed.
3. Scope only `com.dofun.variety` first.
4. Validate radio, music, and SAF separately using `docs/TS18_VALIDATION_RUNBOOK.md`.
5. Enable PackageManager compatibility only if logs show a specific caller-side package lookup needs it.
6. Keep System Framework scope disabled unless app-side logs prove it is required.
7. Attach diagnostic summaries and filtered logs to release notes when available.
