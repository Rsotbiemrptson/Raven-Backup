package dev.raven.backup;
import android.content.*;import android.content.pm.*;import android.net.Uri;import android.os.*;import org.json.*;import java.io.*;import java.util.*;
/** User-invoked verification, restricted to our bundled disposable helper. */
final class SetupCheck {
 static final String PACKAGE="dev.raven.backup.setupfixture";
 static String q(String text){return BackupEngine.q(text);}
 static void mode(BackupEngine engine,String mode)throws Exception{
  String state=LiveRestore.data(PACKAGE,"ce")+"/cache/test-state.txt";
  engine.run(engine.job(),"am force-stop --user 0 "+q(PACKAGE)+"\nrm -f -- "+q(state)+"\nam start -W --user 0 -n "+q(PACKAGE+"/dev.raven.backupfixture.Seed")+" --es mode "+q(mode)+"\nn=0\nwhile [ \"$(cat "+q(state)+" 2>/dev/null)\" != "+q(mode)+" ]; do n=$((n+1)); [ \"$n\" -lt 40 ]; sleep 0.25; done\n",false);
 }
 static boolean installed(BackupEngine engine){try{engine.pm.getPackageInfo(PACKAGE,0);return true;}catch(PackageManager.NameNotFoundException e){return false;}}
 interface Progress{void update(String message);}
 static String run(Context context)throws Exception{return run(context,message->{});}
 static String run(Context context,Progress progress)throws Exception{synchronized(LiveRestore.class){
  LiveRestore live=new LiveRestore(context);BackupEngine engine=live.engine;
  if(live.pointer.exists())throw new IOException("Recover the pending restore before running setup.");
  if(!Environment.isExternalStorageManager())throw new IOException("Use Settings → Allow internal-storage scripts first.");
  if(android.os.Process.myUid()>=100000||!((UserManager)context.getSystemService(Context.USER_SERVICE)).isUserUnlocked())throw new IOException("Unlock the primary device user first.");
  if(installed(engine))throw new IOException("Raven Restore Check is already installed. If a previous setup was interrupted, recover any pending restore, then uninstall Raven Restore Check and retry. No existing app was changed.");
  if(!context.getSharedPreferences("live-restore",0).edit().remove("general-verified").commit())throw new IOException("Could not reset verification before checking.");
  StringBuilder evidence=new StringBuilder(engine.probe()).append("\nFirmware: ").append(Build.FINGERPRINT).append("\nSELinux: ").append(RootBridge.call("getenforce").trim()).append("\n");
  File job=engine.job(),apk=new File(job,"setup.apk"),zip=new File(job,"original.zip");boolean ownInstalled=false;live.testing=true;live.keepRollback=false;live.launchAfter=false;
  try{
   try(InputStream in=context.getAssets().open("restore-check.apk");OutputStream out=new FileOutputStream(apk)){BackupEngine.copy(in,out,16*1024*1024);}
   PackageInfo helper=engine.pm.getPackageArchiveInfo(apk.getPath(),PackageManager.GET_SIGNING_CERTIFICATES),self=engine.pm.getPackageInfo(context.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
   if(helper==null||!PACKAGE.equals(helper.packageName)||helper.signingInfo==null||!Arrays.equals(helper.signingInfo.getApkContentsSigners(),self.signingInfo.getApkContentsSigners()))throw new IOException("Bundled verification app identity/signature differs from Raven Backup. Rebuild both with the same key.");
   engine.run(engine.job(),"pm install --user 0 "+q(apk.getPath())+"\n",false);ownInstalled=true;
   if(!installed(engine))throw new IOException("Verification app did not install.");
   progress.update("Checking test-app backup…");mode(engine,"seed");List<String> apps=Collections.singletonList(PACKAGE);live.verifyLaunch(apps,false);
   File snapshot=engine.job();JSONObject saved=engine.archive(apps,snapshot,false);ApkBackup.capture(engine,snapshot,saved);engine.zip(snapshot,saved,new FileOutputStream(zip));engine.clean(snapshot);
   int originalUid=saved.getJSONArray("apps").getJSONObject(0).getInt("uid");
   mode(engine,"mutate");live.verifyLaunch(apps,true);
   progress.update("Testing recovery from a restore failure…");String rollback=live.restoreInternal(live.prepare(Uri.fromFile(zip)),true);
   if(!rollback.startsWith("Restore failed and rollback verified"))throw new IOException("Controlled rollback check did not pass.");live.verifyLaunch(apps,true);
   evidence.append("PASS controlled restore failure rolled back; app read its pre-restore state.\n");
   progress.update("Checking restored data and file permissions…");live.restore(live.prepare(Uri.fromFile(zip)),false,false);live.verifyLaunch(apps,false);
   evidence.append("PASS restored data, owners, permission modes, SELinux labels, root inodes and exact path set; app launched and read original CE/DE data and preferences.\n");
   progress.update("Checking APK reinstall and fresh app ownership…");engine.run(engine.job(),"pm uninstall "+q(PACKAGE)+"\n",false);if(installed(engine))throw new IOException("Verification app was not removed before reinstall test.");
   LiveRestore.Plan plan=live.importArchive(Uri.fromFile(zip));ApkBackup.installMissing(engine,plan.job,plan.manifest,new TreeSet<>(apps));
   int newUid=engine.info(PACKAGE).getInt("uid");if(originalUid==newUid)throw new IOException("Android reused the verification app UID; fresh-identity verification was not established. Retry setup.");
   live.prepare(plan,new TreeSet<>(apps));live.restore(plan,false,false);live.verifyLaunch(apps,false);
   evidence.append("PASS saved APK reinstalled; fresh UID ").append(originalUid).append(" → ").append(newUid).append(" adapted with security categories; metadata and independent app read verified.\n");
   if(live.pointer.exists())throw new IOException("Verification left a pending recovery journal.");
  }finally{
   live.testing=false;
   if(ownInstalled&&!live.pointer.exists()){
    progress.update("Removing the temporary test app…");if(installed(engine)){engine.run(engine.job(),"pm uninstall "+q(PACKAGE)+"\n",false);if(installed(engine))throw new IOException("Could not remove Raven Restore Check. Live restore remains disabled.");}
    engine.clean(job);
   }
  }
  evidence.append("PASS disposable app removed. Your installed apps and backup folder were not changed.\nLive restore enabled on this firmware. Different-device or firmware backups require migration confirmation.");
  BackupEngine.write(new File(context.getFilesDir(),"setup-check.txt"),evidence.toString());
  if(!context.getSharedPreferences("live-restore",0).edit().putString("general-verified",Build.FINGERPRINT).commit())throw new IOException("Could not save successful setup verification.");
  return evidence.toString();
 }}
}
