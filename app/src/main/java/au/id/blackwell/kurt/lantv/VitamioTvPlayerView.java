package au.id.blackwell.kurt.lantv;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.io.IOException;

import io.vov.vitamio.MediaPlayer;

public class VitamioTvPlayerView  extends RelativeLayout implements TvPlayer {

    private static final String TAG = "VitamioTvPlayerView";

    enum MediaDetailsState {
        UNRESOLVED,
        RESOLVING,
        RESOLVED,
    }

    enum MediaPlayerState {
        NONE,
        IDLE,
        INITIALISED,
        PREPARING,
        PREPARED,
        STARTED,
        PAUSED,
        STOPPED,
        PLAYBACK_COMPLETE,
        ERROR,
    }

    enum PlayState {
        STOPPED,
        PLAYING,
        PAUSED,
    }

    // RGBA video does not seem to be supported on the MoonBox and results in a sort of corrupted appearance.
    // TODO: Investigate whether this is because of a format mismatch, or whether we can detect the format of the device.
    private static final int mSurfacePixelFormat = PixelFormat.RGB_565;
    //private static final int mSurfacePixelFormat = PixelFormat.RGBA_8888;
    private static final int mMediaPlayerVideoChroma = MediaPlayer.VIDEOCHROMA_RGB565;
    //private static final int mMediaPlayerVideoChroma = MediaPlayer.VIDEOCHROMA_RGBA;


    private TvPlayerListener mListener = null;
    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private int mSurfaceFormat = 0;
    private boolean mSurfaceCreated = false;
    private MediaPlayer mMediaPlayer = null;
    private MediaPlayerState mMediaPlayerState = MediaPlayerState.NONE;
    private MediaDetails mMediaDetails = null;
    private MediaDetailsState mMediaDetailsState = MediaDetailsState.UNRESOLVED;
    private MediaResolver mMediaResolver = null;
    private PlayState mPlayState = PlayState.STOPPED;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;

    private final MediaResolver.Callback mMediaResolverCallback = new MediaResolver.Callback() {
        @Override
        public void onMediaResolved(MediaDetails details) {
            mMediaDetails = details;
            mMediaDetailsState = MediaDetailsState.RESOLVED;

            if (mMediaDetails != null) {
                Log.d(TAG, "Got media details");
                onStateProgress(TvPlayerState.RESOLVING, 1);
            } else {
                Log.d(TAG, "Failed to get media details");
                onStateProgress(TvPlayerState.FAILED, 0);
            }

            update();
        }

        @Override
        public void onMediaResolverProgress(float progress) {
            onStateProgress(TvPlayerState.RESOLVING, progress);
        }
    };

    private final SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mSurfaceCreated = true;
            Log.d(TAG, "Surface created");
            update();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, String.format("Surface resized (%d, %d) format %d", width, height, format));
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            mSurfaceFormat = format;
            update();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mSurfaceCreated = false;
            mSurfaceWidth = 0;
            mSurfaceHeight = 0;
            mSurfaceFormat = 0;
        }
    };

    private final MediaPlayer.OnPreparedListener mMediaPlayerOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            Log.d(TAG, "MediaPlayer prepared");
            mMediaPlayerState = MediaPlayerState.PREPARED;
            update();
        }
    };

    private final MediaPlayer.OnBufferingUpdateListener mMediaPlayerOnBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mediaPlayer, int progress) {
            if (mediaPlayer.isPlaying()) {
                onStateProgress(TvPlayerState.PLAYING, 0);
            } else {
                onStateProgress(TvPlayerState.BUFFERING, (float)progress / 100);
            }
        }
    };

    private final MediaPlayer.OnErrorListener mMediaPlayerOnErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
            Log.i(TAG, String.format("Error during playback: what=%d, extra=%d", what, extra));
            mMediaPlayerState = MediaPlayerState.ERROR;
            onStateProgress(TvPlayerState.FAILED, 0);
            return false;
        }
    };

    private final MediaPlayer.OnVideoSizeChangedListener mMediaPlayerOnVideoSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
            if (mVideoWidth != width || mVideoHeight != height) {
                Log.i(TAG, String.format("Video dimensions changed: (%d x %d)", width, height));
                mVideoWidth = width;
                mVideoHeight = height;
                update();
            }
        }
    };

    private final MediaPlayer.OnInfoListener mMediaPlayerOnInfoListener = new MediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
            Log.d(TAG, String.format("Media player info %d, %d", what, extra));
            return false;
        }
    };

    private final MediaPlayer.OnCompletionListener mMediaPlayerOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mMediaPlayerState = MediaPlayerState.PLAYBACK_COMPLETE;
            Log.d(TAG, "Playback complete");
        }
    };

    private final MediaPlayer.OnHWRenderFailedListener mMediaPlayerOnHWRenderFailedListener = new MediaPlayer.OnHWRenderFailedListener() {
        @Override
        public void onFailed() {
            Log.d(TAG, "Hardware renderer failed");
        }
    };

    private final MediaPlayer.OnCachingUpdateListener mMediaPlayerOnCachingUpdateListener = new MediaPlayer.OnCachingUpdateListener() {
        @Override
        public void onCachingUpdate(MediaPlayer mediaPlayer, long[] longs) {
            Log.d(TAG, "Caching update");
        }

        @Override
        public void onCachingSpeed(MediaPlayer mediaPlayer, int speed) {
            Log.d(TAG, String.format("Caching speed %d", speed));
        }

        @Override
        public void onCachingStart(MediaPlayer mediaPlayer) {
            Log.d(TAG, "Caching start");
        }

        @Override
        public void onCachingComplete(MediaPlayer mediaPlayer) {
            Log.d(TAG, "Caching complete");
        }

        @Override
        public void onCachingNotAvailable(MediaPlayer mediaPlayer, int info) {
            Log.d(TAG, String.format("Caching not available (%d)", info));
        }
    };

    private final MediaPlayer.OnSeekCompleteListener mMediaPlayerOnSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(MediaPlayer mediaPlayer) {
            Log.d(TAG, "Seek complete");
        }
    };

    public VitamioTvPlayerView(Context context) {
        super(context);
        initView(context);
    }

    public VitamioTvPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public VitamioTvPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
    }

    private boolean initMediaDetails() {
        if (mMediaDetailsState == MediaDetailsState.UNRESOLVED) {
            if (mMediaResolver != null) {
                Log.d(TAG, "Resolving media");
                mMediaDetailsState = MediaDetailsState.RESOLVING;
                onStateProgress(TvPlayerState.RESOLVING, 0);
                mMediaResolver.resolve(mMediaResolverCallback);
            } else {
                Log.d(TAG, "Can't yet resolve media, nothing to play.");
                mPlayState = PlayState.STOPPED;
            }
        }

        return mMediaDetails != null;
    }

    private void deinitMediaDetails() {
        deinitMediaPlayer();

        if (mMediaDetailsState == MediaDetailsState.RESOLVING) {
            Log.d(TAG, "Media request cancelled");
            mMediaResolver.cancel(mMediaResolverCallback);
        } else if (mMediaDetailsState == MediaDetailsState.RESOLVED) {
            Log.d(TAG, "Media details forgotten");
            mMediaDetails = null;
        }
        mMediaDetailsState = MediaDetailsState.UNRESOLVED;
    }

    private boolean initSurface() {
        if (mSurfaceView == null) {
            mSurfaceView = new SurfaceView(getContext());
            mSurfaceHolder = mSurfaceView.getHolder();
            mSurfaceHolder.addCallback(mSurfaceHolderCallback);
            mSurfaceHolder.setFormat(mSurfacePixelFormat);
            addView(mSurfaceView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }

        if (!mSurfaceCreated) {
            // Nothing to draw to yet
            Log.d(TAG, "Waiting for surface to be created");
        }

        return mSurfaceCreated;
    }

    private void deinitSurface() {
        deinitMediaPlayer();

        if (mSurfaceView != null) {
            Log.d(TAG, "Surface destroyed");
            removeView(mSurfaceView);
            mSurfaceView = null;
            mSurfaceHolder.removeCallback(mSurfaceHolderCallback);
            mSurfaceHolder = null;
        }
    }

    private boolean initMediaPlayer() {
        if (mMediaPlayerState == MediaPlayerState.NONE) {
            Log.d(TAG, "Creating MediaPlayer");
            mMediaPlayer = new MediaPlayer(getContext(), true);
            //mMediaPlayer.setBufferSize(...);
            mMediaPlayer.setOnPreparedListener(mMediaPlayerOnPreparedListener);
            mMediaPlayer.setOnBufferingUpdateListener(mMediaPlayerOnBufferingUpdateListener);
            mMediaPlayer.setOnErrorListener(mMediaPlayerOnErrorListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mMediaPlayerOnVideoSizeChangedListener);
            mMediaPlayer.setOnInfoListener(mMediaPlayerOnInfoListener);
            mMediaPlayer.setOnCompletionListener(mMediaPlayerOnCompletionListener);
            mMediaPlayer.setOnHWRenderFailedListener(mMediaPlayerOnHWRenderFailedListener);
            mMediaPlayer.setOnCachingUpdateListener(mMediaPlayerOnCachingUpdateListener);
            mMediaPlayer.setOnSeekCompleteListener(mMediaPlayerOnSeekCompleteListener);

            mMediaPlayerState = MediaPlayerState.IDLE;
        }

        if (mMediaPlayerState == MediaPlayerState.IDLE
                && initMediaDetails()
                && initSurface()) {
            Log.d(TAG, "Initialising MediaPlayer");
            try {
                // TODO: Check whether we can set the user agent
                //HashMap<String, String> headers = new HashMap<>();
                //mMediaPlayer.setDataSource(mMediaDetails.getUri().toString(), headers);
                mMediaPlayer.setDataSource(mMediaDetails.getUri().toString());
                mMediaPlayerState = MediaPlayerState.INITIALISED;
            } catch (IOException e) {
                onStateProgress(TvPlayerState.FAILED, 0);
                mMediaPlayerState = MediaPlayerState.ERROR;
            }
        }

        if (mMediaPlayerState == MediaPlayerState.INITIALISED) {
            Log.d(TAG, "Preparing MediaPlayer");
            mMediaPlayerState = MediaPlayerState.PREPARING;
            mMediaPlayer.setVideoChroma(mMediaPlayerVideoChroma);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.prepareAsync();
        }

        return mMediaPlayerState == MediaPlayerState.PREPARED;
    }

    private void deinitMediaPlayer() {
        if (mMediaPlayer != null) {
            Log.d(TAG, "Releasing MediaPlayer");
            mMediaPlayer.setDisplay(null);
            mMediaPlayer.release();
            mMediaPlayer = null;
            mMediaPlayerState = MediaPlayerState.NONE;
        }
    }

    private boolean initMediaPlayerPresentation() {
        if (!initMediaPlayer()) {
            return false;
        }

        boolean layoutReady = mVideoWidth != 0 && mVideoHeight != 0;
        boolean surfaceReady = mSurfaceWidth == mVideoWidth
                && mSurfaceHeight == mVideoHeight
                && mSurfaceFormat == mSurfacePixelFormat
                && mMediaPlayerState != MediaPlayerState.STARTED;

        if (!layoutReady) {
            Log.d(TAG, "Waiting for video layout");
        } else if (!surfaceReady) {
            Log.d(TAG, "Waiting for surface change");
            mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
            mSurfaceHolder.setFormat(mSurfacePixelFormat);
        }

        return layoutReady && surfaceReady;
    }

    private void update() {
        switch (mPlayState) {
            case PLAYING:
                if (initMediaPlayerPresentation()
                    && mMediaPlayerState != MediaPlayerState.STARTED) {
                    Log.d(TAG, "Starting to play video");
                    onStateProgress(TvPlayerState.CONNECTING, 0);
                    mMediaPlayer.start();
                    mMediaPlayerState = MediaPlayerState.STARTED;
                }
                break;
            case PAUSED:
                if (mMediaPlayerState == MediaPlayerState.STARTED) {
                    Log.d(TAG, "Pausing video");
                    mMediaPlayerState = MediaPlayerState.PAUSED;
                    mMediaPlayer.pause();
                } else if (mMediaDetailsState == MediaDetailsState.RESOLVING) {
                    deinitMediaDetails();
                }
                break;
            case STOPPED:
                deinitSurface();
                deinitMediaDetails();
                mMediaResolver = null;
                mMediaDetails = null;
                break;
        }
    }

    @Override
    public void play(MediaResolver mediaResolver) {
        stop();
        mMediaResolver = mediaResolver;
        mPlayState = PlayState.PLAYING;
        update();
    }

    @Override
    public void resume() {
        mPlayState = PlayState.PLAYING;
        update();
    }

    @Override
    public void pause() {
        mPlayState = PlayState.PAUSED;
        update();
    }

    @Override
    public void stop() {
        mPlayState = PlayState.STOPPED;
        update();
    }

    @Override
    public void setTvPlayerListener(TvPlayerListener listener) {
        mListener = listener;
    }

    private void onStateProgress(TvPlayerState state, float progress) {
        if (mListener == null) {
            return;
        }
        mListener.onTvPlayerStateChanged(state, progress);
    }

    public View getView() {
        return this;
    }

    public void release() {
        stop();
    }
}