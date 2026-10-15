package com.vsmartcard.acardemulator.cloud;
import ACardEmulator.R;

import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

class CloudApi {
    static final class Denied extends Exception {}
    final String server;
    private final String token;
    private JSONObject session;
    private String openedId;
    static final class Profile {
        final boolean active;final String id,kind;final int version;
        Profile(boolean active,String id,String kind,int version){this.active=active;this.id=id;this.kind=kind;this.version=version;}
    }
    volatile Profile profile;
    static final class Account {
        final String name,language;
        Account(String name,String language){this.name=name;this.language=language;}
    }
    volatile Account account;
    private void updateAccount(JSONObject packet){
        JSONObject value=packet.optJSONObject("account");
        if(value==null){account=null;return;}
        String name=value.optString("display_name","");String language=value.optString("language","");
        if(name.matches("[a-z0-9._-]{1,64}@users\\.phoenix\\.invalid"))name=name.substring(0,name.indexOf('@'));
        account=name.isEmpty()||name.length()>320?null:new Account(name,"en".equals(language)||"de".equals(language)?language:null);
    }
    private void updateProfile(JSONObject packet){
        JSONObject value=packet.optJSONObject("profile");
        boolean active=packet.optBoolean("active");
        if(!active){if(profile==null||profile.active)CloudLog.add("PROFIL",R.string.cloud_log_profile_none,false);profile=new Profile(false,null,null,0);return;}
        String id=value==null?"":value.optString("profile_id","");
        String kind=value==null?"":value.optString("kind","");
        int version=value==null?0:value.optInt("version",0);
        boolean valid=!id.isEmpty()&&id.length()<=200&&("eid".equals(kind)||"epassport".equals(kind))&&version>0;
        Profile previous=profile;
        if(previous==null||!previous.active||!java.util.Objects.equals(previous.id,valid?id:null)||previous.version!=(valid?version:0))
            CloudLog.add("PROFIL",valid?R.string.cloud_log_profile_updated:R.string.cloud_log_profile_legacy,false);
        profile=new Profile(true,valid?id:null,valid?kind:null,valid?version:0);
    }
    CloudApi(String server,String token) { this.server=server;this.token=token; }
    JSONObject post(String endpoint,JSONObject body) throws Exception {
        HttpsURLConnection c=(HttpsURLConnection)new URL(server+"/api/vsmartcard/"+endpoint).openConnection();
        c.setInstanceFollowRedirects(false); c.setConnectTimeout(5000);c.setReadTimeout(15000);
        c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
        if (token!=null)c.setRequestProperty("Authorization","Bearer "+token);
        byte[] data=body.toString().getBytes("UTF-8");c.setFixedLengthStreamingMode(data.length);
        try {
            try(java.io.OutputStream out=c.getOutputStream()){out.write(data);}
            int status=c.getResponseCode();
            if(status==401||status==403)throw new Denied();
            if(status!=200)throw new java.io.IOException("Cloud request failed");
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()) {
                byte[] buffer=new byte[4096];int n;
                while((n=in.read(buffer))!=-1){ if(out.size()+n>4*1024*1024)throw new java.io.IOException();out.write(buffer,0,n); }
            }
            return new JSONObject(out.toString("UTF-8"));
        } finally { c.disconnect(); }
    }
    synchronized JSONObject command(String operation,JSONObject fields) throws Exception {
        JSONObject state=session==null?new JSONObject().put("revision",0):new JSONObject(session.toString());
        state.put("request_id",UUID.randomUUID().toString());
        JSONObject body=new JSONObject().put("operation",operation).put("session",state);
        if(fields!=null)body.put("fields",fields);
        JSONObject result=post("command",body);
        if(result.optBoolean("protocol_error"))throw new java.io.IOException();
        if(!"close".equals(operation)) {
            JSONObject next=result.getJSONObject("session");
            if(next.getLong("revision")!=state.getLong("revision")+1 || !next.getString("id").equals(openedId)
                || next.getString("checkpoint").isEmpty())throw new java.io.IOException();
            session=new JSONObject().put("id",next.getString("id")).put("revision",next.getLong("revision"))
                .put("checkpoint",next.getString("checkpoint"));
        }
        return result;
    }
    void heartbeat(JSONObject body) throws Exception {
        final JSONObject capturedSession;final String capturedId;
        synchronized(this){capturedSession=session;capturedId=openedId;}
        JSONObject current=post("status",body);
        synchronized(this){
            // APDU commits replace the session object; ignore a heartbeat from an older state.
            if(session!=capturedSession||!java.util.Objects.equals(openedId,capturedId))return;
            updateProfile(current);updateAccount(current);
            if(openedId!=null&&!openedId.equals(current.getString("session_id"))){session=null;openedId=null;}
        }
    }
    synchronized void begin() throws Exception {
        session=null;openedId=null;
        JSONObject result=post("open",new JSONObject());
        updateProfile(result);updateAccount(result);
        openedId=result.getString("session_id");UUID.fromString(openedId);
        command("mount",null);
    }
    synchronized byte[] apdu(byte[] bytes) throws Exception {
        if(session==null)begin();
        try{return hex(command("apdu",new JSONObject().put("hex",encode(bytes))).getString("apdu_response"),2,65535);}
        catch(Exception e){session=null;openedId=null;throw e;}
    }
    synchronized void end(){
        try{if(session!=null)command("close",null);}catch(Exception ignored){}
        finally{session=null;openedId=null;}
    }
    static String encode(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(java.util.Locale.ROOT,"%02X",b&255));return s.toString();}
    static byte[] hex(String value,int min,int max) throws Exception {
        if(value==null||value.length()%2!=0||value.length()<2*min||value.length()>2*max||!value.matches("[0-9a-fA-F]+"))throw new java.io.IOException();
        byte[] bytes=new byte[value.length()/2];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(value.substring(2*i,2*i+2),16);return bytes;
    }
}
