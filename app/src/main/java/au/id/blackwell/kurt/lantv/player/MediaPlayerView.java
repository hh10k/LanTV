package au.id.blackwell.kurt.lantv.player;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import java.io.IOException;

import au.id.blackwell.kurt.lantv.MediaDetails;
import au.id.blackwell.kurt.lantv.resolver.MediaResolver;
import au.id.blackwell.kurt.lantv.R;

public abstract class MediaPlayerView extends FrameLayout implements TvPlayer {

    private static final String TAG = "TvMediaPlayerView";

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
    protected static final int mTargetSurfaceFormat = PixelFormat.RGB_565;
    //protected static final int mTargetSurfaceFormat = PixelFormat.RGBA_8888;


    private TvPlayerStatusListener mListener = null;
    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mSurfaceHolder = null;
    private int mSurfaceWidth = 0;
    private int mSurfaceHeight = 0;
    private int mSurfaceFormat = 0;
    private int mLayoutWidth = 0;
    private int mLayoutHeight = 0;
    private boolean mSurfaceCreated = false;
    private TvMediaPlayer mMediaPlayer = null;
    private MediaPlayerState mMediaPlayerState = MediaPlayerState.NONE;
    private MediaDetails mMediaDetails = null;
    private MediaDetailsState mMediaDetailsState = MediaDetailsState.UNRESOLVED;
    private MediaResolver mMediaResolver = null;
    private PlayState mPlayState = PlayState.STOPPED;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private float mVideoAspectRatio = 0;

    private final MediaResolver.Callback mMediaResolverCallback = new MediaResolver.Callback() {
        @Override
        public void onMediaResolved(MediaDetails details) {
            mMediaDetails = details;
            mMediaDetailsState = MediaDetailsState.RESOLVED;

            if (mMediaDetails != null) {
                Log.d(TAG, "Got media details");
                onChangeState(TvPlayerState.RESOLVING, 1);
            } else {
                Log.d(TAG, "Failed to get media details");
                onFailureState(getContext().getString(R.string.error_vitamio_no_media));
            }

            update();
        }

        @Override
        public void onMediaResolverProgress(float progress) {
            onChangeState(TvPlayerState.RESOLVING, progress);
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

    private final TvMediaPlayerListener mMediaPlayerListener = new TvMediaPlayerListener() {
        @Override
        public void onPrepared() {
            Log.d(TAG, "MediaPlayer prepared");
            mMediaPlayerState = MediaPlayerState.PREPARED;
            update();
        }

        @Override
        public void onBufferingUpdate(int progress) {
            if (mMediaPlayer.isPlaying()) {
                onChangeState(TvPlayerState.PLAYING, 0);
            } else {
                onChangeState(TvPlayerState.BUFFERING, (float)progress / 100);
            }
        }

        @Override
        public boolean onError(int what, int extra) {
            Log.i(TAG, String.format("Error during playback: what=%d, extra=%d", what, extra));
            mMediaPlayerState = MediaPlayerState.ERROR;
            onFailureState(getContext().getString(R.string.error_vitamio_playback));
            return false;
        }

        @Override
        public void onVideoSizeChanged(int width, int height, float aspectRatio) {
            if (mVideoWidth != width || mVideoHeight != height || mVideoAspectRatio != aspectRatio) {
                Log.i(TAG, String.format("Video dimensions changed: (%d x %d)", width, height));
                mVideoWidth = width;
                mVideoHeight = height;
                mVideoAspectRatio = aspectRatio;
                update();
            }
        }

        @Override
        public boolean onInfo(int what, int extra) {
            Log.d(TAG, String.format("Media player info %d, %d", what, extra));
            return false;
        }

        @Override
        public void onCompletion() {
            mMediaPlayerState = MediaPlayerState.PLAYBACK_COMPLETE;
            Log.d(TAG, "Playback complete");
        }
    };

    public MediaPlayerView(Context context) {
        super(context);
        initView(context);
    }

    public MediaPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public MediaPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
    }

    protected abstract TvMediaPlayer createMediaPlayer(Context context);

    private boolean initMediaDetails() {
        if (mMediaDetailsState == MediaDetailsState.UNRESOLVED) {
            if (mMediaResolver != null) {
                Log.d(TAG, "Resolving media");
                mMediaDetailsState = MediaDetailsState.RESOLVING;
                onChangeState(TvPlayerState.RESOLVING, 0);
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
            mSurfaceHolder.setFormat(mTargetSurfaceFormat);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            addView(mSurfaceView, params);
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
            mMediaPlayer = createMediaPlayer(getContext());
            mMediaPlayer.setListener(mMediaPlayerListener);

            mMediaPlayerState = MediaPlayerState.IDLE;
        }

        if (mMediaPlayerState == MediaPlayerState.IDLE
                && initMediaDetails()
                && initSurface()) {
            Log.d(TAG, "Initialising MediaPlayer");
            try {
                mMediaPlayer.setMedia(mMediaDetails);
                mMediaPlayerState = MediaPlayerState.INITIALISED;
            } catch (IOException e) {
                onChangeState(TvPlayerState.FAILED, 0);
                mMediaPlayerState = MediaPlayerState.ERROR;
            }
        }

        if (mMediaPlayerState == MediaPlayerState.INITIALISED) {
            Log.d(TAG, "Preparing MediaPlayer");
            mMediaPlayerState = MediaPlayerState.PREPARING;
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setScreenOnWhilePlaying(true);
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

        boolean videoSizeKnown = mVideoWidth != 0 && mVideoHeight != 0;
        boolean surfaceReady = false;

        if (!videoSizeKnown) {
            Log.d(TAG, "Waiting for video size");
        } else {

            int viewWidth = getWidth();
            int viewHeight = getHeight();
            int fitWidth = viewWidth;
            int fitHeight = (int)(fitWidth / mVideoAspectRatio);
            if (fitHeight > viewHeight) {
                fitHeight = viewHeight;
                fitWidth = (int)(fitHeight * mVideoAspectRatio);
            }

            if (fitWidth != mLayoutWidth
                    || fitHeight != mLayoutHeight) {
                Log.d(TAG, String.format("Layout change from %dx%d to %dx%d", mLayoutWidth, mLayoutHeight, fitWidth, fitHeight));
                mLayoutWidth = fitWidth;
                mLayoutHeight = fitHeight;

                LayoutParams lp = (LayoutParams)mSurfaceView.getLayoutParams();
                lp.width = mLayoutWidth;
                lp.height = mLayoutHeight;
                mSurfaceView.setLayoutParams(lp);
                mSurfaceHolder.setFixedSize(mLayoutWidth, mLayoutHeight);
            }

            surfaceReady = mSurfaceFormat == mTargetSurfaceFormat
                    && mSurfaceWidth == mLayoutWidth
                    && mSurfaceHeight == mLayoutHeight;

            if (!surfaceReady) {
                Log.d(TAG, "Waiting for surface change");
            }
        }

        return videoSizeKnown && surfaceReady;
    }

    private void update() {
        switch (mPlayState) {
            case PLAYING:
                if (initMediaPlayerPresentation()
                    && mMediaPlayerState != MediaPlayerState.STARTED) {
                    Log.d(TAG, "Starting to play video");
                    onChangeState(TvPlayerState.CONNECTING, 0);
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
    public void setTvPlayerListener(TvPlayerStatusListener listener) {
        mListener = listener;
    }

    private void onChangeState(TvPlayerState state, float progress) {
        if (mListener == null) {
            return;
        }
        mListener.onTvPlayerStateChanged(state, progress);
    }

    private void onFailureState(String reason) {
        if (mListener == null) {
            return;
        }
        mListener.onTvPlayerFailed(reason);
    }

    public View getView() {
        return this;
    }

    public void release() {
        stop();
    }
}
