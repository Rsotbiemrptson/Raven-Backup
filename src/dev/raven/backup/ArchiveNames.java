package dev.raven.backup;
import java.util.*;
final class ArchiveNames {
 static boolean backup(String name){if(name==null)return false;String value=name.toLowerCase(Locale.ROOT);return value.endsWith(".zip")||value.endsWith(".rbk");}
 static String available(String stem,Set<String> names,boolean encrypted){String extension=encrypted?".rbk":".zip";String result=stem+extension;int n=2;while(names.contains(result.toLowerCase(Locale.ROOT)))result=stem+"_"+(n++)+extension;return result;}
}
