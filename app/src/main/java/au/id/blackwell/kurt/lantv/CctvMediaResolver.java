package au.id.blackwell.kurt.lantv;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

final class CctvMediaResolver implements IMediaResolver {
    private static final String TAG = "CctvMediaResolver";

    enum ResolveState {
        IDLE,
        QUEUED,
        START,
        LOADING,
        FINDING,
    };

    enum PageState {
        NONE,
        LOADING,
        FINISHED,
        ERROR,
    };

    // How often to poll for the appearance of the video in the page, in milliseconds
    private static final long FIND_VIDEO_INTERVAL = 500;

    private String mUrl;
    private IPool<WebView> mWebViewPool;
    private IPool.Item<WebView> mWebView;
    private ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
    private ResolveState mResolveState = ResolveState.IDLE;
    private PageState mPageState = PageState.NONE;
    private final Handler mHandler = new Handler();

    private final IPool.Callback mWebViewPoolCallback = new IPool.Callback() {
        @Override
        public void run(IPool.Item item) {
            mWebView = item;
            enterStartState();
        }
    };

    private final WebViewClient mWebViewClient = new WebViewClient() {
        private String mCurrentUrl = null;

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Page: Navigating to " + url);
            mCurrentUrl = url;
            return false;
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrlLoading(view, request.getUrl().toString());
        }

        private WebResourceResponse shouldInterceptRequest(WebView view, Uri uri) {
            String host = uri.getHost();

            WebResourceResponse response;
            if ("pic.fastapi.net".equals(host)
                || "p1.img.cctvpic.com".equals(host)
                || "bdimg.share.baidu.com".equals(host)) {
                Log.d(TAG, "Page: Blocked " + uri.toString());
                ByteArrayInputStream data = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
                response = new WebResourceResponse("text/plain", StandardCharsets.UTF_8.name(), data);
            } else {
                Log.d(TAG, "Page: Requesting " + uri.toString());
                response = null;
            }
            return response;
        }

        @SuppressWarnings("deprecation")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return shouldInterceptRequest(view, Uri.parse(url));
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return shouldInterceptRequest(view, request.getUrl());
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "Page: Started " + url);
            mCurrentUrl = url;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (mCurrentUrl == url) {
                Log.d(TAG, "Page: Finished " + url);
                mPageState = PageState.FINISHED;
                update();
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            Log.d(TAG, "Page: Error");
            mPageState = PageState.ERROR;
            update();
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            Log.d(TAG, "Page: HTTP Error");
            mPageState = PageState.ERROR;
            update();
        }
    };

    private final Runnable mFindVideoRunnable = new Runnable() {
        @Override
        public void run() {
            enterFindingState();
        }
    };

    public CctvMediaResolver(IPool<WebView> webViewPool, String url) {
        mWebViewPool = webViewPool;
        mUrl = url;
    }

    @Override
    public void resolve(Callback callback) {
        Log.d(TAG, "Adding callback");
        mCallbacks.add(callback);
        update();
    }

    @Override
    public void cancel(Callback callback) {
        Log.d(TAG, "Removing callback");
        mCallbacks.remove(callback);
        update();
    }

    private void onResolved(MediaDetails details) {
        Log.i(TAG, "Resolved " + (details != null ? details.getUri() : "nothing"));

        // Swap out callbacks so it's not possible to modify the array while dispatching.
        ArrayList<Callback> callbacks = mCallbacks;
        mCallbacks = new ArrayList<Callback>();

        enterIdleState();

        for (Callback callback : callbacks) {
            callback.onMediaResolved(details);
        }
    }

    private void enterIdleState() {
        // We can enter this state many ways.  Reset everything.
        Log.i(TAG, "Idle");

        mHandler.removeCallbacks(mFindVideoRunnable);
        mWebViewPool.cancel(mWebViewPoolCallback);
        if (mWebView != null) {
            WebView web = mWebView.get();
            web.setWebViewClient(new WebViewClient());
            web.loadUrl("about:blank");
            web.clearHistory();
            mWebView.release();
            mWebView = null;
            mPageState = PageState.NONE;
        }

        mResolveState = ResolveState.IDLE;
    }

    private void enterQueuedState() {
        Log.d(TAG, "Getting WebView");
        mResolveState = ResolveState.QUEUED;
        mWebViewPool.request(mWebViewPoolCallback);
    }

    private void enterStartState() {
        Log.d(TAG, "Starting");
        mResolveState = ResolveState.START;
        update();
    }

    private void enterLoadingState() {
        if (mPageState != PageState.LOADING) {
            // The page is finished or failed, we don't want to do LOADING anymore.
            Log.d(TAG, "Page state is " + mPageState.toString() + ", loading stopped");
            onResolved(null);
        } else {
            mHandler.postDelayed(mFindVideoRunnable, FIND_VIDEO_INTERVAL);
            mResolveState = ResolveState.LOADING;
            update();
        }
    }

    private void enterFindingState() {
        Log.d(TAG, "Finding <video>");
        mResolveState = ResolveState.FINDING;

        WebView web = mWebView.get();
        String javaScript =
            "(function() {\n" +
            "  var videos = document.getElementsByTagName('video');\n" +
            "  var src = (videos.html5Player) ? videos.html5Player.src\n" +
            "    : (videos.length > 0) ? videos[0].src\n" +
            "    : undefined;\n" +
            "  return { src: src }\n" +
            "})();";
        web.evaluateJavascript(javaScript, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String json) {
                String url = null;
                try {
                    JSONObject reader = new JSONObject(json);
                    url = reader.getString("src");
                } catch (JSONException e) {
                }
                if (url == null || url.isEmpty()) {
                    enterLoadingState();
                } else {
                    // We have it!
                    MediaDetails media = new MediaDetails(Uri.parse(url));
                    onResolved(media);
                }
            }
        });
    }

    private void update() {
        if (mResolveState != ResolveState.IDLE && mCallbacks.isEmpty()) {
            // There are no longer any requesters.
            enterIdleState();
        }

        switch (mResolveState) {
            case IDLE:
                if (!mCallbacks.isEmpty()) {
                    enterQueuedState();
                }
                break;
            case QUEUED:
                // Waiting for a WebView to become available
                break;
            case START:
                mPageState = PageState.LOADING;
                WebView web = mWebView.get();
                WebSettings settings = web.getSettings();
                settings.setUserAgentString("UCWEB/2.0 (iPad; U; CPU OS 7_1 like Mac OS X; en; iPad3,6) U2/1.0.0 UCBrowser/9.3.1.344");
                settings.setJavaScriptEnabled(true);
                settings.setLoadsImagesAutomatically(false);
                web.setWebViewClient(mWebViewClient);
                web.loadUrl(mUrl);

                enterLoadingState();
                break;
            case LOADING:
                // Polling intermittently to see if the video is there.
                break;
            case FINDING:
                // Waiting for JavaScript to complete
                break;
        }
    }
}
