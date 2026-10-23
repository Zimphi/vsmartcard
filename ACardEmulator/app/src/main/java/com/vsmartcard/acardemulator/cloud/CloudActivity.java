// SPDX-License-Identifier: GPL-3.0-or-later
// Phoenix Cloud extension; license notice added 2026-10-23. See LICENSE_REVIEW.md.
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

public final class CloudActivity extends CloudScreen {
    private static final int PAIRING_REQUEST=4703;
    private final Handler handler=new Handler();
    private TextView state,pairingStatus;
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
        findViewById(R.id.cloud_help).setOnClickListener(v->startActivity(new Intent(this,CloudHelpActivity.class)));
        ((TextView)findViewById(R.id.cloud_language_button)).setText(CloudLanguage.language(this).toUpperCase(java.util.Locale.ROOT));
        findViewById(R.id.cloud_language_button).setOnClickListener(v->chooseLanguage());
        findViewById(R.id.cloud_logo).setOnClickListener(v->startActivity(new Intent(this,CloudInfoActivity.class)));
        state=findViewById(R.id.cloud_state);
        pairingStatus=findViewById(R.id.cloud_pairing_status);
        connectionAction=findViewById(R.id.cloud_connection_action);connectionIcon=findViewById(R.id.cloud_connection_icon);
        findViewById(R.id.cloud_manage_pairing).setOnClickListener(v->startActivityForResult(new Intent(this,CloudPairingActivity.class).putExtra("manage_pairing",true),PAIRING_REQUEST));
        ((TextView)findViewById(R.id.cloud_version)).setText(getString(R.string.cloud_version,BuildConfig.VERSION_NAME));
        roles=findViewById(R.id.cloud_roles);
        roles.check(cardChoiceId);roles.setOnCheckedChangeListener((group,id)->{mode=id==cardChoiceId?"card":"reader";if(CloudService.current!=null)connect();});
        connectionAction.setOnClickListener(v->{if(CloudService.connected())disconnect();else connect();});
        refreshConnection();handleIntent(getIntent());
    }
    private void refreshConnection(){
        if(refreshLanguage())return;
        JSONObject saved=null;
        try{saved=new DeviceVault(this).load();}catch(Exception ignored){}
        renderConnection(CloudService.connected(),saved!=null);
        NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);
        boolean online=CloudService.connected();
        boolean nfcUnavailable=nfc==null||!nfc.isEnabled();
        state.setText(nfcUnavailable?(nfc==null?R.string.cloud_nfc_missing:R.string.cloud_nfc_off):CloudService.statusResource);
        state.setVisibility(nfcUnavailable||!online?android.view.View.VISIBLE:android.view.View.GONE);
        CloudService service=CloudService.current;
        renderProfile(online,service==null||service.api==null?null:service.api.profile,
            service!=null&&service.reader.detected());
    }
    void renderProfile(boolean online,CloudApi.Profile profile,boolean cardPresent){
        boolean reading="reader".equals(mode);
        ((TextView)findViewById(R.id.cloud_profile_title)).setText(reading?R.string.cloud_reader_card_title:R.string.cloud_profile_title);
        TextView selected=findViewById(R.id.cloud_profile_status);
        if(reading)selected.setText(cardPresent?R.string.cloud_reader_detected:R.string.cloud_reader_waiting);
        else if(!online)selected.setText(R.string.cloud_profile_offline);
        else if(profile==null)selected.setText(R.string.cloud_profile_unknown);
        else if(!profile.active)selected.setText(R.string.cloud_profile_none);
        else if(profile.id==null)selected.setText(R.string.cloud_profile_legacy);
        else selected.setText(getString(R.string.cloud_profile_detail,"eid".equals(profile.kind)?"eID":"ePassport",profile.id,profile.version));
    }
    void renderConnection(boolean online,boolean paired){
        int color=getColor(online?R.color.cloud_green:R.color.cloud_red);
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
        show(R.string.cloud_not_connected);
    }
    private void show(int message){CloudService.statusResource=message;state.setText(message);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    private void handleIntent(Intent i){
        if(i==null)return;
        if(i.getBooleanExtra("open_diagnostics",false)){i.removeExtra("open_diagnostics");startActivity(new Intent(this,CloudDiagnosticsActivity.class));}
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
            show(R.string.cloud_connecting);
            Intent start=new Intent(this,CloudService.class).putExtra("mode",mode);
            if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(start);else startService(start);
            if("reader".equals(mode))enableReader();else disableReader();
            preferCardService();
        }catch(Exception e){show(R.string.cloud_saved_unavailable);}
    }
    private void enableReader(){
        NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);if(nfc==null)return;
        nfc.enableReaderMode(this,tag->{
            CloudService service=CloudService.current;
            if(service==null||!"reader".equals(service.mode))return;
            try{service.reader.attach(tag);runOnUiThread(()->show(R.string.cloud_reader_detected));}
            catch(Exception e){service.reader.detach();runOnUiThread(()->show(R.string.cloud_reader_error));}
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
