package com.vsmartcard.acardemulator.cloud;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import ACardEmulator.R;
public final class CloudDiagnosticsActivity extends CloudScreen {
    private CloudLog.Entry renderedLog;
    private final Handler handler=new Handler();
    private final Runnable refresh=new Runnable(){public void run(){if(!refreshLanguage())renderLog();handler.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);setContentView(R.layout.phoenix_cloud_diagnostics);
        findViewById(R.id.cloud_diagnostics_back).setOnClickListener(v->finish());
        android.widget.CheckBox developer=findViewById(R.id.cloud_developer_options);
        developer.setChecked(getSharedPreferences("phoenix-ui",0).getBoolean("developer_options",false));
        developer.setOnCheckedChangeListener((button,checked)->{getSharedPreferences("phoenix-ui",0).edit().putBoolean("developer_options",checked).apply();renderedLog=null;renderLog();});
        renderLog();
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
            int start=text.length();text.append(clock.format(new java.util.Date(entry.time))).append(" [").append("PROFIL".equals(entry.tag)?getString(R.string.cloud_tag_profile):entry.tag).append("] ");
            int color=entry.error?R.color.cloud_red:"NFC".equals(entry.tag)?R.color.cloud_green:"CLOUD".equals(entry.tag)?R.color.cloud_blue:R.color.cloud_log_profile;
            text.setSpan(new android.text.style.ForegroundColorSpan(getColor(color)),start,text.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),start,text.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.append(entry.messageResource==0?entry.message:getString(entry.messageResource)).append("\n");
        }
        ((TextView)findViewById(R.id.cloud_log_text)).setText(text.length()==0?getString(R.string.cloud_log_empty):text);
        android.widget.ScrollView scroll=findViewById(R.id.cloud_log_scroll);scroll.post(()->scroll.fullScroll(android.view.View.FOCUS_DOWN));
    }

    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
