// SPDX-License-Identifier: GPL-3.0-or-later
import { test } from '@e2e-dev/mobile';
import { expect } from 'e2e';

// Deterministic local UI check; no model, account changes, cloud writes or NFC tests.
test('the offline license dialog includes the app and third-party licenses', async ({ device, app, screen }) => {
  const id=(name:string)=>screen.getByTestId('com.vsmartcard.acardemulator.phoenixcloud.fdroid:id/'+name);
  await device.installApp();
  await app.open();
  await id('cloud_logo').tap();
  await screen.scrollUntilVisible(id('cloud_license'));
  await id('cloud_license').tap();
  const text=screen.getByText(/GNU GENERAL PUBLIC LICENSE/);
  await expect(text).toContainText('Version 3, 29 June 2007');
  await expect(text).toContainText('Apache License');
  await expect(text).toContainText('Mozilla Public License');
  await expect(text).toContainText('Bouncy Castle');
  await expect(text).toContainText('https://github.com/Zimphi/vsmartcard');
  await screen.getByText(/^(OK|Ok)$/).tap();
  await screen.scrollUntilVisible(id('cloud_info_back'), { direction: 'up' });
  await id('cloud_info_back').tap();
});
