package au.id.blackwell.kurt.lantv;

import android.net.Uri;

public class TvChannel {
    private String mTitle;
    private Uri mMediaResolveUri;

    public TvChannel(String title, String mediaResolveUri) {
        this(title, Uri.parse(mediaResolveUri));
    }

    public TvChannel(String title, Uri mediaResolveUri) {
        mTitle = title;
        mMediaResolveUri = mediaResolveUri;
    }

    public String getTitle() {
        return mTitle;
    }

    public Uri getMediaResolveUri() {
        return mMediaResolveUri;
    }
}
