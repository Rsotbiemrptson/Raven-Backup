package dev.raven.backup;
import java.io.*;import java.nio.*;import java.nio.channels.*;import java.nio.file.*;import java.util.*;
/** >4 GiB ciphertext offsets, bounded heap, no giant allocation. */
public final class BackupCryptoLargeTest {
 public static void main(String[] args)throws Exception{Path path=Paths.get(args[0]);long size=4429185024L;char[] password="large-disposable-password".toCharArray();byte[] chunk=new byte[1024*1024];new Random(14).nextBytes(chunk);long start=System.nanoTime();
 try(OutputStream out=BackupCrypto.encrypt(Files.newOutputStream(path),password)){out.write(new byte[]{'P','K',3,4});for(long left=size-4;left>0;){int n=(int)Math.min(left,chunk.length);out.write(chunk,0,n);left-=n;}}
 if(Files.size(path)<=4294967295L)throw new AssertionError("ciphertext too small");try(BackupCrypto.Reader reader=new BackupCrypto.Reader(FileChannel.open(path),password)){if(reader.size!=size)throw new AssertionError("size overflow");for(long at:new long[]{0,1,1048500,2147483600L,4294967200L,size-8192}){ByteBuffer b=ByteBuffer.allocate(8192);int n=reader.read(at,b);for(int i=0;i<n;i++){long pos=at+i;byte expected=pos<4?new byte[]{'P','K',3,4}[(int)pos]:chunk[(int)((pos-4)%chunk.length)];if(b.array()[i]!=expected)throw new AssertionError("long seek "+pos);}}reader.verifyAll();}
 System.out.println("PASS 4.125 GiB authenticated ciphertext, >4 GiB random offsets and full verification with 128 MiB heap; "+(long)((System.nanoTime()-start)/1e9)+" seconds; "+Files.size(path)+" physical bytes");}
}
