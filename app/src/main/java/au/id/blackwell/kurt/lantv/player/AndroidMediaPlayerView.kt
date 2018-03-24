package au.id.blackwell.kurt.lantv.player

import android.content.Context
import android.media.MediaPlayer
import android.util.AttributeSet
import android.util.Log

import java.io.IOException

internal class AndroidMediaPlayerView : MediaPlayerView {

    internal inner class AndroidMediaPlayer(var mContext: Context) : MediaPlayer(),
            TvMediaPlayer,
            MediaPlayer.OnPreparedListener,
            MediaPlayer.OnBufferingUpdateListener,
            MediaPlayer.OnInfoListener,
            MediaPlayer.OnCompletionListener,
            MediaPlayer.OnErrorListener,
            MediaPlayer.OnVideoSizeChangedListener {
        var mListener: TvMediaPlayerListener? = null

        init {
            setOnPreparedListener(this)
            setOnBufferingUpdateListener(this)
            setOnCompletionListener(this)
            setOnInfoListener(this)
            setOnErrorListener(this)
            setOnVideoSizeChangedListener(this)
        }

        override fun setListener(listener: TvMediaPlayerListener) {
            mListener = listener
        }

        @Throws(IOException::class)
        override fun setMedia(details: MediaDetails) {
            setDataSource(mContext, details.uri)
        }

        override fun onPrepared(mp: MediaPlayer) {
            if (mListener != null) {
                mListener!!.onPrepared()
            }
        }

        override fun onBufferingUpdate(mp: MediaPlayer, percent: Int) {
            if (mListener != null) {
                mListener!!.onBufferingUpdate(percent)
            }
        }

        override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            Log.d(TAG, String.format("info %d, %d", what, extra))

            var handled = false
            if (mListener != null) {
                when (what) {
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                        mListener!!.onPlaying()
                        handled = true
                    }
                }
            }
            return handled
        }

        override fun onCompletion(mp: MediaPlayer) {
            if (mListener != null) {
                mListener!!.onCompletion()
            }
        }

        override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {
            return if (mListener != null) {
                mListener!!.onError(what, extra)
            } else false
        }

        override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
            if (mListener != null) {
                // Assumes pixel size is always square
                mListener!!.onVideoSizeChanged(width, height, width.toFloat() / height.toFloat())
            }
        }
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {}

    override fun createMediaPlayer(context: Context): TvMediaPlayer {
        return AndroidMediaPlayer(context)
    }

    companion object {
        private val TAG = "AndroidMediaPlayerView"
    }
}
