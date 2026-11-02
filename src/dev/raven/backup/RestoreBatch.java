package dev.raven.backup;
import java.io.*;import java.util.*;
/** Runs independent apps; only classified failures before data replacement may be skipped. */
final class RestoreBatch {
 static final class Incompatible extends IOException{Incompatible(String reason){super(reason);}}
 interface Task{String name();void prepare()throws Exception;void restore()throws Exception;boolean pendingRecovery();}
 interface Progress{void update(int current,int total,String name);}
 static final class Result{final Task task;final String status,reason;Result(Task t,String s,String r){task=t;status=s;reason=r;}}
 final List<Result> results=new ArrayList<>();boolean stopped;
 void run(List<? extends Task> tasks,Progress progress){
  for(int i=0;i<tasks.size();i++){
   Task task=tasks.get(i);if(stopped){results.add(new Result(task,"Not run","Queue stopped for safety."));continue;}
   boolean replacing=false;
   try{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Queue interrupted");if(task.pendingRecovery())throw new IOException("Recover the pending restore before continuing.");progress.update(i+1,tasks.size(),task.name());task.prepare();if(task.pendingRecovery())throw new IOException("Recovery became pending before data restore.");replacing=true;task.restore();if(task.pendingRecovery())throw new IOException("Restore needs recovery.");results.add(new Result(task,"Restored","Contents, ownership, permissions and security labels verified."));}
   catch(Exception error){String reason=error.getMessage()==null?error.getClass().getSimpleName():error.getMessage();if(error instanceof Incompatible&&!replacing&&!task.pendingRecovery())results.add(new Result(task,"Skipped",reason));else{results.add(new Result(task,"Failed",reason));stopped=true;}}
  }
 }
 int count(String status){int n=0;for(Result r:results)if(r.status.equals(status))n++;return n;}
 String report(){StringBuilder s=new StringBuilder("Restored "+count("Restored")+" • Skipped "+count("Skipped")+" • Failed "+count("Failed")+" • Not run "+count("Not run")+"\n");if(stopped)s.append("Queue stopped for safety. Earlier successful restores remain restored. Recover any pending restore before continuing.\n");for(String status:Arrays.asList("Skipped","Failed","Not run","Restored")){if(count(status)==0)continue;s.append("\n").append(status).append(":\n");for(Result r:results)if(r.status.equals(status))s.append("• ").append(r.task.name()).append("\n  ").append(r.reason).append("\n");}return s.toString();}
}
