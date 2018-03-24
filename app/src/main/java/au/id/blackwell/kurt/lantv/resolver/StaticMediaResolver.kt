package au.id.blackwell.kurt.lantv.resolver

import au.id.blackwell.kurt.lantv.player.MediaDetails

/**
 * Immediately provides the media that it was constructed with.
 */
internal class StaticMediaResolver(private val mDetails: MediaDetails) : MediaResolver {

    override fun resolve(callback: MediaResolver.Callback) {
        callback.onMediaResolved(mDetails)
    }

    override fun cancel(callback: MediaResolver.Callback) {
        // Nothing to cancel, this class is not asynchronous.
    }
}
