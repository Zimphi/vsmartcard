package com.vsmartcard.acardemulator.cloud;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.util.List;
import ACardEmulator.R;

/** Fixed vendor-byte axis; missing values leave gaps, never zero-height readings. */
public final class NfcGainView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private double[] values=new double[0];private boolean positions;
    public NfcGainView(Context context,AttributeSet attrs){super(context,attrs);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void samples(List<NfcGain.Sample> samples){
        positions=false;int start=Math.max(0,samples.size()-60);values=new double[samples.size()-start];
        for(int i=start;i<samples.size();i++)values[i-start]=samples.get(i).value;invalidate();
    }
    void positions(PositionGuide guide){
        positions=true;values=new double[9];
        for(int i=0;i<9;i++){NfcGain.Stats s=guide.gain(i);values[i]=s==null||s.count==0?-1:s.median;}invalidate();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float dp=getResources().getDisplayMetrics().density;
        float left=42*dp,right=getWidth()-8*dp,top=18*dp,bottom=getHeight()-24*dp;
        if(right<=left||bottom<=top)return;
        paint.setTextSize(12*getResources().getDisplayMetrics().scaledDensity);paint.setStrokeWidth(dp);
        for(int level:new int[]{0,128,255}){
            float y=bottom-(bottom-top)*level/255f;paint.setColor(0xffdce3ef);canvas.drawLine(left,y,right,y,paint);
            paint.setColor(getContext().getColor(R.color.cloud_muted));canvas.drawText(Integer.toString(level),0,y,paint);
        }
        float step=(right-left)/Math.max(1,values.length),previousX=0,previousY=0;boolean connected=false;
        for(int i=0;i<values.length;i++){
            float x=left+(i+.5f)*step;double value=values[i];
            if(positions){paint.setColor(getContext().getColor(R.color.cloud_muted));canvas.drawText(Integer.toString(i+1),x-3*dp,getHeight()-5*dp,paint);}
            if(value<0){connected=false;continue;}
            float y=(float)(bottom-(bottom-top)*value/255);paint.setColor(getContext().getColor(R.color.cloud_blue));paint.setStrokeWidth(2*dp);
            if(positions)canvas.drawRect(x-step*.28f,y,x+step*.28f,bottom,paint);
            else if(connected)canvas.drawLine(previousX,previousY,x,y,paint);
            canvas.drawCircle(x,y,3*dp,paint);previousX=x;previousY=y;connected=true;
        }
    }
}
