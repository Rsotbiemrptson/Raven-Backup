package dev.raven.backup;
import android.app.Instrumentation;import android.content.*;import android.net.Uri;import android.os.*;import android.provider.DocumentsContract;import java.io.*;import java.util.*;import java.util.concurrent.*;import org.json.*;
/** Owner-invoked release checks. Only bundled disposable app and a unique temporary backup folder. */
public class FinalReleaseTest extends Instrumentation {
 void require(boolean v,String m){if(!v)throw new AssertionError(m);}
 void note(String s){android.util.Log.i("RavenReleaseCheck",s);}
 public void onCreate(Bundle b){super.onCreate(b);start();}
 public void onStart(){Bundle result=new Bundle();try{
 Context c=getTargetContext();MainActivity a=(MainActivity)startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
 require(!OperationService.running(),"operation already active");require(!new LiveRestore(c).pointer.exists(),"pending restore");
 note("START_SETUP");result.putString("setup",SetupCheck.run(c));note("SETUP_PASS");
 BackupEngine e=new BackupEngine(c);String pkg=SetupCheck.PACKAGE;File own=e.job(),apk=new File(own,"fixture.apk");Uri folder=null;
 try{
 try(InputStream in=c.getAssets().open("restore-check.apk");OutputStream out=new FileOutputStream(apk)){BackupEngine.copy(in,out,16*1024*1024);}
 e.run(e.job(),"pm install --user 0 "+BackupEngine.q(apk.getPath())+"\n",false);SetupCheck.mode(e,"seed");
 Uri tree=SetupActivity.folder(c);require(tree!=null,"no folder");folder=DocumentsContract.createDocument(c.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)),DocumentsContract.Document.MIME_TYPE_DIR,"Raven-release-disposable-"+UUID.randomUUID());require(folder!=null,"test folder");
 e.backupApks=true;e.backupExternal=true;String ext="/data/media/0/Android/data/"+pkg+"/files";
 e.run(e.job(),"[ ! -e "+BackupEngine.q(ext)+" ] || { echo 'Unexpected existing disposable external files'; exit 1; }\n",false);
 e.backupOne(Collections.singletonList(pkg),tree,folder);File absent=e.job();JSONObject old;try(InputStream in=c.getContentResolver().openInputStream(e.lastBackupUri)){old=e.unpack(in,absent);}require(old.getInt("format")==2,"absent external must keep format 2");e.clean(absent);note("ABSENT_PASS");
 e.run(e.job(),"mkdir -p "+BackupEngine.q(ext)+"\n",false);require(ExternalData.source(e,folder,pkg)==null,"empty directory must be omitted");
 e.run(e.job(),"touch "+BackupEngine.q(ext+"/zero-byte")+"\n",false);require(ExternalData.source(e,folder,pkg)!=null,"zero-byte file must count as content");
 e.runLong(e.job(),"truncate -s 4429185024 "+BackupEngine.q(ext+"/large.bin")+"\ndd if=/dev/urandom of="+BackupEngine.q(ext+"/large.bin")+" bs=1048576 count=8 conv=notrunc\nprintf 'RAVEN_END' | dd of="+BackupEngine.q(ext+"/large.bin")+" bs=1 seek=4429185015 conv=notrunc\n");
 String before=e.runLong(e.job(),"sha256sum "+BackupEngine.q(ext+"/large.bin")+"\n").trim().split("\\s+")[0];
 CountDownLatch done=new CountDownLatch(1);Throwable[] failure=new Throwable[1];Uri target=folder;Uri[] backup=new Uri[1];
 runOnMainSync(()->OperationService.begin(c,"Raven disposable release test",()->{try{note("START_STREAM");e.backupOne(Collections.singletonList(pkg),tree,target);backup[0]=e.lastBackupUri;note("STREAM_PASS");OperationService.result("Disposable streamed backup completed",true);}catch(Throwable x){failure[0]=x;}finally{done.countDown();}},m->{failure[0]=new IOException(m);done.countDown();}));
 require(done.await(20,TimeUnit.MINUTES),"stream timeout");if(failure[0]!=null)throw new RuntimeException(failure[0]);
 SetupCheck.mode(e,"mutate");e.run(e.job(),"printf changed > "+BackupEngine.q(ext+"/large.bin")+"\nprintf extra > "+BackupEngine.q(ext+"/extra")+"\n",false);
 LiveRestore live=new LiveRestore(c);live.testing=true;live.keepRollback=false;LiveRestore.Plan plan=live.prepare(backup[0]);require(plan.manifest.getInt("format")==4,"not streamed format");require(plan.manifest.getJSONObject("external").getLong("bytes")>4294967295L,"not ZIP64-sized");
 note("START_RESTORE");String restored=live.restore(plan,false,false);live.verifyLaunch(Collections.singletonList(pkg),false);
 String after=e.runLong(e.job(),"sha256sum "+BackupEngine.q(ext+"/large.bin")+"\n[ ! -e "+BackupEngine.q(ext+"/extra")+" ]\n[ -f "+BackupEngine.q(ext+"/zero-byte")+" ]\n[ \"$(stat -c %s "+BackupEngine.q(ext+"/large.bin")+")\" = 4429185024 ]\n").trim().split("\\s+")[0];require(before.equals(after),"external SHA mismatch");require(!live.pointer.exists(),"pending restore after success");
 result.putString("streamed_restore",restored+"; 4.125 GiB SHA-256="+after+"; zero-byte retained; added external file removed; independent CE/DE app read passed");note("RESTORE_PASS");
 }finally{
 if(!new LiveRestore(c).pointer.exists()){if(SetupCheck.installed(e))e.run(e.job(),"pm uninstall "+BackupEngine.q(pkg)+"\n",false);if(folder!=null)DocumentsContract.deleteDocument(c.getContentResolver(),folder);e.clean(own);}
 }
 BackupEngine guard=new BackupEngine(c);try{guard.run(guard.job(),SystemAppData.stopped(guard.info("com.odin.gameassistant")),false);throw new AssertionError("running system app passed");}catch(SystemAppData.Running expected){result.putString("system_guard","PASS refused running Game Assistant without stopping it");}
 note("ALL_PASS");result.putString("result","PASS");finish(-1,result);
 }catch(Throwable t){result.putString("error",android.util.Log.getStackTraceString(t));note("FAIL "+t);finish(0,result);}}
}
