# SPDX-License-Identifier: GPL-3.0-or-later
"""Assemble offline app notices from the reviewed inventory and original licenses."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def notices(project):
    phoenix = project == "ACardEmulator"
    inventory_file = "android-runtime.json" if phoenix else "remote-reader-runtime.json"
    inventory = json.loads((ROOT / 'LICENSES' / inventory_file).read_text())
    assert len({item['coordinate'] for item in inventory}) == len(inventory)
    parts = [
        ('Phoenix Cloud' if phoenix else 'Remote Smart Card Reader') + ' — third-party licenses and source information',
        'The application is GPL-3.0-or-later. Original copyrights remain with their holders.\n'
        'Complete app and included applet source: https://github.com/Zimphi/vsmartcard\n'
        'Branch: codex/fdroid-preparation. Preserve source and license notices when redistributing.\n'
        'Resolved Android release runtime components (transitive dependencies included):\n' +
        '\n'.join(item['coordinate'] + ' — ' + item['license'] for item in inventory),
    ]
    external = ROOT / 'ACardEmulator/app/src/main/external'
    applets = [
        ('jcardsim', 'Apache-2.0', 'src/main/java'),
        ('GidsApplet', 'GPL-3.0-or-later', 'src'),
        ('IsoApplet', 'GPL-3.0-or-later', 'src'),
        ('PivApplet', 'MPL-2.0', 'src'),
        ('ykneo-openpgp', 'GPL-2.0-or-later', 'applet/src'),
        ('ykneo-oath', 'GPL-3.0-or-later', 'applet/src'),
    ] if phoenix else []
    for name, license, path in applets:
        copyrights = sorted({line.strip().removeprefix('*').strip()
                             for source in (external / name / path).rglob('*.java')
                             for line in source.read_text().splitlines()
                             if 'copyright' in line.lower() and 'Copyright' in line})
        assert copyrights, name
        parts.append(name + ' — ' + license + '\n' + '\n'.join(copyrights))
    files = ['Apache-2.0.txt']
    if phoenix:
        parts.append('PIV source remains MPL-2.0; see its original headers and license.')
        files += ['MPL-2.0.txt', 'Bouncy-Castle-1.46.txt', 'jCardSim-NOTICE.txt']
    else:
        parts.append((ROOT / 'remote-reader/COPYING').read_text())
    for file in files:
        parts.append(file + '\n\n' + (ROOT / 'LICENSES' / file).read_text())
    return '\n\n' + '\n\n'.join(parts) + '\n'


if __name__ == '__main__':
    for project in ['ACardEmulator', 'remote-reader']:
        output = ROOT / project / 'app/src/main/assets/third_party_notices.txt'
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(notices(project))
        print(output)
