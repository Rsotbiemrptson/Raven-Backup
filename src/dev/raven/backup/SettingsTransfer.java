package dev.raven.backup;
import android.content.*;import org.json.*;import java.io.*;import java.util.*;
/** Portable preferences only. Never imports Android grants, device approval or recovery state. */
final class SettingsTransfer {
 static final int LIMIT=65536;
 static final String[] BOOLEANS={"includeApks","includeExternal","launchAfter","allowDifferentSignature","allowDifferentVersion"};
 static SharedPreferences main(Context c){return c.getSharedPreferences("MainActivity",0);}
 static JSONObject export(Context c)throws Exception{
  SharedPreferences p=main(c),a=c.getSharedPreferences("appearance",0);JSONObject prefs=new JSONObject();
  for(String k:BOOLEANS)prefs.put(k,p.getBoolean(k,k.equals("includeApks")||k.equals("includeExternal")));
  prefs.put("backupOrder",BackupOrder.normalize(p.getInt("backupOrder",0)));
  JSONObject data=new JSONObject().put("type","raven-backup-settings").put("schema",1).put("exportedAt",System.currentTimeMillis()).put("appVersion",c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName)
   .put("appearance",new JSONObject().put("mode",a.getString("mode","dark")).put("accent",String.format(Locale.ROOT,"#%06X",a.getInt("accent",AppearanceColors.DEFAULT_ACCENT)&0xffffff))).put("preferences",prefs);
  JSONObject schedule=BackupSchedule.configuration(c);if(schedule!=null&&!schedule.optBoolean("naturalTest",false))data.put("schedule",new JSONObject().put("packages",schedule.getJSONArray("packages")).put("days",schedule.getInt("days")).put("charging",schedule.getBoolean("charging")).put("idle",schedule.getBoolean("idle")).put("wasEnabled",BackupSchedule.prefs(c).getBoolean("enabled",false)));
  return validate(data);
 }
 static boolean bool(JSONObject o,String k)throws Exception{Object v=o.get(k);if(!(v instanceof Boolean))throw new IOException("Invalid switch: "+k);return (Boolean)v;}
 static int integer(JSONObject o,String k)throws Exception{Object v=o.get(k);if(!(v instanceof Integer))throw new IOException("Invalid number: "+k);return (Integer)v;}
 static void keys(JSONObject o,String... allowed)throws Exception{Set<String> names=new HashSet<>(Arrays.asList(allowed));Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();if(!names.contains(k))throw new IOException("Unsupported settings field: "+k);}}
 static JSONObject read(InputStream in)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();BackupEngine.copy(in,out,LIMIT);return validate(new JSONObject(out.toString("UTF-8")));}
 static JSONObject validate(JSONObject input)throws Exception{
  if(input.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>LIMIT)throw new IOException("Settings file exceeds 64 KiB");
  JSONObject d=new JSONObject(input.toString());keys(d,"type","schema","exportedAt","appVersion","appearance","preferences","schedule");
  if(!"raven-backup-settings".equals(d.getString("type"))||integer(d,"schema")!=1)throw new IOException("This is not a supported Raven settings export.");
  JSONObject a=d.getJSONObject("appearance");keys(a,"mode","accent");if(!Arrays.asList("light","dark","system").contains(a.getString("mode"))||!a.getString("accent").matches("#[0-9A-Fa-f]{6}"))throw new IOException("Invalid theme settings");
  JSONObject p=d.getJSONObject("preferences");keys(p,"includeApks","includeExternal","launchAfter","allowDifferentSignature","allowDifferentVersion","backupOrder");for(String k:BOOLEANS)bool(p,k);int order=integer(p,"backupOrder");if(order<0||order>5)throw new IOException("Invalid backup order");
  if(d.has("schedule")){JSONObject s=d.getJSONObject("schedule");keys(s,"packages","days","charging","idle","wasEnabled");int days=integer(s,"days");if(days!=1&&days!=7)throw new IOException("Invalid schedule interval");bool(s,"charging");bool(s,"idle");bool(s,"wasEnabled");JSONArray apps=s.getJSONArray("packages");if(apps.length()==0||apps.length()>1000)throw new IOException("Invalid scheduled app count");Set<String> unique=new HashSet<>();for(int i=0;i<apps.length();i++){Object value=apps.get(i);if(!(value instanceof String)||((String)value).length()>255||!((String)value).matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")||!unique.add((String)value))throw new IOException("Invalid/duplicate scheduled app");}}
  return d;
 }
 static String review(JSONObject d)throws Exception{
  JSONObject p=d.getJSONObject("preferences"),a=d.getJSONObject("appearance");int order=p.getInt("backupOrder");String[] labels={"Package ID","App name","Size"};
  StringBuilder text=new StringBuilder("Replace Raven’s preferences with these settings?\n\nTheme: "+a.getString("mode")+"\nAccent: "+a.getString("accent")+"\nBackup order: "+labels[BackupOrder.key(order)]+(BackupOrder.descending(order)?" descending":" ascending"));
  String[] names={"Include app installers","Include external app files","Open apps after restore","Allow different signatures","Allow different versions"};for(int i=0;i<BOOLEANS.length;i++)text.append("\n").append(names[i]).append(": ").append(p.getBoolean(BOOLEANS[i])?"On":"Off");
  if(d.has("schedule")){JSONObject s=d.getJSONObject("schedule");text.append("\n\nSchedule: ").append(s.getInt("days")==1?"Daily":"Weekly").append(", ").append(s.getJSONArray("packages").length()).append(" apps").append(s.getBoolean("charging")?", charging":"").append(s.getBoolean("idle")?", idle":"").append(". Imported as OFF; review Choose apps & schedule to enable it.");}else text.append("\n\nNo saved schedule. Any current schedule will be disabled and cleared.");
  return text.append("\n\nYour current backup folder and device checks are kept. On a fresh device, choose the folder and complete guided setup separately. Android permissions, backup ZIPs and recovery data are not imported. Only import a file you trust.").toString();
 }
 static void restore(SharedPreferences p,Map<String,?> values)throws IOException{SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?> v:values.entrySet()){Object x=v.getValue();if(x instanceof String)e.putString(v.getKey(),(String)x);else if(x instanceof Boolean)e.putBoolean(v.getKey(),(Boolean)x);else if(x instanceof Integer)e.putInt(v.getKey(),(Integer)x);else if(x instanceof Long)e.putLong(v.getKey(),(Long)x);else if(x instanceof Float)e.putFloat(v.getKey(),(Float)x);else if(x instanceof Set)e.putStringSet(v.getKey(),new HashSet<>((Set<String>)x));}if(!e.commit())throw new IOException("Could not restore previous preferences");}
 static void apply(Context c,JSONObject input)throws Exception{
  JSONObject d=validate(input);if(OperationService.running()||ScheduledBackupService.running||new LiveRestore(c).pointer.exists())throw new IOException("Wait for the current operation or recover the pending restore before importing settings.");
  SharedPreferences p=main(c),a=c.getSharedPreferences("appearance",0),s=BackupSchedule.prefs(c);Map<String,?> oldP=p.getAll(),oldA=a.getAll(),oldS=s.getAll();
  try{
   JSONObject prefs=d.getJSONObject("preferences"),appearance=d.getJSONObject("appearance");SharedPreferences.Editor pe=p.edit();for(String k:BOOLEANS)pe.putBoolean(k,prefs.getBoolean(k));pe.putInt("backupOrder",prefs.getInt("backupOrder"));if(!pe.commit())throw new IOException("Could not save preferences");
   if(!a.edit().putString("mode",appearance.getString("mode")).putInt("accent",android.graphics.Color.parseColor(appearance.getString("accent"))).commit())throw new IOException("Could not save theme");
   SharedPreferences.Editor se=s.edit().clear().putBoolean("enabled",false);if(d.has("schedule")){JSONObject config=new JSONObject(d.getJSONObject("schedule").toString());config.remove("wasEnabled");config.put("tree",p.getString("tree",""));se.putString("config",config.toString());}if(!se.commit())throw new IOException("Could not save schedule");
  }catch(Exception failure){try{restore(p,oldP);restore(a,oldA);restore(s,oldS);}catch(Exception rollback){failure.addSuppressed(rollback);}throw failure;}
  ((android.app.job.JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(BackupSchedule.JOB);
 }
}
