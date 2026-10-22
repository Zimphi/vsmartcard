# Code license review — 2026-10-22

Scope: the current `codex/fdroid-preparation` branch of Zimphi/vsmartcard,
including Android runtime dependencies, source submodules and known restricted
legacy components. This is evidence of the licenses found and the cleanup
performed, not a warranty of authorship, non-infringement or F-Droid acceptance.
The F-Droid recipe is still **Disabled**. No F-Droid publication is authorized.

## Removed or excluded

| Component | Evidence / reason | Result |
| --- | --- | --- |
| Samsung accessory SDK JARs | Bundled SDK binaries without a demonstrated free-software grant | Removed both JARs, proprietary service implementation, receivers, permissions, accessory metadata and Gear store/settings integration. Both Android flavors now use the same free runtime dependencies. |
| Java Card SDK/tool trees in IsoApplet/PivApplet | Sun Java Card 2.1.2 Binary Code License included internal-use and redistribution restrictions | Replaced those two submodules with source-only snapshots of the exact pinned applet commits; no SDKs, tools or binaries are retained from them. Original source headers and free licenses remain. |
| DevMsi | Included LICENSE states Code Project Open License 1.02 | Removed submodule and MSI integration. |
| BixVReader Windows driver | The upstream README attributes it to Fabio Ottavi's CodeProject implementation; the original article labels that code CPOL | Removed driver, solution, installer and install/uninstall scripts. Windows virtual PC/SC reader/MSI support is no longer supplied. Windows CI now builds only the free command-line programs. |
| Legacy `eID_gui` | No license declaration found in its sources, empty README or component directory, despite an AUTHORS file | Removed this unused standalone Python 2 GUI's source and build definitions rather than assigning a license to someone else's code. Its AUTHORS file and images remain unchanged for the code-only scope. This is an unresolved permission question, not a finding of infringement. |

Original driver provenance:
https://www.codeproject.com/Articles/134010/An-UMDF-Driver-for-a-Virtual-Smart-Card-Reader

No replacement Windows driver is implemented. The free Unix virtual-reader,
PC/SC relay, CCID and Android code remain. The GPL Tizen source and a full GPL-3.0 COPYING text are retained as
reference, but its Samsung companion integration is no longer supported by the
Android app. External vendor SDKs are not bundled.

## Retained Android code and obligations

| Component | License evidence | Source |
| --- | --- | --- |
| ACardEmulator / Phoenix Cloud | Original GPL-3.0-or-later headers and COPYING; fork app changes follow that license | `ACardEmulator/` |
| jCardSim | Apache-2.0 in pinned README/POM and source headers; original Bouncy Castle NOTICE | `app/src/main/external/jcardsim` at `b6218da07a41875cdbfe4cde584e5281b0104fc6` |
| GidsApplet | GPL-3.0-or-later source headers, COPYING text | `GidsApplet` at `5851dbc612b369eaa712db4621748f1f9cd24414` |
| IsoApplet | GPL-3.0-or-later source headers and LICENSE | Source-only snapshot at `dad643e0d8fb9c2d2ad9f5eaa3793ea9aee36b90` |
| PivApplet | MPL-2.0 source headers, no secondary-license incompatibility notice found in retained Java files | Source-only snapshot at `eea6dc9d101cfde27abb4ce068bc5260d6f8f658` |
| Yubico OpenPGP | GPL-2.0-or-later source headers (the later-version permission matters) | `ykneo-openpgp` at `289cb007799d0dafce7a8ba9653aa0a2aa4f651d` |
| Yubico OATH | GPL-3.0-or-later source headers | `ykneo-oath` at `378c634ef9efbe80eb5deb11cfc0105271a44c3c` |
| 66 unique Maven runtime components across both Android apps | Phoenix has 66, remote-reader 65 (a subset); versioned POM licenses including inherited licenses are Apache-2.0 except Bouncy Castle's MIT-style license | [Exact inventories and evidence](LICENSES/README.md) |

All external paths in the table are under `ACardEmulator/app/src/main/external/`.
The source-only snapshots' `UPSTREAM.md` files record upstream URLs and commits.
Their retained Java files are byte-for-byte unchanged from those commits.

The machine-readable [source-component evidence](LICENSES/source-components.json)
records license declarations, evidence hashes and pinned submodule revisions.
New fork tooling and tests carry GPL-3.0-or-later SPDX identifiers.

Phoenix Cloud's offline license dialog now includes the full GPL-3.0, Apache-2.0 and
MPL-2.0 texts, the Bouncy Castle 1.46 license, original jCardSim NOTICE, source
availability URL, the runtime component list and applet copyright notices.
The separate Remote Smart Card Reader APK also bundles its complete GPL-3.0
and Apache-2.0 texts and all 65 runtime dependency notices.
The PIV source remains available under its original MPL-2.0 terms; the app-wide
GPL notice does not erase these source-file obligations. Recipients of binaries
must be provided corresponding source and applicable license/NOTICE information.

References:
https://www.apache.org/licenses/LICENSE-2.0
https://www.mozilla.org/en-US/MPL/2.0/
https://www.gnu.org/licenses/gpl-faq.en.html

## Other retained components

`virtualsmartcard`, `pcsc-relay`, `ccid` and `remote-reader` retain their original
GPL notices; OpenSC retains LGPL-2.1-or-later and its original per-file exceptions
and notices. Their existing licenses are not replaced by the Android inventory.
The `npa` directory now only points to its successor in OpenSC. The Gradle
wrapper remains build tooling under Gradle's Apache-2.0 license.

## Fonts, artwork and server

No font files are shipped by this fork. The Android UI selects system `sans`
and diagnostic `monospace`; the app does not redistribute the device's fonts.
The owner explicitly confirmed rights to the Phoenix logo and requested
code-only cleanup. No logo, screenshot or other artwork was changed or relicensed.
The typeface drawn inside a logo is part of that artwork and cannot be identified
from the app's system font settings.

The separate Phoenix server's source/license and hosted access requirements
remain outside this repository review. `NonFreeNet` remains conservatively
present in the disabled F-Droid draft. A free Android client does not establish
that the external service is free software.

## Verification and limits

- Both Phoenix `fdroid` and legacy `full` release APKs, plus the separate Remote
  Smart Card Reader release APK, build successfully.
- 12 Phoenix release JVM tests pass; instrumentation test APK compiles.
- The notice synchronization/evidence tests pass. A deliberately incomplete
  inventory in a temporary copy is rejected by the Gradle license guard.
- Android Lint: zero errors, 88 warnings for Phoenix and 3 warnings for Remote
  Smart Card Reader. Existing SDK/plugin/Java deprecation
  warnings remain; no toolchain or cryptographic-library upgrade is included.
- APK checks confirm the offline notices and absence of Samsung SDK classes and
  Samsung manifest declarations. Official fdroidserver 2.4.5 APK scanning of
  all three release APKs and source scanning after recipe cleanup pass.
- TesterArmy e2e on the connected Android test phone opens the offline dialog
  and confirms GPL, Apache, MPL, Bouncy Castle and source URL content. No model,
  account changes, cloud writes or NFC/hardware tests are part of this check.
- The device APK was built in a fresh temporary output directory because the
  existing debug output contained duplicate synchronized `.class` files. No
  installed application data was deleted for the test.
- Windows CLI CI is prepared but not locally executed on macOS. Physical NFC
  regression and an isolated F-Droid build-server VM are not covered here.

Cleanup removes files from the current branch tip, **not from Git history,
other branches, existing release artifacts or third-party submodule servers**.
No history rewriting, force push or deletion of published releases is performed.
Past distribution permissions and independent verification of every contributor's
original authorship cannot be established by inspecting license files or scanners.
