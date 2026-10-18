package com.vsmartcard.acardemulator.cloud;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;
import ACardEmulator.R;
public final class CloudHelpActivity extends CloudScreen {
    private int step;
    private final int[] titles={R.string.cloud_help_pair_title,R.string.cloud_help_connect_title,R.string.cloud_help_card_title,R.string.cloud_help_reader_title,R.string.cloud_help_diagnostics_title};
    private final int[] bodies={R.string.cloud_help_pair_body,R.string.cloud_help_connect_body,R.string.cloud_help_card_body,R.string.cloud_help_reader_body,R.string.cloud_help_diagnostics_body};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);setContentView(R.layout.phoenix_cloud_help);
        if(saved!=null)step=saved.getInt("step");else try{if(new DeviceVault(this).load()!=null)step=CloudService.connected()?(CloudService.current!=null&&"reader".equals(CloudService.current.mode)?3:2):1;}catch(Exception ignored){}
        findViewById(R.id.cloud_help_back).setOnClickListener(v->finish());
        findViewById(R.id.cloud_help_previous).setOnClickListener(v->{if(step>0)step--;render();});
        findViewById(R.id.cloud_help_next).setOnClickListener(v->{if(step<4)step++;render();});
        findViewById(R.id.cloud_help_action).setOnClickListener(v->{
            if(step==0)startActivity(new Intent(this,CloudPairingActivity.class));
            else if(step==4)startActivity(new Intent(this,CloudDiagnosticsActivity.class));
            else if(step==2)try{startActivity(CloudInfoActivity.websiteIntent());}catch(android.content.ActivityNotFoundException e){android.widget.Toast.makeText(this,R.string.cloud_no_browser,android.widget.Toast.LENGTH_SHORT).show();}
            else finish();
        });render();
    }
    private void render(){
        ((TextView)findViewById(R.id.cloud_help_title)).setText((step+1)+" / 5 · "+getString(titles[step]));
        ((TextView)findViewById(R.id.cloud_help_body)).setText(bodies[step]);
        ((ImageView)findViewById(R.id.cloud_help_image)).setImageResource(step==0?R.drawable.help_pairing:R.drawable.help_home);
        ((TextView)findViewById(R.id.cloud_help_action)).setText(step==0?R.string.cloud_setup_title:step==4?R.string.cloud_diagnostics:step==2?R.string.cloud_website_title:R.string.cloud_back);
        findViewById(R.id.cloud_help_previous).setEnabled(step>0);findViewById(R.id.cloud_help_next).setEnabled(step<4);
    }
    @Override public void onSaveInstanceState(Bundle saved){saved.putInt("step",step);super.onSaveInstanceState(saved);}
}
