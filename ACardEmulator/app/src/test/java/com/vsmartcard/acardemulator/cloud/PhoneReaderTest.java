package com.vsmartcard.acardemulator.cloud;

import org.junit.Test;
import static org.junit.Assert.*;

public class PhoneReaderTest {
    @Test public void caseThreeSelectsDoNotRequestUnretrievedFci() throws Exception {
        for(String kind:new String[]{"eid","epassport"}) {
            byte[][] commands=PhoneReader.probeCommands(kind);
            assertEquals(4,commands.length);
            for(byte[] command:commands) {
                if(command[1]!=(byte)0xA4)continue;
                assertEquals("Case 3 SELECT has no Le",5+(command[4]&255),command.length);
                assertEquals("Without GET RESPONSE, SELECT must suppress FCI",0x0C,command[3]&255);
            }
            assertEquals("eid".equals(kind)?"E80704007F00070302":"A0000002471001",
                CloudApi.encode(java.util.Arrays.copyOfRange(commands[0],5,commands[0].length)));
            assertArrayEquals(CloudApi.hex("00B0000000",4,16),commands[3]);
        }
    }
    @Test public void readBinaryMayFinishAtEofOnlyWhenItReturnedData() {
        byte[] read={0,(byte)0xB0,0,0,0},select={0,(byte)0xA4,4,0x0C};
        assertTrue(PhoneReader.validProbeResponse(read,new byte[]{0x31,0,0x62,(byte)0x82}));
        assertFalse(PhoneReader.validProbeResponse(read,new byte[]{0x62,(byte)0x82}));
        assertFalse(PhoneReader.validProbeResponse(select,new byte[]{0x31,0,0x62,(byte)0x82}));
        assertFalse(PhoneReader.validProbeResponse(read,new byte[]{0x31,0,0x69,(byte)0x82}));
    }
    @Test public void continuationAndEmptyReadsCannotPassAsCompletedProbes() {
        byte[] select={0,(byte)0xA4,4,0x0C},read={0,(byte)0xB0,0,0,0};
        assertFalse(PhoneReader.validProbeResponse(select,new byte[]{0x61,0x10}));
        assertFalse(PhoneReader.validProbeResponse(read,new byte[]{(byte)0x90,0}));
        assertTrue(PhoneReader.validProbeResponse(read,new byte[]{0x31,0,(byte)0x90,0}));
    }
}
