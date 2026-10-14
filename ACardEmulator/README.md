# Android Smart Card Emulator

## Phoenix Cloud · 4.6-phoenix

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
