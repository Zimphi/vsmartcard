# Samsung protected SELECT: diagnostic boundary

The local PhoneReader probe and the Samsung protected-SELECT defect are different
checks. The plaintext probe selects with P2=0C and accepts a nonempty READ BINARY
response ending in 6282. Passing that probe proves neither PACE/EAC nor delivery
of a protected SELECT to Android HCE.

Forward APDUs byte-for-byte. In particular, do not normalize the protected CLA to
00 or rewrite SELECT parameters as a firmware workaround: this changes the
authenticated command and can invalidate secure messaging.

The calibrated original Galaxy S21 library observation is scoped to SM-G991B,
build G991BXXSJHZC2, `/system/lib64/libnfc_nxpsn_jni.so`, SHA-256
`c31d12dbb8359da86292b6add372f529ea2fd9664efce2492a60ba493f436445`.
The locally patched counterpart has SHA-256
`3ea4de76d589620ada64ebe9e0a3b9e2c15fd19ffba410e8b515d3d07dcf3482`.
Neither hash identifies all Samsung models or firmware versions.

Phoenix Android now provides a separate guided two-phone assistant, combining
complete licensed ePassport/eID sessions in both NFC directions with an additional
authenticated synthetic protected-SELECT transport probe. Compare reader and HCE
evidence and both phones' firmware/library metadata before attributing a failure.
An RF interruption alone is not a Samsung diagnosis. Passing on an already patched
phone does not establish that its original firmware needed no patch.

See the sibling Phoenix-Android project's `docs/nfc-test-assistant.md` and the
workspace `reports/nfc/2026-10-24-android-nfc-*.json` evidence. No universal flashing
function or protected-command rewriting is introduced into vsmartcard.
