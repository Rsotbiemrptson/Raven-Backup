package dev.raven.backup;
import android.app.job.*;import android.content.*;import android.net.Uri;import android.os.Environment;import org.json.*;import java.io.*;import java.util.*;

final class BackupSchedule {
 static final int JOB=71001;
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("backup-schedule",0);}
 static JSONObject configuration(Context c)throws Exception{String json=prefs(c).getString("config",null);return json==null?null:new JSONObject(json);}
 static void enable(Context c,List<String> packages,Uri tree,int days,boolean charging,boolean idle)throws Exception {
  if(days!=1&&days!=7)throw new IOException("Choose a daily or weekly schedule");
  if(packages.isEmpty()||tree==null)throw new IOException("Choose apps and a backup folder first");if(!Environment.isExternalStorageManager())throw new IOException("Allow internal-storage before scheduling");
  BackupEngine engine=new BackupEngine(c);for(String pkg:packages)engine.info(pkg);
  JSONObject config=new JSONObject().put("packages",new JSONArray(new TreeSet<>(packages))).put("tree",tree.toString()).put("days",days).put("charging",charging).put("idle",idle);
  if(!prefs(c).edit().putString("config",config.toString()).putBoolean("enabled",true).commit())throw new IOException("Cannot save schedule");
  register(c,days*24L*60*60*1000,charging,idle);
 }
 static void register(Context c,long interval,boolean charging,boolean idle)throws Exception {
  JobInfo job=new JobInfo.Builder(JOB,new ComponentName(c,ScheduledBackupService.class)).setPeriodic(interval,Math.max(JobInfo.getMinFlexMillis(),Math.min(interval/10,60L*60*1000))).setPersisted(true).setRequiresStorageNotLow(true).setRequiresCharging(charging).setRequiresDeviceIdle(idle).build();
  if(((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).schedule(job)!=JobScheduler.RESULT_SUCCESS){prefs(c).edit().putBoolean("enabled",false).commit();throw new IOException("Android could not register the schedule");}
 }
 static void disable(Context c){((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(JOB);prefs(c).edit().putBoolean("enabled",false).apply();}
 static void ensureRegistered(Context c){if(!prefs(c).getBoolean("enabled",false))return;JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(scheduler.getPendingJob(JOB)!=null)return;try{JSONObject config=configuration(c);if(config==null)throw new IOException("Schedule configuration missing");List<String> packages=new ArrayList<>();JSONArray saved=config.getJSONArray("packages");for(int i=0;i<saved.length();i++)packages.add(saved.getString(i));if(config.optBoolean("naturalTest",false)){disable(c);return;}enable(c,packages,Uri.parse(config.getString("tree")),config.getInt("days"),config.getBoolean("charging"),config.getBoolean("idle"));}catch(Exception e){disable(c);prefs(c).edit().putString("lastResult","Schedule needs setup: "+e.getMessage()).apply();}}
 static String summary(Context c){try{JSONObject config=configuration(c);if(config==null||!prefs(c).getBoolean("enabled",false))return "Off — no automatic backups";if(config.optBoolean("naturalTest",false))return "Verification test • Approx. every 15 minutes • Disposable app only";String frequency=config.getInt("days")==1?"Daily":"Weekly";return frequency+" • "+config.getJSONArray("packages").length()+" apps"+(config.getBoolean("charging")?" • Charging":"")+(config.getBoolean("idle")?" • Idle":"");}catch(Exception e){return "Schedule needs setup";}}
}
