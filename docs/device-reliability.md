# Device reliability — authorized scope (2026-10-20)

User requests all five proposed capabilities. Preserve existing incomplete plans.
No Vercel build, deployment or hosted SQL changes.

| Module | Responsibility | Build order/dependency |
|---|---|---|
| revoke-device | Revoke this token on server before deleting the local vault | 1 / existing device API |
| reconnect | Bounded retries, network recovery, discard uncertain sessions | 2 / CloudApi |
| compatibility-evidence | Exact mode/kind/check/build/date, owner-bound readback | 3 / reports and companion |
| diagnostic-export | Explicit local JSON export of allowlisted metadata | 4 / evidence |
| guided-test | Cloud → prerequisites → reading → simulation, exact results | 5 / all above |

## Contracts and acceptance

- Revoke: POST `/api/vsmartcard/disconnect`, empty JSON, Bearer token. Revoke only
  this grant. Success/already-denied clears vault; network/server failure retains
  credentials and explains retry. Never claim local deletion revoked server access.
- Reconnect: bounded exponential retry and usable-network wakeup, reset on success.
  Disconnect immediately, detach reader, invalidate card state. Never replay APDUs;
  failed NFC sessions need reactivation. Explicit Stop remains stopped.
- Evidence: old reports remain limited legacy evidence. New reports include kind,
  actual protocol checks, firmware/security patch and test time. Separate reader
  SELECT/CardAccess from simulation PACE/EAC/RI/PS. No global certification label.
  Every write requires explicit confirmation. Readback matches owner and exact build.
- Export: Android document picker, bounded JSON device/build/check results, timings,
  counters, gain availability and fixed event names. No tokens, URLs, account names,
  card/profile data, APDU bodies, PIN/CAN/PUK or freeform/exception messages.
- Wizard: under diagnostics on version page, DE/EN, physical card and USB/ADB/REINER
  prerequisites. Reuse reader and lab tests, distinguish untested/running/fail/pass.
  Missing hardware/checks never pass. Show exact kind/build/protocol coverage;
  support cancellation and rerun. Existing main screen stays simple.

## Implementation and checks

Reuse Android Java `CloudScreen`, `CloudApi`, `LabControl`, styles/resources;
Rust `Phoenix-Vercel/src`, migrations/tests `supabase`, Python lab companion.
Extend JSON additively. Follow existing style, strict boundary validation and
owner isolation. Every behavior gets focused tests before implementation.

Checks: Rust simulator tests, `npm ci --prefix Phoenix-Vercel/tests/phone_contract`
and `npm test --prefix Phoenix-Vercel/tests/phone_contract`; Python companion tests;
Gradle `testFdroidDebugUnitTest lintFdroidDebug assembleFdroidDebug
assembleFdroidDebugAndroidTest`; TesterArmy mobile deterministic UI tests through
root `npm run test:e2e` with mobile configuration. Local debug compilation only.
Test real phone when connected; report physical-card/NFC limits honestly. Review,
local commits and AST graph update after implementation. No real whitelist upload.

## Accepted navigation refinements

Version page: “Diagnosedaten exportieren” near the version, direct “NFC-Test”
above “Entwickleroptionen”. Export moved out of developer diagnostics. Both
languages follow this structure; JSON remains the file format. Rotation preserves
pending document-picker export data, bounded to 64 KiB.
