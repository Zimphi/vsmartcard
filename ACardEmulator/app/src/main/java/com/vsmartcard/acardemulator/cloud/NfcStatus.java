package com.vsmartcard.acardemulator.cloud;

/** Local NFC evidence only; cloud heartbeats cannot mark an NFC session active. */
final class NfcStatus {
    enum State { WAITING, SESSION, TRANSFER, ENDED, FAILED }
    private long generation;
    private boolean active, busy, failed, observed;
    synchronized long started(){active=true;busy=true;failed=false;observed=true;CloudLog.add("NFC","APDU empfangen · Übertragung läuft",false);return generation;}
    synchronized void completed(long token){if(token==generation){busy=false;CloudLog.add("NFC","APDU beantwortet",false);}}
    synchronized void failed(long token){if(token==generation){busy=false;active=false;failed=true;CloudLog.add("NFC","APDU-Übertragung fehlgeschlagen",true);}}
    synchronized void ended(){if(active||busy)CloudLog.add("NFC","NFC-Sitzung beendet",false);generation++;active=false;busy=false;}
    synchronized void reset(){ended();failed=false;observed=false;}
    synchronized State state(){return failed?State.FAILED:busy?State.TRANSFER:active?State.SESSION:observed?State.ENDED:State.WAITING;}
}
