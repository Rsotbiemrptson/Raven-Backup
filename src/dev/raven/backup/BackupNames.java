package dev.raven.backup;
import java.text.SimpleDateFormat;import java.util.*;
/** Human-readable filenames, with explicit same-day version numbers. */
final class BackupNames {
 static String stem(String label,Date date){String safe=label.replaceAll("[^\\p{L}\\p{N}._-]","").replaceAll("^\\.+|\\.+$","");if(safe.isEmpty())safe="App";if(safe.length()>80)safe=safe.substring(0,80);return safe+"_"+new SimpleDateFormat("MM-dd-yyyy",Locale.US).format(date);}
 static String available(String stem,Set<String> existing){String name=stem+".zip";int version=2;while(existing.contains(name.toLowerCase(Locale.ROOT)))name=stem+"_"+(version++)+".zip";return name;}
}
