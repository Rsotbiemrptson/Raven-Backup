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
 static final int STREAM_FORMAT=4;
 private static final long RESERVE=64L*1024*1024;
 private static final String MEDIA="/data/media/0/";
 private static final String STAGES=MEDIA+"Android/data/dev.raven.backup/files/";
 private static String q(String value){return BackupEngine.q(value);}

 static JSONObject metadata(JSONObject manifest)throws Exception {
  int format=manifest.getInt("format");
  if(format!=3&&format!=STREAM_FORMAT){
   if(manifest.has("external"))throw new IOException("Unexpected external data metadata");
   return null;
  }
  if(manifest.getJSONArray("apps").length()!=1)throw new IOException("External backup must contain one app");
  return validateMetadata(manifest.getJSONObject("external"));
 }
 private static JSONObject validateMetadata(JSONObject external)throws Exception {
  long bytes=external.getLong("bytes");
  if(bytes<1024||bytes>ExternalTar.LIMIT||bytes%512!=0||!external.getString("sha256").matches("[a-f0-9]{64}"))
   throw new IOException("Invalid external backup metadata");
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
  BackupEngine.copy(in,out,4096);
  JSONObject metadata=validateMetadata(new JSONObject(out.toString("UTF-8")));
  actual.verify(metadata.getLong("bytes"),metadata.getString("sha256"));
  return metadata;
 }
 static String source(Uri parent,String pkg)throws Exception {
  String source=appDirectory(pkg)+"/files";
  String size=RootBridge.call("sh -c "+q("if [ -d "+q(source)+" ]; then du -sk "+q(source)+"; else echo absent; fi")).trim();
  if(size.equals("absent"))return null;
  long estimate=Math.multiplyExact(Long.parseLong(size.split("\\s+")[0]),1024);
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
    zip.setLevel(Deflater.NO_COMPRESSION);
    zip.putNextEntry(new ZipEntry(ENTRY));
    BufferedOutputStream buffered=new BufferedOutputStream(zip,65536);
    ExternalTar.Result result=ExternalTar.scan(new BufferedInputStream(input,65536),buffered);
    try{producer.get();}catch(ExecutionException e){throw new IOException("Could not read external app data",e.getCause());}
    buffered.flush();
    zip.closeEntry();
    zip.setLevel(Deflater.DEFAULT_COMPRESSION);
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
 private static File localFolder(Uri parent)throws IOException {
  if(!"com.android.externalstorage.documents".equals(parent.getAuthority()))throw new IOException("Choose an internal-storage or SD-card folder for external data backup");
  String[] id=DocumentsContract.getDocumentId(parent).split(":",2);
  if(id.length!=2||!(id[0].equals("primary")||id[0].matches("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")))throw new IOException("Unsupported backup volume");
  File volume=(id[0].equals("primary")?Environment.getExternalStorageDirectory():new File("/storage",id[0])).getCanonicalFile();
  File folder=new File(volume,id[1]).getCanonicalFile();
  if(!folder.toPath().startsWith(volume.toPath())||!folder.isDirectory())throw new IOException("Backup folder is unavailable");
  return folder;
 }
 static JSONObject transaction(LiveRestore.Plan plan)throws Exception {
  if(metadata(plan.manifest)==null)return null;
  String pkg=plan.manifest.getJSONArray("apps").getJSONObject(0).getString("package");
  appDirectory(pkg);
  return new JSONObject().put("id",UUID.randomUUID().toString()).put("package",pkg);
 }
 static void stage(BackupEngine engine,LiveRestore.Plan plan,JSONObject transaction)throws Exception {
  JSONObject external=metadata(plan.manifest);
  if(plan.source==null)throw new IOException("External backup must be opened from its original ZIP");
  File base=engine.c.getExternalFilesDir(null);
  File expected=new File(Environment.getExternalStorageDirectory(),"Android/data/"+engine.c.getPackageName()+"/files");
  if(base==null||!base.getCanonicalFile().equals(expected.getCanonicalFile()))throw new IOException("Primary external restore staging is unavailable");
  if(base.getUsableSpace()<external.getLong("bytes")+RESERVE)throw new IOException("Free more internal storage to stage the external files before restore");
  String id=id(transaction);
  File stage=new File(base,".raven-stage-"+id);
  if(!stage.mkdir())throw new IOException("Cannot create external staging folder");
  try{
   try(InputStream raw=engine.c.getContentResolver().openInputStream(plan.source)){
    if(raw==null)throw new IOException("Cannot reopen external backup");
    try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(raw,65536))){
     ZipEntry entry;
     while((entry=zip.getNextEntry())!=null){
      if(!entry.getName().equals(ENTRY))continue;
      read(zip,external,stage);zip.closeEntry();return;
     }
    }
   }
   throw new IOException("External backup payload missing");
  }catch(Exception e){try{cleanup(engine,transaction);}catch(Exception cleanup){e.addSuppressed(cleanup);}throw e;}
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
  String id=id(transaction),app=appDirectory(transaction.getString("package"));
  engine.runLong(engine.job(),"[ ! -L "+q(app)+" ]\nrm -rf -- "+q(STAGES+".raven-stage-"+id)+" "+q(app+"/.raven-old-"+id)+"\n");
 }
}
