package ru.pgws.pocketpet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Compact game-styled living pet portrait; not an Android screenshot or private app data. */
public final class PetCard {
    private PetCard(){}
    public static Bitmap render(Context ctx, PetGame g){
        Bitmap b=Bitmap.createBitmap(480,480,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);Paint p=new Paint();p.setAntiAlias(false);
        int dark=0xff264535,light=0xffd8e8b8,middle=0xff80a275;
        c.drawColor(light);p.setColor(dark);c.drawRect(9,9,471,471,p);
        p.setColor(light);c.drawRect(15,15,465,465,p);
        PixelFont font=new PixelFont(ctx);
        font.draw(c,"КАРМАННЫЙ ПИТОМЕЦ",240,26,23,dark,true);
        String name=g.name==null||g.name.trim().isEmpty()?"ПИТОМЕЦ":g.name;
        font.draw(c,name.toUpperCase(new Locale("ru","RU")),240,72,27,dark,true);
        String[] rows=PetSprites.create(g.genes,g.portraitStage(),g.personality,g.mood(GameClock.now(ctx)),0);
        int pixel=8;int x0=(480-rows[0].length()*pixel)/2;int y0=136;
        for(int y=0;y<rows.length;y++)for(int x=0;x<rows[y].length();x++){
            char ch=rows[y].charAt(x);if(ch=='.')continue;
            p.setColor(ch=='#'?dark:ch=='o'?middle:light);
            c.drawRect(x0+x*pixel,y0+y*pixel,x0+(x+1)*pixel,y0+(y+1)*pixel,p);
        }
        font.draw(c,"ВОЗРАСТ "+g.ageDays(GameClock.now(ctx))+" ДН.  |  ГЕНЫ "+g.genes,240,351,17,dark,true);
        font.draw(c,"ЕДА "+g.hunger+"/4   РАДОСТЬ "+g.happy+"/4",240,393,17,dark,true);
        font.draw(c,"ПОБЕД "+g.gamesWon+"  |  "+g.lifeStage(),240,424,16,dark,true);
        return b;
    }
}
