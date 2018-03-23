package au.id.blackwell.kurt.lantv.player

import au.id.blackwell.kurt.lantv.player.TvPlayerState

interface TvPlayerStatusListener {
    /**
     * Called when the player state has changed
     * @param state The current state.
     * @param progress A value from 0..1.  States which do not support progress will use 0.
     */
    fun onTvPlayerStateChanged(state: TvPlayerState, progress: Float)

    fun onTvPlayerFailed(reason: String)
}
