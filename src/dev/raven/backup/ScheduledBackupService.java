package dev.raven.backup;
import android.app.job.*;import android.net.Uri;import org.json.*;import java.util.*;

public final class ScheduledBackupService extends JobService {
 volatile Thread worker;volatile boolean stopped;volatile BackupEngine active;
 @Override public boolean onStartJob(JobParameters params){if(!BackupSchedule.prefs(this).getBoolean("enabled",false)||(worker!=null&&worker.isAlive()))return false;stopped=false;worker=new Thread(()->{
  String result;
  try{synchronized(LiveRestore.class){if(stopped)return;if(!BackupSchedule.prefs(this).getBoolean("enabled",false))throw new java.io.IOException("Schedule disabled");JSONObject config=BackupSchedule.configuration(this);if(config==null)throw new java.io.IOException("Schedule is not configured");List<String> packages=new ArrayList<>();JSONArray saved=config.getJSONArray("packages");for(int i=0;i<saved.length();i++)packages.add(saved.getString(i));
    active=new BackupEngine(this);active.cancellation=new java.io.File(getFilesDir(),"schedule-cancel-"+java.util.UUID.randomUUID());if(stopped)throw new java.io.InterruptedIOException("Schedule stopped");result=active.backup(packages,Uri.parse(config.getString("tree")));}
  }catch(Exception e){result="Scheduled backup stopped: "+e.getMessage();}
  BackupSchedule.prefs(this).edit().putString("lastResult",result).putLong("lastRun",System.currentTimeMillis()).apply();android.util.Log.i("RavenBackupSchedule",result);if(active!=null&&active.cancellation!=null)active.cancellation.delete();active=null;if(!stopped)jobFinished(params,false);
 });worker.setName("Raven scheduled backup");worker.start();return true;}
 @Override public boolean onStopJob(JobParameters params){stopped=true;if(active!=null&&active.cancellation!=null)try{BackupEngine.write(active.cancellation,"stop");}catch(Exception ignored){}if(worker!=null)worker.interrupt();return true;}
}
