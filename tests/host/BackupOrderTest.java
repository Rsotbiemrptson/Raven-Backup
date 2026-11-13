package dev.raven.backup;
import java.util.*;

public final class BackupOrderTest {
 static List<BackupOrder.App> apps(){
  return new ArrayList<>(Arrays.asList(new BackupOrder.App("z.pkg","Alpha",10),
   new BackupOrder.App("a.pkg","zebra",1),new BackupOrder.App("m.pkg","Beta",10)));
 }
 static void check(int order,String expected){
  List<BackupOrder.App> values=apps();BackupOrder.sort(values,order);
  StringJoiner names=new StringJoiner(",");for(BackupOrder.App value:values)names.add(value.pkg);
  if(!expected.equals(names.toString()))throw new AssertionError(names.toString());
 }
 public static void main(String[] args){
  check(BackupOrder.PACKAGE,"a.pkg,m.pkg,z.pkg");
  check(BackupOrder.NAME,"z.pkg,m.pkg,a.pkg");
  check(BackupOrder.SIZE,"a.pkg,m.pkg,z.pkg");
  check(BackupOrder.SIZE_DESCENDING,"m.pkg,z.pkg,a.pkg");
  check(BackupOrder.PACKAGE_DESCENDING,"z.pkg,m.pkg,a.pkg");
  check(BackupOrder.NAME_DESCENDING,"a.pkg,m.pkg,z.pkg");
  for(int key=0;key<3;key++)for(boolean down:new boolean[]{false,true}){
   int value=BackupOrder.value(key,down);
   if(BackupOrder.key(value)!=key||BackupOrder.descending(value)!=down)throw new AssertionError("Preference round trip");
  }
  if(BackupOrder.key(3)!=BackupOrder.SIZE||!BackupOrder.descending(3))throw new AssertionError("Legacy largest-first preference");
  if(BackupOrder.key(2)!=BackupOrder.SIZE||BackupOrder.descending(2))throw new AssertionError("Legacy smallest-first preference");
  if(BackupOrder.normalize(-1)!=BackupOrder.PACKAGE||BackupOrder.normalize(99)!=BackupOrder.PACKAGE)throw new AssertionError("Invalid preference fallback");
  List<BackupOrder.App> ties=new ArrayList<>(Arrays.asList(new BackupOrder.App("z.pkg","alpha",1),new BackupOrder.App("a.pkg","ALPHA",1)));
  BackupOrder.sort(ties,BackupOrder.NAME_DESCENDING);
  if(!ties.get(0).pkg.equals("a.pkg"))throw new AssertionError("Case-insensitive stable ties");
  BackupOrder.sort(new ArrayList<>(),BackupOrder.SIZE);
  try{BackupOrder.sort(apps(),6);throw new AssertionError("Accepted invalid order");}
  catch(IllegalArgumentException expected){}
  System.out.println("PASS all six key/direction combinations, saved preference compatibility and deterministic ties");
 }
}
