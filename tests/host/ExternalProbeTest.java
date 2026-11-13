package dev.raven.backup;
import java.io.IOException;
public final class ExternalProbeTest {
 public static void main(String[] args)throws Exception {
  for(String state:new String[]{"ABSENT","EMPTY"})if(ExternalProbe.bytes("RAVEN_EXTERNAL_"+state+"\n")!=-1)throw new AssertionError(state);
  if(ExternalProbe.bytes("RAVEN_EXTERNAL_PRESENT 0")!=0||ExternalProbe.bytes("RAVEN_EXTERNAL_PRESENT 157286400")!=161061273600L)throw new AssertionError("size");
  for(String bad:new String[]{"","absent","0","RAVEN_EXTERNAL_PRESENT ","RAVEN_EXTERNAL_PRESENT -1","RAVEN_EXTERNAL_PRESENT 1 x","RAVEN_EXTERNAL_PRESENT 9007199254740992","RAVEN_EXTERNAL_PRESENT 99999999999999999999999999","du: permission denied\nRAVEN_EXTERNAL_EMPTY"}){
   try{ExternalProbe.bytes(bad);throw new AssertionError("Accepted "+bad);}catch(IOException expected){}
  }
  System.out.println("PASS explicit absent/empty/data results, large sizes, missing/malformed/error responses and overflow rejection");
 }
}
