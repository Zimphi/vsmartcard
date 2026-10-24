import { test } from '@e2e-dev/mobile';
import { expect } from 'e2e';

// Requires a second, unlocked phone already simulating the selected synthetic profile.
// No cloud account, PIN, personal data, or model calls; keep both phones back-to-back.
test('NFC SELECT/READ succeeds again without moving the phones', { timeout: 180_000 }, async ({ app, screen }) => {
  const kind = process.env.PHOENIX_NFC_PROFILE ?? 'epassport';
  if (kind !== 'eid' && kind !== 'epassport') throw new Error('PHOENIX_NFC_PROFILE must be eid or epassport');
  const id = (name: string) => screen.getByTestId('com.vsmartcard.acardemulator.phoenixcloud.fdroid:id/' + name);
  await app.open();
  await id('cloud_logo').tap();
  await screen.scrollUntilVisible(id('cloud_info_diagnostics'));
  await id('cloud_info_diagnostics').tap();
  await screen.scrollUntilVisible(id('cloud_test_reader'));
  if (kind === 'epassport') {
    await id('cloud_test_kind').tap();
    await screen.getByText('ePassport', { exact: true }).tap();
  }
  try {
    for (let run = 1; run <= 3; run++) {
      await screen.scrollUntilVisible(id('cloud_test_reader'));
      await id('cloud_test_reader').tap();
      await screen.getByText('TEST STARTEN', { exact: true }).tap();
      await screen.scrollUntilVisible(id('cloud_test_status'));
      await expect(id('cloud_test_status')).toContainText('10/10 Lesetests bestanden', { timeout: 30_000 });
      await expect(id('cloud_test_stop')).toBeDisabled();
      await screen.scrollUntilVisible(id('cloud_quality_summary'), { direction: 'up' });
      console.log(`${kind} run ${run}: ${await id('cloud_quality_summary').textContent()}`);
    }
  } finally {
    await screen.scrollUntilVisible(id('cloud_test_stop'));
    if (!(await id('cloud_test_stop').isDisabled())) await id('cloud_test_stop').tap();
  }
});
