package au.id.blackwell.kurt.lantv;

/**
 * Immediately provides the media that it was constructed with.
 */
final class StaticMediaResolver implements IMediaResolver {
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
