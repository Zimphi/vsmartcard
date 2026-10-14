package com.vsmartcard.acardemulator.cloud;

import android.app.Service;
import ACardEmulator.R;
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
    final NfcStatus nfcStatus=new NfcStatus();
    final PhoneReader reader=new PhoneReader(nfcStatus);
    volatile String mode="card";
    CloudApi api;
    private ScheduledExecutorService worker;
    private volatile long lastBeat;
    private long lastInventory;
    private volatile long lastContact;
    static boolean connectionFresh(boolean running,long confirmed,long now){
        return running&&confirmed>0&&now>=confirmed&&now-confirmed<15000;
    }
    static boolean connected(){
        CloudService service=current;
        return service!=null&&connectionFresh(true,service.lastContact,android.os.SystemClock.elapsedRealtime());
    }
    private final String instance=UUID.randomUUID().toString();
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent i,int flags,int startId){
        if(i==null){stopSelf();return START_NOT_STICKY;}
        String requested=i.getStringExtra("mode");
        if(!"card".equals(requested)&&!"reader".equals(requested)){stopSelf();return START_NOT_STICKY;}
        if(current==this&&worker!=null){if(!mode.equals(requested)){reader.detach();nfcStatus.reset();api.profile=null;worker.execute(api::end);}mode=requested;lastBeat=0;return START_NOT_STICKY;}
        mode=requested;
        CloudLog.add("CLOUD","Verbindung wird hergestellt",false);
        try{
            JSONObject saved=new DeviceVault(this).load();if(saved==null)throw new IllegalStateException();
            api=new CloudApi(saved.getString("server"),saved.getString("token"));
            NotificationManager manager=getSystemService(NotificationManager.class);
            if(android.os.Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel("phoenix-cloud","Phoenix Cloud",NotificationManager.IMPORTANCE_LOW));
            PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,CloudActivity.class),PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder notification=android.os.Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"phoenix-cloud"):new Notification.Builder(this);
            startForeground(4701,notification.setContentTitle("Phoenix Cloud ist aktiv")
                .setContentText("NFC-Karte oder NFC-Leser · Verbindung in der App aufheben")
                .setSmallIcon(R.drawable.cloud_notification).setContentIntent(open).setOngoing(true).build());
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
                if(!connected())CloudLog.add("CLOUD","Cloud-Verbindung bestätigt",false);
                lastContact=android.os.SystemClock.elapsedRealtime();
                status="Cloud erreichbar";
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
        }catch(CloudApi.Denied e){lastContact=0;status="Gerätefreigabe widerrufen · erneut koppeln";new DeviceVault(this).clear();stopSelf();}
        catch(Exception e){if(lastContact>0)CloudLog.add("CLOUD","Cloud-Verbindung unterbrochen",true);lastContact=0;nfcStatus.ended();status="Offline · Verbindung wird erneut geprüft";}
    }
    private void inventory() throws Exception {
        JSONArray list=new JSONArray();
        list.put(new JSONObject().put("id",PhoneReader.ID).put("name","Android NFC-Leser"));
        reader.selection(api.post("reader-inventory",new JSONObject().put("instance",instance).put("readers",list)));
    }
    @Override public void onDestroy(){
        CloudLog.add("CLOUD","Verbindung beendet",false);
        current=null;if(worker!=null)worker.shutdownNow();reader.detach();
        if(api!=null)new Thread(api::end,"phoenix-close").start();
        status="Nicht verbunden";stopForeground(true);super.onDestroy();
    }
}
