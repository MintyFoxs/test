from pathlib import Path
p=Path('app/src/main/java/ru/pgws/pocketpet/MainActivity.java')
s=p.read_text(encoding='utf-8')
src='''        if(!NetworkSettings.consent(this))handler.post(this::enrollment);
        else maybeCheckUpdate();'''
assert src in s
s=s.replace(src,'''        // Check updates regardless of whether the player has submitted a nickname.
        // Pet analytics consent must never block software update security checks.
        maybeCheckUpdate();
        if(!NetworkSettings.consent(this))handler.post(this::enrollment);''')
src='if(checkedUpdatesThisLaunch || updateCheckInFlight || !NetworkSettings.consent(this) || isFinishing())return;'
assert src in s
s=s.replace(src,'if(checkedUpdatesThisLaunch || updateCheckInFlight || isFinishing())return;')
insert='''
    // Hidden first-party networking diagnostic: seven taps near the upper-left corner.
    // No tracking and no diagnostic popups during normal gameplay.
    private int diagnosticTaps=0;
    private long diagnosticStart=0L;
    private String diagnosticText="ПРОВЕРКА НЕ ЗАПУЩЕНА";
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent ev){
        if(ev.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){
            long now=android.os.SystemClock.uptimeMillis();
            float limit=100f*getResources().getDisplayMetrics().density;
            if(ev.getX()<=limit && ev.getY()<=limit){
                if(now-diagnosticStart>4500){diagnosticTaps=0;diagnosticStart=now;}
                diagnosticTaps++;
                if(diagnosticTaps>=7){
                    diagnosticTaps=0;
                    showNetworkDiagnostics();
                    return true;
                }
            }else diagnosticTaps=0;
        }
        return super.dispatchTouchEvent(ev);
    }
    private void showNetworkDiagnostics(){
        final TextView content=lcdLabel("ПРОВЕРЯЕМ СОЕДИНЕНИЕ...",16,false,
            Typeface.create("monospace",Typeface.BOLD));
        content.setGravity(Gravity.LEFT);
        content.setPadding(24,24,24,24);
        final AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle("ДИАГНОСТИКА ОБНОВЛЕНИЙ")
            .setView(content)
            .setPositiveButton("КОПИРОВАТЬ",(d,w)->{
                android.content.ClipboardManager manager=(android.content.ClipboardManager)
                  getSystemService(CLIPBOARD_SERVICE);
                if(manager!=null)manager.setPrimaryClip(android.content.ClipData.newPlainText(
                  "PocketPet96 network diagnostic",diagnosticText));
            })
            .setNegativeButton("ЗАКРЫТЬ",null)
            .create();
        dialog.show();
        final android.content.Context ctx=getApplicationContext();
        new Thread(()->{
            String result;
            try{
                int version=ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),0).versionCode;
                JSONObject upd=NetClient.checkUpdate(ctx);
                result="Приложение: "+version+" / 8.5.2\n"+
                    "Согласие: "+(NetworkSettings.consent(ctx)?"ДА":"НЕТ")+"\n"+
                    "Ответ API: "+(upd==null?"НЕТ НОВОЙ ВЕРСИИ":
                        "НОВАЯ "+upd.optInt("versionCode")+" / "+upd.optString("versionName"));
            }catch(Exception e){
                result="ОШИБКА СОЕДИНЕНИЯ\n"+e.getClass().getSimpleName()+
                    "\n"+String.valueOf(e.getMessage());
                Throwable cause=e.getCause();
                if(cause!=null)result+="\nПричина: "+cause.getClass().getSimpleName()+
                   " / "+String.valueOf(cause.getMessage());
                result+="\nAndroid "+android.os.Build.VERSION.RELEASE;
            }
            final String report=result;
            runOnUiThread(()->{
                diagnosticText=report;
                if(dialog.isShowing())content.setText(report);
            });
        },"pet-network-diagnostic").start();
    }
'''
point='    private void downloadUpdate(JSONObject manifest){'
assert point in s
s=s.replace(point,insert+'\n'+point)
p.write_text(s,encoding='utf-8')
g=Path('app/build.gradle')
s=g.read_text(encoding='utf-8')
assert "versionCode 14" in s and "versionName '8.5.1'" in s
s=s.replace("versionName '8.5.1'","versionName '8.5.2'")
g.write_text(s,encoding='utf-8')
print('v8.5.2: update checks independent of analytics; hidden diagnostics available')
