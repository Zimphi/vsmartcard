package com.vsmartcard.acardemulator.cloud;
import java.util.List;
/** User-controlled grid search; arrows are instructions, not sensed RF direction. */
final class PositionGuide {
    static final int[][] POINTS={{0,0},{-1,0},{-1,-1},{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1}};
    private final NfcQuality.Stats[] positions=new NfcQuality.Stats[9];
    private final NfcGain.Stats[] gains=new NfcGain.Stats[9];
    private int index;
    int index(){return index;}
    boolean record(List<NfcQuality.Sample> samples){
        return record(samples,java.util.Collections.emptyList());
    }
    NfcGain.Stats gain(int position){return gains[position];}
    boolean record(List<NfcQuality.Sample> samples,List<NfcGain.Sample> rawGain){
        NfcQuality.Stats stats=new NfcQuality.Stats(samples);
        NfcGain.Stats gain=new NfcGain.Stats(rawGain);
        if(index>=9||(!stats.sufficient()&&!gain.sufficient()))return false;
        gains[index]=gain;positions[index++]=stats;return true;
    }
    int best(){
        int best=-1;for(int i=0;i<index;i++){
            NfcQuality.Stats s=positions[i];if(!s.sufficient()||s.successes==0)continue;
            if(best<0||1.0*s.successes/s.count>1.0*positions[best].successes/positions[best].count
                ||(1L*s.successes*positions[best].count==1L*positions[best].successes*s.count&&s.p95<positions[best].p95))best=i;
        }return best;
    }
    static String direction(int from,int to){
        int dx=Integer.signum(POINTS[to][0]-POINTS[from][0]),dy=Integer.signum(POINTS[to][1]-POINTS[from][1]);
        if(dx==0&&dy==0)return "●";
        return dy<0?(dx<0?"↖":dx>0?"↗":"↑"):dy>0?(dx<0?"↙":dx>0?"↘":"↓"):dx<0?"←":"→";
    }
    String nextDirection(){return index==0?"●":index<9?direction(index-1,index):best()<0?"?":direction(8,best());}
}
