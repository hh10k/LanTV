package au.id.blackwell.kurt.lantv.player;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

import au.id.blackwell.kurt.lantv.MediaDetails;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

class IjkMediaPlayerView extends MediaPlayerView {
    private static final String TAG = "IjkMediaPlayerView";

    class Wrapper implements
            TvMediaPlayer,
            IjkMediaPlayer.OnPreparedListener,
            IjkMediaPlayer.OnBufferingUpdateListener,
            IjkMediaPlayer.OnInfoListener,
            IjkMediaPlayer.OnCompletionListener,
            IjkMediaPlayer.OnErrorListener,
            IjkMediaPlayer.OnVideoSizeChangedListener {
        Context mContext;
        TvMediaPlayerListener mListener;
        IjkMediaPlayer mPlayer;

        public Wrapper(Context context) {
            mContext = context;
            mPlayer = new IjkMediaPlayer();
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnBufferingUpdateListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnInfoListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnVideoSizeChangedListener(this);
        }

        @Override
        public boolean isPlaying() {
            return mPlayer.isPlaying();
        }

        @Override
        public void setListener(TvMediaPlayerListener listener) {
            mListener = listener;
        }

        @Override
        public void setMedia(MediaDetails details) throws IOException {
            mPlayer.setDataSource(mContext, details.getUri());
        }

        @Override
        public void setScreenOnWhilePlaying(boolean onWhilePlaying) {
            mPlayer.setScreenOnWhilePlaying(onWhilePlaying);
        }

        @Override
        public void setDisplay(SurfaceHolder holder) {
            mPlayer.setDisplay(holder);
        }

        @Override
        public void prepareAsync() {
            mPlayer.prepareAsync();
        }

        @Override
        public void release() {
            mPlayer.release();
        }

        @Override
        public void start() {
            mPlayer.start();
        }

        @Override
        public void pause() {
            mPlayer.pause();
        }

        @Override
        public void onPrepared(IMediaPlayer mp) {
            if (mListener != null) {
                mListener.onPrepared();
            }
        }

        @Override
        public void onBufferingUpdate(IMediaPlayer mp, int percent) {
            if (mListener != null) {
                mListener.onBufferingUpdate(percent);
            }
        }

        @Override
        public boolean onInfo(IMediaPlayer mp, int what, int extra) {
            Log.d(TAG, String.format("info %d, %d", what, extra));

            boolean handled = false;
            if (mListener != null) {
                switch (what) {
                    case IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                        mListener.onPlaying();
                        handled = true;
                        break;
                }
            }
            return handled;
        }

        @Override
        public void onCompletion(IMediaPlayer mp) {
            if (mListener != null) {
                mListener.onCompletion();
            }
        }

        @Override
        public boolean onError(IMediaPlayer mp, int what, int extra) {
            if (mListener != null) {
                return mListener.onError(what, extra);
            }
            return false;
        }

        @Override
        public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
            if (mListener != null) {
                // TODO: sar_num, sar_den
                mListener.onVideoSizeChanged(width, height, (float)width / (float)height);
            }
        }
    }

    public IjkMediaPlayerView(Context context) {
        super(context);
    }

    public IjkMediaPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IjkMediaPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected TvMediaPlayer createMediaPlayer(Context context) {
        return new Wrapper(context);
    }
}
