package dev.raven.backup;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Strict ustar subset: unsupported metadata and links fail closed. */
final class TarGuard {
 static final class Record {
  String path,sha256;long mode,uid,gid,size;boolean directory;
 }
 static List<Record> records(File file)throws Exception {
  validate(file);List<Record> result=new ArrayList<>();
  try(RandomAccessFile in=new RandomAccessFile(file,"r")){
   byte[] header=new byte[512];
   while(in.read(header)==512){boolean zero=true;for(byte v:header)if(v!=0){zero=false;break;}if(zero)break;
    Record r=new Record();String prefix=text(header,345,155),name=text(header,0,100);
    r.path=path(prefix.isEmpty()?name:prefix+"/"+name);r.mode=oct(header,100,8);r.uid=oct(header,108,8);r.gid=oct(header,116,8);r.size=oct(header,124,12);r.directory=header[156]=='5';
    java.security.MessageDigest md=java.security.MessageDigest.getInstance("SHA-256");long remaining=r.size;byte[] bytes=new byte[65536];
    while(remaining>0){int n=in.read(bytes,0,(int)Math.min(bytes.length,remaining));if(n<0)throw new EOFException();md.update(bytes,0,n);remaining-=n;}
    if(!r.directory){StringBuilder digest=new StringBuilder();for(byte v:md.digest())digest.append(String.format(java.util.Locale.ROOT,"%02x",v&255));r.sha256=digest.toString();}
    in.seek(in.getFilePointer()+((512-r.size%512)%512));result.add(r);
   }
  }return result;
 }
 static String text(byte[] b,int off,int len){int end=off;while(end<off+len&&b[end]!=0)end++;return new String(b,off,end-off,StandardCharsets.UTF_8);}
 static long oct(byte[] b,int off,int len)throws IOException{String s=text(b,off,len).trim();if(s.isEmpty())return 0;if(!s.matches("[0-7]+"))throw new IOException("Unsupported tar number");try{return Long.parseLong(s,8);}catch(Exception e){throw new IOException("Invalid tar number");}}
 static String path(String s)throws IOException{while(s.startsWith("./"))s=s.substring(2);while(s.endsWith("/"))s=s.substring(0,s.length()-1);if(s.equals("."))s="";if(s.startsWith("/")||s.contains("\\")||s.indexOf('\n')>=0||s.indexOf('\r')>=0||s.indexOf('\0')>=0)throw new IOException("Unsafe archive path");if(!s.isEmpty())for(String p:s.split("/",-1))if(p.isEmpty()||p.equals("..")||p.equals("."))throw new IOException("Archive traversal");return s;}
 static Set<String> validate(File file)throws IOException{Set<String> paths=new HashSet<>();Map<String,Integer> types=new HashMap<>();long size=0;try(RandomAccessFile in=new RandomAccessFile(file,"r")){byte[] b=new byte[512];int zeros=0;while(in.getFilePointer()<in.length()){if(in.read(b)!=512)throw new IOException("Truncated tar header");boolean zero=true;for(byte v:b)if(v!=0){zero=false;break;}if(zero){zeros++;if(zeros==2){while(in.getFilePointer()<in.length()){if(in.read(b)!=512)throw new IOException("Truncated tar padding");for(byte v:b)if(v!=0)throw new IOException("Nonzero tar trailer");}break;}continue;}if(zeros!=0)throw new IOException("Invalid tar end marker");long sum=0;for(int i=0;i<512;i++)sum+=i>=148&&i<156?32:b[i]&255;if(sum!=oct(b,148,8))throw new IOException("Tar checksum mismatch");if(!text(b,257,6).trim().equals("ustar"))throw new IOException("Only ustar supported");String prefix=text(b,345,155),name=text(b,0,100);String p=path(prefix.isEmpty()?name:prefix+"/"+name);int type=b[156];if(type!=0&&type!='0'&&type!='5')throw new IOException("Rehearsal rejects links, special files and extended tar headers");long len=oct(b,124,12);oct(b,100,8);oct(b,108,8);oct(b,116,8);if(type=='5'&&len!=0)throw new IOException("Directory payload");if(!paths.add(p)||paths.size()>100000)throw new IOException("Duplicate/excessive tar paths");types.put(p,type);size+=len;if(size>16L*1024*1024*1024)throw new IOException("Tar too large");long skip=(len+511)/512*512;if(skip>in.length()-in.getFilePointer())throw new IOException("Truncated tar data");in.seek(in.getFilePointer()+skip);}if(zeros<2)throw new IOException("Missing tar end markers");}
  for(String p:paths){String parent=p;while(parent.contains("/")){parent=parent.substring(0,parent.lastIndexOf('/'));Integer type=types.get(parent);if(type!=null&&type!='5')throw new IOException("File used as parent directory");}}return paths;}
}
