package au.id.blackwell.kurt.lantv;

import android.net.Uri;

public final class MediaDetails {
    public static final String DEFAULT_USER_AGENT_NAME = "LanTV";
    public static final String DEFAULT_HTTP_USER_AGENT = "LanTV";

    private Uri mUri;
    private String mUserAgentName;
    private String mHttpUserAgent;

    public MediaDetails(Uri uri) {
        this(uri, DEFAULT_USER_AGENT_NAME, DEFAULT_HTTP_USER_AGENT);
    }

    public MediaDetails(Uri uri, String userAgentName, String httpUserAgent) {
        mUri = uri;
        mUserAgentName = userAgentName;
        mHttpUserAgent = httpUserAgent;
    }

    public Uri getUri() {
        return mUri;
    }

    public String getUserAgentName() {
        // TODO: What does VLC do with this?
        return mUserAgentName;
    }

    public String getHttpUserAgent() {
        return mHttpUserAgent;
    }
}
