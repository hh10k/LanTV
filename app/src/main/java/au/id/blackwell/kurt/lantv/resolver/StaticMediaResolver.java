package au.id.blackwell.kurt.lantv.resolver;

import au.id.blackwell.kurt.lantv.MediaDetails;

/**
 * Immediately provides the media that it was constructed with.
 */
final class StaticMediaResolver implements MediaResolver {
    private MediaDetails mDetails;

    public StaticMediaResolver(MediaDetails details) {
        mDetails = details;
    }

    @Override
    public void resolve(Callback callback) {
        callback.onMediaResolved(mDetails);
    }

    @Override
    public void cancel(Callback callback) {
        // Nothing to cancel, this class is not asynchronous.
    }
}
