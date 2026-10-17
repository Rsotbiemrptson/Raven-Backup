package dev.raven.backup;
import java.io.IOException;
/** Ordinary app labels plus category-free ART profile files observed on this firmware. */
final class SelinuxLabels {
 static boolean profile(String path,String label){return label.equals("u:object_r:app_data_file:s0")&&path.matches("(?:app_[^/]+/)?oat/(?:[^/]+/)*[^/]+\\.prof");}
 static String adapt(String path,String label,String savedRoot,String currentRoot)throws IOException {
  path=TarGuard.path(path);
  if(label.equals(savedRoot))return currentRoot;
  if(profile(path,label))return label;
  throw new IOException("Unsupported security label at "+path+": "+label);
 }
}
