package dev.raven.backup;

import org.json.*;
import java.io.*;
import java.util.*;

/** Adapts only ordinary app-owned data to the current installed identity. */
final class RestorePreparation {
 static boolean sameSigners(JSONObject saved,JSONObject installed)throws JSONException {
  return saved.getJSONArray("signers").toString().equals(installed.getJSONArray("signers").toString());
 }
 static String destinationIdentity(JSONObject installed)throws JSONException {
  return installed.getString("package")+":"+installed.getInt("uid")+":"
   +installed.getLong("version")+":"+installed.getJSONArray("signers");
 }
 static String prepare(BackupEngine engine,File job,JSONObject manifest,Map<String,String> approvedSigners)throws Exception {
  FirmwareCheck.validate(manifest);
  StringBuilder description=new StringBuilder();JSONArray apps=manifest.getJSONArray("apps");
  for(int i=0;i<apps.length();i++){
   JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");JSONObject current=engine.info(pkg);SystemAppData.require(app,current,manifest);
   String approved=approvedSigners.get(pkg);
   boolean differentSignature=RestorePreferences.differentSignature(app,current);
   boolean differentVersion=RestorePreferences.differentVersion(app,current);
   if(approved!=null&&!approved.equals(destinationIdentity(current)))
    throw new IOException("Installed app changed after compatibility review: "+pkg);
   if(differentSignature||differentVersion){
    String warning=RestorePreferences.warning(engine.c,app,current);
    if(warning!=null)throw new IOException(warning);
    if(approved==null)throw new IOException("Review the installed app again: "+pkg);
   }
   int oldUid=app.getInt("uid"),newUid=current.getInt("uid");if((oldUid<10000||oldUid>=100000)&&!SystemAppData.sharedAssistant(pkg,oldUid,app.optString("sharedUser")))throw new IOException("Invalid saved app identity");
   description.append(engine.pm.getApplicationLabel(engine.pm.getApplicationInfo(pkg,0))).append(" ("+pkg+")\n");
   if(differentSignature||differentVersion)description.append("Data-only restore into the installed copy.\n");
   if(oldUid!=newUid)description.append("Ownership will be adapted to this installation.\n");
   JSONArray payloads=app.getJSONArray("payloads");boolean ce=false;
   for(int j=0;j<payloads.length();j++){
    JSONObject payload=payloads.getJSONObject(j);String domain=payload.getString("domain");if(domain.equals("ce"))ce=true;
    String target=LiveRestore.data(pkg,domain),context=RootBridge.call("ls -Zd "+BackupEngine.q(target)).trim().split("\\s+")[0];
    if(!SystemAppData.context(current,context))throw new IOException("App storage is not ready or uses unsupported labels: "+pkg+". Open the app once, then retry.");
    List<TarGuard.Record> records=TarGuard.records(new File(job,payload.getString("tar")),pkg);
    for(TarGuard.Record record:records)if(record.uid!=oldUid||record.gid!=oldUid||(record.mode&~0777L)!=0)throw new IOException("Unsupported saved ownership or special permissions in "+pkg);
    File sidecar=new File(job,payload.getString("labels"));String[] pairs=BackupEngine.read(sidecar,32*1024*1024).split("\0",-1);String rootContext=null;
    for(int k=0;k<pairs.length-1;k+=2)if(TarGuard.path(pairs[k]).isEmpty())rootContext=pairs[k+1];
    if(!SystemAppData.context(app,rootContext))throw new IOException("Unsupported saved app security context");
    StringBuilder adapted=new StringBuilder();for(int k=0;k<pairs.length-1;k+=2){String normalized=TarGuard.path(pairs[k]);if(!pairs[k+1].equals(rootContext))for(TarGuard.Record record:records)if(record.path.equals(normalized)&&(record.directory||record.symlink||record.fifo))throw new IOException("Category-free profile must be a regular file");String mapped=SelinuxLabels.adapt(pairs[k],pairs[k+1],rootContext,context);adapted.append(pairs[k]).append('\0').append(mapped).append('\0');}
    BackupEngine.write(sidecar,adapted.toString());
    File tar=new File(job,payload.getString("tar"));if(oldUid!=newUid)TarGuard.remapOwners(tar,oldUid,newUid,pkg);
    payload.put("sha256",BackupEngine.hash(tar)).put("labelsSha256",BackupEngine.hash(sidecar));
   }
   if(!ce&&!SystemAppData.system(app))throw new IOException("Credential-protected app data missing: "+pkg);app.put("uid",newUid);
   // Pin the staged plan to the approved installation for the final compatibility check.
   if(differentSignature||differentVersion){app.put("signers",current.getJSONArray("signers"));app.put("version",current.getLong("version"));app.remove("apks");}
  }
  BackupEngine.write(new File(job,"manifest.json"),manifest.toString(2));return description.toString();
 }
 static void remapTar(File tar,int oldUid,int newUid)throws Exception{TarGuard.remapOwners(tar,oldUid,newUid);}
}
