package dev.raven.backup;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.io.*;

/** First-launch setup. Only Android's folder and permission decisions need user input. */
public final class SetupActivity extends Activity {
 static final int FOLDER=40,STORAGE=41,HELPER=42;
 int bg,card,text,muted,accent,ink;boolean light,busy,requested;String error="",stage="",report="";
 SharedPreferences flow;LinearLayout body,actions;Uri tree;
 AlertDialog testingPopup,completionPopup;TextView testingMessage;
 static SharedPreferences mainPrefs(Context c){return c.getSharedPreferences("MainActivity",0);}
 static Uri folder(Context c){String value=mainPrefs(c).getString("tree",null);return value==null?null:Uri.parse(value);}
 static boolean hasFolder(Context c,Uri tree){
  if(tree==null)return false;
  for(UriPermission permission:c.getContentResolver().getPersistedUriPermissions())if(tree.equals(permission.getUri())&&permission.isReadPermission()&&permission.isWritePermission())return true;
  return false;
 }
 static boolean checksReady(Context c){return !c.getSharedPreferences("setup-flow",0).getBoolean("needsCheck",false)&&hasFolder(c,folder(c))&&Environment.isExternalStorageManager()&&new LiveRestore(c).enabled();}
 static boolean ready(Context c){return checksReady(c)&&!c.getSharedPreferences("setup-flow",0).getBoolean("completionPending",false);}
 int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
 TextView label(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
 void add(LinearLayout parent,View v,int height){parent.addView(v,new LinearLayout.LayoutParams(-1,height<0?height:dp(height)));}
 void gap(LinearLayout parent,int height){add(parent,new View(this),height);}
 void button(String value,boolean strong,Runnable action){Button v=new Button(this);v.setText(value);v.setAllCaps(false);v.setTextSize(15);v.setTextColor(strong?ink:text);v.setBackground(shape(strong?accent:card,22));v.setStateListAnimator(null);v.setPadding(dp(16),dp(8),dp(16),dp(8));v.setOnClickListener(ignored->{if(!busy)action.run();});add(actions,v,48);gap(actions,8);}
 @Override public void onCreate(Bundle saved){
  SharedPreferences appearance=getSharedPreferences("appearance",0);String mode=appearance.getString("mode","dark");light=mode.equals("light")||(mode.equals("system")&&(getResources().getConfiguration().uiMode&48)!=32);
  setTheme(getResources().getIdentifier(light?"RavenThemeLight":"RavenTheme","style",getPackageName()));super.onCreate(saved);
  bg=Color.parseColor(light?"#F5F5F8":"#131519");card=Color.parseColor(light?"#FFFFFF":"#272B32");text=Color.parseColor(light?"#20232B":"#F1F2F6");muted=Color.parseColor(light?"#586171":"#A9B0BC");accent=appearance.getInt("accent",AppearanceColors.DEFAULT_ACCENT)|0xff000000;ink=AppearanceColors.ink(accent);
  getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(light?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  flow=getSharedPreferences("setup-flow",0);requested=false;flow.edit().putBoolean("requested",false).apply();tree=folder(this);
  if(flow.getBoolean("running",false)){requested=false;flow.edit().putBoolean("requested",false).apply();error="Setup was interrupted. Recover a pending restore first. If a test app remains afterward, remove it below, then retry.";}
  if(saved!=null&&!saved.getString("error","").isEmpty())error=saved.getString("error");render();
 }
 @Override protected void onResume(){super.onResume();tree=folder(this);if(!busy){render();if(flow.getBoolean("completionPending",false)&&checksReady(this))showCompletion();}}
 @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);render();}
 @Override protected void onSaveInstanceState(Bundle saved){super.onSaveInstanceState(saved);saved.putString("error",error);}
 @Override public void onBackPressed(){if(busy){Toast.makeText(this,"Setup checks are still running.",Toast.LENGTH_SHORT).show();return;}if(ready(this))super.onBackPressed();else moveTaskToBack(true);}
 boolean helperInstalled(){try{getPackageManager().getPackageInfo(SetupCheck.PACKAGE,0);return true;}catch(PackageManager.NameNotFoundException absent){return false;}}
 void step(String title,String detail,boolean done){LinearLayout section=column();section.setPadding(dp(16),dp(12),dp(16),dp(12));section.setBackground(shape(card,16));TextView heading=label((done?"✓  ":"")+title,17,text);heading.setTypeface(null,Typeface.BOLD);add(section,heading,-2);gap(section,4);add(section,label(detail,13,muted),-2);add(body,section,-2);gap(body,10);}
 void render(){
  if(isFinishing()||isDestroyed())return;
  LinearLayout root=column();root.setPadding(dp(22),dp(14),dp(22),dp(12));root.setBackgroundColor(bg);setContentView(root);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);ImageView icon=new ImageView(this);icon.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));icon.setContentDescription("Raven Backup");header.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(42)));
  TextView title=label(checksReady(this)?"Setup complete":"Raven Backup setup",23,text);title.setTypeface(null,Typeface.BOLD);title.setPadding(dp(12),0,0,0);header.addView(title);add(root,header,-2);gap(root,12);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);body=column();scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  boolean folderReady=hasFolder(this,tree),storage=Environment.isExternalStorageManager(),verified=checksReady(this);
  add(body,label("Choose your backup folder and allow storage access. When you are ready, press Run checks to test this device.",14,muted),-2);gap(body,12);
  if(!error.isEmpty()){TextView problem=label(error,14,text);problem.setPadding(0,dp(8),0,dp(12));add(body,problem,-2);}
  step("1. Backup folder",folderReady?BackupEngine.folderName(tree):"Choose an existing backup folder or create a dedicated one.",folderReady);
  step("2. Storage access",storage?"Internal-storage scripts are allowed.":"Allow access so Raven can create its backup scripts.",storage);
  step("3. Device checks",busy?stage:verified?"Folder, root access and live restore are ready.":"Press Run checks to test root, folder access, existing ZIPs and a full restore using a disposable app.",verified);
  add(body,label("The checks temporarily install Raven Restore Check, verify backup, rollback, reinstall, data and file permissions, then remove it. Your other apps and backup ZIPs are not changed. Keep the device unlocked until setup finishes.",13,muted),-2);gap(body,8);
  actions=column();gap(root,8);add(root,actions,-2);
  if(busy){ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(AppearanceColors.readable(accent,card)));add(actions,progress,6);gap(actions,10);add(actions,label(stage,14,text),-2);return;}
  if(new LiveRestore(this).pointer.exists()){
   button("Recover interrupted restore",true,()->runRecovery());return;
  }
  if(helperInstalled()){
   button("Remove leftover test app",true,()->{requested=false;flow.edit().putBoolean("requested",false).apply();startActivityForResult(new Intent(Intent.ACTION_DELETE,Uri.parse("package:"+SetupCheck.PACKAGE)),HELPER);});return;
  }
  if(!folderReady)button("Choose backup folder",true,this::chooseFolder);
  else if(!storage){button("Allow storage access",true,this::allowStorage);button("Change backup folder",false,this::chooseFolder);}
  else if(verified){button("Open Raven Backup",true,this::openApp);button("Run checks again",false,()->{requestChecks();runChecks();});button("Change backup folder",false,this::chooseFolder);}
  else{button(error.isEmpty()?"Run checks":"Retry checks",true,()->{requestChecks();runChecks();});button("Change backup folder",false,this::chooseFolder);}
 }
 void chooseFolder(){requested=false;flow.edit().putBoolean("requested",false).apply();Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);picker.putExtra(DocumentsContract.EXTRA_INITIAL_URI,tree!=null?DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)):DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Documents"));startActivityForResult(picker,FOLDER);}
 void requestChecks(){requested=false;error="";flow.edit().putBoolean("requested",false).putBoolean("needsCheck",true).putBoolean("completionPending",false).apply();}
 void allowStorage(){requested=false;error="";flow.edit().putBoolean("requested",false).putBoolean("needsCheck",true).apply();try{startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName())),STORAGE);}catch(Exception e){error="Open Android Settings and allow all-files access for Raven Backup, then retry.";render();}}
 @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);
  if(code==FOLDER){
   if(result!=RESULT_OK||data==null||data.getData()==null){error=hasFolder(this,tree)?"Folder selection cancelled. Your existing folder is still selected.":"No folder selected. Choose a backup folder when you are ready.";render();return;}
   try{Uri picked=data.getData();getContentResolver().takePersistableUriPermission(picked,data.getFlags()&3);if(!hasFolder(this,picked))throw new IOException("Both read and write folder access are needed.");if(!mainPrefs(this).edit().putString("tree",picked.toString()).commit())throw new IOException("Could not save the backup folder.");tree=picked;requested=false;error="";flow.edit().putBoolean("requested",false).putBoolean("needsCheck",true).putBoolean("completionPending",false).apply();}
   catch(Exception e){requested=false;flow.edit().putBoolean("requested",false).apply();error="Folder access was not saved: "+e.getMessage();}render();
  }
  if(code==STORAGE&&!Environment.isExternalStorageManager()){requested=false;flow.edit().putBoolean("requested",false).apply();error="Storage access was not enabled. Tap Allow storage access and enable it on Android's settings page.";render();}
  if(code==HELPER){error=helperInstalled()?"The test app is still installed. Remove Raven Restore Check to retry setup.":"Test app removed. Retry the checks to finish setup.";render();}
 }
 void stylePopup(AlertDialog popup){popup.getWindow().setBackgroundDrawable(shape(card,22));for(int which:new int[]{-1,-2,-3}){Button button=popup.getButton(which);if(button!=null)button.setTextColor(AppearanceColors.readable(accent,card));}}
 void showTesting(){
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(24),dp(16),dp(24),dp(24));
  ProgressBar spinner=new ProgressBar(this);spinner.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(AppearanceColors.readable(accent,card)));row.addView(spinner,new LinearLayout.LayoutParams(dp(32),dp(32)));
  testingMessage=label(stage,14,text);LinearLayout.LayoutParams space=new LinearLayout.LayoutParams(0,-2,1);space.setMargins(dp(16),0,0,0);row.addView(testingMessage,space);
  testingPopup=new AlertDialog.Builder(this).setTitle("Testing…").setView(row).setCancelable(false).create();testingPopup.setCanceledOnTouchOutside(false);testingPopup.show();stylePopup(testingPopup);
 }
 void closeTesting(){if(testingPopup!=null){testingPopup.dismiss();testingPopup=null;}testingMessage=null;}
 void showCompletion(){
  if(completionPopup!=null||isFinishing()||isDestroyed())return;
  completionPopup=new AlertDialog.Builder(this).setTitle("Done").setMessage("Checks passed. Raven Backup is ready.").setCancelable(false).setPositiveButton("Continue",(d,w)->{completionPopup=null;if(!flow.edit().putBoolean("completionPending",false).commit()){error="Could not save setup completion. Please retry.";render();return;}openApp();}).create();completionPopup.setCanceledOnTouchOutside(false);completionPopup.show();stylePopup(completionPopup);
 }
 void progress(String message){runOnUiThread(()->{if(isFinishing()||isDestroyed())return;stage=message;if(testingMessage!=null)testingMessage.setText(message);});}
 void runChecks(){
  if(busy||!hasFolder(this,tree)||!Environment.isExternalStorageManager())return;
  if(new LiveRestore(this).pointer.exists()||helperInstalled()){requested=false;flow.edit().putBoolean("requested",false).apply();render();return;}
  if(!flow.edit().putBoolean("running",true).putBoolean("needsCheck",true).putBoolean("requested",false).commit()){error="Could not save setup progress. Restart Raven and retry.";render();return;}
  busy=true;requested=false;error="";stage="Checking root access…";render();showTesting();Uri checkedTree=tree;
  new Thread(()->{String failure=null;try{synchronized(LiveRestore.class){
   String root=new BackupEngine(this).probe();progress("Checking backup folder and existing ZIPs…");String library=LibraryCheck.run(this,checkedTree);
   progress("Verifying backup and restore with the test app…");String restore=SetupCheck.run(this,this::progress);report=root+"\n"+library+"\n"+restore;
   if(!flow.edit().putString("report",report).putString("verifiedTree",checkedTree.toString()).putBoolean("running",false).putBoolean("needsCheck",false).putBoolean("completionPending",true).commit())throw new IOException("Could not save completed setup.");
   mainPrefs(this).edit().putString("lastOperation","Setup complete. Backup folder, root and live restore checks passed.").apply();
  }}catch(Exception e){failure=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();flow.edit().putBoolean("running",false).putBoolean("needsCheck",true).putBoolean("completionPending",false).apply();}
  final String stopped=failure;runOnUiThread(()->{busy=false;closeTesting();if(isFinishing()||isDestroyed())return;render();if(stopped==null){showCompletion();}else{error="Setup could not finish: "+stopped;render();}});
  },"Raven guided setup").start();
 }
 void runRecovery(){busy=true;stage="Recovering the interrupted restore…";error="";render();new Thread(()->{String message;try{message=new LiveRestore(this).recover();}catch(Exception e){message="Recovery could not finish: "+e.getMessage();}final String output=message;runOnUiThread(()->{busy=false;error=output;render();});},"Raven setup recovery").start();}
 void openApp(){if(!ready(this)){render();return;}startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();}
 @Override protected void onDestroy(){closeTesting();if(completionPopup!=null){completionPopup.dismiss();completionPopup=null;}super.onDestroy();}
}
