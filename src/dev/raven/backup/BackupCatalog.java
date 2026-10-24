package dev.raven.backup;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.provider.DocumentsContract;import org.json.*;import java.io.*;import java.util.*;import java.util.zip.*;

/** Lightweight directory catalog; full validation happens only when preparing a restore. */
final class BackupCatalog {
 static final class Entry {Uri uri;String name;long size,modified;JSONObject manifest;String problem;boolean recovery,privateRecovery;}
 static List<Entry> list(Context context,Uri tree)throws Exception {
  List<Entry> result=RecoveryStore.entries(context);if(tree!=null)scan(context,tree,DocumentsContract.getTreeDocumentId(tree),0,new HashSet<>(),result);
  return read(context,result);
 }
 static List<Entry> recoveries(Context context)throws Exception{return read(context,RecoveryStore.entries(context));}
 static List<Entry> read(Context context,List<Entry> result)throws Exception{
  result.sort((a,b)->Long.compare(b.modified,a.modified));
  for(Entry entry:result){try(InputStream stream=context.getContentResolver().openInputStream(entry.uri);ZipInputStream zip=new ZipInputStream(stream)){
    ZipEntry first=zip.getNextEntry();if(first==null||!first.getName().equals("manifest.json"))throw new IOException("Import to inspect this ZIP");ByteArrayOutputStream out=new ByteArrayOutputStream();BackupEngine.copy(zip,out,1024*1024);entry.manifest=new JSONObject(out.toString("UTF-8"));if((entry.manifest.getInt("format")!=1&&entry.manifest.getInt("format")!=2)||entry.manifest.getJSONArray("apps").length()==0)throw new IOException("Unknown archive format");entry.recovery=entry.privateRecovery||"recovery".equals(entry.manifest.optString("backupType"));
   }catch(Exception e){entry.problem="Import to inspect";entry.manifest=null;}}
  return result;
 }
 static void scan(Context context,Uri tree,String id,int depth,Set<String> seen,List<Entry> result)throws Exception {
  if(depth>4||seen.size()>=1000||result.size()>=500||!seen.add(id))return;
  Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,id);List<String> folders=new ArrayList<>();
  String[] columns={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED,DocumentsContract.Document.COLUMN_MIME_TYPE};
  try(Cursor cursor=context.getContentResolver().query(children,columns,null,null,null)){
   if(cursor==null)throw new IOException("Cannot read backup folder");
   while(cursor.moveToNext()){
    if(DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(4))){folders.add(cursor.getString(0));continue;}
    String name=cursor.getString(1);if(name==null||!name.toLowerCase(Locale.ROOT).endsWith(".zip"))continue;Entry entry=new Entry();entry.name=name;entry.size=cursor.getLong(2);entry.modified=cursor.getLong(3);entry.uri=DocumentsContract.buildDocumentUriUsingTree(tree,cursor.getString(0));result.add(entry);if(result.size()>=500)break;
   }
  }
  for(String folder:folders)scan(context,tree,folder,depth+1,seen,result);
 }
}
