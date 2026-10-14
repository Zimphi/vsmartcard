package com.vsmartcard.acardemulator.cloud;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import ACardEmulator.R;
public final class NfcQualityView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<NfcQuality.Sample> samples=new ArrayList<>();
    public NfcQualityView(Context context,AttributeSet attrs){super(context,attrs);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    void samples(List<NfcQuality.Sample> values){samples=new ArrayList<>(values.subList(Math.max(0,values.size()-60),values.size()));invalidate();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float w=getWidth(),h=getHeight();
        paint.setColor(0xffdce3ef);paint.setStrokeWidth(1);
        for(int i=0;i<4;i++)canvas.drawLine(0,8+(h-16)*i/3,w,8+(h-16)*i/3,paint);
        if(samples.isEmpty())return;
        double max=1;for(NfcQuality.Sample s:samples)max=Math.max(max,s.ms);
        float step=w/Math.max(12,samples.size());
        for(int i=0;i<samples.size();i++){
            NfcQuality.Sample s=samples.get(i);paint.setColor(getContext().getColor(s.success?R.color.cloud_green:R.color.cloud_red));
            float height=(float)Math.max(3,(h-16)*s.ms/max);
            canvas.drawRoundRect(i*step+2,h-8-height,(i+1)*step-2,h-8,3,3,paint);
        }
    }
}
