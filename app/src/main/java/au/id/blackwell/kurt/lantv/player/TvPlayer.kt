package au.id.blackwell.kurt.lantv.player

import android.view.View

import au.id.blackwell.kurt.lantv.resolver.MediaResolver

interface TvPlayer {

    /**
     * Get the view for this player.
     */
    val view: View

    /**
     * Reset player, and ready new media to load
     * @param mediaResolver
     */
    fun reset(mediaResolver: MediaResolver)

    /**
     * Resume playing the video
     */
    fun play()

    /**
     * Pause video, if playing
     */
    fun pause()

    /**
     * Stop playing the video.
     */
    fun stop()

    /**
     * Set a listener to be told of the playback status
     * @param listener
     */
    fun setTvPlayerListener(listener: TvPlayerStatusListener?)

    /**
     * Release resources from player
     */
    fun release()
}
