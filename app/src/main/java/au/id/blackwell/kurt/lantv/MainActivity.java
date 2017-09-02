package au.id.blackwell.kurt.lantv;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private VlcVideoView mVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideo = (VlcVideoView) findViewById(R.id.video);
        mVideo.playFile(new File("/storage/sdcard/Download/big_buck_bunny.mp4"));
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
