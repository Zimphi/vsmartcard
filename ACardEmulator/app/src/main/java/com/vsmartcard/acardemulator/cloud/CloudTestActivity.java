// SPDX-License-Identifier: GPL-3.0-or-later
// Phoenix Cloud extension; license notice added 2026-10-23. See LICENSE_REVIEW.md.
package com.vsmartcard.acardemulator.cloud;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.Button;
import android.nfc.NfcAdapter;
import org.json.JSONObject;
import org.json.JSONArray;
import ACardEmulator.R;
/** Guided execution of existing tests; unavailable hardware never becomes a pass. */
public final class CloudTestActivity extends CloudScreen {
    private final Handler handler=new Handler();
    private final Runnable refresh=new Runnable(){public void run(){if(!refreshLanguage())render();handler.postDelayed(this,1000);}};
    private DiagnosticEvidence evidence;
    private boolean loading;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);setContentView(R.layout.phoenix_cloud_test);evidence=new DiagnosticEvidence(this);
        Spinner kind=findViewById(R.id.cloud_guided_kind);
        kind.setAdapter(new android.widget.ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"eID","ePassport"}));
        findViewById(R.id.cloud_guided_back).setOnClickListener(v->finish());
        findViewById(R.id.cloud_guided_reader).setOnClickListener(v->start("reader"));
        findViewById(R.id.cloud_guided_card).setOnClickListener(v->start("card"));
        findViewById(R.id.cloud_guided_confirm_reader).setOnClickListener(v->start("confirm-reader"));
        findViewById(R.id.cloud_guided_confirm_card).setOnClickListener(v->start("confirm-card"));
        findViewById(R.id.cloud_guided_next).setOnClickListener(v->next());
        findViewById(R.id.cloud_guided_cloud).setOnClickListener(v->loadCloud());
        kind.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
            public void onItemSelected(android.widget.AdapterView<?> parent,android.view.View view,int position,long id){
                ((TextView)findViewById(R.id.cloud_guided_cloud_results)).setText(R.string.cloud_guided_cloud_context);render();
            }
        });
        findViewById(R.id.cloud_guided_stop).setOnClickListener(v->{try{new LabControl(this).cancel();}catch(Exception unavailable){} });
        render();
    }
    private String kind(){return ((Spinner)findViewById(R.id.cloud_guided_kind)).getSelectedItemPosition()==0?"eid":"epassport";}
    private boolean nfc(){NfcAdapter nfc=NfcAdapter.getDefaultAdapter(this);return nfc!=null&&nfc.isEnabled();}
    private void start(String action){startActivity(new Intent(this,CloudDiagnosticsActivity.class).putExtra("guided_action",action).putExtra("kind",kind()));}
    private boolean passed(String mode){JSONObject value=evidence.latest(mode,kind());return value!=null&&DiagnosticEvidence.eligible(value);}
    private void next(){
        if(!CloudService.connected()){startActivity(new Intent(this,CloudActivity.class));return;}
        if(!nfc()){
            try{startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));}catch(android.content.ActivityNotFoundException unavailable){}
            return;
        }
        if(!passed("reader")){start("reader");return;}
        if(!passed("card")){start("card");return;}
        try{evidence.clearCurrent(kind());render();}catch(Exception invalid){}
    }
    private void render(){
        JSONObject progress=null;try{progress=new LabControl(this).progress();if(progress!=null)evidence.recordProgress(progress);}catch(Exception unavailable){}
        boolean active=progress!=null&&java.util.Arrays.asList("queued","running").contains(progress.optString("phase"));
        findViewById(R.id.cloud_guided_stop).setEnabled(active);
        for(int id:new int[]{R.id.cloud_guided_reader,R.id.cloud_guided_card,R.id.cloud_guided_next,R.id.cloud_guided_kind})findViewById(id).setEnabled(!active);
        boolean online=CloudService.connected(),ready=nfc(),read=passed("reader"),card=passed("card");
        TextView connection=findViewById(R.id.cloud_guided_connection);
        connection.setText(getString(online?R.string.cloud_guided_online:R.string.cloud_guided_offline)+"\n"+getString(ready?R.string.cloud_guided_nfc_ready:R.string.cloud_test_nfc_required));
        connection.setTextColor(getColor(online&&ready?R.color.cloud_green:R.color.cloud_red));
        renderResult("reader",R.id.cloud_guided_reader_result);renderResult("card",R.id.cloud_guided_card_result);
        ((Button)findViewById(R.id.cloud_guided_next)).setText(!online?R.string.cloud_guided_connect:!ready?R.string.cloud_guided_enable_nfc:!read?R.string.cloud_guided_reader:!card?R.string.cloud_guided_card:R.string.cloud_guided_restart);
        ((TextView)findViewById(R.id.cloud_guided_summary)).setText(online&&ready&&read&&card?R.string.cloud_guided_complete:R.string.cloud_guided_incomplete);
        findViewById(R.id.cloud_guided_confirm_reader).setEnabled(online&&read);
        findViewById(R.id.cloud_guided_confirm_card).setEnabled(online&&card);
        findViewById(R.id.cloud_guided_cloud).setEnabled(online&&!loading);
    }
    private void renderResult(String mode,int view){
        JSONObject result=evidence.latest(mode,kind());TextView label=findViewById(view);String text;
        if(result==null)text=getString(R.string.cloud_guided_untested);
        else {
            text=getString(DiagnosticEvidence.eligible(result)?R.string.cloud_guided_pass:R.string.cloud_guided_fail)+"\n"+
                getString(R.string.cloud_guided_result_detail,result.optInt("successes"),result.optInt("attempts"),
                java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(result.optLong("tested_at_ms"))))+"\n"+checks(this,result.optJSONArray("checks"));
        }
        label.setText(text);
        label.setTextColor(getColor(result!=null&&DiagnosticEvidence.eligible(result)?R.color.cloud_green:R.color.cloud_muted));
        try{
            JSONObject progress=new LabControl(this).progress();
            if("card".equals(mode)&&progress!=null&&kind().equals(progress.getJSONObject("request").optString("kind"))
                &&java.util.Arrays.asList("queued","running").contains(progress.optString("phase")))label.setText(R.string.cloud_guided_running);
        }catch(Exception unavailable){}
    }
    static String checks(android.content.Context context,JSONArray checks){
        if(checks==null||checks.length()==0)return context.getString(R.string.cloud_guided_no_checks);
        java.util.List<String> labels=new java.util.ArrayList<>();
        for(int i=0;i<checks.length()&&i<DiagnosticEvidence.CHECKS.length;i++){
            String check=checks.optString(i);if(!java.util.Arrays.asList(DiagnosticEvidence.CHECKS).contains(check))continue;
            int id=context.getResources().getIdentifier("cloud_check_"+check,"string",context.getPackageName());
            if(id!=0)labels.add(context.getString(id));
        }
        return android.text.TextUtils.join(" · ",labels);
    }
    private void loadCloud(){
        CloudService service=CloudService.current;if(loading||service==null||!CloudService.connected())return;
        loading=true;String selected=kind();
        new Thread(()->{
            String text;
            try{
                JSONArray reports=service.api.post("compatibility-status",LabControl.device()).getJSONArray("reports");
                StringBuilder lines=new StringBuilder(getString(R.string.cloud_guided_cloud_context));int count=0;
                for(int i=0;i<reports.length()&&i<20;i++){
                    JSONObject value=reports.getJSONObject(i);String mode=value.optString("mode");
                    if(!selected.equals(value.optString("kind"))||!("reader".equals(mode)||"card".equals(mode)))continue;
                    lines.append("\n\n").append(getString("reader".equals(mode)?R.string.cloud_guided_reader:R.string.cloud_guided_card))
                        .append("\n").append(checks(this,value.optJSONArray("checks")));
                    String date=value.optString("tested_at");if(date.matches("[0-9T:.+Z -]{10,40}"))lines.append("\n").append(date);count++;
                }
                if(count==0)lines.append("\n").append(getString(R.string.cloud_guided_cloud_empty));text=lines.toString();
            }catch(Exception unavailable){text=getString(R.string.cloud_guided_cloud_failed);}
            final String result=text;runOnUiThread(()->{loading=false;if(!selected.equals(kind()))return;((TextView)findViewById(R.id.cloud_guided_cloud_results)).setText(result);render();});
        },"phoenix-evidence").start();
    }
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
}
