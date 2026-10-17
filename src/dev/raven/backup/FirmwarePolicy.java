package dev.raven.backup;
/** Firmware compatibility uses the real framework/vendor identity when available. */
final class FirmwarePolicy {
 static final String ODIN_STOCK="qti/kalama/kalama:13/TKQ1.230531.001/eng.Odin2.20240724.123224:user/release-keys";
 static final String ODIN_SPOOF="google/blazer/blazer:16/BP4A.260205.001/14624666:user/release-keys";
 static final String ODIN_SYSTEM="qti/qssi/qssi:13/TKQ1.230531.001/Odin207241229:user/release-keys";
 static final String ODIN_VENDOR="qti/kalama/kalama:13/TKQ1.230531.001/Odin207241315:user/release-keys";
 static boolean compatible(String saved,String model,int sdk,String system,String vendor,String current,String currentModel,int currentSdk,String currentSystem,String currentVendor){
  if(!model.equals(currentModel)||sdk!=currentSdk)return false;
  if(!system.isEmpty()||!vendor.isEmpty())return !system.isEmpty()&&!vendor.isEmpty()&&system.equals(currentSystem)&&vendor.equals(currentVendor);
  if(saved.equals(current))return true;
  // Legacy archives lack base identities. Recognize only this documented stock-318 spoof pair.
  boolean known=(saved.equals(ODIN_SPOOF)&&current.equals(ODIN_STOCK))||(saved.equals(ODIN_STOCK)&&current.equals(ODIN_SPOOF));
  return known&&model.equals("Odin2")&&sdk==33&&ODIN_SYSTEM.equals(currentSystem)&&ODIN_VENDOR.equals(currentVendor);
 }
}
