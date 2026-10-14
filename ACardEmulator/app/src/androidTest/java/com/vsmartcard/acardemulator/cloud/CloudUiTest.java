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
    @Test public void pairedStatusSwitchesItsLabelMarkIconAndAction() {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        CloudActivity activity=(CloudActivity)instrumentation.startActivitySync(
            new Intent(instrumentation.getTargetContext(),CloudActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->{
            activity.renderConnection(false,true);
            assertEquals("Verbinden",((TextView)activity.findViewById(R.id.cloud_connection_action)).getText().toString());
            assertEquals("✕",((TextView)activity.findViewById(R.id.cloud_status_mark)).getText().toString());
            assertEquals(activity.getString(R.string.cloud_saved_offline),((TextView)activity.findViewById(R.id.cloud_pairing_status)).getText().toString());
            ((TextView)activity.findViewById(R.id.cloud_state)).setText("Nicht verbunden");
            snapshot(activity,"cloud-ui-disconnected.png");
            activity.renderConnection(true,true);
            assertEquals("Verbindung aufheben",((TextView)activity.findViewById(R.id.cloud_connection_action)).getText().toString());
            assertEquals("✓",((TextView)activity.findViewById(R.id.cloud_status_mark)).getText().toString());
            assertEquals(activity.getString(R.string.cloud_connected_icon),activity.findViewById(R.id.cloud_connection_icon).getContentDescription());
            ((TextView)activity.findViewById(R.id.cloud_state)).setText("Online · Kartenmodus");
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
            assertNull(activity.findViewById(R.id.cloud_scan));
            assertNull(activity.findViewById(R.id.cloud_import));
            activity.findViewById(R.id.cloud_manage_pairing).performClick();
        });
        android.app.Activity pairing=instrumentation.waitForMonitorWithTimeout(monitor,5000);
        assertNotNull("Pairing page did not open",pairing);
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->{
            assertNotNull(pairing.findViewById(R.id.cloud_scan));
            assertNotNull(pairing.findViewById(R.id.cloud_import));
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
            assertTrue(status.contains("Verbunden")||status.contains("Nicht verbunden"));
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
            snapshot(info,"cloud-ui-info.png");
            info.findViewById(R.id.cloud_info_back).performClick();
        });
        instrumentation.removeMonitor(monitor);
        instrumentation.runOnMainSync(activity::finish);
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
