package dev.raven.backup;

import com.google.crypto.tink.subtle.AesGcmHkdfStreaming;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;

/** Password envelope around a normal ZIP; authenticated segmented encryption is provided by Tink. */
final class BackupCrypto {
 static final byte[] MAGIC={'R','V','N','E','N','C','0','1'};
 static final int HEADER=32,ITERATIONS=600000,SEGMENT=1024*1024;
 static final long MAX_PLAIN=544L*1024*1024*1024;
 static boolean magic(byte[] prefix){return prefix.length>=MAGIC.length&&Arrays.equals(MAGIC,Arrays.copyOf(prefix,MAGIC.length));}
 static void password(char[] password)throws IOException{if(password==null||password.length<8||password.length>1024)throw new IOException("Use a backup password of 8–1024 characters. Keep a separate copy; Raven cannot recover it.");}
 private static AesGcmHkdfStreaming aead(byte[] header,char[] password)throws Exception{
  password(password);if(header.length!=HEADER||!magic(header))throw new IOException("Not a supported encrypted Raven backup");
  ByteBuffer fields=ByteBuffer.wrap(header);fields.position(24);if(fields.getInt()!=ITERATIONS||fields.getInt()!=0)throw new IOException("Unsupported encrypted-backup parameters");
  PBEKeySpec spec=new PBEKeySpec(password,Arrays.copyOfRange(header,8,24),ITERATIONS,256);byte[] key=null;
  try{key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();return new AesGcmHkdfStreaming(key,"HmacSha256",32,SEGMENT,0);}
  finally{spec.clearPassword();if(key!=null)Arrays.fill(key,(byte)0);}
 }
 static OutputStream encrypt(OutputStream target,char[] password)throws Exception{
  password(password);byte[] header=new byte[HEADER];System.arraycopy(MAGIC,0,header,0,8);byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);System.arraycopy(salt,0,header,8,16);ByteBuffer.wrap(header).position(24).putInt(ITERATIONS).putInt(0);
  AesGcmHkdfStreaming cipher=aead(header,password);target.write(header);OutputStream encrypted=cipher.newEncryptingStream(target,header);
  return new FilterOutputStream(encrypted){long bytes;
   public void write(int value)throws IOException{if(++bytes>MAX_PLAIN)throw new IOException("Encrypted archive exceeds supported size");out.write(value);}
   public void write(byte[] b,int offset,int length)throws IOException{bytes+=length;if(bytes>MAX_PLAIN)throw new IOException("Encrypted archive exceeds supported size");out.write(b,offset,length);}
  };
 }
 /** The caller owns the physical channel; this view includes only Tink's ciphertext. */
 static final class Slice implements SeekableByteChannel {
  final SeekableByteChannel source;long offset;boolean open=true;
  Slice(SeekableByteChannel source){this.source=source;}
  public int read(ByteBuffer buffer)throws IOException{source.position(HEADER+offset);int n=source.read(buffer);if(n>0)offset+=n;return n;}
  public int write(ByteBuffer b)throws IOException{throw new NonWritableChannelException();}
  public long position(){return offset;}
  public SeekableByteChannel position(long value)throws IOException{if(value<0)throw new IOException("Negative encrypted offset");offset=value;return this;}
  public long size()throws IOException{return source.size()-HEADER;}
  public SeekableByteChannel truncate(long length)throws IOException{throw new NonWritableChannelException();}
  public boolean isOpen(){return open&&source.isOpen();}
  public void close()throws IOException{open=false;source.close();}
 }
 static final class Reader implements Closeable {
  final SeekableByteChannel plain;final long size;final int segmentSize,firstOffset;final BitSet authenticated=new BitSet();boolean closed;
  Reader(SeekableByteChannel physical,char[] password)throws Exception{
   try{
    physical.position(0);ByteBuffer header=ByteBuffer.allocate(HEADER);while(header.hasRemaining())if(physical.read(header)<0)throw new EOFException("Encrypted header is truncated");
    AesGcmHkdfStreaming cipher=aead(header.array(),password);plain=cipher.newSeekableDecryptingChannel(new Slice(physical),header.array());size=plain.size();segmentSize=cipher.getPlaintextSegmentSize();firstOffset=cipher.getCiphertextOffset();
    if(size<4||size>MAX_PLAIN)throw new IOException("Invalid encrypted archive size");
    // Authenticate before exposing an archive to the ZIP parser. Central-directory reads authenticate its end.
    ByteBuffer prefix=ByteBuffer.allocate(4);read(0,prefix);byte[] bytes=prefix.array();if(bytes[0]!='P'||bytes[1]!='K'||bytes[2]!=3||bytes[3]!=4)throw new IOException("Password is incorrect or the encrypted archive is damaged");
   }catch(Exception failure){try{physical.close();}catch(IOException close){failure.addSuppressed(close);}throw failure;}
  }
  synchronized int read(long offset,ByteBuffer output)throws IOException{
   if(closed)throw new IOException("Encrypted archive is closed");if(offset<0||offset>size)throw new IOException("Encrypted read offset out of range");plain.position(offset);int total=0;
   while(output.hasRemaining()){int n=plain.read(output);if(n<0)break;if(n==0)break;total+=n;}
   if(total>0){int first=(int)((offset+firstOffset)/segmentSize),last=(int)((offset+total-1+firstOffset)/segmentSize);authenticated.set(first,last+1);}return total==0&&offset==size?-1:total;
  }
  synchronized void verifyAll()throws IOException{
   int last=(int)((size-1+firstOffset)/segmentSize);for(int i=0;i<=last;i++)if(!authenticated.get(i)){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Verification stopped");long offset=Math.max(0,(long)i*segmentSize-firstOffset);ByteBuffer one=ByteBuffer.allocate(1);if(read(offset,one)!=1)throw new EOFException("Encrypted archive is incomplete");}
  }
  public synchronized void close()throws IOException{if(!closed){closed=true;plain.close();authenticated.clear();}}
 }
}
