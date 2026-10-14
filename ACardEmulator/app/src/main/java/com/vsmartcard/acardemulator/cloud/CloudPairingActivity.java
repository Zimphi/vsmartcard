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

public final class CloudPairingActivity extends Activity {
    private static final int IMPORT_JSON=4702;
    private TextView state;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.phoenix_cloud_pairing);
        findViewById(R.id.cloud_logo).setOnClickListener(v->startActivity(new Intent(this,CloudInfoActivity.class)));
        state=findViewById(R.id.cloud_pairing_message);
        findViewById(R.id.cloud_pairing_back).setOnClickListener(v->finish());
        findViewById(R.id.cloud_scan).setOnClickListener(v->new IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setBeepEnabled(false).setBarcodeImageEnabled(false).setPrompt("Phoenix-Kopplungs-QR-Code scannen").initiateScan());
        findViewById(R.id.cloud_import).setOnClickListener(v->{
            Intent file=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
            file.putExtra("android.provider.extra.INITIAL_URI",android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"));
            startActivityForResult(file,IMPORT_JSON);
        });
        if(getIntent().getData()!=null){String raw=getIntent().getDataString();getIntent().setData(null);confirm(raw);}
    }
    private void show(String message){state.setText(message);}
    @Override protected void onActivityResult(int request,int result,Intent data){
        if(request==IMPORT_JSON){
            if(result==RESULT_OK&&data!=null&&data.getData()!=null){
                android.net.Uri file=data.getData();
                new Thread(()->{
                    try(java.io.InputStream input=getContentResolver().openInputStream(file)){
                        Pairing pairing=Pairing.readJson(input);runOnUiThread(()->confirm(pairing));
                    }catch(Exception e){runOnUiThread(()->show("Ungültige Phoenix-JSON-Datei. Bitte eine neue Konfiguration herunterladen."));}
                },"phoenix-json").start();
            }
            return;
        }
        IntentResult qr=IntentIntegrator.parseActivityResult(request,result,data);
        if(qr!=null){if(qr.getContents()!=null)confirm(qr.getContents());}else super.onActivityResult(request,result,data);
    }
    private void confirm(String raw){
        final Pairing p;
        try{p=new Pairing(raw);}catch(Exception e){show("Ungültiger Phoenix-Kopplungs-QR-Code");return;}
        confirm(p);
    }
    private void confirm(Pairing p){
        new AlertDialog.Builder(this).setTitle("Telefon koppeln?")
            .setMessage("Dieser Server erhält Zugriff auf den gewählten NFC-Modus:\n"+p.server+"\nNur mit deinem eigenen Simulator koppeln.")
            .setNegativeButton("Abbrechen",null).setPositiveButton("Koppeln",(d,which)->{
                stopService(new Intent(this,CloudService.class));show("Kopplung läuft …");
                new Thread(()->{
                    try{
                        JSONObject reply=new CloudApi(p.server,null).post("redeem",new JSONObject().put("pairing_code",p.code));
                        String token=reply.getString("bridge_token");if(!token.matches("[A-Za-z0-9_-]{43}"))throw new IllegalArgumentException();
                        new DeviceVault(this).save(p.server,token);
                        runOnUiThread(()->{setResult(RESULT_OK);finish();});
                    }catch(Exception e){runOnUiThread(()->show("Kopplung fehlgeschlagen oder Code abgelaufen. Neuen QR-Code im Simulator erzeugen."));}
                },"phoenix-pair").start();
            }).show();
    }
}
