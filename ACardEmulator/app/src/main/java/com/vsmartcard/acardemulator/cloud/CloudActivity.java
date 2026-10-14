package com.vsmartcard.acardemulator.cloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import org.json.JSONObject;

public final class CloudActivity extends Activity {
    private static final int IMPORT_JSON=4702;
    private final Handler handler=new Handler();
    private TextView state,server;
    private RadioGroup roles;
    private final int cardChoiceId=android.view.View.generateViewId(), readerChoiceId=android.view.View.generateViewId();
    private String mode="card";
    private final Runnable refresh=new Runnable(){public void run(){
        state.setText(CloudService.status);handler.postDelayed(this,1000);
    }};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(24*getResources().getDisplayMetrics().density);root.setPadding(pad,pad,pad,pad);
        TextView title=new TextView(this);title.setText("Phoenix Cloud · NFC");title.setTextSize(24);root.addView(title);
        server=new TextView(this);server.setText("Noch nicht gekoppelt");root.addView(server);
        state=new TextView(this);state.setTextSize(18);root.addView(state);
        TextView help=new TextView(this);help.setText("In den Einstellungen des Online-Simulators einen Kopplungs-QR-Code anzeigen oder eine JSON-Datei herunterladen. Der Rechner wird danach nicht benötigt.");root.addView(help);
        button(root,"QR-Code scannen",()->new IntentIntegrator(this).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setBeepEnabled(false).setBarcodeImageEnabled(false).setPrompt("Phoenix-Kopplungs-QR-Code scannen").initiateScan());
        button(root,"JSON-Datei importieren",()->{
            Intent file=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
            file.putExtra("android.provider.extra.INITIAL_URI",android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"));
            startActivityForResult(file,IMPORT_JSON);
        });
        roles=new RadioGroup(this);
        RadioButton card=new RadioButton(this);card.setId(cardChoiceId);card.setText("Karte simulieren");roles.addView(card);
        RadioButton reader=new RadioButton(this);reader.setId(readerChoiceId);reader.setText("Echte NFC-Karte lesen");roles.addView(reader);
        roles.check(cardChoiceId);roles.setOnCheckedChangeListener((group,id)->{mode=id==cardChoiceId?"card":"reader";if(CloudService.current!=null)connect();});root.addView(roles);
        button(root,"Verbinden",this::connect);
        button(root,"Verbindung beenden",()->{stopService(new Intent(this,CloudService.class));disableReader();show("Nicht verbunden");});
        TextView note=new TextView(this);note.setText("Im Lesemodus die App geöffnet lassen und die Karte an die NFC-Antenne halten. Zum Widerrufen der Gerätefreigabe die Vercel-Einstellungen verwenden.");root.addView(note);
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(root);
        setContentView(scroll);handleIntent(getIntent());
    }
    private void show(String message){CloudService.status=message;state.setText(message);}
    private void button(LinearLayout root,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setOnClickListener(v->action.run());root.addView(b);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);handleIntent(i);}
    private void handleIntent(Intent i){if(i!=null&&i.getData()!=null){String raw=i.getDataString();i.setData(null);confirm(raw);}}
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
                        runOnUiThread(()->{server.setText(p.server);connect();});
                    }catch(Exception e){runOnUiThread(()->show("Kopplung fehlgeschlagen oder Code abgelaufen. Neuen QR-Code im Simulator erzeugen."));}
                },"phoenix-pair").start();
            }).show();
    }
    private void connect(){
        try{
            JSONObject saved=new DeviceVault(this).load();
            if(saved==null){show("Bitte zuerst den Kopplungs-QR-Code scannen");return;}
            server.setText(saved.getString("server"));
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
