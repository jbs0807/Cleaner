package com.jbs0807.cleaner;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, items;
    TextView summary;
    final Set<String> protectedPackages = new HashSet<>();
    final int bg = Color.rgb(246,248,252);
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        protectedPackages.add(getPackageName());
        protectedPackages.add("com.android.systemui");
        protectedPackages.add("com.android.phone");
        protectedPackages.add("com.google.android.gms");
        protectedPackages.add("com.android.settings");
        protectedPackages.add("com.samsung.android.dialer");
        protectedPackages.add("com.samsung.android.messaging");
        protectedPackages.addAll(getPreferences(0).getStringSet("protected", Collections.emptySet()));
        root=new LinearLayout(this); root.setOrientation(1); root.setPadding(22,24,22,12); root.setBackgroundColor(bg);
        setContentView(root);
        TextView title=new TextView(this); title.setText("Cleaner"); title.setTextSize(28); title.setTypeface(null,Typeface.BOLD); title.setTextColor(Color.rgb(25,50,95)); root.addView(title);
        TextView sub=new TextView(this); sub.setText("메모리 · 배터리 사용 앱 관리"); sub.setTextSize(15); root.addView(sub);
        summary=new TextView(this); summary.setPadding(0,20,0,12); root.addView(summary);
        Button scan=new Button(this); scan.setText("사용 중인 앱 새로고침"); root.addView(scan); scan.setOnClickListener(v->load());
        Button permission=new Button(this); permission.setText("사용 정보 접근 권한 설정"); root.addView(permission);
        permission.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        ScrollView scroll=new ScrollView(this); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        items=new LinearLayout(this); items.setOrientation(1); scroll.addView(items);
        load();
    }
    @Override protected void onResume(){super.onResume(); if(items!=null) load();}
    boolean hasAccess() {
        AppOpsManager ops=(AppOpsManager)getSystemService(APP_OPS_SERVICE);
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),getPackageName())==AppOpsManager.MODE_ALLOWED;
    }
    void load(){
        items.removeAllViews();
        ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo();
        ((ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(mi);
        long mb=1024L*1024L;
        summary.setText("사용 메모리: "+(mi.totalMem-mi.availMem)/mb+" MB / "+mi.totalMem/mb+" MB\n※ 앱별 실제 RAM 및 배터리 소모량은 표시되지 않습니다.");
        if(!hasAccess()){line("사용 정보 접근 권한을 허용하면 최근 사용한 앱을 확인할 수 있습니다.");return;}
        UsageStatsManager us=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
        long now=System.currentTimeMillis(), start=now-24L*60*60*1000;
        Map<String,UsageStats> stats=us.queryAndAggregateUsageStats(start,now);
        List<UsageStats> recent=new ArrayList<>(stats.values());
        recent.removeIf(s->s.getLastTimeUsed()<=0 || s.getPackageName().equals(getPackageName()));
        recent.sort((a,b)->Long.compare(b.getLastTimeUsed(),a.getLastTimeUsed()));
        int count=0;
        PackageManager pm=getPackageManager();
        for(UsageStats s:recent){
            if(count>=40) break;
            String pkg=s.getPackageName();
            ApplicationInfo info;
            try{info=pm.getApplicationInfo(pkg,0);}catch(Exception e){continue;}
            // UsageStats is recent usage, not a reliable running/stopped state.
            if((info.flags & ApplicationInfo.FLAG_STOPPED)!=0) continue;
            String name=pm.getApplicationLabel(info).toString();
            boolean fixed=(info.flags & ApplicationInfo.FLAG_SYSTEM)!=0;
            boolean safe=protectedPackages.contains(pkg)||fixed;
            LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(14,12,14,12);
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=10;items.addView(card,cp);
            TextView label=new TextView(this);label.setText(name+(safe?"  [보호]":"")+"\n최근 사용: "+new java.text.SimpleDateFormat("MM-dd HH:mm",Locale.KOREA).format(new Date(s.getLastTimeUsed())));
            label.setTextColor(Color.BLACK);label.setTextSize(15);card.addView(label);
            LinearLayout controls=new LinearLayout(this);card.addView(controls);
            if(!fixed && !pkg.equals(getPackageName())){
                CheckBox check=new CheckBox(this);check.setText("보호");check.setChecked(protectedPackages.contains(pkg));controls.addView(check);
                check.setOnCheckedChangeListener((button,checked)->{
                    if(checked)protectedPackages.add(pkg);else protectedPackages.remove(pkg);
                    getPreferences(0).edit().putStringSet("protected",new HashSet<>(protectedPackages)).apply();
                    label.setText(name+(checked?"  [보호]":"")+"\n최근 사용: "+new java.text.SimpleDateFormat("MM-dd HH:mm",Locale.KOREA).format(new Date(s.getLastTimeUsed())));
                });
            }
            Button detail=new Button(this);detail.setText("설정 열기");controls.addView(detail);
            detail.setOnClickListener(v->{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+pkg));startActivity(i);});
            count++;
        }
        if(count==0)line("표시할 최근 사용 앱이 없습니다.");
        line("최근 사용 기록은 실행 중인 앱 목록과 다릅니다. 앱 중지·배터리 제한은 각 앱의 시스템 설정에서 직접 선택하세요.");
    }
    void line(String msg){TextView t=new TextView(this);t.setText(msg);t.setPadding(8,12,8,12);items.addView(t);}
}
