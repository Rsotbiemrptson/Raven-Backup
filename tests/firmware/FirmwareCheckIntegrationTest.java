package dev.raven.backup;
import org.json.JSONObject;
public final class FirmwareCheckIntegrationTest {
 interface Action {void run()throws Exception;}
 static void check(boolean value){if(!value)throw new AssertionError();}
 static void rejects(Action action)throws Exception{try{action.run();}catch(java.io.IOException|org.json.JSONException expected){return;}throw new AssertionError("Expected rejection");}
 static JSONObject backup()throws Exception{return new JSONObject().put("model","Odin3").put("sdk",35).put("fingerprint","target-fingerprint").put("firmwareBase",new JSONObject().put("system","target-system").put("vendor","target-vendor"));}
 public static void main(String[] args)throws Exception{
  JSONObject same=backup();FirmwareCheck.require(same);check(FirmwareCheck.warning(same).isEmpty());
  JSONObject legacy=new JSONObject().put("model","Odin3").put("sdk",35).put("fingerprint","target-fingerprint");FirmwareCheck.require(legacy);check(FirmwareCheck.source(legacy).contains("Android 15 (API 35)"));
  JSONObject migration=backup().put("model","Odin2").put("sdk",33).put("fingerprint","source-fingerprint");
  migration.getJSONObject("firmwareBase").put("system","source-system").put("vendor","source-vendor");
  String source=migration.toString();check(FirmwareCheck.warning(migration).contains("different device model"));check(FirmwareCheck.warning(migration).contains("Odin2\nAndroid 13 (API 33)"));
  rejects(()->FirmwareCheck.require(migration));rejects(()->FirmwareCheck.require(migration,"unreviewed"));
  String approval=FirmwareCheck.approvalKey(migration);FirmwareCheck.require(migration,approval);check(source.equals(migration.toString()));
  RootBridge.vendor="changed-target";rejects(()->FirmwareCheck.require(migration,approval));RootBridge.vendor="target-vendor";
  migration.getJSONObject("firmwareBase").put("system","changed-source");rejects(()->FirmwareCheck.require(migration,approval));
  JSONObject newer=backup().put("sdk",36);check(FirmwareCheck.warning(newer).contains("temporary recovery backup"));FirmwareCheck.require(newer,FirmwareCheck.approvalKey(newer));
  JSONObject older=backup().put("sdk",33);check(FirmwareCheck.warning(older).contains("different Android version or firmware"));rejects(()->FirmwareCheck.require(older));FirmwareCheck.require(older,FirmwareCheck.approvalKey(older));
  JSONObject malformed=backup().put("firmwareBase",new JSONObject().put("system","partial"));rejects(()->FirmwareCheck.require(malformed,"anything"));
  JSONObject invalid=backup().put("sdk",0);rejects(()->FirmwareCheck.require(invalid,"anything"));
  JSONObject nullBase=backup().put("firmwareBase",JSONObject.NULL);rejects(()->FirmwareCheck.require(nullBase,"anything"));
  check(!FirmwareCheck.approvalKey(older).equals(FirmwareCheck.approvalKey(newer)));
  System.out.println("PASS real FirmwareCheck: same/legacy, migration confirmation, newer/older Android, changed source/target invalidation, malformed identity rejection, source preservation. Platform identities simulated; no device writes.");
 }
}
