package dev.raven.backup;
import android.app.*;import android.content.*;
/** A root backend is selected explicitly; automatic mode never starts a superuser request. */
final class RootConnections {
 static void choose(Activity activity,Runnable done){
  if(OperationService.running()||ScheduledBackupService.running||new LiveRestore(activity).pointer.exists()){new AlertDialog.Builder(activity).setMessage("Finish the current operation or recovery before changing root connection.").setPositiveButton("Close",null).show();return;}
  String[] modes={"auto","odin","su"},labels={"Automatic (compatible Odin service)","Odin OEM root service","Standard superuser (already rooted)"};String mode=activity.getSharedPreferences("device-root",0).getString("transport","auto");int selected=java.util.Arrays.asList(modes).indexOf(mode);
  new AlertDialog.Builder(activity).setTitle("Root connection").setSingleChoiceItems(labels,Math.max(0,selected),(dialog,index)->{dialog.dismiss();Runnable save=()->{activity.getSharedPreferences("device-root",0).edit().putString("transport",modes[index]).commit();activity.getSharedPreferences("live-restore",0).edit().remove("general-verified").commit();activity.getSharedPreferences("setup-flow",0).edit().putBoolean("needsCheck",true).commit();RootBridge.configure(activity);done.run();};if(index==2)new AlertDialog.Builder(activity).setTitle("Standard superuser").setMessage("Requires an already-rooted device and a root manager that grants Raven access. This does not root a device. Run guided checks before restoring; a root grant alone is insufficient.").setPositiveButton("Use superuser",(d,w)->save.run()).setNegativeButton("Cancel",null).show();else save.run();}).setNegativeButton("Cancel",null).show();
 }
}
