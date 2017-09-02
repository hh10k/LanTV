package au.id.blackwell.kurt.lantv;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.io.File;
import java.util.ArrayList;

public class VlcVideoView extends RelativeLayout implements IVLCVout.Callback, IVLCVout.OnNewVideoLayoutListener, MediaPlayer.EventListener {

    private static final String TAG = "VideoView";

    private SurfaceView mSurface = null;
    private FrameLayout mSurfaceFrame = null;
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;

    private final OnLayoutChangeListener mLayoutChangedListener = new OnLayoutChangeListener() {
        private final Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                onResize();
            }
        };

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                // Layout size invalidated.  Refresh at next opportunity.
                removeCallbacks(mRunnable);
                post(mRunnable);
            }
        }
    };

    public VlcVideoView(Context context) {
        super(context);
        init(context);
    }

    public VlcVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VlcVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        // Populate this view based on the layout resource
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        addView(inflater.inflate(R.layout.view_vlc_video, null));

        mSurfaceFrame = (FrameLayout) findViewById(R.id.surface_frame);
        mSurface = (SurfaceView) findViewById(R.id.surface);

        mSurfaceFrame.addOnLayoutChangeListener(mLayoutChangedListener);
    }

    public void playFile(File file) {
        playUri(Uri.fromFile(file));
    }

    public void playUri(Uri mediaUri) {
        closeMedia();

        ArrayList<String> options = new ArrayList<String>();
        options.add("-vvv"); // Very verbose
        // TODO: Investigate VLC options.  These were used by VLC the Android app.
        options.add("--audio-time-stretch");
        options.add("--avcodec-skiploopfilter");
        options.add("1");
        options.add("--avcodec-skip-frame");
        options.add("0");
        options.add("--avcodec-skip-idct");
        options.add("0");
        mLibVLC = new LibVLC(getContext(), options);

        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(this);

        IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.setVideoView(mSurface);
        vout.addCallback(this);
        vout.attachViews(this);

        Media m = new Media(mLibVLC, mediaUri);
        // TODO: Investigate what these media options do.
        m.setHWDecoderEnabled(true, false);
        m.addOption(":no-mediacodec-dr");
        m.addOption(":no-omxil-dr");

        mMediaPlayer.setMedia(m);
        m.release();
        mMediaPlayer.play();

        onResize();
    }

    /**
     * Resume playing the video
     */
    public void resume() {
        if (mMediaPlayer != null) {
            mMediaPlayer.play();
        }
    }

    /**
     * Pause video, if playing
     */
    public void pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    public void closeMedia() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();

            IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.removeCallback(this);
            vout.detachViews();

            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mLibVLC != null) {
            mLibVLC.release();
            mLibVLC = null;
        }
    }

    private void onResize() {
        if (mMediaPlayer != null) {
            // Tell VLC how big the surface is
            int width = getWidth();
            int height = getHeight();
            if (width != 0 && height != 0) {
                IVLCVout vout = mMediaPlayer.getVLCVout();
                vout.setWindowSize(width, height);
            }

            // Set the video to auto fit.  We don't need to support anything else.
            // If you do, check https://code.videolan.org/videolan/libvlc-android-samples/blob/master/java_sample/src/main/java/org/videolan/javasample/JavaActivity.java
            mMediaPlayer.setAspectRatio(null);
            mMediaPlayer.setScale(0);
        }
    }

    @Override
    public void onSurfacesCreated(IVLCVout ivlcVout) {
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout ivlcVout) {
    }

    @Override
    public void onNewVideoLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {

        switch(event.type) {
            case MediaPlayer.Event.MediaChanged:
                Log.d(TAG, "MediaPlayer.Event.MediaChanged");
                break;
            case MediaPlayer.Event.EndReached:
                Log.d(TAG, "MediaPlayer.Event.EndReached");
                break;
            case MediaPlayer.Event.Playing:
                Log.d(TAG, "MediaPlayer.Event.Playing");
                break;
            case MediaPlayer.Event.Paused:
                Log.d(TAG, "MediaPlayer.Event.Paused");
                break;
            case MediaPlayer.Event.Stopped:
                Log.d(TAG, "MediaPlayer.Event.Stopped");
                break;
            case MediaPlayer.Event.EncounteredError:
                Log.d(TAG, "MediaPlayer.Event.EncounteredError");
                break;
            default:
                Log.d(TAG, "MediaPlayer.Event." + Integer.toString(event.type));
                break;
        }
    }
}
