# F-Droid preparation (not submitted)

This prepares **Phoenix Cloud**, the Android app in `ACardEmulator`, not the
separate upstream Remote Smart Card Reader app or the desktop components.
Package: `com.vsmartcard.acardemulator.phoenixcloud.fdroid`, version 4.11.0 (22).
The existing fork package, Phoenix branding and GPL-3.0-or-later notices remain.

Nothing here submits an inclusion request, publishes an APK, creates a release,
or enables automatic F-Droid updates. The metadata is deliberately `Disabled`.
The GitHub workflow only builds/tests on a matching pull request or manual run;
it has no publication step or signing credentials.

## What changed

- Samsung SDK JARs, accessory service/receivers/permissions and watch settings
  have been removed from the repository. Both `full` (legacy package ID) and
  `fdroid` now contain only the free Android code; NFC/HCE/Phoenix pairing remain.
- ISO/PIV applets are vendored as unchanged source-only snapshots with their
  original licenses and pinned provenance, without their bundled proprietary SDKs.
- Complete third-party license texts are bundled in the APK and shown offline
  alongside the GPL text. A checked inventory covers all 66 release runtime
  components and the included applet sources.
- An explicit namespace keeps existing `ACardEmulator.R`/`BuildConfig` imports
  working when the source is checked out under a different directory name.
- German/English Fastlane store descriptions and version notes are in
  `ACardEmulator/fastlane/metadata/android/`.
- `tools/check_fdroid_apk.py` checks the built package, release status, manifest
  and all DEX files for Samsung SDK code. It failed on the previous laboratory
  APK because that APK still declared the Samsung receivers/permissions.

## Local validation

Use OpenJDK 17 (Gradle 8.7 also supports the locally installed OpenJDK 21), Android
SDK platform 35 and build-tools 34.0.0. Set `ANDROID_HOME` to your SDK directory.
Initialize the pinned Android source dependencies from the repository root:

```sh
git submodule update --init --recursive ACardEmulator
./ACardEmulator/gradlew -p ACardEmulator --no-daemon \
  testFdroidReleaseUnitTest assembleFdroidRelease
python3 tools/check_fdroid_apk.py \
  ACardEmulator/app/build/outputs/apk/fdroid/release/ACardEmulator-4.11.0-fdroid-release-unsigned.apk \
  "$ANDROID_HOME/build-tools/34.0.0/aapt2"
```

This produces an **unsigned** release APK, not an update for a debug-signed
laboratory installation. F-Droid signs its own builds. Existing debug installs
of the same package cannot be updated with a differently signed APK; uninstalling
would remove pairing/settings. Do not uninstall a working installation to test
this preparation.

## Initial preparation checks on 2026-10-22

- Original build: regression check reproduced Samsung manifest entries.
- Prepared release APK: package, non-debuggable status and Samsung absence passed.
- Release JVM suite: 12 tests, no failures or errors.
- Android Lint: no errors, 92 warnings; the existing Gradle plugin also warns that
  compile SDK 35 is newer than its tested SDK. No SDK/plugin upgrade is included.
- Before code cleanup, both merged manifests were checked. After cleanup, both
  variants are built without Samsung SDK code or receiver declarations.
- fdroidserver 2.4.5: metadata lint passed with the draft enabled only in a
  temporary copy, using the official category/anti-feature definitions.
- Official source scanner on a disposable copy with the recipe cleanup: zero
  fatal problems. Official APK scanner: exit code 0.
- Build and all 12 JVM tests passed again after the scanner removed bundled
  binaries and unused build files. This used the existing Gradle 8.7 and local
  Android SDK cache, not an isolated F-Droid build-server VM.
- No new physical NFC/reader tests or GitHub-hosted workflow run were performed.

## Recheck on 2026-10-23

- Current local working tree: offline release build passed, including 15 JVM
  tests with no failures/errors and the runtime license inventory guard.
- Both Python license/notice tests passed. Store text length checks passed.
- Official fdroidserver metadata lint passed with the current recipe enabled
  only in a temporary copy; it warned that `apksigner` was not on its PATH.
- The new unsigned APK passed the package, release, license-notice and Samsung
  manifest/DEX checks. SHA-256:
  `6464b02130fc683f6e3f5e16dc7f6d9eb3ddad3300fa7828bf9e7d7db1960a90`.
- This includes existing uncommitted NFC reader changes in the local tree;
  it is not a reproduction of the recipe's pinned commit. The existing Android
  Gradle plugin/SDK compatibility warnings remain. No new clean-server build,
  source scan, hardware test or publication was performed.

## Code cleanup verification

After consolidating all branch tips into `main` on 2026-10-23, the staged
copyright/source-link corrections were exported to a separate clean build tree.
That exact source passed all 12 committed release JVM tests, all three Python
notice tests, the release build, the APK notice/Samsung checks, and the official
F-Droid source and APK scanners (zero fatal problems each). The APK SHA-256 was
`252f4ed8ffba6b447239d15327820f3de549e1c4b7e88f416ec49ed7bc6be398`.
The uncommitted reader fixes and their additional tests were excluded from this
check. This still used the local SDK/Gradle cache, not a F-Droid build-server VM.

[LICENSE_REVIEW.md](../LICENSE_REVIEW.md) and [LICENSES/README.md](../LICENSES/README.md)
record the later code cleanup: all three Android release APKs build, runtime
licenses are inventoried (66 unique components across both apps), complete
notices are packaged, 12 Phoenix JVM tests and the notice/evidence tests pass,
and the offline license dialog passes a deterministic e2e check on Android.
The deliberate missing-inventory test is rejected by the Gradle license guard.
Samsung, CPOL and unlicensed legacy code removal is described there. No artwork
was changed. The external server and old Git history are not covered by a claim
that the current Android runtime is free software.

## Draft recipe

`metadata/com.vsmartcard.acardemulator.phoenixcloud.fdroid.yml` is an fdroiddata
recipe draft. Its pinned source commit must contain the build changes and store
texts, and must be publicly reachable before a future submission. It removes the
unused desktop/reader projects before source scanning. Samsung binaries and
proprietary SDK trees have now been removed from the source branch itself. Remaining
unused binary files are deleted by the scanner rather than ignored.

To test later in a disposable checkout of `fdroiddata`, copy the recipe into
`metadata/`, remove `Disabled` **only in that local test copy**, then run:

```sh
fdroid lint com.vsmartcard.acardemulator.phoenixcloud.fdroid
fdroid build --server com.vsmartcard.acardemulator.phoenixcloud.fdroid
```

Local validation is not a submission. Leave `AutoUpdateMode: None` and
`UpdateCheckMode: None` until release policy and version tags are agreed.

## Before any future submission

- Publish/review the prepared source commit and pin that exact public SHA in the
  recipe. Do not use the old laboratory tag as the source for these changes.
- Run the official F-Droid source/dependency scanner and clean-server build;
  an ordinary Gradle build alone does not establish F-Droid acceptance.
- Clarify the hosted backend's source/license and access requirements with the
  F-Droid reviewers. `NonFreeNet` is conservatively declared in this draft:
  Phoenix Cloud requires a separate Phoenix service/account for its cloud mode,
  and the hosted service may require a license. The server is not bundled here.
- Verify end-user access to pairing/server setup and provide a public support
  channel (GitHub Issues is currently disabled for this fork).
- Capture fresh store screenshots with synthetic profiles and no pairing tokens
  or personal data. None are copied from existing device/laboratory captures.
- Review [the code license inventory and cleanup](../LICENSE_REVIEW.md) and the
  current F-Droid maintenance/inclusion requirements; this fork was created on
  2026-10-14. Artwork was left unchanged at the owner's explicit request.
- Run physical NFC/reader regression tests on a release-signed test build.

References: [inclusion policy](https://f-droid.org/docs/Inclusion_Policy/),
[build metadata](https://f-droid.org/docs/Build_Metadata_Reference/),
[store metadata](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/).
