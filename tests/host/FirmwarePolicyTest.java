package dev.raven.backup;
public final class FirmwarePolicyTest {
 static void check(boolean value){if(!value)throw new AssertionError();}
 static boolean accepts(String fp,String model,int sdk,String system,String vendor,String current,String cs,String cv){return FirmwarePolicy.compatible(fp,model,sdk,system,vendor,current,"Odin2",33,cs,cv);}
 public static void main(String[] args){String stock=FirmwarePolicy.ODIN_STOCK,spoof=FirmwarePolicy.ODIN_SPOOF,s=FirmwarePolicy.ODIN_SYSTEM,v=FirmwarePolicy.ODIN_VENDOR;
  check(accepts(stock,"Odin2",33,"","",stock,s,v));
  check(accepts(spoof,"Odin2",33,"","",stock,s,v));
  check(accepts(stock,"Odin2",33,"","",spoof,s,v));
  check(!accepts("different-firmware","Odin2",33,"","",stock,s,v));
  check(!accepts(spoof,"Other",33,"","",stock,s,v));
  check(!accepts(spoof,"Odin2",36,"","",stock,s,v));
  check(!accepts(spoof,"Odin2",33,"","",stock,"changed-system",v));
  check(!accepts(spoof,"Odin2",33,"","",stock,s,"changed-vendor"));
  check(accepts("other-spoof","Odin2",33,s,v,stock,s,v));
  check(!accepts(stock,"Odin2",33,"changed-system",v,stock,s,v));
  check(!accepts(stock,"Odin2",33,s,"changed-vendor",stock,s,v));
  check(!accepts(stock,"Odin2",33,s,"",stock,s,v));
  System.out.println("PASS firmware policy: stock, known spoof, real-base match; unrelated identities, OS/model mismatch and partial/changed base rejected.");
 }
}
