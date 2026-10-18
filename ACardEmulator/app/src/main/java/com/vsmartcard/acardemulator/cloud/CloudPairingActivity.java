package com.vsmartcard.acardemulator.cloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import ACardEmulator.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import org.json.JSONObject;

public final class CloudPairingActivity extends CloudScreen {
    private static final int IMPORT_JSON=4702;
    private TextView state;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if(getIntent().getBooleanExtra("manage_pairing",false)){
            setContentView(R.layout.phoenix_cloud_pairing_manage);
            state=findViewById(R.id.cloud_pairing_message);
            findViewById(R.id.cloud_pairing_back).setOnClickListener(v->finish());
            findViewById(R.id.cloud_delete_pairing).setOnClickListener(v->showDeleteConfirmation());
            renderSavedPairing();return;
        }
        setContentView(R.layout.phoenix_cloud_pairing);
        state=findViewById(R.id.cloud_pairing_message);
        findViewById(R.id.cloud_pairing_back).setOnClickListener(v->finish());
        findViewById(R.id.cloud_scan).setOnClickListener(v->new IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setBeepEnabled(false).setBarcodeImageEnabled(false).setPrompt(getString(R.string.cloud_scan_prompt)).initiateScan());
        findViewById(R.id.cloud_import).setOnClickListener(v->{
            Intent file=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
            file.putExtra("android.provider.extra.INITIAL_URI",android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"));
            startActivityForResult(file,IMPORT_JSON);
        });
        if(getIntent().getData()!=null){String raw=getIntent().getDataString();getIntent().setData(null);confirm(raw);}
    }
    private void renderSavedPairing(){
        try{
            JSONObject saved=new DeviceVault(this).load();
            ((TextView)findViewById(R.id.cloud_pairing_name)).setText(saved==null?getString(R.string.cloud_not_paired):android.net.Uri.parse(saved.getString("server")).getHost());
            boolean online=saved!=null&&CloudService.connected();
            state.setText(online?R.string.cloud_connected:saved==null?R.string.cloud_not_paired:R.string.cloud_saved_offline);
            ((android.widget.ImageView)findViewById(R.id.cloud_pairing_connection_icon)).setImageResource(online?R.drawable.cloud_connected:R.drawable.cloud_disconnected);
            findViewById(R.id.cloud_delete_pairing).setVisibility(saved==null?android.view.View.GONE:android.view.View.VISIBLE);
        }catch(Exception e){show(getString(R.string.cloud_saved_unavailable));}
    }
    AlertDialog showDeleteConfirmation(){
        return new AlertDialog.Builder(this).setTitle(R.string.cloud_delete_pairing)
            .setMessage(R.string.cloud_delete_pairing_notice)
            .setNegativeButton(R.string.cloud_cancel,null)
            .setPositiveButton(R.string.cloud_delete_pairing,(dialog,which)->{
                stopService(new Intent(this,CloudService.class));
                try{new DeviceVault(this).clear();setResult(RESULT_FIRST_USER);finish();}
                catch(Exception e){show(getString(R.string.cloud_delete_pairing_failed));}
            }).show();
    }
    private void show(String message){state.setText(message);}
    @Override protected void onActivityResult(int request,int result,Intent data){
        if(request==IMPORT_JSON){
            if(result==RESULT_OK&&data!=null&&data.getData()!=null){
                android.net.Uri file=data.getData();
                new Thread(()->{
                    try(java.io.InputStream input=getContentResolver().openInputStream(file)){
                        Pairing pairing=Pairing.readJson(input);runOnUiThread(()->confirm(pairing));
                    }catch(Exception e){runOnUiThread(()->show(getString(R.string.cloud_json_error)));}
                },"phoenix-json").start();
            }
            return;
        }
        IntentResult qr=IntentIntegrator.parseActivityResult(request,result,data);
        if(qr!=null){if(qr.getContents()!=null)confirm(qr.getContents());}else super.onActivityResult(request,result,data);
    }
    private void confirm(String raw){
        final Pairing p;
        try{p=new Pairing(raw);}catch(Exception e){show(getString(R.string.cloud_qr_error));return;}
        confirm(p);
    }
    private void confirm(Pairing p){
        new AlertDialog.Builder(this).setTitle(getString(R.string.cloud_pair_confirm))
            .setMessage(getString(R.string.cloud_pair_disclosure,p.server))
            .setNegativeButton(getString(R.string.cloud_cancel),null).setPositiveButton(getString(R.string.cloud_pair_action),(d,which)->{
                stopService(new Intent(this,CloudService.class));show(getString(R.string.cloud_pair_running));
                new Thread(()->{
                    try{
                        JSONObject reply=new CloudApi(p.server,null).post("redeem",new JSONObject().put("pairing_code",p.code));
                        String token=reply.getString("bridge_token");if(!token.matches("[A-Za-z0-9_-]{43}"))throw new IllegalArgumentException();
                        new DeviceVault(this).save(p.server,token);
                        runOnUiThread(()->{setResult(RESULT_OK);finish();});
                    }catch(Exception e){runOnUiThread(()->show(getString(R.string.cloud_pair_error)));}
                },"phoenix-pair").start();
            }).show();
    }
}
