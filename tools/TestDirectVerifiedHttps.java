import java.io.*;
import java.nio.charset.StandardCharsets;
import ru.pgws.pocketpet.DirectVerifiedHttps;
class TestDirectVerifiedHttps {
  static DirectVerifiedHttps.Response read(String s,int max) throws Exception {
      return DirectVerifiedHttps.readResponse(new ByteArrayInputStream(s.getBytes(StandardCharsets.ISO_8859_1)),max);
  }
  static void eq(Object a,Object b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
  static void failure(String s,int max) throws Exception{
    try{read(s,max);throw new AssertionError("Expected rejection");}catch(IOException e){}
  }
  public static void main(String[]args)throws Exception{
    DirectVerifiedHttps.Response r=read("HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello",10);
    eq(r.status,200);eq(new String(r.body,"UTF-8"),"hello");
    r=read("HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\n\r\n4\r\nWiki\r\n5\r\npedia\r\n0\r\n\r\n",20);
    eq(new String(r.body,"UTF-8"),"Wikipedia");
    failure("HTTP/1.1 200 OK\r\nContent-Length: 8\r\n\r\nabc",20);
    failure("HTTP/1.1 200 OK\r\nContent-Length: 20\r\n\r\na",10);
    failure("HTTP/1.1 200 OK\r\nContent-Encoding: gzip\r\n\r\nabc",20);
    System.out.println("Resolver-free HTTPS response parser: PASS");
  }
}
