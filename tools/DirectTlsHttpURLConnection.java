package ru.pgws.pocketpet;

import java.io.*;
import java.net.*;
import javax.net.ssl.*;

/** Emergency HTTPS implementation using a literal VPS IP and independently verified TLS hostname. */
public final class DirectTlsHttpURLConnection extends HttpURLConnection {
    private static final byte[] FIXED_IPV4={(byte)157,22,(byte)241,82};
    private final ByteArrayOutputStream requestBody=new ByteArrayOutputStream();
    private DirectVerifiedHttps.Response reply;
    public DirectTlsHttpURLConnection(URL url)throws IOException{
        super(url);
        String host=url.getHost();
        if(!"https".equalsIgnoreCase(url.getProtocol())||url.getPort()!=-1||
            !("stat.mymp.fun".equalsIgnoreCase(host)||"mymp.fun".equalsIgnoreCase(host)))
            throw new IOException("Untrusted MYMP HTTPS endpoint");
    }
    @Override public void connect()throws IOException{
        if(connected)return;
        String host=url.getHost();
        Socket raw=new Socket();
        try{
            raw.connect(new InetSocketAddress(InetAddress.getByAddress(FIXED_IPV4),443),12000);
            raw.setSoTimeout(25000);
            SSLSocketFactory factory=(SSLSocketFactory)SSLSocketFactory.getDefault();
            try(SSLSocket tls=(SSLSocket)factory.createSocket(raw,host,443,true)){
                tls.setSoTimeout(25000);
                tls.startHandshake(); // ordinary OS trust manager checks the certificate chain
                if(!HttpsURLConnection.getDefaultHostnameVerifier().verify(host,tls.getSession()))
                    throw new SSLHandshakeException("Certificate hostname mismatch for "+host);
                byte[] body=requestBody.toByteArray();
                String path=url.getFile();if(path==null||path.isEmpty())path="/";
                if(path.contains("\r")||path.contains("\n"))throw new IOException("Invalid URL path");
                String head=method+" "+path+" HTTP/1.1\r\n"+
                    "Host: "+host+"\r\n"+
                    "Connection: close\r\nAccept: application/json\r\nAccept-Encoding: identity\r\n"+
                    ("POST".equals(method)?"Content-Type: application/json; charset=UTF-8\r\nContent-Length: "+body.length+"\r\n":"")+"\r\n";
                OutputStream wire=tls.getOutputStream();
                wire.write(head.getBytes("US-ASCII"));
                if(body.length!=0)wire.write(body);
                wire.flush();
                reply=DirectVerifiedHttps.readResponse(tls.getInputStream(),12*1024*1024);
                responseCode=reply.status;
                connected=true;
            }
        }finally{try{raw.close();}catch(IOException ignored){}}
    }
    @Override public OutputStream getOutputStream()throws IOException{
        if(connected)throw new IOException("Request already submitted");
        return requestBody;
    }
    @Override public InputStream getInputStream()throws IOException{
        connect();
        if(reply.status>=400)throw new IOException("HTTPS server returned HTTP "+reply.status);
        return new ByteArrayInputStream(reply.body);
    }
    @Override public InputStream getErrorStream(){
        try{connect();}catch(IOException ignored){return null;}
        return reply.status>=400?new ByteArrayInputStream(reply.body):null;
    }
    @Override public int getResponseCode()throws IOException{connect();return responseCode;}
    @Override public void disconnect(){connected=false;}
    @Override public boolean usingProxy(){return false;}
}
