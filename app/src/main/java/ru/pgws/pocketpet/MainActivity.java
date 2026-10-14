package ru.pgws.pocketpet;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;

/** Host activity does not show native input dialogs: all controls are in the 8-bit LCD. */
public final class MainActivity extends Activity {
    private final Handler handler=new Handler();
    private PetGame game;
    private PetView view;
    private ToneGenerator tone;
    private boolean running;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,45);}catch(Exception ignored){}
        game=PetStore.load(this);
        long now=GameClock.now(this);game.advance(now);PetStore.save(this,game);
        view=new PetView(this,game,this);setContentView(view);immersive();
        if(game.stage>=PetGame.BABY&&game.stage<PetGame.DEAD&&game.alive&&(game.name==null||game.name.isEmpty()))view.showName();
        permission();PetNotifier.schedule(this);
    }
    private final Runnable ticker=new Runnable(){@Override public void run(){
        if(!running)return;
        int stage=game.stage;long now=GameClock.now(MainActivity.this);
        game.advance(now);
        if(stage!=game.stage){
            persist();
            if(stage==PetGame.EGG&&game.stage>=PetGame.BABY&&(game.name==null||game.name.isEmpty()))view.showName();
        }
        view.invalidate();handler.postDelayed(this,100);
    }};
    @Override protected void onResume(){super.onResume();running=true;
        game.advance(GameClock.now(this));persist();
        if(game.alive&&game.stage>=PetGame.BABY&&game.stage<PetGame.DEAD&&(game.name==null||game.name.isEmpty())&&!view.naming())view.showName();
        handler.removeCallbacks(ticker);handler.post(ticker);
        immersive();PetNotifier.schedule(this);
    }
    @Override protected void onPause(){running=false;handler.removeCallbacks(ticker);persist();super.onPause();}
    @Override protected void onDestroy(){if(tone!=null)tone.release();super.onDestroy();}
    @Override public void onBackPressed(){if(view!=null)view.back();else super.onBackPressed();}
    @Override public void onWindowFocusChanged(boolean has){super.onWindowFocusChanged(has);if(has)immersive();}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
    public void beep(){try{if(tone!=null)tone.startTone(ToneGenerator.TONE_PROP_BEEP,55);}catch(Exception ignored){}}
    public void persist(){PetStore.save(this,game);}
    private void permission(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},93);
    }
}
