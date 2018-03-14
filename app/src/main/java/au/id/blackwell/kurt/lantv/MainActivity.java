package au.id.blackwell.kurt.lantv;

import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;

import au.id.blackwell.kurt.lantv.player.TvPlayer;
import au.id.blackwell.kurt.lantv.player.TvPlayerFactory;
import au.id.blackwell.kurt.lantv.resolver.MediaResolver;
import au.id.blackwell.kurt.lantv.resolver.MediaResolverFactory;
import au.id.blackwell.kurt.lantv.utility.LimitedPool;

public final class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private String mPlayerType;
    private TvPlayer mPlayer;
    private static final TvPlayerFactory mPlayerFactory = new TvPlayerFactory();
    private ViewGroup mPlayerFrame;
    private TvPlayerStatusView mPlayerStatus;
    private final LimitedPool<WebView> mWebViewPool = new LimitedPool<>();
    private final MediaResolverFactory mMediaResolverFactory = new MediaResolverFactory(mWebViewPool);
    private int mChannelIndex = getTvChannelIndexById(DEFAULT_TV_CHANNEL_ID);

    private static final String DEFAULT_TV_CHANNEL_ID = "cctv13";
    private static final String DEFAULT_PLAYER = TvPlayerFactory.IJK;
    private static final TvChannel[] TV_CHANNELS = new TvChannel[] {
        new TvChannel("cctv1", "CCTV-1 综合", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv1/hd/"),
        new TvChannel("cctv2", "CCTV-2 财经", null /* unavailable online */, "cctv://tv.cctv.com/live/cctv2/hd/"),
        new TvChannel("cctv3", "CCTV-3 综艺", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv3/hd/"),
        new TvChannel("cctv4", "CCTV-4 亚洲", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv4/hd/"),
        new TvChannel("cctveurope", "CCTV-4 欧洲", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctveurope/hd/"),
        new TvChannel("cctvamerica", "CCTV-4 美洲", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctvamerica/hd/"),
        new TvChannel("cctv5", "CCTV-5 体育", null /* unavailable in my region */, "cctv://tv.cctv.com/live/cctv5/hd/"),
        new TvChannel("cctv5plus", "CCTV-5+ 体育赛事", null /* unavailable in my region */, "cctv://tv.cctv.com/live/cctv5plus/hd/"),
        new TvChannel("cctv6", "CCTV-6 电影", null /* unavailable online */, "cctv://tv.cctv.com/live/cctv6/hd/"),
        new TvChannel("cctv7", "CCTV-7 军事农业", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv7/hd/"),
        new TvChannel("cctv8", "CCTV-8 电视剧", null /* unavailable online */, "cctv://tv.cctv.com/live/cctv8/hd/"),
        new TvChannel("cctvjilu", "CCTV-9 纪录", null /* unavailable online */, "cctv://tv.cctv.com/live/cctvjilu/hd/"),
        new TvChannel("cctv10", "CCTV-10 科教", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv10/hd/"),
        new TvChannel("cctv11", "CCTV-11 戏曲", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv11/hd/"),
        new TvChannel("cctv12", "CCTV-12 社会与法", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv12/hd/"),
        new TvChannel("cctv13", "CCTV-13 新闻", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv13/hd/"),
        new TvChannel("cctvchild", "CCTV-14 少儿", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctvchild/hd/"),
        new TvChannel("cctv15", "CCTV-15 音乐", DEFAULT_PLAYER, "cctv://tv.cctv.com/live/cctv15/hd/"),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mPlayerFrame = (ViewGroup) findViewById(R.id.player_frame);
        mPlayerStatus = (TvPlayerStatusView) findViewById(R.id.player_status);
        mWebViewPool.addItem((WebView)findViewById(R.id.worker_web_view));

        // If we set this cookie then it'll allow us to use the HD page.
        // I'm not sure if the quality is actually any better.
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setCookie("http://tv.cctv.com/", "country_code=CN; path=/live");
    }

    @Override
    protected void onStart() {
        super.onStart();
        setTvChannel(mChannelIndex);
    }

    @Override
    protected void onResume() {
        super.onResume();

        findViewById(android.R.id.content).setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        mPlayer.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        destroyPlayer();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                stepTvChannel(1);
                handled = true;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                stepTvChannel(-1);
                handled = true;
                break;
        }

        return handled;
    }

    private void stepTvChannel(int direction) {
        int channelIndex = mChannelIndex;
        int step = TV_CHANNELS.length + direction;

        do {
            channelIndex = (channelIndex + step) % TV_CHANNELS.length;
        } while (!TV_CHANNELS[channelIndex].isPlayable()
                && channelIndex != mChannelIndex);

        setTvChannel(channelIndex);
        mPlayer.play();
    }

    private static int getTvChannelIndexById(String id) {
        int channelIndex = 0;
        for (int i = 0; i < TV_CHANNELS.length; ++i) {
            if (TV_CHANNELS[i].getId().equals(DEFAULT_TV_CHANNEL_ID)) {
                channelIndex = i;
                break;
            }
        }
        return channelIndex;
    }

    private void setTvChannel(int channelIndex) {
        TvChannel channel = TV_CHANNELS[channelIndex];

        // Restart playback if we had to recreate the player or the channel has changed
        if (createPlayer(channel.getPlayerType())
                || channelIndex != mChannelIndex) {
            mChannelIndex = channelIndex;
            MediaResolver resolver = mMediaResolverFactory.create(channel.getMediaResolveUri());
            mPlayerStatus.setTitle(channel.getTitle());
            mPlayer.reset(resolver);
        }
    }

    private boolean createPlayer(String playerType) {
        if (!playerType.equals(mPlayerType)) {
            destroyPlayer();
        }
        if (mPlayerType == null) {
            mPlayerType = playerType;
            mPlayer = mPlayerFactory.create(mPlayerType, this);
            mPlayer.setTvPlayerListener(mPlayerStatus);
            mPlayerFrame.addView(mPlayer.getView(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            return true;
        }

        return false;
    }

    private void destroyPlayer() {
        if (mPlayerType != null) {
            mPlayer.stop();
            mPlayer.setTvPlayerListener(null);
            mPlayerFrame.removeView(mPlayer.getView());
            mPlayer.release();
            mPlayer = null;
            mPlayerType = null;
        }
    }
}
