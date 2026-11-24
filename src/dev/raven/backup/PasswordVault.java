package dev.raven.backup;
import android.content.*;
import android.net.Uri;
import android.security.keystore.*;
import android.util.Base64;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Optional device-local password storage; never exported with app settings. */
final class PasswordVault {
 static final class Required extends IOException {
  final Uri uri;Required(Uri uri,String message){super(message);this.uri=uri;}
 }
 private static final class Entry {char[] value;int pins;boolean forget;Entry(char[] value){this.value=value.clone();}}
 private static final Map<String,Entry> session=new HashMap<>();
 private static char[] current,unlockDefault;private static boolean locked;
 static synchronized void cache(Uri uri,char[] password){Entry old=session.get(uri.toString());if(old!=null){Arrays.fill(old.value,'\0');old.value=password.clone();}else session.put(uri.toString(),new Entry(password));}
 static synchronized char[] session(Uri uri){Entry e=session.get(uri.toString());return e==null?null:e.value.clone();}
 static synchronized void forgetUri(Uri uri){Entry e=session.get(uri.toString());if(e==null)return;if(e.pins==0){Arrays.fill(e.value,'\0');session.remove(uri.toString());}else e.forget=true;}
 static synchronized void current(char[] password){if(current!=null)Arrays.fill(current,'\0');current=password==null?null:password.clone();}
 static synchronized char[] current(){return current==null?null:current.clone();}
 static final class Lease implements Closeable {final String key;boolean closed;Lease(String key){this.key=key;}public void close(){synchronized(PasswordVault.class){if(closed)return;closed=true;Entry e=session.get(key);if(e!=null&&--e.pins==0&&e.forget){Arrays.fill(e.value,'\0');session.remove(key);}}}}
 static synchronized Lease pin(Uri uri){Entry e=session.get(uri.toString());if(e==null)return null;e.pins++;return new Lease(uri.toString());}
 static synchronized void lock(){locked=true;current(null);if(unlockDefault!=null){Arrays.fill(unlockDefault,'\0');unlockDefault=null;}Iterator<Map.Entry<String,Entry>> entries=session.entrySet().iterator();while(entries.hasNext()){Entry e=entries.next().getValue();if(e.pins==0){Arrays.fill(e.value,'\0');entries.remove();}else e.forget=true;}}
 private static SharedPreferences prefs(Context c){return c.getSharedPreferences("backup-password-vault",0);}
 private static String alias(Context c){return c.getPackageName()+".backup-password.v1";}
 static boolean remembered(Context c){return prefs(c).contains("ciphertext");}
 static void forget(Context c)throws Exception{if(!prefs(c).edit().clear().commit())throw new IOException("Could not forget the saved password");KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(store.containsAlias(alias(c)))store.deleteEntry(alias(c));}
 static synchronized void activate(char[] password){locked=false;current(password);}
 static void save(Context c,char[] password,boolean remember)throws Exception{
  BackupCrypto.password(password);if(!remember){forget(c);activate(password);return;}
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(!store.containsAlias(alias(c))){KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(alias(c),KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generator.generateKey();}
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,store.getKey(alias(c),null));ByteBuffer encoded=StandardCharsets.UTF_8.encode(CharBuffer.wrap(password));byte[] bytes=new byte[encoded.remaining()];encoded.get(bytes);
  try{String value=Base64.encodeToString(cipher.doFinal(bytes),Base64.NO_WRAP),iv=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP);if(!prefs(c).edit().putString("ciphertext",value).putString("iv",iv).commit())throw new IOException("Could not save the encrypted password");}finally{Arrays.fill(bytes,(byte)0);if(encoded.hasArray())Arrays.fill(encoded.array(),(byte)0);}
  activate(password);
 }
 static char[] saved(Context c)throws Exception{
  if(!remembered(c))return null;KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);Key key=store.getKey(alias(c),null);if(key==null)throw new IOException("Saved password is unavailable. Enter it again; backups are unchanged.");
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.decode(prefs(c).getString("iv",""),Base64.NO_WRAP)));byte[] bytes=cipher.doFinal(Base64.decode(prefs(c).getString("ciphertext",""),Base64.NO_WRAP));
  try{CharBuffer chars=StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));char[] result=new char[chars.remaining()];chars.get(result);if(chars.hasArray())Arrays.fill(chars.array(),'\0');return result;}finally{Arrays.fill(bytes,(byte)0);}
 }
 static char[] backup(Context c)throws Exception{char[] password=current();if(password==null)password=saved(c);BackupCrypto.password(password);return password;}
 static synchronized void unlocked(Uri uri,char[] password){cache(uri,password);if(unlockDefault!=null)Arrays.fill(unlockDefault,'\0');unlockDefault=password.clone();}
 static synchronized char[] forArchive(Context c,Uri uri,char[] provided)throws Exception{if(provided!=null)return provided.clone();char[] value=session(uri);if(value==null&&unlockDefault!=null)value=unlockDefault.clone();if(value==null&&!locked)value=current();if(value==null&&!locked)value=saved(c);if(value==null)throw new Required(uri,"Enter the password for this encrypted backup");return value;}
}
