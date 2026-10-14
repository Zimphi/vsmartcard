package com.vsmartcard.acardemulator.cloud;

import android.app.Activity;
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

public final class CloudInfoActivity extends Activity {
    private final Handler handler=new Handler();
    private final Runnable refresh=new Runnable(){public void run(){
        refreshConnection();handler.postDelayed(this,1000);
    }};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.phoenix_cloud_info);
        ((TextView)findViewById(R.id.cloud_info_version)).setText(getString(R.string.cloud_version,BuildConfig.VERSION_NAME));
        findViewById(R.id.cloud_info_back).setOnClickListener(v->finish());
        findViewById(R.id.cloud_website).setOnClickListener(v->{
            try{startActivity(websiteIntent());}
            catch(ActivityNotFoundException error){Toast.makeText(this,R.string.cloud_no_browser,Toast.LENGTH_LONG).show();}
        });
    }
    static Intent websiteIntent(){return new Intent(Intent.ACTION_VIEW,Uri.parse("https://epassport-simulator.de"));}
    private void refreshConnection(){
        JSONObject saved=null;
        try{saved=new DeviceVault(this).load();}catch(Exception ignored){}
        ((TextView)findViewById(R.id.cloud_info_server)).setText(saved==null?getString(R.string.cloud_not_paired):saved.optString("server"));
        boolean online=CloudService.connected();
        TextView status=findViewById(R.id.cloud_info_status);
        status.setText((online?"✓ ":"✕ ")+getString(online?R.string.cloud_connected:saved!=null?R.string.cloud_saved_offline:R.string.cloud_unpaired));
        status.setTextColor(getColor(online?R.color.cloud_green:R.color.cloud_red));
        ((TextView)findViewById(R.id.cloud_info_detail)).setText(CloudService.status);
    }
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
