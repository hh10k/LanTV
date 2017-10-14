package au.id.blackwell.kurt.lantv.player;

import android.content.Context;
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

import java.util.ArrayList;

import au.id.blackwell.kurt.lantv.MediaDetails;
import au.id.blackwell.kurt.lantv.resolver.MediaResolver;
import au.id.blackwell.kurt.lantv.R;

final class VlcTvPlayerView extends RelativeLayout implements TvPlayer {

    private static final String TAG = "VlcTvPlayerView";

    private SurfaceView mSurface = null;
    private FrameLayout mSurfaceFrame = null;
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private MediaDetails mMediaDetails = null;
    private MediaResolver mMediaResolver = null;
    private TvPlayerStatusListener mListener = null;
    private TvPlayerState mPreviousState = TvPlayerState.NONE;
    private float mPreviousStateProgress = 0;

    private final MediaResolver.Callback mMediaResolverCallback = new MediaResolver.Callback() {
        @Override
        public void onMediaResolved(MediaDetails details) {
            notifyState(TvPlayerState.RESOLVING, 1);
            mMediaDetails = details;
            if (details == null) {
                notifyState(TvPlayerState.FAILED, 0);
            }
            resume();
        }

        @Override
        public void onMediaResolverProgress(float progress) {
            notifyState(TvPlayerState.RESOLVING, progress);
        }
    };

    private final IVLCVout.Callback mVlcVoutCallback = new IVLCVout.Callback() {
        @Override
        public void onSurfacesCreated(IVLCVout ivlcVout) {
            // TODO: Investigate purpose of VLC surface notifications
        }

        @Override
        public void onSurfacesDestroyed(IVLCVout ivlcVout) {
        }
    };

    private final OnLayoutChangeListener mLayoutChangedListener = new OnLayoutChangeListener() {
        private final Runnable mOnResize = new Runnable() {
            @Override
            public void run() {
                onResize();
            }
        };

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            int oldWidth = oldRight - oldLeft;
            int oldHeight = oldBottom - oldTop;
            int newWidth = right - left;
            int newHeight = bottom - top;
            if (newWidth != oldWidth || newHeight != oldHeight) {
                // Layout size invalidated.  Refresh at next opportunity.
                removeCallbacks(mOnResize);
                post(mOnResize);
            }
        }
    };

    private final IVLCVout.OnNewVideoLayoutListener mNewVideoLayoutLisener = new IVLCVout.OnNewVideoLayoutListener() {
        @Override
        public void onNewVideoLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            // TODO: Investigate purpose of onNewVideoLayout when VLC is responsible for maintaining the correct size.
        }
    };

    private final MediaPlayer.EventListener mMediaPlayerEventListener = new MediaPlayer.EventListener() {
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
                    notifyState(TvPlayerState.CONNECTING, 0);
                    break;
                case MediaPlayer.Event.Buffering:
                    float buffering = event.getBuffering();
                    Log.d(TAG, "MediaPlayer.Event.Buffering " + Float.toString(buffering));
                    if (buffering == 0) {
                        notifyState(TvPlayerState.CONNECTING, 0);
                    } else {
                        notifyState(TvPlayerState.BUFFERING, buffering / 100);
                    }
                    break;
                case MediaPlayer.Event.Playing:
                    Log.d(TAG, "MediaPlayer.Event.Playing");
                    break;
                case MediaPlayer.Event.Paused:
                    Log.d(TAG, "MediaPlayer.Event.Paused");
                    notifyState(TvPlayerState.PAUSED, 0);
                    break;
                case MediaPlayer.Event.Stopped:
                    Log.d(TAG, "MediaPlayer.Event.Stopped");
                    notifyState(TvPlayerState.PAUSED, 0);
                    break;
                case MediaPlayer.Event.EncounteredError:
                    Log.d(TAG, "MediaPlayer.Event.EncounteredError");
                    break;
                case MediaPlayer.Event.PositionChanged:
                    Log.d(TAG, "MediaPlayer.Event.PositionChanged " + Float.toString(event.getPositionChanged()));
                    notifyState(TvPlayerState.PLAYING, 0);
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
    };

    public VlcTvPlayerView(Context context) {
        super(context);
        init(context);
    }

    public VlcTvPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VlcTvPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        if (isInEditMode()) {
            return;
        }

        // Populate this view based on the layout resource
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_vlc_tv_player, this, true);

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
        destroyPlayer();
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

        mLibVLC.setUserAgent(mMediaDetails.getUserAgentName(), mMediaDetails.getHttpUserAgent());

        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(mMediaPlayerEventListener);

        IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.setVideoView(mSurface);
        vout.addCallback(mVlcVoutCallback);
        vout.attachViews(mNewVideoLayoutLisener);

        Media m = new Media(mLibVLC, mMediaDetails.getUri());
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
        vout.removeCallback(mVlcVoutCallback);
        vout.detachViews();

        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    public void play(MediaResolver mediaResolver) {
        stop();
        mMediaResolver = mediaResolver;

        notifyState(TvPlayerState.RESOLVING, 0);

        mMediaResolver.resolve(mMediaResolverCallback);
    }

    public void resume() {
        if (mMediaDetails == null) {
            // Nothing to resume
            return;
        }

        notifyState(TvPlayerState.CONNECTING, 0);

        Log.i(TAG, "Playing video");
        createPlayer();
        mMediaPlayer.play();
    }

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
                notifyState(TvPlayerState.PAUSED, 0);
            }
        } else if (mMediaDetails != null) {
            // We may resume later, but we definitely don't care about the result now.
            mMediaResolver.cancel(mMediaResolverCallback);
        }
    }

    /**
     * Stop video, closing it.
     */
    public void stop() {
        if (mMediaResolver != null) {
            mMediaResolver.cancel(mMediaResolverCallback);
            mMediaResolver = null;
        }
        mMediaDetails = null;
        destroyPlayer();
        mListener.onTvPlayerStateChanged(TvPlayerState.NONE, 0);
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

    public void setTvPlayerListener(TvPlayerStatusListener listener) {
        mListener = listener;
    }

    private void notifyState(TvPlayerState state, float progress) {
        if (mListener == null) {
            return;
        }
        if (mPreviousState == state && mPreviousStateProgress == progress) {
            return;
        }
        mPreviousState = state;
        mPreviousStateProgress = progress;
        mListener.onTvPlayerStateChanged(state, progress);
    }

    public View getView() {
        return this;
    }

    public void release() {
        mMediaPlayer.release();
    }
}
