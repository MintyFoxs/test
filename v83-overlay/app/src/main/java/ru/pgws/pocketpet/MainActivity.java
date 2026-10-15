package ru.pgws.pocketpet;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;
import android.text.InputType;
import android.provider.Settings;
import org.json.JSONObject;
import java.util.Random;
import java.io.File;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.Locale;

/** Fullscreen host, with real purr audio and memory-card export to the phone gallery. */
public final class MainActivity extends Activity {
    private final Handler handler=new Handler();
    private PetGame game;
    private PetView view;
    private ToneGenerator tone;
    private MediaPlayer purring;
    private final Random audioRandom=new Random();
    private int lastCat=-1;
    private boolean pendingInstaller=false;
    private boolean running;
    private Bitmap pendingPortrait;
    private static final int STORAGE_PERMISSION=94, EXPORT_DOCUMENT=95;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,45);}catch(Exception ignored){}
        game=PetStore.load(this);
        long now=GameClock.now(this);game.advance(now);PetStore.save(this,game);
        view=new PetView(this,game,this);setContentView(view);immersive();
        if(game.stage>=PetGame.BABY&&game.stage<PetGame.DEAD&&game.alive&&
                (game.name==null||game.name.isEmpty()))view.showName();
        notificationPermission();PetNotifier.schedule(this);SyncScheduler.schedule(this);
    }
    private final Runnable ticker=new Runnable(){@Override public void run(){
        if(!running)return;
        int stage=game.stage;long now=GameClock.now(MainActivity.this);
        game.advance(now);
        if(stage!=game.stage){
            persist();
            if(stage==PetGame.EGG&&game.stage>=PetGame.BABY&&
                    (game.name==null||game.name.isEmpty()))view.showName();
        }
        view.invalidate();handler.postDelayed(this,100);
    }};
    @Override protected void onResume(){super.onResume();running=true;
        game.advance(GameClock.now(this));persist();
        if(game.alive&&game.stage>=PetGame.BABY&&game.stage<PetGame.DEAD&&
                (game.name==null||game.name.isEmpty())&&!view.naming())view.showName();
        handler.removeCallbacks(ticker);handler.post(ticker);
        immersive();PetNotifier.schedule(this);SyncScheduler.schedule(this);
        if(pendingInstaller && (Build.VERSION.SDK_INT<26 || getPackageManager().canRequestPackageInstalls())){pendingInstaller=false;installDownloaded();}
        if(NetworkSettings.consent(this) && NetworkSettings.lastSync(this)!=NetworkSettings.today())new Thread(()->NetClient.sendDaily(getApplicationContext()),"pet-daily-foreground").start();
        maybeCheckUpdate();
    }
    @Override protected void onPause(){running=false;handler.removeCallbacks(ticker);persist();super.onPause();}
    @Override protected void onDestroy(){
        stopPurr();if(tone!=null)tone.release();
        if(pendingPortrait!=null){pendingPortrait.recycle();pendingPortrait=null;}
        super.onDestroy();
    }
    @Override public void onBackPressed(){if(view!=null)view.back();else super.onBackPressed();}
    @Override public void onWindowFocusChanged(boolean has){super.onWindowFocusChanged(has);if(has)immersive();}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
    public void beep(){try{if(tone!=null)tone.startTone(ToneGenerator.TONE_PROP_BEEP,55);}catch(Exception ignored){}}
    public void purr(){
        stopPurr();
        try{
            int[] cats={R.raw.cat_1,R.raw.cat_2,R.raw.cat_3,R.raw.cat_4,R.raw.cat_5};
            int choice=audioRandom.nextInt(cats.length-1);
            if(lastCat>=0 && choice>=lastCat)choice++; // prevent identical sound twice in a row
            lastCat=choice;
            purring=MediaPlayer.create(this,cats[choice]);
            if(purring!=null){
                purring.setVolume(.85f,.85f);
                purring.setOnCompletionListener(mp->{if(purring==mp)purring=null;mp.release();});
                purring.start();
            }
        }catch(Exception e){stopPurr();}
    }
    /** Network screen is optional and never shares private data without explicit consent. */
    public void editPlayerNickname(){
        final EditText input=new EditText(this);input.setSingleLine(true);
        input.setText(NetworkSettings.nickname(this));input.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this).setTitle("Ник игрока")
            .setMessage("Имя отправляется на сервер только с твоего согласия.")
            .setView(input).setPositiveButton("Сохранить",(d,w)->{
                NetworkSettings.nickname(this,NetworkSettings.safe(input.getText().toString(),32));view.invalidate();
            }).setNegativeButton("Отмена",null).show();
    }
    public void editServer(){
        EditText input=new EditText(this);input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(NetworkSettings.endpoint(this));
        new AlertDialog.Builder(this).setTitle("HTTPS-сервер игры")
            .setMessage("Адрес твоего сервера статистики и обновлений. Например https://pets.example.com")
            .setView(input).setPositiveButton("Сохранить",(d,w)->{
                try{NetworkSettings.endpoint(this,input.getText().toString());feedback("Адрес сохранён");view.invalidate();}
                catch(Exception e){feedback("Введите корректный HTTPS-адрес");}
            }).setNegativeButton("Отмена",null).show();
    }
    public void toggleStatistics(){
        if(NetworkSettings.consent(this)){
            NetworkSettings.consent(this,false);SyncScheduler.schedule(this);feedback("Отправка статистики выключена");view.invalidate();return;
        }
        new AlertDialog.Builder(this).setTitle("Ежедневная статистика")
            .setMessage("При согласии примерно после 00:00 (по местному времени) будут отправляться ник игрока, имя и карточка питомца, показатели, модель устройства, версия Android и случайный ID установки. Сервер также увидит твой IP. IP хранится до 7 дней, остальные данные — до 30 дней. По умолчанию отправка выключена. Разрешить?")
            .setNegativeButton("Нет",null).setPositiveButton("Разрешить",(d,w)->{
                NetworkSettings.consent(this,true);SyncScheduler.schedule(this);
                new Thread(()->NetClient.sendDaily(getApplicationContext()),"pet-first-report").start();view.invalidate();
            }).show();
    }
    public void checkUpdates(){
        if(NetworkSettings.endpoint(this).isEmpty()){feedback("Сначала укажи HTTPS-сервер");return;}
        feedback("Проверяю обновления...");
        new Thread(()->{
            try{
                JSONObject result=NetClient.checkUpdate(getApplicationContext());
                runOnUiThread(()->{
                    if(result==null){feedback("Установлена последняя версия");return;}
                    new AlertDialog.Builder(this).setTitle("Доступно обновление "+result.optString("versionName"))
                        .setMessage(result.optString("notes","Новая версия готова к установке.")+"\nТекущий питомец сохранится при одинаковой подписи и идентификаторе приложения. Для установки потребуется подтверждение Android.")
                        .setPositiveButton("Скачать",(d,w)->downloadUpdate(result))
                        .setNegativeButton("Позже",null).show();
                });
            }catch(Exception e){runOnUiThread(()->feedback("Обновление: "+e.getMessage()));}
        },"pet-check-update").start();
    }
    private void maybeCheckUpdate(){
        if(NetworkSettings.endpoint(this).isEmpty())return;
        android.content.SharedPreferences prefs=getSharedPreferences("pocketpet96_network_v1",MODE_PRIVATE);
        long last=prefs.getLong("last_update_check",0),now=System.currentTimeMillis();
        if(last>0&&now-last<12*60*60*1000L)return;
        prefs.edit().putLong("last_update_check",now).apply();checkUpdates();
    }
    private void downloadUpdate(JSONObject manifest){
        feedback("Загружаю APK и проверяю SHA-256...");
        new Thread(()->{
            try{NetClient.download(getApplicationContext(),manifest);runOnUiThread(this::installDownloaded);}
            catch(Exception e){runOnUiThread(()->feedback("Не удалось обновить: "+e.getMessage()));}
        },"pet-update-download").start();
    }
    private void installDownloaded(){
        File f=new File(getFilesDir(),"pet-update.apk");if(!f.isFile())return;
        if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
            pendingInstaller=true;
            new AlertDialog.Builder(this).setTitle("Разрешение на обновление")
              .setMessage("Android должен разрешить установку из этого приложения. Play Защиту отключать не требуется.")
              .setPositiveButton("Открыть настройки",(d,w)->{
                  Intent i=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()));startActivity(i);
              }).setNegativeButton("Отмена",(d,w)->pendingInstaller=false).show();return;
        }
        try{
            Uri uri=Uri.parse("content://"+getPackageName()+".updates/apk");
            Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(uri,"application/vnd.android.package-archive");
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }catch(Exception e){feedback("Не удалось открыть установщик Android");}
    }
    private void stopPurr(){if(purring!=null){try{purring.stop();}catch(Exception ignored){}purring.release();purring=null;}}
    public void feedback(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    public void persist(){PetStore.save(this,game);}
    private void notificationPermission(){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},93);
    }
    /** Saved with no storage permission on Android 10+, legacy permission/fallback on older OS. */
    public void saveMemorial(){
        if(game.alive){beep();return;}
        if(pendingPortrait!=null)pendingPortrait.recycle();
        pendingPortrait=MemorialCard.render(this,game);
        if(Build.VERSION.SDK_INT>=29){writeToGallery();return;}
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},STORAGE_PERMISSION);
            return;
        }
        writeToGallery();
    }
    private void writeToGallery(){
        if(pendingPortrait==null)return;
        String title=fileName();
        if(Build.VERSION.SDK_INT>=29){
            Uri uri=null;
            try{
                ContentValues values=new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME,title);
                values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/PocketPet96");
                values.put(MediaStore.Images.Media.IS_PENDING,1);
                uri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
                if(uri==null)throw new IllegalStateException("MediaStore insert returned null");
                try(OutputStream out=getContentResolver().openOutputStream(uri)){
                    if(out==null||!pendingPortrait.compress(Bitmap.CompressFormat.PNG,100,out))
                        throw new IllegalStateException("PNG encoding failed");
                }
                ContentValues finish=new ContentValues();finish.put(MediaStore.Images.Media.IS_PENDING,0);
                getContentResolver().update(uri,finish,null,null);
                done("Фотография сохранена в галерею");return;
            }catch(Exception e){if(uri!=null)try{getContentResolver().delete(uri,null,null);}catch(Exception ignored){}openDocument();return;}
        }
        try{
            String result=MediaStore.Images.Media.insertImage(getContentResolver(),pendingPortrait,title,"На память о питомце");
            if(result==null)throw new IllegalStateException("Cannot save");
            done("Фотография сохранена в галерею");
        }catch(Exception error){openDocument();}
    }
    private String fileName(){
        String safe=game.name==null?"pet":game.name.replaceAll("[^a-zA-Zа-яА-ЯёЁ0-9_-]","_");
        if(safe.length()==0)safe="pet";
        return String.format(Locale.US,"PocketPet96_%s_%d.png",safe,game.diedAt>0?game.diedAt:System.currentTimeMillis());
    }
    private void done(String message){
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
        if(pendingPortrait!=null){pendingPortrait.recycle();pendingPortrait=null;}
    }
    private void openDocument(){
        try{
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/png");
            i.putExtra(Intent.EXTRA_TITLE,fileName());
            startActivityForResult(i,EXPORT_DOCUMENT);
            Toast.makeText(this,"Выбери папку для памятного фото",Toast.LENGTH_LONG).show();
        }catch(Exception e){done("Не удалось открыть сохранение изображения");}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==STORAGE_PERMISSION){
            if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)writeToGallery();
            else openDocument();
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=EXPORT_DOCUMENT)return;
        if(result==RESULT_OK&&data!=null&&data.getData()!=null&&pendingPortrait!=null){
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                if(out==null||!pendingPortrait.compress(Bitmap.CompressFormat.PNG,100,out))
                    throw new IllegalStateException("PNG encoding failed");
                done("Памятная фотография сохранена");
            }catch(Exception error){done("Не удалось сохранить PNG");}
        }else done("Сохранение отменено");
    }
}
