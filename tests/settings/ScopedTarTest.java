package dev.raven.backup;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ScopedTarTest {
 static byte[] fixture(String name,String value)throws Exception{
  ByteArrayOutputStream out=new ByteArrayOutputStream();ScopedTar.header(out,"",true,0);byte[] bytes=value.getBytes(StandardCharsets.UTF_8);ScopedTar.header(out,name,false,bytes.length);out.write(bytes);ScopedTar.pad(out,bytes.length);ScopedTar.finish(out);return out.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory(Paths.get(args[0]),"scoped-tar-");ByteArrayOutputStream all=new ByteArrayOutputStream();ScopedTar.header(all,"",true,0);
  String name="long_"+String.join("",Collections.nCopies(130,"x"))+".obb";
  for(int i=0;i<4;i++)if(ExternalTar.appendScope(new ByteArrayInputStream(fixture(name,"scope data "+i)),"scope-"+i,all).entries!=2)throw new AssertionError("External entry count differs");ScopedTar.finish(all);byte[] tar=all.toByteArray();String sha=BackupEngine.digest(tar);
  Map<String,Path> routes=new LinkedHashMap<>();for(int i=0;i<4;i++)routes.put("scope-"+i,Files.createDirectory(root.resolve("volume-"+i)));
  ExternalTar.readScopes(new ByteArrayInputStream(tar),tar.length,sha,routes);
  for(int i=0;i<4;i++)if(!new String(Files.readAllBytes(routes.get("scope-"+i).resolve(name)),StandardCharsets.UTF_8).equals("scope data "+i))throw new AssertionError("Scope routing mismatch");
  ByteArrayOutputStream newline=new ByteArrayOutputStream();ScopedTar.header(newline,"",true,0);ExternalTar.appendScope(new ByteArrayInputStream(fixture("line\n.obb","newline")),"scope-0",newline);ScopedTar.finish(newline);ExternalTar.scan(new ByteArrayInputStream(newline.toByteArray()),null);
  ExternalTar.scan(new ByteArrayInputStream(tar),null); // Compatible strict external TAR structure.
  Map<String,Path> missing=new HashMap<>();for(int i=0;i<3;i++)missing.put("scope-"+i,Files.createDirectory(root.resolve("missing-test-"+i)));try{ExternalTar.readScopes(new ByteArrayInputStream(tar),tar.length,sha,missing);throw new AssertionError("Unknown scope accepted");}catch(IOException expected){}
  try{ExternalTar.appendScope(new ByteArrayInputStream(fixture("x","x")),"../outside",new ByteArrayOutputStream());throw new AssertionError("Scope traversal accepted");}catch(IOException expected){}
  try{ExternalTar.readScopes(new ByteArrayInputStream(tar),tar.length,String.join("",Collections.nCopies(64,"0")),null);throw new AssertionError("Wrong checksum accepted");}catch(IOException expected){}
  try{ScopedTar.header(new ByteArrayOutputStream(),String.join("",Collections.nCopies(4096,"x")),false,0);throw new AssertionError("Oversized output path accepted");}catch(IOException expected){}
  System.out.println("PASS streamed four-scope TAR rewriting/routing, GNU long and newline names, strict archive validation, scope traversal/unknown-scope/checksum rejection");
 }
}
