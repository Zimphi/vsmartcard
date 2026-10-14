# Android Smart Card Emulator

## Phoenix Cloud · 4.2-phoenix

The Phoenix screen displays the centered Phoenix logo and the currently paired
simulator. A green check marks a fresh, successful cloud contact; a red cross
marks an inactive connection. The NFC mode choices have their own heading.
Cloud/phone vector icons and a single contextual button switch between
“Verbinden” and “Verbindung aufheben”. Disconnecting preserves the saved pairing.
QR enrollment and JSON configuration import live on a separate “Gerät koppeln”
page, reached through “Kopplung verwalten”. The main page displays the installed
version. The launcher opens Phoenix Cloud directly and uses a Phoenix adaptive
icon; both pages and their dialogs share the same theme.

Connection freshness has a JVM regression test. Device tests verify both screen
states; the connected Android phone also passed real connect, disconnect and
reconnect checks without enrolling again.

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
