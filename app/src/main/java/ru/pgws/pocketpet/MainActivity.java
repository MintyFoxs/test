package ru.pgws.pocketpet;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

public final class MainActivity extends Activity {
    private final Handler handler=new Handler();
    private PetGame game;
    private PetView view;
    private ToneGenerator tones;
    private boolean active, nameDialog;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try {tones=new ToneGenerator(AudioManager.STREAM_MUSIC,50);}catch(Exception ignored){}
        game=PetStore.load(this);
        game.advance(System.currentTimeMillis());
        PetStore.save(this,game);
        view=new PetView(this,game,this);
        setContentView(view);
        immerse();
        askForNotificationPermission();
        PetNotifier.schedule(this);
    }
    private final Runnable ticker=new Runnable(){
        @Override public void run(){
            if(!active)return;
            int before=game.stage;
            game.advance(System.currentTimeMillis());
            if(before!=game.stage)persist();
            if(view!=null)view.invalidate();
            askNameIfReady();
            handler.postDelayed(this,400L);
        }
    };
    @Override protected void onResume(){
        super.onResume();active=true;
        game.advance(System.currentTimeMillis());
        persist();
        handler.removeCallbacks(ticker);handler.post(ticker);
        immerse();PetNotifier.schedule(this);
    }
    @Override protected void onPause(){
        active=false;handler.removeCallbacks(ticker);persist();super.onPause();
    }
    @Override protected void onDestroy(){if(tones!=null)tones.release();super.onDestroy();}
    @Override public void onBackPressed(){if(view!=null)view.back();else super.onBackPressed();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immerse();}
    private void immerse(){
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
            View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
    void beep(){if(tones!=null){try{tones.startTone(ToneGenerator.TONE_PROP_BEEP,55);}catch(Exception ignored){}}}
    void persist(){PetStore.save(this,game);}
    void namePetAgain(){showNameDialog(true);}
    private void askNameIfReady(){
        if(game.alive && game.stage>=PetGame.BABY && game.stage<PetGame.DEAD && (game.name==null||game.name.isEmpty()))showNameDialog(false);
    }
    private void showNameDialog(boolean editing){
        if(nameDialog || isFinishing() || (Build.VERSION.SDK_INT>=17&&isDestroyed()))return;
        nameDialog=true;
        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        input.setHint("Имя питомца");
        input.setText(editing?game.name:"");
        int pad=(int)(24*getResources().getDisplayMetrics().density);
        final android.widget.FrameLayout wrap=new android.widget.FrameLayout(this);
        wrap.setPadding(pad,pad/2,pad,pad/2);
        wrap.addView(input);
        AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle(editing?"Изменить имя":"Питомец вылупился!")
            .setMessage("Как назовём питомца?")
            .setView(wrap)
            .setPositiveButton("Сохранить",(d,w)->{
                String name=input.getText().toString().trim();
                game.name=name.isEmpty()?"Питомец":name;
                persist();PetNotifier.schedule(this);view.invalidate();
            })
            .setNegativeButton("Позже",(d,w)->{
                // Cancel without repeatedly opening another dialog every 400 ms.
                if(!editing){game.name="Питомец";persist();}
            }).create();
        dialog.setOnDismissListener(d->{nameDialog=false;immerse();});
        dialog.show();
    }
    private void askForNotificationPermission(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},93);
        }
    }
}
