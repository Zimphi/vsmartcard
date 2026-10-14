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
    static final class Capabilities {
        final int maxBytes,timeoutMs;final boolean extended;final String technologies;
        Capabilities(IsoDep card,Tag tag){maxBytes=card.getMaxTransceiveLength();timeoutMs=card.getTimeout();extended=card.isExtendedLengthApduSupported();
            StringBuilder list=new StringBuilder();for(String tech:tag.getTechList()){if(list.length()>0)list.append(", ");list.append(tech.substring(tech.lastIndexOf('.')+1));}technologies=list.toString();}
    }
    volatile Capabilities capabilities;
    PhoneReader(NfcStatus activity){this.activity=activity;}
    boolean detected(){return detected;}
    private volatile IsoDep card;
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
            capabilities=new Capabilities(card,tag);detected=true;CloudLog.add("NFC",R.string.cloud_log_card_detected,false);
        }catch(Exception e){detach();throw e;}
    }
    synchronized void detach(){ detected=false;activity.ended();if(card!=null){try{card.close();}catch(Exception ignored){}}card=null;atr=null;session=null; }
    synchronized boolean present(){return card!=null&&card.isConnected();}
    synchronized void selection(JSONObject state) throws Exception {
        String next=state.optString("epoch","");
        if(!next.equals(epoch)){session=null;epoch=next;}
        if(!ID.equals(state.optString("selected","")))session=null;
    }
    void cancel(){detected=false;activity.ended();IsoDep active=card;if(active!=null)try{active.close();}catch(Exception ignored){}}
    void probe(String kind) throws Exception {
        final IsoDep active; synchronized(this){if(!present())throw new java.io.IOException("no_card");active=card;}
        String aid="eid".equals(kind)?"E80704007F00070302":"A0000002471001";
        byte[] selected=CloudApi.hex(aid,1,16);
        byte[] application=new byte[5+selected.length];application[1]=(byte)0xA4;application[2]=4;application[4]=(byte)selected.length;
        System.arraycopy(selected,0,application,5,selected.length);
        byte[][] commands={application,CloudApi.hex("00A4000C023F00",4,16),CloudApi.hex("00A4020C02011C",4,16),CloudApi.hex("00B0000000",4,16)};
        for(byte[] command:commands){
            NfcStatus.Transfer transfer=activity.started();byte[] response;
            try{response=active.transceive(command);if(response.length<2)throw new java.io.IOException();activity.completed(transfer);}
            catch(Exception e){activity.failed(transfer);detach();throw e;}
            if(response[response.length-2]!=(byte)0x90||response[response.length-1]!=0)throw new java.io.IOException("unexpected_status");
        }
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
        NfcStatus.Transfer token=activity.started();
        try{
            byte[] response=card.transceive(CloudApi.hex(job.getString("hex"),4,65542));
            if(response.length<2||response.length>65535)throw new java.io.IOException();
            activity.completed(token);
            return new JSONObject().put("ok",true).put("hex",CloudApi.encode(response));
        }catch(Exception e){activity.failed(token);detach();throw e;}
    }
}
