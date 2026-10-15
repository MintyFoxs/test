package ru.pgws.pocketpet;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Base64;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/** HTTPS API; no analytics or device metadata transmitted until the player opts in. */
public final class NetClient {
    private NetClient(){}
    private static HttpURLConnection connect(String url,String method)throws Exception{
        if(!url.startsWith("https://") || new java.net.URI(url).getHost()==null || new java.net.URI(url).getUserInfo()!=null)throw new IllegalArgumentException("Только HTTPS-соединение");
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(12000);c.setReadTimeout(18000);c.setRequestMethod(method);
        c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept","application/json");
        return c;
    }
    private static byte[] readLimited(InputStream in,int maximum)throws Exception{
        try(InputStream src=in;ByteArrayOutputStream b=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n;while((n=src.read(buf))!=-1){if(b.size()+n>maximum)throw new Exception("Слишком большой ответ");b.write(buf,0,n);}return b.toByteArray();
        }
    }
    public static boolean sendDaily(Context ctx){
        if(!NetworkSettings.consent(ctx))return false;
        String base=NetworkSettings.endpoint(ctx),nick=NetworkSettings.nickname(ctx);
        if(base.isEmpty()||nick.isEmpty())return false;
        long today=NetworkSettings.today();if(NetworkSettings.lastSync(ctx)==today)return true;
        PetGame pet=PetStore.load(ctx);pet.advance(GameClock.now(ctx));
        Bitmap card=PetCard.render(ctx,pet);
        ByteArrayOutputStream out=new ByteArrayOutputStream();card.compress(Bitmap.CompressFormat.PNG,100,out);card.recycle();
        try{
            JSONObject o=new JSONObject();o.put("schema",1);o.put("installId",NetworkSettings.installId(ctx));
            o.put("nickname",nick);o.put("petName",pet.name==null?"":pet.name);
            o.put("petGenes",pet.genes);o.put("petStage",pet.portraitStage());
            o.put("petAgeDays",pet.ageDays(GameClock.now(ctx)));o.put("petAlive",pet.alive);
            o.put("hunger",pet.hunger);o.put("happy",pet.happy);
            o.put("sickness",pet.sickness);o.put("dirty",pet.dirty);o.put("gamesWon",pet.gamesWon);
            o.put("localDate",today);
            JSONObject d=new JSONObject();d.put("manufacturer",Build.MANUFACTURER);d.put("model",Build.MODEL);
            d.put("android",Build.VERSION.RELEASE);d.put("api",Build.VERSION.SDK_INT);
            o.put("device",d);o.put("cardPngBase64",Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP));
            byte[] body=o.toString().getBytes("UTF-8");if(body.length>180_000)return false;
            HttpURLConnection c=connect(base+"/v1/pets/daily","POST");
            try{
                c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                c.setFixedLengthStreamingMode(body.length);
                try(OutputStream stream=c.getOutputStream()){stream.write(body);}
                int code=c.getResponseCode();if(code>=200&&code<300){NetworkSettings.lastSync(ctx,today);return true;}
            }finally{c.disconnect();}
        }catch(Exception ignored){}return false;
    }
    public static JSONObject checkUpdate(Context ctx)throws Exception{
        String base=NetworkSettings.endpoint(ctx);
        if(base.isEmpty())throw new Exception("Сначала укажи адрес HTTPS-сервера");
        HttpURLConnection c=connect(base+"/v1/updates?versionCode="+ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),0).versionCode,"GET");
        try{
            if(c.getResponseCode()!=200)throw new Exception("Сервер обновлений не отвечает");
            JSONObject j=new JSONObject(new String(readLimited(c.getInputStream(),16_384),"UTF-8"));
            if(!"ru.pgws.pocketpet".equals(j.optString("package")))throw new Exception("Неверный идентификатор приложения");
            if(j.optInt("versionCode",0)<=ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),0).versionCode)return null;
            String url=j.optString("apkUrl");
            if(!url.startsWith("https://") || new java.net.URI(url).getHost()==null)throw new Exception("Адрес обновления должен использовать HTTPS");
            if(!j.optString("sha256").matches("[0-9a-fA-F]{64}"))throw new Exception("Нет SHA-256 обновления");
            return j;
        }finally{c.disconnect();}
    }
    public static File download(Context ctx,JSONObject update)throws Exception{
        HttpURLConnection c=connect(update.getString("apkUrl"),"GET");
        File out=new File(ctx.getFilesDir(),"pet-update.apk");
        File temp=new File(ctx.getFilesDir(),"pet-update.tmp");
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try{
            if(c.getResponseCode()!=200)throw new Exception("Не удалось скачать обновление");
            int received=0;
            try(InputStream in=c.getInputStream();OutputStream o=new FileOutputStream(temp)){
                byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1){received+=n;
                    if(received>40*1024*1024)throw new Exception("Обновление слишком большое");
                    digest.update(buffer,0,n);o.write(buffer,0,n);
                }
            }
            StringBuilder hex=new StringBuilder();for(byte v:digest.digest())hex.append(String.format(java.util.Locale.US,"%02x",v&255));
            if(!hex.toString().equalsIgnoreCase(update.getString("sha256")))throw new Exception("Проверка целостности APK не пройдена");
            if(!temp.renameTo(out))throw new Exception("Не удалось подготовить файл");
            return out;
        }finally{c.disconnect();temp.delete();}
    }
}
