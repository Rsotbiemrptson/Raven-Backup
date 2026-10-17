package dev.raven.backup;

import android.content.*;
import android.net.Uri;
import android.os.*;
import org.json.*;
import java.io.*;
import java.util.*;

/** Same-firmware user-app restore, with validated staging and transactional recovery. */
final class LiveRestore {
 static final List<String> FIXTURES=Arrays.asList("dev.raven.backupfixture","dev.raven.backupfixture.two");
 final BackupEngine engine;
 final File pointer;
 boolean testing;
 boolean simulateInterruption;
 boolean keepRollback=true;
 boolean launchAfter=true;
 static final class Plan {File job;JSONObject manifest;String description;}
 LiveRestore(Context context){engine=new BackupEngine(context);pointer=new File(context.getFilesDir(),"live-pending.json");}
 static String q(String s){return BackupEngine.q(s);}
 static String data(String pkg,String domain){return (domain.equals("ce")?"/data/user/0/":"/data/user_de/0/")+pkg;}
 String verificationKey()throws Exception{StringBuilder key=new StringBuilder(Build.FINGERPRINT);for(String pkg:FIXTURES)key.append(engine.info(pkg).toString());return BackupEngine.digest(key.toString().getBytes("UTF-8"));}
 boolean enabled(){return Build.FINGERPRINT.equals(engine.c.getSharedPreferences("live-restore",0).getString("general-verified",null));}
 Plan prepare(Uri uri)throws Exception {
  return prepare(importArchive(uri),null);
 }
 Plan importArchive(Uri uri)throws Exception {
  if(!testing&&!enabled())throw new IOException("Live restore is not verified on this firmware");
  Plan plan=new Plan();plan.job=engine.job();try(InputStream in=engine.c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Cannot read backup");plan.manifest=engine.unpack(in,plan.job);}catch(Exception e){engine.clean(plan.job);throw e;}
  return plan;
 }
 Plan prepare(Plan plan,Set<String> packages)throws Exception {
  if(packages!=null){JSONArray filtered=new JSONArray();Set<String> found=new HashSet<>();JSONArray apps=plan.manifest.getJSONArray("apps");for(int i=0;i<apps.length();i++){JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");if(packages.contains(pkg)){filtered.put(app);found.add(pkg);}}if(filtered.length()==0||!found.equals(packages))throw new IOException("Selected apps are absent from this backup");plan.manifest.put("apps",filtered);}
  plan.description=RestorePreparation.prepare(engine,plan.job,plan.manifest);compatible(plan.manifest);return plan;
 }
 String restore(Plan plan,boolean keep,boolean launch)throws Exception{synchronized(LiveRestore.class){keepRollback=keep;launchAfter=launch;return restoreInternal(plan,false);}}
 static void durable(File f,String text)throws Exception{
  File temporary=new File(f.getParentFile(),f.getName()+".new");try(FileOutputStream out=new FileOutputStream(temporary)){out.write(text.getBytes("UTF-8"));out.getFD().sync();}
  android.system.Os.rename(temporary.getPath(),f.getPath());java.io.FileDescriptor directory=android.system.Os.open(f.getParent(),android.system.OsConstants.O_RDONLY,0);try{android.system.Os.fsync(directory);}finally{android.system.Os.close(directory);}
 }

 void compatible(JSONObject manifest)throws Exception{
  if(android.os.Process.myUid()>=100000||!((UserManager)engine.c.getSystemService(Context.USER_SERVICE)).isUserUnlocked())throw new IOException("Unlocked user 0 required");
  FirmwareCheck.require(manifest);
  JSONArray apps=manifest.getJSONArray("apps");
  for(int i=0;i<apps.length();i++){
   JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");
   JSONObject now;try{now=engine.info(pkg);}catch(android.content.pm.PackageManager.NameNotFoundException missing){throw new IOException("App is not installed: "+pkg);}
   if(now.getInt("uid")!=app.getInt("uid")||now.getLong("version")!=app.getLong("version")||!now.getJSONArray("signers").toString().equals(app.getJSONArray("signers").toString()))throw new IOException("Package, UID, version or signing identity changed: "+pkg);
   JSONArray payloads=app.getJSONArray("payloads");if(payloads.length()<1||payloads.length()>2)throw new IOException("Invalid CE/DE payloads");
   for(int j=0;j<payloads.length();j++){
    String domain=payloads.getJSONObject(j).getString("domain");
    String context=RootBridge.call("ls -Zd "+q(data(pkg,domain))).trim().split("\\s+")[0];
    if(!context.matches("u:object_r:app_data_file:s0(:c[0-9]+(,c[0-9]+)*)?"))throw new IOException("Unexpected current app SELinux context: "+context);
   }
  }
 }

 String labels(File job,JSONObject payload)throws Exception{return BackupEngine.read(new File(job,payload.getString("labels")),32*1024*1024);}

 // Builds exact content/metadata checks from validated tar headers, not live source data.
 String verify(File job,JSONObject payload,String destination,int uid,boolean live)throws Exception{
  List<TarGuard.Record> records=TarGuard.records(new File(job,payload.getString("tar")));
  File expected=new File(job,payload.getString("tar")+".paths");ByteArrayOutputStream paths=new ByteArrayOutputStream();StringBuilder shell=new StringBuilder();boolean root=false;
  for(TarGuard.Record r:records){
   if(r.path.isEmpty()){if(!r.directory)throw new IOException("Archive root is not a directory");root=true;}
   String top=r.path.split("/",2)[0];if(top.equals("lib")||top.equals("cache")||top.equals("code_cache"))throw new IOException("Excluded directory in live archive");
   if(r.uid!=uid||r.gid!=uid||(r.mode&~0777L)!=0)throw new IOException("Unsupported owner/group or special permission bits");
   String relative=r.path.isEmpty()?".":"./"+r.path;paths.write(relative.getBytes("UTF-8"));paths.write(0);
   String path=destination+(r.path.isEmpty()?"":"/"+r.path);
   shell.append("[ ! -L ").append(q(path)).append(" ]\n[ ").append(r.directory?"-d ":"-f ").append(q(path)).append(" ]\n[ \"$(stat -c %a:%u:%g ").append(q(path)).append(")\" = ").append(q(Long.toOctalString(r.mode)+":"+r.uid+":"+r.gid)).append(" ]\n");
   if(!r.directory)shell.append("h=$(sha256sum ").append(q(path)).append("); [ \"${h%% *}\" = ").append(q(r.sha256)).append(" ]\n");
  }
  if(!root)throw new IOException("Archive root metadata missing");try(FileOutputStream out=new FileOutputStream(expected)){out.write(paths.toByteArray());out.getFD().sync();}
  String[] pairs=labels(job,payload).split("\0",-1);String current=RootBridge.call("ls -Zd "+q(destination)).trim().split("\\s+")[0];
  for(int i=0;i<pairs.length-1;i+=2){String relative=TarGuard.path(pairs[i]);if(!pairs[i+1].equals(current))for(TarGuard.Record record:records)if(record.path.equals(relative)&&record.directory&&SelinuxLabels.profile(relative,pairs[i+1]))throw new IOException("Category-free profile must be a regular file");if(live&&!pairs[i+1].equals(SelinuxLabels.adapt(relative,pairs[i+1],current,current)))throw new IOException("Saved SELinux labels differ from installed app category");String path=destination+(relative.isEmpty()?"":"/"+relative);shell.append("l=$(ls -Zd ").append(q(path)).append("); [ \"${l%% *}\" = ").append(q(pairs[i+1])).append(" ]\n");}
  File actual=new File(job,payload.getString("tar")+".actual");
  shell.append("(cd ").append(q(destination)).append("; find . \\( -path ./lib -o -path ./cache -o -path ./code_cache \\) -prune -o -print0) | LC_ALL=C sort -z > ").append(q(actual.getPath())).append("\nLC_ALL=C sort -z ").append(q(expected.getPath())).append(" | cmp - ").append(q(actual.getPath())).append("\n");
  return shell.toString();
 }

 String extract(File job,JSONObject payload,String destination)throws Exception{
  return "tar --numeric-owner -xpf "+q(new File(job,payload.getString("tar")).getPath())+" -C "+q(destination)+"\n(cd "+q(destination)+"; while IFS= read -r -d '' path && IFS= read -r -d '' label; do chcon -h \"$label\" \"$path\" || exit 1; done < "+q(new File(job,payload.getString("labels")).getPath())+")\n";
 }

 String replace(File job,JSONObject manifest)throws Exception{
  StringBuilder shell=new StringBuilder();JSONArray apps=manifest.getJSONArray("apps");
  for(int i=0;i<apps.length();i++){
   JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");shell.append("am force-stop --user 0 ").append(q(pkg)).append("\n");JSONArray payloads=app.getJSONArray("payloads");
   for(int j=0;j<payloads.length();j++){
    JSONObject payload=payloads.getJSONObject(j);String destination=data(pkg,payload.getString("domain"));
    shell.append("[ -d ").append(q(destination)).append(" ]\n[ ! -L ").append(q(destination)).append(" ]\n[ \"$(stat -c %u ").append(q(destination)).append(")\" = ").append(app.getInt("uid")).append(" ]\n");
    // Only children of a fixed installed-app directory are removed; keep its inode and fscrypt policy.
    shell.append("inode=$(stat -c %i ").append(q(destination)).append(")\nfind ").append(q(destination)).append(" -mindepth 1 -maxdepth 1 ! -name lib ! -name cache ! -name code_cache -exec rm -rf -- {} +\n");
    shell.append(extract(job,payload,destination)).append("[ \"$(stat -c %i ").append(q(destination)).append(")\" = \"$inode\" ]\n").append(verify(job,payload,destination,app.getInt("uid"),true));
    shell.append("echo ").append(q(pkg+" "+payload.getString("domain")+": contents, uid/gid, modes, labels, directory inode and exact path set verified")).append("\n");
   }
  }return shell.toString();
 }

 void preflight(File job,JSONObject manifest)throws Exception{
  compatible(manifest);String root="/data/local/tmp/raven-live-stage-"+UUID.randomUUID();StringBuilder shell=new StringBuilder("mkdir -m 700 "+q(root)+"\n");JSONArray apps=manifest.getJSONArray("apps");
  for(int i=0;i<apps.length();i++){
   JSONObject app=apps.getJSONObject(i);JSONArray payloads=app.getJSONArray("payloads");
   for(int j=0;j<payloads.length();j++){
    JSONObject payload=payloads.getJSONObject(j);String dst=root+"/app"+i+"-"+payload.getString("domain");
    // Validate live headers/contexts before root extraction. No existing data is changed here.
    verify(job,payload,data(app.getString("package"),payload.getString("domain")),app.getInt("uid"),true);
    shell.append("mkdir -m 700 ").append(q(dst)).append("\n").append(extract(job,payload,dst));
    // Label comparison for staging uses saved labels; the directory has not been created yet.
    shell.append(verify(job,payload,dst,app.getInt("uid"),false));
   }
  }
  File runner=engine.job();engine.run(runner,shell+"echo 'Staging verification passed'\nrm -rf -- "+q(root)+"\n",false);
 }

 String state(File control)throws Exception{File f=new File(control,"state");return f.exists()?BackupEngine.read(f,100).trim():"prepared";}
 String journal(File control,String value){String uid=""+android.os.Process.myUid();return "printf '%s\\n' "+q(value)+" > "+q(new File(control,"state").getPath())+"\nchown "+uid+":"+uid+" "+q(new File(control,"state").getPath())+"\nsync\n";}
 JSONObject pending()throws Exception{return pointer.exists()?new JSONObject(BackupEngine.read(pointer,1024*1024)):null;}
 void clearPending(File control)throws Exception{
  String state=state(control);if(state.equals("committed")||state.equals("rolled-back")||state.equals("recovered")){
   if(pointer.exists()&&!pointer.delete())throw new IOException("Cannot clear completed recovery journal");
   java.io.FileDescriptor directory=android.system.Os.open(pointer.getParent(),android.system.OsConstants.O_RDONLY,0);try{android.system.Os.fsync(directory);}finally{android.system.Os.close(directory);}
  }
 }

 String restore(Uri uri,boolean injectFailure)throws Exception{synchronized(LiveRestore.class){return restoreInternal(prepare(uri),injectFailure);}}
 String restoreInternal(Plan plan,boolean injectFailure)throws Exception{
  if(!testing&&!enabled())throw new IOException("Live restore is not verified on this firmware");
  JSONObject previous=pending();if(previous!=null)throw new IOException("An earlier restore needs recovery. Use Recover rollback first.");
  File incoming=plan.job;JSONObject manifest=plan.manifest;
  preflight(incoming,manifest);
  List<String> packages=new ArrayList<>();JSONArray apps=manifest.getJSONArray("apps");for(int i=0;i<apps.length();i++)packages.add(apps.getJSONObject(i).getString("package"));
  File rollback=engine.job();JSONObject snapshot=engine.archive(packages,rollback,false);
  int rollbackFiles=0;for(int i=0;i<snapshot.getJSONArray("apps").length();i++){JSONArray payloads=snapshot.getJSONArray("apps").getJSONObject(i).getJSONArray("payloads");for(int j=0;j<payloads.length();j++)for(TarGuard.Record entry:TarGuard.records(new File(rollback,payloads.getJSONObject(j).getString("tar"))))if(!entry.directory)rollbackFiles++;}
  // Re-import the rollback ZIP to check manifest, CRC and hashes before any live deletion.
  File zip=new File(rollback,"rollback.zip");engine.zip(rollback,snapshot,new FileOutputStream(zip));try(RandomAccessFile file=new RandomAccessFile(zip,"rw")){file.getFD().sync();}
  File verifiedRollback=engine.job();try(InputStream in=new FileInputStream(zip)){snapshot=engine.unpack(in,verifiedRollback);}preflight(verifiedRollback,snapshot);
  String exported=keepRollback?exportRollback(zip):"temporary recovery copy";File control=engine.job();JSONObject record=new JSONObject().put("control",control.getPath()).put("rollback",verifiedRollback.getPath()).put("zip",zip.getPath()).put("exported",exported).put("keepRollback",keepRollback).put("incoming",incoming.getPath()).put("rollbackRegularFiles",rollbackFiles).put("manifest",snapshot);
  durable(new File(engine.c.getFilesDir(),"live-last.json"),record.toString(2));
  durable(pointer,record.toString(2));durable(new File(control,"state"),"prepared");
  String undo=replace(verifiedRollback,snapshot),apply=replace(incoming,manifest);
  StringBuilder shell=new StringBuilder();
  shell.append("rollback_data() {\nset -e\n").append(undo).append("}\n");
  String done=q(new File(control,"done").getPath()),log=q(new File(control,"root.log").getPath()),uid=""+android.os.Process.myUid();
  shell.append("failure() { original=$?; trap - EXIT; set +e; echo 'Restore failed; applying rollback'; (rollback_data); recovery=$?; if [ \"$recovery\" = 0 ]; then\n").append(journal(control,"rolled-back")).append("else\n").append(journal(control,"recovery-required")).append("fi\nprintf '%s' \"$original\" > ").append(done).append("\nchown ").append(uid).append(":").append(uid).append(" ").append(done).append(" ").append(log).append("\nexit \"$original\"; }\n");
  shell.append("printf '%s' $$ > ").append(q(new File(control,"pid").getPath())).append("\nchown ").append(uid).append(":").append(uid).append(" ").append(q(new File(control,"pid").getPath())).append("\ntrap failure EXIT\n").append(journal(control,"applying"));
  // Failure injection runs after the first app has been fully restored, exercising cross-app rollback.
  if(injectFailure){JSONObject one=new JSONObject(manifest.toString());JSONArray first=new JSONArray();first.put(apps.getJSONObject(0));one.put("apps",first);shell.append(replace(incoming,one)).append("echo 'Injected failure after first app'\nexit 77\n");}else shell.append(apply);
  shell.append(journal(control,"data-verified")).append("trap finish EXIT\necho 'Live data and metadata verified'\n");
  try{engine.run(control,shell.toString(),false);}catch(Exception e){String s=state(control);if(s.equals("rolled-back")){clearPending(control);releaseCopies(record);return "Restore failed and rollback verified. Rollback ZIP retained: "+exported+"\n"+BackupEngine.read(new File(control,"root.log"),1024*1024);}throw e;}
  if(simulateInterruption)throw new IOException("Simulated app interruption after data verification; durable recovery journal retained");
  String launchResult="Apps remain stopped; open them when ready.";
  try{if(launchAfter)launchResult=launchRestored(packages);durable(new File(control,"state"),"committed");clearPending(control);releaseCopies(record);}catch(Exception e){if(pointer.exists())recover();throw new IOException("Restore completion failed: "+e.getMessage());}
  return "Restored "+packages.size()+" apps. File contents, ownership, permissions and security labels verified.\n"+launchResult+"\n"+(keepRollback?"Rollback ZIP retained: "+exported:"Temporary recovery copies removed after success.");
 }

 void verifyLaunch(List<String> packages)throws Exception{verifyLaunch(packages,false);}
 String launchRestored(List<String> packages)throws Exception {
  if(testing&&packages.equals(FIXTURES)){verifyLaunch(packages);return "Both fixture apps launched and read restored data.";}
  StringBuilder results=new StringBuilder();
  for(String pkg:packages){Intent intent=engine.pm.getLaunchIntentForPackage(pkg);if(intent==null){results.append(pkg).append(": no launcher activity; files restored.\n");continue;}
   try{intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);engine.c.startActivity(intent);results.append(pkg).append(": opened; confirm its data in the app.\n");}
   catch(Exception e){results.append(pkg).append(": files restored; could not open automatically.\n");}
  }return results.toString().trim();
 }
 void releaseCopies(JSONObject record)throws Exception {
  String base=engine.c.getFilesDir().getCanonicalPath()+File.separator;
  for(String name:Arrays.asList("rollback","incoming")){if(!record.has(name))continue;File directory=new File(record.getString(name));if(!directory.getCanonicalPath().startsWith(base))throw new IOException("Invalid recovery cleanup path");engine.clean(directory);}
  File zip=new File(record.getString("zip"));if(!zip.getCanonicalPath().startsWith(base))throw new IOException("Invalid recovery cleanup ZIP");engine.clean(zip.getParentFile());
  if(!record.optBoolean("keepRollback",true)&&zip.exists()&&!zip.delete())throw new IOException("Unable to remove completed temporary rollback ZIP");
 }
 String exportRollback(File zip)throws Exception{
  if(!Environment.isExternalStorageManager())throw new IOException("Internal storage access required to retain an accessible rollback ZIP");
  File directory=new File(Environment.getExternalStorageDirectory(),"RavenBackupRollbacks");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create rollback folder");
  File exported=new File(directory,"rollback-"+UUID.randomUUID()+".zip");
  try(InputStream in=new FileInputStream(zip);FileOutputStream out=new FileOutputStream(exported)){BackupEngine.copy(in,out,BackupEngine.MAX);out.getFD().sync();}
  if(!BackupEngine.hash(exported).equals(BackupEngine.hash(zip)))throw new IOException("Exported rollback ZIP checksum mismatch");return exported.getPath();
 }

 String safetyTests()throws Exception{
  File job=engine.job();JSONObject m;try(InputStream in=new FileInputStream(new File(Environment.getExternalStorageDirectory(),"raven-live-original.zip"))){m=engine.unpack(in,job);}compatible(m);
  StringBuilder evidence=new StringBuilder("Valid original archive passed identity checks.\n");
  for(String kind:Arrays.asList("firmware","package","uid","version","signers")){
   JSONObject bad=new JSONObject(m.toString());JSONObject app=bad.getJSONArray("apps").getJSONObject(0);
   if(kind.equals("firmware"))bad.put("fingerprint","different-firmware");
   if(kind.equals("package"))app.put("package","com.example.unapproved");
   if(kind.equals("uid"))app.put("uid",app.getInt("uid")+1);
   if(kind.equals("version"))app.put("version",app.getLong("version")+1);
   if(kind.equals("signers"))app.put("signers",new JSONArray().put("wrong-signing-key"));
   boolean rejected=false;try{compatible(bad);}catch(IOException|SecurityException expected){rejected=true;}
   if(!rejected)throw new IOException("Accepted invalid "+kind);evidence.append("PASS rejected ").append(kind).append(" mismatch before live writes.\n");
  }
  JSONObject app=m.getJSONArray("apps").getJSONObject(0),payload=app.getJSONArray("payloads").getJSONObject(0);String destination=data(app.getString("package"),payload.getString("domain"));
  for(String kind:Arrays.asList("owner","special-mode")){
   File tar=new File(job,"hostile-"+kind+".tar");try(InputStream in=new FileInputStream(new File(job,payload.getString("tar")));OutputStream out=new FileOutputStream(tar)){BackupEngine.copy(in,out,BackupEngine.MAX);}
   try(RandomAccessFile file=new RandomAccessFile(tar,"rw")){byte[] header=new byte[512];file.readFully(header);int start=kind.equals("owner")?108:100;byte[] value=(kind.equals("owner")?"0000000":"0007777").getBytes("US-ASCII");System.arraycopy(value,0,header,start,value.length);Arrays.fill(header,148,156,(byte)32);int checksum=0;for(byte b:header)checksum+=b&255;byte[] sum=String.format(Locale.ROOT,"%06o",checksum).getBytes("US-ASCII");System.arraycopy(sum,0,header,148,sum.length);header[154]=0;header[155]=32;file.seek(0);file.write(header);}
   JSONObject bad=new JSONObject(payload.toString()).put("tar",tar.getName());boolean rejected=false;try{verify(job,bad,destination,app.getInt("uid"),true);}catch(IOException expected){rejected=true;}if(!rejected)throw new IOException("Accepted invalid "+kind);evidence.append("PASS rejected ").append(kind).append(" before live writes.\n");
  }
  File labels=new File(job,"hostile.labels");String pairs=labels(job,payload);String original=pairs.split("\0",-1)[1];BackupEngine.write(labels,pairs.replace(original,"u:object_r:system_file:s0"));JSONObject bad=new JSONObject(payload.toString()).put("labels",labels.getName());boolean rejected=false;try{verify(job,bad,destination,app.getInt("uid"),true);}catch(IOException expected){rejected=true;}if(!rejected)throw new IOException("Accepted invalid SELinux category");evidence.append("PASS rejected incompatible SELinux labels before live writes.\n");
  BackupEngine.write(new File(Environment.getExternalStorageDirectory(),"raven-live-safety-tests.txt"),evidence.toString());return evidence.toString();
 }
 String generalBackupTest()throws Exception {
  String pkg="dev.raven.backupfixture.generic";File seed=engine.job();engine.run(seed,"am force-stop --user 0 "+q(pkg)+"\nam start -W -n "+q(pkg+"/dev.raven.backupfixture.Seed")+" --es mode seed\nsleep 1\n",false);
  File backup=engine.job();JSONObject m=engine.archive(Arrays.asList(pkg),backup,false);File zip=new File(Environment.getExternalStorageDirectory(),"raven-general-original.zip");engine.zip(backup,m,new FileOutputStream(zip));return "Generic test backup created; old UID="+m.getJSONArray("apps").getJSONObject(0).getInt("uid")+". Reinstall only this synthetic package to test a fresh installation.";
 }
 String versionHistoryTest(Uri tree)throws Exception{synchronized(LiveRestore.class){testing=true;try{
  if(tree==null)throw new IOException("Choose a test backup folder");String generic="dev.raven.backupfixture.generic";List<String> one=Arrays.asList(generic);StringBuilder evidence=new StringBuilder();
  File seedJob=engine.job();engine.run(seedJob,"am force-stop --user 0 "+q(generic)+"\nam start -W -n "+q(generic+"/dev.raven.backupfixture.Seed")+" --es mode seed\nsleep 1\n",false);
  engine.backup(one,tree);Uri original=engine.lastBackupUri;
  File mutateJob=engine.job();engine.run(mutateJob,"am force-stop --user 0 "+q(generic)+"\nam start -W -n "+q(generic+"/dev.raven.backupfixture.Seed")+" --es mode mutate\nsleep 1\n",false);
  engine.backup(one,tree);Uri changed=engine.lastBackupUri;if(original.equals(changed))throw new IOException("Backup versions collided");
  restore(prepare(original),false,false);verifyLaunch(one,false);restore(prepare(changed),false,false);verifyLaunch(one,true);restore(prepare(original),false,false);verifyLaunch(one,false);
  evidence.append("PASS two distinct ZIP versions retained for one app.\nPASS restored original and changed versions independently; app read both expected states.\n");
  seed("seed");File multiJob=engine.job();JSONObject multiManifest=engine.archive(FIXTURES,multiJob,false);File multiZip=new File(multiJob,"legacy-batch.zip");engine.zip(multiJob,multiManifest,new FileOutputStream(multiZip));Uri multi=Uri.fromFile(multiZip);seed("mutate");File before=engine.job();JSONObject unchanged=engine.archive(Arrays.asList(FIXTURES.get(0)),before,false);
  Plan partial=importArchive(multi);prepare(partial,new TreeSet<>(Arrays.asList(FIXTURES.get(1))));restore(partial,false,false);verifyLaunch(Arrays.asList(FIXTURES.get(1)),false);
  JSONObject first=unchanged.getJSONArray("apps").getJSONObject(0);JSONArray payloads=first.getJSONArray("payloads");StringBuilder check=new StringBuilder();for(int i=0;i<payloads.length();i++){JSONObject p=payloads.getJSONObject(i);check.append(verify(before,p,data(first.getString("package"),p.getString("domain")),first.getInt("uid"),true));}engine.run(engine.job(),check.toString()+"echo 'Unselected app unchanged'\n",false);verifyLaunch(Arrays.asList(FIXTURES.get(0)),true);
  restore(prepare(multi),false,true);evidence.append("PASS selected only the second app from a two-app ZIP.\nPASS unselected first app retained its changed contents, owners, modes, labels and exact path set.\nPASS final batch restore and both app reads.\n");
  long batchStarted=System.currentTimeMillis();engine.backup(FIXTURES,tree);int separate=0;for(BackupCatalog.Entry e:BackupCatalog.list(engine.c,tree))if(e.manifest!=null&&e.manifest.getLong("created")>=batchStarted){JSONArray savedApps=e.manifest.getJSONArray("apps");if(savedApps.length()!=1)throw new IOException("New batch backup contains multiple apps");if(FIXTURES.contains(savedApps.getJSONObject(0).getString("package")))separate++;}if(separate!=2)throw new IOException("Expected two separate per-app backups");evidence.append("PASS batch selection produced two separate app-folder ZIPs discoverable by the recursive catalog.\n");
  List<BackupCatalog.Entry> catalog=BackupCatalog.list(engine.c,tree);boolean foundOriginal=false,foundChanged=false;for(BackupCatalog.Entry entry:catalog){if(entry.uri.equals(original))foundOriginal=true;if(entry.uri.equals(changed))foundChanged=true;}if(!foundOriginal||!foundChanged)throw new IOException("History catalog lost a version");evidence.append("PASS both versions remain discoverable in the backup catalog after restores.\n");
  BackupEngine.write(new File(Environment.getExternalStorageDirectory(),"raven-version-history-test.txt"),evidence.toString());engine.clean(before);return evidence.toString();
 }finally{testing=false;}}}
 String labelProfileTest(Uri tree)throws Exception{synchronized(LiveRestore.class){testing=true;try{
  String pkg="dev.raven.backupfixture.generic";List<String> apps=Arrays.asList(pkg);int uid=engine.info(pkg).getInt("uid");
  File seedJob=engine.job();engine.run(seedJob,"am force-stop --user 0 "+q(pkg)+"\nam start -W -n "+q(pkg+"/dev.raven.backupfixture.Seed")+" --es mode seed\nsleep 1\n",false);
  String profile=data(pkg,"ce")+"/app_.gpg.classloader/oat/synthetic.jar.prof";String rootContext=RootBridge.call("ls -Zd "+q(data(pkg,"ce"))).trim().split("\\s+")[0];
  engine.run(engine.job(),"am force-stop --user 0 "+q(pkg)+"\nmkdir -p "+q(new File(profile).getParent())+"\nprintf 'synthetic-profile' > "+q(profile)+"\nchown -R "+uid+":"+uid+" "+q(data(pkg,"ce")+"/app_.gpg.classloader")+"\nchcon -R "+q(rootContext)+" "+q(data(pkg,"ce")+"/app_.gpg.classloader")+"\nchmod 600 "+q(profile)+"\nchcon -h u:object_r:app_data_file:s0 "+q(profile)+"\n",false);
  File saved=engine.job();JSONObject manifest=engine.archive(apps,saved,false);File zip=new File(saved,"profile.zip");engine.zip(saved,manifest,new FileOutputStream(zip));
  engine.run(engine.job(),"printf 'changed' > "+q(profile)+"\n",false);restore(prepare(Uri.fromFile(zip)),false,false);verifyLaunch(apps,false);
  String output="PASS category-free ART profile label retained through verified live restore; ordinary data and app reads verified.";BackupEngine.write(new File(Environment.getExternalStorageDirectory(),"raven-profile-label-test.txt"),output);return output;
 }finally{testing=false;}}}
 String generalRestoreTest()throws Exception{synchronized(LiveRestore.class){testing=true;try{
  String pkg="dev.raven.backupfixture.generic";File imported=engine.job();JSONObject old;File zip=new File(Environment.getExternalStorageDirectory(),"raven-general-original.zip");try(InputStream in=new FileInputStream(zip)){old=engine.unpack(in,imported);}int before=old.getJSONArray("apps").getJSONObject(0).getInt("uid"),now=engine.info(pkg).getInt("uid");if(before==now)throw new IOException("Test needs an actual changed UID after reinstall");
  Plan plan=prepare(Uri.fromFile(zip));String result=restore(plan,false,true);File check=engine.job();engine.run(check,"n=0\nwhile [ ! -f "+q(data(pkg,"ce")+"/cache/launch-report.json")+" ]; do n=$((n+1)); [ \"$n\" -lt 40 ]; sleep 0.25; done\ncat "+q(data(pkg,"ce")+"/cache/launch-report.json")+"\ngrep -q '\"success\":true' "+q(data(pkg,"ce")+"/cache/launch-report.json")+"\nsleep 1\n[ -n \"$(pidof "+q(pkg)+")\" ]\n",false);
  JSONObject record=new JSONObject(BackupEngine.read(new File(engine.c.getFilesDir(),"live-last.json"),1024*1024));if(record.getInt("rollbackRegularFiles")!=0)throw new IOException("Fresh-install test had nonblank rollback data");if(new File(record.getString("zip")).exists()||pointer.exists())throw new IOException("Temporary rollback ZIP or pending journal was not removed");
  if(!engine.c.getSharedPreferences("live-restore",0).edit().putString("general-verified",Build.FINGERPRINT).commit())throw new IOException("Cannot enable general live restore");
  String evidence="PASS restored a newly installed app outside the old fixture allowlist.\nPASS actual Android UID remap "+before+" -> "+now+", with adapted app SELinux categories.\nPASS original files, ownership, modes, labels, exact path set and root inodes verified in CE/DE.\nPASS normal generic app launch; fixture independently read and verified original data.\nPASS fresh target contained no backed-up regular files before restore.\nPASS temporary rollback ZIP and staging payloads removed after success; no exported ZIP requested.\nLive restore enabled for supported installed user apps.\n"+result;BackupEngine.write(new File(Environment.getExternalStorageDirectory(),"raven-general-restore-test.txt"),evidence);return evidence;
 }finally{testing=false;}}}

 String selfTest()throws Exception{synchronized(LiveRestore.class){
  testing=true;engine.c.getSharedPreferences("live-restore",0).edit().remove("verified").commit();StringBuilder evidence=new StringBuilder();
  try{
   JSONObject existing=pending();if(existing!=null)evidence.append(recover()).append('\n');
   seed("seed");File original=engine.job();JSONObject manifest=engine.archive(FIXTURES,original,false);File zip=new File(Environment.getExternalStorageDirectory(),"raven-live-original.zip");engine.zip(original,manifest,new FileOutputStream(zip));
   seed("mutate");verifyLaunch(FIXTURES,true);evidence.append("Mutated CE/DE files, preferences, permission bits and extra-file presence verified by both launched apps.\n");
   String failure=restore(Uri.fromFile(zip),true);if(!failure.startsWith("Restore failed and rollback verified"))throw new IOException("Injected failure did not roll back");evidence.append(failure).append('\n');verifyLaunch(FIXTURES,true);evidence.append("Both apps launched and read the mutated rollback state after injected failure.\n");
   simulateInterruption=true;try{restore(Uri.fromFile(zip),false);throw new IOException("Interruption test did not interrupt");}catch(IOException interrupted){if(pending()==null||!state(new File(pending().getString("control"))).equals("data-verified"))throw interrupted;evidence.append(interrupted.getMessage()).append('\n');}finally{simulateInterruption=false;}
   evidence.append(new LiveRestore(engine.c).recover()).append('\n');verifyLaunch(FIXTURES,true);evidence.append("Fresh restore engine recovered from the durable journal, and both apps launched with the rollback values.\n");
   evidence.append(restore(Uri.fromFile(zip),false)).append('\n');
   if(!engine.c.getSharedPreferences("live-restore",0).edit().putString("verified",verificationKey()).commit())throw new IOException("Cannot persist successful verification gate");
   evidence.append("Fixture regression suite passed; general restore is enabled by the fresh-install test.\n");
   BackupEngine.write(new File(Environment.getExternalStorageDirectory(),"raven-live-selftest.txt"),evidence.toString());return evidence.toString();
  }finally{testing=false;}
 }}
 void seed(String mode)throws Exception{
  File job=engine.job();StringBuilder shell=new StringBuilder();
  for(String pkg:FIXTURES){shell.append("am force-stop --user 0 ").append(q(pkg)).append("\nrm -f -- ").append(q("/data/user/0/"+pkg+"/cache/test-state.txt")).append("\nam start -W -n ").append(q(pkg+"/dev.raven.backupfixture.Seed")).append(" --es mode ").append(q(mode)).append("\nn=0\nwhile [ \"$(cat ").append(q("/data/user/0/"+pkg+"/cache/test-state.txt")).append(" 2>/dev/null)\" != ").append(q(mode)).append(" ]; do n=$((n+1)); [ \"$n\" -lt 40 ]; sleep 0.25; done\n");}
  engine.run(job,shell.toString(),false);
 }
 void verifyLaunch(List<String> packages,boolean mutated)throws Exception{
  for(String pkg:packages){File runner=engine.job();String report="/data/user/0/"+pkg+"/cache/launch-report.json";engine.run(runner,"am force-stop --user 0 "+q(pkg)+"\nrm -f -- "+q(report)+"\n",false);
   Intent intent=engine.pm.getLaunchIntentForPackage(pkg);if(intent==null)throw new IOException("No launcher activity: "+pkg);engine.run(engine.job(),"am start -W --user 0 -n "+q(intent.getComponent().flattenToString())+(mutated?" --es expect mutated":"")+"\n",false);
   File check=engine.job();String body="n=0\nwhile [ ! -f "+q(report)+" ]; do n=$((n+1)); [ \"$n\" -lt 40 ]; sleep 0.25; done\ncat "+q(report)+"\ngrep -q '\"success\":true' "+q(report)+"\nsleep 1\n[ -n \"$(pidof "+q(pkg)+")\" ]\necho "+q(pkg+": launcher process and application data verified")+"\n";
   engine.run(check,body,false);
  }
 }

 String recover()throws Exception{synchronized(LiveRestore.class){return recoverInternal();}}
 String recoverInternal()throws Exception{
  JSONObject record=pending();if(record==null)return "No pending restore requires recovery.";File control=new File(record.getString("control"));
  // Private paths originate solely from our durable journal, never from imported archive names.
  String base=engine.c.getFilesDir().getCanonicalPath()+File.separator;
  for(String field:new String[]{"control","rollback","zip"})if(!new File(record.getString(field)).getCanonicalPath().startsWith(base))throw new IOException("Invalid recovery path");
  String prior=state(control);if(prior.equals("committed")||prior.equals("rolled-back")||prior.equals("recovered")){clearPending(control);return "Previous restore already completed; no recovery writes required.";}
  File pid=new File(control,"pid");if(!new File(control,"done").exists()&&pid.exists()){
   String number=BackupEngine.read(pid,100).trim();if(!number.matches("[0-9]+"))throw new IOException("Invalid running-job PID");
   String running=RootBridge.call("sh -c "+q("if kill -0 "+number+" 2>/dev/null; then echo running; fi"));if(running.contains("running"))throw new IOException("Root restore is still running; wait before recovery");
  }
  JSONObject manifest=record.getJSONObject("manifest");compatible(manifest);File rollback=new File(record.getString("rollback"));
  // Revalidate the durable rollback ZIP and do not rely on staging left from a prior process.
  File imported=engine.job();try(InputStream in=new FileInputStream(record.getString("zip"))){manifest=engine.unpack(in,imported);}preflight(imported,manifest);
  File runner=engine.job();engine.run(runner,replace(imported,manifest)+journal(control,"recovered")+"echo 'Recovery data and metadata verified'\n",false);clearPending(control);engine.clean(imported);releaseCopies(record);return "Rollback recovery verified; apps remain stopped. "+(record.optBoolean("keepRollback",true)?"Rollback ZIP retained.":"Temporary recovery copies removed.");
 }
}
