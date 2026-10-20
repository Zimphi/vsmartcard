# Release checklist

- [x] Device account/language API + migration + Rust/SQL tests (migration applied; Preview and Production deployed).
- [x] Dedicated diagnostic window; remove developer data from home (16 device tests pass).
- [x] German/English resources and subtle language picker; account language round-trip and actual paired owner checked live.
- [x] Measured NFC quality model and sample windows + graph + guided positioning.
- [x] Structured reader/card NFC tester with explicit Start/Stop and prerequisites.
- [x] Authorized ADB Mac companion with safe request parsing + progress/results.
- [x] Confirmed compatibility API/table + disclosure/confirmation UI and tests.
- [x] Context-sensitive help steps with actual app screenshot illustrations.
- [x] Build/install Android; 8 JVM + 16 device tests, lint without errors, localized screenshots checked.
- [x] Migration, Production device API and protected final Preview deployed; live login/preferences/profile and access checks pass.
- [x] App-initiated USB/profile and cloud runs executed and recorded; original pairing restored.
- [ ] Successful physical NFC PACE/EAC remains blocked by no target (PC/SC 8010000C), even after valid profile selection and NFC restart.
- [x] Reader mode limitation recorded: physical synthetic card availability remains unconfirmed; no hardware pass claimed.
- [x] HTTP pooling measured and implemented; eID PACE/EAC and RI/PS rechecked on final cloud build.
- [ ] Legacy ePassport fixture TA fails because its embedded terminal credential is expired; validity validation is preserved.
- [x] Code and release evidence committed; final push verification follows.

## Device reliability (2026-10-20)

- [x] Server device revocation and honest offline failure; other devices survive.
- [x] Bounded reconnect/network wakeup; uncertain sessions discarded, no APDU replay.
- [x] Exact kind/checks/build/date compatibility; consent and owner-bound readback.
- [x] Disclosed SAF JSON export; planted secrets excluded and output bounded.
- [x] Localized guided cloud/NFC/reader/simulation tests and mobile UI tests.
- [x] Local build/lint/integration, device checks, review, commits and graph update.
