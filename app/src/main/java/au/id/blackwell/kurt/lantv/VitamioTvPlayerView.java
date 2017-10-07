package au.id.blackwell.kurt.lantv;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class VitamioTvPlayerView  extends RelativeLayout implements TvPlayer {

    private static final String TAG = "VitamioTvPlayerView";

    private TvPlayerListener mListener = null;
    private VideoView mVitamioVideoView = null;
    private MediaPlayer mMediaPlayer = null;
    private MediaDetails mMediaDetails = null;
    private MediaResolver mMediaResolver = null;

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

        mVitamioVideoView = (VideoView) findViewById(R.id.vitamio);
        mVitamioVideoView.setMediaController(new MediaController(getContext()));
        mVitamioVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                // optional need Vitamio 4.0
                mediaPlayer.setPlaybackSpeed(1.0f);
            }
        });
        mVitamioVideoView.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                notifyState(TvPlayerState.BUFFERING, (float)i / 100);
            }
        });
        mVitamioVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                Log.i(TAG, String.format("Error during playback: what=%d, extra=%d", what, extra));
                notifyState(TvPlayerState.FAILED, 0);
                return false;
            }
        });
    }

    @Override
    public void play(MediaResolver mediaResolver) {
        stop();

        mMediaResolver = mediaResolver;
        notifyState(TvPlayerState.RESOLVING, 0);
        mMediaResolver.resolve(mMediaResolverCallback);

    }

    @Override
    public void resume() {
        if (mMediaDetails != null) {
            notifyState(TvPlayerState.CONNECTING, 0);

            // RGBA video does not seem to be supported on the MoonBox and results in a sort of corrupted appearance.
            // TODO: Investigate whether this is because of a format mismatch, or whether we can detect the format of the device.
            mVitamioVideoView.setVideoChroma(MediaPlayer.VIDEOCHROMA_RGB565);

            // TODO: Check whether optional headers argument allows setting the user agent
            mVitamioVideoView.setVideoURI(mMediaDetails.getUri());
            mVitamioVideoView.requestFocus();
            mVitamioVideoView.start();
        } else if (mMediaResolver != null) {
            mMediaResolver.resolve(mMediaResolverCallback);
        }
    }

    @Override
    public void pause() {
        if (mMediaResolver != null) {
            mMediaResolver.cancel(mMediaResolverCallback);
        }
        mVitamioVideoView.pause();
    }

    @Override
    public void stop() {
        if (mMediaResolver != null) {
            mMediaResolver.cancel(mMediaResolverCallback);
            mMediaResolver = null;
        }
        mVitamioVideoView.stopPlayback();
        mVitamioVideoView.setVideoURI(null);
    }

    @Override
    public void setTvPlayerListener(TvPlayerListener listener) {
        mListener = listener;
    }

    private void notifyState(TvPlayerState state, float progress) {
        if (mListener == null) {
            return;
        }
        mListener.onTvPlayerStateChanged(state, progress);
    }
}