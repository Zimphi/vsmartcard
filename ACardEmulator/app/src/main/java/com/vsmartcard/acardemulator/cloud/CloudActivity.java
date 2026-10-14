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
    private final Runnable refresh=new Runnable(){public void run(){
        refreshConnection();handler.postDelayed(this,1000);
    }};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.phoenix_cloud);
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
        state.setText(CloudService.status);
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
    }
    private void disableReader(){NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);if(nfc!=null)nfc.disableReaderMode(this);}
    private void preferCardService(){
        NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);if(nfc==null)return;
        android.nfc.cardemulation.CardEmulation cards=android.nfc.cardemulation.CardEmulation.getInstance(nfc);
        if("card".equals(mode))cards.setPreferredService(this,new android.content.ComponentName(this,CloudHceService.class));
        else cards.unsetPreferredService(this);
    }
    @Override protected void onResume(){super.onResume();
        CloudService service=CloudService.current;if(service!=null){mode=service.mode;roles.check("reader".equals(mode)?readerChoiceId:cardChoiceId);if("reader".equals(mode))enableReader();}
        preferCardService();handler.post(refresh);
    }
    @Override protected void onPause(){handler.removeCallbacks(refresh);disableReader();
        NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);if(nfc!=null)android.nfc.cardemulation.CardEmulation.getInstance(nfc).unsetPreferredService(this);
        CloudService service=CloudService.current;if(service!=null&&"reader".equals(service.mode))service.reader.detach();super.onPause();}
}
