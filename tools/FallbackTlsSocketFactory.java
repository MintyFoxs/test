package ru.pgws.pocketpet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Only used after Android reports DNS failure for MYMP's official hostnames.
 * Connect TCP to the known VPS address, then wrap it in normal platform TLS
 * with the ORIGINAL hostname for SNI and HTTPS hostname verification.
 * This does not bypass certificate validation.
 */
final class FallbackTlsSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory delegate;
    private static final byte[] IPV4 = {(byte)157,22,(byte)241,82};
    FallbackTlsSocketFactory(SSLSocketFactory delegate) { this.delegate=delegate; }
    @Override public String[] getDefaultCipherSuites(){return delegate.getDefaultCipherSuites();}
    @Override public String[] getSupportedCipherSuites(){return delegate.getSupportedCipherSuites();}
    private Socket verifiedTunnel(String host,int port,InetAddress local,int localPort)throws IOException{
        if (!"stat.mymp.fun".equalsIgnoreCase(host) && !"mymp.fun".equalsIgnoreCase(host))
            throw new IOException("Unexpected update host: "+host);
        if (port!=443)throw new IOException("Unexpected HTTPS port");
        Socket tcp=new Socket();
        try {
            if(local!=null)tcp.bind(new InetSocketAddress(local,localPort));
            tcp.connect(new InetSocketAddress(InetAddress.getByAddress(IPV4),port),12000);
            // delegate is Android's default TLS implementation, with its normal trust store.
            return delegate.createSocket(tcp,host,port,true);
        }catch(IOException | RuntimeException problem) {
            try{tcp.close();}catch(IOException ignored){}
            throw problem;
        }
    }
    @Override public Socket createSocket(String host,int port)throws IOException{
        return verifiedTunnel(host,port,null,0);
    }
    @Override public Socket createSocket(String host,int port,InetAddress local,int localPort)throws IOException{
        return verifiedTunnel(host,port,local,localPort);
    }
    @Override public Socket createSocket(InetAddress host,int port)throws IOException{
        return delegate.createSocket(host,port);
    }
    @Override public Socket createSocket(InetAddress host,int port,InetAddress local,int localPort)throws IOException{
        return delegate.createSocket(host,port,local,localPort);
    }
    @Override public Socket createSocket(Socket plain,String host,int port,boolean autoClose)throws IOException{
        if(plain!=null && autoClose)try{plain.close();}catch(IOException ignored){}
        return verifiedTunnel(host,port,null,0);
    }
}
