package dev.raven.backup;

import org.json.*;
import java.io.*;
import java.util.*;

/** Adapts only ordinary app-owned data to the current installed identity. */
final class RestorePreparation {
 static String prepare(BackupEngine engine,File job,JSONObject manifest)throws Exception {
  FirmwareCheck.require(manifest);
  StringBuilder description=new StringBuilder();if(!android.os.Build.FINGERPRINT.equals(manifest.getString("fingerprint")))description.append(manifest.has("firmwareBase")?"Framework/vendor identities match despite the reported fingerprint change.\n":"Recognized Odin 2 stock-318 fingerprint spoof; actual model and Android version match.\n");JSONArray apps=manifest.getJSONArray("apps");
  for(int i=0;i<apps.length();i++){
   JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");JSONObject current=engine.info(pkg);
   if(current.getLong("version")!=app.getLong("version"))throw new IOException("App version differs for "+pkg+". Install the backed-up version first.");
   if(!current.getJSONArray("signers").toString().equals(app.getJSONArray("signers").toString()))throw new IOException("Signing identity differs for "+pkg);
   int oldUid=app.getInt("uid"),newUid=current.getInt("uid");if(oldUid<10000||oldUid>=100000)throw new IOException("Invalid saved app identity");
   description.append(engine.pm.getApplicationLabel(engine.pm.getApplicationInfo(pkg,0))).append(" ("+pkg+")\n");
   if(oldUid!=newUid)description.append("Ownership will be adapted to this installation.\n");
   JSONArray payloads=app.getJSONArray("payloads");boolean ce=false;
   for(int j=0;j<payloads.length();j++){
    JSONObject payload=payloads.getJSONObject(j);String domain=payload.getString("domain");if(domain.equals("ce"))ce=true;
    String target=LiveRestore.data(pkg,domain),context=RootBridge.call("ls -Zd "+BackupEngine.q(target)).trim().split("\\s+")[0];
    if(!context.matches("u:object_r:app_data_file:s0(:c[0-9]+(,c[0-9]+)*)?"))throw new IOException("App storage is not ready or uses unsupported labels: "+pkg+". Open the app once, then retry.");
    List<TarGuard.Record> records=TarGuard.records(new File(job,payload.getString("tar")));
    for(TarGuard.Record record:records)if(record.uid!=oldUid||record.gid!=oldUid||(record.mode&~0777L)!=0)throw new IOException("Unsupported saved ownership or special permissions in "+pkg);
    File sidecar=new File(job,payload.getString("labels"));String[] pairs=BackupEngine.read(sidecar,32*1024*1024).split("\0",-1);String rootContext=null;
    for(int k=0;k<pairs.length-1;k+=2)if(TarGuard.path(pairs[k]).isEmpty())rootContext=pairs[k+1];
    if(rootContext==null||!rootContext.matches("u:object_r:app_data_file:s0(:c[0-9]+(,c[0-9]+)*)?"))throw new IOException("Unsupported saved app security context");
    StringBuilder adapted=new StringBuilder();for(int k=0;k<pairs.length-1;k+=2){String normalized=TarGuard.path(pairs[k]);if(!pairs[k+1].equals(rootContext))for(TarGuard.Record record:records)if(record.path.equals(normalized)&&record.directory)throw new IOException("Category-free profile must be a regular file");String mapped=SelinuxLabels.adapt(pairs[k],pairs[k+1],rootContext,context);adapted.append(pairs[k]).append('\0').append(mapped).append('\0');}
    BackupEngine.write(sidecar,adapted.toString());
    File tar=new File(job,payload.getString("tar"));if(oldUid!=newUid)remapTar(tar,oldUid,newUid);
    payload.put("sha256",BackupEngine.hash(tar)).put("labelsSha256",BackupEngine.hash(sidecar));
   }
   if(!ce)throw new IOException("Credential-protected app data missing: "+pkg);app.put("uid",newUid);
  }
  BackupEngine.write(new File(job,"manifest.json"),manifest.toString(2));return description.toString();
 }
 static void remapTar(File tar,int oldUid,int newUid)throws Exception {
  TarGuard.validate(tar);
  try(RandomAccessFile file=new RandomAccessFile(tar,"rw")){
   byte[] header=new byte[512];while(file.getFilePointer()<file.length()){
    long start=file.getFilePointer();file.readFully(header);boolean zero=true;for(byte b:header)if(b!=0){zero=false;break;}if(zero)break;
    if(TarGuard.oct(header,108,8)!=oldUid||TarGuard.oct(header,116,8)!=oldUid)throw new IOException("Unrecognized tar owner during remapping");
    long size=TarGuard.oct(header,124,12);byte[] uid=String.format(Locale.ROOT,"%07o",newUid).getBytes("US-ASCII");
    for(int offset:new int[]{108,116}){Arrays.fill(header,offset,offset+8,(byte)0);System.arraycopy(uid,0,header,offset,uid.length);}
    Arrays.fill(header,265,329,(byte)0);Arrays.fill(header,148,156,(byte)32);int sum=0;for(byte b:header)sum+=b&255;byte[] checksum=String.format(Locale.ROOT,"%06o",sum).getBytes("US-ASCII");System.arraycopy(checksum,0,header,148,checksum.length);header[154]=0;header[155]=32;
    file.seek(start);file.write(header);file.seek(start+512+((size+511)/512)*512);
   }file.getFD().sync();
  }TarGuard.validate(tar);
 }
}
