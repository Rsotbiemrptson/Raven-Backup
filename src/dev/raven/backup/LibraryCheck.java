package dev.raven.backup;
import android.content.*;import android.net.Uri;import android.provider.DocumentsContract;import org.json.*;import java.io.*;import java.util.*;
/** Checks existing archives and destination access without restoring or stopping target apps. */
final class LibraryCheck {
 static String run(Context context,Uri tree)throws Exception {
  if(tree==null)throw new IOException("Choose a backup folder first");BackupEngine engine=new BackupEngine(context);String root=engine.probe();
  String access=folderAccess(context,tree);Verification checked=inspect(context,tree);String result=root+"\n"+access+"\n"+checked.report();BackupEngine.write(new File(context.getFilesDir(),"library-check.txt"),result);return result;
 }
 static String folderAccess(Context context,Uri tree)throws Exception{
  if(tree==null)throw new IOException("Choose a backup folder first");
  Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));Uri probe=null;
  try{
   String name=".raven-access-check-"+UUID.randomUUID()+".pending";probe=DocumentsContract.createDocument(context.getContentResolver(),parent,"application/octet-stream",name);if(probe==null)throw new IOException("Cannot create destination check");
   try(OutputStream out=context.getContentResolver().openOutputStream(probe,"w")){if(out==null)throw new IOException("Cannot write destination");out.write("Raven Backup access check".getBytes("UTF-8"));}
   Uri renamed=DocumentsContract.renameDocument(context.getContentResolver(),probe,name+".checked");if(renamed==null)throw new IOException("Cannot rename destination check");probe=renamed;
   try(InputStream in=context.getContentResolver().openInputStream(probe)){if(in==null)throw new IOException("Cannot read destination check");ByteArrayOutputStream bytes=new ByteArrayOutputStream();BackupEngine.copy(in,bytes,1024);if(!bytes.toString("UTF-8").equals("Raven Backup access check"))throw new IOException("Destination read differs");}
  }finally{if(probe!=null&&!DocumentsContract.deleteDocument(context.getContentResolver(),probe))throw new IOException("Could not remove temporary destination check");}
  return "PASS folder read/write/rename/delete access. Existing backups were not scanned or changed.";
 }
 static final class Verification {
  final List<BackupCatalog.Entry> failed=new ArrayList<>();int passed;
  String report(){StringBuilder s=new StringBuilder("Verified "+passed+" backups; "+failed.size()+" need attention.\n");for(BackupCatalog.Entry e:failed)s.append("\n").append(e.name).append("\n").append(e.validationProblem).append("\n");return s.append("\nNo backup was deleted. Installed app data was not changed.").toString();}
 }
 static Verification inspect(Context context,Uri tree)throws Exception{
  Verification result=new Verification();BackupEngine engine=new BackupEngine(context);
  for(BackupCatalog.Entry entry:BackupCatalog.list(context,tree)){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Verification interrupted");File job=engine.job();try(InputStream in=context.getContentResolver().openInputStream(entry.uri)){
   if(in==null)throw new IOException("Cannot read backup");engine.unpack(in,job);BackupIssues.clear(context,entry);result.passed++;
  }catch(InterruptedIOException interrupted){throw interrupted;}catch(Exception invalid){BackupIssues.mark(context,entry,BackupIssues.reason(invalid));result.failed.add(entry);}finally{engine.clean(job);}}
  return result;
 }
}
