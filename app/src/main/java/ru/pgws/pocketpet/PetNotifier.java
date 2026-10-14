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
import android.os.Build;

/** Inexact, battery-friendly state check: survives process death, can be delayed by Doze. */
public final class PetNotifier extends BroadcastReceiver {
    private static final String CHANNEL="pocket_pet_needs";
    private static final int REQUEST_CHECK=712, REQUEST_OPEN=713, NOTIFICATION_ID=96;
    private static final long INTERVAL=30*60_000L;
    private static final long REPEAT=2*60*60_000L;
    private static final String NS="pocketpet96_notifications";

    public static void schedule(Context context){
        AlarmManager a=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(a==null)return;
        Intent i=new Intent(context,PetNotifier.class).setAction("ru.pgws.pocketpet.CHECK");
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent p=PendingIntent.getBroadcast(context,REQUEST_CHECK,i,flags);
        long next=System.currentTimeMillis()+INTERVAL;
        // setAndAllowWhileIdle is inexact under modern Android; no exact-alarm permission necessary.
        if(Build.VERSION.SDK_INT>=23)a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,p);
        else if(Build.VERSION.SDK_INT>=19)a.setWindow(AlarmManager.RTC_WAKEUP,next,INTERVAL/2,p);
        else a.set(AlarmManager.RTC_WAKEUP,next,p);
    }

    @Override public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent==null?null:intent.getAction())){
            schedule(context);return;
        }
        try{check(context);}finally{schedule(context);}
    }
    public static void check(Context ctx){
        long now=System.currentTimeMillis();
        PetGame g=PetStore.load(ctx);
        int oldStage=g.stage;
        g.advance(now);
        PetStore.save(ctx,g);
        SharedPreferences p=ctx.getSharedPreferences(NS,Context.MODE_PRIVATE);
        int reason=reason(g,now);
        int lastReason=p.getInt("reason",0);
        long last=p.getLong("time",0);
        if(reason==0){
            if(lastReason!=0)p.edit().putInt("reason",0).apply();
            return;
        }
        if(reason!=lastReason || now-last>=REPEAT || now<last){
            if(authorized(ctx)){
                notifyNow(ctx,g,reason);
                p.edit().putInt("reason",reason).putLong("time",now).apply();
            }
        }
    }
    private static boolean authorized(Context ctx){
        return Build.VERSION.SDK_INT<33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)== PackageManager.PERMISSION_GRANTED;
    }
    /** stable priority: hatching > sickness > dirt > hunger > mood > sleep */
    public static int reason(PetGame g,long now){
        if(!g.alive)return 0;
        if(g.stage==PetGame.BABY && (g.name==null||g.name.isEmpty()))return 1;
        if(g.stage==PetGame.EGG)return 0;
        if(g.sickness>0)return 2;
        if(g.dirty>=3)return 3;
        if(g.hunger<=1)return 4;
        if(g.happy<=1)return 5;
        if(g.sleeping(now)&&g.lights)return 6;
        return 0;
    }
    private static void notifyNow(Context ctx,PetGame g,int reason){
        NotificationManager m=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if(m==null)return;
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Забота о питомце",NotificationManager.IMPORTANCE_DEFAULT);
            ch.setDescription("Сигналы о потребностях виртуального питомца");
            m.createNotificationChannel(ch);
        }
        String name=(g.name!=null&&!g.name.trim().isEmpty())?g.name:"Питомец";
        String message;
        switch(reason){
            case 1:message="Я вылупился! Открой игру и дай мне имя.";break;
            case 2:message="Мне нехорошо. Нужны лекарства.";break;
            case 3:message="Пора прибраться за мной.";break;
            case 4:message="Я голоден. Покорми меня!";break;
            case 5:message="Мне скучно. Поиграй со мной!";break;
            default:message="Я хочу спать. Выключи свет.";break;
        }
        Intent open=new Intent(ctx,MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi=PendingIntent.getActivity(ctx,REQUEST_OPEN,open,flags);
        Notification.Builder b=Build.VERSION.SDK_INT>=26 ? new Notification.Builder(ctx,CHANNEL):new Notification.Builder(ctx);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
         .setContentTitle(name+" зовёт тебя")
         .setContentText(message)
         .setContentIntent(pi).setAutoCancel(true).setWhen(System.currentTimeMillis());
        if(Build.VERSION.SDK_INT<26)b.setPriority(Notification.PRIORITY_DEFAULT);
        m.notify(NOTIFICATION_ID,b.build());
    }
}
