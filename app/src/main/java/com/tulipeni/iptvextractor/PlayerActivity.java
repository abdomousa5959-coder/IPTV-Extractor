package com.tulipeni.iptvextractor;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends Activity {
    private ExoPlayer player;
    private PlayerView playerView;
    private TextView error;
    private String streamUrl="", userAgent="";
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);
        streamUrl=getIntent().getStringExtra("url");userAgent=getIntent().getStringExtra("user_agent");
        if(streamUrl==null||streamUrl.trim().isEmpty()){finish();return;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.BLACK);
        TextView title=new TextView(this);title.setText(getIntent().getStringExtra("title"));title.setTextColor(Color.WHITE);title.setTextSize(16);title.setPadding(dp(16),dp(12),dp(16),dp(12));root.addView(title);
        playerView=new PlayerView(this);playerView.setUseController(true);playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
        root.addView(playerView,new LinearLayout.LayoutParams(-1,0,1));
        error=new TextView(this);error.setTextColor(0xffff7777);error.setTextSize(13);error.setPadding(dp(14),dp(8),dp(14),dp(8));error.setVisibility(View.GONE);root.addView(error);
        setContentView(root);initializePlayer();
    }
    private void initializePlayer(){
        try{
            DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setConnectTimeoutMs(15000).setReadTimeoutMs(30000).setAllowCrossProtocolRedirects(true);
            if(userAgent!=null&&!userAgent.isEmpty())http.setDefaultRequestProperty("User-Agent",userAgent);
            http.setDefaultRequestProperty("Accept","*/*");
            player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(http)).build();
            playerView.setPlayer(player);
            player.addListener(new Player.Listener(){
                @Override public void onPlayerError(PlaybackException e){
                    error.setVisibility(View.VISIBLE);error.setText("تعذر تشغيل البث: "+e.getErrorCodeName()+"\\nتحقق من الرابط أو صلاحية الوصول أو User-Agent.");
                }
            });
            MediaItem.Builder item=new MediaItem.Builder().setUri(Uri.parse(streamUrl));
            String path=Uri.parse(streamUrl).getPath();
            if(path!=null&&path.toLowerCase().contains(".m3u8"))item.setMimeType(MimeTypes.APPLICATION_M3U8);
            player.setMediaItem(item.build());player.prepare();player.play();
        }catch(Exception e){error.setVisibility(View.VISIBLE);error.setText("خطأ في إعداد المشغل: "+e.getMessage());}
    }
    @Override protected void onStop(){super.onStop();releasePlayer();}
    @Override protected void onDestroy(){releasePlayer();super.onDestroy();}
    private void releasePlayer(){if(player!=null){playerView.setPlayer(null);player.release();player=null;}}
}