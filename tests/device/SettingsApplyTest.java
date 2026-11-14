package dev.raven.backup;
import android.content.*;import android.app.job.*;import org.json.*;import java.io.*;import java.util.*;
final class SettingsApplyTest {
 static void require(boolean v,String m){if(!v)throw new AssertionError(m);}
 static String run(Context real)throws Exception{
  File files=new File(real.getFilesDir(),"settings-transfer-disposable");files.mkdirs();final int[] cancelled={0};
  JobScheduler fake=new JobScheduler(){public int schedule(JobInfo j){return RESULT_SUCCESS;}public int enqueue(JobInfo j,JobWorkItem w){return RESULT_SUCCESS;}public void cancel(int id){require(id==BackupSchedule.JOB,"wrong job");cancelled[0]++;}public void cancelAll(){throw new AssertionError("cancel all");}public List<JobInfo> getAllPendingJobs(){return Collections.emptyList();}public JobInfo getPendingJob(int id){return null;}};
  Context c=new ContextWrapper(real){public SharedPreferences getSharedPreferences(String name,int mode){return real.getSharedPreferences("disposable-settings-"+name,mode);}public File getFilesDir(){return files;}public Object getSystemService(String name){return name.equals(Context.JOB_SCHEDULER_SERVICE)?fake:super.getSystemService(name);}};
  for(String name:new String[]{"MainActivity","appearance","backup-schedule","live-restore"})c.getSharedPreferences(name,0).edit().clear().commit();
  SettingsTransfer.main(c).edit().putString("tree","content://current-folder").putBoolean("includeApks",false).putBoolean("includeExternal",true).putBoolean("allowDifferentSignature",true).putBoolean("allowDifferentVersion",true).putBoolean("launchAfter",true).putInt("backupOrder",5).commit();
  c.getSharedPreferences("appearance",0).edit().putString("mode","light").putInt("accent",0xff123456).commit();c.getSharedPreferences("live-restore",0).edit().putString("general-verified","unchanged-device-approval").commit();
  BackupSchedule.prefs(c).edit().putBoolean("enabled",true).putString("config",new JSONObject().put("packages",new JSONArray().put("dev.raven.fixture")).put("tree","content://old-folder").put("days",7).put("charging",false).put("idle",true).toString()).commit();
  JSONObject exported=SettingsTransfer.export(c),round=SettingsTransfer.read(new ByteArrayInputStream(exported.toString(2).getBytes("UTF-8")));require(exported.toString().equals(round.toString()),"JSON roundtrip");require(!exported.toString().contains("content://"),"folder grant exported");
  SettingsTransfer.main(c).edit().putBoolean("includeApks",true).putInt("backupOrder",0).commit();c.getSharedPreferences("appearance",0).edit().putString("mode","dark").commit();
  SettingsTransfer.apply(c,round);require(!SettingsTransfer.main(c).getBoolean("includeApks",true)&&SettingsTransfer.main(c).getBoolean("allowDifferentVersion",false),"preferences");require(SettingsTransfer.main(c).getInt("backupOrder",0)==5,"order");require("content://current-folder".equals(SettingsTransfer.main(c).getString("tree",null)),"folder replaced");require(c.getSharedPreferences("appearance",0).getInt("accent",0)==0xff123456&&"light".equals(c.getSharedPreferences("appearance",0).getString("mode",null)),"theme");
  require(!BackupSchedule.prefs(c).getBoolean("enabled",true)&&BackupSchedule.configuration(c).getInt("days")==7&&cancelled[0]==1,"schedule automatically enabled");require("unchanged-device-approval".equals(c.getSharedPreferences("live-restore",0).getString("general-verified",null)),"approval changed");
  JSONObject invalid=new JSONObject(round.toString()).put("verified",true);try{SettingsTransfer.apply(c,invalid);throw new AssertionError("bad fields accepted");}catch(IOException expected){}require(cancelled[0]==1,"invalid import changed state");
  File pointer=new File(files,"live-pending.json");try{BackupEngine.write(pointer,"{}");try{SettingsTransfer.apply(c,round);throw new AssertionError("pending recovery bypassed");}catch(IOException expected){}}finally{pointer.delete();}
  JSONObject noSchedule=new JSONObject(round.toString());noSchedule.remove("schedule");SettingsTransfer.apply(c,noSchedule);require(BackupSchedule.configuration(c)==null&&!BackupSchedule.prefs(c).getBoolean("enabled",true),"schedule not cleared");
  for(String name:new String[]{"MainActivity","appearance","backup-schedule","live-restore"})real.deleteSharedPreferences("disposable-settings-"+name);files.delete();
  return "PASS isolated settings export/import, preferences/theme/order, current folder/approval preserved, disabled schedule choices, invalid/pending-recovery refusal and absent-schedule clearing; actual user preferences/jobs untouched";
 }
}
