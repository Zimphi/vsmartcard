# Android Smart Card Emulator

[F-Droid preparation and local validation](../fdroid/README.md) (not submitted).

## Aktuelle Phoenix-Laborversion: 4.10.1

[Änderungen, Installation und bekannte Grenzen](releases/4.10.1.md).
Die APK ist ein signierter Debug-Build für autorisierte Labor-/ADB-Tests;
Quellarchiv und SHA-256-Prüfsummen werden beim GitHub-Release bereitgestellt.


## Phoenix Cloud · 4.7-phoenix

The Phoenix screen displays the centered Phoenix logo and the currently paired
simulator. A green check marks a fresh, successful cloud contact; a red cross
marks an inactive connection. The NFC mode choices have their own heading.
Cloud/phone vector icons and a single contextual button switch between
“Verbinden” and “Verbindung aufheben”. Disconnecting preserves the saved pairing.
QR enrollment and JSON configuration import live on a separate “Gerät koppeln”
page, reached through “Kopplung verwalten”. The main page displays the installed
version. The launcher opens Phoenix Cloud directly and uses a Phoenix adaptive
icon; all pages and their dialogs share the same theme. Tapping the Phoenix
logo opens an information page with the installed version, live connection
status, paired simulator and an HTTPS link to epassport-simulator.de.

Connection freshness has a JVM regression test. Device tests verify both screen
states; the connected Android phone also passed real connect, disconnect and
reconnect checks without enrolling again.

## 4.7 · 2026-10-15

Heartbeat HTTP requests no longer hold the card-session monitor while waiting
for the network. APDUs remain strictly ordered, with unchanged revision checks
and no retry of uncertain commands. A heartbeat response is applied only if the
captured session object and opened ID are still current; an older status response
cannot reset or overwrite a newer card session. Two device regression tests
reproduce the former blocking and verify the concurrency safeguards.

This removes a measured avoidable dispatch delay. Physical NFC stability still
requires a separate soak test; it does not prove that all RF/reader drops are fixed.

## 4.6 · 2026-10-15

Diagnostics, developer settings and the colored live log now have their own
screen. The information page shows the paired account. German/English can be
chosen on the home page and synchronize with the paired user's cloud preference.
The question-mark button opens contextual setup steps with actual localized app
screenshots. The existing GPL notices and full offline license remain available.

NFC diagnostics show device capabilities, optional Android 14+ manufacturer
antenna coordinates, separate APDU latency/reliability graphs and a guided nine
position comparison. Android does not expose RF strength/RSSI: the graph measures
response times and failures; cloud latency includes network and simulator work.
No recommendation is given without at least five observations per position (APDUs or measured failed NFC activations). Latency statistics use successful APDUs only.

Reader tests use SELECT/READ only on synthetic ISO-DEP test cards. The laboratory
Mac companion consumes explicitly started private ADB test requests and runs the
existing local USB profile test or cloud PACE/EAC test. A baseline alignment run
is clearly distinguished from full protocol verification. Start/Stop, prerequisites
and data handling are explained before testing. See the companion setup in
[Phoenix-NFC-Device-Tests](https://github.com/Zimphi/Phoenix-NFC-Device-Tests).

A successful full test enables an explicit confirmation dialog before saving
model/OS/app-version/mode evidence to the owner-bound compatibility list. Nothing
is submitted automatically; baseline alignment does not qualify. No serial,
IMEI, key, pairing token or APDU payload is included in this report.

## 4.5 · 2026-10-14

Cloud contact, NFC readiness and APDU/session activity are displayed separately.
Selected profile metadata comes from the token-protected device status response;
older servers explicitly show that profile details are unavailable. Reader mode
shows card detection without claiming the card profile has been identified.
The information page has an opt-in “Entwickleroptionen” checkbox for a tagged,
colored live log on the main page (120 in-memory entries; no credentials or APDU
payloads). NFC activity ends on deactivation or disconnect; late responses cannot
restore an ended session. The profile metadata API change is prepared locally;
no Vercel deployment is performed as part of this Android UI update.

## 4.4 · 2026-10-14

The NFC mode selector now uses two side-by-side tiles, “Karte lesen” and
“Karte simulieren”, with matching vector icons and an explicit selected border.
Native radio-button semantics and the existing mode-switch behavior are preserved.

## 4.3.1 · 2026-10-14

The information page now explains Phoenix and browser simulation, identifies
this independently developed fork and links to both source repositories.
ACardEmulator remains GPL-3.0-or-later, as stated in the upstream source headers.
The original, unmodified [GPL text](COPYING) is bundled in the APK and can be
read offline from the information page. Existing copyright notices and
third-party licenses remain in place.

## Upstream emulator

The Android Smart Card Emulator allows the emulation of a contact-less smart card.
The emulator uses Android's HCE to fetch APDUs from a contact-less reader.
The app allows to process the Command APDUs either by delegating them to a
remote virtual smart card or by a built-in Java Card simulator. The response
APDUs are then returned to the smart card reader.

With the built-in Java Card runtime of [jCardSim](http://www.jcardsim.org/) the
app includes the following Applets:

- [OpenPGP Applet](https://developers.yubico.com/ykneo-openpgp/) (application identifier ``D2760001240102000000000000010000``)
- [OATH Applet](https://developers.yubico.com/ykneo-oath/) (application identifier ``A000000527210101``)
- [ISO Applet](http://www.pwendland.net/IsoApplet/) (application identifier ``F276A288BCFBA69D34F31001``)
- [GIDS Applet](https://github.com/vletoux/GidsApplet) (application identifier ``A000000397425446590201``)

The remote interface can be used together with the [Virtual Smart
Card](http://frankmorgner.github.io/vsmartcard/virtualsmartcard/README.html),
which allows emulating the following cards:

- Generic ISO-7816 smart card
- German electronic identity card (nPA)
- Electronic passport

Additionally [PersoSim](https://persosim.de) can be used to emulate a german electronic identity card.

Please refer to [our project's website](http://frankmorgner.github.io/vsmartcard/ACardEmulator/README.html) for more information.


### Phoenix Cloud 4.8: NFC polling gain

Android 15+ polling-frame callbacks now feed a separate vendor-gain chart
(0–255) in diagnostics. Missing readings (`-1`) leave gaps; the latest reading
has an age indicator. The graph retains at most 60 of 200 in-memory frames.
No polling-frame bodies, card data or credentials are recorded or uploaded.
The API is passive: this change does not enable Observe Mode, change the wallet
role, install polling filters or alter APDU processing. Compile SDK is 35;
target SDK and minimum SDK remain unchanged.

The 3×3 position guide also records gain median/range/count over each 30-second
measurement window. A position needs at least five transfer observations or
five valid gain readings. Gain-only windows can be compared visually but never
produce a best-position recommendation: vendor scales are not calibrated and
do not define a universal stronger/weaker order. Transfer reliability remains
the basis of recommendations. Gain is unavailable through Android IsoDep in
reader mode, so the existing transfer tests remain the fallback.

Physical test on Samsung SM-S918B / Android 16, 18 October 2026: the device
reports `isObserveModeSupported() == false`. A 60-second observation with the
Phoenix HCE service preferred and a real REINER-SCT NFC SELECT returned zero
polling frames and zero gain values. The SELECT status was `6985` without an
active CloudService; this verifies HCE reachability, not PACE/EAC. No synthetic
samples were injected in the physical observation. Gain availability on other
firmware/devices remains to be tested.

Validation: 11 JVM tests, 19 device tests, one separate physical observation;
Android lint has zero errors. `NfcGainProbeTest` records its bounded, token-free
result to the app cache as `nfc-gain-probe.json` and reports missing measurements
explicitly instead of treating Android API level as evidence of support.
Official references:
[Android HCE polling](https://developer.android.com/develop/connectivity/nfc/hce#polling_loop_filters),
[PollingFrame gain](https://developer.android.com/reference/android/nfc/cardemulation/PollingFrame#getVendorSpecificGain()).

A follow-up physical activation test on the same Samsung (18 October 2026)
called `setObserveModeEnabled(true)` with Phoenix as the preferred foreground
HCE service. It returned `false`; Observe remained disabled and the original
state was verified. The device firmware sets both
`ro.nfc.observe_mode_supported` and `ro.nfc.get_caps_supported` to `false`.
The test writes `nfc-observe-activation.json` to app cache.
ADB NFC commands, Samsung NFC dumps, the available NFC process log and vendor
configuration exposed no gain/RSSI/AGC readings. The REINER-SCT standard PC/SC
feature query exposed only GET_TLV_PROPERTIES, which supplied no RF gain
measurement. No private controller commands, root or firmware modifications
were attempted. These results describe the installed firmware, not proof of
physical controller incapability. Android requires controller/HAL support for
Observe and polling notifications; an app permission or UI switch cannot add it.
See [Android NCI capabilities](https://source.android.com/docs/core/connect/android-proprietary-nci-commands).

### Phoenix Cloud 4.9: simpler home screen

The home screen shows one compact cloud/phone connection icon with an explicit
connected, saved/offline or unpaired label. The server URL and separate NFC
readiness, communication and mode headings are removed. Read/simulate tiles,
active profile, connection action, pairing management and diagnostics remain
directly accessible. Disabled or missing NFC still produces an actionable
notice, rather than silently appearing ready. Pairing credentials and protocol
handling are unchanged.

### Phoenix Cloud 4.10.0: pairing management and diagnostics navigation

Manage pairing now opens only the saved pairing, connection status and a
confirmed local delete action. Deleting stops CloudService and removes the
encrypted local credentials; it does not revoke the server-side device entry.
The confirmation explains this and directs users to remove that entry in the
online simulator separately. QR/JSON setup remains available when connecting
an unpaired phone or through setup help. A link icon replaces the large logo
on both pairing pages. NFC diagnostics are accessible from the version/about
page only, with no diagnostics button on the home screen. The displayed and
built app version uses three components.
