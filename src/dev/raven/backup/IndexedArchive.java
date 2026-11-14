package dev.raven.backup;
import android.net.Uri;import android.os.ParcelFileDescriptor;import android.system.*;import org.json.*;import java.io.*;import java.util.*;import java.util.zip.*;
/** Review a seekable ZIP without reading its large external entry. Restore verifies that entry while staging. */
final class IndexedArchive {
 static final class Stamp {
  final long device,inode,bytes,modified;
  Stamp(StructStat s){device=s.st_dev;inode=s.st_ino;bytes=s.st_size;modified=s.st_mtime;}
  boolean matches(StructStat s){return device==s.st_dev&&inode==s.st_ino&&bytes==s.st_size&&modified==s.st_mtime;}
 }
 static final class Result{JSONObject manifest;Stamp stamp;String headerIdentity;boolean metadataOnly;}
 static void unchanged(BackupEngine e,LiveRestore.Plan plan)throws Exception{
  if(plan.sourceStamp==null)return;
  try(ParcelFileDescriptor fd=e.c.getContentResolver().openFileDescriptor(plan.source,"r")){if(fd==null||!plan.sourceStamp.matches(Os.fstat(fd.getFileDescriptor())))throw new IOException("Backup changed after review. Review it again.");}
 }
 static long extract(ZipFile zip,ZipEntry entry,OutputStream out,long cap)throws Exception{
  if(entry.getSize()<0||entry.getSize()>cap||entry.getCrc()<0)throw new IOException("Invalid or oversized ZIP entry: "+entry.getName());
  CRC32 crc=new CRC32();long bytes;try(InputStream in=new CheckedInputStream(zip.getInputStream(entry),crc)){bytes=BackupEngine.copy(in,out,Math.min(cap,entry.getSize()));}
  if(bytes!=entry.getSize()||crc.getValue()!=entry.getCrc())throw new IOException("ZIP entry checksum mismatch: "+entry.getName());return bytes;
 }
 static void stage(BackupEngine e,LiveRestore.Plan plan,JSONObject external,File stage)throws Exception{
  try(ParcelFileDescriptor fd=e.c.getContentResolver().openFileDescriptor(plan.source,"r")){
   if(fd==null||!plan.sourceStamp.matches(Os.fstat(fd.getFileDescriptor())))throw new IOException("Backup changed after review");
   try(ZipFile zip=new ZipFile("/proc/self/fd/"+fd.getFd())){
    ZipEntry entry=zip.getEntry(ExternalData.ENTRY);if(entry==null||entry.getSize()!=external.getLong("bytes")||entry.getCrc()<0)throw new IOException("External ZIP entry changed");
    CRC32 crc=new CRC32();try(InputStream in=new CheckedInputStream(zip.getInputStream(entry),crc)){ExternalData.read(in,external,stage);}
    if(crc.getValue()!=entry.getCrc())throw new ExternalTar.InvalidData("External ZIP entry checksum mismatch");
   }
   if(!plan.sourceStamp.matches(Os.fstat(fd.getFileDescriptor())))throw new IOException("Backup changed during restore staging");
  }
 }
 static void checksum(String value)throws Exception{if(!value.matches("[a-f0-9]{64}"))throw new IOException("Invalid payload checksum");}
 static void part(Map<String,ZipEntry> entries,Set<String> expected,String name,long bytes)throws Exception{ZipEntry entry=entries.get(name);if(entry==null||bytes<1||entry.getSize()!=bytes||!expected.add(name))throw new IOException("Missing/invalid payload entry: "+name);}
 static JSONObject inspect(JSONObject header,JSONObject footer,Map<String,ZipEntry> entries)throws Exception{
  JSONObject m=new JSONObject(header.toString());int format=m.getInt("format");if(format<1||format>ExternalData.STREAM_FORMAT||m.getInt("user")!=0)throw new IOException("Unsupported format/user");FirmwareCheck.validate(m);
  Set<String> expected=new HashSet<>(Collections.singleton("manifest.json")),packages=new HashSet<>();JSONArray apps=m.getJSONArray("apps");if(apps.length()<1||apps.length()>1000)throw new IOException("Invalid app count");long total=0;
  for(int i=0;i<apps.length();i++){JSONObject app=apps.getJSONObject(i);String pkg=app.getString("package");if(!pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")||!packages.add(pkg))throw new IOException("Invalid app package");app.getInt("uid");if(app.getLong("version")<0)throw new IOException("Invalid saved version");JSONArray signers=app.getJSONArray("signers");if(signers.length()<1||signers.length()>32)throw new IOException("Invalid signing identity");for(int k=0;k<signers.length();k++)checksum(signers.getString(k));
   JSONArray payloads=app.getJSONArray("payloads");if(payloads.length()<1||payloads.length()>2)throw new IOException("Invalid private payload count");Set<String> domains=new HashSet<>();
   for(int j=0;j<payloads.length();j++){JSONObject payload=payloads.getJSONObject(j);String domain=payload.getString("domain"),tar="app"+i+"-"+domain+".tar",labels="app"+i+"-"+domain+".labels";if(!(domain.equals("ce")||domain.equals("de"))||!domains.add(domain)||!tar.equals(payload.getString("tar"))||!labels.equals(payload.getString("labels")))throw new IOException("Invalid private payload path");checksum(payload.getString("sha256"));checksum(payload.getString("labelsSha256"));long bytes=payload.getLong("bytes");part(entries,expected,tar,bytes);ZipEntry side=entries.get(labels);if(side==null||side.getSize()>32*1024*1024)throw new IOException("Missing/oversized labels");part(entries,expected,labels,side.getSize());total=Math.addExact(total,Math.addExact(bytes,side.getSize()));}
   JSONArray apks=app.optJSONArray("apks");if(apks!=null){if(format==1||apks.length()<1||apks.length()>128)throw new IOException("Invalid installer count");SystemAppData.dataOnly(app);for(int j=0;j<apks.length();j++){JSONObject apk=apks.getJSONObject(j);String name="app"+i+"-apk"+j+".apk";if(!name.equals(apk.getString("file")))throw new IOException("Invalid installer path");checksum(apk.getString("sha256"));long bytes=apk.getLong("bytes");part(entries,expected,name,bytes);total=Math.addExact(total,bytes);}}
  }
  if(total>BackupEngine.MAX)throw new IOException("Private payloads/installers exceed size limit");if(format==ExternalData.STREAM_FORMAT){if(footer==null)throw new IOException("External checksum footer missing");m.put("external",footer);expected.add(ExternalData.FOOTER);}JSONObject external=ExternalData.metadata(m);if(external!=null)part(entries,expected,ExternalData.ENTRY,external.getLong("bytes"));if(!expected.equals(entries.keySet()))throw new IOException("ZIP contents differ from manifest");return m;
 }
 static Result read(BackupEngine e,Uri source,File job,boolean metadataOnly)throws Exception{
  ParcelFileDescriptor opened;
  try{opened=e.c.getContentResolver().openFileDescriptor(source,"r");}catch(FileNotFoundException unsupported){return null;}
  if(opened==null)return null;
  try(ParcelFileDescriptor fd=opened){
   StructStat stat=Os.fstat(fd.getFileDescriptor());if((stat.st_mode&0170000)!=0100000)return null;
   try{Os.lseek(fd.getFileDescriptor(),0,OsConstants.SEEK_SET);}catch(ErrnoException notSeekable){return null;}
   Stamp stamp=new Stamp(stat);
   try(ZipFile zip=new ZipFile("/proc/self/fd/"+fd.getFd())){
    LinkedHashMap<String,ZipEntry> entries=new LinkedHashMap<>();Enumeration<? extends ZipEntry> iterator=zip.entries();
    while(iterator.hasMoreElements()){ZipEntry entry=iterator.nextElement();String name=entry.getName();if(entries.isEmpty()&&!name.equals("manifest.json"))throw new IOException("Backup manifest must be first");
     if(entry.isDirectory()||!name.matches("manifest\\.json|app[0-9]+-(ce|de)\\.(tar|labels)|app[0-9]+-apk[0-9]+\\.apk|external-data\\.(tar|json)")||entries.put(name,entry)!=null||entries.size()>10240)throw new IOException("Unexpected/duplicate ZIP entry");
    }
    if(!entries.containsKey("manifest.json"))throw new IOException("Backup manifest missing");
    long total=0;for(Map.Entry<String,ZipEntry> entry:entries.entrySet())if(!entry.getKey().equals(ExternalData.ENTRY)&&(!metadataOnly||entry.getKey().equals("manifest.json")||entry.getKey().equals(ExternalData.FOOTER))){
     long cap=entry.getKey().equals(ExternalData.FOOTER)?4096:entry.getKey().equals("manifest.json")?1024*1024:BackupEngine.MAX-total;
     try(OutputStream out=new FileOutputStream(new File(job,entry.getKey()))){total+=extract(zip,entry.getValue(),out,cap);}
    }
    JSONObject manifest=new JSONObject(BackupEngine.read(new File(job,"manifest.json"),1024*1024)),footer=null;int format=manifest.getInt("format");
    if(format==ExternalData.STREAM_FORMAT){if(manifest.has("external")||manifest.getJSONArray("apps").length()!=1)throw new IOException("Invalid streamed backup manifest");footer=new JSONObject(BackupEngine.read(new File(job,ExternalData.FOOTER),4096));}
    JSONObject inspection=new JSONObject(manifest.toString());if(footer!=null)inspection.put("external",footer);JSONObject external=ExternalData.metadata(inspection);
    if(external!=null){ZipEntry entry=entries.get(ExternalData.ENTRY);if(entry==null||entry.getSize()!=external.getLong("bytes"))throw new IOException("External payload size differs from metadata");}
    Result result=new Result();result.manifest=metadataOnly?inspect(manifest,footer,entries):e.validateImported(job,new HashSet<>(entries.keySet()),footer);result.stamp=stamp;result.metadataOnly=metadataOnly;result.headerIdentity=BackupEngine.hash(new File(job,"manifest.json"))+(footer==null?"":BackupEngine.hash(new File(job,ExternalData.FOOTER)));
    if(!stamp.matches(Os.fstat(fd.getFileDescriptor())))throw new IOException("Backup changed during review");return result;
   }
  }
 }
}
