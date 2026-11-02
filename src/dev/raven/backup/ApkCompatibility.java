package dev.raven.backup;
import java.io.*;import java.util.*;import java.util.zip.*;
/** Reads installers without extracting them or bypassing Android's installer checks. */
final class ApkCompatibility {
 static String nativeReason(List<File> apks,String[] supported)throws IOException{
  Set<String> saved=new TreeSet<>();for(File apk:apks)try(ZipFile z=new ZipFile(apk)){Enumeration<? extends ZipEntry> entries=z.entries();while(entries.hasMoreElements()){String name=entries.nextElement().getName();String[] parts=name.split("/");if(parts.length==3&&parts[0].equals("lib")&&parts[2].endsWith(".so"))saved.add(parts[1]);}}
  if(saved.isEmpty())return null;for(String abi:supported)if(saved.contains(abi))return null;
  return "Saved installer uses "+String.join(", ",saved)+"; this device supports "+String.join(", ",supported)+". A compatible installer is required.";
 }
 static String installReason(String message){if(message==null)return null;
  if(message.contains("INSTALL_FAILED_NO_MATCHING_ABIS")||message.contains("INSTALL_FAILED_CPU_ABI_INCOMPATIBLE"))return "Saved installer is incompatible with this device’s CPU architecture.";
  if(message.contains("INSTALL_FAILED_OLDER_SDK"))return "Saved installer requires a newer Android version.";
  if(message.contains("INSTALL_FAILED_DEPRECATED_SDK_VERSION"))return "Android blocks this installer because it targets an outdated Android version.";
  if(message.contains("INSTALL_FAILED_MISSING_FEATURE"))return "Saved installer requires a device feature that is unavailable.";
  if(message.contains("INSTALL_FAILED_MISSING_SHARED_LIBRARY"))return "Saved installer requires a system library that is unavailable.";
  return null;
 }
}
