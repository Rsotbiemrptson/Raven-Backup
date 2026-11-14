package dev.raven.backup;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Bounded GNU names/targets; app-scoped external-file symlinks require an explicit package context. */
final class TarGuard {
 static final class Record {
  String path,sha256,linkTarget;long mode,uid,gid,size;boolean directory,symlink,fifo;
 }
 static List<Record> records(File file)throws Exception{return parse(file,true,null);}
 static List<Record> records(File file,String pkg)throws IOException{return parse(file,true,pkg);}
 static String longName(byte[] bytes)throws IOException{return path(gnuString(bytes));}
 static String gnuString(byte[] bytes)throws IOException{
  if(bytes.length<2||bytes[bytes.length-1]!=0)throw new IOException("Invalid GNU long filename terminator");
  for(int i=0;i<bytes.length-1;i++)if(bytes[i]==0)throw new IOException("Embedded NUL in GNU long filename");
  try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes,0,bytes.length-1)).toString();}
  catch(java.nio.charset.CharacterCodingException e){throw new IOException("Invalid UTF-8 GNU long filename",e);}
 }
 static String externalRelative(String target,String pkg)throws IOException{
  if(pkg==null||!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))throw new IOException("Symbolic links require an app identity");
  String prefix="/storage/emulated/0/Android/data/"+pkg+"/files/";
  if(!target.startsWith(prefix)||target.indexOf('\n')>=0||target.indexOf('\r')>=0)throw new IOException("Symbolic link target is outside this app's external files");String relative=target.substring(prefix.length());if(relative.isEmpty()||!path(relative).equals(relative))throw new IOException("Unsafe symbolic link target");return relative;
 }
 static List<Record> parse(File file,boolean hashes)throws IOException{return parse(file,hashes,null);}
 static List<Record> parse(File file,boolean hashes,String pkg)throws IOException{
  if(pkg!=null&&!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))throw new IOException("Invalid app identity for archive");
  List<Record> result=new ArrayList<>();Set<String> paths=new HashSet<>();Map<String,Integer> types=new HashMap<>();long total=0;int headers=0;
  try(RandomAccessFile in=new RandomAccessFile(file,"r")){
   byte[] b=new byte[512];int zeros=0;String pending=null,pendingLink=null;
   while(in.getFilePointer()<in.length()){
    if(in.read(b)!=512)throw new IOException("Truncated tar header");boolean zero=true;for(byte v:b)if(v!=0){zero=false;break;}
    if(zero){if(pending!=null||pendingLink!=null)throw new IOException("Dangling GNU long filename/target");zeros++;if(zeros==2){while(in.getFilePointer()<in.length()){if(in.read(b)!=512)throw new IOException("Truncated tar padding");for(byte v:b)if(v!=0)throw new IOException("Nonzero tar trailer");}break;}continue;}
    if(zeros!=0)throw new IOException("Invalid tar end marker");if(++headers>200000)throw new IOException("Excessive tar headers");
    long sum=0;for(int i=0;i<512;i++)sum+=i>=148&&i<156?32:b[i]&255;if(sum!=oct(b,148,8))throw new IOException("Tar checksum mismatch");
    if(!text(b,257,6).trim().equals("ustar"))throw new IOException("Only ustar/GNU long-name archives supported");
    String prefix=text(b,345,155),name=text(b,0,100);String fallback=path(prefix.isEmpty()?name:prefix+"/"+name);int type=b[156];long len=oct(b,124,12),mode=oct(b,100,8),uid=oct(b,108,8),gid=oct(b,116,8);
    if(len>16L*1024*1024*1024)throw new IOException("Tar too large");long padded=(len+511)/512*512;if(padded>in.length()-in.getFilePointer())throw new IOException("Truncated tar data");long next=in.getFilePointer()+padded;
    if(type=='L'||type=='K'){
     if((type=='L'?pending!=null:pendingLink!=null)||!fallback.equals("@LongLink")||len<2||len>4096)throw new IOException("Invalid GNU long filename/target header");
     if(type=='K'&&pkg==null)throw new IOException("Archive rejects links without app context");byte[] bytes=new byte[(int)len];in.readFully(bytes);if(type=='L')pending=longName(bytes);else pendingLink=gnuString(bytes);in.seek(next);continue;
    }
    boolean symlink=type=='2'&&pkg!=null,fifo=type=='6'&&pkg!=null;if(type!=0&&type!='0'&&type!='5'&&!symlink&&!fifo)throw new IOException("Archive rejects links, special files and unsupported extended tar headers");
    String path=pending==null?fallback:pending;pending=null;String target=null;
    if(symlink){target=pendingLink==null?text(b,157,100):pendingLink;externalRelative(target,pkg);if(len!=0||path.isEmpty())throw new IOException("Invalid symbolic link payload/path");}else if(pendingLink!=null)throw new IOException("GNU link target applied to non-link");pendingLink=null;
    if(fifo&&(len!=0||path.isEmpty()||oct(b,329,8)!=0||oct(b,337,8)!=0))throw new IOException("Invalid FIFO metadata");
    if(type=='5'&&len!=0)throw new IOException("Directory payload");
    if(!paths.add(path)||paths.size()>100000)throw new IOException("Duplicate/excessive tar paths");types.put(path,type);total+=len;if(total>16L*1024*1024*1024)throw new IOException("Tar too large");
    Record r=new Record();r.path=path;r.mode=mode;r.uid=uid;r.gid=gid;r.size=len;r.directory=type=='5';r.symlink=symlink;r.fifo=fifo;r.linkTarget=target;
    if(hashes&&!r.directory&&!r.symlink&&!r.fifo){
     try{java.security.MessageDigest md=java.security.MessageDigest.getInstance("SHA-256");byte[] bytes=new byte[65536];long remaining=len;while(remaining>0){int n=in.read(bytes,0,(int)Math.min(bytes.length,remaining));if(n<0)throw new EOFException();md.update(bytes,0,n);remaining-=n;}StringBuilder digest=new StringBuilder();for(byte v:md.digest())digest.append(String.format(Locale.ROOT,"%02x",v&255));r.sha256=digest.toString();}
     catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    in.seek(next);result.add(r);
   }
   if(pending!=null||pendingLink!=null)throw new IOException("Dangling GNU long filename/target");if(zeros<2)throw new IOException("Missing tar end markers");
  }
  for(String p:paths){String parent=p;while(parent.contains("/")){parent=parent.substring(0,parent.lastIndexOf('/'));Integer type=types.get(parent);if(type!=null&&type!='5')throw new IOException("File used as parent directory");}}
  return result;
 }
 static String text(byte[] b,int off,int len){int end=off;while(end<off+len&&b[end]!=0)end++;return new String(b,off,end-off,StandardCharsets.UTF_8);}
 static long oct(byte[] b,int off,int len)throws IOException{String s=text(b,off,len).trim();if(s.isEmpty())return 0;if(!s.matches("[0-7]+"))throw new IOException("Unsupported tar number");try{return Long.parseLong(s,8);}catch(Exception e){throw new IOException("Invalid tar number");}}
 static String path(String s)throws IOException{while(s.startsWith("./"))s=s.substring(2);while(s.endsWith("/"))s=s.substring(0,s.length()-1);if(s.equals("."))s="";if(s.startsWith("/")||s.contains("\\")||s.indexOf('\0')>=0)throw new IOException("Unsafe archive path");if(!s.isEmpty())for(String p:s.split("/",-1))if(p.isEmpty()||p.equals("..")||p.equals("."))throw new IOException("Archive traversal");return s;}
 static Set<String> validate(File file)throws IOException{return validate(file,null);}
 static Set<String> validate(File file,String pkg)throws IOException{Set<String> paths=new HashSet<>();for(Record r:parse(file,false,pkg))paths.add(r.path);return paths;}
 static void remapOwners(File tar,int oldUid,int newUid)throws Exception{remapOwners(tar,oldUid,newUid,null);}
 static void remapOwners(File tar,int oldUid,int newUid,String pkg)throws Exception {
  TarGuard.validate(tar,pkg);
  try(RandomAccessFile file=new RandomAccessFile(tar,"rw")){
   byte[] header=new byte[512];while(file.getFilePointer()<file.length()){
    long start=file.getFilePointer();file.readFully(header);boolean zero=true;for(byte b:header)if(b!=0){zero=false;break;}if(zero)break;
    if(header[156]=='L'||header[156]=='K'){long metadataSize=TarGuard.oct(header,124,12);file.seek(start+512+((metadataSize+511)/512)*512);continue;}
    if(TarGuard.oct(header,108,8)!=oldUid||TarGuard.oct(header,116,8)!=oldUid)throw new IOException("Unrecognized tar owner during remapping");
    long size=TarGuard.oct(header,124,12);byte[] uid=String.format(Locale.ROOT,"%07o",newUid).getBytes("US-ASCII");
    for(int offset:new int[]{108,116}){Arrays.fill(header,offset,offset+8,(byte)0);System.arraycopy(uid,0,header,offset,uid.length);}
    Arrays.fill(header,265,329,(byte)0);Arrays.fill(header,148,156,(byte)32);int sum=0;for(byte b:header)sum+=b&255;byte[] checksum=String.format(Locale.ROOT,"%06o",sum).getBytes("US-ASCII");System.arraycopy(checksum,0,header,148,checksum.length);header[154]=0;header[155]=32;
    file.seek(start);file.write(header);file.seek(start+512+((size+511)/512)*512);
   }file.getFD().sync();
  }TarGuard.validate(tar,pkg);
 }
}
