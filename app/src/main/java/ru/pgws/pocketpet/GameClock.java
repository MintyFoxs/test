package ru.pgws.pocketpet;

import android.content.Context;
import android.content.SharedPreferences;

/** Virtual simulation clock. At 1x it exactly tracks wall time; a secret debug name unlocks speed controls. */
public final class GameClock {
    private static final String PREF="pocketpet96_debug_clock";
    private GameClock() {}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    public static long now(Context c){
        SharedPreferences p=prefs(c);
        long wall=System.currentTimeMillis();
        long baseWall=p.getLong("wall",0);
        long baseGame=p.getLong("game",0);
        int speed=p.getInt("speed",1);
        if(baseWall==0)return wall;
        long elapsed=Math.max(0,wall-baseWall);
        return baseGame+Math.min(elapsed,Long.MAX_VALUE/Math.max(1,speed))*speed;
    }
    public static int speed(Context c){return prefs(c).getInt("speed",1);}
    public static void setSpeed(Context c,int speed){
        if(speed!=1&&speed!=5&&speed!=20&&speed!=60&&speed!=240)return;
        long stamp=now(c);
        prefs(c).edit().putLong("wall",System.currentTimeMillis()).putLong("game",stamp).putInt("speed",speed).commit();
    }
    public static void skip(Context c,long ms){
        long stamp=now(c);
        prefs(c).edit().putLong("wall",System.currentTimeMillis()).putLong("game",stamp+ms).commit();
    }
}
