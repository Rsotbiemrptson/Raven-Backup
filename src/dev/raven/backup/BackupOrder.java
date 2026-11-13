package dev.raven.backup;

import java.util.Comparator;
import java.util.List;

/** Stable batch ordering. Values 0-3 retain the earlier saved preferences. */
final class BackupOrder {
 static final int PACKAGE=0, NAME=1, SIZE=2, SIZE_DESCENDING=3, PACKAGE_DESCENDING=4, NAME_DESCENDING=5;
 static int normalize(int order){return order>=0&&order<=5?order:PACKAGE;}
 static int key(int order){
  order=normalize(order);
  return order==PACKAGE_DESCENDING?PACKAGE:order==NAME_DESCENDING?NAME:order==SIZE_DESCENDING?SIZE:order;
 }
 static boolean descending(int order){return order==SIZE_DESCENDING||order==PACKAGE_DESCENDING||order==NAME_DESCENDING;}
 static int value(int key,boolean descending){
  if(key==PACKAGE)return descending?PACKAGE_DESCENDING:PACKAGE;
  if(key==NAME)return descending?NAME_DESCENDING:NAME;
  if(key==SIZE)return descending?SIZE_DESCENDING:SIZE;
  throw new IllegalArgumentException("Unknown backup sort key");
 }
 static final class App {
  final String pkg,label;
  final long bytes;
  App(String pkg,String label,long bytes){this.pkg=pkg;this.label=label;this.bytes=bytes;}
 }
 static void sort(List<App> apps,int order){
  if(order!=normalize(order))throw new IllegalArgumentException("Unknown backup order");
  Comparator<App> compare=Comparator.comparing(app->app.pkg);
  if(key(order)==NAME)compare=Comparator.comparing(app->app.label,String.CASE_INSENSITIVE_ORDER);
  else if(key(order)==SIZE)compare=Comparator.comparingLong(app->app.bytes);
  if(descending(order))compare=compare.reversed();
  apps.sort(compare.thenComparing(app->app.pkg));
 }
}
