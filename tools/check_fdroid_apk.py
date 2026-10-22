"""Check the built F-Droid APK, including classes pulled in transitively."""
import re
import subprocess
import sys
import zipfile


def check(apk, aapt2):
    manifest = subprocess.check_output(
        [aapt2, 'dump', 'xmltree', '--file', 'AndroidManifest.xml', apk], text=True)
    package = subprocess.check_output([aapt2, 'dump', 'packagename', apk], text=True).strip()
    assert package == 'com.vsmartcard.acardemulator.phoenixcloud.fdroid', package
    assert 'com.samsung' not in manifest, 'Samsung components or permissions in manifest'
    badging = subprocess.check_output([aapt2, 'dump', 'badging', apk], text=True)
    assert 'application-debuggable' not in badging, 'Debug APK'
    with zipfile.ZipFile(apk) as archive:
        dex = [name for name in archive.namelist() if re.fullmatch(r'classes\d*\.dex', name)]
        assert dex, 'APK has no DEX code'
        for name in dex:
            assert b'Lcom/samsung/android/sdk/' not in archive.read(name), 'Samsung SDK in ' + name
    print('F-Droid APK checks passed:', package)


if __name__ == '__main__':
    check(*sys.argv[1:])
