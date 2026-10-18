package com.vsmartcard.acardemulator.cloud;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class NfcGainTest {
    @Test public void missingAndInvalidGainNeverBecomeZeroOrStrengthPercent(){
        NfcGain.clear();
        NfcGain.record(-1,100);NfcGain.record(256,100);NfcGain.record(0,100);NfcGain.record(255,100);
        NfcGain.Stats s=new NfcGain.Stats(NfcGain.after(0));
        assertEquals(4,s.frames);assertEquals(2,s.count);assertEquals(0,s.min);assertEquals(255,s.max);assertEquals(127.5,s.median,0);
    }
    @Test public void samplesAreBoundedAndMeasurementWindowsExcludeOldValues(){
        NfcGain.clear();NfcGain.record(100,10);long from=NfcGain.sequence();NfcGain.record(-1,20);
        assertEquals(0,new NfcGain.Stats(NfcGain.after(from)).count);
        for(int i=0;i<250;i++)NfcGain.record(i%256,i);
        assertEquals(200,NfcGain.after(0).size());
    }
    @Test public void positionStoresGainWithoutInventingAnRfRanking(){
        NfcGain.clear();for(int i=0;i<5;i++)NfcGain.record(80+i,100);
        PositionGuide p=new PositionGuide();
        assertTrue(p.record(Collections.emptyList(),NfcGain.after(0)));
        assertEquals(82,p.gain(0).median,0);assertEquals(-1,p.best());
        assertEquals("←",p.nextDirection());
        assertFalse(p.record(Collections.emptyList(),Arrays.asList(new NfcGain.Sample(1,100,-1))));
    }
}
