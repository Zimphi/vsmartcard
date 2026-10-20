import { test } from '@e2e-dev/mobile';
import { expect } from 'e2e';

// No model, credentials, hardware test or cloud writes. Restore the original UI language.
test('version page exposes export and NFC test above developer options in both languages', async ({ app, screen }) => {
  const id=(name:string)=>screen.getByTestId('com.vsmartcard.acardemulator.phoenixcloud.fdroid:id/'+name);
  await app.open();
  const original=await id('cloud_language_button').textContent();
  try {
    for(const language of ['Deutsch','English']){
      const german=language==='Deutsch';
      await id('cloud_language_button').tap();await screen.getByText(language).tap();
      await id('cloud_logo').tap();
      await expect(id('cloud_info_version')).toContainText('4.11.0');
      await expect(id('cloud_export_diagnostics')).toContainText(german?'Diagnosedaten exportieren':'Export diagnostic data');
      await id('cloud_export_diagnostics').tap();
      await expect(screen.getByText(german?/APDU-Inhalte/:/APDU contents/)).toBeVisible();
      await screen.getByText(/Abbrechen|ABBRECHEN|Cancel|CANCEL/).tap();
      await screen.scrollUntilVisible(id('cloud_info_nfc_test'));
      await expect(id('cloud_info_nfc_test')).toContainText(german?'NFC-Test':'NFC test');
      await id('cloud_info_nfc_test').tap();
      await expect(id('cloud_guided_summary')).toContainText(german?'Noch nicht vollständig geprüft':'Not fully checked yet');
      await id('cloud_guided_back').tap();
      await screen.scrollUntilVisible(id('cloud_info_diagnostics'));
      await expect(id('cloud_info_diagnostics')).toContainText(german?'Entwickleroptionen':'Developer options');
      await id('cloud_info_diagnostics').tap();
      await expect(id('cloud_guided_test')).toContainText(german?'Geführter Gesamttest':'Guided complete test');
      await id('cloud_diagnostics_back').tap();
      await screen.scrollUntilVisible(id('cloud_info_back'),{direction:'up'});
      await id('cloud_info_back').tap();
    }
  } finally {
    await app.open();await id('cloud_language_button').tap();
    await screen.getByText(original?.trim()==='EN'?'English':'Deutsch').tap();
  }
});
