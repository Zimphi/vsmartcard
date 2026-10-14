# Phoenix Cloud on Android

This fork adds an outgoing HTTPS connection to Phoenix. The application ID is
`com.vsmartcard.acardemulator.phoenixcloud.fdroid`; upstream apps can stay installed.
The foreground Phoenix screen selects its own eID/ePassport HCE service to avoid
routing the test into an older app with the same AIDs.

In Phoenix Preview, open **Einstellungen → Smartphone und vsmartcard → Gerät neu
einrichten**. Choose a QR code or **JSON-Datei herunterladen**. For JSON, transfer
`phoenix-phone.json` to the phone's Downloads folder (USB is sufficient), then open
**Phoenix Cloud → JSON-Datei importieren**, select the file and confirm the server.
No camera permission is needed for JSON import.

The file format is bounded to 2048 bytes and has exactly these fields:

```json
{
  "format": "phoenix-phone-pairing-v1",
  "server": "https://epassport-simulator.de",
  "pairing_code": "ONE_TIME_CODE_FROM_PHOENIX"
}
```

The example is deliberately not a valid code. Phoenix generates 32 random bytes
per code. Enrollment expires after ten minutes and is redeemed once. Downloading a
new configuration replaces outstanding enrollment without disconnecting paired
devices. Keep the file private until import and delete it afterward. The durable
device token is returned only after redemption and encrypted with Android Keystore;
it is never written to the JSON export.

Choose **Karte simulieren** to expose the profile selected in the online gateway
through NFC. Choose **Echte NFC-Karte lesen** to use the phone as a reader, keeping
this screen open. The existing reader gateway supports one active reader per
account. Paired cards keep separate session IDs. Profile changes are observed by
the five-second heartbeat and discard stale local card checkpoints.

The public device API requires a one-time code or a scoped device token. The browser
simulator stays in protected Preview. No Vercel account credential is embedded in
this Android app. Deleting a device in Phoenix revokes its future requests.

Build/test with the upstream Gradle wrapper:

```sh
./gradlew testFdroidDebugUnitTest assembleFdroidDebug connectedFdroidDebugAndroidTest lintFdroidDebug
```

Device tests cover encrypted credential storage, JSON size/HTTPS/field validation,
card revisions, profile changes and uncertain APDUs without automatic retry.
The external `Phoenix-NFC-Device-Tests/cloud_phone_test.py` records real enrollment,
NFC CardAccess reads and offline/reconnection. CardAccess reads are a transport
smoke test, not complete PACE/EAC or conformance verification.
