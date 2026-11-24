package dev.raven.backup;
import java.io.File;
/** Probe required tools using only a private disposable job directory. */
final class RootCapabilities {
 static String check(BackupEngine e)throws Exception{
  File job=e.job();String root=BackupEngine.q(job.getPath());String script="for tool in tar find sort stat chown chmod chcon restorecon mv mkfifo sha256sum; do command -v \"$tool\" > /dev/null || { echo \"Missing tool: $tool\"; exit 1; }; done\n"
   +"tar --help | grep numeric-owner > /dev/null\nprintf 'a\\0' | sort -z > "+root+"/names\n( IFS= read -r -d '' item; [ \"$item\" = a ]; ) < "+root+"/names\n"
   +"mkdir "+root+"/one\nprintf capability > "+root+"/one/marker\nmv -T "+root+"/one "+root+"/two\nchmod 640 "+root+"/two/marker\nchown "+android.os.Process.myUid()+":"+android.os.Process.myUid()+" "+root+"/two/marker\n[ \"$(stat -c %a:%u:%g "+root+"/two/marker)\" = 640:"+android.os.Process.myUid()+":"+android.os.Process.myUid()+" ]\n"
   +"label=$(ls -Zd "+root+"); label=${label%% *}; chcon -h \"$label\" "+root+"/two/marker\nrestorecon "+root+"/two/marker\nmkfifo "+root+"/probe.pipe\n[ -p "+root+"/probe.pipe ]\nrm -f "+root+"/probe.pipe\necho 'PASS required archive, path, mode, ownership, label, rename and FIFO tools'\n";
  return e.run(job,script,false);
 }
}
