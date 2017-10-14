package au.id.blackwell.kurt.lantv.player;

import android.view.View;

import au.id.blackwell.kurt.lantv.resolver.MediaResolver;

public interface TvPlayer {
    /**
     * Start playing the given video.
     */
    void play(MediaResolver mediaResolver);

    /**
     * Resume playing the video
     */
    void resume();

    /**
     * Pause video, if playing
     */
    void pause();

    /**
     * Stop playing the video.
     */
    void stop();

    /**
     * Set a listener to be told of the playback status
     * @param listener
     */
    void setTvPlayerListener(TvPlayerStatusListener listener);

    /**
     * Get the view for this player.
     */
    View getView();

    /**
     * Release resources from player
     */
    void release();
}
