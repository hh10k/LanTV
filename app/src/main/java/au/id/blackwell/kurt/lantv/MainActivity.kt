package au.id.blackwell.kurt.lantv

import android.media.AudioManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView

import au.id.blackwell.kurt.lantv.player.TvPlayer
import au.id.blackwell.kurt.lantv.player.TvPlayerFactory
import au.id.blackwell.kurt.lantv.resolver.MediaResolver
import au.id.blackwell.kurt.lantv.resolver.MediaResolverFactory
import au.id.blackwell.kurt.lantv.utility.LimitedPool

class MainActivity : AppCompatActivity() {
    private var mPlayerType: String? = null
    private var mPlayer: TvPlayer? = null
    private var mPlayerFrame: ViewGroup? = null
    private var mPlayerStatus: TvPlayerStatusView? = null
    private val mWebViewPool = LimitedPool<WebView>()
    private val mMediaResolverFactory = MediaResolverFactory(mWebViewPool)
    private var mChannelIndex: Int = 0
    private val mPlayerFactory = TvPlayerFactory()

    companion object {
        private val TAG = "MainActivity"

        private val STATE_CHANNEL = "channel"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        volumeControlStream = AudioManager.STREAM_MUSIC

        mPlayerFrame = findViewById(R.id.player_frame) as ViewGroup
        mPlayerStatus = findViewById(R.id.player_status) as TvPlayerStatusView
        mWebViewPool.addItem(findViewById(R.id.worker_web_view) as WebView)

        // If we set this cookie then it'll allow us to use the CCTV HD page.
        // I'm not sure if the quality is actually any better.
        val cookieManager = CookieManager.getInstance()
        cookieManager.setCookie("http://tv.cctv.com/", "country_code=CN; path=/live")

        if (savedInstanceState != null) {
            mChannelIndex = savedInstanceState.getInt(STATE_CHANNEL, TvChannelConfiguration.DEFAULT_TV_CHANNEL_INDEX)
        } else {
            mChannelIndex = TvChannelConfiguration.DEFAULT_TV_CHANNEL_INDEX
        }
    }

    override fun onStart() {
        super.onStart()
        setTvChannel(mChannelIndex)
    }

    override fun onResume() {
        super.onResume()

        findViewById(android.R.id.content).systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        mPlayer?.play()
    }

    override fun onPause() {
        super.onPause()
        mPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        destroyPlayer()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putInt(STATE_CHANNEL, mChannelIndex)
        super.onSaveInstanceState(savedInstanceState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var handled = false

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                stepTvChannel(1)
                handled = true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                stepTvChannel(-1)
                handled = true
            }
        }

        return handled
    }

    private fun stepTvChannel(direction: Int) {
        var channelIndex = mChannelIndex
        val step = TvChannelConfiguration.TV_CHANNELS.size + direction

        do {
            channelIndex = (channelIndex + step) % TvChannelConfiguration.TV_CHANNELS.size
        } while (!TvChannelConfiguration.TV_CHANNELS[channelIndex].isPlayable && channelIndex != mChannelIndex)

        setTvChannel(channelIndex)
        mPlayer!!.play()
    }

    private fun setTvChannel(channelIndex: Int) {
        val channel = TvChannelConfiguration.TV_CHANNELS[channelIndex]

        // Restart playback if we had to recreate the player or the channel has changed
        if (createPlayer(channel.playerType!!) || channelIndex != mChannelIndex) {
            mChannelIndex = channelIndex
            val resolver = mMediaResolverFactory.create(channel.mediaResolveUri)
            mPlayerStatus!!.setTitle(channel.title)
            mPlayer!!.reset(resolver)
        }
    }

    private fun createPlayer(playerType: String): Boolean {
        if (playerType != mPlayerType) {
            destroyPlayer()
        }
        if (mPlayerType == null) {
            mPlayerType = playerType
            mPlayer = mPlayerFactory.create(playerType, this)
            mPlayer!!.setTvPlayerListener(mPlayerStatus)
            mPlayerFrame!!.addView(mPlayer!!.view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

            return true
        }

        return false
    }

    private fun destroyPlayer() {
        val player = mPlayer
        if (player != null) {
            player.stop()
            player.setTvPlayerListener(null)
            mPlayerFrame?.removeView(player.view)
            player.release()
            mPlayer = null
            mPlayerType = null
        }
    }
}
