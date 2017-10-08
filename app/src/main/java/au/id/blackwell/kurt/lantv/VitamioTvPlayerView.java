package au.id.blackwell.kurt.lantv;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.util.HashMap;

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

            onPlayPrerequisiteChanged();
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
            // TODO: Reassign holder to existing media player?
            onPlayPrerequisiteChanged();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, String.format("Surface resized (%d, %d) format %d", width, height, format));
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "Surface destroyed");
            mSurfaceCreated = false;
            if (mMediaPlayer != null) {
                mMediaPlayer.reset();
                mMediaPlayerState = MediaPlayerState.IDLE;
            }
        }
    };

    private final MediaPlayer.OnPreparedListener mMediaPlayerOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            Log.d(TAG, "MediaPlayer prepared");
            mMediaPlayerState = MediaPlayerState.PREPARED;
            onPlayPrerequisiteChanged();
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
                onPlayPrerequisiteChanged();
            }
        }
    };

    public VitamioTvPlayerView(Context context) {
        super(context);
        init(context);
    }

    public VitamioTvPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VitamioTvPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        if (isInEditMode()) {
            return;
        }

        // Populate this view based on the layout resource
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_vitamio_tv_player, this, true);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurfaceHolder = mSurfaceView.getHolder();

        mSurfaceHolder.setFormat(mSurfacePixelFormat);
        mSurfaceHolder.addCallback(mSurfaceHolderCallback);
    }

    private void cancelPlay() {
        if (mMediaDetailsState == MediaDetailsState.RESOLVING) {
            Log.d(TAG, "Resolving media cancelled");
            mMediaDetailsState = MediaDetailsState.UNRESOLVED;
            mMediaResolver.cancel(mMediaResolverCallback);
        }
        if (mMediaPlayer != null) {
            Log.d(TAG, "Resetting MediaPlayer");
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mMediaPlayerState = MediaPlayerState.NONE;
        }
    }

    private void onPlayPrerequisiteChanged() {
        if (mPlayState != PlayState.PLAYING) {
            if (mPlayState == PlayState.PAUSED) {
                if (mMediaPlayerState == MediaPlayerState.STARTED) {
                    mMediaPlayer.pause();
                }
            } else {
                cancelPlay();
            }
            return;
        }

        if (mMediaDetails == null) {
            // Get media first
            if (mMediaResolver != null && mMediaDetailsState == MediaDetailsState.UNRESOLVED) {
                Log.d(TAG, "Resolving media");
                mMediaDetailsState = MediaDetailsState.RESOLVING;
                onStateProgress(TvPlayerState.RESOLVING, 0);
                mMediaResolver.resolve(mMediaResolverCallback);
            }
            return;
        }

        if (!mSurfaceCreated) {
            // Nothing to draw to yet
            Log.d(TAG, "Waiting for surface to be created");
            return;
        }

        if (mMediaPlayerState == MediaPlayerState.NONE) {
            Log.d(TAG, "Creating MediaPlayer");
            mMediaPlayerState = MediaPlayerState.IDLE;
            mMediaPlayer = new MediaPlayer(getContext(), true);
            //mMediaPlayer.setBufferSize(...);
            mMediaPlayer.setOnPreparedListener(mMediaPlayerOnPreparedListener);
            mMediaPlayer.setOnBufferingUpdateListener(mMediaPlayerOnBufferingUpdateListener);
            mMediaPlayer.setOnErrorListener(mMediaPlayerOnErrorListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mMediaPlayerOnVideoSizeChangedListener);
            mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
                    return false;
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {

                }
            });
            mMediaPlayer.setOnHWRenderFailedListener(new MediaPlayer.OnHWRenderFailedListener() {
                @Override
                public void onFailed() {

                }
            });
            mMediaPlayer.setOnCachingUpdateListener(new MediaPlayer.OnCachingUpdateListener() {
                @Override
                public void onCachingUpdate(MediaPlayer mediaPlayer, long[] longs) {

                }

                @Override
                public void onCachingSpeed(MediaPlayer mediaPlayer, int i) {

                }

                @Override
                public void onCachingStart(MediaPlayer mediaPlayer) {

                }

                @Override
                public void onCachingComplete(MediaPlayer mediaPlayer) {

                }

                @Override
                public void onCachingNotAvailable(MediaPlayer mediaPlayer, int i) {

                }
            });
            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mediaPlayer) {

                }
            });
        }
        if (mMediaPlayerState == MediaPlayerState.IDLE) {
            Log.d(TAG, "Initialising MediaPlayer");
            try {
                // TODO: Check whether we can set the user agent
                HashMap<String, String> headers = new HashMap<>();
                //mMediaPlayer.setDataSource(getContext(), mMediaDetails.getUri(), headers);
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
            mMediaPlayer.setDisplay(mSurfaceHolder);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.prepareAsync();
        }
        if (mMediaPlayerState != MediaPlayerState.PREPARED) {
            return;
        }

        if (mVideoWidth == 0 && mVideoHeight == 0) {
            // The video size should be known by now.  If not, we shouldn't start playing.
            Log.d(TAG, "Waiting for video layout");
            return;
        }

        mSurfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);

        if (mMediaPlayerState != MediaPlayerState.STARTED) {
            Log.d(TAG, "Starting to play video");
            onStateProgress(TvPlayerState.CONNECTING, 0);
            mMediaPlayer.start();
            mMediaPlayerState = MediaPlayerState.STARTED;
        }
    }

    @Override
    public void play(MediaResolver mediaResolver) {
        stop();

        mPlayState = PlayState.PLAYING;
        mMediaResolver = mediaResolver;
        onPlayPrerequisiteChanged();
    }

    @Override
    public void resume() {
        if (mPlayState != PlayState.PAUSED) {
            return;
        }
        mPlayState = PlayState.PLAYING;

        if (mMediaPlayerState == MediaPlayerState.PAUSED) {
            mMediaPlayerState = MediaPlayerState.STARTED;
            mMediaPlayer.start();
        } else {
            onPlayPrerequisiteChanged();
        }
    }

    @Override
    public void pause() {
        if (mPlayState == PlayState.STOPPED) {
            // Can't pause while stopped.
            return;
        }

        mPlayState = PlayState.PAUSED;
        onPlayPrerequisiteChanged();
    }

    @Override
    public void stop() {
        mPlayState = PlayState.STOPPED;
        onPlayPrerequisiteChanged();
        mMediaResolver = null;
        mMediaDetailsState = MediaDetailsState.UNRESOLVED;
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