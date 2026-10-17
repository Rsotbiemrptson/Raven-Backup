package dev.raven.backup;
import android.content.Context;import android.net.Uri;import android.provider.DocumentsContract;import org.json.JSONObject;
/** Names belong to the backup, even after an app is removed. Older ZIPs use installed/cache/folder hints. */
final class AppNames {
 static String saved(Context c,JSONObject app,Uri uri)throws Exception {
  String pkg=app.getString("package"),label=app.optString("label","").trim();
  if(label.isEmpty()||label.equals(pkg))try{label=c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}
  if(label.isEmpty()||label.equals(pkg))label=c.getSharedPreferences("app-labels",0).getString(pkg,"");
  if((label.isEmpty()||label.equals(pkg))&&uri!=null)try{String id=DocumentsContract.getDocumentId(uri);int last=id.lastIndexOf('/');if(last>0){String parent=id.substring(0,last);parent=parent.substring(parent.lastIndexOf('/')+1);String suffix=" ("+pkg+")";if(parent.endsWith(suffix))label=parent.substring(0,parent.length()-suffix.length());}}catch(Exception ignored){}
  if(!label.isEmpty()&&!label.equals(pkg)){c.getSharedPreferences("app-labels",0).edit().putString(pkg,label).apply();return label;}
  return pkg;
 }
}
