package com.vsmartcard.acardemulator.cloud;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import ACardEmulator.R;
import ACardEmulator.BuildConfig;
import org.junit.Test;
import static org.junit.Assert.*;

public class CloudUiTest {
    @Test public void gainChartShowsMissingValuesAndUncalibratedReadingsSeparately(){
        android.app.Instrumentation i=InstrumentationRegistry.getInstrumentation();
        CloudDiagnosticsActivity activity=(CloudDiagnosticsActivity)i.startActivitySync(
            new Intent(i.getTargetContext(),CloudDiagnosticsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        i.waitForIdleSync();
        try{i.runOnMainSync(()->{
            NfcGain.clear();activity.renderGain();
            assertNotNull(activity.findViewById(R.id.cloud_gain_graph));
            assertNotNull(activity.findViewById(R.id.cloud_position_gain_graph));
            NfcGain.record(-1,android.os.SystemClock.elapsedRealtime());
            NfcGain.record(42,android.os.SystemClock.elapsedRealtime());activity.renderGain();
            assertEquals(1,new NfcGain.Stats(NfcGain.after(0)).count);
            snapshot(activity,"cloud-ui-gain.png");
        });}finally{NfcGain.clear();i.runOnMainSync(activity::finish);}
    }
    @Test public void pairedStatusSwitchesItsLabelIconAndAction() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        CloudActivity activity=(CloudActivity)instrumentation.startActivitySync(
            new Intent(instrumentation.getTargetContext(),CloudActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->{
            for(String name:new String[]{"cloud_nfc_title","cloud_nfc_status","cloud_traffic_title","cloud_traffic_status","cloud_server","cloud_status_mark"}){int id=activity.getResources().getIdentifier(name,"id",activity.getPackageName());assertTrue(id==0||activity.findViewById(id)==null);}
            assertEquals(1,((ViewGroup)activity.findViewById(R.id.cloud_connection_icon).getParent()).indexOfChild(activity.findViewById(R.id.cloud_pairing_status)));
            activity.renderConnection(false,true);
            assertEquals(activity.getString(R.string.cloud_disconnected_icon),activity.findViewById(R.id.cloud_connection_icon).getContentDescription());
            assertEquals(activity.getString(R.string.cloud_connect),((TextView)activity.findViewById(R.id.cloud_connection_action)).getText().toString());
            assertEquals(activity.getString(R.string.cloud_saved_offline),((TextView)activity.findViewById(R.id.cloud_pairing_status)).getText().toString());
            ((TextView)activity.findViewById(R.id.cloud_state)).setText("Nicht verbunden");
            activity.renderProfile(false,null,false);
            snapshot(activity,"cloud-ui-disconnected.png");
            activity.renderConnection(true,true);
            assertEquals(activity.getString(R.string.cloud_disconnect),((TextView)activity.findViewById(R.id.cloud_connection_action)).getText().toString());
            assertEquals(activity.getString(R.string.cloud_connected_icon),activity.findViewById(R.id.cloud_connection_icon).getContentDescription());
            activity.findViewById(R.id.cloud_state).setVisibility(View.GONE);
            activity.renderProfile(true,new CloudApi.Profile(true,"erika-mustermann","eid",1),false);
            snapshot(activity,"cloud-ui-connected.png");
            activity.renderConnection(false,false);
            assertEquals(activity.getString(R.string.cloud_unpaired),((TextView)activity.findViewById(R.id.cloud_pairing_status)).getText().toString());
        });
        instrumentation.runOnMainSync(activity::finish);
    }
    @Test public void phoenixLauncherVersionAndSeparatePairingPage() throws Exception {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.content.Context context=instrumentation.getTargetContext();
        Intent launch=context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assertNotNull(launch);
        assertEquals(CloudActivity.class.getName(),launch.getComponent().getClassName());
        assertEquals("Phoenix Cloud",context.getPackageManager().getApplicationLabel(context.getApplicationInfo()).toString());
        CloudActivity activity=(CloudActivity)instrumentation.startActivitySync(launch);
        instrumentation.waitForIdleSync();
        android.app.Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(CloudPairingActivity.class.getName(),null,false);
        instrumentation.runOnMainSync(()->{
            assertEquals("Version "+BuildConfig.VERSION_NAME,((TextView)activity.findViewById(R.id.cloud_version)).getText().toString());
            android.widget.RadioButton read=activity.findViewById(R.id.cloud_reader_mode);
            android.widget.RadioButton simulate=activity.findViewById(R.id.cloud_card_mode);
            assertEquals(activity.getString(R.string.cloud_reader_mode),read.getText().toString());
            assertEquals(activity.getString(R.string.cloud_card_mode),simulate.getText().toString());
            assertTrue(read.getRight()<=simulate.getLeft());
            assertNotNull(read.getCompoundDrawables()[1]);
            assertNotNull(simulate.getCompoundDrawables()[1]);
            read.performClick();assertTrue(read.isChecked());assertFalse(simulate.isChecked());
            simulate.performClick();assertTrue(simulate.isChecked());assertFalse(read.isChecked());
            assertNull(activity.findViewById(R.id.cloud_scan));
            assertNull(activity.findViewById(R.id.cloud_import));
            activity.findViewById(R.id.cloud_manage_pairing).performClick();
        });
        android.app.Activity pairing=instrumentation.waitForMonitorWithTimeout(monitor,5000);
        assertNotNull("Pairing page did not open",pairing);
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->{
            assertNull(pairing.findViewById(R.id.cloud_scan));
            assertNull(pairing.findViewById(R.id.cloud_import));
            assertNotNull(pairing.findViewById(R.id.cloud_delete_pairing));
            snapshot(pairing,"cloud-ui-pairing.png");
            pairing.findViewById(R.id.cloud_pairing_back).performClick();
        });
        instrumentation.removeMonitor(monitor);
        instrumentation.runOnMainSync(activity::finish);
    }
    @Test public void logoOpensLiveInfoAndWebsiteUsesHttpsWithoutCredentials() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        CloudActivity activity=(CloudActivity)instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),CloudActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.waitForIdleSync();
        android.app.Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(CloudInfoActivity.class.getName(),null,false);
        instrumentation.runOnMainSync(()->activity.findViewById(R.id.cloud_logo).performClick());
        CloudInfoActivity info=(CloudInfoActivity)instrumentation.waitForMonitorWithTimeout(monitor,5000);
        assertNotNull("Logo did not open app information",info);
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->{
            assertEquals("Version "+BuildConfig.VERSION_NAME,((TextView)info.findViewById(R.id.cloud_info_version)).getText().toString());
            String status=((TextView)info.findViewById(R.id.cloud_info_status)).getText().toString();
            assertTrue(status.contains(info.getString(R.string.cloud_connected))||status.contains(info.getString(R.string.cloud_saved_offline))||status.contains(info.getString(R.string.cloud_unpaired)));
            Intent website=CloudInfoActivity.websiteIntent();
            assertEquals(Intent.ACTION_VIEW,website.getAction());
            assertEquals("https://epassport-simulator.de",website.getDataString());
            assertNull(website.getExtras());
            android.content.IntentFilter filter=new android.content.IntentFilter(Intent.ACTION_VIEW);
            filter.addDataScheme("https");filter.addDataAuthority("epassport-simulator.de",null);
            android.app.Instrumentation.ActivityMonitor browser=instrumentation.addMonitor(filter,new android.app.Instrumentation.ActivityResult(0,null),true);
            info.findViewById(R.id.cloud_website).performClick();
            assertEquals("Website button did not open the expected URL",1,browser.getHits());
            instrumentation.removeMonitor(browser);
            assertTrue(info.getString(R.string.cloud_license_notice).contains("GPL-3.0-or-later"));
            android.app.AlertDialog license=info.showLicense();
            assertTrue(license.isShowing());
            ViewGroup licensePanel=(ViewGroup)license.findViewById(android.R.id.custom);
            android.widget.ScrollView licenseScroll=(android.widget.ScrollView)licensePanel.getChildAt(0);
            String licenseText=((TextView)licenseScroll.getChildAt(0)).getText().toString();
            assertTrue(licenseText.contains("GNU GENERAL PUBLIC LICENSE"));
            assertTrue(licenseText.contains("Version 3, 29 June 2007"));
            assertTrue(licenseText.contains("END OF TERMS AND CONDITIONS"));
            license.dismiss();
            snapshot(info,"cloud-ui-info.png");
            info.findViewById(R.id.cloud_info_back).performClick();
        });
        instrumentation.removeMonitor(monitor);
        instrumentation.runOnMainSync(activity::finish);
    }
    @Test public void simplifiedHomeKeepsProfileAndSeparateDiagnostics(){
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.content.Context context=instrumentation.getTargetContext();
        android.content.SharedPreferences preferences=context.getSharedPreferences("phoenix-ui",0);
        boolean original=preferences.getBoolean("developer_options",false);
        CloudActivity activity=(CloudActivity)instrumentation.startActivitySync(new Intent(context,CloudActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{
            instrumentation.runOnMainSync(()->{
                activity.renderProfile(true,new CloudApi.Profile(true,"erika-mustermann","eid",1),false);
                assertTrue(((TextView)activity.findViewById(R.id.cloud_profile_status)).getText().toString().contains("erika-mustermann"));
                activity.renderProfile(true,new CloudApi.Profile(true,null,null,0),false);
                assertEquals(activity.getString(R.string.cloud_profile_legacy),((TextView)activity.findViewById(R.id.cloud_profile_status)).getText().toString());
                activity.renderProfile(false,new CloudApi.Profile(true,"old","eid",1),false);
                assertEquals(activity.getString(R.string.cloud_profile_offline),((TextView)activity.findViewById(R.id.cloud_profile_status)).getText().toString());
                ((android.widget.RadioButton)activity.findViewById(R.id.cloud_reader_mode)).performClick();
                activity.renderProfile(true,null,true);
                assertEquals(activity.getString(R.string.cloud_reader_detected),((TextView)activity.findViewById(R.id.cloud_profile_status)).getText().toString());
            });
            CloudInfoActivity info=(CloudInfoActivity)instrumentation.startActivitySync(new Intent(context,CloudInfoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            instrumentation.waitForIdleSync();
            android.app.Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(CloudDiagnosticsActivity.class.getName(),null,false);
            instrumentation.runOnMainSync(()->{
                assertNull(activity.findViewById(R.id.cloud_log_panel));
                int id=activity.getResources().getIdentifier("cloud_open_diagnostics","id",activity.getPackageName());
                assertTrue(id==0||activity.findViewById(id)==null);
                info.findViewById(R.id.cloud_info_diagnostics).performClick();
            });
            CloudDiagnosticsActivity diagnostics=(CloudDiagnosticsActivity)instrumentation.waitForMonitorWithTimeout(monitor,5000);assertNotNull(diagnostics);
            instrumentation.runOnMainSync(()->{
                android.widget.CheckBox developer=diagnostics.findViewById(R.id.cloud_developer_options);
                developer.setChecked(true);assertTrue(preferences.getBoolean("developer_options",false));
                CloudLog.add("CLOUD",R.string.cloud_log_connected,false);
            });
            instrumentation.waitForIdleSync();
            try{Thread.sleep(1100);}catch(InterruptedException e){throw new AssertionError(e);}
            instrumentation.runOnMainSync(()->{
                assertEquals(View.VISIBLE,diagnostics.findViewById(R.id.cloud_log_panel).getVisibility());
                assertTrue(((TextView)diagnostics.findViewById(R.id.cloud_log_text)).getText().toString().contains("[CLOUD]"));
                snapshot(diagnostics,"cloud-ui-developer.png");
                diagnostics.findViewById(R.id.cloud_diagnostics_back).performClick();
            });
            instrumentation.removeMonitor(monitor);
            instrumentation.runOnMainSync(info::finish);
        }finally{
            preferences.edit().putBoolean("developer_options",original).commit();
            instrumentation.runOnMainSync(activity::finish);
        }
    }
    @Test public void englishHomeAndContextHelpUseTranslatedResources(){
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.content.Context context=instrumentation.getTargetContext();
        android.content.SharedPreferences preferences=context.getSharedPreferences("phoenix-ui",0);
        String original=preferences.getString("language",null);
        preferences.edit().putString("language","en").commit();
        CloudActivity activity=null;
        try{
            activity=(CloudActivity)instrumentation.startActivitySync(new Intent(context,CloudActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            final CloudActivity home=activity;instrumentation.waitForIdleSync();
            android.app.Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(CloudHelpActivity.class.getName(),null,false);
            instrumentation.runOnMainSync(()->{
                assertEquals("Read card",((TextView)home.findViewById(R.id.cloud_reader_mode)).getText().toString());
                assertEquals("Simulate card",((TextView)home.findViewById(R.id.cloud_card_mode)).getText().toString());
                assertNull(home.findViewById(R.id.cloud_log_panel));
                snapshot(home,"cloud-ui-home-en.png");
                home.findViewById(R.id.cloud_help).performClick();
            });
            CloudHelpActivity help=(CloudHelpActivity)instrumentation.waitForMonitorWithTimeout(monitor,5000);assertNotNull(help);
            instrumentation.runOnMainSync(()->{
                assertNotNull(help.findViewById(R.id.cloud_help_image));
                assertTrue(((TextView)help.findViewById(R.id.cloud_help_body)).getText().length()>30);
                help.findViewById(R.id.cloud_help_back).performClick();
            });instrumentation.removeMonitor(monitor);
            CloudPairingActivity pairing=(CloudPairingActivity)instrumentation.startActivitySync(new Intent(context,CloudPairingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->{snapshot(pairing,"cloud-ui-pairing-en.png");pairing.finish();});
        }finally{
            if(activity!=null){final CloudActivity home=activity;instrumentation.runOnMainSync(home::finish);}
            if(original==null)preferences.edit().remove("language").commit();else preferences.edit().putString("language",original).commit();
        }
    }
    @Test public void managedPairingDeletesOnlyAfterConfirmationAndSetupStaysSeparate() throws Exception {
        android.app.Instrumentation i=InstrumentationRegistry.getInstrumentation();
        android.content.Context context=i.getTargetContext();
        context.stopService(new Intent(context,CloudService.class));i.waitForIdleSync();
        DeviceVault vault=new DeviceVault(context);org.json.JSONObject original=vault.load();
        CloudPairingActivity manage=null;
        try{
            vault.save("https://example.invalid","synthetic-pairing-token");
            manage=(CloudPairingActivity)i.startActivitySync(new Intent(context,CloudPairingActivity.class).putExtra("manage_pairing",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            final CloudPairingActivity screen=manage;i.waitForIdleSync();
            i.runOnMainSync(()->{
                assertNull(screen.findViewById(R.id.cloud_scan));assertNull(screen.findViewById(R.id.cloud_import));assertNull(screen.findViewById(R.id.cloud_logo));
                assertEquals("example.invalid",((TextView)screen.findViewById(R.id.cloud_pairing_name)).getText().toString());
                android.app.AlertDialog cancel=screen.showDeleteConfirmation();cancel.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick();
            });
            assertNotNull(vault.load());
            i.runOnMainSync(()->{
                snapshot(screen,"cloud-ui-managed-pairing.png");
                android.app.AlertDialog confirm=screen.showDeleteConfirmation();confirm.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
            });
            i.waitForIdleSync();assertNull(vault.load());assertTrue(screen.isFinishing());
            CloudPairingActivity empty=(CloudPairingActivity)i.startActivitySync(new Intent(context,CloudPairingActivity.class).putExtra("manage_pairing",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            i.runOnMainSync(()->{assertEquals(View.GONE,empty.findViewById(R.id.cloud_delete_pairing).getVisibility());assertNull(empty.findViewById(R.id.cloud_scan));empty.finish();});
            CloudPairingActivity setup=(CloudPairingActivity)i.startActivitySync(new Intent(context,CloudPairingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            i.runOnMainSync(()->{assertNotNull(setup.findViewById(R.id.cloud_scan));assertNotNull(setup.findViewById(R.id.cloud_import));assertNull(setup.findViewById(R.id.cloud_logo));setup.finish();});
        }finally{
            if(manage!=null){final CloudPairingActivity screen=manage;i.runOnMainSync(screen::finish);}
            if(original==null)vault.clear();else vault.save(original.getString("server"),original.getString("token"));
        }
    }
    private static void snapshot(android.app.Activity activity,String name){
        ViewGroup frame=activity.findViewById(android.R.id.content);
        View view=((ViewGroup)frame.getChildAt(0)).getChildAt(0);
        int width=view.getWidth();
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        view.layout(0,0,width,view.getMeasuredHeight());
        Bitmap bitmap=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);canvas.drawColor(activity.getColor(R.color.cloud_background));view.draw(canvas);
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(activity.getCacheDir(),name))){
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));
        }catch(java.io.IOException error){throw new AssertionError(error);}
        finally{bitmap.recycle();}
    }
}
