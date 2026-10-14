package com.vsmartcard.acardemulator.cloud;
import org.junit.Test;
import static org.junit.Assert.*;
public class NfcStatusTest {
    @Test public void trafficIsMeasuredAndLateResponsesCannotReviveEndedSession(){
        NfcStatus status=new NfcStatus();assertEquals(NfcStatus.State.WAITING,status.state());
        long first=status.started();assertEquals(NfcStatus.State.TRANSFER,status.state());
        status.completed(first);assertEquals(NfcStatus.State.SESSION,status.state());
        status.ended();assertEquals(NfcStatus.State.ENDED,status.state());
        status.failed(first);status.completed(first);assertEquals(NfcStatus.State.ENDED,status.state());
        long next=status.started();status.failed(next);assertEquals(NfcStatus.State.FAILED,status.state());
        status.ended();assertEquals(NfcStatus.State.FAILED,status.state());
        status.reset();assertEquals(NfcStatus.State.WAITING,status.state());
    }
}
