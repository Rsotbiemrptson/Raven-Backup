package dev.raven.backup;
import android.content.*;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

/** Full validation is read-only and never installs/stops an app. */
final class ArchiveVerifier {
 static final String VERIFY="verifyAfterBackup",RETENTION="retentionCount",ENCRYPT="encryptBackups";
 static void verify(BackupEngine engine,Uri uri)throws Exception{
  File job=engine.job();TransferProgress.phase("Verifying completed archive",-1);
  try(ArchiveAccess.Handle handle=engine.openArchive(uri)){
   IndexedArchive.Result checked=IndexedArchive.read(engine,uri,job,false);
   if(checked==null){try(InputStream input=engine.archiveStream(uri)){engine.unpack(input,job);}}
   else{
    JSONObject external=ExternalData.metadata(checked.manifest);
    if(external!=null){LiveRestore.Plan plan=new LiveRestore.Plan();plan.source=uri;plan.sourceStamp=checked.stamp;plan.manifest=checked.manifest;plan.job=job;TransferProgress.phase("Verifying external archive data",external.getLong("bytes"));
     if(external.has("scopes")){Map<String,Path> routes=new LinkedHashMap<>();JSONArray parts=external.getJSONArray("scopes");for(int i=0;i<parts.length();i++)routes.put(parts.getJSONObject(i).getString("id"),null);IndexedArchive.stageScopes(engine,plan,external,routes);}else IndexedArchive.stage(engine,plan,external,null);
    }
   }
   handle.verifyEncryption();
  }finally{engine.clean(job);}
 }
 static void mark(Context c,Uri uri)throws Exception{long[] stamp=stamp(c,uri);String value=stamp[0]+":"+stamp[1]+":"+System.currentTimeMillis();if(!c.getSharedPreferences("archive-verification",0).edit().putString(BackupEngine.digest(uri.toString().getBytes("UTF-8")),value).commit())throw new IOException("Could not save verification status");}
 static long verifiedAt(Context c,BackupCatalog.Entry entry){try{String value=c.getSharedPreferences("archive-verification",0).getString(BackupEngine.digest(entry.uri.toString().getBytes("UTF-8")),"");String[] fields=value.split(":");return fields.length==3&&Long.parseLong(fields[0])==entry.size&&Long.parseLong(fields[1])==entry.modified?Long.parseLong(fields[2]):0;}catch(Exception invalid){return 0;}}
 private static long[] stamp(Context c,Uri uri)throws Exception{try(android.database.Cursor row=c.getContentResolver().query(uri,new String[]{android.provider.DocumentsContract.Document.COLUMN_SIZE,android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED},null,null,null)){if(row==null||!row.moveToFirst())throw new IOException("Cannot inspect verified backup");return new long[]{row.getLong(0),row.getLong(1)};}}
}
