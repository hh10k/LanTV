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

    private static final TvChannel[] TV_CHANNELS = new TvChannel[] {
        //new TvChannel("Test", Uri.fromFile(new File("/storage/sdcard/Download/big_buck_bunny.mp4"))),
        new TvChannel("CCTV13", "cctv://tv.cctv.com/live/cctv13/hd/index.shtml"),
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
