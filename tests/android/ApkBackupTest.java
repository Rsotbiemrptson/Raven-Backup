package dev.raven.backup;
import android.content.Context;import android.net.Uri;import android.os.Environment;import org.json.*;import java.io.*;import java.util.*;
/** Development-only tests restricted to the disposable generic fixture. */
final class ApkBackupTest {
 static String run(Context c,Uri tree,boolean restore)throws Exception {
  synchronized(LiveRestore.class){LiveRestore live=new LiveRestore(c);live.testing=true;BackupEngine e=live.engine;String pkg="dev.raven.backupfixture.generic";
   File saved=new File(c.getFilesDir(),"apk-test-uri.txt");
   if(!restore){
    e.run(e.job(),"am force-stop --user 0 "+BackupEngine.q(pkg)+"\nam start -W -n "+BackupEngine.q(pkg+"/dev.raven.backupfixture.Seed")+" --es mode seed\nsleep 1\n",false);
    e.backupApks=true;e.backup(Arrays.asList(pkg),tree);BackupEngine.write(saved,e.lastBackupUri.toString());LiveRestore.Plan plan=live.importArchive(e.lastBackupUri);JSONObject app=plan.manifest.getJSONArray("apps").getJSONObject(0);
    if(plan.manifest.getInt("format")!=2||app.getJSONArray("apks").length()!=2)throw new IOException("Expected base APK and installed split");
    File base=new File(plan.job,app.getJSONArray("apks").getJSONObject(0).getString("file"));int original;try(RandomAccessFile f=new RandomAccessFile(base,"rw")){original=f.read();f.seek(0);f.write(original^1);}boolean rejected=false;
    try{ApkBackup.validate(e,plan.job,app,0,new HashSet<>());}catch(IOException expected){rejected=true;}finally{try(RandomAccessFile f=new RandomAccessFile(base,"rw")){f.write(original);}}
    if(!rejected)throw new IOException("Accepted damaged APK");JSONObject wrong=new JSONObject(app.toString()).put("package","dev.raven.wrong");rejected=false;try{ApkBackup.validate(e,plan.job,wrong,0,new HashSet<>());}catch(IOException expected){rejected=true;}if(!rejected)throw new IOException("Accepted wrong package");e.clean(plan.job);
    String result="PASS app+data ZIP contains base APK and split; damaged APK and wrong package rejected.";BackupEngine.write(new File(Environment.getExternalStorageDirectory(),"raven-apk-backup-test.txt"),result);return result;
   }
   LiveRestore.Plan plan=live.importArchive(Uri.parse(BackupEngine.read(saved,8192)));Set<String> selected=new TreeSet<>(Arrays.asList(pkg));if(ApkBackup.missing(e,plan.manifest,selected).size()!=1)throw new IOException("Test fixture must be uninstalled first");
   int old=plan.manifest.getJSONArray("apps").getJSONObject(0).getInt("uid");ApkBackup.installMissing(e,plan.job,plan.manifest,selected);android.content.pm.PackageInfo pi=e.pm.getPackageInfo(pkg,0);if(pi.splitNames==null||pi.splitNames.length!=1)throw new IOException("Split missing after reinstall");
   live.prepare(plan,selected);live.restore(plan,false,false);live.verifyLaunch(Arrays.asList(pkg),false);
   String result="PASS missing disposable app reinstalled from saved base APK and split.\nPASS data restored with UID adaptation "+old+" -> "+e.info(pkg).getInt("uid")+"; content/ownership/modes/labels verified and app read original values.";BackupEngine.write(new File(Environment.getExternalStorageDirectory(),"raven-apk-restore-test.txt"),result);return result;
  }
 }
}
