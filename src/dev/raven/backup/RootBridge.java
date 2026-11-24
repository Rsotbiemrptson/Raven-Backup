package dev.raven.backup;
import android.os.*;
/** Observed OEM Settings protocol; not a public or portable API. */
public final class RootBridge {
 static volatile String mode="auto";
 static void configure(android.content.Context c){String value=c.getSharedPreferences("device-root",0).getString("transport","auto");if(!java.util.Arrays.asList("auto","odin","su").contains(value))throw new SecurityException("Unknown root connection");mode=value;}
 static String name(){return mode.equals("su")?"Standard superuser (su)":"Odin OEM PServerBinder";}
 public static String call(String command) throws Exception {
  RootBackends.Transport transport=mode.equals("su")?new RootBackends.Superuser():RootBridge::callOem;return transport.call(command);
 }
 private static String callOem(String command)throws Exception{
  Class<?> sm=Class.forName("android.os.ServiceManager");
  IBinder binder=(IBinder)sm.getMethod("getService",String.class).invoke(null,"PServerBinder");
  if(binder==null) throw new SecurityException("Compatible Odin OEM root service unavailable. Select Standard superuser in guided setup only if this device is already rooted");
  Parcel data=Parcel.obtain(), reply=Parcel.obtain();
  try {data.writeStringArray(new String[]{command,"1"});
   if(!binder.transact(0,data,reply,0)) throw new SecurityException("OEM transaction rejected");
   byte[] result=reply.createByteArray();
   if(result==null) throw new SecurityException("OEM returned no result");
   return new String(result,java.nio.charset.StandardCharsets.UTF_8);
  } finally {data.recycle();reply.recycle();}
 }
}
