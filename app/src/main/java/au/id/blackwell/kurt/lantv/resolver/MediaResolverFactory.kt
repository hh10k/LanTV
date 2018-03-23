package au.id.blackwell.kurt.lantv.resolver

import android.net.Uri
import android.webkit.WebView

import au.id.blackwell.kurt.lantv.MediaDetails
import au.id.blackwell.kurt.lantv.utility.Pool

class MediaResolverFactory(internal var mWebViewPool: Pool<WebView>) {

    fun create(mediaResolveUri: Uri): MediaResolver {
        when (mediaResolveUri.scheme) {
            "cctv" -> return CctvMediaResolver(mWebViewPool, mediaResolveUri.toString().replaceFirst("^cctv://".toRegex(), "http://"))
            else -> return StaticMediaResolver(MediaDetails(mediaResolveUri))
        }
    }
}
