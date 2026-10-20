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
                .put("source","phone-reader").put("attempts",1).put("successes",1);
            api.post("compatibility",report);api.post("compatibility",report);
            try {api.post("compatibility",report.put("consent",false));fail("unconfirmed report accepted");}
            catch(java.io.IOException expected) {}
            try {new CloudApi(server,"BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB").heartbeat(heartbeat);fail("invalid token accepted");}
            catch(CloudApi.Denied expected) {}
        } finally {HttpsURLConnection.setDefaultSSLSocketFactory(original);}
    }
}
