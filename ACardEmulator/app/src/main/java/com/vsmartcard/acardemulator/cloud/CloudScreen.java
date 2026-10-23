// SPDX-License-Identifier: GPL-3.0-or-later
// Phoenix Cloud extension; license notice added 2026-10-23. See LICENSE_REVIEW.md.
package com.vsmartcard.acardemulator.cloud;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.WindowManager;
import ACardEmulator.R;
abstract class CloudScreen extends Activity {
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(CloudLanguage.wrap(base));}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);}
    @Override protected void onResume(){super.onResume();refreshLanguage();}
    final boolean refreshLanguage(){
        String rendered=getResources().getConfiguration().locale.getLanguage();
        if(!CloudLanguage.language(this).equals(rendered)){recreate();return true;}return false;
    }
    final void chooseLanguage(){
        new AlertDialog.Builder(this).setTitle(R.string.cloud_language)
            .setSingleChoiceItems(new String[]{"Deutsch","English"},CloudLanguage.language(this).equals("en")?1:0,(dialog,index)->{
                CloudLanguage.save(this,index==0?"de":"en");dialog.dismiss();recreate();
            }).setNegativeButton(android.R.string.cancel,null).show();
    }
}
