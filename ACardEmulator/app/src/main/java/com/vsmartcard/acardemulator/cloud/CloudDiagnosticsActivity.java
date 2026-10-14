package com.vsmartcard.acardemulator.cloud;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import ACardEmulator.R;
public final class CloudDiagnosticsActivity extends CloudScreen {
    private CloudLog.Entry renderedLog;
    private final NfcStatus localNfc=new NfcStatus();
    private volatile PhoneReader testReader=new PhoneReader(localNfc);
    private volatile boolean testing,positionReader;
    private volatile long measuringUntil;
    private volatile Thread readerThread;
    private volatile String readerKind;private boolean useLab=true;
    private boolean positionNotice;
    private PositionGuide guide=new PositionGuide();
    private long measuringFrom;
    private String measuringSource,sourceOverride,labMode;
    private LabControl lab;private boolean labActive;
    private org.json.JSONObject successfulReport;
    private final Handler handler=new Handler();
    private final Runnable refresh=new Runnable(){public void run(){if(!refreshLanguage()){renderLog();refreshDiagnostics();}handler.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);setContentView(R.layout.phoenix_cloud_diagnostics);
        NfcAntennaView antenna=findViewById(R.id.cloud_antenna_view);
        if(antenna.load()){
            String description=getString(R.string.cloud_antenna_explanation)+"\n"+antenna.coordinates();
            ((TextView)findViewById(R.id.cloud_antenna_description)).setText(description);antenna.setContentDescription(description);
        }else antenna.setVisibility(android.view.View.GONE);
        lab=new LabControl(this);localNfc.source("phone-reader");
        android.widget.Spinner kind=findViewById(R.id.cloud_test_kind);
        kind.setAdapter(new android.widget.ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"eID","ePassport"}));
        findViewById(R.id.cloud_test_reader).setOnClickListener(v->confirmReaderTest(false));
        findViewById(R.id.cloud_test_mac).setOnClickListener(v->confirmMacTest());
        findViewById(R.id.cloud_test_stop).setOnClickListener(v->{stopReaderTest();try{lab.cancel();}catch(Exception ignored){}labActive=false;useLab=false;measuringUntil=0;message(R.string.cloud_test_stopped);});
        findViewById(R.id.cloud_position_measure).setOnClickListener(v->measurePosition());
        findViewById(R.id.cloud_position_reset).setOnClickListener(v->{measuringUntil=0;positionNotice=false;guide=new PositionGuide();renderPosition();});
        findViewById(R.id.cloud_compatibility).setOnClickListener(v->confirmCompatibility());
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

    private String kind(){return ((android.widget.Spinner)findViewById(R.id.cloud_test_kind)).getSelectedItemPosition()==0?"eid":"epassport";}
    private String source(){
        if(labActive||"mac-pcsc".equals(sourceOverride))return "mac-pcsc";
        CloudService service=CloudService.current;
        return "phone-reader".equals(sourceOverride)||(service!=null&&"reader".equals(service.mode))?"phone-reader":"phone-card";
    }
    private void refreshDiagnostics(){
        readLabProgress();renderQuality();renderPosition();
        android.nfc.NfcAdapter adapter=android.nfc.NfcAdapter.getDefaultAdapter(this);
        String values=getString(R.string.cloud_device_summary,android.os.Build.MANUFACTURER,android.os.Build.MODEL,android.os.Build.VERSION.RELEASE,android.os.Build.VERSION.SDK_INT,android.os.Build.VERSION.SECURITY_PATCH,
            getString(getPackageManager().hasSystemFeature("android.hardware.nfc.hce")?R.string.cloud_yes:R.string.cloud_no),getString(adapter!=null&&adapter.isEnabled()?R.string.cloud_yes:R.string.cloud_no));
        ((TextView)findViewById(R.id.cloud_device_values)).setText(values);
        PhoneReader.Capabilities caps=testReader.capabilities;
        ((TextView)findViewById(R.id.cloud_reader_capabilities)).setText(caps==null?"":getString(R.string.cloud_reader_capabilities,caps.technologies,caps.maxBytes,caps.timeoutMs,getString(caps.extended?R.string.cloud_yes:R.string.cloud_no)));
        findViewById(R.id.cloud_test_reader).setEnabled(!testing&&!labActive);
        findViewById(R.id.cloud_test_mac).setEnabled(!testing&&!labActive);
        findViewById(R.id.cloud_test_stop).setEnabled(testing||labActive);
        findViewById(R.id.cloud_compatibility).setEnabled(successfulReport!=null&&!testing&&!labActive);
    }
    private void renderQuality(){
        String origin=source();java.util.List<NfcQuality.Sample> samples=NfcQuality.after(origin,0);
        NfcQuality.Stats stats=new NfcQuality.Stats(samples);
        ((TextView)findViewById(R.id.cloud_quality_source)).setText(origin.equals("mac-pcsc")?R.string.cloud_quality_mac:origin.equals("phone-reader")?R.string.cloud_quality_phone_reader:R.string.cloud_quality_phone_card);
        String summary=stats.count==0?getString(R.string.cloud_quality_none):!stats.sufficient()?getString(R.string.cloud_quality_pending,stats.count)
            :stats.successes==0?getString(R.string.cloud_quality_failed,stats.count)
            :getString(R.string.cloud_quality_summary,stats.percent(),stats.successes,stats.count,stats.count-stats.successes,stats.median,stats.p95,stats.max);
        ((TextView)findViewById(R.id.cloud_quality_summary)).setText(summary);
        android.widget.ProgressBar meter=findViewById(R.id.cloud_quality_meter);meter.setProgress(stats.sufficient()?stats.percent():0);meter.setContentDescription(summary);
        NfcQualityView graph=findViewById(R.id.cloud_quality_graph);graph.samples(samples);graph.setContentDescription(summary);
    }
    private void renderPosition(){
        TextView status=findViewById(R.id.cloud_position_status),arrow=findViewById(R.id.cloud_position_arrow);
        String[] names=getString(R.string.cloud_position_names).split("\\|");
        if(measuringUntil>0){
            long remaining=measuringUntil-android.os.SystemClock.elapsedRealtime();
            java.util.List<NfcQuality.Sample> samples=NfcQuality.after(measuringSource,measuringFrom);
            if(remaining>0){status.setText(getString(R.string.cloud_position_measuring,(remaining+999)/1000,samples.size()));return;}
            measuringUntil=0;
            if(!guide.record(samples)){positionNotice=true;findViewById(R.id.cloud_position_measure).setEnabled(true);status.setText(R.string.cloud_position_insufficient);if(positionReader)stopReaderTest();return;}
            if(positionReader)stopReaderTest();
        }
        if(positionNotice)return;
        arrow.setText(guide.nextDirection());
        if(guide.index()<9)status.setText(getString(R.string.cloud_position_step,guide.index()+1,names[guide.index()],guide.nextDirection()));
        else status.setText(guide.best()<0?getString(R.string.cloud_position_no_best):getString(R.string.cloud_position_best,guide.best()+1,names[guide.best()]));
        findViewById(R.id.cloud_position_measure).setEnabled(guide.index()<9&&measuringUntil==0);
    }
    private void beginPosition(){
        if(guide.index()>=9||measuringUntil>0)return;
        positionNotice=false;measuringSource=source();measuringFrom=NfcQuality.sequence();measuringUntil=android.os.SystemClock.elapsedRealtime()+30000;
        findViewById(R.id.cloud_position_measure).setEnabled(false);
    }
    private void measurePosition(){
        if(labActive&&"baseline".equals(labMode)){positionReader=false;beginPosition();return;}
        if("phone-reader".equals(source())||CloudService.current==null||"reader".equals(CloudService.current.mode)){
            confirmReaderTest(true);return;
        }
        if(!labActive||!"baseline".equals(labMode)){message(R.string.cloud_position_baseline_required);return;}
        positionReader=false;beginPosition();
    }
    private void message(int resource){((TextView)findViewById(R.id.cloud_test_status)).setText(resource);}
    private void confirmReaderTest(boolean position){
        if(testing||labActive)return;
        CloudService service=CloudService.current;
        if(service!=null&&!"reader".equals(service.mode)){message(R.string.cloud_test_wrong_mode);return;}
        android.nfc.NfcAdapter nfc=android.nfc.NfcAdapter.getDefaultAdapter(this);
        if(nfc==null||!nfc.isEnabled()){message(R.string.cloud_test_nfc_required);return;}
        new android.app.AlertDialog.Builder(this).setTitle(R.string.cloud_test_reader).setMessage(R.string.cloud_test_disclosure)
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.cloud_test_start,(dialog,which)->{
                sourceOverride="phone-reader";useLab=false;NfcQuality.clear("phone-reader");readerKind=kind();successfulReport=null;testing=true;positionReader=position;
                if(service!=null){service.localTest=true;testReader=service.reader;}
                if(position)beginPosition();enableReader();message(R.string.cloud_test_waiting);
                if(testReader.detected())runReaderTest();
            }).show();
    }
    private void enableReader(){
        android.nfc.NfcAdapter nfc=android.nfc.NfcAdapter.getDefaultAdapter(this);if(nfc==null||!nfc.isEnabled())return;
        nfc.enableReaderMode(this,tag->{
            try{testReader.attach(tag);if(testing)runReaderTest();}
            catch(Exception unavailable){testReader.detach();runOnUiThread(()->message(R.string.cloud_reader_error));}
        },android.nfc.NfcAdapter.FLAG_READER_NFC_A|android.nfc.NfcAdapter.FLAG_READER_NFC_B|android.nfc.NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,null);
    }
    private synchronized void runReaderTest(){
        if(readerThread!=null||!testing)return;
        final String selected=readerKind;final PhoneReader reader=testReader;
        readerThread=new Thread(()->{
            int completed=0;boolean passed=true;
            try{
                while(testing&&(completed<10||(positionReader&&measuringUntil>0))){
                    reader.probe(selected);completed++;final int count=completed;
                    runOnUiThread(()->((TextView)findViewById(R.id.cloud_test_status)).setText(getString(R.string.cloud_test_progress,count,count)));
                    Thread.sleep(750);
                }
            }catch(Exception failure){passed=false;}
            final boolean success=passed&&testing&&completed>=10;final int count=completed;
            testing=false;CloudService service=CloudService.current;if(service!=null)service.localTest=false;reader.detach();
            synchronized(this){readerThread=null;}
            runOnUiThread(()->{
                if(isFinishing())return;
                if(success)((TextView)findViewById(R.id.cloud_test_status)).setText(getString(R.string.cloud_test_pass,count));else message(R.string.cloud_test_fail);
                if(success)try{successfulReport=LabControl.device().put("test_id",java.util.UUID.randomUUID().toString())
                    .put("mode","reader").put("source","phone-reader").put("attempts",count).put("successes",count).put("passed",true);}catch(Exception ignored){}
                refreshDiagnostics();
            });
        },"phoenix-reader-test");readerThread.start();
    }
    private void stopReaderTest(){
        boolean running=testing;testing=false;CloudService service=CloudService.current;if(service!=null)service.localTest=false;
        if(readerThread!=null)readerThread.interrupt();testReader.cancel();if(running)message(R.string.cloud_test_stopped);
    }
    private void confirmMacTest(){
        if(testing||labActive)return;
        if(!ACardEmulator.BuildConfig.DEBUG){message(R.string.cloud_lab_debug_required);return;}
        final String[] modes={"usb-profile","cloud-eac","baseline"};
        new android.app.AlertDialog.Builder(this).setTitle(R.string.cloud_lab_title)
            .setItems(getString(R.string.cloud_lab_choices).split("\\|"),(choice,index)->confirmMacMode(modes[index])).show();
    }
    android.app.AlertDialog confirmMacMode(String mode){
        if(!java.util.Arrays.asList("usb-profile","cloud-eac","baseline").contains(mode))throw new IllegalArgumentException();
        return new android.app.AlertDialog.Builder(this).setTitle(R.string.cloud_lab_title).setMessage(R.string.cloud_lab_disclosure)
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.cloud_test_start,(dialog,which)->{
                try{useLab=true;labMode=mode;NfcQuality.clear("mac-pcsc");lab.request(labMode,kind(),"baseline".equals(mode)?120:1);labActive=true;sourceOverride="mac-pcsc";successfulReport=null;message(R.string.cloud_lab_waiting);}
                catch(Exception failure){message(R.string.cloud_lab_error);}
            }).show();
    }
    private void readLabProgress(){
        if(!useLab)return;
        try{
            org.json.JSONObject progress=lab.progress();if(progress==null)return;
            String phase=progress.optString("phase");labMode=progress.getJSONObject("request").getString("mode");
            labActive="running".equals(phase)||"queued".equals(phase);sourceOverride="mac-pcsc";
            if(labActive)((TextView)findViewById(R.id.cloud_test_status)).setText(getString(R.string.cloud_lab_running,progress.optInt("completed"),progress.getJSONObject("request").getInt("attempts")));
            else if("complete".equals(phase)){
                successfulReport=LabControl.successfulReport(progress);
                if(progress.optBoolean("passed"))((TextView)findViewById(R.id.cloud_test_status)).setText(getString(R.string.cloud_lab_pass,labMode));else message(R.string.cloud_lab_failed);
            }else if("aborted".equals(phase)||"failed".equals(phase))message(R.string.cloud_lab_failed);
        }catch(Exception invalid){message(R.string.cloud_lab_error);}
    }
    private void confirmCompatibility(){
        if(successfulReport==null){message(R.string.cloud_compatibility_no_result);return;}
        CloudService service=CloudService.current;
        if(service==null||service.api==null||!CloudService.connected()){message(R.string.cloud_compatibility_failed);return;}
        final org.json.JSONObject report;try{report=new org.json.JSONObject(successfulReport.toString());}catch(Exception invalid){return;}
        showCompatibility(report,service.api);
    }
    android.app.AlertDialog showCompatibility(final org.json.JSONObject report,CloudApi api){
        String account=api.account==null?api.server:api.account.name+"\n"+api.server;
        String details=report.optString("manufacturer")+" · "+report.optString("model")+"\nAndroid "+report.optString("android_release")+" · "+report.optString("app_version")+"\n"+report.optString("mode")+" · "+report.optInt("successes")+"/"+report.optInt("attempts");
        return new android.app.AlertDialog.Builder(this).setTitle(R.string.cloud_compatibility)
            .setMessage(getString(R.string.cloud_compatibility_disclosure)+"\n\n"+account+"\n\n"+details)
            .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.cloud_compatibility_save,(dialog,which)->{
                new Thread(()->{
                    boolean saved=false;try{report.put("consent",true);saved=api.post("compatibility",report).optBoolean("saved");}catch(Exception failure){}
                    final boolean accepted=saved;runOnUiThread(()->message(accepted?R.string.cloud_compatibility_saved:R.string.cloud_compatibility_failed));
                },"phoenix-compatibility").start();
            }).show();
    }

    @Override protected void onResume(){
        super.onResume();CloudService service=CloudService.current;
        android.nfc.NfcAdapter nfc=android.nfc.NfcAdapter.getDefaultAdapter(this);
        if(service!=null&&"reader".equals(service.mode)){testReader=service.reader;sourceOverride="phone-reader";enableReader();}
        else if(service!=null&&nfc!=null&&nfc.isEnabled())android.nfc.cardemulation.CardEmulation.getInstance(nfc).setPreferredService(this,new android.content.ComponentName(this,CloudHceService.class));
        handler.post(refresh);
    }
    @Override protected void onPause(){
        handler.removeCallbacks(refresh);stopReaderTest();measuringUntil=0;
        android.nfc.NfcAdapter nfc=android.nfc.NfcAdapter.getDefaultAdapter(this);
        if(nfc!=null){nfc.disableReaderMode(this);if(nfc.isEnabled())android.nfc.cardemulation.CardEmulation.getInstance(nfc).unsetPreferredService(this);}
        super.onPause();
    }
}
