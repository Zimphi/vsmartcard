package com.vsmartcard.acardemulator.cloud;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Physical observation, no synthetic samples and no claim of support from API level alone. */
public class NfcGainProbeTest {
    @Test @SdkSuppress(minSdkVersion=35) public void attemptObserveModeActivation() throws Exception {
        android.app.Instrumentation i=InstrumentationRegistry.getInstrumentation();
        android.content.Context context=i.getTargetContext();NfcAdapter nfc=NfcAdapter.getDefaultAdapter(context);
        assertNotNull(nfc);assertTrue(nfc.isEnabled());final boolean original=nfc.isObserveModeEnabled();
        JSONObject report=new JSONObject().put("format","phoenix-observe-activation-v1")
            .put("model",android.os.Build.MODEL).put("android_api",android.os.Build.VERSION.SDK_INT)
            .put("supported",nfc.isObserveModeSupported()).put("original_enabled",original);
        CloudDiagnosticsActivity activity=(CloudDiagnosticsActivity)i.startActivitySync(
            new Intent(context,CloudDiagnosticsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        i.waitForIdleSync();
        try{
            i.runOnMainSync(()->{
                nfc.disableReaderMode(activity);
                assertTrue(CardEmulation.getInstance(nfc).setPreferredService(activity,
                    new android.content.ComponentName(context,CloudHceService.class)));
                NfcGain.clear();
                try{report.put("enable_returned",nfc.setObserveModeEnabled(true));}
                catch(Exception e){try{report.put("enable_exception",e.getClass().getSimpleName());}catch(Exception impossible){throw new AssertionError(impossible);}}
            });
            report.put("enabled_after_attempt",nfc.isObserveModeEnabled());
            Thread.sleep(nfc.isObserveModeEnabled()?10000:1000);
            NfcGain.Stats stats=new NfcGain.Stats(NfcGain.after(0));
            report.put("polling_frames",stats.frames).put("valid_gain_readings",stats.count);
        }finally{
            try{i.runOnMainSync(()->{if(nfc.isObserveModeEnabled()!=original)assertTrue(nfc.setObserveModeEnabled(original));});}
            finally{i.runOnMainSync(activity::finish);}
            report.put("restored_enabled",nfc.isObserveModeEnabled()).put("restoration_verified",original==nfc.isObserveModeEnabled());
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(context.getCacheDir(),"nfc-observe-activation.json"))){out.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            assertEquals(original,nfc.isObserveModeEnabled());
        }
    }
    @Test @SdkSuppress(minSdkVersion=35) public void captureActualPollingFrames() throws Exception {
        android.app.Instrumentation i=InstrumentationRegistry.getInstrumentation();
        android.content.Context context=i.getTargetContext();NfcAdapter nfc=NfcAdapter.getDefaultAdapter(context);
        assertNotNull(nfc);assertTrue(nfc.isEnabled());
        CloudDiagnosticsActivity activity=(CloudDiagnosticsActivity)i.startActivitySync(
            new Intent(context,CloudDiagnosticsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        i.waitForIdleSync();
        try{
            i.runOnMainSync(()->{
                nfc.disableReaderMode(activity);
                assertTrue(CardEmulation.getInstance(nfc).setPreferredService(activity,
                    new android.content.ComponentName(context,CloudHceService.class)));
                NfcGain.clear();
            });
            long from=NfcGain.sequence(),start=SystemClock.elapsedRealtime();
            android.util.Log.i("PhoenixGainProbe","Physical polling observation started; no samples injected");
            Thread.sleep(60000);
            java.util.List<NfcGain.Sample> samples=NfcGain.after(from);NfcGain.Stats stats=new NfcGain.Stats(samples);
            JSONObject report=new JSONObject().put("format","phoenix-nfc-gain-probe-v1")
                .put("model",android.os.Build.MODEL).put("android_api",android.os.Build.VERSION.SDK_INT)
                .put("app_version",ACardEmulator.BuildConfig.VERSION_NAME)
                .put("observe_mode_supported",nfc.isObserveModeSupported()).put("foreground_hce_preference",true)
                .put("elapsed_ms",SystemClock.elapsedRealtime()-start).put("total_frames",NfcGain.sequence()-from)
                .put("retained_frames",stats.frames).put("retained_valid_readings",stats.count)
                .put("outcome",stats.count>0?"gain_available":stats.frames>0?"frames_without_gain":"no_polling_frames_observed");
            JSONArray values=new JSONArray();for(NfcGain.Sample s:samples)values.put(s.value);report.put("raw_gain_values",values);
            if(stats.count>0)report.put("min",stats.min).put("median",stats.median).put("max",stats.max);
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(context.getCacheDir(),"nfc-gain-probe.json"))){out.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            android.util.Log.i("PhoenixGainProbe","Physical polling observation complete: "+report.getString("outcome"));
        }finally{i.runOnMainSync(activity::finish);}
    }
}
