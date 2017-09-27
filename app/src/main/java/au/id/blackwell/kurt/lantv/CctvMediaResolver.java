package au.id.blackwell.kurt.lantv;

import android.net.Uri;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
            mResolveState = ResolveState.START;
            update();
        }
    };

    private final WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mPageState = PageState.FINISHED;
            update();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError
        error) {
            super.onReceivedError(view, request, error);
            mPageState = PageState.ERROR;
            update();
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
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
        mCallbacks.add(callback);
        update();
    }

    @Override
    public void cancel(Callback callback) {
        mCallbacks.remove(callback);
        update();
    }

    private void onResolved(MediaDetails details) {
        // Swap out callbacks so it's not possible to modify the array while dispatching.
        ArrayList<Callback> callbacks = mCallbacks;
        mCallbacks = new ArrayList<Callback>();

        enterIdleState();

        for (Callback callback : mCallbacks) {
            callback.onMediaResolved(details);
        }
    }

    private void enterIdleState() {
        // We can enter this state many ways.  Reset everything.

        mHandler.removeCallbacks(mFindVideoRunnable);
        mWebViewPool.cancel(mWebViewPoolCallback);
        if (mWebView != null) {
            WebView web = mWebView.get();
            web.setWebViewClient(null);
            web.loadUrl("about:blank");
            web.clearHistory();
            mWebView.release();
            mWebView = null;
            mPageState = PageState.NONE;
        }

        mResolveState = ResolveState.IDLE;
    }

    private void enterLoadingState() {
        if (mPageState != PageState.LOADING) {
            // The page is finished or failed, we don't want to do LOADING anymore.
            onResolved(null);
        } else {
            mHandler.postDelayed(mFindVideoRunnable, FIND_VIDEO_INTERVAL);
            mResolveState = ResolveState.LOADING;
            update();
        }
    }

    private void enterFindingState() {
        mResolveState = ResolveState.FINDING;

        WebView web = mWebView.get();
        String javaScript =
            "var videos = document.getElementsByTagName('video');\n" +
            "if (videos.html5Player) return videos.html5Player.src;\n" +
            "if (videos.length > 0) return videos[0].src;\n" +
            "return null;";
        web.evaluateJavascript(javaScript, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                if (s == null) {
                    enterLoadingState();
                } else {
                    // We have it!
                    MediaDetails media = new MediaDetails(Uri.parse(s));
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
                    mResolveState = ResolveState.QUEUED;
                    mWebViewPool.request(mWebViewPoolCallback);
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
