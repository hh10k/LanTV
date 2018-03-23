package au.id.blackwell.kurt.lantv.resolver

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

import org.json.JSONException
import org.json.JSONObject

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList

import au.id.blackwell.kurt.lantv.MediaDetails
import au.id.blackwell.kurt.lantv.utility.NumberUtility
import au.id.blackwell.kurt.lantv.utility.Pool

internal class CctvMediaResolver(private val mWebViewPool: Pool<WebView>, private val mUrl: String) : MediaResolver {
    companion object {
        private val TAG = "CctvMediaResolver"

        // The maximum number of millseconds to wait after the page has finished
        // loading before we give up looking for the video URL to appear.
        private val PAGE_FINISHED_LOADING_TIMEOUT = 10000

        // How often to poll for the appearance of the video in the page, in milliseconds
        private val FIND_VIDEO_INTERVAL: Long = 500
    }

    private var mWebView: Pool.Item<WebView>? = null
    private var mCallbacks = ArrayList<MediaResolver.Callback>()
    private var mResolveState = ResolveState.IDLE
    private var mPageState = PageState.NONE
    private var mPageStateTimestamp: Long = 0
    private var mPageLoadCount = 0
    private val mHandler = Handler()

    private val mWebViewPoolCallback = { item: Pool.Item<WebView> ->
        mWebView = item
        enterStartState()
    }

    private val mWebViewClient = object : WebViewClient() {
        private var mCurrentUrl: String? = null

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Log.d(TAG, "Page: Navigating to " + url)
            if (mCurrentUrl != url) {
                mCurrentUrl = url
                ++mPageLoadCount
            }
            return false
        }

        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return shouldOverrideUrlLoading(view, request.url.toString())
        }

        private fun shouldInterceptRequest(view: WebView, uri: Uri): WebResourceResponse? {
            val host = uri.host

            val response: WebResourceResponse?
            if ("pic.fastapi.net" == host
                    || "p1.img.cctvpic.com" == host
                    || "bdimg.share.baidu.com" == host) {
                Log.d(TAG, "Page: Blocked " + uri.toString())
                val data = ByteArrayInputStream("".toByteArray(StandardCharsets.UTF_8))
                response = WebResourceResponse("text/plain", StandardCharsets.UTF_8.name(), data)
            } else {
                Log.d(TAG, "Page: Requesting " + uri.toString())
                response = null
            }
            return response
        }

        override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
            return shouldInterceptRequest(view, Uri.parse(url))
        }

        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            return shouldInterceptRequest(view, request.url)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            Log.d(TAG, "Page: Started " + url)
            mCurrentUrl = url
            ++mPageLoadCount
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            if (url == mCurrentUrl) {
                Log.d(TAG, "Page: Finished " + url)
                setPageState(PageState.FINISHED)
                update()
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            super.onReceivedError(view, request, error)
            Log.d(TAG, "Page: Error")
            setPageState(PageState.ERROR)
            update()
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            super.onReceivedHttpError(view, request, errorResponse)
            Log.d(TAG, "Page: HTTP Error")
            setPageState(PageState.ERROR)
            update()
        }
    }

    private val mWebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            val totalProgress = if (mPageLoadCount == 0) 0f
                else NumberUtility.getProgressForUnboundedStages(mPageLoadCount, newProgress.toFloat())

            for (callback in mCallbacks) {
                callback.onMediaResolverProgress(totalProgress)
            }
        }
    }

    private val mFindVideoRunnable = Runnable { enterFindingState() }

    internal enum class ResolveState {
        IDLE,
        QUEUED,
        START,
        LOADING,
        FINDING
    }

    internal enum class PageState {
        NONE,
        LOADING,
        FINISHED,
        ERROR
    }

    override fun resolve(callback: MediaResolver.Callback) {
        Log.d(TAG, "Adding callback")
        mCallbacks.add(callback)
        update()
    }

    override fun cancel(callback: MediaResolver.Callback) {
        Log.d(TAG, "Removing callback")
        mCallbacks.remove(callback)
        update()
    }

    private fun onResolved(details: MediaDetails?) {
        Log.i(TAG, "Resolved " + (details?.uri ?: "nothing"))

        // Swap out callbacks so it's not possible to modify the array while dispatching.
        val callbacks = mCallbacks
        mCallbacks = ArrayList()

        enterIdleState()

        for (callback in callbacks) {
            callback.onMediaResolved(details)
        }
    }

    private fun setPageState(state: PageState) {
        mPageState = state
        mPageStateTimestamp = System.currentTimeMillis()
    }

    private fun enterIdleState() {
        // We can enter this state many ways.  Reset everything.
        Log.i(TAG, "Idle")

        mHandler.removeCallbacks(mFindVideoRunnable)
        mWebViewPool.cancel(mWebViewPoolCallback)
        if (mWebView != null) {
            val web = mWebView!!.get()
            web.webViewClient = WebViewClient()
            web.webChromeClient = WebChromeClient()
            web.loadUrl("about:blank")
            web.clearHistory()
            mWebView!!.release()
            mWebView = null
            setPageState(PageState.NONE)
        }

        mResolveState = ResolveState.IDLE
    }

    private fun enterQueuedState() {
        Log.d(TAG, "Getting WebView")
        mResolveState = ResolveState.QUEUED
        mWebViewPool.request(mWebViewPoolCallback)
    }

    private fun enterStartState() {
        Log.d(TAG, "Starting")
        mResolveState = ResolveState.START
        update()
    }

    private fun enterLoadingState() {
        if (mPageState == PageState.LOADING || mPageState == PageState.FINISHED && System.currentTimeMillis() - mPageStateTimestamp < PAGE_FINISHED_LOADING_TIMEOUT) {
            // Continue looking for the video URL.
            mHandler.postDelayed(mFindVideoRunnable, FIND_VIDEO_INTERVAL)
            mResolveState = ResolveState.LOADING
            update()
        } else {
            // The page is finished or failed, we don't want to do LOADING anymore.
            Log.d(TAG, "Page state is " + mPageState.toString() + ", loading stopped")
            onResolved(null)
        }
    }

    private fun enterFindingState() {
        Log.d(TAG, "Finding <video>")
        mResolveState = ResolveState.FINDING

        val web = mWebView!!.get()
        val javaScript = "(function() {\n" +
                "  var videos = document.getElementsByTagName('video');\n" +
                "  var src = (videos.html5Player) ? videos.html5Player.src\n" +
                "    : (videos.length > 0) ? videos[0].src\n" +
                "    : undefined;\n" +
                "  return { src: src }\n" +
                "})();"
        web.evaluateJavascript(javaScript) { json ->
            var url: String? = null
            try {
                val reader = JSONObject(json)
                url = reader.optString("src")
            } catch (e: JSONException) {
                // The page is loading.  It's ok if this fails sometimes.
            }

            if (url == null || url.isEmpty()) {
                enterLoadingState()
            } else {
                // We have it!
                val media = MediaDetails(Uri.parse(url))
                onResolved(media)
            }
        }
    }

    private fun update() {
        if (mResolveState != ResolveState.IDLE && mCallbacks.isEmpty()) {
            // There are no longer any requesters.
            enterIdleState()
        }

        when (mResolveState) {
            CctvMediaResolver.ResolveState.IDLE -> if (!mCallbacks.isEmpty()) {
                enterQueuedState()
            }
            CctvMediaResolver.ResolveState.QUEUED -> {
                // Waiting for a WebView to become available
            }
            CctvMediaResolver.ResolveState.START -> {
                setPageState(PageState.LOADING)
                mPageLoadCount = 0
                val web = mWebView!!.get()
                val settings = web.settings
                //settings.setUserAgentString("UCWEB/2.0 (iPad; U; CPU OS 7_1 like Mac OS X; en; iPad3,6) U2/1.0.0 UCBrowser/9.3.1.344");
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Mobile Safari/537.36"
                settings.javaScriptEnabled = true
                settings.loadsImagesAutomatically = false
                web.webViewClient = mWebViewClient
                web.webChromeClient = mWebChromeClient
                web.loadUrl(mUrl)

                enterLoadingState()
            }
            CctvMediaResolver.ResolveState.LOADING -> {
                // Polling intermittently to see if the video is there.
            }
            CctvMediaResolver.ResolveState.FINDING -> {
                // Waiting for JavaScript to complete
            }
        }
    } 
}
