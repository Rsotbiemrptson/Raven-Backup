package dev.raven.backup;
import java.io.*;import java.util.*;
public class RestoreBatchTest {
 static class Task implements RestoreBatch.Task{String label;Exception before,during;boolean pending;int prepared,applied;Task(String n){label=n;}public String name(){return label;}public boolean pendingRecovery(){return pending;}public void prepare()throws Exception{prepared++;if(before!=null)throw before;}public void restore()throws Exception{applied++;if(during!=null)throw during;}}
 static RestoreBatch run(Task... tasks){RestoreBatch b=new RestoreBatch();b.run(Arrays.asList(tasks),(n,t,name)->{});return b;}
 static void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  Task first=new Task("First"),skip=new Task("Legacy game"),last=new Task("Last");skip.before=new RestoreBatch.Incompatible("32-bit only");RestoreBatch b=run(first,skip,last);check(b.count("Restored")==2&&b.count("Skipped")==1&&!b.stopped&&skip.applied==0&&last.applied==1);check(b.report().contains("Legacy game")&&b.report().contains("32-bit only"));
  Task unsafe=new Task("Unsafe"),later=new Task("Later");unsafe.during=new RestoreBatch.Incompatible("Not safe after replacement starts");b=run(unsafe,later);check(b.stopped&&b.count("Failed")==1&&b.count("Not run")==1&&later.prepared==0);
  Task pending=new Task("Pending");pending.pending=true;b=run(pending,new Task("Later"));check(b.stopped&&pending.prepared==0&&pending.applied==0);
  Task disk=new Task("Disk error");disk.before=new IOException("No space");b=run(disk,new Task("Later"));check(b.stopped&&b.count("Skipped")==0);
  Task rollback=new Task("Recovered failure");rollback.during=new IOException("Rollback verified");b=run(rollback,new Task("Later"));check(b.stopped&&b.count("Failed")==1);
  Task a=new Task("A"),c=new Task("C");a.before=new RestoreBatch.Incompatible("Version differs");c.before=new RestoreBatch.Incompatible("Signer differs");b=run(a,c);check(b.count("Skipped")==2&&b.count("Restored")==0&&!b.stopped);
  Thread.currentThread().interrupt();Task interrupted=new Task("Interrupted");b=run(interrupted);check(b.stopped&&interrupted.prepared==0);Thread.interrupted();
  System.out.println("PASS mixed/all-skipped queues, app/reason reports, continuation, recovery/error/interrupt stops and no writes for skipped apps");
 }
}
