package au.id.blackwell.kurt.lantv;

/**
 * Used by the video player to find, authenticate, etc, the media to play.
 * The player may try to resolve a media URI multiple times in an attempt to recover from an error.
 */
public interface MediaResolver {
    /**
     * Request the details for the media.
     * @param callback
     */
    void resolve(Callback callback);

    /**
     * Cancel a resolve request for the given callback.
     * Implementations may still continue to do work but they may not call anything on the callback after this call.
     * @param callback
     */
    void cancel(Callback callback);

    public interface Callback {
        /**
         * Called whenever some progress is made in receiving the media details.
         * @param progress A value in the range [0, 1)
         */
        void onMediaResolverProgress(float progress);

        /**
         * Called once the media has been found, and is ready to be accessed.
         * This may be called immediately by MediaResolver.resolve or asynchronously.
         *
         * @param details Everything needed to play the media.
         */
        void onMediaResolved(MediaDetails details);
    }
}
