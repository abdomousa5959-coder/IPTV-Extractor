package com.tulipeni.iptvextractor

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.*
import android.text.TextWatcher
import android.text.Editable
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

data class Channel(val name:String,val url:String,val group:String)

class MainActivity:Activity(){
 private val channels= mutableListOf<Channel>()
 private lateinit var list:LinearLayout
 private lateinit var status:TextView
 private lateinit var source:EditText
 private lateinit var filter:EditText
 private var playlist=""
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  window.statusBarColor=Color.rgb(4,12,8); window.navigationBarColor=Color.BLACK
  val root=LinearLayout(this).apply{orientation=1;setPadding(16,20,16,10);setBackgroundColor(Color.rgb(4,12,8))}
  root.addView(TextView(this).apply{text="IPTV EXTRACTOR";textSize=25f;setTextColor(Color.rgb(0,230,118))})
  root.addView(TextView(this).apply{text="استخراج القنوات من قوائم M3U";setTextColor(Color.LTGRAY)})
  source=EditText(this).apply{setSingleLine();hint="رابط قائمة M3U";setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);setText("http://go8knm.optikl.ink/Go/playlist.m3u")}
  root.addView(source,LinearLayout.LayoutParams(-1,54))
  val load=Button(this).apply{text="تحميل واستخراج القنوات"};root.addView(load)
  status=TextView(this).apply{text="جاهز";setTextColor(Color.GREEN)};root.addView(status)
  filter=EditText(this).apply{hint="بحث باسم القناة أو المجموعة";setTextColor(Color.WHITE);setHintTextColor(Color.GRAY)};root.addView(filter,LinearLayout.LayoutParams(-1,50))
  val bar=LinearLayout(this).apply{orientation=0}
  val copy=Button(this).apply{text="نسخ كل الروابط"};val export=Button(this).apply{text="مشاركة M3U"}
  bar.addView(copy,LinearLayout.LayoutParams(0,-2,1f));bar.addView(export,LinearLayout.LayoutParams(0,-2,1f));root.addView(bar)
  val scroll=ScrollView(this);list=LinearLayout(this).apply{orientation=1};scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
  load.setOnClickListener{fetch()}
  filter.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c:Int){render(s.toString())};override fun afterTextChanged(e:Editable?){}})
  copy.setOnClickListener{copyText(channels.joinToString("\n"){it.url},"تم نسخ ${channels.size} رابط")}
  export.setOnClickListener{if(playlist.isNotBlank()){val i=android.content.Intent(android.content.Intent.ACTION_SEND).apply{type="text/plain";putExtra(android.content.Intent.EXTRA_TEXT,playlist)};startActivity(android.content.Intent.createChooser(i,"مشاركة القائمة"))}else toast("حمّل قائمة أولاً")}
 }
 private fun fetch(){val address=source.text.toString().trim();if(!address.startsWith("http")){toast("الرابط غير صحيح");return};status.text="جارٍ التحميل...";thread{try{val c=URL(address).openConnection() as HttpURLConnection;c.connectTimeout=20000;c.readTimeout=30000;c.setRequestProperty("User-Agent","IPTV-Extractor/1.0");val body=c.inputStream.bufferedReader().use{it.readText()};c.disconnect();val parsed=parse(body);runOnUiThread{playlist=body;channels.clear();channels.addAll(parsed);status.text="تم استخراج ${channels.size} قناة";render("")}}catch(e:Exception){runOnUiThread{status.text="فشل التحميل: ${e.message}"}}}}
 private fun parse(body:String):List<Channel>{val out= mutableListOf<Channel>();var name="قناة بدون اسم";var group="";body.lineSequence().forEach{line->val s=line.trim();if(s.startsWith("#EXTINF",true)){name=s.substringAfter(',',"قناة بدون اسم").trim();group=Regex("""group-title=[\"']([^\"']*)[\"']""").find(s)?.groupValues?.get(1).orEmpty()}else if((s.startsWith("http://")||s.startsWith("https://"))&&!s.startsWith("#")){out.add(Channel(name,s,group));name="قناة بدون اسم";group=""}};return out}
 private fun render(q:String){list.removeAllViews();channels.filter{it.name.contains(q,true)||it.group.contains(q,true)}.take(1500).forEachIndexed{n,ch->val row=LinearLayout(this).apply{orientation=1;setPadding(10,10,10,10);setBackgroundColor(if(n%2==0)Color.rgb(12,25,18) else Color.rgb(7,17,12))};row.addView(TextView(this).apply{text=ch.name;setTextColor(Color.rgb(0,230,118));textSize=15f});row.addView(TextView(this).apply{text=(if(ch.group.isBlank())"" else ch.group+" • ")+ch.url;setTextColor(Color.LTGRAY);textSize=10f});row.setOnClickListener{copyText(ch.url,"تم نسخ رابط القناة")};list.addView(row,ViewGroup.LayoutParams(-1,-2)}}
 private fun copyText(t:String,msg:String){if(t.isBlank()){toast("لا توجد روابط");return};(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("IPTV",t));toast(msg)}
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
