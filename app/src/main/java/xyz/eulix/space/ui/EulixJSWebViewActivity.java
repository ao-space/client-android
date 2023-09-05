package xyz.eulix.space.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.presenter.EulixJSWebViewPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;

public abstract class EulixJSWebViewActivity extends AbsActivity<EulixJSWebViewPresenter.IEulixJSWebView, EulixJSWebViewPresenter> implements EulixJSWebViewPresenter.IEulixJSWebView, View.OnClickListener {
    protected static String TAG = EulixJSWebViewActivity.class.getSimpleName();
    protected static final String SHOW_TITLE = "showTitle";
    protected static final String SHOW_BACK = "showBack";
    protected static final String TITLE_NAME = "titleName";
    protected static final String FUNCTION_NAME = "functionName";
    protected static final String WEB_URL = "webUrl";
    protected static final String JS_INTERFACE_NAME = "jsInterfaceName";
    protected static final String OVERRIDE_REBUILD = "override_rebuild";
    private RelativeLayout titleContainer;
    private ImageButton back;
    private ImageButton backFullscreen;
    private TextView title;
    private Button functionText;
    private RelativeLayout webViewContainer;
    private ProgressBar loadingProgress;
    private WebView mWebView;
    private boolean isShowTitle = true;
    private boolean isShowBack = true;
    private String titleName;
    private String functionName;
    private String webUrl;
    private String jsInterfaceName;
    private boolean isOverrideUrlRebuildWebView = false;
    private EulixJSWebViewHandler mHandler;

    static class EulixJSWebViewHandler extends Handler {
        private WeakReference<EulixJSWebViewActivity> eulixJSWebViewActivityWeakReference;

        public EulixJSWebViewHandler(EulixJSWebViewActivity activity) {
            eulixJSWebViewActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixJSWebViewActivity activity = eulixJSWebViewActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    protected abstract void addJavascriptInterface(WebView webView, String jsInterfaceName);

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_js_web_view);
        titleContainer = findViewById(R.id.title_container);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        functionText = findViewById(R.id.function_text);
        webViewContainer = findViewById(R.id.web_view_container);
        backFullscreen = findViewById(R.id.back_fullscreen);
        loadingProgress = findViewById(R.id.loading_progress);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            isShowTitle = intent.getBooleanExtra(SHOW_TITLE, true);
            isShowBack = intent.getBooleanExtra(SHOW_BACK, true);
            titleName = (intent.hasExtra(TITLE_NAME) ? intent.getStringExtra(TITLE_NAME) : null);
            functionName = (intent.hasExtra(FUNCTION_NAME) ? intent.getStringExtra(FUNCTION_NAME) : null);
            webUrl = (intent.hasExtra(WEB_URL) ? intent.getStringExtra(WEB_URL) : null);
            jsInterfaceName = (intent.hasExtra(JS_INTERFACE_NAME) ? intent.getStringExtra(JS_INTERFACE_NAME) : null);
            isOverrideUrlRebuildWebView = intent.getBooleanExtra(OVERRIDE_REBUILD, false);
        }
        mHandler = new EulixJSWebViewHandler(this);
    }

    @Override
    public void initViewData() {
        title.setText(StringUtil.nullToEmpty(titleName));
        showTitlePattern(isShowTitle, isShowBack);
        if (functionName == null) {
            functionText.setVisibility(View.GONE);
        } else {
            functionText.setVisibility(View.VISIBLE);
            functionText.setText(functionName);
        }
        createWebView(StringUtil.nullToEmpty(webUrl));
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        backFullscreen.setOnClickListener(this);
        if (View.VISIBLE == functionText.getVisibility()) {
            functionText.setOnClickListener(this);
        } else {
            functionText.setClickable(false);
        }
    }

    private void showTitlePattern(boolean showTitle, boolean showBack) {
        titleContainer.setVisibility(showTitle ? View.VISIBLE : View.GONE);
        back.setVisibility((showTitle && showBack) ? View.VISIBLE : View.GONE);
        backFullscreen.setVisibility((!showTitle && showBack) ? View.VISIBLE : View.GONE);
    }

    private void setTitle(JSONObject params) {
        if (params != null) {
            String defaultTitleName = "";
            if (title != null) {
                CharSequence textCharSequence = title.getText();
                if (textCharSequence != null) {
                    defaultTitleName = textCharSequence.toString();
                }
            }
            String titleName = params.optString("titleName", defaultTitleName);
            boolean canGoBack = params.optBoolean("canGoBack", (isViewVisible(back) || isViewVisible(backFullscreen)));
            if (mHandler != null) {
                mHandler.post(() -> {
                    if (title != null && !TextUtils.isEmpty(titleName)) {
                        title.setText(titleName);
                    }
                    showTitlePattern((isShowTitle || !TextUtils.isEmpty(titleName)), canGoBack);
                });
            }
        }
    }

    private void performClickExit() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }

    protected void performFunctionEvent() {
        // todo 给继承类重写
    }

    protected boolean callNativeMethod(JSONObject params, String method, String callbackId) {
        boolean isHandle = false;
        if (method != null) {
            isHandle = true;
            switch (method) {
                case "setNativeTitle":
                    setTitle(params);
                    break;
                case "onClickExit":
                    performClickExit();
                    break;
                default:
                    isHandle = false;
                    break;
            }
        }
        return isHandle;
    }

    protected void handlerPost(Runnable runnable) {
        if (mHandler != null && runnable != null) {
            mHandler.post(runnable);
        }
    }

    protected void handlerPostDelayed(Runnable runnable, long delayMillis) {
        if (mHandler != null && runnable != null) {
            mHandler.postDelayed(runnable, delayMillis);
        }
    }

    protected void loadUrl(String url) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (mWebView != null) {
                    mWebView.loadUrl(url);
                }
            });
        }
    }

    private void handleLoadingProgress(boolean isVisible) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebSettings(WebView webView) {
        if (webView != null) {
            WebSettings webSettings = webView.getSettings();
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setUserAgentString(webSettings.getUserAgentString() + ConstantField.WEB_VIEW_USER_AGENT_EXTEND);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
//            webView.addJavascriptInterface(new GranteeLoginActivity.GranteeJavascriptInterface(), "JScallAndroidObj");
            if (jsInterfaceName != null) {
                addJavascriptInterface(webView, jsInterfaceName);
            }
            //webView.addJavascriptInterface(new EulixJavascriptInterface(this), "JScallNativeAppletObj");
        }
    }

    private void createWebView(String url) {
        mWebView = new WebView(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        webViewContainer.addView(mWebView, layoutParams);
        mWebView.loadUrl(url);
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (loadingProgress != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        loadingProgress.setProgress(newProgress, true);
                    } else {
                        loadingProgress.setProgress(newProgress);
                    }
                }
            }
        });
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                boolean isHandle = false;
                if (request != null) {
                    Uri uri = request.getUrl();
                    if (uri != null) {
                        isHandle = true;
                        String requestUrl = uri.toString();
                        Logger.d(TAG, "should override url loading: " + requestUrl);
                        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
                            overrideUrlLoading(view, request.getUrl().toString());
                        } else if (mHandler != null) {
                            mHandler.post(() -> overrideUrlLoading(view, requestUrl));
                        } else if (view != null) {
                            view.loadUrl(requestUrl);
                        } else {
                            isHandle = false;
                        }
                    }
                }
                return (isHandle || super.shouldOverrideUrlLoading(view, request));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Logger.d(TAG, "page start: " + url);
                handleLoadingProgress(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Logger.d(TAG, "page finish: " + url);
                handleLoadingProgress(false);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                Logger.d(TAG, "error: " + errorResponse.toString());
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                Logger.d(TAG, "error: " + error.toString());
                if (handler != null) {
                    handler.proceed();
                } else {
                    super.onReceivedSslError(view, handler, error);
                }
            }
        });
        initWebSettings(mWebView);
        initLocale();
    }

    private void overrideUrlLoading(WebView webView, String url) {
        if (isOverrideUrlRebuildWebView) {
            destroyWebView();
            createWebView(url);
        } else if (webView != null) {
            webView.loadUrl(url);
        }
    }

    private void destroyWebView() {
        if (mWebView != null) {
            mWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mWebView.clearHistory();
            if (webViewContainer != null) {
                webViewContainer.removeView(mWebView);
            }
            mWebView.destroy();
            mWebView = null;
        }
        handleLoadingProgress(false);
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @NotNull
    @Override
    public EulixJSWebViewPresenter createPresenter() {
        return new EulixJSWebViewPresenter();
    }

    @Override
    protected void onDestroy() {
        destroyWebView();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    public class EulixJavascriptInterface implements EulixKeep {
        private Context context;

        public EulixJavascriptInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        public void setNativeTitle(final String message) {
            Logger.d(TAG, "message: " + message);
        }

        @JavascriptInterface
        public void jsCallNativeMethod(String message) {
            Logger.d(TAG, "js call native method: " + message);
            JSONObject jsonObject = null;
            JSONObject params = null;
            String method = null;
            String callbackId = null;
            if (message != null) {
                try {
                    jsonObject = new JSONObject(message);
                    params = jsonObject.optJSONObject("params");
                    method = jsonObject.optString("method");
                    callbackId = jsonObject.optString("jsCallbackId");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (method != null && mHandler != null) {
                JSONObject finalParams = params;
                String finalMethod = method;
                String finalCallbackId = callbackId;
                mHandler.post(() -> callNativeMethod(finalParams, finalMethod, finalCallbackId));
            }
        }

        /**
         * 点击退出
         */
        @JavascriptInterface
        public void onClickExit() {
            Logger.d(TAG, "web click exit");
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                case R.id.back_fullscreen:
                    finish();
                    break;
                case R.id.function_text:
                    performFunctionEvent();
                    break;
                default:
                    break;
            }
        }
    }

    protected static class EulixJSWebViewBean {
        // 是否展示标题栏，顶部有标题栏（TRUE），顶部到状态栏（FALSE）
        private boolean showTitle;
        // 左上角是否展示返回键
        private boolean showBack;
        private String titleName;
        private String functionName;
        private String webUrl;
        private String jsInterfaceName;
        private boolean overrideRebuild;

        public boolean isShowTitle() {
            return showTitle;
        }

        public void setShowTitle(boolean showTitle) {
            this.showTitle = showTitle;
        }

        public boolean isShowBack() {
            return showBack;
        }

        public void setShowBack(boolean showBack) {
            this.showBack = showBack;
        }

        public String getTitleName() {
            return titleName;
        }

        public void setTitleName(String titleName) {
            this.titleName = titleName;
        }

        public String getFunctionName() {
            return functionName;
        }

        public void setFunctionName(String functionName) {
            this.functionName = functionName;
        }

        public String getWebUrl() {
            return webUrl;
        }

        public void setWebUrl(String webUrl) {
            this.webUrl = webUrl;
        }

        public String getJsInterfaceName() {
            return jsInterfaceName;
        }

        public void setJsInterfaceName(String jsInterfaceName) {
            this.jsInterfaceName = jsInterfaceName;
        }

        public boolean isOverrideRebuild() {
            return overrideRebuild;
        }

        public void setOverrideRebuild(boolean overrideRebuild) {
            this.overrideRebuild = overrideRebuild;
        }
    }
}
