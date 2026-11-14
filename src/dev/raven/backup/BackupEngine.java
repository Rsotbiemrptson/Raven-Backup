package dev.raven.backup;
import android.content.*;import android.content.pm.*;import android.os.*;import android.net.Uri;import android.provider.*;import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;import java.security.*;import java.util.*;import java.util.zip.*;
public final class BackupEngine {
 final Context c;final PackageManager pm;static final long MAX=16L*1024*1024*1024;
 volatile File cancellation;
 volatile android.net.Uri lastBackupUri;
 Boolean backupApks,backupExternal;
 Integer backupOrder;
 static final class BatchFailure extends IOException {
  final List<String> completed;
  BatchFailure(String message,Exception cause,List<String> completed){super(message,cause);this.completed=new ArrayList<>(completed);}
 }
 BackupEngine(Context c){this.c=c;pm=c.getPackageManager();}
 String tarFunction(){
  if(cancellation==null)return "archive_tar() { tar \"$@\"; }\n";
  return "archive_tar() { tar \"$@\" & child=$!; while kill -0 \"$child\" 2>/dev/null; do if [ -f "+q(cancellation.getPath())+" ]; then kill -TERM \"$child\" 2>/dev/null || true; wait \"$child\" || true; echo 'Scheduled backup cancelled'; exit 130; fi; sleep 0.25; done; wait \"$child\"; }\n";
 }
 static boolean eligible(Context c,ApplicationInfo a){
  if(a.packageName.equals(c.getPackageName()))return false;
  try{return SystemAppData.eligible(c.getPackageManager().getPackageInfo(a.packageName,0));}
  catch(PackageManager.NameNotFoundException absent){return false;}
 }
 static String q(String s){return "'"+s.replace("'","'\\''")+"'";}
 static void write(File f,String s)throws IOException{try(FileOutputStream o=new FileOutputStream(f)){o.write(s.getBytes(StandardCharsets.UTF_8));}}
 static long copy(InputStream in,OutputStream out,long limit)throws IOException{byte[] b=new byte[65536];long total=0;int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Operation stopped");total+=n;if(total>limit)throw new IOException("Input exceeds size limit");out.write(b,0,n);}return total;}
 static String read(File f,long limit)throws IOException{try(InputStream in=new FileInputStream(f)){ByteArrayOutputStream out=new ByteArrayOutputStream();copy(in,out,limit);return out.toString("UTF-8");}}
 static String digest(byte[] bytes)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
 static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 static String hash(File f)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}return hex(md.digest());}
 String probe()throws Exception{String id=RootBridge.call("id");if(!id.startsWith("uid=0("))throw new SecurityException("Root not demonstrated: "+id);return id+"\nOEM bridge confirmed on this firmware.";}
 static String folderName(Uri tree){try{return DocumentsContract.getTreeDocumentId(tree).replace("primary:","Internal storage/");}catch(Exception e){return "Chosen backup folder";}}
 File job()throws IOException{File f=new File(c.getFilesDir(),"job-"+UUID.randomUUID());if(!f.mkdir())throw new IOException("Cannot create private job");return f;}
 String run(File job,String body,boolean launcher)throws Exception{return run(job,body,launcher,30L*60*1000);}
 String runLong(File job,String body)throws Exception{return run(job,body,false,12L*60*60*1000);}
 private String run(File job,String body,boolean launcher,long timeout)throws Exception{
  probe();File script=new File(job,"job.sh"),log=new File(job,"root.log"),done=new File(job,"done");String uid=""+android.os.Process.myUid();
  write(script,"#!/system/bin/sh\nset -eu\nset -o pipefail\numask 077\n[ ! -e "+q(done.getPath())+" ] || exit 1\nfinish() { rc=$?; trap - EXIT; printf '%s' \"$rc\" > "+q(done.getPath())+"; chown "+uid+":"+uid+" "+q(done.getPath())+" "+q(log.getPath())+"; exit \"$rc\"; }\ntrap finish EXIT\nexec > "+q(log.getPath())+" 2>&1\n[ \"$(id -u)\" = 0 ]\n"+body+"\n");
  if(launcher){if(!Environment.isExternalStorageManager())throw new IOException("Internal storage access required");File shared=new File(Environment.getExternalStorageDirectory(),"raven-backup-"+job.getName()+".sh");write(shared,"sh "+q(script.getPath())+"\n");}
  RootBridge.call("sh "+q(script.getPath()));if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Operation stopped");long deadline=System.currentTimeMillis()+timeout;while(!done.exists()&&System.currentTimeMillis()<deadline)Thread.sleep(250);
  if(!done.exists())throw new IOException("Root job timed out; inspect "+log+" before retrying");String rc=read(done,100).trim();if(!rc.equals("0")){String output=read(log,1024*1024);String running=SystemAppData.runningPackage(output);if(running!=null)throw new SystemAppData.Running(running);throw new IOException("Root job failed ("+rc+"): "+output);}return read(log,1024*1024);
 }
 JSONObject info(String pkg)throws Exception{if(!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))throw new SecurityException("Invalid package");PackageInfo pi=pm.getPackageInfo(pkg,PackageManager.GET_SIGNING_CERTIFICATES);if(!eligible(c,pi.applicationInfo))throw new SecurityException("This app identity is unsupported: "+pkg);if(pi.signingInfo==null)throw new SecurityException("No signing identity");List<String> hashes=new ArrayList<>();for(android.content.pm.Signature sig:pi.signingInfo.getApkContentsSigners())hashes.add(digest(sig.toByteArray()));Collections.sort(hashes);return new JSONObject().put("package",pkg).put("label",pm.getApplicationLabel(pi.applicationInfo).toString()).put("uid",pi.applicationInfo.uid).put("version",pi.getLongVersionCode()).put("signers",new JSONArray(hashes)).put("systemApp",SystemAppData.system(pi.applicationInfo)).put("persistent",(pi.applicationInfo.flags&ApplicationInfo.FLAG_PERSISTENT)!=0).put("sharedUser",pi.sharedUserId==null?"":pi.sharedUserId);}
 JSONObject archive(List<String> packages,File job,boolean launcher)throws Exception{synchronized(LiveRestore.class){return archiveInternal(packages,job,launcher);}}
 JSONObject archiveInternal(List<String> packages,File job,boolean launcher)throws Exception{
  if(new File(c.getFilesDir(),"live-pending.json").exists())throw new IOException("Recover the pending restore before backing up apps");
  if(android.os.Process.myUid()>=100000)throw new SecurityException("Only user 0 supported");if(!((UserManager)c.getSystemService(Context.USER_SERVICE)).isUserUnlocked())throw new IOException("Unlock device first");if(packages.isEmpty()||packages.size()>1000)throw new IOException("Invalid app count");
  JSONObject manifest=new JSONObject().put("format",1).put("fingerprint",Build.FINGERPRINT).put("model",Build.MODEL).put("sdk",Build.VERSION.SDK_INT).put("androidRelease",Build.VERSION.RELEASE).put("manufacturer",Build.MANUFACTURER).put("device",Build.DEVICE).put("user",0).put("created",System.currentTimeMillis()).put("selinux","NUL-paired sidecar; tar --selinux broken on tested firmware");FirmwareCheck.capture(manifest);JSONArray entries=new JSONArray();StringBuilder shell=new StringBuilder("tar --help | grep -q numeric-owner\n"+tarFunction());int index=0;
  Set<String> targets=new TreeSet<>(packages);
  for(String pkg:targets){JSONObject app=info(pkg);JSONArray payloads=new JSONArray();if(SystemAppData.system(app)&&app.optBoolean("persistent",false))shell.append(SystemAppData.stopped(app));shell.append("am force-stop --user 0 ").append(q(pkg)).append("\n").append(SystemAppData.stopped(app));for(String domain:new String[]{"ce","de"}){String source=(domain.equals("ce")?"/data/user/0/":"/data/user_de/0/")+pkg;String base="app"+index+"-"+domain;File tar=new File(job,base+".tar"),labels=new File(job,base+".labels");if(domain.equals("ce")&&!SystemAppData.system(app))shell.append("[ -d ").append(q(source)).append(" ] || { echo 'CE directory absent'; exit 1; }\n");
   shell.append("if [ -d ").append(q(source)).append(" ]; then\n[ ! -L ").append(q(source)).append(" ]\n[ \"$(stat -c %u ").append(q(source)).append(")\" = ").append(app.getInt("uid")).append(" ]\narchive_tar --numeric-owner --exclude ./lib --exclude ./cache --exclude ./code_cache -cpf ").append(q(tar.getPath())).append(" -C ").append(q(source)).append(" .\n(cd ").append(q(source)).append("; find . \\( -path ./lib -o -path ./cache -o -path ./code_cache \\) -prune -o -print0 | while IFS= read -r -d '' path; do label=$(ls -Zd \"$path\"); label=${label%% *}; printf '%s\\0%s\\0' \"$path\" \"$label\"; done) > ").append(q(labels.getPath())).append("\nchown ").append(android.os.Process.myUid()).append(":").append(android.os.Process.myUid()).append(" ").append(q(tar.getPath())).append(" ").append(q(labels.getPath())).append("\nfi\n");payloads.put(new JSONObject().put("domain",domain).put("tar",base+".tar").put("labels",base+".labels"));}
   shell.append(SystemAppData.stopped(app));app.put("payloads",payloads);entries.put(app);index++;}
  shell.append("echo 'Backup tar phase complete'\n");run(job,shell.toString(),launcher);
  for(int i=0;i<entries.length();i++){JSONArray p=entries.getJSONObject(i).getJSONArray("payloads");for(int j=p.length()-1;j>=0;j--){JSONObject payload=p.getJSONObject(j);File tar=new File(job,payload.getString("tar"));if(!tar.exists()){p.remove(j);continue;}payload.put("sha256",hash(tar)).put("bytes",tar.length()).put("labelsSha256",hash(new File(job,payload.getString("labels"))));}if(p.length()==0)throw new IOException("No app data directory is available");}
  manifest.put("apps",entries);write(new File(job,"manifest.json"),manifest.toString(2));return manifest;
 }
 void addZip(ZipOutputStream out,File f)throws IOException{out.putNextEntry(new ZipEntry(f.getName()));try(InputStream in=new FileInputStream(f)){copy(in,out,MAX);}out.closeEntry();}
 void zip(File job,JSONObject manifest,OutputStream stream)throws Exception{zip(job,manifest,stream,null);}
 void zip(File job,JSONObject manifest,OutputStream stream,String external)throws Exception{try(ZipOutputStream out=new ZipOutputStream(stream)){addZip(out,new File(job,"manifest.json"));JSONArray apps=manifest.getJSONArray("apps");for(int i=0;i<apps.length();i++){JSONArray p=apps.getJSONObject(i).getJSONArray("payloads");for(int j=0;j<p.length();j++){addZip(out,new File(job,p.getJSONObject(j).getString("tar")));addZip(out,new File(job,p.getJSONObject(j).getString("labels")));}JSONArray apks=apps.getJSONObject(i).optJSONArray("apks");if(apks!=null)for(int j=0;j<apks.length();j++)addZip(out,new File(job,apks.getJSONObject(j).getString("file")));}if(external!=null)ExternalData.writeStream(this,job,external,out);
 }}
 interface BackupProgress {void update(String phase,int current,int total,String label,long estimatedBytes);}
 String backup(List<String> packages,Uri tree)throws Exception{
  return backup(packages,tree,null);
 }
 static boolean canReadSizes(Context context){
  android.app.AppOpsManager ops=context.getSystemService(android.app.AppOpsManager.class);
  return ops!=null&&ops.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
   android.os.Process.myUid(),context.getPackageName())==android.app.AppOpsManager.MODE_ALLOWED;
 }
 long installerSize(ApplicationInfo app){
  if(!backupApks||SystemAppData.system(app))return 0;
  long bytes=new File(app.sourceDir).length();
  if(app.splitSourceDirs!=null)for(String split:app.splitSourceDirs)bytes=Math.addExact(bytes,new File(split).length());
  return bytes;
 }
 long estimate(String pkg)throws Exception{
  ApplicationInfo app=pm.getApplicationInfo(pkg,0);
  if(!eligible(c,app))throw new IOException("Unsupported backup app: "+pkg);
  // Android's internal-volume data total also includes primary external app files.
  // A private-only backup needs the directory fallback to keep its scope meaningful.
  if(backupExternal&&app.uid>=10000&&canReadSizes(c)
    &&(app.storageUuid==null||app.storageUuid.equals(android.os.storage.StorageManager.UUID_DEFAULT))){
   try{
    android.app.usage.StorageStats stats=c.getSystemService(android.app.usage.StorageStatsManager.class)
     .queryStatsForPackage(android.os.storage.StorageManager.UUID_DEFAULT,pkg,android.os.Process.myUserHandle());
    return Math.addExact(Math.max(0,stats.getDataBytes()-stats.getCacheBytes()),installerSize(app));
   }catch(SecurityException|IOException unavailable){
    // The user may revoke Usage access or the firmware may lack quota statistics.
   }
  }
  List<String> paths=new ArrayList<>();
  paths.add("/data/user/0/"+pkg);paths.add("/data/user_de/0/"+pkg);
  if(backupExternal)paths.add("/data/media/0/Android/data/"+pkg+"/files");
  StringBuilder command=new StringBuilder("set -eu\n");
  for(String path:paths)command.append("[ ! -L ").append(q(path)).append(" ]\nif [ -d ")
   .append(q(path)).append(" ]; then du -sk ").append(q(path)).append("; fi\n");
  command.append("printf 'RAVEN_SIZE_OK\\n'\n");
  String output=run(job(),command.toString(),false).trim();
  if(!output.endsWith("RAVEN_SIZE_OK"))throw new IOException("Cannot estimate all selected app data: "+pkg);
  output=output.substring(0,output.length()-"RAVEN_SIZE_OK".length()).trim();
  long bytes=0;
  if(!output.isEmpty())for(String line:output.split("\\r?\\n")){
   String count=line.trim().split("\\s+",2)[0];
   if(!count.matches("[0-9]+"))throw new IOException("Cannot estimate app data size: "+pkg);
   bytes=Math.addExact(bytes,Math.multiplyExact(Long.parseLong(count),1024));
  }
  return Math.addExact(bytes,installerSize(app));
 }
 String backup(List<String> packages,Uri tree,BackupProgress progress)throws Exception{
  if(packages.isEmpty())throw new IOException("Select at least one app");
  if(backupApks==null)backupApks=c.getSharedPreferences("MainActivity",0).getBoolean("includeApks",true);
  if(backupExternal==null)backupExternal=RestorePreferences.prefs(c).getBoolean(RestorePreferences.EXTERNAL,true);
  if(backupOrder==null)backupOrder=BackupOrder.normalize(c.getSharedPreferences("MainActivity",0).getInt("backupOrder",BackupOrder.PACKAGE));
  List<String> finished=new ArrayList<>();
  Set<String> targets=new TreeSet<>(packages);
  List<BackupOrder.App> ordered=new ArrayList<>();
  long totalBytes=0;
  for(String pkg:targets){
   if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Backup preparation stopped");
   String label=pm.getApplicationLabel(pm.getApplicationInfo(pkg,0)).toString();
   if(progress!=null)progress.update("Preparing",ordered.size()+1,targets.size(),label,totalBytes);
   long bytes=estimate(pkg);totalBytes=Math.addExact(totalBytes,bytes);
   ordered.add(new BackupOrder.App(pkg,label,bytes));
   if(progress!=null)progress.update("Preparing",ordered.size(),targets.size(),label,totalBytes);
  }
  BackupOrder.sort(ordered,backupOrder);
  for(BackupOrder.App app:ordered){
   if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Backup stopped.\n\nCompleted apps ("+finished.size()+"):\n"+String.join("\n",finished));
   try{
    if(progress!=null)progress.update("Backing up",finished.size()+1,ordered.size(),app.label,app.bytes);
    Uri parent=appFolder(tree,app.label,app.pkg);
    backupOne(Collections.singletonList(app.pkg),tree,parent);finished.add(app.label);
   }catch(Exception e){throw new BatchFailure("Backup stopped for "+app.label+"\n"+app.pkg+"\n\n"+e.getMessage()+"\n\nCompleted apps ("+finished.size()+"):\n"+String.join("\n",finished),e,finished);}
  }
  return "Backed up "+finished.size()+" apps, each in its own ZIP and app folder.\n"+folderName(tree)+"\nApps remain stopped.\n\nCompleted apps:\n"+String.join("\n",finished);
 }
 Uri appFolder(Uri tree,String label,String pkg)throws Exception {
  String safe=label.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]","_").trim();if(safe.length()>70)safe=safe.substring(0,70);if(safe.isEmpty())safe=pkg;
  String name=safe+" ("+pkg+")";Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
  Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
  try(android.database.Cursor rows=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
   if(rows==null)throw new IOException("Cannot read backup folder");while(rows.moveToNext())if(name.equals(rows.getString(1))&&DocumentsContract.Document.MIME_TYPE_DIR.equals(rows.getString(2)))return DocumentsContract.buildDocumentUriUsingTree(tree,rows.getString(0));
  }
  Uri folder=DocumentsContract.createDocument(c.getContentResolver(),parent,DocumentsContract.Document.MIME_TYPE_DIR,name);if(folder==null)throw new IOException("Cannot create app backup folder");return folder;
 }
 String backupOne(List<String> packages,Uri tree,Uri parent)throws Exception{
  synchronized(LiveRestore.class){return backupOneLocked(packages,tree,parent);}
 }
 Set<String> backupFiles(Uri parent)throws Exception{Set<String> names=new HashSet<>();Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(parent,DocumentsContract.getDocumentId(parent));try(android.database.Cursor rows=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){if(rows==null)throw new IOException("Cannot list backup versions");while(rows.moveToNext())names.add(rows.getString(0).toLowerCase(Locale.ROOT));}return names;}
 String backupOneLocked(List<String> packages,Uri tree,Uri parent)throws Exception{
  File job=job();String label=packages.size()==1?pm.getApplicationLabel(pm.getApplicationInfo(packages.get(0),0)).toString():"RavenBackup";String stem=BackupNames.stem(label,new Date());String name=BackupNames.available(stem,backupFiles(parent));
  Uri doc=DocumentsContract.createDocument(c.getContentResolver(),parent,"application/octet-stream",".raven-"+UUID.randomUUID()+".pending");if(doc==null)throw new IOException("Destination cannot create a backup");
  String external=null;
  try{
   try(android.database.Cursor row=c.getContentResolver().query(doc,new String[]{DocumentsContract.Document.COLUMN_FLAGS},null,null,null)){if(row==null||!row.moveToFirst()||(row.getInt(0)&DocumentsContract.Document.FLAG_SUPPORTS_RENAME)==0)throw new IOException("Choose a folder that supports safely finishing backups, such as internal storage or an SD card");}
   JSONObject manifest=archive(packages,job,true);if(backupApks!=null?backupApks:c.getSharedPreferences("MainActivity",0).getBoolean("includeApks",true))ApkBackup.capture(this,job,manifest);
   if(packages.size()==1&&(backupExternal!=null?backupExternal:RestorePreferences.prefs(c).getBoolean(RestorePreferences.EXTERNAL,true))){
    external=ExternalData.source(this,parent,packages.get(0));
    if(external!=null)manifest.put("format",ExternalData.STREAM_FORMAT);
   }
   write(new File(job,"manifest.json"),manifest.toString(2));
   ParcelFileDescriptor descriptor=c.getContentResolver().openFileDescriptor(doc,"w");if(descriptor==null)throw new IOException("Destination cannot write");
   try(OutputStream raw=new ParcelFileDescriptor.AutoCloseOutputStream(descriptor)){
    zip(job,manifest,new FilterOutputStream(raw){@Override public void write(byte[] bytes,int offset,int length)throws IOException{out.write(bytes,offset,length);}@Override public void close()throws IOException{flush();}},external);raw.flush();
    try{android.system.Os.fsync(descriptor.getFileDescriptor());}catch(Exception unsupported){throw new IOException("This storage provider cannot safely finish backups. Choose local storage or an SD card.",unsupported);}
   }
   if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Backup stopped before completion");
   StringBuilder systemCheck=new StringBuilder();JSONArray savedApps=manifest.getJSONArray("apps");for(int i=0;i<savedApps.length();i++)systemCheck.append(SystemAppData.stopped(savedApps.getJSONObject(i)));if(systemCheck.length()>0)run(job(),systemCheck.toString(),false);
   name=BackupNames.available(stem,backupFiles(parent));Uri finished=DocumentsContract.renameDocument(c.getContentResolver(),doc,name);if(finished==null)throw new IOException("Could not finish the backup filename");doc=finished;
  }catch(Exception e){try{DocumentsContract.deleteDocument(c.getContentResolver(),doc);}catch(Exception ignored){}throw e;}finally{clean(job);}
  lastBackupUri=doc;return "Backed up "+packages.size()+" apps to "+folderName(tree)+"/"+name+"\nApps remain stopped. Restore is available for supported installed apps.";
 }
 JSONObject unpack(InputStream stream,File job)throws Exception{Set<String> names=new HashSet<>();long total=0;JSONObject streamedExternal=null;try(ZipInputStream in=new ZipInputStream(new BufferedInputStream(stream,65536))){ZipEntry e;while((e=in.getNextEntry())!=null){String n=e.getName();if(n.equals(ExternalData.ENTRY)){
    if(e.isDirectory()||!names.contains("manifest.json")||!names.add(n))throw new IOException("Unexpected external data entry");
    JSONObject manifest=new JSONObject(read(new File(job,"manifest.json"),1024*1024));
    if(manifest.getInt("format")==ExternalData.STREAM_FORMAT){
     if(manifest.has("external")||manifest.getJSONArray("apps").length()!=1)throw new IOException("Invalid streamed backup manifest");
     streamedExternal=ExternalData.verifyStream(in);
     names.add(ExternalData.FOOTER);
     in.closeEntry();continue;
    }
    JSONObject external=ExternalData.metadata(manifest);
    if(external==null)throw new IOException("Unexpected external data entry");
    ExternalData.read(in,external,null);in.closeEntry();continue;
   }if(!n.matches("manifest\\.json|app[0-9]+-(ce|de)\\.(tar|labels)|app[0-9]+-apk[0-9]+\\.apk")||e.isDirectory()||!names.add(n)||names.size()>10240)throw new IOException("Unexpected/duplicate ZIP entry");try(OutputStream out=new FileOutputStream(new File(job,n))){total+=copy(in,out,MAX-total);}in.closeEntry();}}
  return validateImported(job,names,streamedExternal);
 }
 JSONObject validateImported(File job,Set<String> names,JSONObject streamedExternal)throws Exception{
  JSONObject m=new JSONObject(read(new File(job,"manifest.json"),1024*1024));int format=m.getInt("format");if(format<1||format>ExternalData.STREAM_FORMAT||m.getInt("user")!=0)throw new IOException("Unsupported format/user");JSONArray apps=m.getJSONArray("apps");if(apps.length()==0||apps.length()>1000)throw new IOException("Invalid app count");Set<String> expected=new HashSet<>(Collections.singleton("manifest.json")),packages=new HashSet<>();for(int i=0;i<apps.length();i++){JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");if(!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")||!packages.add(pkg))throw new IOException("Invalid/duplicate package");JSONArray payloads=app.getJSONArray("payloads");if(payloads.length()<1||payloads.length()>2)throw new IOException("Invalid payload count");Set<String> domains=new HashSet<>();for(int j=0;j<payloads.length();j++){JSONObject p=payloads.getJSONObject(j);String domain=p.getString("domain");if(!(domain.equals("ce")||domain.equals("de"))||!domains.add(domain))throw new IOException("Invalid domain");String tar="app"+i+"-"+domain+".tar",labels="app"+i+"-"+domain+".labels";if(!p.getString("tar").equals(tar)||!p.getString("labels").equals(labels))throw new IOException("Invalid payload path");expected.add(tar);expected.add(labels);File f=new File(job,tar);if(f.length()!=p.getLong("bytes")||!hash(f).equals(p.getString("sha256"))||!hash(new File(job,labels)).equals(p.getString("labelsSha256")))throw new IOException("Payload checksum mismatch");Set<String> paths=TarGuard.validate(f,pkg);validateLabels(new File(job,labels),paths);}}
  for(int i=0;i<apps.length();i++){JSONObject app=apps.getJSONObject(i);if(m.getInt("format")==1&&app.has("apks"))throw new IOException("APK entries require format 2");ApkBackup.validate(this,job,app,i,expected);}if(format==ExternalData.STREAM_FORMAT){
   if(streamedExternal==null)throw new IOException("Streamed external data missing");
   m.put("external",streamedExternal);expected.add(ExternalData.FOOTER);
  }
  if(ExternalData.metadata(m)!=null)expected.add(ExternalData.ENTRY);if(!names.equals(expected))throw new IOException("ZIP contents differ from manifest");return m;
 }
 void validateLabels(File f,Set<String> paths)throws Exception{String text=read(f,32*1024*1024);if(!text.endsWith("\0"))throw new IOException("Truncated labels");String[] v=text.split("\0",-1);if(v.length%2!=1)throw new IOException("Invalid labels");Set<String> seen=new HashSet<>();for(int i=0;i<v.length-1;i+=2){String p=TarGuard.path(v[i]);if(!paths.contains(p)||!seen.add(p)||!v[i+1].matches("[A-Za-z0-9_:.,-]+"))throw new IOException("Unsafe SELinux record");}if(!seen.equals(paths))throw new IOException("Missing SELinux records");}
 String rehearse(Uri uri)throws Exception{File job=job();try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Cannot read ZIP");return sandbox(job,unpack(in,job));}finally{clean(job);}}
 String sandbox(File job,JSONObject m)throws Exception{String root="/data/local/tmp/raven-restore-"+UUID.randomUUID();StringBuilder shell=new StringBuilder("mkdir -m 700 "+q(root)+"\n"),compat=new StringBuilder();JSONArray apps=m.getJSONArray("apps");for(int i=0;i<apps.length();i++){JSONObject app=apps.getJSONObject(i);try{JSONObject now=info(app.getString("package"));boolean match=now.getInt("uid")==app.getInt("uid")&&now.getLong("version")==app.getLong("version")&&now.getJSONArray("signers").toString().equals(app.getJSONArray("signers").toString())&&Build.FINGERPRINT.equals(m.getString("fingerprint"));compat.append(app.getString("package")).append(match?": identity/version match\n":": live restore incompatible\n");}catch(Exception e){compat.append(app.getString("package")).append(": not eligible/installed\n");}JSONArray payloads=app.getJSONArray("payloads");for(int j=0;j<payloads.length();j++){JSONObject p=payloads.getJSONObject(j);String dst=root+"/app"+i+"-"+p.getString("domain");shell.append("mkdir -m 700 ").append(q(dst)).append("\ntar --numeric-owner -xpf ").append(q(new File(job,p.getString("tar")).getPath())).append(" -C ").append(q(dst)).append("\n(cd ").append(q(dst)).append("; while IFS= read -r -d '' path && IFS= read -r -d '' label; do chcon -h \"$label\" \"$path\" || exit 1; done < ").append(q(new File(job,p.getString("labels")).getPath())).append(")\n");}}
  for(int i=0;i<apps.length();i++){
   JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");
   if(!(pkg.equals("dev.raven.backupfixture")||pkg.equals("dev.raven.backupfixture.two")))continue;
   JSONArray payloads=app.getJSONArray("payloads");
   for(int j=0;j<payloads.length();j++){
    String domain=payloads.getJSONObject(j).getString("domain");String relative=domain.equals("ce")?"files/nested/seed.txt":"files/de-seed.txt";
    String source=(domain.equals("ce")?"/data/user/0/":"/data/user_de/0/")+pkg+"/"+relative;
    String target=root+"/app"+i+"-"+domain+"/"+relative;
    shell.append("cmp ").append(q(source)).append(" ").append(q(target)).append("\n[ \"$(stat -c %a:%u:%g ").append(q(source)).append(")\" = \"$(stat -c %a:%u:%g ").append(q(target)).append(")\" ]\na=$(ls -Zd ").append(q(source)).append("); b=$(ls -Zd ").append(q(target)).append("); [ \"${a%% *}\" = \"${b%% *}\" ]\necho ").append(q(pkg+" "+domain+": contents, modes, owners, labels verified")).append("\n");
   }
  }
  shell.append("echo ").append(q("Isolated restore: "+root)).append("\n");String log=run(job,shell.toString(),false);return "Archive validated. "+log+compat+"Extracted ownership, modes and saved labels in isolation. Existing apps untouched. Live restore disabled. Test directory retained for inspection.";
 }
 String selfTest()throws Exception{if(!Environment.isExternalStorageManager())throw new IOException("Grant storage access");File job=job();JSONObject m=archive(Arrays.asList("dev.raven.backupfixture","dev.raven.backupfixture.two"),job,true);File zip=new File(Environment.getExternalStorageDirectory(),"raven-poc-fixture.zip");zip(job,m,new FileOutputStream(zip));File imported=job();try(InputStream in=new FileInputStream(zip)){String result=sandbox(imported,unpack(in,imported));write(new File(Environment.getExternalStorageDirectory(),"raven-poc-selftest.txt"),result);return "Multi-app backup self-test passed: "+zip+"\n"+result;}finally{clean(job);clean(imported);}}
 void clean(File job){File[] files=job.listFiles();if(files!=null)for(File f:files)if(f.isFile()&&(f.getName().endsWith(".tar")||f.getName().endsWith(".labels")||f.getName().endsWith(".apk")))f.delete();}
}
