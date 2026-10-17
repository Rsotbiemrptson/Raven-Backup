package dev.raven.backup;
public class SelinuxLabelsTest {
 static String oldRoot="u:object_r:app_data_file:s0:c1,c2",newRoot="u:object_r:app_data_file:s0:c3,c4";
 static void reject(String path,String label)throws Exception{try{SelinuxLabels.adapt(path,label,oldRoot,newRoot);throw new AssertionError("Accepted "+path+" "+label);}catch(java.io.IOException expected){}}
 public static void main(String[] a)throws Exception{
 if(!SelinuxLabels.adapt("./files/save",oldRoot,oldRoot,newRoot).equals(newRoot))throw new AssertionError();
 if(!SelinuxLabels.adapt("./app_.gpg.classloader/oat/test.jar.prof","u:object_r:app_data_file:s0",oldRoot,newRoot).equals("u:object_r:app_data_file:s0"))throw new AssertionError();
 reject("files/save","u:object_r:app_data_file:s0");reject("app_x/oat/test.prof","u:object_r:system_file:s0");reject("app_x/oat/test.prof","u:object_r:app_data_file:s0:c99");reject("../oat/a.prof","u:object_r:app_data_file:s0");reject("app_x/oat/test.vdex","u:object_r:app_data_file:s0");System.out.println("PASS ordinary category adaptation and category-free ART profile preservation; unrelated paths, types, categories and traversal rejected.");}
}
