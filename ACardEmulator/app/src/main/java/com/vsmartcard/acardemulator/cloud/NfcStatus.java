package com.vsmartcard.acardemulator.cloud;
import ACardEmulator.R;

/** Local NFC evidence only; cloud heartbeats cannot mark an NFC session active. */
final class NfcStatus {
    enum State { WAITING, SESSION, TRANSFER, ENDED, FAILED }
    private long generation;
    private boolean active, busy, failed, observed;
    synchronized long started(){active=true;busy=true;failed=false;observed=true;CloudLog.add("NFC",R.string.cloud_log_apdu_started,false);return generation;}
    synchronized void completed(long token){if(token==generation){busy=false;CloudLog.add("NFC",R.string.cloud_log_apdu_completed,false);}}
    synchronized void failed(long token){if(token==generation){busy=false;active=false;failed=true;CloudLog.add("NFC",R.string.cloud_log_apdu_failed,true);}}
    synchronized void ended(){if(active||busy)CloudLog.add("NFC",R.string.cloud_log_session_ended,false);generation++;active=false;busy=false;}
    synchronized void reset(){ended();failed=false;observed=false;}
    synchronized State state(){return failed?State.FAILED:busy?State.TRANSFER:active?State.SESSION:observed?State.ENDED:State.WAITING;}
}
