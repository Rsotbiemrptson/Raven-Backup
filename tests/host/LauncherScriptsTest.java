package dev.raven.backup;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

public final class LauncherScriptsTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static void write(Path file,String value)throws IOException{Files.write(file,value.getBytes(StandardCharsets.UTF_8));}
 static Path wrapper(Path shared,Path job)throws IOException{
  Path script=shared.resolve("raven-backup-"+job.getFileName()+".sh");
  write(script,"sh '"+job.resolve("job.sh").toString().replace("'","'\\''")+"'\n");return script;
 }
 public static void main(String[] args)throws Exception{
  Path root=Files.createTempDirectory(Paths.get("work"),"launchers-"),shared=Files.createDirectory(root.resolve("shared")),privateFiles=Files.createDirectory(root.resolve("private"));
  Path job=Files.createDirectory(privateFiles.resolve("job-"+UUID.randomUUID())),done=job.resolve("done"),script=wrapper(shared,job);
  write(job.resolve("job.sh"),"private job retained");
  LauncherScripts.sweep(shared.toFile(),privateFiles.toFile());check(Files.exists(script),"Running job removed");
  for(String bad:new String[]{"","running","256","-1","0 garbage","000000000000"}){
   write(done,bad);check(!LauncherScripts.remove(script.toFile(),privateFiles.toFile()),"Invalid completion accepted");
  }
  write(done,"0");write(script,"sh '/another/job.sh'\n");
  check(!LauncherScripts.remove(script.toFile(),privateFiles.toFile()),"Foreign script removed");
  script=wrapper(shared,job);check(LauncherScripts.remove(script.toFile(),privateFiles.toFile()),"Success wrapper retained");
  check(Files.exists(done)&&Files.exists(job.resolve("job.sh")),"Private job touched");
  script=wrapper(shared,job);write(done,"130");LauncherScripts.sweep(shared.toFile(),privateFiles.toFile());check(!Files.exists(script),"Failed completed wrapper retained");
  Path orphan=shared.resolve("raven-backup-job-"+UUID.randomUUID()+".sh");write(orphan,"sh '/missing/job.sh'\n");
  Path unrelated=shared.resolve("raven-backup-user.sh");write(unrelated,"user script");
  LauncherScripts.sweep(shared.toFile(),privateFiles.toFile());check(Files.exists(orphan)&&Files.exists(unrelated),"Unrecognized/orphan script removed");
  script=wrapper(shared,job);
  try{
   Path target=shared.resolve("user-file");write(target,"keep me");Files.delete(script);Files.createSymbolicLink(script,target.toAbsolutePath());
   check(!LauncherScripts.remove(script.toFile(),privateFiles.toFile()),"Symlink wrapper followed");check(Files.exists(target),"Symlink target removed");
  }catch(UnsupportedOperationException|FileSystemException unavailable){System.out.println("Host cannot create symlinks; NOFOLLOW checks remain enabled");}
  System.out.println("PASS completed success/failure cleanup, active/malformed/modified/orphan preservation, private artifacts retained");
 }
}
