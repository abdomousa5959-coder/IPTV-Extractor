package com.tulipeni.iptvextractor;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final String DEFAULT_URL="http://go8knm.optikl.ink/Go/playlist.m3u";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private EditText url,search;
    private TextView status,count;
    private LinearLayout list;
    private ProgressBar progress;
    private String playlist="";
    private final ArrayList<Channel> channels=new ArrayList<>();
    static class Channel {
        String name,group,logo,url;
        Channel(String n,String g,String l,String u){name=n;group=g;logo=l;url=u;}
    }
    public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(7,15,12));
        getWindow().setNavigationBarColor(Color.rgb(7,15,12));
        ui();
    }
    int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,int size,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;
    }
    LinearLayout.LayoutParams full(){return new LinearLayout.LayoutParams(-1,-2);}
    Button button(String s){
        Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.BLACK);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(80,255,150)));return b;
    }
    void ui(){
        ScrollView sc=new ScrollView(this);
        LinearLayout page=new LinearLayout(this);page.setOrientation(1);page.setPadding(dp(16),dp(18),dp(16),dp(20));page.setBackgroundColor(Color.rgb(7,15,12));sc.addView(page);setContentView(sc);
        TextView title=text("IPTV EXTRACTOR",25,Color.rgb(80,255,150));title.setGravity(Gravity.CENTER);title.setTypeface(null,1);page.addView(title,full());
        TextView sub=text("استخراج وتحليل قوائم M3U",14,Color.LTGRAY);sub.setGravity(Gravity.CENTER);sub.setPadding(0,dp(5),0,dp(18));page.addView(sub,full());
        page.addView(text("رابط الـ Playlist",14,Color.WHITE),full());
        url=new EditText(this);url.setSingleLine(true);url.setText(DEFAULT_URL);url.setTextColor(-1);url.setHintTextColor(Color.GRAY);url.setTextSize(13);url.setPadding(dp(12),0,dp(12),0);url.setBackgroundColor(Color.rgb(22,35,29));
        LinearLayout.LayoutParams p=full();p.height=dp(52);p.topMargin=dp(7);page.addView(url,p);
        Button go=button("استخراج القنوات");page.addView(go,full());
        progress=new ProgressBar(this);progress.setVisibility(View.GONE);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-2,-2);pp.gravity=Gravity.CENTER;page.addView(progress,pp);
        status=text("جاهز — اضغط استخراج لقراءة القائمة.",13,Color.LTGRAY);status.setPadding(0,dp(10),0,dp(10));page.addView(status,full());
        count=text("القنوات: 0",15,Color.rgb(80,255,150));page.addView(count,full());
        search=new EditText(this);search.setSingleLine(true);search.setHint("ابحث باسم القناة أو المجموعة...");search.setTextColor(-1);search.setHintTextColor(Color.GRAY);search.setBackgroundColor(Color.rgb(22,35,29));search.setPadding(dp(12),0,dp(12),0);
        LinearLayout.LayoutParams sp=full();sp.height=dp(48);sp.topMargin=dp(8);page.addView(search,sp);
        LinearLayout row=new LinearLayout(this);Button share=button("مشاركة M3U"),save=button("حفظ الملف");
        row.addView(share,new LinearLayout.LayoutParams(0,dp(48),1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(48),1);rp.leftMargin=dp(8);row.addView(save,rp);page.addView(row,full());
        list=new LinearLayout(this);list.setOrientation(1);LinearLayout.LayoutParams lp=full();lp.topMargin=dp(12);page.addView(list,lp);
        go.setOnClickListener(v->fetch());share.setOnClickListener(v->share());save.setOnClickListener(v->save());
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){render(s.toString());}public void afterTextChanged(Editable e){}});
    }
    void fetch(){
        String target=url.getText().toString().trim();
        if(!target.startsWith("http://")&&!target.startsWith("https://")){status.setText("اكتب رابط HTTP أو HTTPS صحيح.");return;}
        progress.setVisibility(View.VISIBLE);status.setText("جاري تحميل وتحليل القائمة...");
        worker.execute(()->{
            try{
                HttpURLConnection c=(HttpURLConnection)new URL(target).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setInstanceFollowRedirects(true);c.setRequestProperty("User-Agent","Mozilla/5.0 IPTVExtractor/1.0");c.setRequestProperty("Accept","*/*");
                int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String body=read(in);c.disconnect();
                if(code<200||code>=300)throw new IOException("HTTP "+code+" — "+body.substring(0,Math.min(160,body.length())));
                ArrayList<Channel> parsed=parse(body);
                runOnUiThread(()->{playlist=body;channels.clear();channels.addAll(parsed);progress.setVisibility(View.GONE);status.setText(parsed.isEmpty()?"تم التحميل لكن لم يتم العثور على قنوات M3U.":"تم استخراج القائمة بنجاح.");render(search.getText().toString());});
            }catch(Exception e){runOnUiThread(()->{progress.setVisibility(View.GONE);status.setText("فشل الاستخراج: "+e.getMessage());});}
        });
    }
    String read(InputStream in)throws IOException{
        if(in==null)return "";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);
    }
    ArrayList<Channel> parse(String body){
        ArrayList<Channel> out=new ArrayList<>();String name="قناة غير مسماة",group="غير مصنف",logo="";
        for(String raw:body.replace("\uFEFF","").split("\\r?\\n")){
            String line=raw.trim();if(line.isEmpty())continue;
            if(line.startsWith("#EXTINF")){int comma=line.lastIndexOf(',');name=comma>=0&&comma<line.length()-1?line.substring(comma+1).trim():"قناة غير مسماة";group=attr(line,"group-title");logo=attr(line,"tvg-logo");}
            else if(line.startsWith("#EXTGRP:"))group=line.substring(8).trim();
            else if(!line.startsWith("#")){out.add(new Channel(name,group.isEmpty()?"غير مصنف":group,logo,line));name="قناة غير مسماة";group="غير مصنف";logo="";}
        }return out;
    }
    String attr(String line,String key){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:^|\\s)"+key+"=\\\"([^\\\"]*)\\\"").matcher(line);return m.find()?m.group(1):"";
    }
    void render(String query){
        list.removeAllViews();String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);int shown=0;
        for(Channel ch:channels){
            if(!ch.name.toLowerCase(Locale.ROOT).contains(q)&&!ch.group.toLowerCase(Locale.ROOT).contains(q))continue;
            LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackgroundColor(Color.rgb(18,31,25));LinearLayout.LayoutParams cp=full();cp.bottomMargin=dp(7);list.addView(card,cp);
            TextView nm=text(ch.name,15,Color.rgb(100,255,160));nm.setTypeface(null,1);card.addView(nm);
            card.addView(text("المجموعة: "+ch.group,12,Color.LTGRAY));
            TextView link=text(ch.url,10,Color.GRAY);link.setMaxLines(2);link.setTextIsSelectable(true);card.addView(link,full());
            card.setOnClickListener(v->{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("Stream URL",ch.url));Toast.makeText(this,"تم نسخ رابط البث",Toast.LENGTH_SHORT).show();});shown++;
        }
        if(shown==0){TextView empty=text(channels.isEmpty()?"لا توجد بيانات بعد.":"لا توجد نتائج مطابقة.",13,Color.LTGRAY);empty.setGravity(Gravity.CENTER);empty.setPadding(0,dp(20),0,dp(20));list.addView(empty);}
        count.setText("القنوات: "+channels.size()+" | المعروض: "+shown);
    }
    void share(){
        if(playlist.isEmpty()){Toast.makeText(this,"استخرج القائمة أولاً",Toast.LENGTH_SHORT).show();return;}
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,playlist);startActivity(Intent.createChooser(i,"مشاركة Playlist"));
    }
    void save(){
        if(playlist.isEmpty()){Toast.makeText(this,"استخرج القائمة أولاً",Toast.LENGTH_SHORT).show();return;}
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("audio/x-mpegurl");i.putExtra(Intent.EXTRA_TITLE,"playlist.m3u");startActivityForResult(i,77);
    }
    protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);if(req==77&&res==RESULT_OK&&data!=null&&data.getData()!=null){
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(playlist.getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"تم حفظ ملف M3U",Toast.LENGTH_LONG).show();}
            catch(Exception e){Toast.makeText(this,"تعذر الحفظ: "+e.getMessage(),Toast.LENGTH_LONG).show();}
        }
    }
    protected void onDestroy(){super.onDestroy();worker.shutdownNow();}
}