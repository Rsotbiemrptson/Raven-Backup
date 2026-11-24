package dev.raven.backup;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Operation-thread accounting. File-provider callbacks and background catalog scans do not double-count. */
final class TransferProgress {
 private static final ThreadLocal<State> local=new ThreadLocal<>();
 private static volatile State active;
 static final class State {volatile String phase="Preparing",app="";volatile int index,totalApps;volatile long total=-1,started=System.nanoTime();final AtomicLong bytes=new AtomicLong();}
 static void start(String phase){State state=new State();state.phase=phase;active=state;local.set(state);}
 static void attach(){State state=active;if(state!=null)local.set(state);}
 static void stop(){local.remove();active=null;}
 static void app(int index,int total,String name){State state=local.get();if(state!=null){state.index=index;state.totalApps=total;state.app=name;}}
 static void phase(String name,long total){State state=local.get();if(state!=null){state.phase=name;state.total=total;state.bytes.set(0);state.started=System.nanoTime();}}
 static void bytes(long count){State state=local.get();if(state!=null&&count>0)state.bytes.addAndGet(count);}
 static InputStream counted(InputStream input){return new FilterInputStream(input){public int read()throws IOException{int n=in.read();if(n>=0)bytes(1);return n;}public int read(byte[] b,int offset,int length)throws IOException{int n=in.read(b,offset,length);if(n>0)bytes(n);return n;}};}
 static String detail(){State s=active;if(s==null)return null;long bytes=s.bytes.get();double elapsed=(System.nanoTime()-s.started)/1e9;StringBuilder text=new StringBuilder();if(!s.app.isEmpty())text.append(s.app).append(s.totalApps>0?" · "+s.index+" of "+s.totalApps:"").append('\n');text.append(s.phase);
  if(bytes>0){text.append('\n').append(size(bytes));if(s.total>0&&bytes<=s.total)text.append(" / about ").append(size(s.total));if(elapsed>=1){double rate=bytes/elapsed;text.append(" · ").append(size((long)rate)).append("/s");if(s.total>bytes&&rate>0)text.append(" · ~").append(time((long)((s.total-bytes)/rate))).append(" remaining");}}
  else text.append("\nElapsed ").append(time((long)elapsed));return text.toString();}
 static String size(long n){return n>=1073741824L?String.format(Locale.ROOT,"%.1f GiB",n/1073741824.0):n>=1048576?String.format(Locale.ROOT,"%.1f MiB",n/1048576.0):n>=1024?String.format(Locale.ROOT,"%.1f KiB",n/1024.0):n+" B";}
 static String time(long seconds){return seconds>=3600?seconds/3600+"h "+(seconds%3600)/60+"m":seconds>=60?seconds/60+"m "+seconds%60+"s":Math.max(0,seconds)+"s";}
}
