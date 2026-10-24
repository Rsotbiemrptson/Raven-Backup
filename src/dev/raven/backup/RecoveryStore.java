package dev.raven.backup;
import android.content.Context;import android.net.Uri;import org.json.*;import java.io.*;import java.nio.file.Files;import java.util.*;
/** App-owned, standalone recovery ZIPs. Active transaction copies remain separate. */
final class RecoveryStore {
 static File directory(Context c)throws IOException{File root=new File(c.getFilesDir(),"recovery-backups");if(Files.isSymbolicLink(root.toPath()))throw new IOException("Invalid recovery storage");if(!root.isDirectory()&&!root.mkdir())throw new IOException("Cannot create recovery storage");return root;}
 static File owned(Context c,File file)throws IOException{File root=directory(c);if(!root.getCanonicalFile().equals(file.getCanonicalFile().getParentFile())||Files.isSymbolicLink(file.toPath())||!file.isFile()||!file.getName().startsWith("Recovery_")||!file.getName().endsWith(".zip"))throw new IOException("Not an owned recovery backup");return file;}
 static List<BackupCatalog.Entry> entries(Context c)throws Exception{List<BackupCatalog.Entry> result=new ArrayList<>();File[] files=directory(c).listFiles();if(files==null)throw new IOException("Cannot read recovery storage");for(File file:files){if(!file.getName().startsWith("Recovery_")||!file.getName().endsWith(".zip"))continue;owned(c,file);BackupCatalog.Entry e=new BackupCatalog.Entry();e.uri=Uri.fromFile(file);e.name=file.getName();e.size=file.length();e.modified=file.lastModified();e.recovery=true;e.privateRecovery=true;result.add(e);}return result;}
 static String save(BackupEngine engine,File snapshotJob,JSONObject snapshot)throws Exception{
  File root=directory(engine.c);String group=UUID.randomUUID().toString();JSONArray apps=snapshot.getJSONArray("apps");List<String> names=new ArrayList<>();
  for(int i=0;i<apps.length();i++){
   JSONObject app=new JSONObject(apps.getJSONObject(i).toString());File stage=engine.job();File pending=null;
   try{
    JSONArray payloads=app.getJSONArray("payloads");for(int j=0;j<payloads.length();j++){JSONObject payload=payloads.getJSONObject(j);String base="app0-"+payload.getString("domain");for(String key:new String[]{"tar","labels"}){String name=base+(key.equals("tar")?".tar":".labels");try(InputStream in=new FileInputStream(new File(snapshotJob,payload.getString(key)));OutputStream out=new FileOutputStream(new File(stage,name))){BackupEngine.copy(in,out,BackupEngine.MAX);}payload.put(key,name);}}
    JSONObject manifest=new JSONObject(snapshot.toString()).put("apps",new JSONArray().put(app)).put("backupType","recovery").put("recoveryGroup",group).put("recoveryPurpose","App data immediately before restore");
    String label=app.optString("label",app.getString("package"));String name="Recovery_"+BackupNames.stem(label,new Date(snapshot.getLong("created")))+"_"+UUID.randomUUID().toString().substring(0,8)+".zip";
    File target=new File(root,name);if(target.exists())throw new IOException("Recovery name collision; retry restore");pending=new File(root,name+".pending");
    BackupEngine.write(new File(stage,"manifest.json"),manifest.toString(2));engine.zip(stage,manifest,new FileOutputStream(pending));try(RandomAccessFile file=new RandomAccessFile(pending,"rw")){file.getFD().sync();}
    File check=engine.job();try(InputStream in=new FileInputStream(pending)){engine.unpack(in,check);}finally{engine.clean(check);}
    android.system.Os.rename(pending.getPath(),target.getPath());pending=null;java.io.FileDescriptor dir=android.system.Os.open(root.getPath(),android.system.OsConstants.O_RDONLY,0);try{android.system.Os.fsync(dir);}finally{android.system.Os.close(dir);}names.add(name);
   }finally{engine.clean(stage);if(pending!=null&&pending.exists())pending.delete();}
  }return String.join("\n",names);
 }
 static void allowed(Context c,BackupCatalog.Entry e)throws Exception{if(!e.privateRecovery||!"file".equals(e.uri.getScheme()))throw new IOException("Invalid recovery selection");File file=owned(c,new File(e.uri.getPath()));if(!e.name.equals(file.getName())||e.size!=file.length()||e.modified!=file.lastModified())throw new IOException("Recovery backups changed; review again");}
 static void delete(Context c,BackupCatalog.Entry e)throws Exception{allowed(c,e);if(!new File(e.uri.getPath()).delete())throw new IOException("Cannot remove recovery backup: "+e.name);}
}
