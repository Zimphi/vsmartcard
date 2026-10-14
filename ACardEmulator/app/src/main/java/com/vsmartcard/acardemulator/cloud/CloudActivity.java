package com.vsmartcard.acardemulator.cloud;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import ACardEmulator.R;
import ACardEmulator.BuildConfig;
import android.widget.TextView;
import android.widget.RadioGroup;
import org.json.JSONObject;

public final class CloudActivity extends Activity {
    private static final int PAIRING_REQUEST=4703;
    private final Handler handler=new Handler();
    private TextView state,server,pairingStatus,statusMark;
    private Button connectionAction;
    private ImageView connectionIcon;
    private RadioGroup roles;
    private final int cardChoiceId=R.id.cloud_card_mode, readerChoiceId=R.id.cloud_reader_mode;
    private String mode="card";
    private boolean readerEnabled,cardPreferred;
    private CloudLog.Entry renderedLog;
    private final Runnable refresh=new Runnable(){public void run(){
        refreshConnection();handler.postDelayed(this,1000);
    }};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.phoenix_cloud);
        findViewById(R.id.cloud_logo).setOnClickListener(v->startActivity(new Intent(this,CloudInfoActivity.class)));
        state=findViewById(R.id.cloud_state);server=findViewById(R.id.cloud_server);
        pairingStatus=findViewById(R.id.cloud_pairing_status);statusMark=findViewById(R.id.cloud_status_mark);
        connectionAction=findViewById(R.id.cloud_connection_action);connectionIcon=findViewById(R.id.cloud_connection_icon);
        findViewById(R.id.cloud_manage_pairing).setOnClickListener(v->startActivityForResult(new Intent(this,CloudPairingActivity.class),PAIRING_REQUEST));
        ((TextView)findViewById(R.id.cloud_version)).setText(getString(R.string.cloud_version,BuildConfig.VERSION_NAME));
        roles=findViewById(R.id.cloud_roles);
        roles.check(cardChoiceId);roles.setOnCheckedChangeListener((group,id)->{mode=id==cardChoiceId?"card":"reader";if(CloudService.current!=null)connect();});
        connectionAction.setOnClickListener(v->{if(CloudService.connected())disconnect();else connect();});
        refreshConnection();handleIntent(getIntent());
    }
    private void refreshConnection(){
        JSONObject saved=null;
        try{saved=new DeviceVault(this).load();}catch(Exception ignored){}
        server.setText(saved==null?getString(R.string.cloud_not_paired):saved.optString("server"));
        renderConnection(CloudService.connected(),saved!=null);
        state.setText(CloudService.status);state.setVisibility(CloudService.connected()?android.view.View.GONE:android.view.View.VISIBLE);
        NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);
        CloudService service=CloudService.current;
        boolean enabled=nfc!=null&&nfc.isEnabled();
        boolean ready=service!=null&&enabled&&("reader".equals(mode)?readerEnabled:cardPreferred);
        renderDetails(CloudService.connected(),nfc!=null,enabled,ready,
            service==null||service.api==null?null:service.api.profile,
            service!=null&&service.reader.detected(),service==null?NfcStatus.State.WAITING:service.nfcStatus.state());
        renderLog();
    }
    void renderDetails(boolean online,boolean supported,boolean enabled,boolean ready,CloudApi.Profile profile,boolean cardPresent,NfcStatus.State traffic){
        boolean reading="reader".equals(mode);
        TextView nfc=findViewById(R.id.cloud_nfc_status);
        int nfcText=!supported?R.string.cloud_nfc_missing:!enabled?R.string.cloud_nfc_off:!online?R.string.cloud_nfc_disconnected
            :!ready?R.string.cloud_nfc_unavailable:reading?R.string.cloud_nfc_reader_ready
            :profile==null||!profile.active?R.string.cloud_nfc_no_profile:R.string.cloud_nfc_card_ready;
        nfc.setText(nfcText);nfc.setTextColor(getColor(!supported||!enabled?R.color.cloud_red:R.color.cloud_muted));
        int communication=R.string.cloud_traffic_idle;
        if(online&&enabled)switch(traffic){
            case TRANSFER:communication=R.string.cloud_traffic_busy;break;
            case SESSION:communication=R.string.cloud_traffic_active;break;
            case ENDED:communication=R.string.cloud_traffic_ended;break;
            case FAILED:communication=R.string.cloud_traffic_failed;break;
            default:break;
        }
        TextView transmission=findViewById(R.id.cloud_traffic_status);transmission.setText(communication);
        transmission.setTextColor(getColor(communication==R.string.cloud_traffic_failed?R.color.cloud_red:communication==R.string.cloud_traffic_busy?R.color.cloud_green:R.color.cloud_muted));
        ((TextView)findViewById(R.id.cloud_profile_title)).setText(reading?R.string.cloud_reader_card_title:R.string.cloud_profile_title);
        TextView selected=findViewById(R.id.cloud_profile_status);
        if(reading)selected.setText(cardPresent?R.string.cloud_reader_detected:R.string.cloud_reader_waiting);
        else if(!online)selected.setText(R.string.cloud_profile_offline);
        else if(profile==null)selected.setText(R.string.cloud_profile_unknown);
        else if(!profile.active)selected.setText(R.string.cloud_profile_none);
        else if(profile.id==null)selected.setText(R.string.cloud_profile_legacy);
        else selected.setText(getString(R.string.cloud_profile_detail,"eid".equals(profile.kind)?"eID":"ePassport",profile.id,profile.version));
    }
    private void renderLog(){
        boolean developer=getSharedPreferences("phoenix-ui",MODE_PRIVATE).getBoolean("developer_options",false);
        findViewById(R.id.cloud_log_panel).setVisibility(developer?android.view.View.VISIBLE:android.view.View.GONE);
        if(!developer)return;
        java.util.List<CloudLog.Entry> entries=CloudLog.snapshot();
        CloudLog.Entry last=entries.isEmpty()?null:entries.get(entries.size()-1);
        if(last==renderedLog)return;renderedLog=last;
        android.text.SpannableStringBuilder text=new android.text.SpannableStringBuilder();
        java.text.SimpleDateFormat clock=new java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.GERMANY);
        for(CloudLog.Entry entry:entries){
            int start=text.length();text.append(clock.format(new java.util.Date(entry.time))).append(" [").append(entry.tag).append("] ");
            int color=entry.error?R.color.cloud_red:"NFC".equals(entry.tag)?R.color.cloud_green:"CLOUD".equals(entry.tag)?R.color.cloud_blue:R.color.cloud_log_profile;
            text.setSpan(new android.text.style.ForegroundColorSpan(getColor(color)),start,text.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),start,text.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.append(entry.message).append("\n");
        }
        ((TextView)findViewById(R.id.cloud_log_text)).setText(text.length()==0?getString(R.string.cloud_log_empty):text);
        android.widget.ScrollView scroll=findViewById(R.id.cloud_log_scroll);scroll.post(()->scroll.fullScroll(android.view.View.FOCUS_DOWN));
    }
    void renderConnection(boolean online,boolean paired){
        int color=getColor(online?R.color.cloud_green:R.color.cloud_red);
        statusMark.setText(online?"✓":"✕");statusMark.setTextColor(color);
        pairingStatus.setText(online?R.string.cloud_connected:paired?R.string.cloud_saved_offline:R.string.cloud_unpaired);
        pairingStatus.setTextColor(color);
        connectionIcon.setImageResource(online?R.drawable.cloud_connected:R.drawable.cloud_disconnected);
        connectionIcon.setContentDescription(getString(online?R.string.cloud_connected_icon:R.string.cloud_disconnected_icon));
        connectionAction.setText(online?R.string.cloud_disconnect:R.string.cloud_connect);
    }
    private void disconnect(){
        stopService(new Intent(this,CloudService.class));disableReader();
        NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);
        if(nfc!=null)android.nfc.cardemulation.CardEmulation.getInstance(nfc).unsetPreferredService(this);
        show("Nicht verbunden");
    }
    private void show(String message){CloudService.status=message;state.setText(message);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    private void handleIntent(Intent i){
        if(i==null)return;
        if(i.getData()!=null){android.net.Uri uri=i.getData();i.setData(null);startActivityForResult(new Intent(this,CloudPairingActivity.class).setData(uri),PAIRING_REQUEST);}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PAIRING_REQUEST&&result==RESULT_OK)connect();
    }
    private void connect(){
        try{
            JSONObject saved=new DeviceVault(this).load();
            if(saved==null){startActivityForResult(new Intent(this,CloudPairingActivity.class),PAIRING_REQUEST);return;}
            server.setText(saved.getString("server"));
            show("Verbindung wird hergestellt …");
            Intent start=new Intent(this,CloudService.class).putExtra("mode",mode);
            if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(start);else startService(start);
            if("reader".equals(mode))enableReader();else disableReader();
            preferCardService();
        }catch(Exception e){show("Gespeicherte Kopplung nicht verfügbar. Bitte erneut koppeln.");}
    }
    private void enableReader(){
        NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);if(nfc==null)return;
        nfc.enableReaderMode(this,tag->{
            CloudService service=CloudService.current;
            if(service==null||!"reader".equals(service.mode))return;
            try{service.reader.attach(tag);runOnUiThread(()->show("NFC-Karte erkannt"));}
            catch(Exception e){service.reader.detach();runOnUiThread(()->show("NFC-Karte nicht erreichbar · erneut auflegen"));}
        },NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,null);
        readerEnabled=true;
    }
    private void disableReader(){readerEnabled=false;NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);if(nfc!=null)nfc.disableReaderMode(this);}
    private void preferCardService(){
        NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);if(nfc==null)return;
        android.nfc.cardemulation.CardEmulation cards=android.nfc.cardemulation.CardEmulation.getInstance(nfc);
        if("card".equals(mode))cardPreferred=cards.setPreferredService(this,new android.content.ComponentName(this,CloudHceService.class));
        else {cardPreferred=false;cards.unsetPreferredService(this);}
    }
    @Override protected void onResume(){super.onResume();
        CloudService service=CloudService.current;if(service!=null){mode=service.mode;roles.check("reader".equals(mode)?readerChoiceId:cardChoiceId);if("reader".equals(mode))enableReader();}
        preferCardService();handler.post(refresh);
    }
    @Override protected void onPause(){cardPreferred=false;handler.removeCallbacks(refresh);disableReader();
        NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);if(nfc!=null)android.nfc.cardemulation.CardEmulation.getInstance(nfc).unsetPreferredService(this);
        CloudService service=CloudService.current;if(service!=null&&"reader".equals(service.mode))service.reader.detach();super.onPause();}
}
