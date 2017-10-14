package au.id.blackwell.kurt.lantv.resolver;

import android.net.Uri;
import android.webkit.WebView;

import au.id.blackwell.kurt.lantv.MediaDetails;
import au.id.blackwell.kurt.lantv.utility.Pool;

public final class MediaResolverFactory {
    Pool<WebView> mWebViewPool;

    public MediaResolverFactory(Pool<WebView> webViewPool) {
        mWebViewPool = webViewPool;
    }

    public MediaResolver create(Uri mediaResolveUri) {
        switch (mediaResolveUri.getScheme()) {
            case "cctv":
                return new CctvMediaResolver(mWebViewPool, mediaResolveUri.toString().replaceFirst("^cctv://", "http://"));
            default:
                return new StaticMediaResolver(new MediaDetails(mediaResolveUri));
        }
    }
}
