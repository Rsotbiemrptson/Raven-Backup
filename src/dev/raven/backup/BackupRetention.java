package dev.raven.backup;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.*;
import java.util.*;
import java.io.*;

/** Opt-in pruning affects only recognized backups for this app, never recovery or unknown files. */
final class BackupRetention {
 static String prune(BackupEngine engine,Uri tree,Uri parent,String pkg,Uri newest,int keep)throws Exception{
  if(keep==0)return "";if(keep<1||keep>50)throw new IOException("Invalid backup retention setting");
  List<BackupCatalog.Entry> candidates=new ArrayList<>();List<BackupCatalog.Entry> rows=new ArrayList<>();Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(parent,DocumentsContract.getDocumentId(parent));
  try(android.database.Cursor cursor=engine.c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_SIZE,DocumentsContract.Document.COLUMN_LAST_MODIFIED,DocumentsContract.Document.COLUMN_MIME_TYPE},null,null,null)){
   if(cursor==null)throw new IOException("Cannot review older backups");while(cursor.moveToNext()){if(DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(4))||!ArchiveNames.backup(cursor.getString(1)))continue;BackupCatalog.Entry e=new BackupCatalog.Entry();e.uri=DocumentsContract.buildDocumentUriUsingTree(parent,cursor.getString(0));e.name=cursor.getString(1);e.size=cursor.getLong(2);e.modified=cursor.getLong(3);rows.add(e);}
  }
  BackupCatalog.read(engine.c,rows);
  for(BackupCatalog.Entry entry:rows){if(entry.manifest==null||entry.recovery||entry.privateRecovery||entry.validationProblem!=null)continue;JSONArray apps=entry.manifest.getJSONArray("apps");if(apps.length()==1&&pkg.equals(apps.getJSONObject(0).getString("package")))candidates.add(entry);}
  // New archive is always retained even if a storage provider has coarse/incorrect timestamps.
  candidates.sort((a,b)->a.uri.equals(b.uri)?0:a.uri.equals(newest)?-1:b.uri.equals(newest)?1:Long.compare(b.manifest.optLong("created",b.modified),a.manifest.optLong("created",a.modified)));
  List<BackupCatalog.Entry> remove=RetentionPolicy.older(candidates,keep);if(remove.isEmpty())return "";
  BackupCatalog.Entry anchor=null;for(BackupCatalog.Entry row:rows)if(row.uri.equals(newest))anchor=row;
  if(anchor==null||ArchiveVerifier.verifiedAt(engine.c,anchor)==0)throw new IOException("Newest backup verification receipt is missing; no older backups removed");
  android.system.StructStat stamp=ArchiveAccess.stat(engine.c,newest);ArchiveAccess.Physical physical=new ArchiveAccess.Physical(stamp);
  StringBuilder report=new StringBuilder();for(BackupCatalog.Entry old:remove){if(!physical.same(ArchiveAccess.stat(engine.c,newest)))throw new IOException("Newest backup changed; retention stopped");report.append(BackupStorage.delete(engine.c,tree,Collections.singletonList(old)));}return report.toString();
 }
}
