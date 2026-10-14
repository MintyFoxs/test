package ru.pgws.pocketpet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;

public final class MainActivity extends Activity {
    private static final String STORAGE="pocketpet96_save_v1";
    private final Handler handler = new Handler();
    private final PetGame game=new PetGame();
    private PetView view;
    private ToneGenerator tones;
    private boolean active;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try { tones=new ToneGenerator(AudioManager.STREAM_MUSIC,50); } catch(Exception ignored) {}
        load();
        game.advance(System.currentTimeMillis());
        view=new PetView(this,game,this);
        setContentView(view);
        immerse();
    }
    private final Runnable ticker=new Runnable() {
        @Override public void run() {
            if (!active) return;
            game.advance(System.currentTimeMillis());
            if(view!=null) view.invalidate();
            handler.postDelayed(this,1000L);
        }
    };
    @Override protected void onResume() { super.onResume(); active=true;
        game.advance(System.currentTimeMillis());
        handler.removeCallbacks(ticker);handler.post(ticker);
        immerse();
    }
    @Override protected void onPause() { active=false;handler.removeCallbacks(ticker);save();super.onPause(); }
    @Override protected void onDestroy() { if(tones!=null)tones.release();super.onDestroy(); }
    @Override public void onBackPressed() { if(view!=null) view.back(); else super.onBackPressed(); }
    @Override public void onWindowFocusChanged(boolean hasFocus) {super.onWindowFocusChanged(hasFocus); if(hasFocus)immerse();}
    private void immerse() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
    void beep() { if(tones!=null) {try{tones.startTone(ToneGenerator.TONE_PROP_BEEP,55);}catch(Exception ignored){}} }
    void persist() {save();}
    private void save() {
        SharedPreferences.Editor e=getSharedPreferences(STORAGE,Context.MODE_PRIVATE).edit();
        e.putLong("born",game.born).putLong("last",game.lastTick).putLong("hatch",game.hatch)
         .putInt("stage",game.stage).putInt("hunger",game.hunger).putInt("happy",game.happy)
         .putInt("discipline",game.discipline).putInt("weight",game.weight)
         .putInt("dirty",game.dirty).putInt("sickness",game.sickness).putInt("missed",game.missed)
         .putInt("strain",game.strain).putInt("ticks",game.ticks)
         .putInt("genes",game.genes).putInt("personality",game.personality)
         .putBoolean("lights",game.lights).putBoolean("alive",game.alive).apply();
    }
    private void load() {
        SharedPreferences p=getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        if(!p.contains("born")){ game.reset(System.currentTimeMillis());return; }
        game.born=p.getLong("born",0); game.lastTick=p.getLong("last",0);
        game.hatch=p.getLong("hatch",game.born+5*PetGame.MINUTE);
        game.stage=p.getInt("stage",PetGame.EGG);game.hunger=p.getInt("hunger",4);
        game.happy=p.getInt("happy",4);game.discipline=p.getInt("discipline",0);
        game.weight=p.getInt("weight",5);game.dirty=p.getInt("dirty",0);
        game.sickness=p.getInt("sickness",0);game.missed=p.getInt("missed",0);
        game.strain=p.getInt("strain",0);game.ticks=p.getInt("ticks",0);
        game.genes=p.getInt("genes",0);game.personality=p.getInt("personality",0);
        game.lights=p.getBoolean("lights",true);game.alive=p.getBoolean("alive",true);
    }
}
