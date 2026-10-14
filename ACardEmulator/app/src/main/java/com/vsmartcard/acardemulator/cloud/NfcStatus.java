package com.vsmartcard.acardemulator.cloud;
import ACardEmulator.R;
import java.util.HashSet;
import java.util.Set;
/** Local NFC evidence only; cloud heartbeats cannot mark an NFC session active. */
final class NfcStatus {
    enum State { WAITING, SESSION, TRANSFER, ENDED, FAILED }
    static final class Transfer {
        final long generation,start=System.nanoTime();final String source;
        Transfer(long generation,String source){this.generation=generation;this.source=source;}
    }
    private long generation;
    private String source="phone-card";
    private boolean active,failed,observed;
    private final Set<Transfer> pending=new HashSet<>();
    synchronized void source(String source){reset();this.source=source;}
    synchronized Transfer started(){
        active=true;failed=false;observed=true;Transfer transfer=new Transfer(generation,source);pending.add(transfer);
        CloudLog.add("NFC",R.string.cloud_log_apdu_started,false);return transfer;
    }
    synchronized void completed(Transfer transfer){
        if(transfer.generation==generation&&pending.remove(transfer)){
            NfcQuality.record(transfer.source,(System.nanoTime()-transfer.start)/1e6,true);
            CloudLog.add("NFC",R.string.cloud_log_apdu_completed,false);
        }
    }
    synchronized void failed(Transfer transfer){
        if(transfer.generation==generation&&pending.remove(transfer)){
            NfcQuality.record(transfer.source,(System.nanoTime()-transfer.start)/1e6,false);active=false;failed=true;
            CloudLog.add("NFC",R.string.cloud_log_apdu_failed,true);
        }
    }
    synchronized void ended(){
        if(active||!pending.isEmpty())CloudLog.add("NFC",R.string.cloud_log_session_ended,false);
        for(Transfer t:pending)NfcQuality.record(t.source,(System.nanoTime()-t.start)/1e6,false);
        pending.clear();generation++;active=false;
    }
    synchronized void reset(){ended();failed=false;observed=false;}
    synchronized State state(){return failed?State.FAILED:!pending.isEmpty()?State.TRANSFER:active?State.SESSION:observed?State.ENDED:State.WAITING;}
}
