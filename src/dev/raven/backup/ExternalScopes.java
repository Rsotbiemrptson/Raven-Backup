package dev.raven.backup;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.os.storage.*;
import org.json.*;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

/** Explicit external scopes. Archive metadata never supplies an executable filesystem path. */
final class ExternalScopes {
 static final String[] KEYS={"includeExternal","includeInternalObb","includeRemovableData","includeRemovableObb"};
 static final String[] LABELS={"Internal app data files","Internal OBB files","Removable-card app data files","Removable-card OBB files"};
 static boolean any(Context c){for(String key:KEYS)if(RestorePreferences.prefs(c).getBoolean(key,true))return true;return false;}
 static String selection(Context c){List<String> selected=new ArrayList<>();for(int i=0;i<KEYS.length;i++)if(RestorePreferences.prefs(c).getBoolean(KEYS[i],true))selected.add(LABELS[i]);return selected.isEmpty()?"None selected.":String.join(", ",selected)+". Missing or empty folders are omitted.";}
 static String description(JSONObject manifest)throws Exception{JSONObject metadata=ExternalData.metadata(manifest);if(metadata==null)return "";if(!metadata.has("scopes"))return "Includes primary Android/data app files.\n";StringBuilder text=new StringBuilder("Included app storage:\n");JSONArray parts=metadata.getJSONArray("scopes");boolean card=false;for(int i=0;i<parts.length();i++){JSONObject part=parts.getJSONObject(i);String volume=part.getString("volume");card|=!volume.equals("primary");text.append(volume.equals("primary")?"Internal storage":"Source card "+volume).append(part.getString("kind").equals("data")?": Android/data files\n":": whole Android/obb app folder\n");}if(card)text.append("A matching mounted card, or the sole available destination card, is required.\n");return text.toString();}
 static Map<String,File> volumes(Context c)throws IOException{
  Map<String,File> result=new LinkedHashMap<>();result.put("primary",new File("/storage/emulated/0"));
  StorageManager manager=c.getSystemService(StorageManager.class);
  for(StorageVolume volume:manager.getStorageVolumes())if(!volume.isPrimary()&&Environment.MEDIA_MOUNTED.equals(volume.getState())){
   String uuid=volume.getUuid();File directory=volume.getDirectory();
   if(uuid!=null&&uuid.matches("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}")&&directory!=null&&directory.getCanonicalFile().equals(new File("/storage",uuid).getCanonicalFile()))result.put(uuid,new File("/storage",uuid));
  }
  return result;
 }
 static String raw(String volume)throws IOException{
  if(volume.equals("primary"))return "/data/media/0";
  if(!volume.matches("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}"))throw new IOException("Invalid removable volume");return "/mnt/media_rw/"+volume;
 }
 static String destination(String volume,String kind,String pkg)throws IOException{
  if(!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")||pkg.equals("dev.raven.backup"))throw new IOException("Invalid external app package");
  if(kind.equals("data"))return raw(volume)+"/Android/data/"+pkg+"/files";
  if(kind.equals("obb"))return raw(volume)+"/Android/obb/"+pkg;
  throw new IOException("Invalid external scope kind");
 }
 static JSONArray validate(JSONArray scopes)throws Exception{
  if(scopes.length()<1||scopes.length()>32)throw new IOException("Invalid external scope count");
  Set<String> identities=new HashSet<>();
  for(int i=0;i<scopes.length();i++){
   JSONObject part=scopes.getJSONObject(i);if(!part.getString("id").equals("scope-"+i))throw new IOException("Invalid external scope ID");
   String volume=part.getString("volume"),kind=part.getString("kind");raw(volume);destination(volume,kind,"dev.raven.scopefixture");
   if(!identities.add(volume+":"+kind))throw new IOException("Duplicate external scope");
   if(part.has("bytes")&&(part.getLong("bytes")<512||part.getLong("bytes")>ExternalTar.LIMIT))throw new IOException("Invalid scoped size");
  }
  return scopes;
 }
 static JSONArray sources(BackupEngine e,Uri parent,String pkg)throws Exception{
  JSONArray parts=new JSONArray();long total=0;Map<String,File> volumes=volumes(e.c);
  for(String volume:volumes.keySet())for(String kind:new String[]{"data","obb"}){
   int option=(volume.equals("primary")?0:2)+(kind.equals("data")?0:1);
   if(!RestorePreferences.prefs(e.c).getBoolean(KEYS[option],true))continue;
   String relative="Android/"+(kind.equals("data")?"data/"+pkg+"/files":"obb/"+pkg);
   if(ExternalData.localFolder(parent).getCanonicalFile().toPath().startsWith(new File(volumes.get(volume),relative).getCanonicalFile().toPath()))throw new IOException("Choose a backup folder outside the app data being backed up");
   String source=destination(volume,kind,pkg);File probe=e.job();
   long bytes=ExternalProbe.bytes(e.run(probe,ExternalProbe.command(source,new File(probe,"entries").getPath(),new File(probe,"size").getPath()),false));
   if(bytes<0)continue;total=Math.addExact(total,bytes);if(total>ExternalTar.LIMIT)throw new IOException("Combined external files exceed 512 GiB");
   parts.put(new JSONObject().put("id","scope-"+parts.length()).put("volume",volume).put("kind",kind));
  }
  if(parts.length()>0&&ExternalData.localFolder(parent).getUsableSpace()<total+64L*1024*1024)throw new IOException("Backup drive needs room for the selected external files");
  return parts;
 }
 static JSONObject transaction(BackupEngine e,LiveRestore.Plan plan)throws Exception{
  JSONObject meta=ExternalData.metadata(plan.manifest);JSONArray scopes=validate(meta.getJSONArray("scopes"));
  String pkg=plan.manifest.getJSONArray("apps").getJSONObject(0).getString("package");Map<String,File> mounted=volumes(e.c);JSONArray parts=new JSONArray();
  Set<String> used=new HashSet<>();Map<String,String> remap=new HashMap<>();
  for(int i=0;i<scopes.length();i++){
   JSONObject source=scopes.getJSONObject(i);String volume=source.getString("volume");
   if(!mounted.containsKey(volume)){
    if(!remap.containsKey(volume)){
     List<String> cards=new ArrayList<>(mounted.keySet());cards.remove("primary");
     if(cards.size()!=1||(!remap.isEmpty()&&!remap.containsValue(cards.get(0))))throw new IOException("Insert the backup's removable card or one unambiguous destination card before restoring");
     if(remap.containsValue(cards.get(0)))throw new IOException("Multiple source cards cannot be merged into one destination card");remap.put(volume,cards.get(0));
    }volume=remap.get(volume);
   }
   String kind=source.getString("kind");destination(volume,kind,pkg);if(!used.add(volume+":"+kind))throw new IOException("External restore destinations overlap");
   parts.put(new JSONObject().put("id",source.getString("id")).put("volume",volume).put("kind",kind).put("bytes",source.getLong("bytes")));
  }
  return new JSONObject().put("id",UUID.randomUUID().toString()).put("package",pkg).put("parts",parts);
 }
 static String transactionId(JSONObject t)throws Exception{String id=t.getString("id");if(!UUID.fromString(id).toString().equals(id))throw new IOException("Invalid external transaction");return id;}
 static String rawStage(JSONObject t,JSONObject part)throws Exception{return raw(part.getString("volume"))+"/Android/data/dev.raven.backup/files/.raven-stage-"+transactionId(t)+"-"+part.getString("id");}
 static String old(JSONObject t,JSONObject part)throws Exception{
  String dest=destination(part.getString("volume"),part.getString("kind"),t.getString("package"));return new File(dest).getParent()+"/.raven-old-"+transactionId(t)+"-"+part.getString("id");
 }
 static Map<String,Path> stages(BackupEngine e,JSONObject t,boolean create)throws Exception{
  Map<String,File> mounted=volumes(e.c);Map<String,Path> routes=new LinkedHashMap<>();Map<String,Long> needed=new HashMap<>();JSONArray parts=t.getJSONArray("parts");
  // Creates only Raven's own app-specific directories; no target app data is touched.
  e.c.getExternalFilesDirs(null);
  for(int i=0;i<parts.length();i++){JSONObject part=parts.getJSONObject(i);String volume=part.getString("volume");if(!mounted.containsKey(volume))throw new IOException("Restore volume is no longer mounted");needed.put(volume,Math.addExact(needed.getOrDefault(volume,0L),part.getLong("bytes")));}
  for(Map.Entry<String,Long> n:needed.entrySet())if(create&&mounted.get(n.getKey()).getUsableSpace()<n.getValue()+64L*1024*1024)throw new IOException("Free space on "+n.getKey()+" is insufficient for safe external restore staging");
  for(int i=0;i<parts.length();i++){
   JSONObject part=parts.getJSONObject(i);String relative="Android/data/dev.raven.backup/files/.raven-stage-"+transactionId(t)+"-"+part.getString("id");File stage=new File(mounted.get(part.getString("volume")),relative);
   if(create&&!stage.mkdir())throw new IOException("Cannot create staging folder on "+part.getString("volume"));routes.put(part.getString("id"),stage.toPath());
  }return routes;
 }
 static String volumeGuard(String volume)throws IOException{String root=raw(volume);return "[ -d "+q(root)+" ]\n[ ! -L "+q(root)+" ]\n"+(volume.equals("primary")?"":"grep -F "+q(" "+root+" ")+" /proc/self/mounts > /dev/null\n");}
 static String apply(JSONObject t,File control)throws Exception{
  StringBuilder command=new StringBuilder();JSONArray parts=t.getJSONArray("parts");
  for(int i=0;i<parts.length();i++){
   JSONObject p=parts.getJSONObject(i);String dest=destination(p.getString("volume"),p.getString("kind"),t.getString("package")),parent=new File(dest).getParent();String stage=rawStage(t,p),old=old(t,p),marker=new File(control,"external-swap-"+p.getString("id")).getPath();
   command.append(volumeGuard(p.getString("volume"))).append("[ ! -L ").append(q(parent)).append(" ]\nmkdir -p ").append(q(parent)).append("\n[ -d ").append(q(stage)).append(" ]\n[ ! -L ").append(q(stage)).append(" ]\n[ ! -e ").append(q(old)).append(" ]\n[ ! -L ").append(q(old)).append(" ]\n[ ! -L ").append(q(dest)).append(" ]\n[ \"$(stat -c %d ").append(q(stage)).append(")\" = \"$(stat -c %d ").append(q(parent)).append(")\" ]\n[ -z \"$(find ").append(q(stage)).append(" ! -type f ! -type d -print -quit)\" ]\n")
    .append("if [ -e ").append(q(dest)).append(" ]; then [ -d ").append(q(dest)).append(" ]; echo present > ").append(q(marker)).append("; else echo absent > ").append(q(marker)).append("; fi\nsync\nif [ -d ").append(q(dest)).append(" ]; then mv -T -- ").append(q(dest)).append(" ").append(q(old)).append("; fi\nmv -T -- ").append(q(stage)).append(" ").append(q(dest)).append("\n");
   if(p.getString("volume").equals("primary"))command.append("chown -R \"$(stat -c %u:%g ").append(q("/data/user/0/"+t.getString("package"))).append(")\" ").append(q(dest)).append("\nrestorecon -RF ").append(q(dest)).append("\n");
   command.append("sync\n");
  }return command.toString();
 }
 static String undo(JSONObject t,File control)throws Exception{
  StringBuilder command=new StringBuilder();JSONArray parts=t.getJSONArray("parts");
  for(int i=parts.length()-1;i>=0;i--){JSONObject p=parts.getJSONObject(i);String dest=destination(p.getString("volume"),p.getString("kind"),t.getString("package")),stage=rawStage(t,p),old=old(t,p),marker=new File(control,"external-swap-"+p.getString("id")).getPath();
   command.append("if [ -f ").append(q(marker)).append(" ]; then\n").append(volumeGuard(p.getString("volume"))).append("[ ! -L ").append(q(dest)).append(" ]\n[ ! -L ").append(q(stage)).append(" ]\n[ ! -L ").append(q(old)).append(" ]\ncase $(cat ").append(q(marker)).append(") in\npresent) if [ -d ").append(q(old)).append(" ]; then if [ -d ").append(q(dest)).append(" ]; then [ ! -e ").append(q(stage)).append(" ]; mv -T -- ").append(q(dest)).append(" ").append(q(stage)).append("; fi; mv -T -- ").append(q(old)).append(" ").append(q(dest)).append("; fi; [ -d ").append(q(dest)).append(" ];;\nabsent) if [ -d ").append(q(dest)).append(" ]; then [ ! -e ").append(q(stage)).append(" ]; mv -T -- ").append(q(dest)).append(" ").append(q(stage)).append("; fi;;\n*) exit 1;;\nesac\nsync\nfi\n");
  }return command.toString();
 }
 static void cleanup(BackupEngine e,JSONObject t)throws Exception{JSONArray parts=t.getJSONArray("parts");StringBuilder command=new StringBuilder();for(int i=0;i<parts.length();i++){JSONObject part=parts.getJSONObject(i);command.append(volumeGuard(part.getString("volume"))).append("rm -rf -- ").append(q(rawStage(t,part))).append(" ").append(q(old(t,part))).append("\n");}e.runLong(e.job(),command.toString());}
 private static String q(String value){return BackupEngine.q(value);}
}
