package dev.raven.backupfixture;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.system.Os;
import android.widget.TextView;
import java.io.*;
import org.json.JSONObject;

/** Seed/mutate are explicit test operations. Normal launcher only reads restored data. */
public class Seed extends Activity {
 static void write(File file,String value)throws Exception{file.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(file)){out.write(value.getBytes("UTF-8"));out.getFD().sync();}}
 static String read(File file)throws Exception{try(FileInputStream in=new FileInputStream(file)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}}
 public void onCreate(Bundle bundle){boolean hidden=getPackageName().equals("dev.raven.backup.setupfixture");if(hidden)setTheme(android.R.style.Theme_Translucent_NoTitleBar);super.onCreate(bundle);TextView view=new TextView(this);if(hidden){getWindow().setBackgroundDrawableResource(android.R.color.transparent);getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);overridePendingTransition(0,0);}else{view.setPadding(30,30,30,30);view.setTextSize(22);setContentView(view);}
  try{
   String mode=getIntent().getStringExtra("mode");Context de=createDeviceProtectedStorageContext();File seed=new File(getFilesDir(),"nested/seed.txt"),device=new File(de.getFilesDir(),"de-seed.txt"),extra=new File(getFilesDir(),"extra-after-backup.txt");
   if("seed".equals(mode)||"mutate".equals(mode)){
    boolean changed=mode.equals("mutate");write(seed,changed?"Modified after backup\n":"Raven backup fixture: "+getPackageName()+"\n");write(device,changed?"Modified device data\n":"device protected fixture\n");
    Os.chmod(seed.getPath(),changed?0600:0640);Os.chmod(device.getPath(),changed?0600:0640);Os.chmod(seed.getParent(),0750);
    if(!getSharedPreferences("fixture",0).edit().putString("value",changed?"modified-after-backup":"unchanged-original").commit())throw new IOException("Preference commit failed");
    if(changed)write(extra,"Must disappear after original restore\n");else extra.delete();
    write(new File(getCacheDir(),"test-state.txt"),mode);view.setText("Fixture "+mode+" complete");finish();return;
   }
   boolean mutated="mutated".equals(getIntent().getStringExtra("expect"));String seedValue=read(seed),deviceValue=read(device),preference=getSharedPreferences("fixture",0).getString("value",null);
   boolean success=seedValue.equals(mutated?"Modified after backup\n":"Raven backup fixture: "+getPackageName()+"\n")&&deviceValue.equals(mutated?"Modified device data\n":"device protected fixture\n")&&(mutated?"modified-after-backup":"unchanged-original").equals(preference)&&extra.exists()==mutated;
   JSONObject report=new JSONObject().put("package",getPackageName()).put("success",success).put("expect",mutated?"mutated":"original").put("seed",seedValue).put("deviceProtected",deviceValue).put("preference",preference).put("extraFile",extra.exists());
   write(new File(getCacheDir(),"launch-report.json"),report.toString());android.util.Log.i("FixtureVerify",report.toString());view.setText("Backup test fixture\n"+getPackageName()+"\n"+(success?"PASS":"FAIL")+" — "+(mutated?"rollback state":"restored original data")+"\n\n"+report.toString(2));
  }catch(Exception e){view.setText("Fixture verification failed: "+e);android.util.Log.e("FixtureVerify",e.toString());}
 }
}
