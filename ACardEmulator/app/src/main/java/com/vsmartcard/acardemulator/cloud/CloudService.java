package com.vsmartcard.acardemulator.cloud;

import android.app.Service;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.IBinder;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class CloudService extends Service {
    static volatile CloudService current;
    static volatile String status="Nicht verbunden";
    final PhoneReader reader=new PhoneReader();
    volatile String mode="card";
    CloudApi api;
    private ScheduledExecutorService worker;
    private long lastBeat,lastInventory;
    private final String instance=UUID.randomUUID().toString();
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent i,int flags,int startId){
        if(i==null){stopSelf();return START_NOT_STICKY;}
        String requested=i.getStringExtra("mode");
        if(!"card".equals(requested)&&!"reader".equals(requested)){stopSelf();return START_NOT_STICKY;}
        if(current==this&&worker!=null){if(!mode.equals(requested)){reader.detach();worker.execute(api::end);}mode=requested;return START_NOT_STICKY;}
        mode=requested;
        try{
            JSONObject saved=new DeviceVault(this).load();if(saved==null)throw new IllegalStateException();
            api=new CloudApi(saved.getString("server"),saved.getString("token"));
            NotificationManager manager=getSystemService(NotificationManager.class);
            if(android.os.Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel("phoenix-cloud","Phoenix Cloud",NotificationManager.IMPORTANCE_LOW));
            PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,CloudActivity.class),PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder notification=android.os.Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"phoenix-cloud"):new Notification.Builder(this);
            startForeground(4701,notification.setContentTitle("Phoenix Cloud ist aktiv")
                .setContentText("NFC-Karte oder NFC-Leser · Verbindung in der App beenden")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentIntent(open).setOngoing(true).build());
            current=this;worker=Executors.newSingleThreadScheduledExecutor();
            worker.scheduleWithFixedDelay(this::tick,0,500,TimeUnit.MILLISECONDS);
        }catch(Exception e){status="Kopplung nicht verfügbar";stopSelf();}
        return START_NOT_STICKY;
    }
    private void tick(){
        try{
            long now=android.os.SystemClock.elapsedRealtime();
            NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);boolean enabled=nfc!=null&&nfc.isEnabled();
            if(now-lastBeat>=5000||lastBeat==0){lastBeat=now;
                api.heartbeat(new JSONObject().put("reader_connected","card".equals(mode)&&enabled)
                    .put("device_kind","android").put("mode",mode).put("nfc_enabled",enabled));
                status=enabled?("card".equals(mode)?"Online · Kartenmodus":"Online · Lesemodus") :"Online · NFC ausgeschaltet";
            }
            if("reader".equals(mode)){
                if(now-lastInventory>=5000||lastInventory==0){lastInventory=now;inventory();}
                JSONObject packet=api.post("reader-poll",new JSONObject());
                if(!packet.isNull("job")){
                    JSONObject job=packet.getJSONObject("job");inventory();JSONObject result;
                    try{result=reader.execute(job.getJSONObject("payload"));}
                    catch(Exception e){reader.detach();result=new JSONObject().put("error","reader_failed");}
                    api.post("reader-result",new JSONObject().put("id",job.getString("id")).put("result",result));
                }
            }
        }catch(CloudApi.Denied e){status="Gerätefreigabe widerrufen · erneut koppeln";new DeviceVault(this).clear();stopSelf();}
        catch(Exception e){status="Offline · Verbindung wird erneut geprüft";}
    }
    private void inventory() throws Exception {
        JSONArray list=new JSONArray();
        list.put(new JSONObject().put("id",PhoneReader.ID).put("name","Android NFC-Leser"));
        reader.selection(api.post("reader-inventory",new JSONObject().put("instance",instance).put("readers",list)));
    }
    @Override public void onDestroy(){
        current=null;if(worker!=null)worker.shutdownNow();reader.detach();
        if(api!=null)new Thread(api::end,"phoenix-close").start();
        status="Nicht verbunden";stopForeground(true);super.onDestroy();
    }
}
