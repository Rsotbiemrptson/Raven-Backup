package dev.raven.backup;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import org.json.JSONException;

final class RestorePreferences {
 static final String SIGNATURE="allowDifferentSignature", VERSION="allowDifferentVersion", EXTERNAL="includeExternal";
 static final String SIGNATURE_LABEL="Allow different signatures", VERSION_LABEL="Allow different versions";
 static SharedPreferences prefs(Context context){return context.getSharedPreferences("MainActivity",0);}
 static boolean differentVersion(JSONObject saved,JSONObject installed)throws JSONException {
  return saved.getLong("version")!=installed.getLong("version");
 }
 static boolean differentSignature(JSONObject saved,JSONObject installed)throws JSONException {
  return !RestorePreparation.sameSigners(saved,installed);
 }
 static String warning(Context context,JSONObject saved,JSONObject installed)throws JSONException {
  boolean signature=differentSignature(saved,installed),version=differentVersion(saved,installed);
  boolean needSignature=signature&&!prefs(context).getBoolean(SIGNATURE,false);
  boolean needVersion=version&&!prefs(context).getBoolean(VERSION,false);
  if(!needSignature&&!needVersion)return null;
  String difference=signature&&version?"Different signature and version":signature?"Different signature":"Different version";
  String options=needSignature&&needVersion?SIGNATURE_LABEL+" and "+VERSION_LABEL:needSignature?SIGNATURE_LABEL:VERSION_LABEL;
  return difference+" for "+saved.getString("package")+". The transferred data may be unstable or unreadable. If you trust this installed app, enable "+options+" in Settings → Restore preferences and try again.";
 }
}
