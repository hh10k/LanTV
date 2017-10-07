package au.id.blackwell.kurt.lantv;

import android.net.Uri;

class TvChannel {
    private String mId;
    private String mTitle;
    private String mPlayer;
    private Uri mMediaResolveUri;

    public TvChannel(String id, String title, String player, String mediaResolveUri) {
        this(id, title, player, Uri.parse(mediaResolveUri));
    }

    public TvChannel(String id, String title, String player, Uri mediaResolveUri) {
        mId = id;
        mTitle = title;
        mPlayer = player;
        mMediaResolveUri = mediaResolveUri;
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getPlayerType() { return mPlayer; }

    public Uri getMediaResolveUri() {
        return mMediaResolveUri;
    }

    public boolean isPlayable() {
        return mPlayer != null && mMediaResolveUri != null;
    }
}
