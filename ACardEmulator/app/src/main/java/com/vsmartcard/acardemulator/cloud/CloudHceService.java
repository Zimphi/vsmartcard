package com.vsmartcard.acardemulator.cloud;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/** APDUs never enter upstream's plaintext APDU logging/broadcast path. */
public final class CloudHceService extends HostApduService {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(2));
    private volatile int generation;
    private boolean failed;
    @Override public byte[] processCommandApdu(byte[] apdu,Bundle extras){
        CloudService service=CloudService.current;
        if(failed||service==null||!"card".equals(service.mode))return new byte[]{0x69,(byte)0x85};
        int current=generation;
        try{worker.execute(()->{
            if(current!=generation)return;
            byte[] response;
            try{response=service.api.apdu(apdu);}
            catch(Exception e){response=new byte[]{0x64,0};main.post(()->{if(current==generation)failed=true;});}
            byte[] result=response;
            main.post(()->{if(current==generation)sendResponseApdu(result);});
        });}catch(java.util.concurrent.RejectedExecutionException e){failed=true;return new byte[]{0x64,0};}
        return null;
    }
    @Override public void onDeactivated(int reason){
        generation++;failed=false;
        CloudService service=CloudService.current;
        if(service!=null)try{worker.execute(service.api::end);}catch(java.util.concurrent.RejectedExecutionException ignored){}
    }
    @Override public void onDestroy(){generation++;worker.shutdownNow();super.onDestroy();}
}
