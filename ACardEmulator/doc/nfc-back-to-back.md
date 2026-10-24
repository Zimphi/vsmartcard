# NFC back-to-back regression

Use two unlocked phones with NFC enabled, held back-to-back. Start a synthetic
local eID or ePassport simulation in Phoenix Android on the card phone. Keep that
app in the foreground. The reader phone needs the corrected Phoenix Cloud debug
APK; it needs no cloud pairing for this local test.

From the ePassport workspace root:

```sh
PHOENIX_NFC_READER='<reader-adb-serial>' PHOENIX_NFC_PROFILE=epassport \
  npm run test:e2e -- --config e2e.nfc-back-to-back.config.ts
```

Repeat with `PHOENIX_NFC_PROFILE=eid` after stopping the card simulation, selecting
an eID profile and starting it again. Use the reader serial listed by `adb devices`
for `PHOENIX_NFC_READER`. ADB must be on PATH.

The dedicated hardware suite performs three consecutive tests of ten probe loops
without repositioning the phones or manually toggling NFC. Each loop selects the
application, MF and EF.CardAccess, then reads data. It asserts the completed test
status and logs per-run APDU counts and latency. Hardware tests stay outside the
default frontend/UI suite.

## Failure fixed on 2026-10-22

The original application SELECT used P2=00 with no Le. The ePassport returned
`6110`, asking the reader to retrieve FCI. The probe expected a completed `9000`
response and incorrectly failed at the first command. The probe now uses P2=0C
(no FCI), as the existing desktop reader does.

READ BINARY requests up to 256 bytes. A shorter file can return its remaining data
with `6282` (EOF). The probe now accepts this status only for READ BINARY with
nonempty data. Empty reads, continuation responses and other errors remain failures.
Unit regressions cover both cases and fail before their respective fixes.

The diagnostic screen also disables reader mode when a test finishes or is
stopped. Starting another test consequently opens a fresh NFC discovery session,
even when the phones have stayed in contact.

These checks prove ISO-DEP application selection and CardAccess access. They do
not prove PACE/EAC authentication, complete document conformance, long-term RF
stability or the reverse reader/card arrangement.

For the distinct protected-SELECT firmware observation and the full two-phone
document-session checks, see [Samsung protected SELECT](samsung-protected-select.md).
