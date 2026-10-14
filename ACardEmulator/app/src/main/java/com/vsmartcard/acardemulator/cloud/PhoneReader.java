package com.vsmartcard.acardemulator.cloud;
import ACardEmulator.R;

import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcB;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.util.UUID;

final class PhoneReader {
    static final String ID;
    static { try { ID=CloudApi.encode(MessageDigest.getInstance("SHA-256").digest("phoenix-android-nfc-v1".getBytes("UTF-8"))).toLowerCase(java.util.Locale.ROOT); }
        catch(Exception e){throw new ExceptionInInitializerError(e);} }
    private final NfcStatus activity;
    private volatile boolean detected;
    PhoneReader(NfcStatus activity){this.activity=activity;}
    boolean detected(){return detected;}
    private IsoDep card;
    private byte[] atr;
    private String session, epoch;
    synchronized void attach(Tag tag) throws Exception {
        detach(); card=IsoDep.get(tag);if(card==null)throw new java.io.IOException();
        try {
            card.connect();card.setTimeout(5000);
            byte[] historical=card.getHistoricalBytes();
            if(historical==null) {
                NfcB b=NfcB.get(tag);
                if(b!=null){byte[] a=b.getApplicationData(),p=b.getProtocolInfo();historical=new byte[8];
                    System.arraycopy(a,0,historical,0,4);System.arraycopy(p,0,historical,4,3);}
            }
            if(historical==null)historical=new byte[0];
            if(historical.length>15)throw new java.io.IOException();
            // Same ISO14443-to-PC/SC ATR mapping as upstream remote-reader/NFCReader.
            atr=new byte[5+historical.length];atr[0]=0x3b;atr[1]=(byte)(0x80+historical.length);atr[2]=(byte)0x80;atr[3]=1;
            System.arraycopy(historical,0,atr,4,historical.length);
            for(int i=1;i<atr.length-1;i++)atr[atr.length-1]^=atr[i];
            detected=true;CloudLog.add("NFC",R.string.cloud_log_card_detected,false);
        }catch(Exception e){detach();throw e;}
    }
    synchronized void detach(){ detected=false;activity.ended();if(card!=null){try{card.close();}catch(Exception ignored){}}card=null;atr=null;session=null; }
    synchronized boolean present(){return card!=null&&card.isConnected();}
    synchronized void selection(JSONObject state) throws Exception {
        String next=state.optString("epoch","");
        if(!next.equals(epoch)){session=null;epoch=next;}
        if(!ID.equals(state.optString("selected","")))session=null;
    }
    synchronized JSONObject execute(JSONObject job) throws Exception {
        if(!ID.equals(job.getString("reader_id"))||!job.getString("epoch").equals(epoch)
            ||System.currentTimeMillis()/1000>=job.getLong("expires"))throw new java.io.IOException();
        String op=job.getString("operation");
        if("close".equals(op)){detach();return new JSONObject().put("ok",true);}
        if(!present())throw new java.io.IOException();
        if("test".equals(op)||"connect".equals(op)){
            if(session!=null)throw new java.io.IOException();
            if("connect".equals(op)){session=job.getString("session_id");UUID.fromString(session);}
            return new JSONObject().put("ok",true).put("atr",CloudApi.encode(atr));
        }
        if(!"apdu".equals(op)||session==null||!session.equals(job.getString("session_id")))throw new java.io.IOException();
        long token=activity.started();
        try{
            byte[] response=card.transceive(CloudApi.hex(job.getString("hex"),4,65542));
            if(response.length<2||response.length>65535)throw new java.io.IOException();
            activity.completed(token);
            return new JSONObject().put("ok",true).put("hex",CloudApi.encode(response));
        }catch(Exception e){activity.failed(token);detach();throw e;}
    }
}
