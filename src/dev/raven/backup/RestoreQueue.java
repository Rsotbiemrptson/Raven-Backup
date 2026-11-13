package dev.raven.backup;
import android.content.Context;import android.content.pm.*;import android.os.Build;import org.json.*;import java.io.*;import java.util.*;
/** Validate archives first; independently restore each app and retain a readable report. */
final class RestoreQueue {
 interface Progress{void update(int current,int total,String name);}
 static final class Item{BackupCatalog.Entry entry;LiveRestore.Plan plan;Set<String> targets=new TreeSet<>();List<String> missing=new ArrayList<>();String label,skipReason,signingTarget;boolean invalidArchive;}
 final LiveRestore restore;final List<Item> items=new ArrayList<>();final List<BackupCatalog.Entry> invalid=new ArrayList<>();final Set<File> jobs=new LinkedHashSet<>();String lastReport="";
 RestoreQueue(Context c){restore=new LiveRestore(c);}
 String compatibility(Item item,JSONObject saved,boolean review)throws Exception{
  String pkg=saved.getString("package");
  try{
   JSONObject now=restore.engine.info(pkg);
   try{SystemAppData.require(saved,now,item.plan.manifest);}catch(IOException incompatible){return incompatible.getMessage();}
   String identity=RestorePreparation.destinationIdentity(now);
   if(!review&&item.signingTarget!=null&&!item.signingTarget.equals(identity))
    return "Installed app changed after compatibility review.";
   if(RestorePreferences.differentSignature(saved,now)||RestorePreferences.differentVersion(saved,now)){
    String warning=RestorePreferences.warning(restore.engine.c,saved,now);
    if(warning!=null)return warning;
    if(review){item.signingTarget=identity;item.plan.approvedSigners.put(pkg,identity);}
    else if(!identity.equals(item.plan.approvedSigners.get(pkg)))return "Review the changed installation again.";
   }
   return null;
  }
  catch(PackageManager.NameNotFoundException absent){if(SystemAppData.system(saved))return "The matching system app must already be installed by the firmware.";if(item.signingTarget!=null)return "Reviewed app is no longer installed.";JSONArray apks=saved.optJSONArray("apks");if(apks==null||apks.length()==0)return "App is not installed and this backup contains no installer.";List<File> files=new ArrayList<>();for(int i=0;i<apks.length();i++)files.add(new File(item.plan.job,apks.getJSONObject(i).getString("file")));String reason=ApkCompatibility.nativeReason(files,Build.SUPPORTED_ABIS);if(reason!=null)return reason;PackageInfo info=restore.engine.pm.getPackageArchiveInfo(files.get(0).getPath(),0);if(info==null)throw new IOException("Cannot inspect saved installer for "+item.label);if(info.applicationInfo.minSdkVersion>Build.VERSION.SDK_INT)return "Saved installer requires Android API "+info.applicationInfo.minSdkVersion+"; this device uses API "+Build.VERSION.SDK_INT+".";item.missing.add(pkg);return null;}
 }
 File copyAppJob(File raw,JSONObject app)throws Exception{File job=restore.engine.job();jobs.add(job);Set<String> names=new LinkedHashSet<>();JSONArray payloads=app.getJSONArray("payloads");for(int i=0;i<payloads.length();i++){JSONObject p=payloads.getJSONObject(i);names.add(p.getString("tar"));names.add(p.getString("labels"));}JSONArray apks=app.optJSONArray("apks");if(apks!=null)for(int i=0;i<apks.length();i++)names.add(apks.getJSONObject(i).getString("file"));for(String name:names){if(!new File(name).getName().equals(name))throw new IOException("Unsafe staged filename");java.nio.file.Files.copy(new File(raw,name).toPath(),new File(job,name).toPath());}return job;}
 void prepare(List<BackupCatalog.Entry> entries)throws Exception{synchronized(LiveRestore.class){if(restore.pointer.exists())throw new IOException("Recover the pending restore first");if(!restore.enabled())throw new IOException("Live restore is not verified on this firmware");Set<String> seen=new HashSet<>();try{for(BackupCatalog.Entry entry:entries){LiveRestore.Plan raw;try{raw=restore.importArchive(entry.uri);}catch(InterruptedIOException interrupted){throw interrupted;}catch(Exception e){String reason=BackupIssues.reason(e);BackupIssues.mark(restore.engine.c,entry,reason);invalid.add(entry);Item bad=new Item();bad.entry=entry;bad.label=entry.name;bad.skipReason="Backup validation failed: "+reason;bad.invalidArchive=true;items.add(bad);continue;}BackupIssues.clear(restore.engine.c,entry);jobs.add(raw.job);JSONArray apps=raw.manifest.getJSONArray("apps");for(int i=0;i<apps.length();i++){JSONObject saved=apps.getJSONObject(i);String pkg=saved.getString("package");if(!seen.add(pkg))throw new IOException("Choose only one backup version per app: "+AppNames.saved(restore.engine.c,saved,entry.uri));Item item=new Item();item.entry=entry;item.label=AppNames.saved(restore.engine.c,saved,entry.uri);item.targets.add(pkg);item.plan=new LiveRestore.Plan();item.plan.source=raw.source;item.plan.job=apps.length()==1?raw.job:copyAppJob(raw.job,saved);item.plan.manifest=new JSONObject(raw.manifest.toString());item.plan.manifest.put("apps",new JSONArray().put(new JSONObject(saved.toString())));items.add(item);restore.reviewEnvironment(item.plan);item.skipReason=compatibility(item,saved,true);}}if(items.isEmpty())throw new IOException("No backups selected");}catch(Exception e){close();throw e;}}}
 boolean hasValidArchives(){for(Item i:items)if(!i.invalidArchive)return true;return false;}
 boolean needsMigrationConsent(){for(Item i:items)if(i.skipReason==null&&!i.plan.migrationWarning.isEmpty())return true;return false;}
 void approveMigrations(){for(Item i:items)if(i.skipReason==null)restore.approveMigration(i.plan);}
 String description()throws Exception{StringBuilder s=new StringBuilder();int index=0;for(Item i:items){s.append(++index).append(". ").append(i.label).append("\n").append(i.entry.name).append("\n");if(i.skipReason!=null)s.append("Will be skipped: ").append(i.skipReason);else s.append(i.plan.migrationWarning.isEmpty()?FirmwareCheck.source(i.plan.manifest):i.plan.migrationWarning).append(i.missing.isEmpty()?"":"\nSaved installer will be installed if still missing.");if(i.plan!=null&&i.plan.manifest.has("external"))s.append("\nIncludes primary external app files. The recovery ZIP covers private data only.");if(i.signingTarget!=null)s.append("\nData-only restore into the installed copy.");s.append("\n\n");}return s.toString();}
 String run(boolean keep,boolean launch,Progress progress)throws Exception{synchronized(LiveRestore.class){RestoreBatch batch=new RestoreBatch();List<RestoreBatch.Task> tasks=new ArrayList<>();Map<RestoreBatch.Task,Item> taskItems=new IdentityHashMap<>();try{
  for(Item item:items){RestoreBatch.Task task=new RestoreBatch.Task(){public String name(){return item.label+" ("+item.entry.name+")";}public boolean pendingRecovery(){return restore.pointer.exists();}
   public void prepare()throws Exception{if(item.skipReason!=null)throw new RestoreBatch.Incompatible(item.skipReason);FirmwareCheck.require(item.plan.manifest,item.plan.migrationApproval);item.missing.clear();String reason=compatibility(item,item.plan.manifest.getJSONArray("apps").getJSONObject(0),false);if(reason!=null)throw new RestoreBatch.Incompatible(reason);if(item.signingTarget==null)ApkBackup.installMissing(restore.engine,item.plan.job,item.plan.manifest,item.targets,item.plan.migrationApproval);restore.prepare(item.plan,item.targets);}
   public void restore()throws Exception{String result=restore.restore(item.plan,keep,false);if(!result.startsWith("Restored "))throw new IOException(result);}};tasks.add(task);taskItems.put(task,item);}
  batch.run(tasks,(n,total,name)->progress.update(n,total,name));List<String> restored=new ArrayList<>();for(RestoreBatch.Result result:batch.results)if(result.status.equals("Restored"))restored.addAll(taskItems.get(result.task).targets);
  String tail=restored.isEmpty()?"No app data was replaced.\n":keep?"Recovery backups for completed restores are kept in Settings.\n":"";if(launch&&!batch.stopped&&!restored.isEmpty())try{tail+=restore.launchRestored(restored);}catch(Exception e){tail+="App launch warning: "+e.getMessage();}else tail+="Restored apps remain stopped; open them when ready.";
  lastReport=batch.report()+"\n"+tail;return lastReport;
 }finally{close();}}}
 void close(){String preserve=null;if(restore.pointer.exists())try{preserve=new JSONObject(BackupEngine.read(restore.pointer,1024*1024)).optString("incoming");}catch(Exception e){return;}for(File job:jobs)if(!job.getPath().equals(preserve))restore.engine.clean(job);}
}
