package dev.raven.backup;
public final class MigrationPolicyTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  check(MigrationPolicy.warning(true,33,35).startsWith("This backup was made on a different device model and may not restore correctly."));
  check(MigrationPolicy.warning(false,33,35).contains("different Android version or firmware"));
  check(MigrationPolicy.warning(false,35,33).contains("temporary recovery backup"));
  check(!MigrationPolicy.warning(true,33,35).contains("Keystore"));
  check(MigrationPolicy.source("","Odin2","",33,"","legacy-build").equals("Backup device: Odin2\nAndroid 13 (API 33)\nFirmware: legacy-build"));
  check(MigrationPolicy.source("AYN","Odin3","15",35,"hardware","base").contains("AYN Odin3 (hardware)\nAndroid 15 (API 35)"));
  check(MigrationPolicy.androidVersion("",99).contains("not recorded"));
  check(MigrationPolicy.display("Spoof\n\r\tdevice").equals("Spoof   device"));
  check(MigrationPolicy.display(new String(new char[300]).replace('\0','x')).length()==241);
  check(MigrationPolicy.approvalMatches(false,"key",null));
  check(!MigrationPolicy.approvalMatches(true,"key",null));
  check(!MigrationPolicy.approvalMatches(true,"changed-device","old-device"));
  check(!MigrationPolicy.approvalMatches(true,"",""));
  check(MigrationPolicy.approvalMatches(true,"reviewed-plan","reviewed-plan"));
  System.out.println("PASS migration warnings, upgrade/downgrade, legacy version display, untrusted display text and per-plan consent invalidation.");
 }
}
