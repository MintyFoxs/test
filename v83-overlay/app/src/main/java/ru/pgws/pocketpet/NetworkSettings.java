package ru.pgws.pocketpet;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.SecureRandom;
import java.util.Locale;

/** Telemetry is opt-in. No device fingerprint, advertising ID or phone permissions. */
public final class NetworkSettings {
    private static final String PREF="pocketpet96_network_v1";
    private NetworkSettings(){}
    private static SharedPreferences p(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    public static boolean consent(Context c){return p(c).getBoolean("statistics_consent",false);}
    public static void consent(Context c,boolean on){p(c).edit().putBoolean("statistics_consent",on).apply();}
    public static String nickname(Context c){return p(c).getString("nickname","");}
    public static void nickname(Context c,String s){p(c).edit().putString("nickname",safe(s,32)).apply();}
    public static String endpoint(Context c){return p(c).getString("endpoint","");}
    public static boolean validEndpoint(String s){
        if(s==null || s.length()>200 || !s.startsWith("https://") || s.contains("@"))return false;
        try{java.net.URI u=new java.net.URI(s);return u.getHost()!=null&&!u.getHost().isEmpty()&&u.getRawQuery()==null&&u.getRawFragment()==null;}
        catch(Exception e){return false;}
    }
    public static void endpoint(Context c,String value){
        String cleaned=value.trim().replaceAll("/+$","");
        if(!cleaned.isEmpty()&&!validEndpoint(cleaned))throw new IllegalArgumentException("Нужен HTTPS-адрес сервера");
        p(c).edit().putString("endpoint",cleaned).apply();
    }
    public static long lastSync(Context c){return p(c).getLong("last_sync_day",-1);}
    public static void lastSync(Context c,long d){p(c).edit().putLong("last_sync_day",d).apply();}
    public static String installId(Context c){
        String id=p(c).getString("random_install_id","");if(!id.isEmpty())return id;
        byte[] b=new byte[16];new SecureRandom().nextBytes(b);StringBuilder s=new StringBuilder();
        for(byte v:b)s.append(String.format(Locale.US,"%02x",v&255));
        id=s.toString();p(c).edit().putString("random_install_id",id).apply();return id;
    }
    public static String safe(String s,int max){if(s==null)return "";String normalized=s.replaceAll("[\\p{Cntrl}]","").trim();return normalized.substring(0,Math.min(max,normalized.length()));}
    /** Local midnight key, using calendar date (not virtual clock or UTC). */
    public static long today(){java.util.Calendar t=java.util.Calendar.getInstance();return t.get(java.util.Calendar.YEAR)*10000L+(t.get(java.util.Calendar.MONTH)+1)*100+t.get(java.util.Calendar.DAY_OF_MONTH);}
}
