package dev.raven.backup;
final class RootBridge {
 static String system="target-system",vendor="target-vendor";
 static String call(String command){if(command.equals("getprop ro.system.build.fingerprint"))return system;if(command.equals("getprop ro.vendor.build.fingerprint"))return vendor;throw new AssertionError(command);}
}
final class BackupEngine {
 static String digest(byte[] input)throws Exception{StringBuilder out=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(input))out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();}
}
