package ru.pgws.pocketpet;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.net.ssl.*;

/** Resolver-independent HTTPS transport; always validates CA AND expected hostname. */
public final class DirectVerifiedHttps {
    public static final class Response {
        public final int status;
        public final byte[] body;
        Response(int status,byte[] body){this.status=status;this.body=body;}
    }
    private DirectVerifiedHttps(){}
    public static Response readResponse(InputStream in,int maxBytes)throws IOException {
        if(maxBytes<=0 || maxBytes>16*1024*1024)throw new IOException("Invalid response limit");
        String start=readLine(in);
        if(!start.startsWith("HTTP/1."))throw new IOException("Bad HTTP response");
        String[] parts=start.split(" ",3);
        if(parts.length<2)throw new IOException("Malformed HTTP status line");
        int status;
        try{status=Integer.parseInt(parts[1]);}catch(NumberFormatException e){throw new IOException("Invalid HTTP status",e);}
        boolean chunked=false;
        long length=-1;
        int headers=0;
        while(true){
            String line=readLine(in);
            headers+=line.length()+2;
            if(headers>32768)throw new IOException("Too many headers");
            if(line.isEmpty())break;
            int sep=line.indexOf(':');if(sep<=0)throw new IOException("Malformed header");
            String name=line.substring(0,sep).trim().toLowerCase(Locale.US);
            String value=line.substring(sep+1).trim();
            if("content-length".equals(name)){
                try{length=Long.parseLong(value);}catch(NumberFormatException e){throw new IOException("Bad length",e);}
            }
            if("transfer-encoding".equals(name))chunked=value.toLowerCase(Locale.US).contains("chunked");
            if("content-encoding".equals(name) && !"identity".equalsIgnoreCase(value))
                throw new IOException("Unexpected compressed HTTP response");
        }
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        if(chunked){
            while(true){
                String raw=readLine(in);
                long n;
                try{n=Long.parseLong(raw.split(";",2)[0].trim(),16);}
                catch(NumberFormatException e){throw new IOException("Malformed chunk size",e);}
                if(n<0 || n>maxBytes-out.size())throw new IOException("Chunk too large");
                if(n==0){
                    int trailers=0;
                    while(true){
                        String t=readLine(in);trailers+=t.length();
                        if(trailers>8192)throw new IOException("Too many trailers");
                        if(t.isEmpty())return new Response(status,out.toByteArray());
                    }
                }
                copyExactly(in,out,n);
                if(in.read()!=13||in.read()!=10)throw new IOException("Invalid chunk separator");
            }
        }
        if(length>=0){
            if(length>maxBytes)throw new IOException("Response too large");
            copyExactly(in,out,length);
        }else{
            byte[] buf=new byte[8192];int n;
            while((n=in.read(buf))!=-1){
                if(n>maxBytes-out.size())throw new IOException("Response too large");
                out.write(buf,0,n);
            }
        }
        return new Response(status,out.toByteArray());
    }
    private static void copyExactly(InputStream in,ByteArrayOutputStream out,long n)throws IOException{
        byte[] buf=new byte[8192];
        while(n>0){
            int got=in.read(buf,0,(int)Math.min(n,buf.length));
            if(got<0)throw new IOException("Truncated HTTP response");
            if(got>0){out.write(buf,0,got);n-=got;}
        }
    }
    private static String readLine(InputStream in)throws IOException{
        ByteArrayOutputStream b=new ByteArrayOutputStream();int c;
        while((c=in.read())!=-1){
            if(c==10){
                byte[] data=b.toByteArray();int size=data.length;
                if(size>0 && data[size-1]==13)size--;
                return new String(data,0,size,StandardCharsets.ISO_8859_1);
            }
            if(b.size()>8192)throw new IOException("HTTP line too long");
            b.write(c);
        }
        throw new IOException("Premature EOF reading response");
    }
}
