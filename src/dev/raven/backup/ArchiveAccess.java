package dev.raven.backup;
import android.content.Context;
import android.net.Uri;
import android.os.*;
import android.os.storage.StorageManager;
import android.system.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/** Seekable authenticated views avoid staging a whole decrypted large ZIP. */
final class ArchiveAccess {
 private static final LinkedHashMap<String,Cached> readers=new LinkedHashMap<>(8,.75f,true);
 private static final HandlerThread thread=new HandlerThread("Raven encrypted file reader");
 private static Handler handler;
 static final class Physical {
  // exFAT rename ctime can be rounded on writeback without changing content; use inode, size and precise mtime.
  final long device,inode,size,modified,modifiedNanos;Physical(StructStat s){device=s.st_dev;inode=s.st_ino;size=s.st_size;modified=s.st_mtime;modifiedNanos=s.st_mtim.tv_nsec;}
  boolean same(StructStat s){return device==s.st_dev&&inode==s.st_ino&&size==s.st_size&&modified==s.st_mtime&&modifiedNanos==s.st_mtim.tv_nsec;}
 }
 private static final class Cached {BackupCrypto.Reader reader;Physical stamp;int references;long used;boolean discard;}
 static final class Handle implements Closeable {
  final ParcelFileDescriptor fd;final StructStat physical;final boolean encrypted;final Cached cache;
  Handle(ParcelFileDescriptor fd,StructStat physical,boolean encrypted,Cached cache){this.fd=fd;this.physical=physical;this.encrypted=encrypted;this.cache=cache;}
  void verifyEncryption()throws IOException{if(cache!=null)cache.reader.verifyAll();}
  public void close()throws IOException{fd.close();}
 }
 private static synchronized Handler handler(){if(handler==null){thread.start();handler=new Handler(thread.getLooper());handler.postDelayed(ArchiveAccess::expire,60000);}return handler;}
 private static synchronized void expire(){long now=android.os.SystemClock.elapsedRealtime();Iterator<Cached> it=readers.values().iterator();while(it.hasNext()){Cached value=it.next();if(value.references==0&&now-value.used>5*60*1000){try{value.reader.close();}catch(IOException ignored){}it.remove();}}if(handler!=null)handler.postDelayed(ArchiveAccess::expire,60000);}
 static synchronized void discard(Uri uri){PasswordVault.forgetUri(uri);Cached cached=readers.get(uri.toString());if(cached==null)return;cached.discard=true;if(cached.references==0){try{cached.reader.close();}catch(IOException ignored){}readers.remove(uri.toString());}}
 static synchronized void lock()throws IOException{for(Cached c:readers.values())if(c.references!=0)throw new IOException("Wait for the current encrypted-file operation to finish");for(Cached c:readers.values())c.reader.close();readers.clear();PasswordVault.lock();}
 static boolean encrypted(Context c,Uri uri)throws Exception{
  try(ParcelFileDescriptor fd=c.getContentResolver().openFileDescriptor(uri,"r")){if(fd==null)throw new IOException("Backup cannot be opened");return magic(fd);}
 }
 private static boolean magic(ParcelFileDescriptor fd)throws Exception{byte[] prefix=new byte[8];try{int n=Os.pread(fd.getFileDescriptor(),prefix,0,8,0);return n==8&&BackupCrypto.magic(prefix);}catch(ErrnoException notSeekable){return false;}}
 static Handle open(Context c,Uri uri)throws Exception{return open(c,uri,null);}
 static Handle open(Context c,Uri uri,char[] provided)throws Exception{
  ParcelFileDescriptor raw=c.getContentResolver().openFileDescriptor(uri,"r");if(raw==null)throw new IOException("Cannot open backup");
  try{
   StructStat physical=Os.fstat(raw.getFileDescriptor());if(!magic(raw))return new Handle(raw,physical,false,null);
   Handler callbacks=handler();Cached cached;
   synchronized(ArchiveAccess.class){
    cached=readers.get(uri.toString());
    if(cached!=null&&!cached.stamp.same(physical)){if(cached.references>0)throw new IOException("Encrypted backup changed while open");cached.reader.close();readers.remove(uri.toString());cached=null;}
    if(provided!=null&&cached!=null){if(cached.references>0)throw new IOException("Encrypted backup is in use");cached.reader.close();readers.remove(uri.toString());cached=null;}
    if(cached==null){
     Iterator<Cached> old=readers.values().iterator();while(readers.size()>=4&&old.hasNext()){Cached value=old.next();if(value.references==0){value.reader.close();old.remove();}}
     if(readers.size()>=4)throw new IOException("Too many encrypted archives are open; retry after the current operation");
     char[] password=PasswordVault.forArchive(c,uri,provided);
     try{FileChannel channel=new ParcelFileDescriptor.AutoCloseInputStream(ParcelFileDescriptor.dup(raw.getFileDescriptor())).getChannel();cached=new Cached();try{cached.reader=new BackupCrypto.Reader(channel,password);}catch(Exception invalid){throw new PasswordVault.Required(uri,"Password is incorrect or the encrypted backup is damaged");}cached.stamp=new Physical(physical);PasswordVault.cache(uri,password);readers.put(uri.toString(),cached);}finally{Arrays.fill(password,'\0');}
    }
    cached.references++;cached.used=android.os.SystemClock.elapsedRealtime();
   }
   final Cached selected=cached;ParcelFileDescriptor proxy;
   try{proxy=c.getSystemService(StorageManager.class).openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY,new ProxyFileDescriptorCallback(){
    @Override public long onGetSize(){return selected.reader.size;}
    @Override public int onRead(long offset,int size,byte[] data)throws ErrnoException{try{if(offset>=selected.reader.size)return 0;int n=selected.reader.read(offset,ByteBuffer.wrap(data,0,(int)Math.min(size,selected.reader.size-offset)));selected.used=android.os.SystemClock.elapsedRealtime();return Math.max(n,0);}catch(IOException invalid){throw new ErrnoException("Authenticated backup read",OsConstants.EIO,invalid);}}
    @Override public void onRelease(){synchronized(ArchiveAccess.class){selected.references--;selected.used=android.os.SystemClock.elapsedRealtime();if(selected.references==0&&selected.discard){try{selected.reader.close();}catch(IOException ignored){}if(readers.get(uri.toString())==selected)readers.remove(uri.toString());}}}
   },callbacks);}catch(Exception failure){synchronized(ArchiveAccess.class){cached.references--;}throw failure;}
   raw.close();return new Handle(proxy,physical,true,selected);
  }catch(Exception failure){raw.close();throw failure;}
 }
 static void unlock(Context c,Uri uri,char[] password)throws Exception{try(Handle handle=open(c,uri,password)){if(!handle.encrypted)throw new IOException("This backup is not encrypted");PasswordVault.unlocked(uri,password);}}
 static StructStat stat(Context c,Uri uri)throws Exception{try(ParcelFileDescriptor fd=c.getContentResolver().openFileDescriptor(uri,"r")){if(fd==null)throw new IOException("Cannot inspect backup");return Os.fstat(fd.getFileDescriptor());}}
 static InputStream stream(Context c,Uri uri)throws Exception{Handle handle=open(c,uri);return new ParcelFileDescriptor.AutoCloseInputStream(handle.fd);}
}
