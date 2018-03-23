package au.id.blackwell.kurt.lantv.player

interface TvMediaPlayerListener {
    fun onPrepared()

    fun onBufferingUpdate(progress: Int)

    fun onError(what: Int, extra: Int): Boolean

    fun onVideoSizeChanged(width: Int, height: Int, aspectRatio: Float)

    fun onPlaying()

    fun onCompletion()
}
