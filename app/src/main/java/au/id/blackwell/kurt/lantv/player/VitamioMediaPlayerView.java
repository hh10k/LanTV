package au.id.blackwell.kurt.lantv.player;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.Log;

import java.io.IOException;

import au.id.blackwell.kurt.lantv.MediaDetails;
import io.vov.vitamio.MediaPlayer;

class VitamioMediaPlayerView extends MediaPlayerView {
    private static final String TAG = "VitamioTvPlayerView";

    private static final int mMediaPlayerVideoChroma = (mSurfacePixelFormat == PixelFormat.RGB_565) ? MediaPlayer.VIDEOCHROMA_RGB565 : MediaPlayer.VIDEOCHROMA_RGBA;

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

    class VitamioMediaPlayer extends MediaPlayer implements
            TvMediaPlayer,
            MediaPlayer.OnPreparedListener,
            MediaPlayer.OnBufferingUpdateListener,
            MediaPlayer.OnInfoListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnVideoSizeChangedListener {
        TvMediaPlayerListener mListener;

        public VitamioMediaPlayer(Context context) {
            super(context, true);
            setOnPreparedListener(this);
            setOnBufferingUpdateListener(this);
            setOnCompletionListener(this);
            setOnInfoListener(this);
            setOnErrorListener(this);
            setOnVideoSizeChangedListener(this);

            setOnHWRenderFailedListener(mMediaPlayerOnHWRenderFailedListener);
            setOnCachingUpdateListener(mMediaPlayerOnCachingUpdateListener);
            setOnSeekCompleteListener(mMediaPlayerOnSeekCompleteListener);

            setVideoChroma(mMediaPlayerVideoChroma);
        }

        @Override
        public void setListener(TvMediaPlayerListener listener) {
            mListener = listener;
        }

        @Override
        public void setMedia(MediaDetails details) throws IOException {
            setDataSource(details.getUri().toString());
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            if (mListener != null) {
                mListener.onPrepared();
            }
        }

        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            if (mListener != null) {
                mListener.onBufferingUpdate(percent);
            }
        }

        @Override
        public boolean onInfo(MediaPlayer mp, int what, int extra) {
            if (mListener != null) {
                return mListener.onInfo(what, extra);
            }
            return false;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (mListener != null) {
                mListener.onCompletion();
            }
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            if (mListener != null) {
                return mListener.onError(what, extra);
            }
            return false;
        }

        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            if (mListener != null) {
                mListener.onVideoSizeChanged(width, height);
            }
        }
    }

    public VitamioMediaPlayerView(Context context) {
        super(context);
    }

    public VitamioMediaPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VitamioMediaPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected TvMediaPlayer createMediaPlayer(Context context) {
        return new VitamioMediaPlayer(context);
    }
}