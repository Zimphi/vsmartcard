# Phoenix Cloud: device diagnostics and laboratory release

Deadline requested: 2026-10-15 08:00 Europe/Berlin (06:00 UTC).
User authorizes Android changes, real hardware tests, server migration and Vercel
build/deployment. No automatic compatibility submission: explicit in-app consent
before each submission is mandatory. Existing unrelated dirty files are preserved.

## Capability map and order

| Module | Responsibility | Depends on |
|---|---|---|
| device-account | Owner display and account language through existing pairing | existing device token and user_preferences |
| diagnostics-ui | Dedicated diagnostics screen, localized common UI | device-account |
| nfc-quality | Measured APDU timings/errors, graphs, guided position grid | diagnostics-ui |
| lab-control | App-authorized requests consumed by USB/ADB Mac companion | nfc-quality, existing lab_nfc_record.py |
| compatibility | User-confirmed device/mode results saved owner-bound in cloud | lab-control, device-account |
| setup-help | Contextual step-by-step help with app screenshots | localized UI |
| release-verification | Build, migrations, production device API and preview UI; hardware suite | all above |
| performance | Evidence-based latency work without changing protocol semantics | release-verification |

## Contracts

- Existing machine status gains account {display_name, language}; only owner of the
  paired device is returned. Account language uses existing public.user_preferences.
- POST device-preferences accepts {language:"de"|"en"}, owner derived from device
  token, never from caller-supplied owner. Invalid/revoked grants cannot write.
- POST compatibility accepts explicit consent plus model/manufacturer/OS/app build,
  confirmed mode and bounded test summary. No serial, IMEI, credentials, APDU body
  or real document data. Self-reported confirmation is labeled as such, not formal
  certification. Different NFC modes require separate evidence and confirmation.
- Mac companion: app writes bounded JSON request in private app cache on explicit
  Start; authorized USB/ADB companion consumes one UUID once, runs fixed allowlisted
  test modes using existing script, writes metadata progress/results atomically.
  No arbitrary shell/argv from phone. Debug lab APK required for adb run-as; this
  prerequisite is shown. Cloud pairing stays separate from local upstream test app.
- Quality is successful APDUs/attempts + latency distribution + interruption count;
  RF strength/RSSI is unavailable. A guided 3x3 position grid uses user-confirmed
  equal-duration samples. Arrows guide planned movement or return to best *measured*
  position, never claim a sensed magnetic direction. Too few samples => inconclusive.
- English/German via Android resources and per-app locale; preference saved locally
  offline, synchronized to paired account when authenticated. Strings with protocol
  identifiers remain unchanged. Main screen has question-mark help and subtle
  language control. Diagnostics and developer logs no longer render on main page.

## Verification

Per slice: focused JVM/Python/Rust tests, Android lint, device instrumentation and
own-view screenshots. Final: full existing suites; real card-mode/cloud PACE/EAC
and morning USB test if fixtures/app available; reader mode with an actual NFC
card if present. Missing physical card is recorded as blocked, never pass.
No real personal document data or compatibility submissions without user consent.
Deployment uses prepared temporary checkout, avoiding unrelated dirty source files.
Measure APDU latency before optimizing; keep certificate validation, protocol order,
secure messaging sequence counters, replay protection and owner isolation intact.
