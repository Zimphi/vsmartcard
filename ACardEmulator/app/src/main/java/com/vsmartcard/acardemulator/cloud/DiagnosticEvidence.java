package com.vsmartcard.acardemulator.cloud;
import android.content.Context;
import org.json.JSONObject;
import org.json.JSONArray;
/** Bounded local observations, not certification; never persist arbitrary test payloads. */
final class DiagnosticEvidence {
    static final String[] DEVICE={"manufacturer","model","android_release","api_level","app_version","security_patch","build_id"};
    static final String[] CHECKS={"application_selection","read_card_access","card_access_matches","pace","secure_messaging_file_matches","passive_authentication","chip_authentication","terminal_authentication","unauthorized_read_rejected","authorized_file_matches","ca_v3_key_agreement","pseudonymous_signature","ps_tampering_rejected","ps_uncertified_sector_rejected","restricted_identification","ri_uncertified_sector_rejected"};
    private final android.content.SharedPreferences prefs;
    DiagnosticEvidence(Context context){prefs=context.getSharedPreferences("phoenix-diagnostics",Context.MODE_PRIVATE);}
    static JSONObject clean(JSONObject input)throws Exception{
        JSONObject result=new JSONObject();
        for(String key:DEVICE){
            if("api_level".equals(key)){int api=input.optInt(key);if(api<23||api>100)throw new IllegalArgumentException();result.put(key,api);}
            else {String value=input.getString(key);int max="build_id".equals(key)?128:"app_version".equals(key)?60:"manufacturer".equals(key)||"model".equals(key)?80:40;
                if(value.trim().isEmpty()||value.length()>max||value.matches("(?s).*\\p{Cntrl}.*"))throw new IllegalArgumentException();result.put(key,value);}
        }
        String id=input.getString("test_id"),mode=input.getString("mode"),source=input.getString("source"),kind=input.getString("kind");
        java.util.UUID.fromString(id);
        if(!("eid".equals(kind)||"epassport".equals(kind))||!("reader".equals(mode)&&"phone-reader".equals(source)||"card".equals(mode)&&("cloud-eac".equals(source)||"usb-profile".equals(source))))throw new IllegalArgumentException();
        int attempts=input.getInt("attempts"),successes=input.getInt("successes");long tested=input.getLong("tested_at_ms");
        if(attempts<0||attempts>10000||successes<0||successes>attempts||tested<=0)throw new IllegalArgumentException();
        JSONArray checks=new JSONArray(),raw=input.optJSONArray("checks");
        java.util.Set<String> seen=new java.util.HashSet<>();
        if(raw!=null)for(int i=0;i<raw.length()&&i<32;i++){String value=raw.optString(i);if(java.util.Arrays.asList(CHECKS).contains(value)&&seen.add(value))checks.put(value);}
        return result.put("test_id",id).put("mode",mode).put("source",source).put("kind",kind).put("attempts",attempts).put("successes",successes)
            .put("passed",input.optBoolean("passed")).put("checks",checks).put("tested_at_ms",tested);
    }
    static boolean eligible(JSONObject value){
        long tested=value.optLong("tested_at_ms"),now=System.currentTimeMillis();
        if(tested<=0||tested<now-30L*86400000||tested>now+300000)return false;
        if(!value.optBoolean("passed")||value.optInt("attempts")<1||value.optInt("successes")!=value.optInt("attempts"))return false;
        JSONArray checks=value.optJSONArray("checks");if(checks==null)return false;
        java.util.Set<String> set=new java.util.HashSet<>();for(int i=0;i<checks.length();i++)set.add(checks.optString(i));
        return "reader".equals(value.optString("mode"))?set.size()==2&&set.contains("application_selection")&&set.contains("read_card_access")
            :"card".equals(value.optString("mode"))&&set.containsAll(java.util.Arrays.asList("pace","secure_messaging_file_matches","passive_authentication","chip_authentication","terminal_authentication"));
    }
    JSONArray records(){
        JSONArray result=new JSONArray();String saved=prefs.getString("records","[]");if(saved.length()>65536)return result;
        try{JSONArray raw=new JSONArray(saved);for(int i=Math.max(0,raw.length()-20);i<raw.length();i++)try{result.put(clean(raw.getJSONObject(i)));}catch(Exception invalid){}}
        catch(Exception invalid){}return result;
    }
    synchronized void record(JSONObject input)throws Exception{
        JSONObject value=clean(input);JSONArray values=records(),result=new JSONArray();
        for(int i=0;i<values.length();i++)if(values.getJSONObject(i).toString().equals(value.toString()))return;
        for(int i=Math.max(0,values.length()-19);i<values.length();i++)if(!values.getJSONObject(i).getString("test_id").equals(value.getString("test_id")))result.put(values.get(i));
        result.put(value);if(!prefs.edit().putString("records",result.toString()).commit())throw new java.io.IOException();
    }
    void recordProgress(JSONObject progress)throws Exception{
        String phase=progress.optString("phase");if(!java.util.Arrays.asList("complete","failed","aborted").contains(phase))return;
        JSONObject request=progress.getJSONObject("request");String mode=request.getString("mode");if("baseline".equals(mode))return;
        JSONObject result=new JSONObject(request.getJSONObject("device").toString());
        result.put("test_id",request.getString("id")).put("kind",request.getString("kind")).put("mode","card").put("source",mode)
            .put("attempts",request.getInt("attempts")).put("successes",progress.optInt("successes"))
            .put("passed","complete".equals(phase)&&progress.optBoolean("passed")&&progress.optBoolean("profile_verified"))
            .put("checks",progress.optJSONArray("checks")==null?new JSONArray():progress.getJSONArray("checks"))
            .put("tested_at_ms",progress.optLong("completed_at_ms",request.getLong("requested_at_ms")));
        record(result);
    }
    synchronized void clearCurrent(String kind)throws Exception{
        JSONArray raw=records(),kept=new JSONArray();JSONObject device=LabControl.device();
        for(int i=0;i<raw.length();i++){JSONObject value=raw.getJSONObject(i);boolean same=kind.equals(value.optString("kind"));
            for(String key:DEVICE)same&=device.get(key).equals(value.get(key));if(!same)kept.put(value);
        }
        if(!prefs.edit().putString("records",kept.toString()).commit())throw new java.io.IOException();
    }
    JSONObject latest(String mode,String kind){
        JSONArray values=records();try{JSONObject device=LabControl.device();
            for(int i=values.length()-1;i>=0;i--){JSONObject value=values.getJSONObject(i);boolean same=true;
                for(String key:DEVICE)same&=device.get(key).equals(value.get(key));
                if(same&&mode.equals(value.getString("mode"))&&kind.equals(value.getString("kind")))return value;
            }
        }catch(Exception invalid){}return null;
    }
}
