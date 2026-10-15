from pathlib import Path
root=Path(".")
src=root/"app/src/main/java/ru/pgws/pocketpet"
net=src/"NetClient.java"
s=net.read_text(encoding="utf-8")
a=s.index("    public static JSONObject checkUpdate(Context ctx)throws Exception{")
b=s.index("    public static File download(Context ctx,JSONObject update)throws Exception{",a)
new='''    /** Two certified first-party hostnames, never plain HTTP. */
    public static JSONObject checkUpdate(Context ctx)throws Exception{
        Exception last=null;
        for(String base:new String[]{"https://stat.mymp.fun","https://mymp.fun"}){
            try{return checkUpdateAt(ctx,base);}
            catch(Exception problem){
                Log.w("PocketPetNet","Update lookup failed at "+base+": "+problem.getClass().getSimpleName()+": "+problem.getMessage(),problem);
                last=problem;
            }
        }
        if(last!=null)throw last;
        throw new java.io.IOException("No endpoint");
    }
    private static JSONObject checkUpdateAt(Context ctx,String base)throws Exception{
        int installed=ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),0).versionCode;
        HttpURLConnection c=connect(base+"/v1/updates?versionCode="+installed,"GET");
        try{
            int status=c.getResponseCode();
            if(status!=200)throw new java.io.IOException("Update API HTTP "+status);
            JSONObject j=new JSONObject(new String(readLimited(c.getInputStream(),16384),"UTF-8"));
            if(!"ru.pgws.pocketpet".equals(j.optString("package")))throw new java.io.IOException("Unexpected package");
            if(j.optInt("versionCode",0)<=installed)return null;
            String url=j.optString("apkUrl");
            java.net.URI link=new java.net.URI(url);
            if(!"https".equalsIgnoreCase(link.getScheme()) ||
               !"mymp.fun".equalsIgnoreCase(link.getHost()) ||
               !link.getPath().startsWith("/download/") ||
               link.getPort()!=-1 || link.getUserInfo()!=null)throw new java.io.IOException("Invalid update URL");
            if(!j.optString("sha256").matches("[0-9a-fA-F]{64}"))throw new java.io.IOException("Invalid SHA-256");
            return j;
        }finally{c.disconnect();}
    }
'''
s=s[:a]+new+s[b:]
net.write_text(s,encoding="utf-8")
main=src/"MainActivity.java"
s=main.read_text(encoding="utf-8")
old="    private boolean checkedUpdatesThisLaunch=false;"
assert old in s
s=s.replace(old,old+"\n    private boolean updateCheckInFlight=false;\n    private int updateFailures=0;")
a=s.index("    private void maybeCheckUpdate(){")
b=s.index("    private void downloadUpdate(JSONObject manifest){",a)
replacement='''    private void maybeCheckUpdate(){
        if(checkedUpdatesThisLaunch || updateCheckInFlight || !NetworkSettings.consent(this) || isFinishing())return;
        updateCheckInFlight=true;
        new Thread(()->{
            try{
                JSONObject result=NetClient.checkUpdate(getApplicationContext());
                runOnUiThread(()->{
                    updateCheckInFlight=false;
                    checkedUpdatesThisLaunch=true;
                    if(result!=null && !isFinishing()){
                        new AlertDialog.Builder(this).setTitle("НОВАЯ ВЕРСИЯ "+result.optString("versionName"))
                            .setMessage(result.optString("notes","ДОСТУПНО ОБНОВЛЕНИЕ"))
                            .setPositiveButton("СКАЧАТЬ",(d,w)->downloadUpdate(result))
                            .setNegativeButton("ПОЗЖЕ",null).show();
                    }
                });
            }catch(Exception ex){
                Log.w("PocketPetNet","Update check failed, retry planned: "+ex.getMessage(),ex);
                runOnUiThread(()->{
                    updateCheckInFlight=false;
                    if(!isFinishing() && updateFailures<6){
                        long delay=15000L*(++updateFailures);
                        handler.postDelayed(()->{if(running)maybeCheckUpdate();},delay);
                    }
                });
            }
        },"pet-auto-update").start();
    }
'''
s=s[:a]+replacement+s[b:]
main.write_text(s,encoding="utf-8")
gradle=root/"app/build.gradle"
s=gradle.read_text(encoding="utf-8")
assert "versionCode 14" in s and "versionName '8.5.0'" in s
gradle.write_text(s.replace("versionName '8.5.0'","versionName '8.5.1'"),encoding="utf-8")
print("PocketPet96 v8.5.1 patch applied; versionCode 14, saves unchanged")
