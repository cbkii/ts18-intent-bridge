# Release workflow

**Inferred / Requires device validation:** CI can build and test the APK without a TS18. A real TS18 validation pass must still be performed before broad enablement.

## Distribution and target SDK policy

**Inferred / Requires device validation:** This repository currently produces GitHub release APKs for private side-loading on TS18 Android 10 / API 29 head units. It is not configured as a Google Play release pipeline.

The app intentionally keeps `targetSdk 29` to preserve Android 10 TS18 behaviour for Xposed/Vector preference sharing, background launches, boot/OEM integration, and storage expectations until exact-device TS18 validation proves that a higher target SDK is safe. The release lint policy narrowly disables only `ExpiredTargetSdkVersion` for that reason. Do not treat these APKs as Play-compatible artifacts without adding a separate Play-compatible variant, raising the target SDK, and validating all target-SDK behaviour changes.

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

## Optional local Gradle wrapper generation

This repository intentionally does not commit Gradle wrapper binaries. If a maintainer wants a wrapper for local or fork-specific use, generate it from a trusted Gradle installation and review the generated files before committing them in that fork:

```bash
gradle wrapper --gradle-version 8.10.2 --distribution-type bin
```

If you choose to commit wrapper artifacts later, verify the distribution URL, checksum policy, and provenance of `gradle/wrapper/gradle-wrapper.jar` before merging.

## Workflow and package visibility notes

**Observed:** The repository intentionally does not commit Gradle wrapper binaries, so CI uses `./gradlew` when a trusted wrapper is present and falls back to the installed Gradle configured by `gradle/actions/setup-gradle` otherwise.

**Inferred:** Because the TS18 prerelease build intentionally keeps `targetSdk 29`, Android 11+ package visibility `<queries>` changes are not required for this release model. If a future Play/public variant raises target SDK, add precise `<queries>` entries for the configured target packages and picker intents rather than `QUERY_ALL_PACKAGES` unless new evidence justifies it.
