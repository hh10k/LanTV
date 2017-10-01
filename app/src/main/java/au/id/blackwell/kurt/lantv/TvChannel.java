package au.id.blackwell.kurt.lantv;

import android.net.Uri;

public class TvChannel {
    private String mId;
    private String mTitle;
    private Uri mMediaResolveUri;

    public TvChannel(String id, String title, String mediaResolveUri) {
        this(id, title, Uri.parse(mediaResolveUri));
    }

    public TvChannel(String id, String title, Uri mediaResolveUri) {
        mId = id;
        mTitle = title;
        mMediaResolveUri = mediaResolveUri;
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public Uri getMediaResolveUri() {
        return mMediaResolveUri;
    }
}
