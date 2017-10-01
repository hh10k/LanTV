package au.id.blackwell.kurt.lantv;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;

import au.id.blackwell.kurt.lantv.utility.LimitedPool;

public final class MainActivity extends AppCompatActivity {

    private TvPlayer mVideo;
    private TvPlayerStatusView mVideoStatus;
    private LimitedPool<WebView> mWebViewPool = new LimitedPool<>();
    private MediaResolverFactory mMediaResolverFactory = new MediaResolverFactory(mWebViewPool);

    private static final String DEFAULT_TV_CHANNEL_ID = "cctv13";
    private static final TvChannel[] TV_CHANNELS = new TvChannel[] {
        new TvChannel("cctv1", "CCTV-1 综合", "cctv://tv.cctv.com/live/cctv1/hd"),
        new TvChannel("cctv2", "CCTV-2 财经", "cctv://tv.cctv.com/live/cctv2/hd"),
        new TvChannel("cctv3", "CCTV-3 综艺", "cctv://tv.cctv.com/live/cctv3/hd"),
        new TvChannel("cctv4", "CCTV-4 亚洲", "cctv://tv.cctv.com/live/cctv4/hd"),
        new TvChannel("cctv4", "CCTV-4 欧洲", "cctv://tv.cctv.com/live/cctveurope/hd"),
        new TvChannel("cctv4", "CCTV-4 美洲", "cctv://tv.cctv.com/live/cctvamerica/hd"),
        new TvChannel("cctv5", "CCTV-5 体育", "cctv://tv.cctv.com/live/cctv5/hd"),
        new TvChannel("cctv5plus", "CCTV-5+ 体育赛事", "cctv://tv.cctv.com/live/cctv5plus/hd"),
        new TvChannel("cctv6", "CCTV-6 电影", "cctv://tv.cctv.com/live/cctv6/hd"),
        new TvChannel("cctv7", "CCTV-7 军事农业", "cctv://tv.cctv.com/live/cctv7/hd"),
        new TvChannel("cctv8", "CCTV-8 电视剧", "cctv://tv.cctv.com/live/cctv8/hd"),
        new TvChannel("cctv9", "CCTV-9 纪录", "cctv://tv.cctv.com/live/cctvjilu/hd"),
        new TvChannel("cctv10", "CCTV-10 科教", "cctv://tv.cctv.com/live/cctv10/hd"),
        new TvChannel("cctv11", "CCTV-11 戏曲", "cctv://tv.cctv.com/live/cctv11/hd"),
        new TvChannel("cctv12", "CCTV-12 社会与法", "cctv://tv.cctv.com/live/cctv12/hd"),
        new TvChannel("cctv13", "CCTV-13 新闻", "cctv://tv.cctv.com/live/cctv13/hd"),
        new TvChannel("cctv14", "CCTV-14 少儿", "cctv://tv.cctv.com/live/cctvchild/hd"),
        new TvChannel("cctv15", "CCTV-15 音乐", "cctv://tv.cctv.com/live/cctv15/hd"),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideo = (VlcTvPlayerView) findViewById(R.id.video);
        mVideoStatus = (TvPlayerStatusView) findViewById(R.id.video_status);
        mWebViewPool.addItem((WebView)findViewById(R.id.worker_web_view));

        mVideo.setTvPlayerListener(mVideoStatus);

        // If we set this cookie then it'll allow us to use the HD page.
        // I'm not sure if the quality is actually any better.
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setCookie("http://tv.cctv.com/", "country_code=CN; path=/live/cctv13");

        TvChannel channel = TV_CHANNELS[0];
        for (TvChannel c : TV_CHANNELS) {
            if (c.getId().equals(DEFAULT_TV_CHANNEL_ID)) {
                channel = c;
                break;
            }
        }

        MediaResolver resolver = mMediaResolverFactory.create(channel.getMediaResolveUri());
        mVideoStatus.setTitle(channel.getTitle());
        mVideo.play(resolver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        findViewById(android.R.id.content).setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        mVideo.resume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVideo.pause();
    }
}
