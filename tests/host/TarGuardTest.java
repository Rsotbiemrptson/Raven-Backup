package dev.raven.backup;
import java.io.*;import java.util.*;
public class TarGuardTest {
 static byte[] header(String name,char type,int size){byte[] b=new byte[512];put(b,0,name);put(b,100,"0000640");put(b,108,"0030000");put(b,116,"0030000");put(b,124,String.format("%011o",size));put(b,136,"00000000000");Arrays.fill(b,148,156,(byte)32);b[156]=(byte)type;put(b,257,"ustar");put(b,263,"00");int sum=0;for(byte v:b)sum+=v&255;put(b,148,String.format("%06o",sum));return b;}
 static void put(byte[] b,int off,String s){byte[] x=s.getBytes(java.nio.charset.StandardCharsets.UTF_8);System.arraycopy(x,0,b,off,x.length);}
 static File file(String name,byte[]... headers)throws Exception{File f=new File("work/guard-"+name+".tar");try(OutputStream out=new FileOutputStream(f)){for(byte[] b:headers)out.write(b);out.write(new byte[1024]);}return f;}
 static void bad(String n,byte[]... h)throws Exception{try{TarGuard.validate(file(n,h));throw new AssertionError("Accepted "+n);}catch(IOException expected){System.out.println("PASS reject "+n+": "+expected.getMessage());}}
 static byte[] padded(byte[] data){return Arrays.copyOf(data,(data.length+511)/512*512);}
 static byte[] utf(String s){return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);}
 static byte[] longHeader(byte[] name){return header("././@LongLink",'L',name.length);}
 static void longNames()throws Exception{
  String name="files/"+String.join("",Collections.nCopies(130,"a"));byte[] full=utf(name+"\0");
  File f=file("long-valid",header("./",'5',0),longHeader(full),padded(full),header("./truncated",'0',3),padded(utf("abc")));
  Set<String> paths=TarGuard.validate(f);if(!paths.contains(name)||paths.contains("truncated")||paths.contains("@LongLink"))throw new AssertionError("Wrong effective long path");
  List<TarGuard.Record> records=TarGuard.records(f);TarGuard.Record r=records.get(1);if(!r.path.equals(name)||r.size!=3||r.uid!=12288||!r.sha256.equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))throw new AssertionError("Long record metadata/hash differs");
  System.out.println("PASS GNU long filename path, metadata and content hash");
  byte[] dir=utf(name+"/\0");TarGuard.validate(file("long-dir",longHeader(dir),padded(dir),header("./truncated",'5',0),longHeader(utf(name+"/child\0")),padded(utf(name+"/child\0")),header("child",'0',0)));
  for(String unsafe:new String[]{"/escape","../escape","safe/../../escape","a\\b","a\nb"}){byte[] bytes=utf(unsafe+"\0");bad("long-unsafe",longHeader(bytes),padded(bytes),header("safe",'0',0));}
  bad("long-dangling",longHeader(full),padded(full));bad("long-stacked",longHeader(full),padded(full),longHeader(full),padded(full),header("safe",'0',0));
  byte[] noNul=utf("no-null");bad("long-no-terminator",longHeader(noNul),padded(noNul),header("safe",'0',0));
  byte[] embedded=utf("a\0b\0");bad("long-embedded-nul",longHeader(embedded),padded(embedded),header("safe",'0',0));
  byte[] invalid={(byte)0xc3,(byte)0x28,0};bad("long-invalid-utf8",longHeader(invalid),padded(invalid),header("safe",'0',0));
  byte[] oversized=new byte[4097];Arrays.fill(oversized,(byte)'a');oversized[4096]=0;bad("long-oversized",longHeader(oversized),padded(oversized),header("safe",'0',0));
  bad("long-duplicate",longHeader(full),padded(full),header("safe",'0',0),longHeader(full),padded(full),header("other",'0',0));
  bad("long-file-parent",longHeader(full),padded(full),header("safe",'0',0),longHeader(utf(name+"/child\0")),padded(utf(name+"/child\0")),header("child",'0',0));
  bad("long-symlink",longHeader(full),padded(full),header("safe",'2',0));bad("long-hardlink",longHeader(full),padded(full),header("safe",'1',0));bad("gnu-long-link",header("././@LongLink",'K',0));bad("pax",header("pax",'x',0));
 }
 static void longOwnerRemap()throws Exception{
  byte[] name=utf("files/"+String.join("",Collections.nCopies(140,"b"))+"\0");byte[] metadata=longHeader(name);put(metadata,108,"0000000");put(metadata,116,"0000000");Arrays.fill(metadata,148,156,(byte)32);int sum=0;for(byte v:metadata)sum+=v&255;put(metadata,148,String.format("%06o",sum));
  File f=file("long-remap",metadata,padded(name),header("truncated",'0',3),padded(utf("abc")));TarGuard.remapOwners(f,12288,12500);
  try(RandomAccessFile in=new RandomAccessFile(f,"r")){byte[] after=new byte[512];in.readFully(after);if(!Arrays.equals(after,metadata))throw new AssertionError("Filename metadata was changed");}
  TarGuard.Record record=TarGuard.records(f).get(0);if(record.uid!=12500||record.gid!=12500||!record.path.equals(new String(name,0,name.length-1,"UTF-8"))||!record.sha256.equals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))throw new AssertionError("Owner remap changed long path or content");
  byte[] wrong=header("file",'0',0);put(wrong,108,"0000000");Arrays.fill(wrong,148,156,(byte)32);sum=0;for(byte v:wrong)sum+=v&255;put(wrong,148,String.format("%06o",sum));try{TarGuard.remapOwners(file("wrong-owner",wrong),12288,12500);throw new AssertionError("Accepted wrong regular-file owner");}catch(IOException expected){}
  System.out.println("PASS long-name metadata preserved, actual owners remapped and unexpected file owners rejected");
 }
 public static void main(String[] args)throws Exception{longNames();longOwnerRemap();TarGuard.validate(file("valid",header("./",'5',0),header("./files/",'5',0),header("./files/a",'0',0)));System.out.println("PASS valid archive");bad("absolute",header("/data/user/0/x",'0',0));bad("traversal",header("./../escape",'0',0));bad("symlink",header("./link",'2',0));bad("hardlink",header("./link",'1',0));bad("fifo",header("./pipe",'6',0));bad("extension",header("./long",'L',0));bad("duplicate",header("./a",'0',0),header("./a",'0',0));bad("file-parent",header("./a",'0',0),header("./a/b",'0',0));byte[] corrupt=header("./a",'0',0);corrupt[0]='z';bad("checksum",corrupt);bad("truncated-payload",header("./a",'0',4096));File f=file("end",header("./a",'0',0));try(RandomAccessFile r=new RandomAccessFile(f,"rw")){r.setLength(512);}try{TarGuard.validate(f);throw new AssertionError("Accepted missing end");}catch(IOException expected){System.out.println("PASS reject missing end");}}
}
