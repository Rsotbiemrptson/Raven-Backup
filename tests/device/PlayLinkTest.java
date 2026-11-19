package dev.raven.backup;
import android.app.Instrumentation;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import java.io.*;
import java.util.*;
import org.json.*;

/** Checks Android store attribution using only the bundled disposable helper. */
public final class PlayLinkTest extends Instrumentation {
 void require(boolean value,String why){if(!value)throw new AssertionError(why);}
 public void onCreate(Bundle args){super.onCreate(args);start();}
 public void onStart(){Bundle report=new Bundle();try{
  Context c=getTargetContext();require(!OperationService.running(),"Backup/restore running");
  LiveRestore live=new LiveRestore(c);require(!live.pointer.exists(),"Pending restore");live.testing=true;live.keepRollback=false;
  BackupEngine e=live.engine;require(!SetupCheck.installed(e),"Disposable helper already installed");e.pm.getPackageInfo("com.android.vending",0);
  File own=e.job(),apk=new File(own,"helper.apk"),dataZip=new File(own,"data-only.zip"),fullZip=new File(own,"app-and-data.zip");boolean installed=false;
  try{
   try(InputStream in=c.getAssets().open("restore-check.apk");OutputStream out=new FileOutputStream(apk)){BackupEngine.copy(in,out,16*1024*1024);}
   e.run(e.job(),"pm install --user 0 -i com.android.vending "+BackupEngine.q(apk.getPath())+"\n",false);installed=true;SetupCheck.mode(e,"seed");
   File snapshot=e.job();JSONObject manifest=e.archive(Collections.singletonList(SetupCheck.PACKAGE),snapshot,false);
   JSONObject app=manifest.getJSONArray("apps").getJSONObject(0);
   require(app.getString("installer").equals("com.android.vending")&&app.has("installerAppSigners"),"Data-only backup did not capture store identity");
   require(!app.has("apks"),"Data-only archive unexpectedly has installers");e.zip(snapshot,manifest,new FileOutputStream(dataZip));
   ApkBackup.capture(e,snapshot,manifest);e.zip(snapshot,manifest,new FileOutputStream(fullZip));e.clean(snapshot);
   SetupCheck.mode(e,"mutate");ApkBackup.setInstaller(e,SetupCheck.PACKAGE,c.getPackageName());
   String restored=live.restore(live.prepare(Uri.fromFile(dataZip)),false,false);live.verifyLaunch(Collections.singletonList(SetupCheck.PACKAGE),false);
   require(e.pm.getInstallSourceInfo(SetupCheck.PACKAGE).getInstallingPackageName().equals("com.android.vending"),"Data-only restore lost Play attribution");require(restored.contains("Google Play installer link verified"),"Store result missing from restore report");report.putString("data_only","PASS captured Play installer and signing identities without APKs; restored data independently read; installer attribution restored");
   e.run(e.job(),"pm uninstall "+BackupEngine.q(SetupCheck.PACKAGE)+"\n",false);installed=false;
   LiveRestore.Plan plan=live.importArchive(Uri.fromFile(fullZip));ApkBackup.installMissing(e,plan.job,plan.manifest,Collections.singleton(SetupCheck.PACKAGE));installed=true;
   require(e.pm.getInstallSourceInfo(SetupCheck.PACKAGE).getInstallingPackageName().equals("com.android.vending"),"APK reinstall lost store identity");live.prepare(plan,null);live.restore(plan,false,false);live.verifyLaunch(Collections.singletonList(SetupCheck.PACKAGE),false);report.putString("reinstall","PASS saved APK installed with verified original store; data restored and independently read");
   ApkBackup.setInstaller(e,SetupCheck.PACKAGE,c.getPackageName());
   JSONObject changed=new JSONObject(manifest.toString());changed.getJSONArray("apps").getJSONObject(0).put("installerAppSigners",new JSONArray().put(String.join("",Collections.nCopies(64,"0"))));
   require(ApkBackup.restoreInstallerLinks(e,changed).contains("differently signed"),"Changed app identity was not guarded");
   require(e.pm.getInstallSourceInfo(SetupCheck.PACKAGE).getInstallingPackageName().equals(c.getPackageName()),"Changed identity attribution overwritten");
   changed=new JSONObject(manifest.toString());changed.getJSONArray("apps").getJSONObject(0).put("installerSigners",new JSONArray().put(String.join("",Collections.nCopies(64,"0"))));require(ApkBackup.restoreInstallerLinks(e,changed).contains("signing identity differs"),"Different store signer was accepted");
   JSONObject old=new JSONObject(manifest.toString());old.getJSONArray("apps").getJSONObject(0).remove("installer");require(ApkBackup.restoreInstallerLinks(e,old).contains("not recorded"),"Legacy attribution inferred without evidence");
   report.putString("guards","PASS changed app signer, changed store signer, and missing legacy metadata do not assign Play attribution");
  }finally{
   if(installed&&!live.pointer.exists())e.run(e.job(),"pm uninstall "+BackupEngine.q(SetupCheck.PACKAGE)+"\n",false);
   if(!live.pointer.exists()){dataZip.delete();fullZip.delete();e.clean(own);}
  }
  report.putString("limit","Synthetic helper proves Android installer attribution only. It has no Play listing or owned entitlement, so actual Play update delivery is not asserted.");report.putString("result","PASS");finish(-1,report);
 }catch(Throwable failure){report.putString("error",android.util.Log.getStackTraceString(failure));finish(0,report);}}
}
