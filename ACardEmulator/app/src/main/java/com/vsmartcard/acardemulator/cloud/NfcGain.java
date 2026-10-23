// SPDX-License-Identifier: GPL-3.0-or-later
// Phoenix Cloud extension; license notice added 2026-10-23. See LICENSE_REVIEW.md.
package com.vsmartcard.acardemulator.cloud;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Vendor raw polling gain; never calibrated RSSI or an inferred strength percentage. */
final class NfcGain {
    static final class Sample {
        final long sequence,timeMs;final int value;
        Sample(long sequence,long timeMs,int value){this.sequence=sequence;this.timeMs=timeMs;this.value=value;}
    }
    static final class Stats {
        final int frames,count,min,max;final double median;
        Stats(List<Sample> samples){
            frames=samples.size();List<Integer> values=new ArrayList<>();
            for(Sample s:samples)if(s.value>=0)values.add(s.value);
            Collections.sort(values);count=values.size();
            if(count==0){min=max=-1;median=Double.NaN;return;}
            min=values.get(0);max=values.get(count-1);
            median=count%2==1?values.get(count/2):(values.get(count/2-1)+values.get(count/2))/2.0;
        }
        boolean sufficient(){return count>=5;}
    }
    private static final ArrayDeque<Sample> samples=new ArrayDeque<>();
    private static long sequence;
    static synchronized long sequence(){return sequence;}
    static synchronized void clear(){samples.clear();}
    static synchronized void record(int value,long now){
        samples.addLast(new Sample(++sequence,now,value>=0&&value<=255?value:-1));
        while(samples.size()>200)samples.removeFirst();
    }
    static synchronized List<Sample> after(long from){
        List<Sample> result=new ArrayList<>();for(Sample s:samples)if(s.sequence>from)result.add(s);return result;
    }
}
