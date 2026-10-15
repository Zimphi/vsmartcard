# Phoenix Cloud 4.6 verification

Implemented: separate diagnostics/developer log, paired owner, German/English account language, contextual illustrated help, measured NFC quality/antenna map, guided positioning, structured tests, ADB laboratory companion and explicit compatibility confirmation.

Passed: 8 JVM tests, 16 device tests on Samsung SM-S918B/Android 16, Android lint without errors, 57 laboratory Python tests, 29 Rust tests, 38 GUI Node tests and Chrome checks at four widths. Four relevant SQL rollback suites pass. Server Python: 39 passed, 21 private native fixture tests skipped; one Rust private-fixture test remains ignored.

Preview and Production are deployed. Paired-account display, account language EN → DE, actual active profile display and access controls were checked live. eID native cloud PACE/EAC passes; npa-04 also passes RI/PS. Compatibility storage remains empty: no real report was submitted without the user's manual confirmation.

Physical tests were executed through the app, but PC/SC reports 8010000C (no NFC target) before any APDU, including with a valid selected profile and fresh NFC session. Original pairing was restored and verified. Reader mode has no confirmed physical test card. These checks are incomplete, not passed.

The legacy deutscher-reisepass-eac fixture separately fails TA validity; the corresponding source fixture's terminal certificate expired on 2026-08-29. Certificate validation was not relaxed.

Full redacted evidence: Phoenix-NFC-Device-Tests/docs/diagnostics-release-2026-10-15.json. Model/OS/app and firmware context are retained privately per test run. The app shows bounded recent metrics; the full history stays in the host recordings. No RF RSSI or field strength is claimed.
