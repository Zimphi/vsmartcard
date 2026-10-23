// SPDX-License-Identifier: GPL-3.0-or-later
// Phoenix Cloud extension; license notice added 2026-10-23. See LICENSE_REVIEW.md.
package com.vsmartcard.acardemulator.cloud;
import android.content.Context;
import android.os.Build;
import ACardEmulator.BuildConfig;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;
/** Private, consent-created requests for the explicitly authorized USB/ADB companion. */
final class LabControl {
    private final Context context;
    private String observed;private long lastSample;
    LabControl(Context context){this.context=context;}
    static JSONObject device() throws Exception {
        return new JSONObject().put("manufacturer",Build.MANUFACTURER).put("model",Build.MODEL)
            .put("android_release",Build.VERSION.RELEASE).put("api_level",Build.VERSION.SDK_INT).put("app_version",BuildConfig.VERSION_NAME)
            .put("security_patch",Build.VERSION.SECURITY_PATCH.isEmpty()?"unknown":Build.VERSION.SECURITY_PATCH).put("build_id",Build.ID.isEmpty()?"unknown":Build.ID);
    }
    private void write(String name,JSONObject value) throws Exception {
        File temp=new File(context.getCacheDir(),name+".tmp"),target=new File(context.getCacheDir(),name);
        try(FileOutputStream out=new FileOutputStream(temp)){out.write(value.toString().getBytes("UTF-8"));out.getFD().sync();}
        if(!temp.renameTo(target))throw new java.io.IOException();
    }
    private JSONObject read(String name) throws Exception {
        File file=new File(context.getCacheDir(),name);if(!file.exists())return null;
        if(file.length()>65536)throw new java.io.IOException();
        try(FileInputStream in=new FileInputStream(file);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
            byte[] b=new byte[4096];int n;while((n=in.read(b))>=0){if(out.size()+n>65536)throw new java.io.IOException();out.write(b,0,n);}
            return new JSONObject(out.toString("UTF-8"));
        }
    }
    String request(String mode,String kind,int attempts) throws Exception {
        if(!("usb-profile".equals(mode)||"cloud-eac".equals(mode)||"baseline".equals(mode))||!("eid".equals(kind)||"epassport".equals(kind))||attempts<1||attempts>("baseline".equals(mode)?120:30))throw new IllegalArgumentException();
        String id=UUID.randomUUID().toString();
        write("lab-request.json",new JSONObject().put("format","phoenix-lab-request-v1").put("id",id).put("consent",true)
            .put("mode",mode).put("kind",kind).put("attempts",attempts).put("requested_at_ms",System.currentTimeMillis()).put("device",device()));
        return id;
    }
    void cancel() throws Exception {JSONObject request=read("lab-request.json");if(request!=null)write("lab-cancel.json",new JSONObject().put("id",request.getString("id")));}
    JSONObject progress() throws Exception {
        JSONObject request=read("lab-request.json"),value=read("lab-progress.json");
        if(request==null||value==null||!"phoenix-lab-progress-v1".equals(value.optString("format"))||!request.getString("id").equals(value.optString("id")))return null;
        if(!request.getJSONObject("device").getString("app_version").equals(BuildConfig.VERSION_NAME))return null;
        String id=value.getString("id");if(!id.equals(observed)){observed=id;lastSample=0;}
        JSONArray samples=value.optJSONArray("samples");
        if(samples!=null&&samples.length()<=200)for(int i=0;i<samples.length();i++){
            JSONObject s=samples.getJSONObject(i);long seq=s.getLong("sequence");double ms=s.getDouble("duration_ms");
            if(seq>lastSample&&Double.isFinite(ms)&&ms>=0&&ms<=600000){NfcQuality.record("mac-pcsc",ms,s.getBoolean("success"));lastSample=seq;}
        }
        value.put("request",request);return value;
    }
    static JSONObject successfulReport(JSONObject progress) throws Exception {
        if(!"complete".equals(progress.optString("phase"))||!progress.optBoolean("passed")||!progress.optBoolean("profile_verified"))return null;
        JSONObject request=progress.getJSONObject("request");String source=request.getString("mode");
        if(!"usb-profile".equals(source)&&!"cloud-eac".equals(source))return null;
        int attempts=request.getInt("attempts");if(progress.optInt("successes")!=attempts)return null;
        JSONObject metadata=request.getJSONObject("device"),report=new JSONObject();
        for(String key:new String[]{"manufacturer","model","android_release","api_level","app_version"})report.put(key,metadata.get(key));
        if(progress.has("checks")&&metadata.has("security_patch")&&metadata.has("build_id")){
            report.put("kind",request.getString("kind")).put("checks",progress.getJSONArray("checks"))
                .put("security_patch",metadata.getString("security_patch")).put("build_id",metadata.getString("build_id"))
                .put("tested_at_ms",progress.optLong("completed_at_ms",request.getLong("requested_at_ms")));
        }
        return report.put("test_id",request.getString("id"))
            .put("mode","card").put("source",source).put("attempts",attempts).put("successes",attempts).put("passed",true);
    }
}
