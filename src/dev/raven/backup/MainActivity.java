package dev.raven.backup;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.provider.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.File;
import java.util.*;
import java.text.DateFormat;

/** Independent UI guided by Neo Backup's app rows, navigation and batch workflows. */
public class MainActivity extends Activity {
 static final int INFO_POPUP_WIDTH_DP=480;
 static final String[] BACKUP_ORDER_LABELS={"Package ID","App name","Size"};
 int BG,SURFACE,CARD,TEXT,MUTED,ACCENT,INK,ACCENT_TEXT; boolean lightTheme;
 static final class AppRow {ApplicationInfo info;String label,version;long updated;boolean launchable;}
 final Set<String> selected=new TreeSet<>();final Set<String> selectedBackups=new LinkedHashSet<>();Button deleteBackups,selectBackups,showSelected;List<AppRow> allApps=new ArrayList<>();List<BackupCatalog.Entry> catalog=new ArrayList<>();final Map<String,BackupCatalog.Entry> latest=new HashMap<>();
 LinearLayout page,content,list,footer,nav;TextView title,subtitle,status,summary;EditText search;Button primary;Uri tree;boolean busy,loading,selectedOnly,keyboardVisible;String lastStatus="Ready to back up private app data.";String section="Backup",query="";int filter=0,sort=0,loadGeneration;
 AlertDialog operationPopup;TextView operationMessage;int openDialogs;boolean pendingUiRebuild;
 final List<Button> navigation=new ArrayList<>();
 interface Job {String run()throws Exception;}
 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
 Button button(String value,boolean strong,Runnable action){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(strong?INK:TEXT);b.setBackgroundTintList(null);b.setBackground(shape(strong?ACCENT:CARD,24));b.setStateListAnimator(null);b.setElevation(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(16),dp(9),dp(16),dp(9));b.setOnClickListener(v->{if(!busy)action.run();});return b;}
 void add(LinearLayout parent,View view,int width,int height){parent.addView(view,new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height)));}
 void gap(LinearLayout parent,int height){add(parent,new View(this),1,height);}
 LinearLayout column(){LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);return layout;}
 LinearLayout row(){LinearLayout layout=new LinearLayout(this);layout.setOrientation(0);layout.setGravity(Gravity.CENTER_VERTICAL);return layout;}
 LinearLayout card(){LinearLayout layout=column();layout.setPadding(dp(18),dp(14),dp(18),dp(14));layout.setBackground(shape(CARD,18));return layout;}
 LinearLayout listCard(){LinearLayout layout=card();layout.setBackground(shape(SURFACE,18));return layout;}

 @Override public void onCreate(Bundle saved){loadAppearance();super.onCreate(saved);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setSystemUiVisibility(lightTheme?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  if(!SetupActivity.ready(this)){startActivity(new Intent(this,SetupActivity.class));finish();return;}
  lastStatus=getPreferences(0).getString("lastOperation",lastStatus);String stored=getPreferences(0).getString("tree",null);if(stored!=null)tree=Uri.parse(stored);getPreferences(0).edit().remove("restoreTree").apply();buildShell();if(saved!=null){if(saved.getStringArrayList("selectedBackups")!=null)selectedBackups.addAll(saved.getStringArrayList("selectedBackups"));selected.addAll(saved.getStringArrayList("selected")==null?Collections.emptyList():saved.getStringArrayList("selected"));}selectedOnly=saved!=null&&saved.getBoolean("selectedOnly",false);showSection(saved==null?"Backup":saved.getString("section","Backup"));refreshData();BackupSchedule.ensureRegistered(this);if(getIntent().getBooleanExtra("settingsImportDone",false)){getIntent().removeExtra("settingsImportDone");showSection("Settings");showMessage("Settings imported","Preferences and theme restored. Automatic backups are off. Review Choose apps & schedule to enable them. Your backup folder, permissions and device verification were not changed.");}

 }

 void loadAppearance(){
  android.content.SharedPreferences prefs=getSharedPreferences("appearance",0);String mode=prefs.getString("mode","dark");
  lightTheme=mode.equals("light")||(mode.equals("system")&&(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES);
  setTheme(getResources().getIdentifier(lightTheme?"RavenThemeLight":"RavenTheme","style",getPackageName()));
  BG=Color.parseColor(lightTheme?"#F5F5F8":"#131519");SURFACE=Color.parseColor(lightTheme?"#E9EAF0":"#1F2228");CARD=Color.parseColor(lightTheme?"#FFFFFF":"#272B32");TEXT=Color.parseColor(lightTheme?"#20232B":"#F1F2F6");MUTED=Color.parseColor(lightTheme?"#586171":"#A9B0BC");
  ACCENT=prefs.getInt("accent",AppearanceColors.DEFAULT_ACCENT)|0xff000000;INK=AppearanceColors.ink(ACCENT);ACCENT_TEXT=AppearanceColors.readable(ACCENT,CARD);
 }
 AlertDialog.Builder dialog(){return new AlertDialog.Builder(this){@Override public AlertDialog create(){AlertDialog d=super.create();d.setOnShowListener(ignored->{openDialogs++;styleDialog(d);});d.setOnDismissListener(ignored->{openDialogs=Math.max(0,openDialogs-1);rebuildWhenReady();});return d;}};}
 void rebuildWhenReady(){getWindow().getDecorView().post(()->{if(pendingUiRebuild&&!busy&&openDialogs==0&&!isFinishing()&&!isDestroyed()){pendingUiRebuild=false;recreate();}});}
 @Override public void onConfigurationChanged(android.content.res.Configuration configuration){super.onConfigurationChanged(configuration);pendingUiRebuild=true;rebuildWhenReady();}
 void styleDialog(AlertDialog d){GradientDrawable background=shape(CARD,22);background.setStroke(dp(1),lightTheme?0xffD5D8E0:0xff404650);d.getWindow().setBackgroundDrawable(background);d.getWindow().getDecorView().setSystemUiVisibility(lightTheme?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0);tintControls(d.getWindow().getDecorView());for(int which:new int[]{-1,-2,-3}){Button b=d.getButton(which);if(b!=null)b.setTextColor(ACCENT_TEXT);}}
 void tintControls(View v){android.content.res.ColorStateList tint=new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{ACCENT_TEXT,MUTED});
  if(v instanceof CompoundButton)((CompoundButton)v).setButtonTintList(tint);
  if(v instanceof CheckedTextView)((CheckedTextView)v).setCheckMarkTintList(tint);
  if(v instanceof ProgressBar){((ProgressBar)v).setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT_TEXT));((ProgressBar)v).setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ACCENT_TEXT));}
  if(v instanceof SeekBar)((SeekBar)v).setThumbTintList(android.content.res.ColorStateList.valueOf(ACCENT_TEXT));
  if(v instanceof EditText){((EditText)v).setHighlightColor((ACCENT&0xffffff)|0x55000000);GradientDrawable cursor=shape(ACCENT_TEXT,1);cursor.setSize(dp(2),dp(22));((EditText)v).setTextCursorDrawable(cursor);}
  if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)tintControls(group.getChildAt(i));group.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener(){public void onChildViewAdded(View parent,View child){tintControls(child);}public void onChildViewRemoved(View parent,View child){}});}
 }
 Button colorPreset(int color,Runnable action){Button preset=new Button(this){final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);@Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);paint.setColor(color);float radius=Math.min(dp(16),Math.min(getWidth(),getHeight())/2f-dp(4));canvas.drawCircle(getWidth()/2f,getHeight()/2f,Math.max(0,radius),paint);if(color==Color.WHITE){paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(MUTED);canvas.drawCircle(getWidth()/2f,getHeight()/2f,Math.max(0,radius),paint);paint.setStyle(android.graphics.Paint.Style.FILL);}}};preset.setText("");preset.setBackground(shape(CARD,24));preset.setStateListAnimator(null);preset.setElevation(0);preset.setMinHeight(0);preset.setMinimumHeight(0);preset.setMinWidth(0);preset.setMinimumWidth(0);preset.setPadding(0,0,0,0);preset.setOnClickListener(v->{if(!busy)action.run();});return preset;}
 void showThemeSettings(LinearLayout settings){
  LinearLayout theme=card();add(theme,text("Theme",18,TEXT),-1,-2);gap(theme,8);String mode=getSharedPreferences("appearance",0).getString("mode","dark");String display=mode.equals("light")?"Light":mode.equals("system")?"Follow system":"Dark";
  LinearLayout appearanceRow=row();Button appearance=button("Appearance: "+display,true,()->{String[] values={"system","light","dark"};String[] labels={"Follow system","Light","Dark"};int choice=Arrays.asList(values).indexOf(mode);dialog().setTitle("Appearance").setSingleChoiceItems(labels,choice,(d,index)->{getSharedPreferences("appearance",0).edit().putString("mode",values[index]).apply();d.dismiss();recreate();}).setNegativeButton("Cancel",null).show();});addSettingsHalf(appearanceRow,appearance,SETTINGS_BUTTON_HEIGHT);Button accent=button("Accent color",true,this::chooseAccent);addSettingsHalf(appearanceRow,accent,SETTINGS_BUTTON_HEIGHT);add(theme,appearanceRow,-1,SETTINGS_BUTTON_HEIGHT);gap(theme,8);add(theme,text("Choose a color with the wheel, RGB sliders or a hex value. Text contrast adjusts for readability.",12,MUTED),-1,-2);add(settings,theme,-1,-2);gap(settings,12);
 }
 void chooseAccent(){
  LinearLayout layout=column();layout.setPadding(dp(24),dp(8),dp(24),dp(8));boolean narrow=getResources().getConfiguration().screenWidthDp<440;LinearLayout controls=narrow?column():row();ColorWheelView wheel=new ColorWheelView(this,ACCENT,TEXT);add(controls,wheel,176,176);LinearLayout fields=column();fields.setPadding(narrow?0:dp(20),0,0,0);controls.addView(fields,narrow?new LinearLayout.LayoutParams(-1,-2):new LinearLayout.LayoutParams(0,-2,1));TextView preview=text("Accent preview",16,INK);preview.setGravity(Gravity.CENTER);preview.setPadding(dp(8),dp(12),dp(8),dp(12));preview.setBackground(shape(ACCENT,16));add(fields,preview,-1,-2);gap(fields,8);
  EditText hex=new EditText(this);hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);hex.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);hex.setSingleLine(true);hex.setTextColor(TEXT);hex.setHintTextColor(MUTED);hex.setHint("#RRGGBB");hex.setContentDescription("Accent hex color");hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});add(fields,hex,-1,44);
  SeekBar[] rgb=new SeekBar[3];TextView[] rgbLabels=new TextView[3];String[] channels={"Red","Green","Blue"};
  for(int i=0;i<3;i++){LinearLayout line=row();rgbLabels[i]=text(channels[i]+" 255",12,TEXT);add(line,rgbLabels[i],76,36);rgb[i]=new SeekBar(this);rgb[i].setMax(255);rgb[i].setContentDescription(channels[i]+" channel");line.addView(rgb[i],new LinearLayout.LayoutParams(0,dp(36),1));add(fields,line,-1,36);}
  add(layout,controls,-1,-2);gap(layout,12);
  LinearLayout swatches=row();for(int color:new int[]{0xffFF8000,0xffFB0B24,0xff0070CC,0xff8000FF,0xffFFFF00,0xffFFFFFF}){Button swatch=colorPreset(color,()->wheel.setColor(color));swatch.setContentDescription(String.format(Locale.ROOT,"Use #%06X",color&0xffffff));swatches.addView(swatch,new LinearLayout.LayoutParams(0,dp(42),1));}add(layout,swatches,-1,42);
  final boolean[] updating={false};wheel.changed=color->{updating[0]=true;hex.setText(String.format(Locale.ROOT,"#%06X",color&0xffffff));hex.setError(null);int[] values={Color.red(color),Color.green(color),Color.blue(color)};for(int i=0;i<3;i++){rgb[i].setProgress(values[i]);rgbLabels[i].setText(channels[i]+" "+values[i]);rgb[i].setContentDescription(channels[i]+" channel "+values[i]+" of 255");}preview.setBackground(shape(color,16));preview.setTextColor(AppearanceColors.ink(color));updating[0]=false;};
  hex.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int after){}public void onTextChanged(CharSequence s,int a,int before,int c){if(updating[0])return;String value=s.toString();if(value.matches("#[0-9a-fA-F]{6}")){wheel.setColor(Color.parseColor(value));hex.setSelection(hex.length());}}public void afterTextChanged(Editable e){}});
  for(int i=0;i<3;i++)rgb[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int value,boolean user){if(user&&!updating[0])wheel.setColor(Color.rgb(rgb[0].getProgress(),rgb[1].getProgress(),rgb[2].getProgress()));}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}});
  wheel.setColor(ACCENT);
  ScrollView scroll=new ScrollView(this);scroll.addView(layout);AlertDialog popup=dialog().setTitle("Accent color").setView(scroll).setPositiveButton("Apply",null).setNeutralButton("Default",(d,w)->{getSharedPreferences("appearance",0).edit().remove("accent").apply();recreate();}).setNegativeButton("Cancel",null).create();popup.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);layout.setFocusableInTouchMode(true);layout.requestFocus();popup.show();popup.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(40),dp(600)),-2);popup.getButton(-1).setOnClickListener(v->{String value=hex.getText().toString();if(!value.matches("#[0-9a-fA-F]{6}")){hex.setError("Use #RRGGBB");return;}getSharedPreferences("appearance",0).edit().putInt("accent",Color.parseColor(value)).apply();popup.dismiss();recreate();});
 }
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("section",section);out.putBoolean("selectedOnly",selectedOnly);out.putStringArrayList("selectedBackups",new ArrayList<>(selectedBackups));out.putStringArrayList("selected",new ArrayList<>(selected));}
 ImageView appIcon(){ImageView image=new ImageView(this);try{image.setImageDrawable(getPackageManager().getApplicationIcon(getPackageName()));}catch(Exception ignored){}image.setContentDescription("Raven Backup");return image;}
 void buildShell(){navigation.clear();
  boolean landscape=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
  LinearLayout root=new LinearLayout(this);root.setOrientation(landscape?0:1);root.setBackgroundColor(BG);setContentView(root);
  nav=landscape?column():row();nav.setPadding(dp(12),dp(16),dp(12),dp(12));nav.setBackgroundColor(SURFACE);
  if(landscape){LinearLayout brandRow=row();add(brandRow,appIcon(),40,40);TextView brand=text("Raven\nBackup",18,TEXT);brand.setTypeface(null,Typeface.BOLD);brand.setPadding(dp(6),0,0,0);brandRow.addView(brand,new LinearLayout.LayoutParams(0,-2,1));add(nav,brandRow,-1,-2);gap(nav,24);add(root,nav,148,-1);}
  page=column();page.setPadding(dp(22),dp(16),dp(22),dp(12));root.addView(page,new LinearLayout.LayoutParams(landscape?0:-1,landscape?-1:0,1));
  LinearLayout header=row();if(!landscape){ImageView emblem=appIcon();LinearLayout.LayoutParams iconSpace=new LinearLayout.LayoutParams(dp(42),dp(42));iconSpace.setMargins(0,0,dp(10),0);header.addView(emblem,iconSpace);}LinearLayout names=column();if(!landscape)add(names,text("Raven Backup",12,MUTED),-1,-2);title=text("Backup",25,TEXT);title.setTypeface(null,Typeface.BOLD);subtitle=text("App and data backups",12,MUTED);add(names,title,-1,-2);add(names,subtitle,-1,-2);header.addView(names,new LinearLayout.LayoutParams(0,-2,1));Button overflow=overflowButton();add(header,overflow,48,44);add(page,header,-1,-2);gap(page,12);
  content=column();page.addView(content,new LinearLayout.LayoutParams(-1,0,1));
  status=text(lastStatus,12,MUTED);status.setMaxLines(2);status.setEllipsize(TextUtils.TruncateAt.END);status.setPadding(0,dp(8),0,dp(4));status.setOnClickListener(v->showMessage("Operation details",lastStatus));add(page,status,-1,-2);
  for(String name:Arrays.asList("Backup","Restore")){Button b=button(name,false,()->showSection(name));navigation.add(b);if(landscape){add(nav,b,-1,48);gap(nav,8);}else{LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);p.setMargins(dp(3),0,dp(3),0);nav.addView(b,p);}}
  if(landscape){View spacer=new View(this);nav.addView(spacer,new LinearLayout.LayoutParams(1,0,1));}else add(root,nav,-1,70);tintControls(root);root.getViewTreeObserver().addOnGlobalLayoutListener(()->{android.graphics.Rect visible=new android.graphics.Rect();root.getWindowVisibleDisplayFrame(visible);boolean showing=getWindowManager().getCurrentWindowMetrics().getBounds().bottom-visible.bottom>dp(140);if(showing==keyboardVisible)return;keyboardVisible=showing;header.setVisibility(showing?View.GONE:View.VISIBLE);status.setVisibility(showing?View.GONE:View.VISIBLE);if(footer!=null)footer.setVisibility(showing?View.GONE:View.VISIBLE);});
 }
 void footerAction(LinearLayout row,Button action,boolean expand){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(expand?0:-2,dp(44),expand?1:0);p.setMargins(0,0,dp(8),0);row.addView(action,p);}
 Button overflowButton(){Button b=new Button(this);b.setText("");b.setPadding(0,0,0,0);b.setBackgroundTintList(null);
  // Draw the dots in the background bounds so TextView font/scroll offsets cannot move them.
  b.setBackground(new GradientDrawable(){final android.graphics.Paint dots=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);{setColor(CARD);setCornerRadius(dp(24));}@Override public void draw(android.graphics.Canvas canvas){super.draw(canvas);android.graphics.Rect bounds=getBounds();dots.setColor(TEXT);for(int i=-1;i<=1;i++)canvas.drawCircle(bounds.exactCenterX(),bounds.exactCenterY()+i*dp(6),dp(1.7f),dots);}});
  b.setStateListAnimator(null);b.setElevation(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setContentDescription("More options");b.setOnClickListener(v->showMenu(b));return b;}
 void showMenu(View anchor){
  if(busy)return;
  PopupMenu menu=new PopupMenu(this,anchor);
  menu.getMenu().add("Settings").setOnMenuItemClickListener(item->{showSection("Settings");return true;});
  menu.getMenu().add("About").setOnMenuItemClickListener(item->{about();return true;});
  menu.getMenu().add("Report an issue").setOnMenuItemClickListener(item->{reportIssue();return true;});
  menu.show();
 }
 void reportIssue(){
  String issueUrl="https://github.com/Rsotbiemrptson/Raven-Backup/issues/new";
  String version="";
  try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
  catch(PackageManager.NameNotFoundException ignored){}
  String body="### Test environment\nRaven Backup version: "+version
   +"\nDevice model: \nAndroid and firmware version: \n"
   +"\n### Steps to reproduce\n\n### Expected result\n\n### Actual result or error message\n";
  Uri issue=Uri.parse(issueUrl).buildUpon().appendQueryParameter("body",body).build();
  try{startActivity(new Intent(Intent.ACTION_VIEW,issue));}
  catch(ActivityNotFoundException unavailable){
   showMessage("Open GitHub Issues","Open this address in a browser to report an issue:\n\n"+issueUrl);
  }
 }
 void about(){
  LinearLayout body=column();
  body.setPadding(dp(24),dp(8),dp(24),dp(4));
  ImageView icon=appIcon();
  LinearLayout.LayoutParams iconLayout=new LinearLayout.LayoutParams(dp(72),dp(72));
  iconLayout.gravity=Gravity.CENTER_HORIZONTAL;
  body.addView(icon,iconLayout);
  gap(body,8);

  TextView name=text("Raven Backup",22,TEXT);
  name.setTypeface(null,Typeface.BOLD);
  name.setGravity(Gravity.CENTER);
  add(body,name,-1,-2);
  String version="";
  try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}
  catch(PackageManager.NameNotFoundException ignored){}
  TextView release=text("Version "+version,13,ACCENT_TEXT);
  release.setGravity(Gravity.CENTER);
  add(body,release,-1,-2);
  gap(body,8);

  add(body,text("Your apps. Your progress.",16,ACCENT_TEXT),-1,-2);
  gap(body,8);
  add(body,text("Back up your apps and their data, keep multiple versions, and restore when you need them.",14,TEXT),-1,-2);
  gap(body,8);
  add(body,text("Build date: "+BuildInfo.BUILD_DATE+"\nCreator: Rsotbiemrptson",13,MUTED),-1,-2);
  gap(body,8);
  add(body,text("Open source · GNU GPL v3 only",13,MUTED),-1,-2);
  gap(body,8);

  TextView license=text("License",14,ACCENT_TEXT);
  license.setPaintFlags(license.getPaintFlags()|android.graphics.Paint.UNDERLINE_TEXT_FLAG);
  license.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
  license.setMinHeight(dp(44));
  license.setFocusable(true);
  license.setContentDescription("Read GNU GPL v3 license");
  license.setOnClickListener(v->showLicense());
  add(body,license,-2,-2);

  AlertDialog popup=dialog().setTitle("About").setView(body).setPositiveButton("Close",null).show();
  sizeInfoPopup(popup);
 }

 void sizeInfoPopup(AlertDialog popup){
  int availableWidth=getWindowManager().getCurrentWindowMetrics().getBounds().width()-dp(40);
  popup.getWindow().setLayout(Math.min(availableWidth,dp(INFO_POPUP_WIDTH_DP)),ViewGroup.LayoutParams.WRAP_CONTENT);
 }

 void showLicense(){try(java.io.InputStream in=getResources().openRawResource(getResources().getIdentifier("gpl","raw",getPackageName()))){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);AlertDialog popup=dialog().setTitle("GNU GPL v3 only").setMessage(new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8)).setPositiveButton("Close",null).show();sizeInfoPopup(popup);}catch(Exception e){showMessage("License","License text could not be opened.");}}
 @Override public void onBackPressed(){if(section.equals("Settings")&&!busy){showSection("Backup");return;}super.onBackPressed();}
 void showSection(String name){if(busy)return;if(name.equals("Apps"))name="Backup";section=name;query="";content.removeAllViews();title.setText(name.equals("Backup")?"Back up apps":name.equals("Restore")?"Restore backups":"Settings");
  for(int i=0;i<navigation.size();i++){boolean active=Arrays.asList("Backup","Restore").get(i).equals(name);navigation.get(i).setBackground(shape(active?ACCENT:CARD,24));navigation.get(i).setTextColor(active?INK:TEXT);}
  subtitle.setText(name.equals("Restore")?"Review a backup before replacing app data":name.equals("Settings")?"Storage and restore preferences":"Select one app or build a batch");
  if(name.equals("Settings")){showSettings();return;}
  LinearLayout tools=row();search=new EditText(this);search.setSingleLine(true);search.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setTextSize(14);search.setHint(name.equals("Restore")?"Search backups or apps":"Search app name or package");search.setOnEditorActionListener((view,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(),0);return true;}return false;});search.setPadding(dp(16),dp(8),dp(16),dp(8));search.setBackground(shape(SURFACE,24));tools.addView(search,new LinearLayout.LayoutParams(0,dp(44),1));
  if(!name.equals("Restore")){Button filters=button("Sort & filter",false,this::showFilters);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(132),dp(44));p.setMargins(dp(10),0,0,0);tools.addView(filters,p);}add(content,tools,-1,44);gap(content,10);
  summary=text("Loading…",12,MUTED);add(content,summary,-1,-2);gap(content,8);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);list=column();scroll.addView(list);content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  footer=row();footer.setVisibility(keyboardVisible?View.GONE:View.VISIBLE);footer.setPadding(0,dp(10),0,0);boolean wide=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;add(content,footer,-1,wide?54:110);
  if(name.equals("Restore")){
   selectBackups=button("Select all",false,()->{List<BackupCatalog.Entry> visible=visibleBackups();boolean all=!visible.isEmpty();for(BackupCatalog.Entry e:visible)if(!selectedBackups.contains(e.uri.toString()))all=false;for(BackupCatalog.Entry e:visible)if(all)selectedBackups.remove(e.uri.toString());else selectedBackups.add(e.uri.toString());renderBackups();});
   primary=button("Restore selected",true,this::reviewQueue);deleteBackups=button("Delete selected",false,()->confirmDelete(chosenBackups(),false));
   if(wide){footerAction(footer,button("Import ZIP",false,()->pickZip(12)),false);footerAction(footer,selectBackups,false);footerAction(footer,deleteBackups,false);footerAction(footer,primary,true);}
   else{footer.setOrientation(1);LinearLayout files=row();footerAction(files,button("Import ZIP",false,()->pickZip(12)),true);footerAction(files,selectBackups,true);add(footer,files,-1,44);gap(footer,8);LinearLayout batch=row();footerAction(batch,deleteBackups,true);footerAction(batch,primary,true);add(footer,batch,-1,44);}
  }else{
   showSelected=button(selectedOnly?"Show all":"Show selected",false,()->{selectedOnly=!selectedOnly;renderList();});Button clear=button("Clear",false,()->{selected.clear();renderList();});primary=button("Back up apps",true,()->confirmBackup(new ArrayList<>(selected)));
   Button selectAll=button("Select all",false,()->{
    for(AppRow app:visibleApps())selected.add(app.info.packageName);
    renderList();
   });
   Button refresh=button("Refresh",false,this::refreshData);
   if(wide){
    Button[] actions={refresh,showSelected,selectAll,clear};
    int width=0;
    for(Button action:actions){action.setSingleLine(true);action.setPadding(dp(10),action.getPaddingTop(),dp(10),action.getPaddingBottom());width=Math.max(width,
     (int)Math.ceil(action.getPaint().measureText(action==showSelected?"Show selected":action.getText().toString()))+action.getPaddingLeft()+action.getPaddingRight());}
    for(Button action:actions){LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(width,dp(44));params.setMargins(0,0,dp(8),0);footer.addView(action,params);}
    primary.setSingleLine(true);primary.setAutoSizeTextTypeUniformWithConfiguration(10,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);
    footer.addView(primary,new LinearLayout.LayoutParams(0,dp(44),1));
   }else{
    footer.setOrientation(1);
    LinearLayout toolsRow=row();
    footerAction(toolsRow,refresh,true);
    footerAction(toolsRow,showSelected,true);
    footerAction(toolsRow,selectAll,true);
    footerAction(toolsRow,clear,true);
    add(footer,toolsRow,-1,44);
    gap(footer,8);
    add(footer,primary,-1,44);
   }
  }
  search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString().trim().toLowerCase(Locale.ROOT);renderList();}public void afterTextChanged(Editable e){}});renderList();
 }
 void refreshData(){refreshData(null);}
 void refreshData(String completion){if(busy)return;int generation=++loadGeneration;Uri selectedTree=tree;loading=true;setStatus("Refreshing app and backup lists…");new Thread(()->{
   List<AppRow> apps=new ArrayList<>();for(ApplicationInfo info:getPackageManager().getInstalledApplications(0)){if(!BackupEngine.eligible(this,info))continue;AppRow app=new AppRow();app.info=info;app.label=getPackageManager().getApplicationLabel(info).toString();app.launchable=getPackageManager().getLaunchIntentForPackage(info.packageName)!=null;try{PackageInfo pi=getPackageManager().getPackageInfo(info.packageName,0);app.version=pi.versionName==null?"Version "+pi.getLongVersionCode():pi.versionName;app.updated=pi.lastUpdateTime;}catch(Exception e){app.version="Installed";}apps.add(app);}
   List<BackupCatalog.Entry> backups=new ArrayList<>();String error=null;try{backups=BackupCatalog.list(this,selectedTree);}catch(Exception e){error="Backup folder is unavailable. Choose it again in Settings.";try{backups=BackupCatalog.recoveries(this);}catch(Exception recoveryError){error+=" Recovery storage could not be read.";}}final List<BackupCatalog.Entry> found=backups;final String problem=error;
   runOnUiThread(()->{if(generation!=loadGeneration)return;loading=false;allApps=apps;catalog=found;Set<String> existingUris=new HashSet<>();for(BackupCatalog.Entry e:catalog)existingUris.add(e.uri.toString());selectedBackups.retainAll(existingUris);latest.clear();for(BackupCatalog.Entry backup:catalog)if(!backup.recovery&&backup.manifest!=null)try{org.json.JSONArray entries=backup.manifest.getJSONArray("apps");for(int i=0;i<entries.length();i++){String pkg=entries.getJSONObject(i).getString("package");if(!latest.containsKey(pkg))latest.put(pkg,backup);}}catch(Exception ignored){}if(!section.equals("Settings"))renderList();setStatus(problem==null?(completion==null?"Ready. "+apps.size()+" apps • "+catalog.size()+" backups available.":completion):problem);});
  }).start();}
 List<AppRow> visibleApps(){List<AppRow> visible=new ArrayList<>();for(AppRow app:allApps){if(!selectedOnly&&SystemAppData.systemSection(app.info)!=(filter==3))continue;if(selectedOnly&&!selected.contains(app.info.packageName))continue;if(!(app.label.toLowerCase(Locale.ROOT).contains(query)||app.info.packageName.toLowerCase(Locale.ROOT).contains(query)))continue;if(filter==1&&!latest.containsKey(app.info.packageName))continue;if(filter==2&&latest.containsKey(app.info.packageName))continue;visible.add(app);}visible.sort((a,b)->{if(sort==1)return a.info.packageName.compareTo(b.info.packageName);if(sort==2)return Long.compare(b.updated,a.updated);return a.label.compareToIgnoreCase(b.label);});return visible;}
 void updateAppSelection(){int count=selected.size();primary.setText(count==0?"Back up apps":"Back up "+count+(count==1?" app":" apps"));primary.setEnabled(!busy&&count>0);primary.setAlpha(primary.isEnabled()?1f:.55f);}
 void renderList(){if(list==null||section.equals("Settings"))return;list.removeAllViews();if(section.equals("Restore")){renderBackups();return;}
  if(showSelected!=null){showSelected.setText(selectedOnly?"Show all":"Show selected");showSelected.setBackground(shape(selectedOnly?ACCENT:CARD,24));showSelected.setTextColor(selectedOnly?INK:TEXT);}List<AppRow> visible=visibleApps();summary.setText(visible.size()+" apps • "+selected.size()+" selected"+(selectedOnly?" • Selected only":"")+(filter==0?"":" • Filter active"));updateAppSelection();
  if(visible.isEmpty()){empty(loading?"Loading your apps…":selectedOnly&&selected.isEmpty()?"No apps selected":"No matching apps",selectedOnly&&selected.isEmpty()?"Choose Show all to find apps and select them.":"Try another search or filter.");return;}
  boolean landscape=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
  for(AppRow app:visible){
   LinearLayout card=listCard();if(landscape)card.setPadding(dp(16),dp(10),dp(16),dp(10));LinearLayout line=row();ImageView icon=new ImageView(this);try{icon.setImageDrawable(getPackageManager().getApplicationIcon(app.info));}catch(Exception ignored){}add(line,icon,40,40);
   LinearLayout labels=column();labels.setPadding(dp(14),0,dp(8),0);TextView name=text(app.label,16,TEXT);name.setTypeface(null,Typeface.BOLD);name.setMaxLines(1);name.setEllipsize(TextUtils.TruncateAt.END);add(labels,name,-1,-2);TextView pkg=text(app.info.packageName,11,MUTED);pkg.setMaxLines(1);pkg.setEllipsize(TextUtils.TruncateAt.END);add(labels,pkg,-1,-2);line.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
   BackupCatalog.Entry latestBackup=latest.get(app.info.packageName);String backupText=latestBackup==null?"No backup in this folder":"Backed up "+date(latestBackup);
   if(landscape){LinearLayout metadata=column();add(metadata,text((SystemAppData.system(app.info)?"System app · data only • ":"User app • ")+app.version,11,MUTED),-1,-2);add(metadata,text(backupText,11,latestBackup==null?MUTED:AppearanceColors.readable(ACCENT,SURFACE)),-1,-2);add(line,metadata,190,-2);}
   Switch toggle=accentSwitch("",selected.contains(app.info.packageName));toggle.setContentDescription("Select "+app.label);toggle.setOnCheckedChangeListener((v,on)->{if(on)selected.add(app.info.packageName);else selected.remove(app.info.packageName);if(selectedOnly)renderList();else{summary.setText(visibleApps().size()+" apps • "+selected.size()+" selected");updateAppSelection();}});add(line,toggle,56,48);add(card,line,-1,-2);
   if(!landscape){gap(card,8);add(card,text((SystemAppData.system(app.info)?"System app · data only • ":"User app • ")+app.version+" • "+backupText,11,latestBackup==null?MUTED:AppearanceColors.readable(ACCENT,SURFACE)),-1,-2);}labels.setOnClickListener(v->details(app));icon.setOnClickListener(v->details(app));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,0,0,dp(8));list.addView(card,p);
  }
 }
 List<BackupCatalog.Entry> visibleBackups(){List<BackupCatalog.Entry> rows=new ArrayList<>();for(BackupCatalog.Entry e:catalog)if(query.isEmpty()||(e.name+" "+backupNames(e)+(e.recovery?" recovery":"")).toLowerCase(Locale.ROOT).contains(query))rows.add(e);return rows;}
 List<BackupCatalog.Entry> chosenBackups(){List<BackupCatalog.Entry> rows=new ArrayList<>();for(String uri:selectedBackups)for(BackupCatalog.Entry e:catalog)if(uri.equals(e.uri.toString())){rows.add(e);break;}return rows;}
 void updateBackupSelection(){List<BackupCatalog.Entry> visible=visibleBackups();summary.setText(visible.size()+" backups • "+selectedBackups.size()+" selected");primary.setText(selectedBackups.isEmpty()?"Restore selected":"Restore "+selectedBackups.size()+" selected");primary.setEnabled(!busy&&!selectedBackups.isEmpty());deleteBackups.setEnabled(!busy&&!selectedBackups.isEmpty());boolean all=!visible.isEmpty();for(BackupCatalog.Entry e:visible)if(!selectedBackups.contains(e.uri.toString()))all=false;selectBackups.setText(all?"Clear selection":"Select all");}
 void renderBackups(){list.removeAllViews();List<BackupCatalog.Entry> visible=visibleBackups();for(BackupCatalog.Entry entry:visible){LinearLayout card=listCard();LinearLayout heading=row();TextView label=text(backupNames(entry),16,TEXT);label.setTypeface(null,Typeface.BOLD);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));if(entry.validationProblem!=null){TextView warning=text("Check failed",12,AppearanceColors.readable(ACCENT,SURFACE));warning.setPadding(dp(10),dp(4),dp(10),dp(4));add(heading,warning,-2,-2);}if(entry.recovery){TextView tag=text("Recovery",12,INK);tag.setPadding(dp(10),dp(4),dp(10),dp(4));tag.setBackground(shape(ACCENT,12));add(heading,tag,-2,-2);}Switch toggle=accentSwitch("",selectedBackups.contains(entry.uri.toString()));toggle.setContentDescription("Select backup "+backupNames(entry)+" "+entry.name);toggle.setOnCheckedChangeListener((v,on)->{if(on)selectedBackups.add(entry.uri.toString());else selectedBackups.remove(entry.uri.toString());updateBackupSelection();});add(heading,toggle,56,48);add(card,heading,-1,-2);gap(card,5);add(card,text(date(entry)+"  •  "+size(entry.size)+"  •  "+backupContents(entry),12,AppearanceColors.readable(ACCENT,SURFACE)),-1,-2);add(card,text(entry.name,11,MUTED),-1,-2);if(entry.validationProblem!=null)add(card,text(entry.validationProblem,12,TEXT),-1,-2);gap(card,10);LinearLayout actions=row();Button restore=button(entry.recovery?"Recovery restore":"Review & restore",true,()->reviewLive(entry.uri));add(actions,restore,-2,38);Button details=button("Details",false,()->backupDetails(entry));details.setBackground(shape(CARD,24));LinearLayout.LayoutParams space=new LinearLayout.LayoutParams(-2,dp(38));space.setMargins(dp(10),0,0,0);actions.addView(details,space);add(card,actions,-1,-2);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(10));list.addView(card,cp);}updateBackupSelection();if(visible.isEmpty())empty(tree==null?"Choose your backup folder":"No matching backups",tree==null?"Choose a backup folder in Settings to show your backup ZIPs here.":"You can also import a ZIP from another location.");}
 void empty(String title,String message){LinearLayout card=listCard();add(card,text(title,19,TEXT),-1,-2);gap(card,8);add(card,text(message,13,MUTED),-1,-2);add(list,card,-1,-2);}
 String date(BackupCatalog.Entry entry){long time=entry.modified;if(entry.manifest!=null)time=entry.manifest.optLong("created",time);return DateFormat.getDateTimeInstance(DateFormat.SHORT,entry.recovery?DateFormat.MEDIUM:DateFormat.SHORT).format(new Date(time));}
 String size(long bytes){if(bytes>=1024L*1024*1024)return String.format(Locale.ROOT,"%.1f GiB",bytes/(1024.0*1024*1024));return bytes>=1024*1024?String.format(Locale.ROOT,"%.1f MB",bytes/(1024.0*1024)):String.format(Locale.ROOT,"%.1f KB",bytes/1024.0);}
 String backupNames(BackupCatalog.Entry entry){if(entry.manifest==null)return "Backup ZIP";try{org.json.JSONArray apps=entry.manifest.getJSONArray("apps");List<String> names=new ArrayList<>();for(int i=0;i<Math.min(2,apps.length());i++){JSONObject saved=apps.getJSONObject(i);names.add(AppNames.saved(this,saved,entry.uri));}return String.join(", ",names)+(apps.length()>2?" + "+(apps.length()-2)+" more":"");}catch(Exception e){return entry.name;}}
 void showFilters(){LinearLayout layout=column();layout.setPadding(dp(24),dp(16),dp(24),dp(16));TextView ft=new TextView(this);ft.setText("Show");add(layout,ft,-1,-2);Spinner f=new Spinner(this);f.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"All user apps","With backups","Without backups","System apps & services"}));f.setSelection(filter);add(layout,f,-1,48);TextView st=new TextView(this);st.setText("Sort by");add(layout,st,-1,-2);Spinner s=new Spinner(this);s.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"App name","Package name","Recently updated"}));s.setSelection(sort);add(layout,s,-1,48);dialog().setTitle("Sort & filter").setView(layout).setPositiveButton("Apply",(d,w)->{filter=f.getSelectedItemPosition();sort=s.getSelectedItemPosition();renderList();}).setNeutralButton("Reset",(d,w)->{filter=0;sort=0;renderList();}).setNegativeButton("Cancel",null).show();}
 void details(AppRow app){final AlertDialog[] detailPopup=new AlertDialog[1];BackupCatalog.Entry backup=latest.get(app.info.packageName);String message=app.info.packageName+"\n"+app.version+(SystemAppData.system(app.info)?"\nSystem app • data only\n":"\nUser app • private data\n")+(backup==null?"No backup in the chosen folder":"Latest backup: "+date(backup));LinearLayout extra=column();extra.setPadding(dp(24),dp(8),dp(24),dp(8));add(extra,text(message,14,TEXT),-1,-2);gap(extra,12);if(!SystemAppData.system(app.info))add(extra,button("Repair Google Play link",false,()->{if(detailPopup[0]!=null)detailPopup[0].dismiss();confirmPlayLink(app);}), -2,42);detailPopup[0]=dialog().setTitle(app.label).setView(extra).setPositiveButton("Back up",(d,w)->confirmBackup(Arrays.asList(app.info.packageName))).setNeutralButton("History",(d,w)->showHistory(app)).setNegativeButton("Close",null).show();}
 String backupContents(BackupCatalog.Entry entry){if(entry.manifest!=null&&(entry.manifest.has("external")||entry.manifest.optInt("format")==ExternalData.STREAM_FORMAT))return "Includes external data";if(entry.recovery)return "Recovery • app data before restore";if(entry.manifest!=null)try{JSONArray apps=entry.manifest.getJSONArray("apps");for(int i=0;i<apps.length();i++)if(apps.getJSONObject(i).has("apks"))return "App + data";}catch(Exception ignored){}return "Data only";}
 void confirmPlayLink(AppRow app){dialog().setTitle("Restore Google Play link?").setMessage(app.label+"\n\nUse this only if the app originally came from Google Play. It reinstalls the same signed APKs with app data retained to repair Android’s store attribution. It does not grant a license or subscription. The app will close during the repair.").setPositiveButton("Repair link",(d,w)->work("Repairing Play link",()->{return ApkBackup.repairPlayLink(new BackupEngine(this),app.info.packageName);})).setNegativeButton("Cancel",null).show();}
 void backupDetails(BackupCatalog.Entry entry){StringBuilder message=new StringBuilder(entry.name+"\n"+date(entry)+" • "+size(entry.size)+" • "+backupContents(entry)+"\n\n");if(entry.manifest!=null)try{message.append(FirmwareCheck.source(entry.manifest)).append("\n\n");org.json.JSONArray apps=entry.manifest.getJSONArray("apps");for(int i=0;i<apps.length();i++){org.json.JSONObject app=apps.getJSONObject(i);message.append(AppNames.saved(this,app,entry.uri)).append("\n").append(app.getString("package")).append(" • version ").append(app.getLong("version")).append('\n');}}catch(Exception ignored){}message.append("\nContents and compatibility are checked when you review a restore.");dialog().setTitle("Backup details").setMessage(message.toString()).setPositiveButton("Review restore",(d,w)->reviewLive(entry.uri)).setNeutralButton("Rehearse",(d,w)->confirmRehearsal(entry.uri)).setNegativeButton("Close",null).show();}
 String backupOrderLabel(int order){return BACKUP_ORDER_LABELS[BackupOrder.key(order)]+(BackupOrder.descending(order)?" ↓ Descending":" ↑ Ascending");}
 void chooseBackupOrder(){
  int current=BackupOrder.normalize(getPreferences(0).getInt("backupOrder",BackupOrder.PACKAGE));
  LinearLayout body=column();body.setPadding(dp(24),dp(8),dp(24),dp(16));
  add(body,text("Sort by",14,TEXT),-1,-2);
  Spinner key=new Spinner(this);key.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,BACKUP_ORDER_LABELS));key.setSelection(BackupOrder.key(current));add(body,key,-1,48);gap(body,12);
  final boolean[] descending={BackupOrder.descending(current)};
  Button[] directions=new Button[2];TextView hint=text("",14,MUTED);
  Runnable update=()->{
   for(int i=0;i<directions.length;i++)if(directions[i]!=null){boolean active=(i==1)==descending[0];directions[i].setBackground(shape(active?ACCENT:SURFACE,24));directions[i].setTextColor(active?INK:TEXT);directions[i].setSelected(active);directions[i].setContentDescription((i==0?"Ascending":"Descending")+(active?", selected":""));}
   hint.setText(key.getSelectedItemPosition()==BackupOrder.SIZE?(descending[0]?"Largest first":"Smallest first"):(descending[0]?"Z–A":"A–Z"));
  };
  directions[0]=button("↑ Ascending",!descending[0],()->{descending[0]=false;update.run();});
  directions[1]=button("↓ Descending",descending[0],()->{descending[0]=true;update.run();});
  LinearLayout choices=row();footerAction(choices,directions[0],true);footerAction(choices,directions[1],true);add(body,choices,-1,44);gap(body,10);add(body,hint,-1,-2);
  key.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> parent,View view,int position,long id){update.run();}public void onNothingSelected(AdapterView<?> parent){}});update.run();
  dialog().setTitle("Backup order").setView(body).setPositiveButton("Save",(d,w)->{
   getPreferences(0).edit().putInt("backupOrder",BackupOrder.value(key.getSelectedItemPosition(),descending[0])).apply();showSection("Settings");
  }).setNegativeButton("Cancel",null).show();
 }
 void showSettings(){ScrollView scroll=new ScrollView(this);LinearLayout settings=column();scroll.addView(settings);add(content,scroll,-1,-1);showThemeSettings(settings);
  showAutomaticBackupSettings(settings);
  showSettingsTransfer(settings);
  LinearLayout backup=card();
  add(backup,text("Backup preferences",18,TEXT),-1,-2);gap(backup,12);
  LinearLayout backupActions=row();backupActions.setGravity(Gravity.TOP);backupActions.setBaselineAligned(false);
  settingsButtonColumn(backupActions,button("Choose backup folder",true,this::chooseFolder),
   tree==null?"Choose where Raven saves and looks for backups.":"Selected folder: "+BackupEngine.folderName(tree));
  settingsButtonColumn(backupActions,button("Manage recovery backups",true,this::reviewRecoveryCleanup),
   "Review or delete private recovery copies. Uninstalling Raven removes these copies.");
  add(backup,backupActions,-1,-2);gap(backup,12);
  if(!Environment.isExternalStorageManager()){
   LinearLayout access=row();
   addSettingsHalf(access,button("Allow internal-storage",true,()->startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName())))),SETTINGS_BUTTON_HEIGHT);
   addSettingsHalf(access,new View(this),SETTINGS_BUTTON_HEIGHT);
   add(backup,access,-1,SETTINGS_BUTTON_HEIGHT);gap(backup,8);
  }
  if(!BackupEngine.canReadSizes(this)){
   LinearLayout sizes=row();
   addSettingsHalf(sizes,button("Allow app size access",true,()->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))),SETTINGS_BUTTON_HEIGHT);
   addSettingsHalf(sizes,new View(this),SETTINGS_BUTTON_HEIGHT);
   add(backup,sizes,-1,SETTINGS_BUTTON_HEIGHT);gap(backup,8);
   add(backup,text("Optional Usage access lets preparation use Android’s app sizes. Otherwise Raven counts files.",13,MUTED),-1,-2);
   gap(backup,12);
  }
  int savedOrder=BackupOrder.normalize(getPreferences(0).getInt("backupOrder",BackupOrder.PACKAGE));
  LinearLayout orderRow=row();orderRow.setGravity(Gravity.TOP);orderRow.setBaselineAligned(false);
  settingsButtonColumn(orderRow,button("Backup order",true,this::chooseBackupOrder),
   "Current: "+backupOrderLabel(savedOrder)+". Applies to manual and scheduled backups.");
  addSettingsHalf(orderRow,new View(this),SETTINGS_BUTTON_HEIGHT);
  add(backup,orderRow,-1,-2);gap(backup,8);
  add(backup,text("Preparation uses Android app sizes when available, otherwise counts files. ZIP sizes may differ.",13,MUTED),-1,-2);gap(backup,12);
  addSettingsPreference(backup,"Include app APKs and installed splits","includeApks",true,
   "Save installers with your data for manual and scheduled backups. Game installers can be large.");
  addSettingsPreference(backup,"Include external app data",RestorePreferences.EXTERNAL,true,
   "Include the app’s primary Android/data files in new backups. Large backups need extra space on the backup drive.");
  add(settings,backup,-1,-2);gap(settings,12);
  LinearLayout restore=card();
  add(restore,text("Restore preferences",18,TEXT),-1,-2);
  addSettingsPreference(restore,"Open apps after restore","launchAfter",false,
   "Launch restored apps after the restore finishes successfully.");
  addSettingsPreference(restore,RestorePreferences.SIGNATURE_LABEL,RestorePreferences.SIGNATURE,false,
   "Restore data into a copy of the same app signed with another key. Enable only for a copy you trust.");
  addSettingsPreference(restore,RestorePreferences.VERSION_LABEL,RestorePreferences.VERSION,false,
   "Restore data across app versions. The installed version may be unable to read some data.");
  add(settings,restore,-1,-2);gap(settings,12);
  LinearLayout advanced=card(),tools=column(),advancedHeader=row();TextView advancedTitle=text("Advanced",18,TEXT);advancedHeader.addView(advancedTitle,new LinearLayout.LayoutParams(0,-2,1));
  View chevron=new View(this){final android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);@Override protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);paint.setColor(TEXT);paint.setStyle(android.graphics.Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);float x=getWidth()/2f,y=getHeight()/2f,d=tools.getVisibility()==View.VISIBLE?-1:1;android.graphics.Path path=new android.graphics.Path();path.moveTo(x-dp(6),y-d*dp(3));path.lineTo(x,y+d*dp(3));path.lineTo(x+dp(6),y-d*dp(3));canvas.drawPath(path,paint);}};
  chevron.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);add(advancedHeader,chevron,32,32);advancedHeader.setContentDescription("Expand Advanced");advancedHeader.setFocusable(true);advancedHeader.setClickable(true);add(advanced,advancedHeader,-1,48);tools.setVisibility(View.GONE);add(advanced,tools,-1,-2);advancedHeader.setOnClickListener(v->{if(busy)return;boolean expand=tools.getVisibility()!=View.VISIBLE;tools.setVisibility(expand?View.VISIBLE:View.GONE);advancedHeader.setContentDescription(expand?"Collapse Advanced":"Expand Advanced");chevron.invalidate();});gap(tools,12);
  advancedAction(tools,"Rerun Guided Setup",this::confirmRestoreSetup,"Check storage and restore with a test app.");
  advancedAction(tools,"Verify backup folder",this::verifyBackupLibrary,"Check all archives on demand.");
  advancedAction(tools,"Test root access",()->work(()->{new BackupEngine(this).probe();return "Root access confirmed.";}),"Check access to the root-script service.");
  advancedAction(tools,"Recover pending restore",()->dialog().setTitle("Recover pending restore?").setMessage("Return affected apps to their state before the interrupted restore. Recovered apps remain stopped.").setPositiveButton("Recover",(d,w)->work(()->new LiveRestore(this).recover())).setNegativeButton("Cancel",null).show(),"Undo an interrupted restore. Apps stay stopped.");
  advancedAction(tools,"Rehearse a restore",()->pickZip(11),"Test a backup without changing app data.");
  advancedAction(tools,"Purge all backups",this::choosePurge,"Delete all files and subfolders in the backup folder.");  add(settings,advanced,-1,-2);
 }
 static final int SETTINGS_BUTTON_HEIGHT=44;
 void addSettingsHalf(LinearLayout row,View view,int height){
  if(view instanceof Button)fitSettingsButton((Button)view);
  LinearLayout.LayoutParams space=new LinearLayout.LayoutParams(0,height<0?height:dp(height),1);
  if(row.getChildCount()>0)space.leftMargin=dp(10);
  row.addView(view,space);
 }
 void fitSettingsButton(Button action){
  action.setMinWidth(0);action.setMinimumWidth(0);action.setMaxLines(2);
  action.setPadding(dp(8),0,dp(8),0);
 }
 void settingsButtonColumn(LinearLayout row,Button action,String description){
  fitSettingsButton(action);
  LinearLayout item=column();
  add(item,action,-1,SETTINGS_BUTTON_HEIGHT);
  gap(item,6);add(item,text(description,12,MUTED),-1,-2);
  addSettingsHalf(row,item,-2);
 }
 void showAutomaticBackupSettings(LinearLayout settings){
  LinearLayout scheduling=card();
  add(scheduling,text("Automatic backups",18,TEXT),-1,-2);gap(scheduling,12);
  LinearLayout actions=row();
  addSettingsHalf(actions,button("Choose apps & schedule",true,this::chooseScheduleApps),SETTINGS_BUTTON_HEIGHT);
  addSettingsHalf(actions,button("Disable schedule",true,()->{
   BackupSchedule.disable(this);showSection("Settings");setStatus("Automatic backups disabled.");
  }),SETTINGS_BUTTON_HEIGHT);
  add(scheduling,actions,-1,SETTINGS_BUTTON_HEIGHT);gap(scheduling,6);
  add(scheduling,text(BackupSchedule.summary(this),13,ACCENT_TEXT),-1,-2);
  long lastRun=BackupSchedule.prefs(this).getLong("lastRun",0);
  if(lastRun>0)add(scheduling,text("Last run: "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(lastRun))+"\n"+BackupSchedule.prefs(this).getString("lastResult",""),12,MUTED),-1,-2);
  gap(scheduling,16);
  LinearLayout background=row();
  addSettingsHalf(background,button("Allow background backups",true,this::requestBackgroundAllowance),SETTINGS_BUTTON_HEIGHT);
  addSettingsHalf(background,new View(this),SETTINGS_BUTTON_HEIGHT);
  add(scheduling,background,-1,SETTINGS_BUTTON_HEIGHT);gap(scheduling,6);
  boolean exempt=((PowerManager)getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName());
  add(scheduling,text("Battery optimization: "+(exempt?"exempt":"not exempt")+". Allowing background use can reduce restrictions; timing remains approximate.",12,MUTED),-1,-2);
  add(settings,scheduling,-1,-2);gap(settings,12);
 }
 void advancedAction(LinearLayout parent,String label,Runnable action,String description){
  View last=parent.getChildCount()==0?null:parent.getChildAt(parent.getChildCount()-1);
  LinearLayout actions;
  if(last instanceof LinearLayout&&((LinearLayout)last).getChildCount()==1)actions=(LinearLayout)last;
  else{
   actions=row();actions.setGravity(Gravity.TOP);actions.setBaselineAligned(false);
   LinearLayout.LayoutParams space=new LinearLayout.LayoutParams(-1,-2);space.bottomMargin=dp(8);parent.addView(actions,space);
  }
  Button actionButton=button(label,false,action);actionButton.setBackground(shape(SURFACE,24));
  settingsButtonColumn(actions,actionButton,description);
 }
 boolean requireRestoreSetup(){if(SetupActivity.ready(this))return true;confirmRestoreSetup();return false;}
 void confirmRestoreSetup(){startActivity(new Intent(this,SetupActivity.class));}
 void confirmDelete(List<BackupCatalog.Entry> entries,boolean purge){if(entries.isEmpty()){showMessage("Nothing to delete",purge?"There are no backup ZIPs in the chosen folder.":"Select backup ZIPs first.");return;}Uri reviewedTree=tree;StringBuilder names=new StringBuilder();long bytes=0;for(BackupCatalog.Entry e:entries){names.append(backupNames(e)).append("\n").append(e.name).append("\n\n");bytes+=e.size;}TextView detail=text("Permanently delete "+entries.size()+" ZIPs ("+size(bytes)+") from their listed locations?\n\n"+names+"This cannot be undone. Installed apps and other files are not removed.",14,TEXT);detail.setPadding(dp(24),dp(8),dp(24),dp(8));ScrollView scroll=new ScrollView(this);scroll.addView(detail);dialog().setTitle(purge?"Purge these backups?":"Delete selected backups?").setView(scroll).setPositiveButton("Delete "+entries.size()+" ZIPs",(d,w)->work("Deleting backups",()->{String result=BackupStorage.delete(this,reviewedTree,entries);runOnUiThread(()->{for(BackupCatalog.Entry e:entries)selectedBackups.remove(e.uri.toString());});return result;})).setNegativeButton("Cancel",null).show();}
 void reviewRecoveryCleanup(){final List<BackupCatalog.Entry>[] reviewed=new List[]{null};work("Reading recovery backups",()->{synchronized(LiveRestore.class){if(new LiveRestore(this).pointer.exists())throw new java.io.IOException("Recover the interrupted restore first.");reviewed[0]=BackupCatalog.recoveries(this);return "Recovery backups reviewed.";}},()->showRecoveryManager(reviewed[0]));}
 void showRecoveryManager(List<BackupCatalog.Entry> entries){
  if(entries.isEmpty()){showMessage("No recovery backups","There are no saved recovery backups to manage.");return;}
  String[] labels=new String[entries.size()];boolean[] checked=new boolean[entries.size()];for(int i=0;i<entries.size();i++){BackupCatalog.Entry e=entries.get(i);labels[i]=backupNames(e)+"\n"+date(e)+" • "+size(e.size);}
  final AlertDialog[] holder=new AlertDialog[1];Runnable update=()->{int count=0;for(boolean on:checked)if(on)count++;holder[0].getButton(-1).setText(count==0?"Delete selected":"Delete "+count+" selected");holder[0].getButton(-1).setEnabled(count>0);holder[0].getButton(-1).setAlpha(count>0?1f:.45f);holder[0].getButton(-3).setText(count==checked.length?"Clear selection":"Select all");};
  holder[0]=dialog().setTitle("Manage recovery backups").setMultiChoiceItems(labels,checked,(d,index,on)->{checked[index]=on;update.run();}).setPositiveButton("Delete selected",(d,w)->{List<BackupCatalog.Entry> selected=new ArrayList<>();for(int i=0;i<entries.size();i++)if(checked[i])selected.add(entries.get(i));confirmRecoveryDeletion(selected);}).setNeutralButton("Select all",null).setNegativeButton("Cancel",null).create();holder[0].show();update.run();holder[0].getButton(-3).setOnClickListener(v->{boolean all=true;for(boolean on:checked)if(!on)all=false;for(int i=0;i<checked.length;i++){checked[i]=!all;holder[0].getListView().setItemChecked(i,!all);}update.run();});
 }
 void confirmRecoveryDeletion(List<BackupCatalog.Entry> entries){if(entries.isEmpty())return;long bytes=0;StringBuilder names=new StringBuilder();for(BackupCatalog.Entry e:entries){bytes+=e.size;names.append(backupNames(e)).append(" • ").append(date(e)).append("\n");}TextView detail=text("Permanently delete "+entries.size()+" recovery backup"+(entries.size()==1?"":"s")+" ("+size(bytes)+")? Confirm your restored apps work and their data is correct first.\n\n"+names+"\nNormal backups and installed app data will be kept. This cannot be undone.",14,TEXT);detail.setPadding(dp(24),dp(8),dp(24),dp(8));ScrollView scroll=new ScrollView(this);scroll.addView(detail);dialog().setTitle("Delete recovery backups?").setView(scroll).setPositiveButton("Delete",(d,w)->work("Deleting recovery backups",()->BackupStorage.delete(this,tree,entries))).setNegativeButton("Cancel",(d,w)->reviewRecoveryCleanup()).show();}
 void choosePurge(){if(tree==null){showMessage("Choose a backup folder","Choose a backup folder before purging.");return;}Uri reviewedTree=tree;final BackupStorage.PurgePlan[] plan=new BackupStorage.PurgePlan[1];work("Reviewing backup storage",()->{plan[0]=BackupStorage.reviewPurge(this,reviewedTree);return "All folder contents reviewed.";},()->confirmPurge(plan[0]));}
 void confirmPurge(BackupStorage.PurgePlan plan){if(plan.items.isEmpty()){showMessage("Folder is empty","The selected backup folder is already empty.");return;}StringBuilder names=new StringBuilder();for(BackupStorage.PurgeItem item:plan.items)names.append(item.folder?"Folder: ":"File: ").append(item.path).append("\n");TextView detail=text("Empty "+BackupEngine.folderName(plan.tree)+"?\n\nPermanently delete ALL "+plan.files+" files ("+size(plan.bytes)+") and "+plan.folders+" subfolders, including non-ZIP and unfinished files. The selected backup folder itself will be kept.\n\n"+names+"\nThis cannot be undone. Installed app data is not removed.",14,TEXT);detail.setPadding(dp(24),dp(8),dp(24),dp(8));ScrollView scroll=new ScrollView(this);scroll.addView(detail);dialog().setTitle("Empty backup folder?").setView(scroll).setPositiveButton("Empty folder",(d,w)->work("Purging backup storage",()->BackupStorage.purge(this,plan))).setNegativeButton("Cancel",null).show();}
 void addSettingsPreference(LinearLayout card,String title,String key,boolean defaultValue,String description){
  Switch toggle=accentSwitch(title,getPreferences(0).getBoolean(key,defaultValue));
  toggle.setOnCheckedChangeListener((v,on)->getPreferences(0).edit().putBoolean(key,on).apply());
  add(card,toggle,-1,-2);
  add(card,text(description,12,MUTED),-1,-2);
  gap(card,8);
 }
 Switch accentSwitch(String title,boolean on){
  Switch toggle=new Switch(this);toggle.setText(title);toggle.setTextColor(TEXT);toggle.setChecked(on);
  int[][] states={{android.R.attr.state_checked},{}};
  toggle.setThumbTintList(new android.content.res.ColorStateList(states,new int[]{ACCENT,MUTED}));
  toggle.setTrackTintList(new android.content.res.ColorStateList(states,new int[]{ACCENT,0x66000000|(MUTED&0xffffff)}));
  return toggle;
 }
 void reviewQueue(){if(!requireRestoreSetup())return;List<BackupCatalog.Entry> entries=chosenBackups();if(entries.isEmpty())return;RestoreQueue queue=new RestoreQueue(this);work("Checking restore queue",()->{queue.prepare(entries,(current,total,name)->runOnUiThread(()->{OperationService.progress("Reviewing "+current+" of "+total,name);if(operationPopup!=null){operationPopup.setTitle("Reviewing "+current+" of "+total);if(operationMessage!=null)operationMessage.setText(name+"\nReading app information and private data…");}}));return "Selected archives reviewed. "+queue.invalid.size()+" need attention.";},()->{if(queue.invalid.isEmpty())showQueueReview(queue);else showBackupProblems(queue.invalid,queue.hasValidArchives()?()->showQueueReview(queue):null,queue::close);});}
 void verifyBackupLibrary(){final LibraryCheck.Verification[] checked=new LibraryCheck.Verification[1];work("Verifying backup folder",()->{String root=new BackupEngine(this).probe();String access=LibraryCheck.folderAccess(this,tree);checked[0]=LibraryCheck.inspect(this,tree);return root+"\n"+access+"\n"+checked[0].report();},()->{if(checked[0].failed.isEmpty())showMessage("Verification complete",checked[0].report());else showBackupProblems(checked[0].failed,null,()->{});});}
 void showBackupProblems(List<BackupCatalog.Entry> entries,Runnable proceed,Runnable close){
  String[] labels=new String[entries.size()];boolean[] checked=new boolean[entries.size()];for(int i=0;i<entries.size();i++){BackupCatalog.Entry e=entries.get(i);labels[i]=e.name+"\n"+e.validationProblem;}
  final AlertDialog[] holder=new AlertDialog[1];Runnable update=()->{int count=0;for(boolean on:checked)if(on)count++;holder[0].getButton(-3).setEnabled(count>0);holder[0].getButton(-3).setText(count==0?"Delete selected":"Delete "+count+" selected");};
  holder[0]=dialog().setTitle("Backups need attention").setMultiChoiceItems(labels,checked,(d,index,on)->{checked[index]=on;update.run();}).setPositiveButton(proceed==null?"Close":"Continue without these",(d,w)->{if(proceed!=null)proceed.run();else close.run();}).setNeutralButton("Delete selected",(d,w)->{close.run();List<BackupCatalog.Entry> remove=new ArrayList<>();for(int i=0;i<checked.length;i++)if(checked[i])remove.add(entries.get(i));confirmDelete(remove,false);}).setNegativeButton("Cancel",(d,w)->close.run()).setOnCancelListener(d->close.run()).create();holder[0].show();update.run();
 }
 void showQueueReview(RestoreQueue queue){try{LinearLayout layout=column();layout.setPadding(dp(24),dp(8),dp(24),dp(8));add(layout,text(queue.description()+"Backups run in selection order. Apps stop during restore. Incompatible apps are skipped and listed in the final report. A data-restore or recovery failure stops the queue; earlier successful restores remain in place. Recovery backups are kept until cleared in Settings. Missing apps will be installed from these trusted backups before data restore.",14,TEXT),-1,-2);Switch launch=accentSwitch("Open restored apps after the whole queue",getPreferences(0).getBoolean("launchAfter",false));add(layout,launch,-1,-2);Switch migration=accentSwitch("I understand that backups from other devices or firmware may not work",false);if(queue.needsMigrationConsent())add(layout,migration,-1,-2);ScrollView scroll=new ScrollView(this);scroll.addView(layout);AlertDialog review=dialog().setTitle("Restore "+queue.items.size()+" selected apps?").setView(scroll).setPositiveButton("Restore queue",(d,w)->work("Restoring queued backups",()->{if(queue.needsMigrationConsent()&&!migration.isChecked())throw new java.io.IOException("Confirm the migration warning first");queue.approveMigrations();return queue.run(true,launch.isChecked(),(current,total,name)->runOnUiThread(()->{OperationService.progress("Restoring "+current+" of "+total,name);if(operationPopup!=null){operationPopup.setTitle("Restoring "+current+" of "+total);if(operationMessage!=null)operationMessage.setText(name+"\nRestoring data and verifying files and permissions…");setStatus(name);}}));},()->{showMessage("Restore report",queue.lastReport);refreshDataAfterJob(queue.lastReport);})).setNegativeButton("Cancel",(d,w)->queue.close()).setOnCancelListener(d->queue.close()).show();if(queue.needsMigrationConsent()){review.getButton(-1).setEnabled(false);migration.setOnCheckedChangeListener((b,on)->review.getButton(-1).setEnabled(on));}}catch(Exception e){queue.close();showMessage("Cannot prepare queue",e.getMessage());}}
 void chooseFolder(){Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(67);if(tree!=null)try{picker.putExtra(DocumentsContract.EXTRA_INITIAL_URI,DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree)));}catch(Exception ignored){}startActivityForResult(picker,10);}
 void pickZip(int code){if(code==12&&!new LiveRestore(this).enabled()){confirmRestoreSetup();return;}startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE),code);}
 void confirmBackup(List<String> packages){if(packages.isEmpty())return;if(tree==null){showMessage("Choose a backup folder","Select a folder in Settings before backing up apps.");return;}if(!Environment.isExternalStorageManager()){showMessage("Storage access needed","Use Settings → Allow internal-storage first.");return;}
  int systemCount=0;StringBuilder names=new StringBuilder();
  for(String pkg:packages)try{
   ApplicationInfo info=getPackageManager().getApplicationInfo(pkg,0);
   boolean system=SystemAppData.system(info);if(system)systemCount++;
   names.append(getPackageManager().getApplicationLabel(info)).append(system?" (system app · data only)":"").append('\n');
  }catch(Exception e){names.append(pkg).append('\n');}
  boolean onlySystem=systemCount==packages.size();ScrollView scroll=new ScrollView(this);TextView text=new TextView(this);text.setPadding(dp(24),dp(8),dp(24),dp(8));boolean external=RestorePreferences.prefs(this).getBoolean(RestorePreferences.EXTERNAL,true);text.setText(names+"\nExternal app data: "+(external?"Included (primary Android/data files).":"Not included. Enable it in Settings → Backup preferences.")+"\n\nEach app gets its own folder and backup ZIP. These apps will stop while their private data is copied. Do not open them until completion. Backups are unencrypted.");LinearLayout options=column();options.addView(text);Switch include=accentSwitch("Include app installers (base APK + all installed splits)",(!onlySystem&&getPreferences(0).getBoolean("includeApks",true)));include.setEnabled(!onlySystem);options.addView(include);TextView systemNote=text("System-app backups always contain data only. Restore requires the matching installed system app and firmware. System apps that cannot stay stopped are refused.",14,MUTED);if(systemCount>0){systemNote.setPadding(dp(24),dp(8),dp(24),dp(8));options.addView(systemNote);}
  int selectedOrder=BackupOrder.normalize(getPreferences(0).getInt("backupOrder",BackupOrder.PACKAGE));
  scroll.addView(options);Uri destination=tree;dialog().setTitle("Back up "+packages.size()+(packages.size()==1?" app?":" apps?")).setView(scroll).setPositiveButton("Back up",(d,w)->{if(!onlySystem)getPreferences(0).edit().putBoolean("includeApks",include.isChecked()).apply();
  runBackup(packages,destination,include.isChecked(),external,selectedOrder);}).setNegativeButton("Cancel",null).show();}
 void runBackup(List<String> packages,Uri destination,boolean installers,boolean external,int order){
  work("Preparing backups",()->{BackupEngine e=new BackupEngine(this);e.backupApks=installers;e.backupExternal=external;e.backupOrder=order;
  return e.backup(packages,destination,(phase,current,total,label,estimated)->runOnUiThread(()->{
   String heading=phase+" "+current+" of "+total;
   String detail=label+"\n"+(phase.equals("Preparing")?"Estimated total so far: ":"Estimated contents: ")+size(estimated);
   OperationService.progress(heading,detail);
   if(operationPopup!=null){
    operationPopup.setTitle(heading);
    if(operationMessage!=null)operationMessage.setText(detail);
   }
   setStatus(heading+" · "+label);
  }));},null,error->{
   SystemAppData.Running running=null;for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof SystemAppData.Running){running=(SystemAppData.Running)cause;break;}
   if(running==null){showMessage("Operation stopped",error.getMessage());return;}
   String pkg=running.pkg,label=pkg;try{label=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}
   String message=label+" keeps running and cannot be safely backed up on this device. No backup was created for this app.";
   if(error instanceof BackupEngine.BatchFailure){List<String> completed=((BackupEngine.BatchFailure)error).completed;if(!completed.isEmpty())message+="\n\nCompleted apps:\n"+String.join("\n",completed);}
   showMessage("App can’t be backed up",message);
  });
 }
 void reviewLive(Uri uri){reviewLive(uri,null);}
 void reviewLive(Uri uri,String onlyPackage){if(!requireRestoreSetup())return;LiveRestore restoreEngine=new LiveRestore(this);final LiveRestore.Plan[] imported=new LiveRestore.Plan[1];final BackupCatalog.Entry[] failed=new BackupCatalog.Entry[1];work("Checking backup",()->{BackupCatalog.Entry entry=null;for(BackupCatalog.Entry e:catalog)if(e.uri.equals(uri)){entry=e;break;}try{imported[0]=restoreEngine.importArchive(uri);if(entry!=null&&!imported[0].deferredExternal)BackupIssues.clear(this,entry);}catch(java.io.InterruptedIOException interrupted){throw interrupted;}catch(Exception e){if(entry==null)throw e;BackupIssues.mark(this,entry,BackupIssues.reason(e));failed[0]=entry;return "Backup validation failed. No app data was changed.";}return "Backup reviewed. External files will be validated during restore. Choose apps to restore.";},()->{
   if(failed[0]!=null){showBackupProblems(Arrays.asList(failed[0]),null,()->{});return;}LiveRestore.Plan raw=imported[0];if(raw==null)return;
   try{org.json.JSONArray apps=raw.manifest.getJSONArray("apps");if(onlyPackage!=null){prepareSelectedRestore(restoreEngine,raw,new TreeSet<>(Arrays.asList(onlyPackage)));return;}if(apps.length()==1){prepareSelectedRestore(restoreEngine,raw,new TreeSet<>(Arrays.asList(apps.getJSONObject(0).getString("package"))));return;}
    String[] labels=new String[apps.length()],packages=new String[apps.length()];boolean[] chosen=new boolean[apps.length()];Arrays.fill(chosen,true);for(int i=0;i<apps.length();i++){packages[i]=apps.getJSONObject(i).getString("package");labels[i]=AppNames.saved(this,apps.getJSONObject(i),uri)+"\n"+packages[i];}
    dialog().setTitle("Apps in this backup").setMultiChoiceItems(labels,chosen,(d,index,on)->chosen[index]=on).setPositiveButton("Continue",(d,w)->{Set<String> targets=new TreeSet<>();for(int i=0;i<packages.length;i++)if(chosen[i])targets.add(packages[i]);if(targets.isEmpty()){new BackupEngine(this).clean(raw.job);showMessage("Nothing selected","Select at least one app to restore.");return;}prepareSelectedRestore(restoreEngine,raw,targets);}).setNegativeButton("Cancel",(d,w)->new BackupEngine(this).clean(raw.job)).setOnCancelListener(d->new BackupEngine(this).clean(raw.job)).show();
   }catch(Exception e){new BackupEngine(this).clean(raw.job);showMessage("Cannot prepare restore",e.getMessage());}
  });}
 void prepareSelectedRestore(LiveRestore restoreEngine,LiveRestore.Plan raw,Set<String> targets){
  final List<String>[] missing=new List[]{null};
  List<String> blocked=new ArrayList<>();
  work("Checking installed apps",()->{
   restoreEngine.reviewEnvironment(raw);
   if(!raw.migrationWarning.isEmpty()&&!raw.migrationKey.equals(raw.migrationApproval))return "Migration review required.";
   missing[0]=ApkBackup.missing(restoreEngine.engine,raw.manifest,targets);
   JSONArray apps=raw.manifest.getJSONArray("apps");
   for(int i=0;i<apps.length();i++){
    JSONObject saved=apps.getJSONObject(i);
    String pkg=saved.getString("package");
    if(!targets.contains(pkg)||missing[0].contains(pkg))continue;
    JSONObject installed=restoreEngine.engine.info(pkg);
    String warning=RestorePreferences.warning(this,saved,installed);
    if(warning!=null)blocked.add(warning);
    else if(RestorePreferences.differentSignature(saved,installed)||RestorePreferences.differentVersion(saved,installed))
     raw.approvedSigners.put(pkg,RestorePreparation.destinationIdentity(installed));
   }
   if(!blocked.isEmpty())return "Restore preference required.";
   if(missing[0].isEmpty())restoreEngine.prepare(raw,targets);
   return "Selected apps checked. Review the restore.";
  },()->{
   if(missing[0]==null){showMigrationReview(restoreEngine,raw,targets);return;}
   if(!blocked.isEmpty()){
    restoreEngine.engine.clean(raw.job);
    dialog().setTitle("Restore compatibility").setMessage(String.join("\n\n",blocked))
     .setPositiveButton("Restore preferences",(d,w)->showSection("Settings"))
     .setNegativeButton("Close",null).show();
   }else if(missing[0].isEmpty())showRestoreReview(restoreEngine,raw);
   else showInstallReview(restoreEngine,raw,targets,missing[0]);
  });
 }
 void showMigrationReview(LiveRestore restoreEngine,LiveRestore.Plan raw,Set<String> targets){TextView detail=text(raw.migrationWarning,14,TEXT);detail.setPadding(dp(24),dp(8),dp(24),dp(8));ScrollView scroll=new ScrollView(this);scroll.addView(detail);dialog().setTitle("Restore on a different device or firmware?").setView(scroll).setPositiveButton("Continue",(d,w)->{restoreEngine.approveMigration(raw);prepareSelectedRestore(restoreEngine,raw,targets);}).setNegativeButton("Cancel",(d,w)->restoreEngine.engine.clean(raw.job)).setOnCancelListener(d->restoreEngine.engine.clean(raw.job)).show();}
 void showInstallReview(LiveRestore restoreEngine,LiveRestore.Plan raw,Set<String> targets,List<String> missing){List<String> names=new ArrayList<>();try{JSONArray apps=raw.manifest.getJSONArray("apps");for(int i=0;i<apps.length();i++)if(missing.contains(apps.getJSONObject(i).getString("package")))names.add(AppNames.saved(this,apps.getJSONObject(i),null));}catch(Exception ignored){names.addAll(missing);}dialog().setTitle("Install missing apps from backup?").setMessage(android.text.TextUtils.join("\n",names)+"\n\nThis installs the saved APKs and splits. Android checks their signatures and compatibility. Use only backups you trust. You will review data restore afterward. Cancelling data restore leaves the newly installed app in place.").setPositiveButton("Install apps",(d,w)->work(()->{ApkBackup.installMissing(restoreEngine.engine,raw.job,raw.manifest,new TreeSet<>(missing),raw.migrationApproval);restoreEngine.prepare(raw,targets);return "Apps installed. Review data restore.";},()->showRestoreReview(restoreEngine,raw))).setNegativeButton("Cancel",(d,w)->restoreEngine.engine.clean(raw.job)).setOnCancelListener(d->restoreEngine.engine.clean(raw.job)).show();}
 void showRestoreReview(LiveRestore restoreEngine,LiveRestore.Plan plan){LinearLayout layout=column();layout.setPadding(dp(24),dp(8),dp(24),dp(8));TextView info=new TextView(this);info.setText((plan.migrationWarning.isEmpty()?"":plan.migrationWarning+"\n\n")+plan.description+(plan.manifest.has("external")?"\nExternal files are validated while writing temporary restore files, then replace the app’s primary external files. The recovery ZIP covers private data only.\n":"")+"\nOnly these apps will be restored. Recovery backups are kept until cleared in Settings.");add(layout,info,-1,-2);Switch launch=accentSwitch("Open restored apps afterward",getPreferences(0).getBoolean("launchAfter",false));add(layout,launch,-1,-2);ScrollView scroll=new ScrollView(this);scroll.addView(layout);dialog().setTitle("recovery".equals(plan.manifest.optString("backupType"))?"Recovery restore?":"Restore these apps?").setView(scroll).setPositiveButton("Restore",(d,w)->work("Restoring apps",()->{try{String result=restoreEngine.restore(plan,true,launch.isChecked());if(result.startsWith("Restored "))for(BackupCatalog.Entry entry:catalog)if(entry.uri.equals(plan.source))BackupIssues.clear(this,entry);return result;}catch(Exception failure){if(BackupIssues.invalidPayload(failure))for(BackupCatalog.Entry entry:catalog)if(entry.uri.equals(plan.source))BackupIssues.mark(this,entry,BackupIssues.reason(failure));throw failure;}})).setNegativeButton("Cancel",(d,w)->new BackupEngine(this).clean(plan.job)).setOnCancelListener(d->new BackupEngine(this).clean(plan.job)).show();}
 void confirmRehearsal(Uri uri){dialog().setTitle("Restore rehearsal").setMessage("Validate and extract in a separate test directory. Installed app data is untouched.").setPositiveButton("Rehearse",(d,w)->work(()->new BackupEngine(this).rehearse(uri))).setNegativeButton("Cancel",null).show();}
 void showMessage(String title,String message){dialog().setTitle(title).setMessage(message).setPositiveButton("Close",null).show();}
 void showHistory(AppRow app){List<BackupCatalog.Entry> versions=new ArrayList<>();List<String> labels=new ArrayList<>();for(BackupCatalog.Entry entry:catalog)if(entry.manifest!=null)try{org.json.JSONArray apps=entry.manifest.getJSONArray("apps");for(int i=0;i<apps.length();i++){org.json.JSONObject saved=apps.getJSONObject(i);if(saved.getString("package").equals(app.info.packageName)){versions.add(entry);labels.add((entry.recovery?"Recovery • ":"")+date(entry)+" • version "+saved.getLong("version")+" • "+size(entry.size));break;}}}catch(Exception ignored){}if(versions.isEmpty()){showMessage("No backup history","There are no backups for this app in the chosen folder.");return;}dialog().setTitle(app.label+" — "+versions.size()+" backups").setItems(labels.toArray(new String[0]),(d,index)->reviewLive(versions.get(index).uri,app.info.packageName)).setNegativeButton("Close",null).show();}
 void showSettingsTransfer(LinearLayout settings){
  LinearLayout transfer=card();add(transfer,text("Settings & configuration",18,TEXT),-1,-2);gap(transfer,12);LinearLayout actions=row();actions.setGravity(Gravity.TOP);actions.setBaselineAligned(false);
  settingsButtonColumn(actions,button("Export settings",true,()->{try{Intent pick=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"RavenBackup-settings-"+new java.text.SimpleDateFormat("yyyy-MM-dd-HHmmss",java.util.Locale.ROOT).format(new Date())+".json");startActivityForResult(pick,40);}catch(Exception e){showMessage("Cannot export settings",e.getMessage());}}),"Save theme, backup/restore preferences and schedule choices to a JSON file.");
  settingsButtonColumn(actions,button("Import settings",true,()->{try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json"),41);}catch(Exception e){showMessage("Cannot import settings",e.getMessage());}}),"Review before applying. Imported schedules stay off until enabled.");add(transfer,actions,-1,-2);add(settings,transfer,-1,-2);gap(settings,12);
 }
 void exportSettings(Uri uri){if(busy||OperationService.running()||ScheduledBackupService.running){showMessage("Operation in progress","Wait for backups or restores to finish before exporting settings.");return;}work("Exporting settings",()->{byte[] data=SettingsTransfer.export(this).toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8);try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException("Cannot open the export file");out.write(data);out.flush();}return "Settings exported. Theme, preferences and schedule choices saved. Keep this file outside storage you plan to wipe. It contains scheduled app package names, not app data or Android permissions.";});}
 void importSettings(Uri uri){if(busy||OperationService.running()||ScheduledBackupService.running){showMessage("Operation in progress","Wait for backups or restores to finish before importing settings.");return;}final org.json.JSONObject[] data={null};work("Reading settings",()->{try(java.io.InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new java.io.IOException("Cannot open settings file");data[0]=SettingsTransfer.read(in);}return "Settings checked. Review before importing.";},()->reviewSettingsImport(data[0]));}
 void reviewSettingsImport(org.json.JSONObject data){try{ScrollView scroll=new ScrollView(this);TextView detail=text(SettingsTransfer.review(data),14,TEXT);detail.setPadding(dp(24),dp(8),dp(24),dp(8));scroll.addView(detail);dialog().setTitle("Import settings?").setView(scroll).setPositiveButton("Import",(d,w)->{try{SettingsTransfer.apply(this,data);getIntent().putExtra("settingsImportDone",true);recreate();}catch(Exception e){showMessage("Import failed",e.getMessage());}}).setNegativeButton("Cancel",null).show();}catch(Exception e){showMessage("Invalid settings file",e.getMessage());}}
 void chooseScheduleApps(){if(allApps.isEmpty()){showMessage("Apps are loading","Refresh the app list, then try again.");return;}List<AppRow> apps=new ArrayList<>(allApps);apps.sort((a,b)->a.label.compareToIgnoreCase(b.label));Set<String> existing=new HashSet<>(selected);try{org.json.JSONObject config=BackupSchedule.configuration(this);if(existing.isEmpty()&&config!=null){org.json.JSONArray saved=config.getJSONArray("packages");for(int i=0;i<saved.length();i++)existing.add(saved.getString(i));}}catch(Exception ignored){}String[] labels=new String[apps.size()];boolean[] checked=new boolean[apps.size()];for(int i=0;i<apps.size();i++){labels[i]=apps.get(i).label+"\n"+apps.get(i).info.packageName;checked[i]=existing.contains(apps.get(i).info.packageName);}dialog().setTitle("Apps to back up automatically").setMultiChoiceItems(labels,checked,(d,index,on)->checked[index]=on).setPositiveButton("Continue",(d,w)->{List<String> targets=new ArrayList<>();for(int i=0;i<apps.size();i++)if(checked[i])targets.add(apps.get(i).info.packageName);configureSchedule(targets);}).setNegativeButton("Cancel",null).show();}
 void configureSchedule(List<String> targets){if(targets.isEmpty()||tree==null){showMessage("Schedule needs apps and storage","Choose at least one app and a backup folder first.");return;}LinearLayout layout=column();layout.setPadding(dp(24),dp(8),dp(24),dp(8));TextView description=new TextView(this);description.setText(targets.size()+" apps\n"+BackupEngine.folderName(tree)+"\n\nEvery run creates a new backup version. Apps stop during backup. Android may delay the job.");add(layout,description,-1,-2);Spinner frequency=new Spinner(this);frequency.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Daily — approximate","Weekly — approximate"}));add(layout,frequency,-1,48);CheckBox charging=new CheckBox(this);charging.setText("Only while charging");charging.setChecked(true);add(layout,charging,-1,-2);CheckBox idle=new CheckBox(this);idle.setText("Only when device is idle");idle.setChecked(true);add(layout,idle,-1,-2);try{org.json.JSONObject saved=BackupSchedule.configuration(this);if(saved!=null){frequency.setSelection(saved.getInt("days")==7?1:0);charging.setChecked(saved.getBoolean("charging"));idle.setChecked(saved.getBoolean("idle"));}}catch(Exception ignored){}Uri destination=tree;dialog().setTitle("Backup schedule").setView(layout).setPositiveButton("Enable schedule",(d,w)->work(()->{BackupSchedule.enable(this,targets,destination,frequency.getSelectedItemPosition()==0?1:7,charging.isChecked(),idle.isChecked());return "Backup schedule enabled: "+BackupSchedule.summary(this);})).setNegativeButton("Cancel",null).show();}
 void requestBackgroundAllowance(){PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);if(power.isIgnoringBatteryOptimizations(getPackageName())){showMessage("Background allowance","Battery optimization exemption is already allowed.");return;}try{startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}}
 @Override protected void onResume(){super.onResume();if(content!=null&&section.equals("Settings")&&!busy)showSection("Settings");}
 void setStatus(String message){lastStatus=message;if(status!=null)status.setText(message);}
 void showProgress(String heading){
  LinearLayout body=row();body.setPadding(dp(24),dp(16),dp(24),dp(24));TextView message=text(heading.startsWith("Backing")?"Preparing selected apps…":heading.startsWith("Restoring")?"Restoring data and verifying files and permissions…":"Checking files and preparing the next step…",14,TEXT);operationMessage=message;LinearLayout.LayoutParams spacing=new LinearLayout.LayoutParams(0,-2,1);body.addView(message,spacing);
  operationPopup=dialog().setTitle(heading).setView(body).setCancelable(false).create();operationPopup.setCanceledOnTouchOutside(false);operationPopup.show();
 }
 void work(Job job){work("Working",job,null);}
 void work(String title,Job job){work(title,job,null);}
 void work(Job job,Runnable completed){work("Preparing restore",job,completed);}
 void work(String heading,Job job,Runnable completed){work(heading,job,completed,null);}
 void work(String heading,Job job,Runnable completed,java.util.function.Consumer<Exception> failed){
  if(busy)return;
  if(OperationService.running()){showMessage("Operation in progress","A backup or restore is already running. Check the progress notification.");return;}
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
   requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},30);
  busy=true;setStatus(heading+"… the screen can be turned off.");showProgress(heading);
  if(primary!=null)primary.setEnabled(false);
  try{
   OperationService.begin(this,heading,()->{
    String result;boolean success=true;Exception failure=null;
    try{result=job.run();}
    catch(Exception e){failure=e;result="Stopped: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());success=false;}
    final String output=result;final boolean passed=success;final Exception problem=failure;
    OperationService.result(output,passed);
    android.util.Log.i("RavenBackup",output);
    getPreferences(0).edit().putString("lastOperation",output).apply();
    runOnUiThread(()->{
     busy=false;
     if(operationPopup!=null){operationPopup.dismiss();operationPopup=null;}
     if(isFinishing()||isDestroyed())return;
     setStatus(output);if(!section.equals("Settings"))renderList();else showSection("Settings");
     if(!passed){if(failed!=null)failed.accept(problem);else showMessage("Operation stopped",output);}
     else if(completed!=null)completed.run();
     else{showMessage("Completed",output);refreshDataAfterJob(output);}
    });
   },reason->{
    busy=false;
    if(operationPopup!=null){operationPopup.dismiss();operationPopup=null;}
    setStatus("Could not start background operation");
    if(primary!=null)primary.setEnabled(true);
    if(!isFinishing()&&!isDestroyed())showMessage("Cannot start operation",reason);
   });
  }catch(RuntimeException error){
   busy=false;if(operationPopup!=null){operationPopup.dismiss();operationPopup=null;}
   if(primary!=null)primary.setEnabled(true);
   showMessage("Cannot start operation",error.getMessage());
  }
 }
 @Override protected void onDestroy(){if(operationPopup!=null){operationPopup.dismiss();operationPopup=null;}super.onDestroy();}
 void refreshDataAfterJob(String message){refreshData(message);}
 @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();if(code==40){exportSettings(uri);return;}if(code==41){importSettings(uri);return;}if(code==10){try{getContentResolver().takePersistableUriPermission(uri,data.getFlags()&3);tree=uri;getPreferences(0).edit().putString("tree",uri.toString()).apply();selectedBackups.clear();showSection(section);refreshData();}catch(Exception e){showMessage("Folder access failed","Please choose the folder again. "+e.getMessage());}}if(code==11)confirmRehearsal(uri);if(code==12)reviewLive(uri);}
}
