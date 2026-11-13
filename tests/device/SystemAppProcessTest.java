package dev.raven.backup;
import android.app.Instrumentation;import android.os.Bundle;import org.json.JSONObject;
public class SystemAppProcessTest extends Instrumentation {
 public void onCreate(Bundle b){super.onCreate(b);start();}
 void require(boolean value,String message){if(!value)throw new AssertionError(message);}
 void blocked(BackupEngine e,String name,String script)throws Exception{try{String output=e.run(e.job(),script,false);throw new AssertionError(name+" unsafe check passed: "+output);}catch(java.io.IOException expected){}}
 public void onStart(){Bundle report=new Bundle();try{
  BackupEngine e=new BackupEngine(getTargetContext());JSONObject app=e.info("com.odin.gameassistant");String guard=SystemAppData.stopped(app);
  try{e.run(e.job(),guard,false);throw new AssertionError("Did not detect live Game Assistant");}catch(SystemAppData.Running expected){require(expected.pkg.equals("com.odin.gameassistant"),"running package");report.putString("actual_running","PASS detected with corrected check");}
  blocked(e,"large list","ps() { printf 'NAME\\n'; i=0; while [ \"$i\" -lt 20000 ]; do echo unrelated.process; i=$((i+1)); done; echo com.odin.gameassistant; }\n"+guard);
  blocked(e,"subprocess","ps() { printf 'NAME\\ncom.odin.gameassistant:worker\\n'; }\n"+guard);
  blocked(e,"enumeration failure","ps() { return 9; }\n"+guard);
  blocked(e,"empty output","ps() { :; }\n"+guard);
  e.run(e.job(),"ps() { printf 'NAME\\ncom.odin.gameassistant.other\\nsh\\n'; }\n"+guard+"echo SAFE_NO_MATCH",false);
  report.putString("guard_regression","PASS large output, subprocess, enumeration failure, empty output and prefix-only name");
  report.putString("result","PASS; running processes are refused without force close");finish(-1,report);
 }catch(Throwable t){report.putString("error",android.util.Log.getStackTraceString(t));finish(0,report);}}
}
