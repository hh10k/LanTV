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
    private Uri mMediaUri = null;

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

        mSurface = (SurfaceView) findViewById(R.id.surface);
        mSurfaceFrame = (FrameLayout) findViewById(R.id.surface_frame);
        mSurfaceFrame.addOnLayoutChangeListener(mLayoutChangedListener);

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
    }

    private void deinit() {
        if (mLibVLC != null) {
            mLibVLC.release();
            mLibVLC = null;
        }
    }

    private void createPlayer() {
        if (mMediaPlayer != null) {
            // Already created
            return;
        }

        Log.i(TAG, "Creating video player");
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(this);

        IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.setVideoView(mSurface);
        vout.addCallback(this);
        vout.attachViews(this);

        Media m = new Media(mLibVLC, mMediaUri);
        // TODO: Investigate what these media options do.
        m.setHWDecoderEnabled(true, false);
        m.addOption(":no-mediacodec-dr");
        m.addOption(":no-omxil-dr");
        mMediaPlayer.setMedia(m);
        m.release();

        onResize();
    }

    private void destroyPlayer() {
        if (mMediaPlayer == null) {
            // Already destroyed
            return;
        }

        Log.i(TAG, "Destroying video player");
        mMediaPlayer.stop();

        IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(this);
        vout.detachViews();

        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    public void playFile(File file) {
        playUri(Uri.fromFile(file));
    }

    public void playUri(Uri mediaUri) {
        stop();
        mMediaUri = mediaUri;
        resume();
    }

    /**
     * Resume playing the video
     */
    public void resume() {
        if (mMediaUri == null) {
            // Nothing to resume
            return;
        }

        Log.i(TAG, "Playing video");
        createPlayer();
        mMediaPlayer.play();
    }

    /**
     * Pause video, if playing
     */
    public void pause() {
        if (mMediaPlayer != null) {
            // We are informed via the PausableChanged event whether a video is pausable, but it tells lies.
            // Checking whether a stream is seekable seems to be the best way of telling whether the video needs to be restarted when resuming.
            if (mMediaPlayer.isSeekable()) {
                Log.i(TAG, "Pausing video");
                mMediaPlayer.pause();
            } else {
                Log.i(TAG, "Stopping streaming video");
                stop();
            }
        }
    }

    /**
     * Stop video, closing it.
     */
    public void stop() {
        destroyPlayer();
    }

    private void onResize() {
        if (mMediaPlayer == null) {
            // Nothing to resize
            return;
        }

        // Set the video to auto fit.  We don't need to support anything else.
        // If you do, check https://code.videolan.org/videolan/libvlc-android-samples/blob/master/java_sample/src/main/java/org/videolan/javasample/JavaActivity.java
        mMediaPlayer.setAspectRatio(null);
        mMediaPlayer.setScale(0);

        // Tell VLC how big the surface is
        int width = getWidth();
        int height = getHeight();
        if (width != 0 && height != 0) {
            IVLCVout vout = mMediaPlayer.getVLCVout();
            vout.setWindowSize(width, height);
            Log.i(TAG, "Video resized to " + Integer.toString(width) + "x" + Integer.toString(height));
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
            case MediaPlayer.Event.Opening:
                Log.d(TAG, "MediaPlayer.Event.Opening");
                break;
            case MediaPlayer.Event.Buffering:
                Log.d(TAG, "MediaPlayer.Event.Buffering " + Float.toString(event.getBuffering()));
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
            case MediaPlayer.Event.PositionChanged:
                Log.d(TAG, "MediaPlayer.Event.PositionChanged " + Float.toString(event.getPositionChanged()));
                break;
            case MediaPlayer.Event.SeekableChanged:
                Log.d(TAG, "MediaPlayer.Event.SeekableChanged " + Boolean.toString(event.getSeekable()));
                break;
            case MediaPlayer.Event.PausableChanged:
                Log.d(TAG, "MediaPlayer.Event.PausableChanged " + Boolean.toString(event.getPausable()));
                break;
            default:
                Log.d(TAG, "MediaPlayer.Event." + Integer.toString(event.type));
                break;
        }
    }
}
