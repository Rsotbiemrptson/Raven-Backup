package dev.raven.backup;
import android.app.Instrumentation;
import android.content.Context;
import android.os.*;
import java.io.*;
import org.json.*;

public final class CompatibilityPolicyTest extends Instrumentation {
 interface Check {void run()throws Exception;}
 void require(boolean value,String why){if(!value)throw new AssertionError(why);}
 void reject(Check check)throws Exception{try{check.run();}catch(IOException expected){return;}throw new AssertionError("Unsafe system migration accepted");}
 public void onCreate(Bundle args){super.onCreate(args);start();}
 public void onStart(){Bundle result=new Bundle();try{
  Context c=getTargetContext();BackupEngine e=new BackupEngine(c);require(!OperationService.running()&&!new LiveRestore(c).pointer.exists(),"Operation active");e.probe();
  JSONObject base=new JSONObject().put("package","dev.raven.synthetic.system").put("uid",12345).put("version",1).put("signers",new JSONArray().put("test-signer")).put("sharedUser","").put("systemApp",true);
  JSONObject current=new JSONObject(base.toString()).put("version",2);JSONObject manifest=new JSONObject().put("model",Build.MODEL).put("fingerprint",Build.FINGERPRINT).put("sdk",Build.VERSION.SDK_INT);FirmwareCheck.capture(manifest);
  reject(()->SystemAppData.require(base,current,manifest,false));SystemAppData.require(base,current,manifest,true);
  JSONObject firmware=new JSONObject(manifest.toString()).put("fingerprint","synthetic-other-firmware");firmware.remove("firmwareBase");reject(()->SystemAppData.require(base,current,firmware,false));SystemAppData.require(base,current,firmware,true);
  JSONObject badSign=new JSONObject(current.toString()).put("signers",new JSONArray().put("wrong-key"));reject(()->SystemAppData.require(base,badSign,manifest,true));
  JSONObject badUid=new JSONObject(current.toString()).put("uid",12346);reject(()->SystemAppData.require(base,badUid,manifest,true));
  JSONObject otherModel=new JSONObject(manifest.toString()).put("model","other-model");reject(()->SystemAppData.require(base,current,otherModel,true));
  result.putString("system_migration","PASS strict default blocks changed version/firmware; explicit option permits them; package signer, UID and same-model requirements remain. Synthetic policy test only; no real system app was changed.");
  require(AppLabelPolicy.accepts("u:object_r:app_data_file:s0:c1,c2"),"App label not recognized");
  require(AppLabelPolicy.sourceRoot("u:object_r:vendor_example_app_data:s0:c1,c2"),"Well-formed source root not recognized");
  require(!AppLabelPolicy.accepts("u:object_r:vendor_nonexistent_privileged_type:s0:c1"),"Undeclared destination label accepted");
  require(!AppLabelPolicy.sourceRoot("u:object_r:app_data_file:s0:c99999"),"Invalid category accepted");
  require(AppLabelPolicy.sourceRoot("u:object_r:custom_app_data_file:s0:c1.c3,c256"),"Valid custom type/category range rejected");
  require(!AppLabelPolicy.sourceRoot("u:object_r:app_data_file:s0:c4.c2")&&!AppLabelPolicy.sourceRoot("u:object_r:app_data_file:s0:c1.c1024"),"Invalid category range accepted");
  String mapped=SelinuxLabels.adapt("files/save","u:object_r:vendor_example_app_data:s0:c1,c2","u:object_r:vendor_example_app_data:s0:c1,c2","u:object_r:app_data_file:s0:c3,c4");require(mapped.equals("u:object_r:app_data_file:s0:c3,c4"),"Source root not mapped to destination");
  reject(()->SelinuxLabels.adapt("files/save","u:object_r:system_file:s0","u:object_r:app_data_file:s0:c1,c2","u:object_r:app_data_file:s0:c3,c4"));
  result.putString("labels","PASS firmware seapp_contexts loaded; declared app-data destination types recognized; foreign source root mapped to destination; undeclared destination types, invalid categories and privileged child labels rejected");
  result.putString("result","PASS");finish(-1,result);
 }catch(Throwable failure){result.putString("error",android.util.Log.getStackTraceString(failure));finish(0,result);}}
}
