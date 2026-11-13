package dev.raven.backup;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import java.util.function.Consumer;

/** Keeps a user-started operation alive with the display off. Never retries an interrupted restore. */
public final class OperationService extends Service {
 private static final String CHANNEL="backup-operations";
 private static final int NOTIFICATION=1700;
 private static Runnable pending;
 private static Consumer<String> startupFailure;
 private static volatile OperationService active;
 private static volatile boolean running;
 private static volatile String title="Raven Backup";
 private PowerManager.WakeLock wakeLock;
 private Thread worker;
 private String result;
 private boolean passed;
 private volatile boolean timedOut;

 static synchronized void begin(Context context,String heading,Runnable work,Consumer<String> failure){
  if(running)throw new IllegalStateException("A Raven operation is already running");
  title=heading;pending=work;startupFailure=failure;running=true;
  try{
   if(context.startForegroundService(new Intent(context,OperationService.class))==null)
    throw new IllegalStateException("Could not start background backup protection");
  }catch(RuntimeException error){pending=null;startupFailure=null;running=false;throw error;}
 }
 static boolean running(){return running;}
 static void progress(String heading,String message){
  OperationService service=active;
  if(service==null)return;
  title=heading;
  service.getSystemService(NotificationManager.class).notify(NOTIFICATION,service.notification(heading,message,true));
 }
 static void result(String message,boolean success){
  OperationService service=active;
  if(service!=null&&!service.timedOut){service.result=message;service.passed=success;}
 }
 private Notification notification(String heading,String message,boolean ongoing){
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),
   PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_notify_sync)
   .setContentTitle(heading).setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message))
   .setContentIntent(open).setOngoing(ongoing).setOnlyAlertOnce(true).setAutoCancel(!ongoing).build();
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(worker!=null)return START_NOT_STICKY;
  Runnable task=pending;
  if(task==null){stopSelf();return START_NOT_STICKY;}
  try{
   getSystemService(NotificationManager.class).createNotificationChannel(
    new NotificationChannel(CHANNEL,"Backup and restore progress",NotificationManager.IMPORTANCE_LOW));
   startForeground(NOTIFICATION,notification(title,"Working. The screen can be turned off.",true),
    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
   wakeLock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(
    PowerManager.PARTIAL_WAKE_LOCK,getPackageName()+":backup-operation");
   wakeLock.acquire();
   active=this;
   worker=new Thread(()->{
    try{task.run();}
    finally{new Handler(getMainLooper()).post(()->finishOperation());}
   },"Raven backup operation");
   worker.start();
   startupFailure=null;
  }catch(Exception error){
   Consumer<String> failure=startupFailure;
   if(failure!=null)failure.accept(error.getMessage()==null?error.toString():error.getMessage());
   finishOperation();
  }
  return START_NOT_STICKY;
 }
 private void finishOperation(){
  if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();
  stopForeground(STOP_FOREGROUND_REMOVE);
  if(result!=null)getSystemService(NotificationManager.class).notify(NOTIFICATION,
   notification(passed?"Raven operation completed":"Raven operation stopped",result,false));
  stopSelf();
 }
 @Override public void onTimeout(int startId,int type){
  timedOut=true;
  result="Android ended the background-operation time allowance. Open Raven Backup to check the result and any pending recovery.";
  passed=false;
  if(worker!=null)worker.interrupt();
  finishOperation();
 }
 @Override public void onDestroy(){
  if(worker!=null&&worker.isAlive())worker.interrupt();
  if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();
  if(active==this)active=null;
  synchronized(OperationService.class){pending=null;startupFailure=null;running=false;}
  super.onDestroy();
 }
 @Override public IBinder onBind(Intent intent){return null;}
}
