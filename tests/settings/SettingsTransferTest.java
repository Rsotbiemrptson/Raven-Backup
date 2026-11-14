package dev.raven.backup;
import org.json.*;import java.io.*;import java.nio.charset.StandardCharsets;
public final class SettingsTransferTest {
 interface Checked {void run()throws Exception;}
 static void reject(Checked c)throws Exception{try{c.run();}catch(Exception expected){return;}throw new AssertionError("Invalid settings accepted");}
 static JSONObject valid()throws Exception{return new JSONObject().put("type","raven-backup-settings").put("schema",1).put("appearance",new JSONObject().put("mode","dark").put("accent","#64C895")).put("preferences",new JSONObject().put("includeApks",true).put("includeExternal",true).put("launchAfter",false).put("allowDifferentSignature",false).put("allowDifferentVersion",false).put("backupOrder",5)).put("schedule",new JSONObject().put("packages",new JSONArray().put("dev.raven.fixture")).put("days",7).put("charging",true).put("idle",false).put("wasEnabled",true));}
 public static void main(String[] args)throws Exception{
 JSONObject good=valid(),parsed=SettingsTransfer.read(new ByteArrayInputStream(good.toString().getBytes(StandardCharsets.UTF_8)));if(!parsed.similar(good))throw new AssertionError("Round trip changed settings");
 if(!SettingsTransfer.review(parsed).contains("Imported as OFF"))throw new AssertionError("Schedule warning missing");
 for(String key:new String[]{"verified","tree","permissions","recovery","rootApproved"}){JSONObject bad=valid().put(key,true);reject(()->SettingsTransfer.validate(bad));}
 for(String key:SettingsTransfer.BOOLEANS){JSONObject bad=valid();bad.getJSONObject("preferences").put(key,"true");reject(()->SettingsTransfer.validate(bad));}
 for(int order:new int[]{-1,6,99}){JSONObject bad=valid();bad.getJSONObject("preferences").put("backupOrder",order);reject(()->SettingsTransfer.validate(bad));}
 JSONObject version=valid().put("schema",2);reject(()->SettingsTransfer.validate(version));JSONObject kind=valid().put("type","backup-archive");reject(()->SettingsTransfer.validate(kind));
 for(String accent:new String[]{"red","#12345","#GGGGGG","#FF112233"}){JSONObject bad=valid();bad.getJSONObject("appearance").put("accent",accent);reject(()->SettingsTransfer.validate(bad));}
 for(String pkg:new String[]{"../bad","dev.raven;reboot","","android"}){JSONObject bad=valid();bad.getJSONObject("schedule").put("packages",new JSONArray().put(pkg));reject(()->SettingsTransfer.validate(bad));}
 JSONObject duplicate=valid();duplicate.getJSONObject("schedule").getJSONArray("packages").put("dev.raven.fixture");reject(()->SettingsTransfer.validate(duplicate));
 JSONObject interval=valid();interval.getJSONObject("schedule").put("days",2);reject(()->SettingsTransfer.validate(interval));
 JSONObject huge=valid().put("appVersion",new String(new char[65536]));reject(()->SettingsTransfer.validate(huge));
 JSONObject noSchedule=valid();noSchedule.remove("schedule");SettingsTransfer.validate(noSchedule);
 reject(()->SettingsTransfer.read(new ByteArrayInputStream(new byte[65537])));reject(()->SettingsTransfer.read(new ByteArrayInputStream("not JSON".getBytes(StandardCharsets.UTF_8))));
 System.out.println("PASS settings round trip, review warning, strict switches/order/theme/schema, unsafe/duplicate schedule apps, unsupported grants/approval fields, no-schedule export, malformed and oversized imports");
 }
}
