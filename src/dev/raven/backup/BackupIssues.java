package dev.raven.backup;
import android.content.*;import org.json.*;
/** Remember a failed validation only while the reviewed file identity remains unchanged. */
final class BackupIssues {
 static String key(BackupCatalog.Entry e){return e.uri.toString();}
 static void mark(Context c,BackupCatalog.Entry e,String reason){try{c.getSharedPreferences("backup-issues",0).edit().putString(key(e),new JSONObject().put("size",e.size).put("modified",e.modified).put("reason",reason).toString()).apply();e.validationProblem=reason;}catch(JSONException ignored){}}
 static void clear(Context c,BackupCatalog.Entry e){c.getSharedPreferences("backup-issues",0).edit().remove(key(e)).apply();e.validationProblem=null;}
 static void load(Context c,BackupCatalog.Entry e){try{String raw=c.getSharedPreferences("backup-issues",0).getString(key(e),null);if(raw==null)return;JSONObject value=new JSONObject(raw);if(value.getLong("size")==e.size&&value.getLong("modified")==e.modified)e.validationProblem=value.getString("reason");else clear(c,e);}catch(Exception invalid){clear(c,e);}}
 static boolean invalidPayload(Exception e){for(Throwable cause=e;cause!=null;cause=cause.getCause())if(cause instanceof ExternalTar.InvalidData||cause instanceof java.util.zip.ZipException||cause instanceof java.io.EOFException)return true;return false;}
 static String reason(Exception e){String message=e.getMessage();return message==null||message.trim().isEmpty()?e.getClass().getSimpleName():message;}
}
