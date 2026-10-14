package com.vsmartcard.acardemulator.cloud;
import org.junit.Test;
import static org.junit.Assert.*;
public class CloudLogTest {
    @Test public void logIsBoundedAndSnapshotsCannotChangeStoredEntries(){
        for(int i=0;i<130;i++)CloudLog.add("NFC","APDU beantwortet",false);
        assertEquals(120,CloudLog.snapshot().size());
        CloudLog.snapshot().clear();assertEquals(120,CloudLog.snapshot().size());
    }
}
