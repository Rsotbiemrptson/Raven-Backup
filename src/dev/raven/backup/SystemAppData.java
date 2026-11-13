package dev.raven.backup;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import java.io.IOException;
import org.json.JSONObject;

/** System data is restored only into the matching installed system app and firmware. */
final class SystemAppData {
 static boolean system(ApplicationInfo app){return (app.flags&ApplicationInfo.FLAG_SYSTEM)!=0;}
 static boolean googleService(String pkg){
  return pkg.equals("com.google.android.verifier")||pkg.equals("com.google.android.contactkeys")||pkg.equals("com.google.android.safetycore");
 }
 static boolean systemSection(ApplicationInfo app){return system(app)||googleService(app.packageName);}
 static boolean sharedAssistant(String pkg,int uid,String shared){
  return pkg.equals("com.odin.gameassistant")&&uid==1000&&"android.uid.system".equals(shared);
 }
 static boolean eligible(PackageInfo app){
  int uid=app.applicationInfo.uid;
  if(uid>=10000&&uid<100000&&app.sharedUserId==null)return true;
  return system(app.applicationInfo)&&sharedAssistant(app.packageName,uid,app.sharedUserId);
 }
 static boolean system(JSONObject app){return app.optBoolean("systemApp",false);}
 static void dataOnly(JSONObject app)throws Exception{
  if(system(app)&&app.has("apks"))throw new IOException("System-app backups must contain data only");
 }
 static void require(JSONObject saved,JSONObject current,JSONObject manifest)throws Exception{
  if(!system(saved)&&!system(current))return;
  dataOnly(saved);
  if(!system(saved)||!system(current))throw new IOException("System-app data requires a matching system-app backup");
  if(!saved.getString("package").equals(current.getString("package"))
    ||saved.getInt("uid")!=current.getInt("uid")
    ||saved.getLong("version")!=current.getLong("version")
    ||!saved.getJSONArray("signers").toString().equals(current.getJSONArray("signers").toString())
    ||!saved.optString("sharedUser").equals(current.optString("sharedUser")))
   throw new IOException("System-app restore requires the same installed identity, version and signature");
  if(!FirmwareCheck.matches(manifest,FirmwareCheck.current()))
   throw new IOException("System-app data requires the same device model, Android version and firmware");
 }
 static boolean context(JSONObject app,String value){
  if(value==null)return false;
  if(value.matches("u:object_r:app_data_file:s0(:c[0-9]+(,c[0-9]+)*)?"))return true;
  return system(app)&&value.equals("u:object_r:system_app_data_file:s0");
 }
 static final class Running extends IOException {
  final String pkg;
  Running(String pkg){super("System app is still running: "+pkg);this.pkg=pkg;}
 }
 static String runningPackage(String log){
  for(String line:log.split("\\r?\\n"))if(line.matches("RAVEN_SYSTEM_RUNNING [A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))return line.substring("RAVEN_SYSTEM_RUNNING ".length());
  return null;
 }
 static String stopped(JSONObject app)throws Exception{
  if(!system(app))return "";
  String pkg=app.getString("package");
  String pattern="^"+pkg.replace(".","\\.")+"(:|$)";
  // Do not use grep -q: its early exit can SIGPIPE ps under pipefail.
  return "raven_process_file=\"${0%/*}/system-processes\"\nps -A -w -o NAME > \"$raven_process_file\" || { echo 'Cannot verify whether the system app is stopped'; exit 1; }\n"
   +"[ -s \"$raven_process_file\" ] || { echo 'System process list is empty; operation stopped'; exit 1; }\n"
   +"if grep -E "+BackupEngine.q(pattern)+" \"$raven_process_file\" > /dev/null; then\n"
   +"echo "+BackupEngine.q("RAVEN_SYSTEM_RUNNING "+pkg)+"; echo 'System app is still running; its data cannot be safely backed up or restored'; exit 1\n"
   +"else raven_process_status=$?; [ \"$raven_process_status\" = 1 ] || { echo 'System process check failed; operation stopped'; exit 1; }; fi\n";
 }
}
