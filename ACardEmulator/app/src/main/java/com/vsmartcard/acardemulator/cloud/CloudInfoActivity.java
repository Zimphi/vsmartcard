package com.vsmartcard.acardemulator.cloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.ScrollView;
import java.util.Scanner;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import ACardEmulator.R;
import ACardEmulator.BuildConfig;
import org.json.JSONObject;

public final class CloudInfoActivity extends CloudScreen {
    private static final int EXPORT_JSON=4710;
    private byte[] pendingExport;
    private final Handler handler=new Handler();
    private final Runnable refresh=new Runnable(){public void run(){
        refreshConnection();handler.postDelayed(this,1000);
    }};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.phoenix_cloud_info);
        ((TextView)findViewById(R.id.cloud_info_version)).setText(getString(R.string.cloud_version,BuildConfig.VERSION_NAME));
        findViewById(R.id.cloud_info_nfc_test).setOnClickListener(v->startActivity(new Intent(this,CloudTestActivity.class)));
        findViewById(R.id.cloud_export_diagnostics).setOnClickListener(v->confirmExport());
        if(saved!=null){byte[] restored=saved.getByteArray("diagnostic_export");if(restored!=null&&restored.length<=65536)pendingExport=restored;}
        findViewById(R.id.cloud_info_diagnostics).setOnClickListener(v->startActivity(new Intent(this,CloudDiagnosticsActivity.class)));
        findViewById(R.id.cloud_license).setOnClickListener(v->showLicense());
        findViewById(R.id.cloud_info_back).setOnClickListener(v->finish());
        findViewById(R.id.cloud_website).setOnClickListener(v->{
            try{startActivity(websiteIntent());}
            catch(ActivityNotFoundException error){Toast.makeText(this,R.string.cloud_no_browser,Toast.LENGTH_LONG).show();}
        });
    }
    AlertDialog showLicense(){
        TextView text=new TextView(this);
        try(Scanner license=new Scanner(getResources().openRawResource(R.raw.gpl_3),"UTF-8");
            Scanner thirdParty=new Scanner(getAssets().open("third_party_notices.txt"),"UTF-8")){
            text.setText(license.useDelimiter("\\A").next()+thirdParty.useDelimiter("\\A").next());
        }catch(java.io.IOException error){
            throw new IllegalStateException("Bundled license notices unavailable",error);
        }
        text.setTextSize(14);text.setTextIsSelectable(true);
        int padding=(int)(16*getResources().getDisplayMetrics().density);
        text.setPadding(padding,padding,padding,padding);
        ScrollView scroll=new ScrollView(this);scroll.addView(text);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.cloud_license_title)
            .setView(scroll).setPositiveButton(android.R.string.ok,null).create();
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        dialog.show();return dialog;
    }
    static Intent websiteIntent(){return new Intent(Intent.ACTION_VIEW,Uri.parse("https://epassport-simulator.de"));}
    private void refreshConnection(){
        if(refreshLanguage())return;
        CloudService service=CloudService.current;
        CloudApi.Account account=service==null||service.api==null?null:service.api.account;
        ((TextView)findViewById(R.id.cloud_info_user)).setText(account==null?getString(R.string.cloud_account_pending):account.name);
        JSONObject saved=null;
        try{saved=new DeviceVault(this).load();}catch(Exception ignored){}
        ((TextView)findViewById(R.id.cloud_info_server)).setText(saved==null?getString(R.string.cloud_not_paired):saved.optString("server"));
        boolean online=CloudService.connected();
        TextView status=findViewById(R.id.cloud_info_status);
        status.setText((online?"✓ ":"✕ ")+getString(online?R.string.cloud_connected:saved!=null?R.string.cloud_saved_offline:R.string.cloud_unpaired));
        status.setTextColor(getColor(online?R.color.cloud_green:R.color.cloud_red));
        ((TextView)findViewById(R.id.cloud_info_detail)).setText(CloudService.statusResource);
    }
    android.app.AlertDialog confirmExport(){
        return new android.app.AlertDialog.Builder(this).setTitle(R.string.cloud_export_diagnostics).setMessage(R.string.cloud_export_notice)
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.cloud_export_action,(dialog,which)->{
                try{pendingExport=DiagnosticExport.snapshot(this).toString(2).getBytes("UTF-8");
                    if(pendingExport.length>65536)throw new java.io.IOException();
                    startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json")
                        .putExtra(Intent.EXTRA_TITLE,"phoenix-diagnostics.json"),EXPORT_JSON);
                }catch(Exception unavailable){pendingExport=null;exportMessage(R.string.cloud_export_failed);}
            }).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=EXPORT_JSON)return;
        byte[] payload=pendingExport;pendingExport=null;
        if(result!=RESULT_OK||data==null||data.getData()==null||payload==null)return;
        android.net.Uri destination=data.getData();
        new Thread(()->{
            boolean saved=false;try(java.io.OutputStream out=getContentResolver().openOutputStream(destination,"wt")){
                if(out==null)throw new java.io.IOException();out.write(payload);out.flush();saved=true;
            }catch(Exception unavailable){}
            final boolean accepted=saved;runOnUiThread(()->exportMessage(accepted?R.string.cloud_export_saved:R.string.cloud_export_failed));
        },"phoenix-export").start();
    }

    private void exportMessage(int resource){
        TextView status=findViewById(R.id.cloud_info_export_status);status.setText(resource);status.setVisibility(android.view.View.VISIBLE);
    }
    @Override protected void onSaveInstanceState(Bundle saved){
        if(pendingExport!=null)saved.putByteArray("diagnostic_export",pendingExport);super.onSaveInstanceState(saved);
    }
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
