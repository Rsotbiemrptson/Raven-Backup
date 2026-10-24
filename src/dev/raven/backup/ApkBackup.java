package dev.raven.backup;
import android.content.pm.*;import android.os.*;import org.json.*;import java.io.*;import java.util.*;
/** Original signed APKs, including installed splits. Installation is a separate confirmed step. */
final class ApkBackup {
 static void capture(BackupEngine e,File job,JSONObject manifest)throws Exception {
  JSONArray apps=manifest.getJSONArray("apps");
  for(int i=0;i<apps.length();i++){
   JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");PackageInfo pi=e.pm.getPackageInfo(pkg,0);captureInstaller(e,app);List<String> sources=new ArrayList<>();sources.add(pi.applicationInfo.sourceDir);if(pi.applicationInfo.splitSourceDirs!=null)Collections.addAll(sources,pi.applicationInfo.splitSourceDirs);
   if(sources.size()>128)throw new IOException("Too many installed APK splits");JSONArray apks=new JSONArray();
   for(int j=0;j<sources.size();j++){String name="app"+i+"-apk"+j+".apk";File file=new File(job,name);try(InputStream in=new FileInputStream(sources.get(j));OutputStream out=new FileOutputStream(file)){BackupEngine.copy(in,out,BackupEngine.MAX);}apks.put(new JSONObject().put("file",name).put("bytes",file.length()).put("sha256",BackupEngine.hash(file)).put("split",j==0?"base":pi.splitNames[j-1]));}
   JSONObject now=e.info(pkg);if(now.getLong("version")!=app.getLong("version")||!now.getJSONArray("signers").toString().equals(app.getJSONArray("signers").toString()))throw new IOException("App changed during backup; retry: "+pkg);app.put("apks",apks);validate(e,job,app,i,new HashSet<>());
  }manifest.put("format",2);BackupEngine.write(new File(job,"manifest.json"),manifest.toString(2));
 }
 static String repairPlayLink(BackupEngine e,String pkg)throws Exception {
  synchronized(LiveRestore.class){
   if(new File(e.c.getFilesDir(),"live-pending.json").exists())throw new IOException("Recover the pending restore first");
   JSONObject before=e.info(pkg);e.pm.getPackageInfo("com.android.vending",0);ApplicationInfo info=e.pm.getApplicationInfo(pkg,0);List<String> paths=new ArrayList<>();paths.add(info.sourceDir);if(info.splitSourceDirs!=null)Collections.addAll(paths,info.splitSourceDirs);long total=0;for(String path:paths)total+=new File(path).length();
   StringBuilder shell=new StringBuilder("result=$(pm install-create -r -i com.android.vending --user 0 -S "+total+")\necho \"$result\"\nsession=$(printf '%s' \"$result\" | sed -n 's/.*\\[\\([0-9]*\\)\\].*/\\1/p')\n[ -n \"$session\" ]\n");
   for(int i=0;i<paths.size();i++)shell.append("if ! pm install-write -S ").append(new File(paths.get(i)).length()).append(" \"$session\" ").append(BackupEngine.q("part"+i+".apk")).append(" ").append(BackupEngine.q(paths.get(i))).append("; then pm install-abandon \"$session\"; exit 1; fi\n");
   shell.append("if ! pm install-commit \"$session\"; then pm install-abandon \"$session\" || true; exit 1; fi\n");
   e.run(e.job(),shell.toString(),false);JSONObject after=e.info(pkg);if(before.getInt("uid")!=after.getInt("uid")||before.getLong("version")!=after.getLong("version")||!before.getJSONArray("signers").toString().equals(after.getJSONArray("signers").toString()))throw new IOException("App identity changed unexpectedly");
   if(!"com.android.vending".equals(e.pm.getInstallSourceInfo(pkg).getInstallingPackageName()))throw new IOException("Installer link was not restored");
   return "Google Play installer link restored. The same signed app and splits were reinstalled with data retained. Close and reopen the app; Google Play still verifies your license.";
  }
 }
 static void captureInstaller(BackupEngine e,JSONObject app)throws Exception {
  String installer=e.pm.getInstallSourceInfo(app.getString("package")).getInstallingPackageName();if(installer==null)return;
  try{PackageInfo info=e.pm.getPackageInfo(installer,PackageManager.GET_SIGNING_CERTIFICATES);if(info.signingInfo==null)return;List<String> hashes=new ArrayList<>();for(android.content.pm.Signature sig:info.signingInfo.getApkContentsSigners())hashes.add(BackupEngine.digest(sig.toByteArray()));Collections.sort(hashes);app.put("installer",installer).put("installerSigners",new JSONArray(hashes));}catch(PackageManager.NameNotFoundException ignored){}
 }
 static String verifiedInstaller(BackupEngine e,JSONObject app)throws Exception {
  String installer=app.optString("installer","");if(!installer.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")||!app.has("installerSigners"))return null;
  try{PackageInfo info=e.pm.getPackageInfo(installer,PackageManager.GET_SIGNING_CERTIFICATES);if(info.signingInfo==null)return null;List<String> hashes=new ArrayList<>();for(android.content.pm.Signature sig:info.signingInfo.getApkContentsSigners())hashes.add(BackupEngine.digest(sig.toByteArray()));Collections.sort(hashes);return new JSONArray(hashes).toString().equals(app.getJSONArray("installerSigners").toString())?installer:null;}catch(PackageManager.NameNotFoundException absent){return null;}
 }
 static void validate(BackupEngine e,File job,JSONObject app,int index,Set<String> expected)throws Exception {
  JSONArray apks=app.optJSONArray("apks");if(apks==null)return;if(apks.length()<1||apks.length()>128)throw new IOException("Invalid APK count");Set<String> splits=new HashSet<>();
  for(int j=0;j<apks.length();j++){JSONObject a=apks.getJSONObject(j);String name="app"+index+"-apk"+j+".apk";if(!name.equals(a.getString("file"))||!splits.add(a.getString("split"))||(j==0&&!a.getString("split").equals("base")))throw new IOException("Invalid APK identity/path");File file=new File(job,name);if(file.length()!=a.getLong("bytes")||!BackupEngine.hash(file).equals(a.getString("sha256")))throw new IOException("APK checksum mismatch");expected.add(name);}
  PackageInfo base=e.pm.getPackageArchiveInfo(new File(job,apks.getJSONObject(0).getString("file")).getPath(),PackageManager.GET_SIGNING_CERTIFICATES);if(base==null||base.signingInfo==null||!base.packageName.equals(app.getString("package"))||base.getLongVersionCode()!=app.getLong("version"))throw new IOException("Base APK package/version mismatch");base.applicationInfo.sourceDir=new File(job,apks.getJSONObject(0).getString("file")).getPath();base.applicationInfo.publicSourceDir=base.applicationInfo.sourceDir;if(!app.has("label")){String label=e.pm.getApplicationLabel(base.applicationInfo).toString();if(!label.equals(base.packageName))app.put("label",label);}List<String> hashes=new ArrayList<>();for(android.content.pm.Signature sig:base.signingInfo.getApkContentsSigners())hashes.add(BackupEngine.digest(sig.toByteArray()));Collections.sort(hashes);if(!new JSONArray(hashes).toString().equals(app.getJSONArray("signers").toString()))throw new IOException("Base APK signing identity mismatch");
 }
 static List<String> missing(BackupEngine e,JSONObject manifest,Set<String> selected)throws Exception {
  FirmwareCheck.validate(manifest);List<String> missing=new ArrayList<>();JSONArray apps=manifest.getJSONArray("apps");
  for(int i=0;i<apps.length();i++){JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");if(!selected.contains(pkg))continue;try{e.pm.getPackageInfo(pkg,0);}catch(PackageManager.NameNotFoundException absent){if(app.optJSONArray("apks")==null)throw new IOException("App is not installed and this backup has no APKs: "+pkg);missing.add(pkg);}}
  return missing;
 }
 static String installMissing(BackupEngine e,File job,JSONObject manifest,Set<String> selected)throws Exception {return installMissing(e,job,manifest,selected,null);}
 static String installMissing(BackupEngine e,File job,JSONObject manifest,Set<String> selected,String approval)throws Exception {
  FirmwareCheck.require(manifest,approval);
  synchronized(LiveRestore.class){if(new File(e.c.getFilesDir(),"live-pending.json").exists())throw new IOException("Recover pending restore first");if(android.os.Process.myUid()>=100000||!((UserManager)e.c.getSystemService(android.content.Context.USER_SERVICE)).isUserUnlocked())throw new IOException("Unlock user 0 first");List<String> missing=missing(e,manifest,selected);JSONArray apps=manifest.getJSONArray("apps");
   for(int i=0;i<apps.length();i++){JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");if(!missing.contains(pkg))continue;JSONArray apks=app.getJSONArray("apks");long total=0;for(int j=0;j<apks.length();j++)total+=apks.getJSONObject(j).getLong("bytes");
    StringBuilder shell=new StringBuilder("result=$(pm install-create --user 0 "+(verifiedInstaller(e,app)==null?"":"-i "+BackupEngine.q(verifiedInstaller(e,app))+" ")+"-S "+total+")\necho \"$result\"\nsession=$(printf '%s' \"$result\" | sed -n 's/.*\\[\\([0-9]*\\)\\].*/\\1/p')\n[ -n \"$session\" ]\n");
    for(int j=0;j<apks.length();j++){JSONObject a=apks.getJSONObject(j);shell.append("if ! pm install-write -S ").append(a.getLong("bytes")).append(" \"$session\" ").append(BackupEngine.q("part"+j+".apk")).append(" ").append(BackupEngine.q(new File(job,a.getString("file")).getPath())).append("; then pm install-abandon \"$session\"; exit 1; fi\n");}
    shell.append("if ! pm install-commit \"$session\"; then pm install-abandon \"$session\" || true; exit 1; fi\n");e.run(e.job(),shell.toString(),false);JSONObject now=e.info(pkg);if(now.getLong("version")!=app.getLong("version")||!now.getJSONArray("signers").toString().equals(app.getJSONArray("signers").toString()))throw new IOException("Installed identity differs from backup: "+pkg);
   }return "Installed "+missing.size()+" missing apps. Data restore still requires confirmation.";
  }
 }
}
