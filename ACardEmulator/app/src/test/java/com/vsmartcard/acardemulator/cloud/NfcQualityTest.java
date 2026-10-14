package com.vsmartcard.acardemulator.cloud;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
public class NfcQualityTest {
    private List<NfcQuality.Sample> samples(int count,int good,int ms){List<NfcQuality.Sample> s=new ArrayList<>();for(int i=0;i<count;i++)s.add(new NfcQuality.Sample(i,"test",ms,i<good));return s;}
    @Test public void qualitySeparatesEvidenceFromMissingDataAndSources(){
        assertFalse(new NfcQuality.Stats(samples(4,4,10)).sufficient());
        NfcQuality.Stats stats=new NfcQuality.Stats(samples(10,8,20));assertEquals(80,stats.percent());assertEquals(20,stats.p95,0);
        long from=NfcQuality.sequence();NfcQuality.record("a",10,true);NfcQuality.record("b",30,false);
        assertEquals(1,NfcQuality.after("a",from).size());assertEquals(1,NfcQuality.after("b",from).size());
    }
    @Test public void positionSearchRequiresSamplesAndPrefersReliabilityBeforeLatency(){
        PositionGuide p=new PositionGuide();assertEquals(-1,p.best());assertFalse(p.record(samples(2,2,10)));
        assertTrue(p.record(samples(5,5,100)));assertEquals("←",p.nextDirection());
        p.record(samples(5,4,1));assertEquals(0,p.best());
        p.record(samples(5,5,50));assertEquals(2,p.best());
        assertEquals("↖",PositionGuide.direction(0,2));
    }
    @Test public void anAllFailedSearchNeverInventsBestPosition(){PositionGuide p=new PositionGuide();for(int i=0;i<9;i++)p.record(samples(5,0,20));assertEquals(-1,p.best());assertEquals("?",p.nextDirection());}
}
