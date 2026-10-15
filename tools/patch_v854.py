from pathlib import Path
import shutil
src=Path("app/src/main/java/ru/pgws/pocketpet")
for name in ("DirectVerifiedHttps.java","DirectTlsHttpURLConnection.java"):
    shutil.copyfile("tools/"+name,src/name)
p=src/"NetClient.java"
s=p.read_text(encoding="utf-8")
old='''        HttpURLConnection c=connect(url,method);
        if(ipFallback){
            if(!(c instanceof HttpsURLConnection))throw new java.io.IOException("Only verified TLS is allowed");
            HttpsURLConnection https=(HttpsURLConnection)c;
            https.setSSLSocketFactory(new FallbackTlsSocketFactory(
                (SSLSocketFactory)SSLSocketFactory.getDefault()));
            Log.i("PocketPetNet","DNS-independent TLS route with original HTTPS hostname");
        }
        return c;'''
new='''        if(ipFallback){
            // The old socket factory did not help: HttpsURLConnection resolved the
            // hostname before invoking the factory. This independent implementation
            // connects to a literal IP and validates the expected HTTPS hostname.
            HttpURLConnection direct=new DirectTlsHttpURLConnection(new URL(url));
            direct.setConnectTimeout(12000);
            direct.setReadTimeout(18000);
            direct.setRequestMethod(method);
            direct.setInstanceFollowRedirects(false);
            Log.i("PocketPetNet","Raw TCP + certified TLS without system DNS for MYMP");
            return direct;
        }
        return connect(url,method);'''
assert old in s, "v8.5.3 connection method changed"
s=s.replace(old,new)
p.write_text(s,encoding="utf-8")
m=src/"MainActivity.java"
s=m.read_text(encoding="utf-8")
assert ' / 8.5.2' in s
s=s.replace(' / 8.5.2',' / 8.5.4')
m.write_text(s,encoding="utf-8")
g=Path("app/build.gradle")
s=g.read_text(encoding="utf-8")
assert "versionCode 14" in s and "versionName '8.5.3'" in s
g.write_text(s.replace("versionName '8.5.3'","versionName '8.5.4'"),encoding="utf-8")
print("v8.5.4: real DNS-independent HTTPS fallback, unchanged versionCode 14 and saves")
