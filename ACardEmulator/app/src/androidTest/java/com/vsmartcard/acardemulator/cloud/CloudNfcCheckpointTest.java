package com.vsmartcard.acardemulator.cloud;

import android.content.Context;
import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;
import static org.junit.Assert.*;

/** Explicit loopback laboratory fixture; restores the user's encrypted pairing afterwards. */
public class CloudNfcCheckpointTest {
    @Test public void realHceThroughLocalCheckpointGateway() throws Exception {
        android.os.Bundle args=InstrumentationRegistry.getArguments();
        String server=args.getString("server");org.junit.Assume.assumeTrue(server!=null);
        assertTrue(server.matches("https://127\\.0\\.0\\.1:[0-9]+"));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        DeviceVault vault=new DeviceVault(context);JSONObject original=vault.load();
        CloudService saved=CloudService.current;String oldMode=saved==null?null:saved.mode;
        KeyStore store=KeyStore.getInstance(KeyStore.getDefaultType());store.load(null);
        store.setCertificateEntry("loopback-lab",CertificateFactory.getInstance("X.509").generateCertificate(
            new ByteArrayInputStream(android.util.Base64.decode(args.getString("ca"),android.util.Base64.DEFAULT))));
        TrustManagerFactory trust=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());trust.init(store);
        SSLContext tls=SSLContext.getInstance("TLS");tls.init(null,trust.getTrustManagers(),null);
        CloudActivity activity=null;
        SSLSocketFactory previous=HttpsURLConnection.getDefaultSSLSocketFactory();
        try {
            context.stopService(new Intent(context,CloudService.class));
            long deadline=android.os.SystemClock.elapsedRealtime()+5000;
            while(CloudService.current!=null&&android.os.SystemClock.elapsedRealtime()<deadline)Thread.sleep(50);
            assertNull(CloudService.current);
            HttpsURLConnection.setDefaultSSLSocketFactory(tls.getSocketFactory());
            vault.save(server,args.getString("token"));
            context.startForegroundService(new Intent(context,CloudService.class).putExtra("mode","card"));
            deadline=android.os.SystemClock.elapsedRealtime()+20000;
            while(!CloudService.connected()&&android.os.SystemClock.elapsedRealtime()<deadline)Thread.sleep(100);
            assertTrue("laboratory gateway connection",CloudService.connected());
            activity=(CloudActivity)InstrumentationRegistry.getInstrumentation().startActivitySync(
                new Intent(context,CloudActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            android.nfc.NfcAdapter adapter=android.nfc.NfcAdapter.getDefaultAdapter(context);
            final CloudActivity foreground=activity;
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->foreground.getWindow()
                .addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
            final boolean[] preferred={false};
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->preferred[0]=android.nfc.cardemulation.CardEmulation.getInstance(adapter)
                .setPreferredService(foreground,new android.content.ComponentName(context,CloudHceService.class)));
            assertTrue("Phoenix Cloud HCE foreground selection",preferred[0]);
            new CloudApi(server,args.getString("token")).post("lab-ready",new JSONObject());
            System.out.println("PQC_PHONE_READY");
            long end=android.os.SystemClock.elapsedRealtime()+300000;
            while(android.os.SystemClock.elapsedRealtime()<end){
                if(new CloudApi(server,args.getString("token")).post("lab-result",new JSONObject()).optBoolean("complete"))return;
                Thread.sleep(500);
            }
            fail("hardware test did not finish within laboratory window");
        } finally {
            if(activity!=null){final CloudActivity closing=activity;InstrumentationRegistry.getInstrumentation().runOnMainSync(closing::finish);}
            context.stopService(new Intent(context,CloudService.class));
            long deadline=android.os.SystemClock.elapsedRealtime()+5000;
            while(CloudService.current!=null&&android.os.SystemClock.elapsedRealtime()<deadline)Thread.sleep(50);
            if(original==null)vault.clear();else vault.save(original.getString("server"),original.getString("token"));
            HttpsURLConnection.setDefaultSSLSocketFactory(previous);
            if(oldMode!=null&&original!=null)context.startForegroundService(new Intent(context,CloudService.class).putExtra("mode",oldMode));
        }
    }
}
