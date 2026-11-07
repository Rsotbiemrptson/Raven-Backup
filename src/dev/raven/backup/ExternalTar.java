package dev.raven.backup;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Validates an external tar while reading it, optionally extracting to a new scratch directory. */
final class ExternalTar {
 static final long LIMIT=512L*1024*1024*1024;
 static final class Result {
  final long bytes;
  final String sha256;
  Result(long bytes,String sha256){this.bytes=bytes;this.sha256=sha256;}
  void verify(long expectedBytes,String expectedHash)throws IOException {
   if(bytes!=expectedBytes||!sha256.equals(expectedHash))throw new IOException("External data size or checksum mismatch");
  }
 }
 static void read(InputStream input,long bytes,String sha256,Path stage)throws Exception {
  if(bytes<1024||bytes>LIMIT||bytes%512!=0)throw new IOException("Invalid external archive size");
  scan(input,bytes,stage,null).verify(bytes,sha256);
 }
 static Result scan(InputStream input,OutputStream copy)throws Exception {
  return scan(input,LIMIT,null,copy);
 }
 private static Result scan(InputStream input,long limit,Path stage,OutputStream copy)throws Exception {
  MessageDigest digest=MessageDigest.getInstance("SHA-256");
  InputStream checked=new DigestInputStream(input,digest);
  if(copy!=null)checked=new FilterInputStream(checked){
   @Override public int read()throws IOException{int value=in.read();if(value!=-1)copy.write(value);return value;}
   @Override public int read(byte[] b,int offset,int length)throws IOException{int n=in.read(b,offset,length);if(n>0)copy.write(b,offset,n);return n;}
  };
  DataInputStream in=new DataInputStream(checked);
  Map<String,Boolean> paths=new HashMap<>();
  byte[] header=new byte[512],buffer=new byte[1024*1024];
  long remaining=limit;
  String pending=null;
  int zeros=0;
  while(remaining>0){
   if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("External data operation stopped");
   int first=in.read();
   if(first==-1)break;
   header[0]=(byte)first;
   in.readFully(header,1,511);remaining-=512;
   boolean empty=true;for(byte b:header)if(b!=0){empty=false;break;}
   if(empty){if(pending!=null)throw new IOException("Dangling external filename");zeros++;continue;}
   if(zeros!=0)throw new IOException("Invalid external tar trailer");
   long sum=0;for(int i=0;i<512;i++)sum+=(i>=148&&i<156)?32:header[i]&255;
   if(sum!=TarGuard.oct(header,148,8)||!TarGuard.text(header,257,6).trim().equals("ustar"))throw new IOException("Invalid external tar header");
   String prefix=TarGuard.text(header,345,155),name=TarGuard.text(header,0,100);
   String path=TarGuard.path(prefix.isEmpty()?name:prefix+"/"+name);
   int type=header[156];long size=number(header);
   if(size>remaining||size>LIMIT)throw new IOException("Truncated external tar entry");
   if(type=='L'){
    if(pending!=null||!path.equals("@LongLink")||size<2||size>4096)throw new IOException("Invalid external long filename");
    byte[] longName=new byte[(int)size];in.readFully(longName);pending=TarGuard.longName(longName);
   }else{
    if(pending!=null){path=pending;pending=null;}
    boolean directory=type=='5';
    if(!directory&&type!=0&&type!='0')throw new IOException("External archive contains links or special files");
    if((directory&&size!=0)||(!directory&&path.isEmpty())||paths.put(path,directory)!=null||paths.size()>250000)throw new IOException("Invalid or excessive external paths");
    Path target=stage==null?null:stage.resolve(path).normalize();
    if(target!=null&&!target.startsWith(stage))throw new IOException("External archive path escapes staging");
    if(directory){if(target!=null)Files.createDirectories(target);}
    else if(target==null)transfer(in,null,size,buffer);
    else{
     Files.createDirectories(target.getParent());
     try(OutputStream out=Files.newOutputStream(target,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){transfer(in,out,size,buffer);}
    }
   }
   long padding=(512-size%512)%512;
   if(padding>remaining-size)throw new IOException("Truncated external tar padding");
   transfer(in,null,padding,buffer);remaining-=size+padding;
  }
  if(zeros<2||!Boolean.TRUE.equals(paths.get(""))||in.read()!=-1)throw new IOException("Incomplete external archive");
  for(String path:paths.keySet())for(int slash=path.lastIndexOf('/');slash>=0;slash=path.lastIndexOf('/')){
   path=path.substring(0,slash);if(Boolean.FALSE.equals(paths.get(path)))throw new IOException("External file used as directory");
  }
  return new Result(limit-remaining,BackupEngine.hex(digest.digest()));
 }
 private static long number(byte[] header)throws IOException {
  if((header[124]&128)==0)return TarGuard.oct(header,124,12);
  if(header[124]!=(byte)128)throw new IOException("Invalid external file size");
  long size=0;
  for(int i=125;i<136;i++){if(size>(LIMIT>>>8))throw new IOException("External file too large");size=(size<<8)|(header[i]&255);}
  return size;
 }
 private static void transfer(DataInputStream in,OutputStream out,long size,byte[] buffer)throws IOException {
  while(size>0){
   if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("External data operation stopped");
   int n=(int)Math.min(buffer.length,size);in.readFully(buffer,0,n);if(out!=null)out.write(buffer,0,n);size-=n;
  }
 }
}
