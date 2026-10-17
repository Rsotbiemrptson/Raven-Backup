package dev.raven.backup;
import android.os.Build;import org.json.*;import java.io.IOException;
final class FirmwareCheck {
 static JSONObject current()throws Exception{
  String[] lines={RootBridge.call("getprop ro.system.build.fingerprint").trim(),RootBridge.call("getprop ro.vendor.build.fingerprint").trim()};
  if(lines.length!=2||lines[0].trim().isEmpty()||lines[1].trim().isEmpty())throw new IOException("Cannot establish framework and vendor firmware identities.");
  return new JSONObject().put("system",lines[0].trim()).put("vendor",lines[1].trim());
 }
 static void capture(JSONObject manifest)throws Exception{manifest.put("firmwareBase",current());}
 static void require(JSONObject manifest)throws Exception{
  JSONObject saved=manifest.optJSONObject("firmwareBase"),now=current();
  if(manifest.has("firmwareBase")&&saved==null)throw new IOException("Invalid saved firmware identity.");
  String system=saved==null?"":saved.getString("system"),vendor=saved==null?"":saved.getString("vendor");
  if(saved!=null&&(system.isEmpty()||vendor.isEmpty()))throw new IOException("Incomplete saved firmware identity.");
  if(!FirmwarePolicy.compatible(manifest.getString("fingerprint"),manifest.getString("model"),manifest.getInt("sdk"),system,vendor,Build.FINGERPRINT,Build.MODEL,Build.VERSION.SDK_INT,now.getString("system"),now.getString("vendor")))throw new IOException("Backup firmware is incompatible. Fingerprint spoofing is supported only with matching saved framework/vendor identities or the recognized Odin 2 stock-318 legacy spoof.");
 }
}
