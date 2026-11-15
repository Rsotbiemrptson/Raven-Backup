package dev.raven.backup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Removes only Raven's unchanged shared launcher for a demonstrably finished job. */
final class LauncherScripts {
 private static final String NAME="raven-backup-job-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.sh";
 private LauncherScripts(){}
 static void sweep(File shared,File privateFiles){
  try(DirectoryStream<Path> files=Files.newDirectoryStream(shared.toPath(),"raven-backup-job-*.sh")){
   for(Path file:files)remove(file.toFile(),privateFiles);
  }catch(IOException|DirectoryIteratorException|SecurityException ignored){/* Optional storage permission may be absent. */}
 }
 static boolean remove(File wrapper,File privateFiles){
  try{
   String name=wrapper.getName();if(!name.matches(NAME))return false;
   File job=new File(privateFiles,name.substring("raven-backup-".length(),name.length()-3));
   Path folder=job.toPath(),done=folder.resolve("done"),path=wrapper.toPath();
   if(!Files.isDirectory(folder,LinkOption.NOFOLLOW_LINKS)||!regular(done)||!regular(path))return false;
   String status=small(done,4).trim();
   if(!status.matches("[0-9]{1,3}")||Integer.parseInt(status)>255)return false;
   String script=new File(job,"job.sh").getPath();
   String expected="sh '"+script.replace("'","'\\''")+"'\n";
   if(!small(path,4096).equals(expected))return false;
   Files.delete(path);return true;
  }catch(IOException|SecurityException ignored){return false;}
 }
 private static boolean regular(Path path){return Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS);}
 private static String small(Path path,int limit)throws IOException{
  try(InputStream in=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS);ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] bytes=new byte[256];int n;
   while((n=in.read(bytes))!=-1){if(out.size()+n>limit)throw new IOException("Not a launcher or completion marker");out.write(bytes,0,n);}
   return new String(out.toByteArray(),StandardCharsets.UTF_8);
  }
 }
}
