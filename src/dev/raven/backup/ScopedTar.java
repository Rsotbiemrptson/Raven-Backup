package dev.raven.backup;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Canonical external TAR headers used to stream independently named storage scopes. */
final class ScopedTar {
 private ScopedTar(){}
 static void header(OutputStream out,String path,boolean directory,long size)throws IOException{
  path=TarGuard.path(path);byte[] name=(path.isEmpty()?".":path).getBytes(StandardCharsets.UTF_8);
  if(name.length>4095)throw new IOException("Scoped external path exceeds the supported TAR name length");
  if(name.length>100){byte[] longName=new byte[name.length+1];System.arraycopy(name,0,longName,0,name.length);raw(out,"././@LongLink",'L',longName.length);out.write(longName);pad(out,longName.length);raw(out,"long-name",directory?'5':'0',size);}
  else raw(out,path.isEmpty()?".":path,directory?'5':'0',size);
 }
 private static void raw(OutputStream out,String name,int type,long size)throws IOException{
  byte[] header=new byte[512];byte[] text=name.getBytes(StandardCharsets.UTF_8);System.arraycopy(text,0,header,0,Math.min(100,text.length));
  oct(header,100,8,type=='5'?0700:0600);oct(header,108,8,0);oct(header,116,8,0);
  if(size<=077777777777L)oct(header,124,12,size);else{header[124]=(byte)128;long n=size;for(int i=135;i>=125;i--){header[i]=(byte)n;n>>>=8;}if(n!=0)throw new IOException("External size overflow");}
  oct(header,136,12,0);java.util.Arrays.fill(header,148,156,(byte)' ');header[156]=(byte)type;
  byte[] magic="ustar\00000".getBytes(StandardCharsets.US_ASCII);System.arraycopy(magic,0,header,257,magic.length);
  long sum=0;for(byte b:header)sum+=b&255;oct(header,148,8,sum);out.write(header);
 }
 private static void oct(byte[] bytes,int offset,int length,long value){byte[] text=Long.toOctalString(value).getBytes(StandardCharsets.US_ASCII);java.util.Arrays.fill(bytes,offset,offset+length-1,(byte)'0');System.arraycopy(text,0,bytes,offset+length-1-text.length,text.length);}
 static void pad(OutputStream out,long length)throws IOException{int padding=(int)((512-length%512)%512);out.write(new byte[padding]);}
 static void finish(OutputStream out)throws IOException{out.write(new byte[1024]);}
}
