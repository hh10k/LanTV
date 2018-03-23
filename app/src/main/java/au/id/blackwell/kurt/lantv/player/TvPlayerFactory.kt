package au.id.blackwell.kurt.lantv.player

import android.content.Context

class TvPlayerFactory {

    fun create(playerType: String, context: Context): TvPlayer? {
        when (playerType) {
            ANDROID -> return AndroidMediaPlayerView(context)
            IJK -> return IjkMediaPlayerView(context)
            else -> return null
        }
    }

    companion object {
        val ANDROID = "android"
        val IJK = "ijk"
    }
}
