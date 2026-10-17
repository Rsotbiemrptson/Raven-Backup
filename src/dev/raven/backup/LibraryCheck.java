package dev.raven.backup;
import android.content.*;import android.net.Uri;import android.provider.DocumentsContract;import org.json.*;import java.io.*;import java.util.*;
/** Checks existing archives and destination access without restoring or stopping target apps. */
final class LibraryCheck {
 static String run(Context context,Uri tree)throws Exception {
  if(tree==null)throw new IOException("Choose a backup folder first");BackupEngine engine=new BackupEngine(context);String root=engine.probe();
  List<BackupCatalog.Entry> entries=BackupCatalog.list(context,tree);int count=0;Set<String> packages=new TreeSet<>();
  for(BackupCatalog.Entry entry:entries){File job=engine.job();try(InputStream in=context.getContentResolver().openInputStream(entry.uri)){
   if(in==null)throw new IOException("Cannot read "+entry.name);JSONObject manifest=engine.unpack(in,job);JSONArray apps=manifest.getJSONArray("apps");for(int i=0;i<apps.length();i++)packages.add(apps.getJSONObject(i).getString("package"));count++;
  }catch(Exception invalid){throw new IOException("Could not verify backup \""+entry.name+"\". It may be damaged or not a Raven backup. Check or move this file, then retry. No backup was deleted.",invalid);}finally{engine.clean(job);}}
  Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));Uri probe=null;
  try{
   String name=".raven-access-check-"+UUID.randomUUID()+".pending";probe=DocumentsContract.createDocument(context.getContentResolver(),parent,"application/octet-stream",name);if(probe==null)throw new IOException("Cannot create destination check");
   try(OutputStream out=context.getContentResolver().openOutputStream(probe,"w")){if(out==null)throw new IOException("Cannot write destination");out.write("Raven Backup access check".getBytes("UTF-8"));}
   Uri renamed=DocumentsContract.renameDocument(context.getContentResolver(),probe,name+".checked");if(renamed==null)throw new IOException("Cannot rename destination check");probe=renamed;
   try(InputStream in=context.getContentResolver().openInputStream(probe)){if(in==null)throw new IOException("Cannot read destination check");ByteArrayOutputStream bytes=new ByteArrayOutputStream();BackupEngine.copy(in,bytes,1024);if(!bytes.toString("UTF-8").equals("Raven Backup access check"))throw new IOException("Destination read differs");}
  }finally{if(probe!=null&&!DocumentsContract.deleteDocument(context.getContentResolver(),probe))throw new IOException("Could not remove temporary destination check");}
  String result="PASS root access from "+context.getPackageName()+": "+root+"\nPASS folder read/write/rename/delete access.\nPASS "+count+" existing ZIPs fully validated (CRC, hashes, paths and payloads). Apps: "+packages+"\nLive restore verified: "+new LiveRestore(context).enabled()+"\nExisting backups and installed app data unchanged.";
  BackupEngine.write(new File(context.getFilesDir(),"library-check.txt"),result);return result;
 }
}
