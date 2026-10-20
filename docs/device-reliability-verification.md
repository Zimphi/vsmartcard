# Phoenix Cloud 4.11.0 — local verification, 2026-10-20

Implemented the five requested capabilities and the subsequent navigation changes:

- Server self-revocation before local credential removal, with honest offline
  failure. Only explicit device-denial JSON counts as an already-revoked token;
  generic proxy/gateway 401/403 retains credentials. Compare-and-clear prevents
  late replies from deleting a newer pairing. Vault operations share one lock.
- Retry at 1/2/4/8/15 seconds, usable-network wakeup, immediate offline status,
  discard opaque card state and detached readers. APDUs are never replayed.
- Guided tests reuse the existing reader and laboratory paths, separate eID and
  ePassport, and distinguish prerequisites, untested, running, failed and passed.
  Reading is SELECT/nonempty EF.CardAccess, not authentication. Card evidence
  requires actual PACE/SM/PA/CA/TA checks; RI/PS are listed only when recorded.
- Explicit document-picker export of bounded allowlisted metadata. No account,
  credential, profile/document content, APDU body or arbitrary log text. Pending
  export survives activity recreation. Label: “Diagnosedaten exportieren”.
- Exact app/Android/firmware/security-patch and mode/kind/protocol evidence,
  timestamps, immutable per-test identifiers, user confirmation and owner-bound
  cloud readback. Old reports remain limited legacy evidence. Local history is
  limited to 20 records; re-running clears only the selected current context.

Version page: export near the version, then direct “NFC-Test” above
“Entwickleroptionen”. Home remains unchanged. Both languages verified.

## Results

- Debug app and instrumentation APK built locally; Android lint: no errors.
- JVM: 12 tests, no failures/skips.
- Device instrumentation: 27 tests passed on connected Samsung; extra test-isolation
  check passed. Test-generated observations were backed up/removed and fixtures now
  restore diagnostic history. Original pairing and language restored.
- TesterArmy: deterministic navigation/export-disclosure test passed in DE/EN,
  with the direct version-page NFC link and developer entry. No model used.
- Actual Android CloudApi and foreground CloudService over HTTPS/ADB reverse to
  real local Rust handlers + migrated in-memory PostgreSQL passed. Exercised a
  controlled server outage and automatic recovery, both heartbeat modes, metadata,
  languages, evidence/readback/build mismatch, consent, duplicate reports, device
  self-revocation, already-denied retries and generic-gateway-denial rejection.
- Backend: 26 Rust tests passed; 2 explicitly opt-in fixtures ignored in unit run.
  Four actual SQL regression suites passed in disposable PostgreSQL. Python backend
  suite: 46 passed, 21 optional-environment tests skipped. Companion suite: 63 passed.
- Visual review: full version/test screenshots inspected; labels fit and the
  requested order is present. No source credentials in commits or exports.

No push, Vercel build/deployment, hosted SQL change or actual whitelist submission.
The installed APK is a local debug/laboratory build, not a published release.
Live-cloud activation awaits reconciliation/application of the prepared migrations
and deployment. RF, physical-card reading and cloud NFC PACE/EAC were not rerun;
no physical compatibility success is claimed by these software tests.

## Reproduce

From the parent Phoenix workspace (Android SDK/ADB authorized):

```sh
npm ci
PATH="$PWD/Phoenix-NFC-Device-Tests/.tools/platform-tools:$PATH" npm run test:e2e -- --config e2e.android.config.ts
PHONE_ADB="$PWD/Phoenix-NFC-Device-Tests/.tools/platform-tools/adb" npm test --prefix Phoenix-Vercel/tests/phone_contract
PYTHONPATH=Phoenix-NFC-Device-Tests python3 -m unittest discover -s Phoenix-NFC-Device-Tests/tests
```

Android framework references used for native integration:
[document picker](https://developer.android.com/training/data-storage/shared/documents-files),
[network callbacks](https://developer.android.com/develop/connectivity/network-ops/reading-network-state).
