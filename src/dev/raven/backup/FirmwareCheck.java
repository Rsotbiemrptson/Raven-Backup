package dev.raven.backup;
import android.os.Build;import org.json.*;import java.io.IOException;
/** Source identity is preserved; migrations need explicit, per-plan consent. */
final class FirmwareCheck {
 static JSONObject current()throws Exception{
  String system=RootBridge.call("getprop ro.system.build.fingerprint").trim(),vendor=RootBridge.call("getprop ro.vendor.build.fingerprint").trim();
  if(system.isEmpty()||vendor.isEmpty())throw new IOException("Cannot establish framework and vendor firmware identities.");
  return new JSONObject().put("system",system).put("vendor",vendor);
 }
 static void capture(JSONObject manifest)throws Exception{manifest.put("firmwareBase",current());}
 static void validate(JSONObject manifest)throws Exception{
  if(manifest.getString("model").trim().isEmpty()||manifest.getString("fingerprint").trim().isEmpty()||manifest.getInt("sdk")<1)throw new IOException("Invalid backup device identity.");
  if(manifest.has("firmwareBase")){JSONObject base=manifest.getJSONObject("firmwareBase");if(base.getString("system").trim().isEmpty()||base.getString("vendor").trim().isEmpty())throw new IOException("Incomplete saved firmware identity.");}
 }
 static boolean matches(JSONObject manifest,JSONObject now)throws Exception{
  validate(manifest);JSONObject saved=manifest.optJSONObject("firmwareBase");
  return FirmwarePolicy.compatible(manifest.getString("fingerprint"),manifest.getString("model"),manifest.getInt("sdk"),saved==null?"":saved.getString("system"),saved==null?"":saved.getString("vendor"),Build.FINGERPRINT,Build.MODEL,Build.VERSION.SDK_INT,now.getString("system"),now.getString("vendor"));
 }
 static String source(JSONObject manifest)throws Exception{
  validate(manifest);return MigrationPolicy.source(manifest.optString("manufacturer",""),manifest.getString("model"),manifest.optString("androidRelease",""),manifest.getInt("sdk"),manifest.optString("device",""),manifest.has("firmwareBase")?manifest.getJSONObject("firmwareBase").getString("system"):manifest.getString("fingerprint"));
 }
 static String warning(JSONObject manifest)throws Exception{
  if(matches(manifest,current()))return "";
  return MigrationPolicy.warning(!manifest.getString("model").equals(Build.MODEL),manifest.getInt("sdk"),Build.VERSION.SDK_INT)+"\n\n"+source(manifest)+"\n\nThis device: "+MigrationPolicy.display(Build.MODEL)+"\nAndroid "+MigrationPolicy.display(Build.VERSION.RELEASE)+" (API "+Build.VERSION.SDK_INT+")\n\nYou can remove the recovery backup from Settings once you confirm the restore was successful.";
 }
 static String approvalKey(JSONObject manifest)throws Exception{
  JSONObject now=current();validate(manifest);
  return BackupEngine.digest((manifest.getString("model")+"\n"+manifest.getInt("sdk")+"\n"+manifest.getString("fingerprint")+"\n"+manifest.optString("firmwareBase","")+"\n"+Build.MODEL+"\n"+Build.VERSION.SDK_INT+"\n"+Build.FINGERPRINT+"\n"+now.toString()).getBytes("UTF-8"));
 }
 static void require(JSONObject manifest)throws Exception{require(manifest,null);}
 static void require(JSONObject manifest,String approval)throws Exception{
  if(!MigrationPolicy.approvalMatches(!matches(manifest,current()),approvalKey(manifest),approval))throw new IOException("This backup was made on a different device or firmware. Review the migration warning and confirm this restore first.");
 }
}
