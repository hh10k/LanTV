package au.id.blackwell.kurt.lantv;

import android.net.Uri;

public class MediaDetails {
    private Uri mUri;
    private String mUserAgent;

    public MediaDetails(Uri uri) {
        this(uri, null);
    }

    public MediaDetails(Uri uri, String userAgent) {
        mUri = uri;
        mUserAgent = userAgent;
    }

    public Uri getUri() {
        return mUri;
    }

    public String getUserAgent() {
        return mUserAgent;
    }
}
