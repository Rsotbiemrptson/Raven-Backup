package dev.raven.backup;

import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.provider.DocumentsContract;
import java.io.*;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.zip.*;
import org.json.*;

/** Primary shared-storage app files, separate from private CE/DE payloads. */
final class ExternalData {
 static final String ENTRY="external-data.tar", FOOTER="external-data.json";
 static final int STREAM_FORMAT=4, SCOPED_FORMAT=5;
 static boolean streamed(int format){return format==STREAM_FORMAT||format==SCOPED_FORMAT;}
 private static final long RESERVE=64L*1024*1024;
 private static final String MEDIA="/data/media/0/";
 private static final String STAGES=MEDIA+"Android/data/dev.raven.backup/files/";
 private static String q(String value){return BackupEngine.q(value);}

 static JSONObject metadata(JSONObject manifest)throws Exception {
  int format=manifest.getInt("format");
  if(format!=3&&!streamed(format)){
   if(manifest.has("external"))throw new IOException("Unexpected external data metadata");
   return null;
  }
  if(manifest.getJSONArray("apps").length()!=1)throw new IOException("External backup must contain one app");
  JSONObject metadata=validateMetadata(manifest.getJSONObject("external"));
  if(format==SCOPED_FORMAT){JSONArray scopes=ExternalScopes.validate(metadata.getJSONArray("scopes"));JSONArray declared=ExternalScopes.validate(manifest.getJSONArray("externalScopes"));if(scopes.length()!=declared.length())throw new IOException("External scopes differ from manifest");for(int i=0;i<scopes.length();i++)for(String field:new String[]{"id","kind","volume"})if(!scopes.getJSONObject(i).getString(field).equals(declared.getJSONObject(i).getString(field)))throw new IOException("External scope changed");}
  else if(metadata.has("scopes"))throw new IOException("Scoped external payload requires format 5");
  return metadata;
 }
 private static JSONObject validateMetadata(JSONObject external)throws Exception {
  long bytes=external.getLong("bytes");
  if(bytes<1024||bytes>ExternalTar.LIMIT||bytes%512!=0||!external.getString("sha256").matches("[a-f0-9]{64}"))
   throw new IOException("Invalid external backup metadata");
  if(external.has("scopes")){JSONArray scopes=ExternalScopes.validate(external.getJSONArray("scopes"));long size=1536;for(int i=0;i<scopes.length();i++)size=Math.addExact(size,scopes.getJSONObject(i).getLong("bytes"));if(size!=bytes)throw new IOException("External scope sizes differ from total");}
  return external;
 }
 static void read(InputStream in,JSONObject external,File stage)throws Exception {
  ExternalTar.read(in,external.getLong("bytes"),external.getString("sha256"),stage==null?null:stage.toPath());
 }
 static JSONObject verifyStream(ZipInputStream in)throws Exception {
  ExternalTar.Result actual=ExternalTar.scan(in,null);
  ZipEntry footer=in.getNextEntry();
  if(footer==null||footer.isDirectory()||!FOOTER.equals(footer.getName()))throw new IOException("External data checksum entry missing");
  ByteArrayOutputStream out=new ByteArrayOutputStream();
  BackupEngine.copy(in,out,65536);
  JSONObject metadata=validateMetadata(new JSONObject(out.toString("UTF-8")));
  actual.verify(metadata.getLong("bytes"),metadata.getString("sha256"));
  return metadata;
 }
 static String source(BackupEngine engine,Uri parent,String pkg)throws Exception {
  String source=appDirectory(pkg)+"/files";
  File probe=engine.job();
  String result=engine.run(probe,ExternalProbe.command(source,
   new File(probe,"external-entries.txt").getPath(),new File(probe,"external-size.txt").getPath()),false);
  long estimate=ExternalProbe.bytes(result);
  if(estimate<0)return null;
  if(estimate>ExternalTar.LIMIT)throw new IOException("External files exceed the 512 GiB limit");
  if(localFolder(parent).getUsableSpace()<estimate+RESERVE)throw new IOException("The backup drive needs room for the finished ZIP");
  return source;
 }
 static void writeStream(BackupEngine engine,File job,String source,ZipOutputStream zip)throws Exception {
  File pipe=new File(job,"external.pipe");
  Os.mkfifo(pipe.getPath(),0600);
  try(ParcelFileDescriptor keepOpen=ParcelFileDescriptor.open(pipe,ParcelFileDescriptor.MODE_READ_WRITE);
      InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.open(pipe,ParcelFileDescriptor.MODE_READ_ONLY))){
   // Keep the reader open until the OEM command finishes, even if it fails before opening the pipe.
   FutureTask<Void> producer=new FutureTask<>(()->{
    try{
     engine.runLong(engine.job(),"[ -d "+q(source)+" ]\n[ ! -L "+q(source)+" ]\n[ -p "+q(pipe.getPath())+" ]\n"
      +engine.tarFunction()+"archive_tar -cpf - -C "+q(source)+" . > "+q(pipe.getPath())+"\n");
     return null;
    }finally{keepOpen.close();}
   });
   try{
    Thread worker=new Thread(producer,"Raven external backup");
    worker.setDaemon(true);
    worker.start();
    // Keep the ZIP compression level fixed across entries on Android.
    zip.putNextEntry(new ZipEntry(ENTRY));
    BufferedOutputStream buffered=new BufferedOutputStream(zip,65536);
    ExternalTar.Result result=ExternalTar.scan(new BufferedInputStream(input,65536),buffered);
    try{producer.get();}catch(ExecutionException e){throw new IOException("Could not read external app data",e.getCause());}
    buffered.flush();
    zip.closeEntry();
    zip.putNextEntry(new ZipEntry(FOOTER));
    JSONObject footer=new JSONObject().put("bytes",result.bytes).put("sha256",result.sha256);
    zip.write(footer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    zip.closeEntry();
   }finally{
    // Unlink while a reader is still open so a late producer cannot block on opening the path.
    Files.deleteIfExists(pipe.toPath());
   }
  }finally{Files.deleteIfExists(pipe.toPath());}
 }
 static final class Counted extends FilterOutputStream {
  long bytes;Counted(OutputStream out){super(out);}
  public void write(int value)throws IOException{if(++bytes>ExternalTar.LIMIT)throw new IOException("Combined external TAR exceeds 512 GiB");out.write(value);}
  public void write(byte[] b,int o,int n)throws IOException{bytes=Math.addExact(bytes,n);if(bytes>ExternalTar.LIMIT)throw new IOException("Combined external TAR exceeds 512 GiB");out.write(b,o,n);}
 }
 static void writeScopes(BackupEngine engine,File job,String pkg,JSONArray scopes,ZipOutputStream zip)throws Exception {
  ExternalScopes.validate(scopes);zip.putNextEntry(new ZipEntry(ENTRY));
  java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
  Counted count=new Counted(new java.security.DigestOutputStream(new BufferedOutputStream(zip,65536),digest));
  ScopedTar.header(count,"",true,0);JSONArray written=new JSONArray();int entries=1;
  for(int i=0;i<scopes.length();i++){
   JSONObject scope=scopes.getJSONObject(i);long start=count.bytes;File pipe=new File(job,"external-"+i+".pipe");Os.mkfifo(pipe.getPath(),0600);
   String source=ExternalScopes.destination(scope.getString("volume"),scope.getString("kind"),pkg);
   try(ParcelFileDescriptor keep=ParcelFileDescriptor.open(pipe,ParcelFileDescriptor.MODE_READ_WRITE);InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.open(pipe,ParcelFileDescriptor.MODE_READ_ONLY))){
    FutureTask<Void> producer=new FutureTask<>(()->{try{engine.runLong(engine.job(),"[ -d "+q(source)+" ]\n"+"[ ! -L "+q(source)+" ]\n"+engine.tarFunction()+"archive_tar -cpf - -C "+q(source)+" . > "+q(pipe.getPath())+"\n");return null;}finally{keep.close();}});
    Thread worker=new Thread(producer,"Raven scoped external backup");worker.setDaemon(true);worker.start();
    try{ExternalTar.Result appended=ExternalTar.appendScope(new BufferedInputStream(input,65536),scope.getString("id"),count);entries=Math.addExact(entries,appended.entries);if(entries>250000)throw new IOException("Combined external archive exceeds 250000 paths");try{producer.get();}catch(ExecutionException failure){throw new IOException("Could not read "+scope.getString("kind")+" on "+scope.getString("volume"),failure.getCause());}}finally{Files.deleteIfExists(pipe.toPath());}
   }finally{Files.deleteIfExists(pipe.toPath());}
   written.put(new JSONObject(scope.toString()).put("bytes",count.bytes-start));
  }
  ScopedTar.finish(count);count.flush();zip.closeEntry();zip.putNextEntry(new ZipEntry(FOOTER));
  JSONObject footer=new JSONObject().put("bytes",count.bytes).put("sha256",BackupEngine.hex(digest.digest())).put("scopes",written);
  zip.write(footer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();
 }
 static void stageScopes(BackupEngine engine,LiveRestore.Plan plan,JSONObject external,java.util.Map<String,java.nio.file.Path> routes)throws Exception{
  IndexedArchive.unchanged(engine,plan);
  if(plan.sourceStamp!=null){IndexedArchive.stageScopes(engine,plan,external,routes);return;}
  try(InputStream raw=engine.c.getContentResolver().openInputStream(plan.source);ZipInputStream zip=new ZipInputStream(new BufferedInputStream(raw,65536))){ZipEntry entry;while((entry=zip.getNextEntry())!=null)if(entry.getName().equals(ENTRY)){ExternalTar.readScopes(zip,external.getLong("bytes"),external.getString("sha256"),routes);zip.closeEntry();IndexedArchive.unchanged(engine,plan);return;}}
  throw new IOException("External data entry missing");
 }
 static File localFolder(Uri parent)throws IOException {
  if(!"com.android.externalstorage.documents".equals(parent.getAuthority()))throw new IOException("Choose an internal-storage or SD-card folder for external data backup");
  String[] id=DocumentsContract.getDocumentId(parent).split(":",2);
  if(id.length!=2||!(id[0].equals("primary")||id[0].matches("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")))throw new IOException("Unsupported backup volume");
  File volume=(id[0].equals("primary")?Environment.getExternalStorageDirectory():new File("/storage",id[0])).getCanonicalFile();
  File folder=new File(volume,id[1]).getCanonicalFile();
  if(!folder.toPath().startsWith(volume.toPath())||!folder.isDirectory())throw new IOException("Backup folder is unavailable");
  return folder;
 }
 static JSONObject transaction(BackupEngine engine,LiveRestore.Plan plan)throws Exception {
  if(metadata(plan.manifest)==null)return null;
  if(plan.manifest.getInt("format")==SCOPED_FORMAT)return ExternalScopes.transaction(engine,plan);
  String pkg=plan.manifest.getJSONArray("apps").getJSONObject(0).getString("package");
  appDirectory(pkg);
  return new JSONObject().put("id",UUID.randomUUID().toString()).put("package",pkg);
 }
 static void stage(BackupEngine engine,LiveRestore.Plan plan,JSONObject transaction)throws Exception {
  JSONObject external=metadata(plan.manifest);
  if(transaction.has("parts")){
   try{java.util.Map<String,java.nio.file.Path> routes=ExternalScopes.stages(engine,transaction,true);stageScopes(engine,plan,external,routes);return;}catch(Exception failure){try{cleanup(engine,transaction);}catch(Exception cleanup){failure.addSuppressed(cleanup);}throw failure;}
  }
  IndexedArchive.unchanged(engine,plan);
  if(plan.source==null)throw new IOException("External backup must be opened from its original ZIP");
  File base=engine.c.getExternalFilesDir(null);
  File expected=new File(Environment.getExternalStorageDirectory(),"Android/data/"+engine.c.getPackageName()+"/files");
  if(base==null||!base.getCanonicalFile().equals(expected.getCanonicalFile()))throw new IOException("Primary external restore staging is unavailable");
  if(base.getUsableSpace()<external.getLong("bytes")+RESERVE)throw new IOException("Free more internal storage to stage the external files before restore");
  String id=id(transaction);
  File stage=new File(base,".raven-stage-"+id);
  if(!stage.mkdir())throw new IOException("Cannot create external staging folder");
  try{
   if(plan.sourceStamp!=null){IndexedArchive.stage(engine,plan,external,stage);return;}
   try(InputStream raw=engine.c.getContentResolver().openInputStream(plan.source)){
    if(raw==null)throw new IOException("Cannot reopen external backup");
    try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(raw,65536))){
     ZipEntry entry;
     while((entry=zip.getNextEntry())!=null){
      if(!entry.getName().equals(ENTRY))continue;
      read(zip,external,stage);zip.closeEntry();IndexedArchive.unchanged(engine,plan);return;
     }
    }
   }
   throw new IOException("External backup payload missing");
  }catch(Exception e){try{cleanup(engine,transaction);}catch(Exception cleanup){e.addSuppressed(cleanup);}throw e;}
 }
 static void validateLinkTargets(BackupEngine engine,LiveRestore.Plan plan,JSONObject transaction)throws Exception{
  if(transaction==null)return;String pkg=plan.manifest.getJSONArray("apps").getJSONObject(0).getString("package");File stage=new File(engine.c.getExternalFilesDir(null),".raven-stage-"+id(transaction));
  if(transaction.has("parts")){stage=null;JSONArray parts=transaction.getJSONArray("parts");for(int i=0;i<parts.length();i++){JSONObject part=parts.getJSONObject(i);if(part.getString("volume").equals("primary")&&part.getString("kind").equals("data"))stage=ExternalScopes.stages(engine,transaction,false).get(part.getString("id")).toFile();}}
  org.json.JSONArray payloads=plan.manifest.getJSONArray("apps").getJSONObject(0).getJSONArray("payloads");
  for(int i=0;i<payloads.length();i++)for(TarGuard.Record record:TarGuard.parse(new File(plan.job,payloads.getJSONObject(i).getString("tar")),false,pkg))if(record.symlink){if(stage==null)throw new IOException("Private symbolic link needs the primary external data scope");java.nio.file.Path target=stage.toPath().resolve(TarGuard.externalRelative(record.linkTarget,pkg)).normalize();if(!target.startsWith(stage.toPath())||!java.nio.file.Files.isRegularFile(target,java.nio.file.LinkOption.NOFOLLOW_LINKS))throw new IOException("Symbolic link target is missing from the staged external backup: "+record.path);}
 }
 private static String appDirectory(String pkg)throws IOException {
  if(!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")||pkg.equals("dev.raven.backup"))throw new IOException("Invalid external destination package");
  return MEDIA+"Android/data/"+pkg;
 }
 private static String id(JSONObject transaction)throws Exception {
  String value=transaction.getString("id");if(!UUID.fromString(value).toString().equals(value))throw new IOException("Invalid external restore identifier");return value;
 }
 static String apply(JSONObject transaction,File control)throws Exception {
  if(transaction==null)return "";
  if(transaction.has("parts"))return ExternalScopes.apply(transaction,control);
  String id=id(transaction),app=q(appDirectory(transaction.getString("package"))),stage=q(STAGES+".raven-stage-"+id);
  String dest=q(appDirectory(transaction.getString("package"))+"/files"),old=q(appDirectory(transaction.getString("package"))+"/.raven-old-"+id);
  String marker=q(new File(control,"external-swap").getPath());
  return "[ ! -L "+app+" ]\nmkdir -p "+app+"\n"
   +"[ -d "+stage+" ]\n[ ! -L "+stage+" ]\n[ ! -e "+old+" ]\n[ ! -L "+old+" ]\n"
   +"[ \"$(stat -c %d "+stage+")\" = \"$(stat -c %d "+app+")\" ]\n"
   +"[ -z \"$(find "+stage+" ! -type f ! -type d -print -quit)\" ]\n"
   +"[ ! -L "+dest+" ]\nif [ -e "+dest+" ]; then [ -d "+dest+" ]; echo present > "+marker+"; else echo absent > "+marker+"; fi\nsync\n"
   +"if [ -d "+dest+" ]; then mv -T -- "+dest+" "+old+"; fi\n"
   +"mv -T -- "+stage+" "+dest+"\nchown -R \"$(stat -c %u:%g "+app+")\" "+dest+"\nrestorecon -RF "+dest+"\nsync\n";
 }
 static String undo(JSONObject transaction,File control)throws Exception {
  if(transaction==null)return "";
  if(transaction.has("parts"))return ExternalScopes.undo(transaction,control);
  String id=id(transaction),app=q(appDirectory(transaction.getString("package")));
  String stage=q(STAGES+".raven-stage-"+id),dest=q(appDirectory(transaction.getString("package"))+"/files"),old=q(appDirectory(transaction.getString("package"))+"/.raven-old-"+id);
  String marker=q(new File(control,"external-swap").getPath());
  return "if [ -f "+marker+" ]; then\n"
   +"[ ! -L "+app+" ]\n[ ! -L "+stage+" ]\n[ ! -L "+dest+" ]\n[ ! -L "+old+" ]\n"
   +"case $(cat "+marker+") in\npresent)\n"
   +"if [ -d "+old+" ]; then\nif [ -d "+dest+" ]; then [ ! -e "+stage+" ]; mv -T -- "+dest+" "+stage+"; fi\nmv -T -- "+old+" "+dest+"\nfi\n[ -d "+dest+" ];;\n"
   +"absent) if [ -d "+dest+" ]; then [ ! -e "+stage+" ]; mv -T -- "+dest+" "+stage+"; fi;;\n"
   +"*) exit 1;;\nesac\nsync\nfi\n";
 }
 static void cleanup(BackupEngine engine,JSONObject transaction)throws Exception {
  if(transaction==null)return;
  if(transaction.has("parts")){ExternalScopes.cleanup(engine,transaction);return;}
  String id=id(transaction),app=appDirectory(transaction.getString("package"));
  engine.runLong(engine.job(),"[ ! -L "+q(app)+" ]\nrm -rf -- "+q(STAGES+".raven-stage-"+id)+" "+q(app+"/.raven-old-"+id)+"\n");
 }
}
