package dev.raven.backup;
import android.app.Instrumentation;
import android.content.Context;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Owner-invoked launcher lifecycle check; no personal apps/data or preferences changed. */
public final class LauncherCleanupTest extends Instrumentation {
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
 public void onStart(){Bundle result=new Bundle();try{
  Context c=getTargetContext();require(!OperationService.running(),"Operation active");
  require(!new LiveRestore(c).pointer.exists(),"Pending restore; no tests performed");
  BackupEngine e=new BackupEngine(c);require(!SetupCheck.installed(e),"Disposable helper already installed");
  require(Environment.isExternalStorageManager(),"Internal storage access unavailable");
  File shared=Environment.getExternalStorageDirectory(),own=e.job(),apk=new File(own,"fixture.apk");
  Uri folder=null;boolean installed=false;
  try{
   // Verify NOFOLLOW behavior on the device's native filesystem, without touching shared files.
   Path sandbox=Files.createDirectory(new File(own,"sandbox").toPath());
   Path target=Files.createDirectory(sandbox.resolve("private"));
   File job=new File(target.toFile(),"job-"+UUID.randomUUID());require(job.mkdir(),"Sandbox job");
   File marker=new File(job,"done"),wrapper=new File(sandbox.toFile(),"raven-backup-"+job.getName()+".sh");
   String command="sh "+BackupEngine.q(new File(job,"job.sh").getPath())+"\n";
   BackupEngine.write(wrapper,command);LauncherScripts.sweep(sandbox.toFile(),target.toFile());
   require(wrapper.exists(),"Unfinished wrapper removed");BackupEngine.write(marker,"0");
   BackupEngine.write(wrapper,"modified script\n");require(!LauncherScripts.remove(wrapper,target.toFile()),"Modified wrapper removed");
   require(wrapper.delete(),"Remove own modified sandbox script");
   Path linkTarget=sandbox.resolve("keep-file");Files.write(linkTarget,new byte[]{1,2,3});
   Files.createSymbolicLink(wrapper.toPath(),linkTarget);require(!LauncherScripts.remove(wrapper,target.toFile()),"Symlink removed");
   require(Files.size(linkTarget)==3,"Symlink target changed");Files.delete(wrapper.toPath());
   BackupEngine.write(wrapper,command);require(LauncherScripts.remove(wrapper,target.toFile()),"Completed sandbox wrapper retained");
   result.putString("guards","PASS unfinished and modified scripts retained; symlink not followed; completed marker readable with NOFOLLOW");

   try(InputStream in=c.getAssets().open("restore-check.apk");OutputStream out=new FileOutputStream(apk)){BackupEngine.copy(in,out,16*1024*1024);}
   e.run(e.job(),"pm install --user 0 "+BackupEngine.q(apk.getPath())+"\n",false);installed=true;SetupCheck.mode(e,"seed");
   Uri tree=SetupActivity.folder(c);require(tree!=null,"No chosen backup folder");
   folder=DocumentsContract.createDocument(c.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)),DocumentsContract.Document.MIME_TYPE_DIR,"Raven-launcher-disposable-"+UUID.randomUUID());
   require(folder!=null,"Cannot create disposable backup folder");
   Set<String> initial=new HashSet<>();File[] old=shared.listFiles();require(old!=null,"Cannot list shared storage");for(File f:old)initial.add(f.getName());
   Set<String> observed=Collections.synchronizedSet(new HashSet<>());AtomicBoolean watching=new AtomicBoolean(true);
   Thread watcher=new Thread(()->{while(watching.get()){File[] files=shared.listFiles();if(files!=null)for(File file:files)if(file.getName().startsWith("raven-backup-job-")&&file.getName().endsWith(".sh")&&!initial.contains(file.getName()))observed.add(file.getName());try{Thread.sleep(5);}catch(InterruptedException stop){return;}}});
   e.backupApks=true;e.backupExternal=false;watcher.start();String backup;
   try{backup=e.backupOne(Collections.singletonList(SetupCheck.PACKAGE),tree,folder);}finally{watching.set(false);watcher.join(5000);}
   require(!observed.isEmpty(),"No new launcher observed during backup");
   for(String name:observed){require(!new File(shared,name).exists(),"New launcher retained after backup");File completed=new File(c.getFilesDir(),name.substring("raven-backup-".length(),name.length()-3));require(BackupEngine.read(new File(completed,"done"),4).equals("0"),"Backup job did not complete successfully");}
   require(e.lastBackupUri!=null,"No completed ZIP");File unpack=e.job();
   try(InputStream in=c.getContentResolver().openInputStream(e.lastBackupUri)){JSONObject manifest=e.unpack(in,unpack);require(manifest.getJSONArray("apps").length()==1&&manifest.getJSONArray("apps").getJSONObject(0).getString("package").equals(SetupCheck.PACKAGE),"Wrong backup contents");}finally{e.clean(unpack);}
   result.putString("backup",backup+"; ZIP reopened and validated; observed and removed "+observed.size()+" new launcher(s)");
   File failed=e.job();boolean rejected=false;
   try{e.run(failed,"echo 'Disposable expected failure'\nexit 23\n",true);}catch(IOException expected){require(expected.getMessage().contains("Root job failed (23)"),"Unexpected failure: "+expected);rejected=true;}
   require(rejected,"Expected failure accepted");require(BackupEngine.read(new File(failed,"done"),4).equals("23"),"Failure marker missing");
   require(!new File(shared,"raven-backup-"+failed.getName()+".sh").exists(),"Failed completed job launcher retained");
   result.putString("failed_job","PASS exit 23 reported and launcher removed");
  }finally{
   if(installed)e.run(e.job(),"pm uninstall "+BackupEngine.q(SetupCheck.PACKAGE)+"\n",false);
   if(folder!=null)require(DocumentsContract.deleteDocument(c.getContentResolver(),folder),"Disposable backup folder retained");
   e.clean(own);
  }
  result.putString("result","PASS");finish(-1,result);
 }catch(Throwable error){result.putString("error",android.util.Log.getStackTraceString(error));finish(0,result);}}
}
