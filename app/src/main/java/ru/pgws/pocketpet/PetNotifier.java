package ru.pgws.pocketpet;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;

/** Inexact battery-friendly notifications. Native OS frame is system-controlled. */
public final class PetNotifier extends BroadcastReceiver {
    private static final String CHANNEL="pocket_pet_needs_v5";
    private static final String NS="pocketpet96_notifications";
    private static final long INTERVAL=30*60_000L,REPEAT=2*60*60_000L;
    public static void schedule(Context ctx){
        AlarmManager manager=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        if(manager==null)return;
        Intent i=new Intent(ctx,PetNotifier.class).setAction("ru.pgws.pocketpet.CHECK");
        int flags=PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
        PendingIntent pi=PendingIntent.getBroadcast(ctx,712,i,flags);
        long at=System.currentTimeMillis()+INTERVAL;
        if(Build.VERSION.SDK_INT>=23)manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        else manager.setWindow(AlarmManager.RTC_WAKEUP,at,INTERVAL/2,pi);
    }
    @Override public void onReceive(Context ctx,Intent i){
        if(Intent.ACTION_BOOT_COMPLETED.equals(i==null?null:i.getAction())){schedule(ctx);return;}
        try{check(ctx);}finally{schedule(ctx);}
    }
    public static int reason(PetGame g,long now){
        if(!g.alive)return 0;
        if(g.stage==PetGame.BABY && (g.name==null||g.name.isEmpty()))return 1;
        if(g.stage==PetGame.EGG)return 0;
        if(g.sickness>0)return 2;
        if(g.dirty>=2)return 3;
        if(g.hunger<=1)return 4;
        if(g.happy<=1)return 5;
        if(g.sleeping(now)&&g.lights)return 6;
        return 0;
    }
    public static void check(Context ctx){
        long now=GameClock.now(ctx);
        PetGame g=PetStore.load(ctx);g.advance(now);PetStore.save(ctx,g);
        int reason=reason(g,now);
        SharedPreferences prefs=ctx.getSharedPreferences(NS,Context.MODE_PRIVATE);
        int lastReason=prefs.getInt("reason",0);
        long last=prefs.getLong("stamp",0);
        long wall=System.currentTimeMillis();
        if(reason==0){if(lastReason!=0)prefs.edit().putInt("reason",0).apply();return;}
        if((reason!=lastReason||wall-last>=REPEAT||wall<last)&&authorized(ctx)){
            show(ctx,g,reason);
            prefs.edit().putInt("reason",reason).putLong("stamp",wall).apply();
        }
    }
    private static boolean authorized(Context c){return Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
    public static void debugNotify(Context c,PetGame g){if(authorized(c))show(c,g,4);}
    private static void show(Context c,PetGame g,int reason){
        NotificationManager n=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);if(n==null)return;
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel channel=new NotificationChannel(CHANNEL,"Сигналы LCD-питомца",NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Напоминания о потребностях карманного питомца");n.createNotificationChannel(channel);
        }
        String name=(g.name==null||g.name.isEmpty())?"ПИТОМЕЦ":g.name;
        String[] messages={"","Я ВЫЛУПИЛСЯ! НАЗОВИ МЕНЯ!","МНЕ НЕХОРОШО! ВЫЛЕЧИ МЕНЯ!",
            "ОЙ! НУЖНО УБРАТЬ ГРЯЗЬ!","МОЙ ЖИВОТИК УРЧИТ! ПОКОРМИ МЕНЯ!",
            "МНЕ СКУЧНО! ПОИГРАЕМ?","ХОЧУ СПАТЬ! ВЫКЛЮЧИ СВЕТ!"};
        String message=messages[Math.max(1,Math.min(messages.length-1,reason))];
        Intent open=new Intent(c,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
        PendingIntent pi=PendingIntent.getActivity(c,713,open,flags);
        Bitmap card=drawCard(c,g,name,message);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
        b.setSmallIcon(R.drawable.notif_icon).setLargeIcon(card).setContentTitle(name+" • PIXEL PET")
            .setContentText(message).setContentIntent(pi).setAutoCancel(true)
            .setWhen(System.currentTimeMillis()).setShowWhen(true)
            .setStyle(new Notification.BigPictureStyle().bigPicture(card).setSummaryText(message));
        if(Build.VERSION.SDK_INT<26)b.setPriority(Notification.PRIORITY_DEFAULT);
        n.notify(96,b.build());
    }
    private static Bitmap drawCard(Context ctx,PetGame g,String name,String message){
        Bitmap image=Bitmap.createBitmap(512,224,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(image);c.drawColor(Color.rgb(181,208,155));
        Paint p=new Paint();p.setColor(Color.rgb(45,68,51));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);
        c.drawRect(5,5,507,219,p);p.setStyle(Paint.Style.FILL);
        PixelFont f=new PixelFont(ctx);
        f.draw(c,"PIXEL PET / УВЕДОМЛЕНИЕ",20,18,17,Color.rgb(45,68,51),false);
        f.draw(c,name.toUpperCase(java.util.Locale.ROOT),20,54,23,Color.rgb(45,68,51),false);
        f.draw(c,message,20,177,13,Color.rgb(45,68,51),false);
        String[] s=PetSprites.create(g.genes,g.stage,g.personality);
        int step=4,x0=209,y0=73;
        for(int y=0;y<s.length;y++)for(int x=0;x<s[y].length();x++){
            char ch=s[y].charAt(x);
            if(ch=='.')continue;
            p.setColor(ch=='#'?Color.rgb(45,68,51):ch=='o'?Color.rgb(113,153,104):Color.rgb(226,238,186));
            c.drawRect(x0+x*step,y0+y*step,x0+(x+1)*step,y0+(y+1)*step,p);
        }
        return image;
    }
}
