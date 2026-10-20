package com.vsmartcard.acardemulator.cloud;

import android.content.Context;
import android.content.ContextWrapper;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

public class LabControlTest {
    private String originalEvidence;
    @org.junit.Before public void preserveDiagnosticEvidence(){
        originalEvidence=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("phoenix-diagnostics",0).getString("records",null);
    }
    @org.junit.After public void restoreDiagnosticEvidence(){
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        android.content.SharedPreferences.Editor edit=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("phoenix-diagnostics",0).edit();
        if(originalEvidence==null)edit.remove("records");else edit.putString("records",originalEvidence);assertTrue(edit.commit());
    }
    @Test public void privateRequestProgressAndCancelRequireMatchingId() throws Exception {
        Context original=InstrumentationRegistry.getInstrumentation().getTargetContext();
        final File folder=new File(original.getCacheDir(),"isolated-lab-test");assertTrue(folder.isDirectory()||folder.mkdir());
        Context isolated=new ContextWrapper(original){@Override public File getCacheDir(){return folder;}};
        try {
            LabControl control=new LabControl(isolated);
            String id=control.request("usb-profile","eid",1);
            JSONObject request=read(folder,"lab-request.json");
            assertTrue(request.getBoolean("consent"));assertEquals(id,request.getString("id"));
            assertEquals(7,request.getJSONObject("device").length());
            JSONObject progress=new JSONObject().put("format","phoenix-lab-progress-v1").put("id","other")
                .put("phase","complete").put("passed",true).put("profile_verified",true).put("successes",1)
                .put("samples",new JSONArray().put(new JSONObject().put("sequence",1).put("duration_ms",12.5).put("success",true)));
            write(folder,"lab-progress.json",progress);assertNull(control.progress());
            progress.put("id",id);write(folder,"lab-progress.json",progress);
            JSONObject accepted=control.progress();assertNotNull(accepted);
            long sequence=NfcQuality.sequence();control.progress();assertEquals(sequence,NfcQuality.sequence());
            accepted.getJSONObject("request").getJSONObject("device").put("serial","must-not-be-uploaded");
            JSONObject report=LabControl.successfulReport(accepted);assertNotNull(report);
            assertFalse(report.has("serial"));assertFalse(report.has("consent"));
            accepted.put("profile_verified",false);assertNull(LabControl.successfulReport(accepted));
            control.cancel();assertEquals(id,read(folder,"lab-cancel.json").getString("id"));
            try{control.request("shell","eid",1);fail();}catch(IllegalArgumentException expected){}
        }finally{NfcQuality.clear("mac-pcsc");for(File file:folder.listFiles())assertTrue(file.delete());assertTrue(folder.delete());}
    }
    @Test public void cancelledDisclosureNeverCreatesTestRequest() throws Exception {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Context context=instrumentation.getTargetContext();File request=new File(context.getCacheDir(),"lab-request.json");
        byte[] before=request.exists()?java.nio.file.Files.readAllBytes(request.toPath()):null;
        CloudDiagnosticsActivity activity=(CloudDiagnosticsActivity)instrumentation.startActivitySync(new android.content.Intent(context,CloudDiagnosticsActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.runOnMainSync(()->{
            android.app.AlertDialog dialog=activity.confirmMacMode("usb-profile");assertTrue(dialog.isShowing());
            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick();activity.finish();
        });
        byte[] after=request.exists()?java.nio.file.Files.readAllBytes(request.toPath()):null;assertArrayEquals(before,after);
    }
    @Test public void compatibilityUploadHappensOnlyAfterPositiveConfirmation() throws Exception {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Context context=instrumentation.getTargetContext();
        CloudDiagnosticsActivity activity=(CloudDiagnosticsActivity)instrumentation.startActivitySync(new android.content.Intent(context,CloudDiagnosticsActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        java.util.concurrent.CountDownLatch uploaded=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
        final JSONObject[] received={null};
        CloudApi api=new CloudApi("https://example.org",null){@Override JSONObject post(String endpoint,JSONObject value)throws Exception{
            assertEquals("compatibility",endpoint);received[0]=new JSONObject(value.toString());calls.incrementAndGet();uploaded.countDown();return new JSONObject().put("saved",true);
        }};
        JSONObject report=LabControl.device().put("passed",true).put("test_id",java.util.UUID.randomUUID().toString())
            .put("mode","card").put("source","cloud-eac").put("attempts",1).put("successes",1);
        try{
            instrumentation.runOnMainSync(()->{
                android.app.AlertDialog dialog=activity.showCompatibility(report,api);
                assertTrue(dialog.isShowing());assertFalse(report.has("consent"));assertEquals(0,calls.get());
                dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick();assertEquals(0,calls.get());
                activity.showCompatibility(report,api).getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
            });
            assertTrue(uploaded.await(5,java.util.concurrent.TimeUnit.SECONDS));assertEquals(1,calls.get());assertTrue(received[0].getBoolean("consent"));
        }finally{instrumentation.runOnMainSync(activity::finish);}
    }
    @Test public void unchangedLabResultsDoNotOverwriteNewUserGuidance() throws Exception {
        android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();Context context=instrumentation.getTargetContext();
        File folder=new File(context.getCacheDir(),"isolated-lab-guidance");assertTrue(folder.isDirectory()||folder.mkdir());
        LabControl control=new LabControl(new ContextWrapper(context){@Override public File getCacheDir(){return folder;}});
        String id=control.request("usb-profile","eid",1);
        JSONObject progress=new JSONObject().put("format","phoenix-lab-progress-v1").put("id",id).put("phase","failed").put("passed",false).put("failure_code","no_nfc_target");
        write(folder,"lab-progress.json",progress);
        CloudDiagnosticsActivity activity=(CloudDiagnosticsActivity)instrumentation.startActivitySync(new android.content.Intent(context,CloudDiagnosticsActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        try{
            java.lang.reflect.Field field=CloudDiagnosticsActivity.class.getDeclaredField("lab");field.setAccessible(true);field.set(activity,control);
            java.lang.reflect.Method refresh=CloudDiagnosticsActivity.class.getDeclaredMethod("readLabProgress");refresh.setAccessible(true);
            instrumentation.runOnMainSync(()->{
                try{
                    refresh.invoke(activity);android.widget.TextView status=activity.findViewById(ACardEmulator.R.id.cloud_test_status);
                    assertEquals(activity.getString(ACardEmulator.R.string.cloud_lab_no_target),status.getText().toString());
                    status.setText(ACardEmulator.R.string.cloud_lab_card_required);refresh.invoke(activity);
                    assertEquals(activity.getString(ACardEmulator.R.string.cloud_lab_card_required),status.getText().toString());
                }catch(Exception failure){throw new AssertionError(failure);}
            });
        }finally{instrumentation.runOnMainSync(activity::finish);for(File file:folder.listFiles())assertTrue(file.delete());assertTrue(folder.delete());}
    }
    private static JSONObject read(File folder,String name) throws Exception {
        try(java.io.FileInputStream in=new java.io.FileInputStream(new File(folder,name))){
            byte[] bytes=new byte[(int)new File(folder,name).length()];assertEquals(bytes.length,in.read(bytes));return new JSONObject(new String(bytes,"UTF-8"));
        }
    }
    private static void write(File folder,String name,JSONObject value) throws Exception {
        try(FileOutputStream out=new FileOutputStream(new File(folder,name))){out.write(value.toString().getBytes("UTF-8"));}
    }
}
