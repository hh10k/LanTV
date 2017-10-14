package au.id.blackwell.kurt.lantv.player;

import au.id.blackwell.kurt.lantv.player.TvPlayerState;

public interface TvPlayerStatusListener {
    /**
     * Called when the player state has changed
     * @param state The current state.
     * @param progress A value from 0..1.  States which do not support progress will use 0.
     */
    void onTvPlayerStateChanged(TvPlayerState state, float progress);

    void onTvPlayerFailed(String reason);
}
