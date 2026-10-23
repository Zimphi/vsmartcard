// SPDX-License-Identifier: GPL-3.0-or-later
// Phoenix Cloud extension; license notice added 2026-10-23. See LICENSE_REVIEW.md.
package com.vsmartcard.acardemulator.cloud;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/** Transport observations, never RF strength. Sources are deliberately not mixed. */
final class NfcQuality {
    static final class Sample {
        final long sequence;final String source;final double ms;final boolean success;
        Sample(long sequence,String source,double ms,boolean success){this.sequence=sequence;this.source=source;this.ms=ms;this.success=success;}
    }
    static final class Stats {
        final int count,successes;final double median,p95,max;
        Stats(List<Sample> values){
            count=values.size();int good=0;List<Double> times=new ArrayList<>();
            for(Sample s:values)if(s.success){good++;times.add(s.ms);}
            successes=good;Collections.sort(times);
            if(times.isEmpty()){median=p95=max=Double.NaN;return;}
            int n=times.size();median=n%2==1?times.get(n/2):(times.get(n/2-1)+times.get(n/2))/2;
            p95=times.get((int)Math.ceil(n*.95)-1);max=times.get(n-1);
        }
        int percent(){return count==0?0:(int)Math.round(100.0*successes/count);}
        boolean sufficient(){return count>=5;}
    }
    private static final ArrayDeque<Sample> samples=new ArrayDeque<>();
    private static long sequence;
    static synchronized void clear(String source){
        java.util.Iterator<Sample> iterator=samples.iterator();while(iterator.hasNext())if(iterator.next().source.equals(source))iterator.remove();
    }
    static synchronized long sequence(){return sequence;}
    static synchronized void record(String source,double ms,boolean success){
        if(!Double.isFinite(ms)||ms<0)throw new IllegalArgumentException();
        samples.addLast(new Sample(++sequence,source,ms,success));while(samples.size()>200)samples.removeFirst();
    }
    static synchronized List<Sample> after(String source,long from){
        List<Sample> result=new ArrayList<>();for(Sample s:samples)if(s.sequence>from&&s.source.equals(source))result.add(s);return result;
    }
}
