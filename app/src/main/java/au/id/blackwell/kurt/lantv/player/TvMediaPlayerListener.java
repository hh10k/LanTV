package au.id.blackwell.kurt.lantv.player;

interface TvMediaPlayerListener {
    void onPrepared();

    void onBufferingUpdate(int progress);

    boolean onError(int what, int extra);

    void onVideoSizeChanged(int width, int height);

    boolean onInfo(int what, int extra);

    void onCompletion();
}
