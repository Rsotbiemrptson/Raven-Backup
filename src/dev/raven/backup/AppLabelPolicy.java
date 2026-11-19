package dev.raven.backup;
import java.util.*;
import java.util.regex.*;

/** Recognize app-root types declared by this firmware; never import arbitrary saved types. */
final class AppLabelPolicy {
 private static volatile Set<String> types=Collections.singleton("app_data_file");
 private static volatile boolean loaded;
 static synchronized void load()throws Exception{
  if(loaded)return;
  String policy=RootBridge.call("for f in /system/etc/selinux/plat_seapp_contexts /system_ext/etc/selinux/system_ext_seapp_contexts /product/etc/selinux/product_seapp_contexts /vendor/etc/selinux/vendor_seapp_contexts /odm/etc/selinux/odm_seapp_contexts; do [ ! -f \"$f\" ] || cat \"$f\"; done");
  if(policy.length()>1024*1024)throw new java.io.IOException("App label policy exceeds size limit");
  Set<String> found=new HashSet<>();found.add("app_data_file");
  Pattern field=Pattern.compile("(?:^|\\s)type=([A-Za-z_][A-Za-z0-9_]*)($|\\s)");
  for(String line:policy.split("\\r?\\n")){line=line.split("#",2)[0];Matcher match=field.matcher(line);if(match.find())found.add(match.group(1));}
  types=Collections.unmodifiableSet(found);loaded=true;
 }
 static boolean sourceRoot(String context){
  if(context==null||!context.matches("u:object_r:[A-Za-z_][A-Za-z0-9_]*:s0(:c[0-9]+(\\.c[0-9]+)?(,c[0-9]+(\\.c[0-9]+)?)*)?"))return false;
  int categories=context.indexOf(":s0:");
  if(categories>=0)for(String part:context.substring(categories+4).split(","))try{
   String[] range=part.substring(1).split("\\.c",-1);int first=Integer.parseInt(range[0]),last=range.length==2?Integer.parseInt(range[1]):first;
   if(first>last||last>1023)return false;
  }catch(NumberFormatException invalid){return false;}
  return true;
 }
 static boolean accepts(String context){return sourceRoot(context)&&types.contains(context.split(":",4)[2]);}
}
