package au.id.blackwell.kurt.lantv.player

import android.content.Context
import android.graphics.PixelFormat
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout

import java.io.IOException

import au.id.blackwell.kurt.lantv.resolver.MediaResolver
import au.id.blackwell.kurt.lantv.R

abstract class MediaPlayerView : FrameLayout, TvPlayer {
    companion object {
        private val TAG = "TvMediaPlayerView"

        // RGBA video does not seem to be supported on the MoonBox and results in a sort of corrupted appearance.
        // TODO: Investigate whether this is because of a format mismatch, or whether we can detect the format of the device.
        protected val mTargetSurfaceFormat = PixelFormat.RGB_565
        //protected static final int mTargetSurfaceFormat = PixelFormat.RGBA_8888;
    }

    private var mListener: TvPlayerStatusListener? = null
    private var mSurfaceView: SurfaceView? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mSurfaceWidth = 0
    private var mSurfaceHeight = 0
    private var mSurfaceFormat = 0
    private var mLayoutWidth = 0
    private var mLayoutHeight = 0
    private var mSurfaceCreated = false
    private var mMediaPlayer: TvMediaPlayer? = null
    private var mMediaPlayerState = MediaPlayerState.NONE
    private var mMediaDetails: MediaDetails? = null
    private var mMediaDetailsState = MediaDetailsState.UNRESOLVED
    private var mMediaResolver: MediaResolver? = null
    private var mPlayState = PlayState.STOPPED
    private var mVideoWidth = 0
    private var mVideoHeight = 0
    private var mVideoAspectRatio = 0f

    private val mMediaResolverCallback = object : MediaResolver.Callback {
        override fun onMediaResolved(details: MediaDetails?) {
            mMediaDetails = details
            mMediaDetailsState = MediaDetailsState.RESOLVED

            if (mMediaDetails != null) {
                Log.d(TAG, "Got media details")
                onChangeState(TvPlayerState.RESOLVING, 1f)
            } else {
                Log.d(TAG, "Failed to get media details")
                onFailureState(context.getString(R.string.status_failed_connection))
            }

            update()
        }

        override fun onMediaResolverProgress(progress: Float) {
            onChangeState(TvPlayerState.RESOLVING, progress)
        }
    }

    private val mSurfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            mSurfaceCreated = true
            Log.d(TAG, "Surface created")
            update()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            Log.d(TAG, String.format("Surface resized (%d, %d) format %d", width, height, format))
            mSurfaceWidth = width
            mSurfaceHeight = height
            mSurfaceFormat = format
            update()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            mSurfaceCreated = false
            mSurfaceWidth = 0
            mSurfaceHeight = 0
            mSurfaceFormat = 0
        }
    }

    private val mMediaPlayerListener = object : TvMediaPlayerListener {
        override fun onPrepared() {
            Log.d(TAG, "MediaPlayer prepared")
            mMediaPlayerState = MediaPlayerState.PREPARED
            update()
        }

        override fun onBufferingUpdate(progress: Int) {
            if (mMediaPlayer!!.isPlaying()) {
                onChangeState(TvPlayerState.PLAYING, 0f)
            } else {
                onChangeState(TvPlayerState.BUFFERING, progress.toFloat() / 100)
            }
        }

        override fun onError(what: Int, extra: Int): Boolean {
            Log.i(TAG, String.format("Error during playback: what=%d, extra=%d", what, extra))
            mMediaPlayerState = MediaPlayerState.ERROR
            onFailureState(context.getString(R.string.status_failed_playback))
            return false
        }

        override fun onVideoSizeChanged(width: Int, height: Int, aspectRatio: Float) {
            if (mVideoWidth != width || mVideoHeight != height || mVideoAspectRatio != aspectRatio) {
                Log.i(TAG, String.format("Video dimensions changed: (%d x %d)", width, height))
                mVideoWidth = width
                mVideoHeight = height
                mVideoAspectRatio = aspectRatio
                update()
            }
        }

        override fun onPlaying() {
            onChangeState(TvPlayerState.PLAYING, 0f)
        }

        override fun onCompletion() {
            mMediaPlayerState = MediaPlayerState.PLAYBACK_COMPLETE
            onChangeState(TvPlayerState.STOPPED, 0f)
            Log.d(TAG, "Playback complete")
        }
    }

    override val view: View
        get() = this

    internal enum class MediaDetailsState {
        UNRESOLVED,
        RESOLVING,
        RESOLVED
    }

    internal enum class MediaPlayerState {
        NONE,
        IDLE,
        INITIALISED,
        PREPARING,
        PREPARED,
        STARTED,
        PAUSED,
        PLAYBACK_COMPLETE,
        ERROR
    }

    internal enum class PlayState {
        STOPPED,
        PLAYING,
        PAUSED
    }

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView(context)
    }

    private fun initView(context: Context) {
        setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.black))
    }

    protected abstract fun createMediaPlayer(context: Context): TvMediaPlayer

    private fun initMediaDetails(): Boolean {
        if (mMediaDetailsState == MediaDetailsState.UNRESOLVED) {
            if (mMediaResolver != null) {
                Log.d(TAG, "Resolving media")
                mMediaDetailsState = MediaDetailsState.RESOLVING
                onChangeState(TvPlayerState.RESOLVING, 0f)
                mMediaResolver!!.resolve(mMediaResolverCallback)
            } else {
                Log.d(TAG, "Can't yet resolve media, nothing to play.")
                mPlayState = PlayState.STOPPED
            }
        }

        return mMediaDetails != null
    }

    private fun deinitMediaDetails() {
        deinitMediaPlayer()

        if (mMediaDetailsState == MediaDetailsState.RESOLVING) {
            Log.d(TAG, "Media request cancelled")
            mMediaResolver!!.cancel(mMediaResolverCallback)
        } else if (mMediaDetailsState == MediaDetailsState.RESOLVED) {
            Log.d(TAG, "Media details forgotten")
            mMediaDetails = null
        }
        mMediaDetailsState = MediaDetailsState.UNRESOLVED
    }

    private fun initSurface(): Boolean {
        if (mSurfaceView == null) {
            mSurfaceView = SurfaceView(context)
            mSurfaceHolder = mSurfaceView!!.holder
            mSurfaceHolder!!.addCallback(mSurfaceHolderCallback)
            mSurfaceHolder!!.setFormat(mTargetSurfaceFormat)
            val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            params.gravity = Gravity.CENTER
            addView(mSurfaceView, params)
        }

        if (!mSurfaceCreated) {
            // Nothing to draw to yet
            Log.d(TAG, "Waiting for surface to be created")
        }

        return mSurfaceCreated
    }

    private fun deinitSurface() {
        deinitMediaPlayer()

        if (mSurfaceView != null) {
            Log.d(TAG, "Surface destroyed")
            removeView(mSurfaceView)
            mSurfaceView = null
            mSurfaceHolder!!.removeCallback(mSurfaceHolderCallback)
            mSurfaceHolder = null
        }
    }

    private fun initMediaPlayer(): Boolean {
        if (mMediaPlayerState == MediaPlayerState.NONE) {
            Log.d(TAG, "Creating MediaPlayer")
            mMediaPlayer = createMediaPlayer(context)
            mMediaPlayer!!.setListener(mMediaPlayerListener)

            mMediaPlayerState = MediaPlayerState.IDLE
        }

        if (mMediaPlayerState == MediaPlayerState.IDLE
                && initMediaDetails()
                && initSurface()) {
            Log.d(TAG, "Initialising MediaPlayer")
            try {
                mMediaPlayer!!.setMedia(mMediaDetails!!)
                mMediaPlayerState = MediaPlayerState.INITIALISED
            } catch (e: IOException) {
                onChangeState(TvPlayerState.FAILED, 0f)
                mMediaPlayerState = MediaPlayerState.ERROR
            }

        }

        if (mMediaPlayerState == MediaPlayerState.INITIALISED) {
            Log.d(TAG, "Preparing MediaPlayer")
            mMediaPlayerState = MediaPlayerState.PREPARING
            mMediaPlayer!!.setDisplay(mSurfaceHolder)
            mMediaPlayer!!.setScreenOnWhilePlaying(true)
            mMediaPlayer!!.prepareAsync()
        }

        return mMediaPlayerState == MediaPlayerState.PREPARED
    }

    private fun deinitMediaPlayer() {
        if (mMediaPlayer != null) {
            Log.d(TAG, "Releasing MediaPlayer")
            mMediaPlayer!!.setDisplay(null)
            mMediaPlayer!!.release()
            mMediaPlayer = null
            mMediaPlayerState = MediaPlayerState.NONE
        }
    }

    private fun initLayout(): Boolean {
        if (!initMediaPlayer()) {
            return false
        }

        val videoSizeKnown = mVideoWidth != 0 && mVideoHeight != 0
        var surfaceReady = false

        if (!videoSizeKnown) {
            Log.d(TAG, "Waiting for video size")
        } else {

            val viewWidth = width
            val viewHeight = height
            var fitWidth = viewWidth
            var fitHeight = (fitWidth / mVideoAspectRatio).toInt()
            if (fitHeight > viewHeight) {
                fitHeight = viewHeight
                fitWidth = (fitHeight * mVideoAspectRatio).toInt()
            }

            if (fitWidth != mLayoutWidth || fitHeight != mLayoutHeight) {
                Log.d(TAG, String.format("Layout change from %dx%d to %dx%d", mLayoutWidth, mLayoutHeight, fitWidth, fitHeight))
                mLayoutWidth = fitWidth
                mLayoutHeight = fitHeight

                val lp = mSurfaceView!!.layoutParams as FrameLayout.LayoutParams
                lp.width = mLayoutWidth
                lp.height = mLayoutHeight
                mSurfaceView!!.layoutParams = lp
                mSurfaceHolder!!.setFixedSize(mLayoutWidth, mLayoutHeight)
            }

            surfaceReady = (mSurfaceFormat == mTargetSurfaceFormat
                    && mSurfaceWidth == mLayoutWidth
                    && mSurfaceHeight == mLayoutHeight)

            if (!surfaceReady) {
                Log.d(TAG, "Waiting for surface change")
            }
        }

        return videoSizeKnown && surfaceReady
    }

    private fun update() {
        when (mPlayState) {
            MediaPlayerView.PlayState.PLAYING -> if (initLayout() && mMediaPlayerState != MediaPlayerState.STARTED) {
                Log.d(TAG, "Starting to play video")
                mMediaPlayerState = MediaPlayerState.STARTED
                onChangeState(TvPlayerState.CONNECTING, 0f)
                try {
                    mMediaPlayer!!.start()
                } catch (error: Throwable) {
                    Log.e(TAG, "Error when attempting to start media player", error)
                    mMediaPlayerState = MediaPlayerState.ERROR
                    throw error
                }

            }
            MediaPlayerView.PlayState.PAUSED -> if (mMediaPlayerState == MediaPlayerState.STARTED) {
                Log.d(TAG, "Pausing video")
                mMediaPlayerState = MediaPlayerState.PAUSED
                mMediaPlayer!!.pause()
            } else if (mMediaDetailsState == MediaDetailsState.RESOLVING) {
                deinitMediaDetails()
            }
            MediaPlayerView.PlayState.STOPPED -> {
                deinitSurface()
                deinitMediaDetails()
                mMediaResolver = null
                mMediaDetails = null
            }
        }
    }

    override fun reset(mediaResolver: MediaResolver) {
        stop()
        mMediaResolver = mediaResolver
    }

    override fun play() {
        mPlayState = PlayState.PLAYING
        update()
    }

    override fun pause() {
        mPlayState = PlayState.PAUSED
        update()
    }

    override fun stop() {
        mPlayState = PlayState.STOPPED
        update()
    }

    override fun setTvPlayerListener(listener: TvPlayerStatusListener?) {
        mListener = listener
    }

    private fun onChangeState(state: TvPlayerState, progress: Float) {
        if (mListener == null) {
            return
        }
        mListener!!.onTvPlayerStateChanged(state, progress)
    }

    private fun onFailureState(reason: String) {
        if (mListener == null) {
            return
        }
        mListener!!.onTvPlayerFailed(reason)
    }

    override fun release() {
        stop()
    }
}
