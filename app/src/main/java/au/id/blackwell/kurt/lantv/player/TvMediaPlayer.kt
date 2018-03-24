package au.id.blackwell.kurt.lantv.player

import android.view.SurfaceHolder

import java.io.IOException

interface TvMediaPlayer {
    fun isPlaying(): Boolean
    fun setListener(listener: TvMediaPlayerListener)
    @Throws(IOException::class)
    fun setMedia(details: MediaDetails)

    fun setScreenOnWhilePlaying(onWhilePlaying: Boolean)
    fun setDisplay(holder: SurfaceHolder?)
    fun prepareAsync()
    fun release()
    fun start()
    fun pause()
}
