package dev.raven.backup;
import java.io.*;
import java.util.concurrent.*;

/** Optional standard superuser transport. Merely choosing Auto never launches a root-manager prompt. */
final class RootBackends {
 interface Transport {String call(String command)throws Exception;}
 static final class Superuser implements Transport {
  public String call(String command)throws Exception{
   Process process=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();ByteArrayOutputStream output=new ByteArrayOutputStream();
   FutureTask<Void> collect=new FutureTask<>(()->{try(InputStream input=process.getInputStream()){byte[] b=new byte[8192];int n;while((n=input.read(b))!=-1){synchronized(output){if(output.size()<1024*1024)output.write(b,0,Math.min(n,1024*1024-output.size()));}}}return null;});Thread reader=new Thread(collect,"Raven root output");reader.setDaemon(true);reader.start();
   boolean completed=process.waitFor(command.equals("id")?60:12*60*60,TimeUnit.SECONDS);
   if(!completed)throw new IOException("Superuser command timed out. Inspect the current job/recovery state before retrying; it was not automatically killed or restarted.");
   collect.get();String result=output.toString("UTF-8");if(process.exitValue()!=0)throw new IOException("Superuser command failed ("+process.exitValue()+"): "+result);return result;
  }
 }
}
