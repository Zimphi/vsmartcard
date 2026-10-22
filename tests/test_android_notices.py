# SPDX-License-Identifier: GPL-3.0-or-later
"""Keep reviewed runtime licenses and the distributed offline notices in sync."""
import hashlib
import json
from pathlib import Path
import runpy
import unittest

ROOT = Path(__file__).resolve().parents[1]
GENERATE = runpy.run_path(str(ROOT / 'tools/update_android_notices.py'))['notices']


class AndroidNoticesTest(unittest.TestCase):
    def test_source_evidence_and_removed_restricted_components(self):
        components = json.loads((ROOT / 'LICENSES/source-components.json').read_text())
        for component in components:
            evidence = ROOT / component['evidence']
            self.assertEqual(hashlib.sha256(evidence.read_bytes()).hexdigest(),
                             component['evidence_sha256'], component['path'])
        for removed in ['ACardEmulator/app/libs', 'ACardEmulator/app/src/full',
                        'virtualsmartcard/win32', 'eID_gui/src', 'eID_gui/configure.ac',
                        'ACardEmulator/app/src/main/external/IsoApplet/ext',
                        'ACardEmulator/app/src/main/external/PivApplet/ext']:
            self.assertFalse((ROOT / removed).exists(), removed)
        modules = (ROOT / '.gitmodules').read_text()
        for removed in ['DevMsi', 'IsoApplet', 'PivApplet']:
            self.assertNotIn(removed, modules)

    def test_every_reviewed_runtime_component_is_in_the_distributed_notices(self):
        for project, inventory_file in [('ACardEmulator', 'android-runtime.json'),
                                        ('remote-reader', 'remote-reader-runtime.json')]:
            with self.subTest(project=project):
                expected = GENERATE(project)
                asset = ROOT / project / 'app/src/main/assets/third_party_notices.txt'
                self.assertEqual(asset.read_text(), expected, 'Regenerate the offline notices')
                inventory = json.loads((ROOT / 'LICENSES' / inventory_file).read_text())
                self.assertGreater(len(inventory), 0)
                for component in inventory:
                    self.assertIn(component['coordinate'] + ' — ' + component['license'], expected)
                    self.assertIn(component['license'], {'Apache-2.0', 'MIT'})
                self.assertIn('Apache License', expected)
                self.assertIn('https://github.com/Zimphi/vsmartcard', expected)
        self.assertIn('Mozilla Public License', GENERATE('ACardEmulator'))
        self.assertIn('Bouncy Castle', GENERATE('ACardEmulator'))
        self.assertIn('GNU GENERAL PUBLIC LICENSE', GENERATE('remote-reader'))


if __name__ == '__main__':
    unittest.main()
