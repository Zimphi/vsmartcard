package com.vsmartcard.acardemulator.cloud;
import android.content.Context;
import android.nfc.NfcAdapter;
import org.json.JSONObject;
import org.json.JSONArray;
/** Positive allowlist only: no credentials, card content, arbitrary logs or exception text. */
final class DiagnosticExport {
    static JSONObject snapshot(Context context)throws Exception{
        JSONObject out=new JSONObject().put("format","phoenix-diagnostics-v1").put("generated_at_ms",System.currentTimeMillis())
            .put("device",LabControl.device()).put("connection_confirmed",CloudService.connected())
            .put("observations_are_bounded",true).put("tests",new DiagnosticEvidence(context).records());
        NfcAdapter nfc=NfcAdapter.getDefaultAdapter(context);
        out.put("nfc_enabled",nfc!=null&&nfc.isEnabled()).put("hce_supported",context.getPackageManager().hasSystemFeature("android.hardware.nfc.hce"));
        out.put("observe_supported",android.os.Build.VERSION.SDK_INT>=35&&nfc!=null&&nfc.isObserveModeSupported());
        JSONArray sources=new JSONArray();
        for(String source:new String[]{"phone-reader","phone-card","mac-pcsc"}){
            NfcQuality.Stats stats=new NfcQuality.Stats(NfcQuality.after(source,0));
            JSONObject row=new JSONObject().put("source",source).put("attempts",stats.count).put("successes",stats.successes).put("failures",stats.count-stats.successes);
            if(stats.successes>0)row.put("median_ms",stats.median).put("p95_ms",stats.p95).put("max_ms",stats.max);
            sources.put(row);
        }
        out.put("transfers",sources);NfcGain.Stats gain=new NfcGain.Stats(NfcGain.after(0));
        JSONObject gains=new JSONObject().put("frames",gain.frames).put("samples",gain.count).put("is_rssi",false);
        if(gain.count>0)gains.put("min",gain.min).put("median",gain.median).put("max",gain.max);
        out.put("vendor_gain",gains);
        JSONArray events=new JSONArray();
        for(CloudLog.Entry entry:CloudLog.snapshot()){
            String tag=java.util.Arrays.asList("CLOUD","NFC","PROFIL").contains(entry.tag)?entry.tag:"OTHER";
            String name=entry.messageResource==0?"freeform_omitted":context.getResources().getResourceEntryName(entry.messageResource);
            events.put(new JSONObject().put("time_ms",entry.time).put("tag",tag).put("event",name).put("error",entry.error));
        }
        out.put("events",events);
        if(out.toString().getBytes("UTF-8").length>65536)throw new java.io.IOException();
        return out;
    }
}
