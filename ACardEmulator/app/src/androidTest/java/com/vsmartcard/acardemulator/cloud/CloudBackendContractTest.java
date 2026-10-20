package com.vsmartcard.acardemulator.cloud;

import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;

/** Explicit local fixture only: never loads or changes the user's saved pairing. */
public class CloudBackendContractTest {
    @Test public void realHttpsBackendAcceptsAndroidContract() throws Exception {
        android.os.Bundle args=InstrumentationRegistry.getArguments();
        String server=args.getString("server");
        org.junit.Assume.assumeTrue(server!=null);
        assertTrue(server.matches("https://127\\.0\\.0\\.1:[0-9]+"));
        byte[] certificate=android.util.Base64.decode(args.getString("ca"),android.util.Base64.DEFAULT);
        KeyStore store=KeyStore.getInstance(KeyStore.getDefaultType());store.load(null);
        store.setCertificateEntry("local-fixture",CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certificate)));
        TrustManagerFactory trust=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());trust.init(store);
        SSLContext tls=SSLContext.getInstance("TLS");tls.init(null,trust.getTrustManagers(),null);
        SSLSocketFactory original=HttpsURLConnection.getDefaultSSLSocketFactory();
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(tls.getSocketFactory());
            CloudApi api=new CloudApi(server,args.getString("token"));
            JSONObject heartbeat=new JSONObject().put("reader_connected",true).put("device_kind","android").put("mode","card").put("nfc_enabled",true);
            api.heartbeat(heartbeat);
            assertNotNull(api.profile);assertEquals("synthetic-eid",api.profile.id);assertEquals("eid",api.profile.kind);assertEquals(1,api.profile.version);
            assertNotNull(api.account);assertEquals("Synthetic Test",api.account.name);
            for(String language:new String[]{"en","de"}) {
                api.post("preferences",new JSONObject().put("language",language));api.heartbeat(heartbeat);
                assertEquals(language,api.account.language);
            }
            api.heartbeat(heartbeat.put("mode","reader").put("reader_connected",false));
            api.heartbeat(heartbeat.put("mode","card").put("reader_connected",true).put("nfc_enabled",false));
            JSONObject report=new JSONObject().put("consent",true).put("passed",true)
                .put("test_id","33333333-3333-4333-8333-333333333333").put("manufacturer","Synthetic").put("model","android-client-fixture")
                .put("android_release","16").put("api_level",36).put("app_version","contract").put("mode","reader")
                .put("source","phone-reader").put("attempts",1).put("successes",1)
                .put("kind","eid").put("checks",new org.json.JSONArray().put("application_selection").put("read_card_access"))
                .put("security_patch","2026-10-01").put("build_id","android-fixture").put("tested_at_ms",System.currentTimeMillis());
            api.post("compatibility",report);api.post("compatibility",report);
            JSONObject context=new JSONObject();for(String key:DiagnosticEvidence.DEVICE)context.put(key,report.get(key));
            org.json.JSONArray evidence=api.post("compatibility-status",context).getJSONArray("reports");
            assertEquals(1,evidence.length());assertEquals("reader",evidence.getJSONObject(0).getString("mode"));
            context.put("build_id","different-build");assertEquals(0,api.post("compatibility-status",context).getJSONArray("reports").length());
            CloudApi revoke=new CloudApi(server,args.getString("revoke_token"));revoke.revoke();revoke.revoke();
            try{revoke.heartbeat(heartbeat);fail("revoked token connected");}catch(CloudApi.Denied denied){assertTrue(denied.deviceDenied);}
            try{new CloudApi(server+"/gateway",null).revoke();fail("gateway error confirmed revocation");}catch(CloudApi.Denied denied){assertFalse(denied.deviceDenied);}
            try {api.post("compatibility",report.put("consent",false));fail("unconfirmed report accepted");}
            catch(java.io.IOException expected) {}
            android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
            android.content.Context target=instrumentation.getTargetContext();DeviceVault vault=new DeviceVault(target);JSONObject saved=vault.load();
            android.content.SharedPreferences preferences=target.getSharedPreferences("phoenix-ui",0);
            String language=preferences.getString("language",null);boolean dirty=preferences.getBoolean("language_dirty",false);
            android.app.Activity foreground=instrumentation.startActivitySync(new android.content.Intent(target,CloudActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            try {
                vault.save(server,args.getString("token"));
                android.content.Intent start=new android.content.Intent(target,CloudService.class).putExtra("mode","card");
                if(android.os.Build.VERSION.SDK_INT>=26)target.startForegroundService(start);else target.startService(start);
                awaitConnection(true,10000);
                api.post("fixture-outage",new JSONObject());
                awaitConnection(false,10000);awaitConnection(true,20000);
            }finally{
                target.stopService(new android.content.Intent(target,CloudService.class));
                long until=android.os.SystemClock.elapsedRealtime()+10000;while(CloudService.current!=null&&android.os.SystemClock.elapsedRealtime()<until)android.os.SystemClock.sleep(20);
                if(saved==null)vault.clear();else vault.save(saved.getString("server"),saved.getString("token"));
                android.content.SharedPreferences.Editor edit=preferences.edit().putBoolean("language_dirty",dirty);if(language==null)edit.remove("language");else edit.putString("language",language);edit.commit();
                instrumentation.runOnMainSync(foreground::finish);
            }
            try {new CloudApi(server,"BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB").heartbeat(heartbeat);fail("invalid token accepted");}
            catch(CloudApi.Denied expected) {}
        } finally {HttpsURLConnection.setDefaultSSLSocketFactory(original);}
    }
    private static void awaitConnection(boolean expected,long timeout){
        long until=android.os.SystemClock.elapsedRealtime()+timeout;
        while(CloudService.connected()!=expected&&android.os.SystemClock.elapsedRealtime()<until)android.os.SystemClock.sleep(20);
        assertEquals("connection state did not recover as expected",expected,CloudService.connected());
    }

}
