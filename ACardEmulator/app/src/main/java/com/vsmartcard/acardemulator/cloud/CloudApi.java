package com.vsmartcard.acardemulator.cloud;

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
    synchronized void heartbeat(JSONObject body) throws Exception {
        JSONObject current=post("status",body);
        if(openedId!=null&&!openedId.equals(current.getString("session_id"))){session=null;openedId=null;}
    }
    synchronized void begin() throws Exception {
        session=null;openedId=null;
        JSONObject result=post("open",new JSONObject());
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
