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

- Bundled Samsung SDK JARs are dependencies of `full` only. Their permissions,
  receivers and accessory metadata also live in the `full` manifest only.
- The `fdroid` variant retains the existing no-op watch service. Tizen/Samsung
  watch integration is unavailable; NFC, HCE and Phoenix pairing remain.
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

## Checked locally on 2026-10-22

- Original build: regression check reproduced Samsung manifest entries.
- Prepared release APK: package, non-debuggable status and Samsung absence passed.
- Release JVM suite: 12 tests, no failures or errors.
- Android Lint: no errors, 92 warnings; the existing Gradle plugin also warns that
  compile SDK 35 is newer than its tested SDK. No SDK/plugin upgrade is included.
- Both merged manifests checked: Samsung receivers remain in `full` only.
- fdroidserver 2.4.5: metadata lint passed with the draft enabled only in a
  temporary copy, using the official category/anti-feature definitions.
- Official source scanner on a disposable copy with the recipe cleanup: zero
  fatal problems. Official APK scanner: exit code 0.
- Build and all 12 JVM tests passed again after the scanner removed bundled
  binaries and unused build files. This used the existing Gradle 8.7 and local
  Android SDK cache, not an isolated F-Droid build-server VM.
- No new physical NFC/reader tests or GitHub-hosted workflow run were performed.

## Draft recipe

`metadata/com.vsmartcard.acardemulator.phoenixcloud.fdroid.yml` is an fdroiddata
recipe draft. Its pinned source commit must contain the build changes and store
texts, and must be publicly reachable before a future submission. It removes the
unused desktop/reader projects, Samsung binaries and full-only sources, and
unused bundled Java Card SDK/tool directories before source scanning. Remaining
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
- Recheck third-party/artwork licenses, maintenance/inclusion requirements and
  the current F-Droid policy; this fork was created on 2026-10-14.
- Run physical NFC/reader regression tests on a release-signed test build.

References: [inclusion policy](https://f-droid.org/docs/Inclusion_Policy/),
[build metadata](https://f-droid.org/docs/Build_Metadata_Reference/),
[store metadata](https://f-droid.org/docs/All_About_Descriptions_Graphics_and_Screenshots/).
