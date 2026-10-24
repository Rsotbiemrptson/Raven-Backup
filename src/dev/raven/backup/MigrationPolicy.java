package dev.raven.backup;
/** Human-readable migration information, independent of Android UI. */
final class MigrationPolicy {
 static String display(String value){String clean=value.replaceAll("[\\p{Cntrl}]"," ").trim();return clean.length()>240?clean.substring(0,240)+"…":clean;}
 static String androidVersion(String release,int sdk){
  if(release==null||release.trim().isEmpty()){
   switch(sdk){case 30:release="11";break;case 31:release="12";break;case 32:release="12L";break;case 33:release="13";break;case 34:release="14";break;case 35:release="15";break;case 36:release="16";break;default:release="version not recorded";}
  }return "Android "+display(release)+" (API "+sdk+")";
 }
 static String source(String maker,String model,String release,int sdk,String device,String firmware){
  return "Backup device: "+(maker.trim().isEmpty()?"":display(maker)+" ")+display(model)+(device.trim().isEmpty()?"":" ("+display(device)+")")+"\n"+androidVersion(release,sdk)+"\nFirmware: "+display(firmware);
 }
 static String warning(boolean differentModel,int savedSdk,int currentSdk){
  String text=differentModel?"This backup was made on a different device model and may not restore correctly.":"This backup was made on a different Android version or firmware and may not restore correctly.";
  return text+" A temporary recovery backup of the app’s data is created during restore. Continue only if you accept this risk.";
 }
 static boolean approvalMatches(boolean migration,String expected,String supplied){return !migration||(expected!=null&&!expected.isEmpty()&&expected.equals(supplied));}
}
