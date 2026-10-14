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
    static volatile int statusResource=R.string.cloud_not_connected;
    final NfcStatus nfcStatus=new NfcStatus();
    final PhoneReader reader=new PhoneReader(nfcStatus);
    volatile String mode="card";
    CloudApi api;
    private ScheduledExecutorService worker;
    private volatile long lastBeat;
    private long lastInventory,lastLanguageAttempt;
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
        CloudLog.add("CLOUD",R.string.cloud_log_connecting,false);
        try{
            JSONObject saved=new DeviceVault(this).load();if(saved==null)throw new IllegalStateException();
            api=new CloudApi(saved.getString("server"),saved.getString("token"));
            NotificationManager manager=getSystemService(NotificationManager.class);
            if(android.os.Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel("phoenix-cloud","Phoenix Cloud",NotificationManager.IMPORTANCE_LOW));
            PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,CloudActivity.class),PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder notification=android.os.Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"phoenix-cloud"):new Notification.Builder(this);
            startForeground(4701,notification.setContentTitle(CloudLanguage.wrap(this).getString(R.string.cloud_notification_title))
                .setContentText(CloudLanguage.wrap(this).getString(R.string.cloud_notification_body))
                .setSmallIcon(R.drawable.cloud_notification).setContentIntent(open).setOngoing(true).build());
            current=this;worker=Executors.newSingleThreadScheduledExecutor();
            worker.scheduleWithFixedDelay(this::tick,0,500,TimeUnit.MILLISECONDS);
        }catch(Exception e){statusResource=R.string.cloud_pair_unavailable;stopSelf();}
        return START_NOT_STICKY;
    }
    private void tick(){
        try{
            long now=android.os.SystemClock.elapsedRealtime();
            NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);boolean enabled=nfc!=null&&nfc.isEnabled();
            android.content.SharedPreferences preferences=getSharedPreferences("phoenix-ui",0);
            if(preferences.getBoolean("language_dirty",false)&&(lastLanguageAttempt==0||now-lastLanguageAttempt>=30000)){
                lastLanguageAttempt=now;String selected=CloudLanguage.language(this);
                try{
                    api.post("preferences",new JSONObject().put("language",selected));
                    if(selected.equals(preferences.getString("language","")))preferences.edit().putBoolean("language_dirty",false).apply();
                }catch(CloudApi.Denied denied){throw denied;}catch(Exception unavailable){}
            }
            if(now-lastBeat>=5000||lastBeat==0){lastBeat=now;
                api.heartbeat(new JSONObject().put("reader_connected","card".equals(mode)&&enabled)
                    .put("device_kind","android").put("mode",mode).put("nfc_enabled",enabled));
                if(!connected())CloudLog.add("CLOUD",R.string.cloud_log_connected,false);
                lastContact=android.os.SystemClock.elapsedRealtime();
                statusResource=R.string.cloud_reachable;
                CloudApi.Account account=api.account;
                if(account!=null&&account.language!=null&&!preferences.getBoolean("language_dirty",false)
                    &&!account.language.equals(preferences.getString("language","")))
                    preferences.edit().putString("language",account.language).apply();
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
        }catch(CloudApi.Denied e){lastContact=0;statusResource=R.string.cloud_revoke_notice;new DeviceVault(this).clear();stopSelf();}
        catch(Exception e){if(lastContact>0)CloudLog.add("CLOUD",R.string.cloud_log_disconnected,true);lastContact=0;nfcStatus.ended();statusResource=R.string.cloud_retry_notice;}
    }
    private void inventory() throws Exception {
        JSONArray list=new JSONArray();
        list.put(new JSONObject().put("id",PhoneReader.ID).put("name","Android NFC-Leser"));
        reader.selection(api.post("reader-inventory",new JSONObject().put("instance",instance).put("readers",list)));
    }
    @Override public void onDestroy(){
        CloudLog.add("CLOUD",R.string.cloud_log_stopped,false);
        current=null;if(worker!=null)worker.shutdownNow();reader.detach();
        if(api!=null)new Thread(api::end,"phoenix-close").start();
        statusResource=R.string.cloud_not_connected;stopForeground(true);super.onDestroy();
    }
}
