package dev.raven.backup;
import android.app.Instrumentation;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import java.io.*;
import java.util.*;
import org.json.*;

/** Owner-invoked four-location streaming and rollback check, restricted to disposable data. */
public final class ScopeStorageTest extends Instrumentation {
 void require(boolean value,String why){if(!value)throw new AssertionError(why);}
 String q(String value){return BackupEngine.q(value);}
 public void onCreate(Bundle args){super.onCreate(args);start();}
 public void onStart(){Bundle result=new Bundle();try{
  Context c=getTargetContext();LiveRestore live=new LiveRestore(c);BackupEngine e=live.engine;
  require(!OperationService.running()&&!live.pointer.exists(),"Operation/pending restore active");require(!SetupCheck.installed(e),"Helper already installed");live.testing=true;live.keepRollback=false;
  SharedPreferences prefs=RestorePreferences.prefs(c);Map<String,?> original=prefs.getAll();File own=e.job(),apk=new File(own,"helper.apk"),legacy=new File(own,"legacy-format4.zip");boolean installed=false;Uri folder=null;
  List<String> targets=new ArrayList<>();List<String> hashes=new ArrayList<>();
  try{
   SharedPreferences.Editor options=prefs.edit();for(String key:ExternalScopes.KEYS)options.putBoolean(key,true);require(options.commit(),"Cannot save temporary test choices");
   try(InputStream in=c.getAssets().open("restore-check.apk");OutputStream out=new FileOutputStream(apk)){BackupEngine.copy(in,out,16*1024*1024);}
   e.run(e.job(),"pm install --user 0 "+q(apk.getPath())+"\n",false);installed=true;SetupCheck.mode(e,"seed");
   Map<String,File> volumes=ExternalScopes.volumes(c);require(volumes.size()==2,"Test needs internal storage and one mounted removable card");
   for(String volume:volumes.keySet())for(String kind:new String[]{"data","obb"}){
    String path=ExternalScopes.destination(volume,kind,SetupCheck.PACKAGE);targets.add(path);
    e.run(e.job(),"[ ! -e "+q(path)+" ]\n[ ! -L "+q(path)+" ]\nmkdir -p "+q(path)+"\nprintf "+q(volume+"-"+kind+"-original")+" > "+q(path+"/marker")+"\ndd if=/dev/urandom of="+q(path+"/payload.bin")+" bs=1048576 count=1\n",false);
    hashes.add(e.run(e.job(),"sha256sum < "+q(path+"/payload.bin")+"\n",false).trim().split("\\s+")[0]);
   }
   // Exercise the unchanged format-4 writer, so the new reader must still restore older streamed ZIPs.
   File old=e.job();JSONObject oldManifest=e.archive(Collections.singletonList(SetupCheck.PACKAGE),old,false);oldManifest.put("format",4);BackupEngine.write(new File(old,"manifest.json"),oldManifest.toString(2));e.zip(old,oldManifest,new FileOutputStream(legacy),targets.get(0));e.clean(old);
   Uri tree=SetupActivity.folder(c);require(tree!=null,"No backup folder");folder=DocumentsContract.createDocument(c.getContentResolver(),DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)),DocumentsContract.Document.MIME_TYPE_DIR,"Raven-four-scope-disposable-"+UUID.randomUUID());require(folder!=null,"No test folder");
   e.backupApks=true;e.backupExternal=true;e.backupOne(Collections.singletonList(SetupCheck.PACKAGE),tree,folder);Uri saved=e.lastBackupUri;BackupCatalog.Entry catalogEntry=new BackupCatalog.Entry();catalogEntry.uri=saved;catalogEntry.name="scoped-test.zip";BackupCatalog.read(c,Collections.singletonList(catalogEntry));require(catalogEntry.problem==null&&catalogEntry.manifest.getInt("format")==5,"Restore catalog failed to recognize scoped backup");
   LiveRestore.Plan plan=live.importArchive(saved,true);require(plan.metadataOnly,"Expected indexed quick review");require(plan.manifest.getInt("format")==5,"Expected format 5");require(plan.manifest.getJSONObject("external").getJSONArray("scopes").length()==4,"Missing external scope");
   File full=e.job();try(InputStream in=c.getContentResolver().openInputStream(saved)){JSONObject checked=e.unpack(in,full);require(checked.getInt("format")==5,"Sequential reader rejected format 5");}finally{e.clean(full);}
   SetupCheck.mode(e,"mutate");for(String path:targets)e.run(e.job(),"printf changed > "+q(path+"/marker")+"\nprintf changed > "+q(path+"/payload.bin")+"\nprintf extra > "+q(path+"/extra")+"\n",false);
   live.prepare(plan,null);String restored=live.restore(plan,false,false);require(restored.startsWith("Restored "),"Restore failed");live.verifyLaunch(Collections.singletonList(SetupCheck.PACKAGE),false);
   for(int i=0;i<targets.size();i++){String path=targets.get(i);String hash=e.run(e.job(),"[ ! -e "+q(path+"/extra")+" ]\nsha256sum < "+q(path+"/payload.bin")+"\n",false).trim().split("\\s+")[0];require(hash.equals(hashes.get(i)),"Restored scope content mismatch: "+path);}
   String reportFile="/data/user/0/"+SetupCheck.PACKAGE+"/cache/scopes-report.json";
   e.run(e.job(),"am force-stop --user 0 "+q(SetupCheck.PACKAGE)+"\nam start -W --user 0 -n "+q(SetupCheck.PACKAGE+"/dev.raven.backupfixture.Seed")+" --es mode verify-scopes\nn=0; while [ ! -f "+q(reportFile)+" ]; do n=$((n+1)); [ \"$n\" -lt 40 ]; sleep .25; done\n",false);
   JSONObject appReport=new JSONObject(e.run(e.job(),"cat "+q(reportFile)+"\n",false));require(appReport.getBoolean("success"),"Restored app could not read all four locations");for(int i=0;i<4;i++)require(hashes.contains(appReport.getJSONArray("entries").getJSONObject(i).getString("sha256")),"Independent app hash differs");
   result.putString("four_scopes","PASS streamed all four locations into one ZIP; indexed and sequential readers accepted it; primary/card data and whole OBB directories restored with matching SHA-256; extra files removed; private app reads passed");
   // Exercise recovery after two external swaps have happened. Target only this disposable package.
   LiveRestore.Plan rollbackPlan=live.importArchive(saved);JSONObject transaction=ExternalData.transaction(e,rollbackPlan);File control=e.job();ExternalData.stage(e,rollbackPlan,transaction);
   JSONObject firstTwo=new JSONObject(transaction.toString());JSONArray subset=new JSONArray().put(transaction.getJSONArray("parts").getJSONObject(0)).put(transaction.getJSONArray("parts").getJSONObject(1));firstTwo.put("parts",subset);
   for(String path:targets)e.run(e.job(),"printf before-failure > "+q(path+"/marker")+"\n",false);
   String undo=ExternalScopes.undo(transaction,control),apply=ExternalScopes.apply(firstTwo,control);boolean failed=false;
   try{e.run(e.job(),"undo_test() { rc=$?; trap - EXIT; set +e; (set -e; "+undo+"); rollback_rc=$?; trap finish EXIT; [ \"$rollback_rc\" = 0 ] || exit 79; exit \"$rc\"; }\ntrap undo_test EXIT\n"+apply+"exit 77\n",false);}catch(IOException expected){require(expected.getMessage().contains("Root job failed (77)"),"Unexpected injected failure: "+expected);failed=true;}
   require(failed,"Injected external failure was ignored");for(String path:targets)require(e.run(e.job(),"cat "+q(path+"/marker")+"\n",false).trim().equals("before-failure"),"External rollback lost previous data");ExternalData.cleanup(e,transaction);result.putString("rollback","PASS after two swaps, injected failure restored pre-restore external directories across the transaction; no pending restore");
   live.restore(live.prepare(Uri.fromFile(legacy)),false,false);require(e.run(e.job(),"sha256sum < "+q(targets.get(0)+"/payload.bin")+"\n",false).trim().split("\\s+")[0].equals(hashes.get(0)),"Older streamed ZIP failed");result.putString("older_backup","PASS legacy format-4 primary-only backup restored; other scopes were not selected for replacement");
   // A disabled scope is omitted from the next backup; absent/empty folders do not abort private data.
   require(prefs.edit().putBoolean("includeInternalObb",false).commit(),"Cannot change test scope");String cardObb=targets.get(3);e.run(e.job(),"rm -f -- "+q(cardObb+"/marker")+" "+q(cardObb+"/payload.bin")+"\n",false);e.backupOne(Collections.singletonList(SetupCheck.PACKAGE),tree,folder);LiveRestore.Plan omitted=live.importArchive(e.lastBackupUri,true);require(omitted.manifest.getJSONObject("external").getJSONArray("scopes").length()==2,"Disabled/empty scopes not omitted");result.putString("scope_choices","PASS disabled internal OBB and empty removable OBB omitted while private and remaining external data backed up");
  }finally{
   SharedPreferences.Editor restore=prefs.edit();for(String key:ExternalScopes.KEYS){Object prior=original.get(key);if(prior instanceof Boolean)restore.putBoolean(key,(Boolean)prior);else restore.remove(key);}restore.commit();
   if(!live.pointer.exists()){
    if(installed)e.run(e.job(),"pm uninstall "+q(SetupCheck.PACKAGE)+"\n",false);
    if(folder!=null)DocumentsContract.deleteDocument(c.getContentResolver(),folder);
    for(String path:targets)e.run(e.job(),"rm -rf -- "+q(path)+"\n",false);
    legacy.delete();e.clean(own);
   }
  }
  require(!live.pointer.exists(),"Test left pending restore");result.putString("result","PASS");finish(-1,result);
 }catch(Throwable failure){result.putString("error",android.util.Log.getStackTraceString(failure));finish(0,result);}}
}
