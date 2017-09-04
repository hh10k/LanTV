package au.id.blackwell.kurt.lantv;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

public final class MainActivity extends AppCompatActivity {

    private VlcVideoView mVideo;
    private Pool<WebView> mWebViewPool = new Pool<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideo = (VlcVideoView) findViewById(R.id.video);
        mWebViewPool.addItem((WebView)findViewById(R.id.worker_web_view));

        mVideo.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        //MediaDetails media = new MediaDetails(Uri.fromFile((new File("/storage/sdcard/Download/big_buck_bunny.mp4"))));
        //IMediaResolver resolver = new StaticMediaResolver(media);
        IMediaResolver resolver = new CctvMediaResolver(mWebViewPool, "http://tv.cctv.com/live/cctv13/sd/index.shtml");
        mVideo.play(resolver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mVideo.resume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVideo.pause();
    }
}
