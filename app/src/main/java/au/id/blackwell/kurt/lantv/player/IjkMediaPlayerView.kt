package au.id.blackwell.kurt.lantv.player

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder

import java.io.IOException

import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

internal class IjkMediaPlayerView : MediaPlayerView {
    companion object {
        private val TAG = "IjkMediaPlayerView"
    }

    internal inner class Wrapper(var mContext: Context) : TvMediaPlayer,
            IMediaPlayer.OnPreparedListener,
            IMediaPlayer.OnBufferingUpdateListener,
            IMediaPlayer.OnInfoListener,
            IMediaPlayer.OnCompletionListener,
            IMediaPlayer.OnErrorListener,
            IMediaPlayer.OnVideoSizeChangedListener {
        var mListener: TvMediaPlayerListener? = null
        var mPlayer: IjkMediaPlayer

        override fun isPlaying(): Boolean {
            return mPlayer.isPlaying
        }

        init {
            mPlayer = IjkMediaPlayer()
            mPlayer.setOnPreparedListener(this)
            mPlayer.setOnBufferingUpdateListener(this)
            mPlayer.setOnCompletionListener(this)
            mPlayer.setOnInfoListener(this)
            mPlayer.setOnErrorListener(this)
            mPlayer.setOnVideoSizeChangedListener(this)
        }

        override fun setListener(listener: TvMediaPlayerListener) {
            mListener = listener
        }

        @Throws(IOException::class)
        override fun setMedia(details: MediaDetails) {
            mPlayer.setDataSource(mContext, details.uri)
        }

        override fun setScreenOnWhilePlaying(onWhilePlaying: Boolean) {
            mPlayer.setScreenOnWhilePlaying(onWhilePlaying)
        }

        override fun setDisplay(holder: SurfaceHolder?) {
            mPlayer.setDisplay(holder)
        }

        override fun prepareAsync() {
            mPlayer.prepareAsync()
        }

        override fun release() {
            mPlayer.release()
        }

        override fun start() {
            mPlayer.start()
        }

        override fun pause() {
            mPlayer.pause()
        }

        override fun onPrepared(mp: IMediaPlayer) {
            mListener?.onPrepared()
        }

        override fun onBufferingUpdate(mp: IMediaPlayer, percent: Int) {
            mListener?.onBufferingUpdate(percent)
        }

        override fun onInfo(mp: IMediaPlayer, what: Int, extra: Int): Boolean {
            Log.d(TAG, String.format("info %d, %d", what, extra))

            var handled = false
            when (what) {
                IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                    mListener?.onPlaying()
                    handled = true
                }
            }
            return handled
        }

        override fun onCompletion(mp: IMediaPlayer) {
            mListener?.onCompletion()
        }

        override fun onError(mp: IMediaPlayer, what: Int, extra: Int): Boolean {
            return mListener?.onError(what, extra) ?: false
        }

        override fun onVideoSizeChanged(mp: IMediaPlayer, width: Int, height: Int, sar_num: Int, sar_den: Int) {
            // TODO: sar_num, sar_den
            mListener?.onVideoSizeChanged(width, height, width.toFloat() / height.toFloat())
        }
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {}

    override fun createMediaPlayer(context: Context): TvMediaPlayer {
        return Wrapper(context)
    }
}
