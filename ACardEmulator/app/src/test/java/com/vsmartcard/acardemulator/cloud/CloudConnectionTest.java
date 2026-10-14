package com.vsmartcard.acardemulator.cloud;

import org.junit.Test;
import static org.junit.Assert.*;

public class CloudConnectionTest {
    @Test public void onlyAFreshSuccessfulContactIsConnected() {
        assertFalse(CloudService.connectionFresh(true, 0, 10000));
        assertFalse(CloudService.connectionFresh(false, 10000, 10001));
        assertTrue(CloudService.connectionFresh(true, 10000, 24999));
        assertFalse(CloudService.connectionFresh(true, 10000, 25000));
        assertFalse(CloudService.connectionFresh(true, 10000, 9999));
    }
}
