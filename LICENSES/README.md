# Android code license evidence

- `android-runtime.json` lists all 66 unique Maven runtime components resolved
  for both Phoenix Android release flavors, including transitive dependencies.
  `remote-reader-runtime.json` covers the second Android app's 65 runtime
  components, all Apache-2.0 and a subset of the same 66 unique components. Licenses
  were read from their versioned POMs; ZXing core and Guava ListenableFuture
  inherit Apache-2.0 from their parent POMs. Bouncy Castle 1.46 uses its own
  MIT-style license. No runtime component in that resolved set had an unknown
  license after resolving the parents.
- `Apache-2.0.txt` and `MPL-2.0.txt` are unmodified official license texts from
  https://www.apache.org/licenses/LICENSE-2.0.txt and
  https://www.mozilla.org/media/MPL/2.0/index.815ca599c9df.txt.
- `Bouncy-Castle-1.46.txt` is the exact license printed by
  `org.bouncycastle.LICENSE` in the resolved `bcprov-jdk14:1.46` JAR. Its
  copyright years differ from the older notice bundled with jCardSim; both
  notices are retained.
- `jCardSim-NOTICE.txt` is the unchanged NOTICE from jCardSim commit
  `b6218da07a41875cdbfe4cde584e5281b0104fc6`.
- GPL-3.0 is in `ACardEmulator/COPYING` and remains bundled as
  `app/src/main/res/raw/gpl_3.txt`. Original applet source headers and existing
  license files remain in their respective source directories.

`source-components.json` records the retained source components, license evidence
paths, SHA-256 hashes of that evidence, and pinned submodule revisions.

`python3 tools/update_android_notices.py` regenerates both offline notice assets
from this reviewed inventory, source attributions and license texts. Phoenix Cloud
shows its notices with the GPL text, and `tools/check_fdroid_apk.py` verifies the APK
contains it. `verifyRuntimeLicenses` compares each app's resolved release dependency
set to its inventory and fails the Android build on any drift. A version or
library change requires reviewing its own POM/source license, updating the
inventory and regenerating the notice asset; editing the inventory alone is
not a license review.

POM repository locations:

- AndroidX: https://dl.google.com/dl/android/maven2/
- Material: https://dl.google.com/dl/android/maven2/com/google/android/material/material/1.12.0/material-1.12.0.pom
- Other Maven components: https://repo.maven.apache.org/maven2/

This inventory covers shipped Android runtime code. Test/build tools and other
vsmartcard components retain their own licenses; this does not relicense them.
