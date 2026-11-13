package dev.raven.backup;

import java.io.IOException;

/** Explicit root-job result; a missing response never means an empty directory. */
final class ExternalProbe {
 private static String q(String s){return "'"+s.replace("'", "'\\''")+"'";}
 static String command(String source,String entries,String size){
  String s=q(source),e=q(entries),z=q(size);
  return "if [ ! -e "+s+" ] && [ ! -L "+s+" ]; then\n"
   +"echo RAVEN_EXTERNAL_ABSENT\n"
   +"else\n[ -d "+s+" ] && [ ! -L "+s+" ] || { echo 'External app path is not a regular directory'; exit 1; }\n"
   +"find "+s+" -mindepth 1 ! -type d -print -quit > "+e+"\n"
   +"if [ ! -s "+e+" ]; then echo RAVEN_EXTERNAL_EMPTY; else\n"
   +"du -sk "+s+" > "+z+"\n"
   +"read -r count rest < "+z+"\n"
   +"case \"$count\" in ''|*[!0-9]*) echo 'External app size is unavailable'; exit 1;; esac\n"
   +"printf 'RAVEN_EXTERNAL_PRESENT %s\\n' \"$count\"\nfi\nfi\n";
 }
 static long bytes(String result)throws IOException {
  String value=result.trim();
  if(value.equals("RAVEN_EXTERNAL_ABSENT")||value.equals("RAVEN_EXTERNAL_EMPTY"))return -1;
  if(!value.matches("RAVEN_EXTERNAL_PRESENT [0-9]+"))
   throw new IOException("Could not confirm external app files or their size. No external files were silently skipped.");
  try{return Math.multiplyExact(Long.parseLong(value.substring("RAVEN_EXTERNAL_PRESENT ".length())),1024);}
  catch(NumberFormatException|ArithmeticException invalid){throw new IOException("External app size is invalid or too large",invalid);}
 }
}
