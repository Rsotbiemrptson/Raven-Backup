package dev.raven.backup;
import android.os.*;
/** Observed OEM Settings protocol; not a public or portable API. */
public final class RootBridge {
 public static String call(String command) throws Exception {
  Class<?> sm=Class.forName("android.os.ServiceManager");
  IBinder binder=(IBinder)sm.getMethod("getService",String.class).invoke(null,"PServerBinder");
  if(binder==null) throw new SecurityException("PServerBinder unavailable to this app");
  Parcel data=Parcel.obtain(), reply=Parcel.obtain();
  try {data.writeStringArray(new String[]{command,"1"});
   if(!binder.transact(0,data,reply,0)) throw new SecurityException("OEM transaction rejected");
   byte[] result=reply.createByteArray();
   if(result==null) throw new SecurityException("OEM returned no result");
   return new String(result,java.nio.charset.StandardCharsets.UTF_8);
  } finally {data.recycle();reply.recycle();}
 }
}
