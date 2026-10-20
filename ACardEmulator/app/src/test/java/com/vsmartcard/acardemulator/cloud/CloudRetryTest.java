package com.vsmartcard.acardemulator.cloud;
import org.junit.Test;
import static org.junit.Assert.*;
public class CloudRetryTest {
    @Test public void retriesBackOffAndNetworkRecoveryResetsTheDelay(){
        CloudRetry retry=new CloudRetry();assertTrue(retry.ready(100));
        long now=100;
        for(long delay:new long[]{1000,2000,4000,8000,15000,15000}){
            retry.failed(now);assertFalse(retry.ready(now+delay-1));assertTrue(retry.ready(now+delay));now+=delay;
        }
        retry.reset();assertTrue(retry.ready(now));retry.failed(now);assertTrue(retry.ready(now+1000));
    }
}
