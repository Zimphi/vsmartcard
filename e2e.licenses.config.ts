// SPDX-License-Identifier: GPL-3.0-or-later
import type { E2EConfig } from 'e2e';
import { mobile } from '@e2e-dev/mobile';

export default {
  tests: ['tests/e2e/licenses.e2e.ts'],
  workers: 1,
  targets: [{
    engine: mobile({ platform: 'android' }),
    app: {
      bundleId: 'com.vsmartcard.acardemulator.phoenixcloud.fdroid',
      appPath: process.env.PHOENIX_TEST_APK ?? new URL('./ACardEmulator/app/build/outputs/apk/fdroid/debug/ACardEmulator-4.11.0-fdroid-debug.apk', import.meta.url).pathname,
    },
  }],
} satisfies E2EConfig;
