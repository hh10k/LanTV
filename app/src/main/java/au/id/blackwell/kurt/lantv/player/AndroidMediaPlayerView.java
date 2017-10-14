package au.id.blackwell.kurt.lantv.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;

import java.io.IOException;

import au.id.blackwell.kurt.lantv.MediaDetails;

final class AndroidMediaPlayerView extends MediaPlayerView {
    private static final String TAG = "AndroidMediaPlayerView";

    class AndroidMediaPlayer extends MediaPlayer implements
            TvMediaPlayer,
            MediaPlayer.OnPreparedListener,
            MediaPlayer.OnBufferingUpdateListener,
            MediaPlayer.OnInfoListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnVideoSizeChangedListener {
        Context mContext;
        TvMediaPlayerListener mListener;

        public AndroidMediaPlayer(Context context) {
            mContext = context;
            setOnPreparedListener(this);
            setOnBufferingUpdateListener(this);
            setOnCompletionListener(this);
            setOnInfoListener(this);
            setOnErrorListener(this);
            setOnVideoSizeChangedListener(this);
        }

        @Override
        public void setListener(TvMediaPlayerListener listener) {
            mListener = listener;
        }

        @Override
        public void setMedia(MediaDetails details) throws IOException {
            setDataSource(mContext, details.getUri());
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
            Log.d(TAG, String.format("info %d, %d", what, extra));

            boolean handled = false;
            if (mListener != null) {
                switch (what) {
                    case MEDIA_INFO_VIDEO_RENDERING_START:
                        mListener.onPlaying();
                        handled = true;
                        break;
                }
            }
            return handled;
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
                // Assumes pixel size is always square
                mListener.onVideoSizeChanged(width, height, (float)width / (float)height);
            }
        }
    }

    public AndroidMediaPlayerView(Context context) {
        super(context);
    }

    public AndroidMediaPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AndroidMediaPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected TvMediaPlayer createMediaPlayer(Context context) {
        return new AndroidMediaPlayer(context);
    }
}