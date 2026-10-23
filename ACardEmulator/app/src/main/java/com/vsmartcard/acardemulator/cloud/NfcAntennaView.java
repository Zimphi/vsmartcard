// SPDX-License-Identifier: GPL-3.0-or-later
// Phoenix Cloud extension; license notice added 2026-10-23. See LICENSE_REVIEW.md.
package com.vsmartcard.acardemulator.cloud;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
/** Manufacturer-supplied antenna geometry; not a measured RF field. */
public final class NfcAntennaView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int widthMm,heightMm;private final List<int[]> antennas=new ArrayList<>();
    public NfcAntennaView(Context context,AttributeSet attrs){super(context,attrs);}
    boolean load(){
        antennas.clear();
        if(android.os.Build.VERSION.SDK_INT<34)return false;
        try{
            android.nfc.NfcAdapter nfc=android.nfc.NfcAdapter.getDefaultAdapter(getContext());if(nfc==null)return false;
            android.nfc.NfcAntennaInfo info=nfc.getNfcAntennaInfo();if(info==null||info.getDeviceWidth()<=0||info.getDeviceHeight()<=0)return false;
            widthMm=info.getDeviceWidth();heightMm=info.getDeviceHeight();
            for(android.nfc.AvailableNfcAntenna a:info.getAvailableNfcAntennas())if(a.getLocationX()>=0&&a.getLocationX()<=widthMm&&a.getLocationY()>=0&&a.getLocationY()<=heightMm)antennas.add(new int[]{a.getLocationX(),a.getLocationY()});
            invalidate();return !antennas.isEmpty();
        }catch(RuntimeException unavailable){return false;}
    }
    String coordinates(){
        StringBuilder result=new StringBuilder(widthMm+" × "+heightMm+" mm");
        for(int[] a:antennas)result.append("\nX ").append(a[0]).append(" mm · Y ").append(a[1]).append(" mm");return result.toString();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);if(antennas.isEmpty())return;
        float scale=Math.min((getWidth()-32f)/widthMm,(getHeight()-24f)/heightMm);
        float w=widthMm*scale,h=heightMm*scale,x=(getWidth()-w)/2,y=(getHeight()-h)/2;
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xffe4eaf7);canvas.drawRoundRect(x,y,x+w,y+h,18,18,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setColor(0xff345fc4);canvas.drawRoundRect(x,y,x+w,y+h,18,18,paint);
        for(int[] a:antennas){float ax=x+a[0]*scale,ay=y+a[1]*scale;canvas.drawCircle(ax,ay,16,paint);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(ax,ay,5,paint);paint.setStyle(Paint.Style.STROKE);}
        paint.setStyle(Paint.Style.FILL);
    }
}
