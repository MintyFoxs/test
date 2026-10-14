package ru.pgws.pocketpet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

/** 8-bit bitmap Cyrillic+Latin font. 12x16 atlas, nearest-neighbour scaled. */
public final class PixelFont {
    private static final String GLYPHS=" АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯabcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,:!?+-*/=<>#%№()[]♡♥★●×↑↓→←_•✓♪—";
    private final Bitmap atlas;
    private final Paint p=new Paint();
    public PixelFont(Context c){atlas=BitmapFactory.decodeResource(c.getResources(),R.drawable.pixel_font);p.setFilterBitmap(false);p.setAntiAlias(false);}
    public float width(String s,float height){return (s==null?0:s.length())*(height*0.75f);}
    public void draw(Canvas c,String value,float x,float top,float height,int color,boolean center){
        if(value==null)return;
        float charW=height*.75f;
        if(center)x-=width(value,height)/2f;
        p.setColor(color);p.setAlpha(Color.alpha(color));p.setColorFilter(new PorterDuffColorFilter(color,PorterDuff.Mode.SRC_IN));
        for(int i=0;i<value.length();i++){
            char ch=value.charAt(i);
            if(ch==' ')continue;
            int j=GLYPHS.indexOf(ch);
            if(j<0)j=GLYPHS.indexOf(Character.toUpperCase(ch));
            if(j<0)j=GLYPHS.indexOf('?');
            if(j<0)continue;
            int cx=(j%16)*12,cy=(j/16)*16;
            c.drawBitmap(atlas,new Rect(cx,cy,cx+12,cy+16),new RectF(x+i*charW,top,x+(i+1)*charW,top+height),p);
        }
    }
}
