from pathlib import Path
import shutil
p=Path("app/src/main/java/ru/pgws/pocketpet")
shutil.copyfile("tools/FallbackTlsSocketFactory.java",p/"FallbackTlsSocketFactory.java")
f=p/"NetClient.java"
s=f.read_text(encoding="utf-8")

def replace_exact(old,new):
    global s
    if old not in s:
        raise RuntimeError("NetClient unexpected shape: "+old[:130])
    s=s.replace(old,new,1)

replace_exact(
    "import java.net.HttpURLConnection;\nimport java.net.URL;",
    "import java.net.HttpURLConnection;\nimport java.net.UnknownHostException;\nimport java.net.URL;\nimport javax.net.ssl.HttpsURLConnection;\nimport javax.net.ssl.SSLSocketFactory;"
)
replace_exact(
    "    private static byte[] readLimited(InputStream in,int maximum)throws Exception{",
    """    /** Preserve ordinary Android DNS normally, fall back to known MYMP VPS only on EAI_NODATA. */
    private static HttpURLConnection connect(String url,String method,boolean ipFallback)throws Exception{
        HttpURLConnection c=connect(url,method);
        if(ipFallback){
            if(!(c instanceof HttpsURLConnection))throw new java.io.IOException("Only verified TLS is allowed");
            HttpsURLConnection https=(HttpsURLConnection)c;
            https.setSSLSocketFactory(new FallbackTlsSocketFactory(
                (SSLSocketFactory)SSLSocketFactory.getDefault()));
            Log.i("PocketPetNet","DNS-independent TLS route with original HTTPS hostname");
        }
        return c;
    }
    private static byte[] readLimited(InputStream in,int maximum)throws Exception{"""
)
replace_exact(
    '            HttpURLConnection c=connect(base+"/v1/pets/daily","POST");',
    '            if(postSnapshot(base,body)){NetworkSettings.lastSync(ctx,today);NetworkSettings.lastPetBorn(ctx,pet.born);Log.i("PocketPetNet","Daily pet snapshot accepted by MYMP");return true;}\n            /* POST request moved into a retriable helper to recover from EAI_NODATA. */\n            /*'
)
old='''            try{
                c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                c.setFixedLengthStreamingMode(body.length);
                try(OutputStream stream=c.getOutputStream()){stream.write(body);}
                int code=c.getResponseCode();if(code>=200&&code<300){
                    NetworkSettings.lastSync(ctx,today);NetworkSettings.lastPetBorn(ctx,pet.born);
                    Log.i("PocketPetNet","Daily pet snapshot accepted by MYMP");return true;
                }
                String message="";
                try{message=new String(readLimited(c.getErrorStream(),4096),"UTF-8");}catch(Exception ignored){}
                Log.w("PocketPetNet","Daily snapshot rejected: HTTP "+code+" "+message);
            }finally{c.disconnect();}
'''
replace_exact(old,"            */\n")
# Safer to use a shared sender that connects with normal DNS, then retries on UnknownHostException.
replace_exact(
    '    public static JSONObject checkUpdate(Context ctx)throws Exception{',
    '''    private static boolean postSnapshot(String base,byte[] body)throws Exception{
        try{return postSnapshotOnce(base,body,false);}
        catch(UnknownHostException dns){
            Log.w("PocketPetNet","Android DNS unavailable for reports; using verified TLS fallback");
            return postSnapshotOnce(base,body,true);
        }
    }
    private static boolean postSnapshotOnce(String base,byte[] body,boolean fixedIp)throws Exception{
        HttpURLConnection c=connect(base+"/v1/pets/daily","POST",fixedIp);
        try{
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            c.setFixedLengthStreamingMode(body.length);
            try(OutputStream stream=c.getOutputStream()){stream.write(body);}
            int code=c.getResponseCode();
            if(code>=200&&code<300)return true;
            String message="";
            try{message=new String(readLimited(c.getErrorStream(),4096),"UTF-8");}catch(Exception ignored){}
            Log.w("PocketPetNet","Daily snapshot rejected HTTP "+code+" "+message);
            return false;
        }finally{c.disconnect();}
    }
    public static JSONObject checkUpdate(Context ctx)throws Exception{'''
)
replace_exact(
    '''    private static JSONObject checkUpdateAt(Context ctx,String base)throws Exception{
        int installed=ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),0).versionCode;
        HttpURLConnection c=connect(base+"/v1/updates?versionCode="+installed,"GET");''',
    '''    private static JSONObject checkUpdateAt(Context ctx,String base)throws Exception{
        try{return checkUpdateAtOnce(ctx,base,false);}
        catch(UnknownHostException dns){
            Log.w("PocketPetNet","Android DNS unavailable for "+base+"; trying certificate-verified IP");
            return checkUpdateAtOnce(ctx,base,true);
        }
    }
    private static JSONObject checkUpdateAtOnce(Context ctx,String base,boolean fixedIp)throws Exception{
        int installed=ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),0).versionCode;
        HttpURLConnection c=connect(base+"/v1/updates?versionCode="+installed,"GET",fixedIp);'''
)
replace_exact(
    '''    public static File download(Context ctx,JSONObject update)throws Exception{
        HttpURLConnection c=connect(update.getString("apkUrl"),"GET");''',
    '''    public static File download(Context ctx,JSONObject update)throws Exception{
        try{return downloadOnce(ctx,update,false);}
        catch(UnknownHostException dns){
            Log.w("PocketPetNet","Android DNS unavailable during APK download; using verified TLS fallback");
            return downloadOnce(ctx,update,true);
        }
    }
    private static File downloadOnce(Context ctx,JSONObject update,boolean fixedIp)throws Exception{
        HttpURLConnection c=connect(update.getString("apkUrl"),"GET",fixedIp);'''
)
f.write_text(s,encoding="utf-8")
g=Path("app/build.gradle")
s=g.read_text(encoding="utf-8")
assert "versionCode 14" in s and "versionName '8.5.2'" in s, "Expected v8.5.2 diagnostic base"
g.write_text(s.replace("versionName '8.5.2'","versionName '8.5.3'"),encoding="utf-8")
assert "FallbackTlsSocketFactory" in f.read_text()
print("v8.5.3: DNS fallback for update API / APK / reports, standard TLS verification, no changed saves")
