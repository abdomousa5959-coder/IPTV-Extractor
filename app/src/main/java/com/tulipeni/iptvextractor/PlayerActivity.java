package com.tulipeni.iptvextractor;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.*;
import android.media.MediaPlayer;
import java.util.HashMap;
import java.util.Map;

public class PlayerActivity extends Activity implements SurfaceHolder.Callback {
    private MediaPlayer media;
    private SurfaceView surface;
    private ProgressBar loading;
    private TextView state;
    private Button playPause;
    private String streamUrl="", userAgent="";
    private boolean prepared=false, surfaceReady=false, shouldPlay=true;
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
        streamUrl=getIntent().getStringExtra("url");userAgent=getIntent().getStringExtra("user_agent");
        if(streamUrl==null||streamUrl.trim().isEmpty()){finish();return;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.BLACK);
        TextView title=new TextView(this);title.setText(getIntent().getStringExtra("title"));title.setTextColor(Color.WHITE);title.setTextSize(17);title.setPadding(dp(16),dp(14),dp(16),dp(14));root.addView(title);
        FrameLayout frame=new FrameLayout(this);frame.setBackgroundColor(Color.BLACK);
        surface=new SurfaceView(this);surface.getHolder().addCallback(this);
        frame.addView(surface,new FrameLayout.LayoutParams(-1,-1));
        loading=new ProgressBar(this);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER);frame.addView(loading,lp);
        root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        state=new TextView(this);state.setText("جاري تجهيز البث...");state.setTextColor(0xffb9c7bf);state.setPadding(dp(14),dp(8),dp(14),dp(8));root.addView(state);
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setPadding(dp(12),dp(6),dp(12),dp(16));
        playPause=new Button(this);playPause.setText("إيقاف مؤقت");playPause.setEnabled(false);
        Button retry=new Button(this);retry.setText("إعادة المحاولة");
        controls.addView(playPause,new LinearLayout.LayoutParams(0,dp(48),1));
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,dp(48),1);rp.leftMargin=dp(8);controls.addView(retry,rp);
        root.addView(controls);setContentView(root);
        playPause.setOnClickListener(v->{if(media==null||!prepared)return;if(media.isPlaying()){media.pause();shouldPlay=false;playPause.setText("تشغيل");}else{media.start();shouldPlay=true;playPause.setText("إيقاف مؤقت");}});
        retry.setOnClickListener(v->startPlayback());
    }
    @Override public void surfaceCreated(SurfaceHolder holder){surfaceReady=true;startPlayback();}
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height){}
    @Override public void surfaceDestroyed(SurfaceHolder holder){surfaceReady=false;release();}
    private void startPlayback(){
        if(!surfaceReady)return;
        release();loading.setVisibility(View.VISIBLE);state.setText("جاري الاتصال بسيرفر البث...");
        try{
            media=new MediaPlayer();media.setDisplay(surface.getHolder());
            Map<String,String> headers=new HashMap<>();
            if(userAgent!=null&&!userAgent.trim().isEmpty())headers.put("User-Agent",userAgent.trim());
            headers.put("Accept","*/*");
            media.setDataSource(this,Uri.parse(streamUrl),headers);
            media.setOnPreparedListener(mp->{prepared=true;loading.setVisibility(View.GONE);state.setText("متصل — البث يعمل داخل التطبيق");playPause.setEnabled(true);if(shouldPlay){mp.start();playPause.setText("إيقاف مؤقت");}});
            media.setOnBufferingUpdateListener((mp,percent)->state.setText("جاري التشغيل — تحميل "+percent+"%"));
            media.setOnCompletionListener(mp->{state.setText("انتهى البث");playPause.setText("تشغيل");});
            media.setOnErrorListener((mp,what,extra)->{prepared=false;loading.setVisibility(View.GONE);state.setText("تعذر تشغيل الرابط (رمز "+what+"/"+extra+"). قد لا يدعم الجهاز الترميز أو أن الرابط منتهي.");playPause.setEnabled(false);return true;});
            media.prepareAsync();
        }catch(Exception e){loading.setVisibility(View.GONE);state.setText("خطأ: "+e.getMessage());}
    }
    private void release(){prepared=false;if(media!=null){try{media.reset();media.release();}catch(Exception ignored){}media=null;}if(playPause!=null)playPause.setEnabled(false);}
    @Override protected void onPause(){super.onPause();if(media!=null&&media.isPlaying()){media.pause();shouldPlay=true;}}
    @Override protected void onDestroy(){release();super.onDestroy();}
}