package dev.raven.backup;
import java.util.*;
/** Selection is separate from provider mutation. The new backup must already be fully verified. */
final class RetentionPolicy {
 static <T> List<T> older(List<T> newestFirst,int keep){if(keep<1||keep>50)throw new IllegalArgumentException("Keep 1–50 backups");return newestFirst.size()<=keep?Collections.emptyList():new ArrayList<>(newestFirst.subList(keep,newestFirst.size()));}
}
