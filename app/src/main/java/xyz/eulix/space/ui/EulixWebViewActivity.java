/*
 * Copyright (c) 2022 Institute of Software, Chinese Academy of Sciences (ISCAS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.eulix.space.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.presenter.WebViewPresenter;
import xyz.eulix.space.util.CameraUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PermissionUtils;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.share.ShareUtil;
import xyz.eulix.space.view.BottomDialog;
import xyz.eulix.space.view.TitleBarWithSelect;

/**
 * Author:      Zhu Fuyu
 * Description: 浏览器页面
 * History:     2021/7/16
 */
public class EulixWebViewActivity extends AbsActivity<WebViewPresenter.IWebView, WebViewPresenter> implements WebViewPresenter.IWebView {
    private TitleBarWithSelect titleBar;
    private WebView mWebView;
    private ProgressBar progressBar;
    private ScrollView scrollView;
    private ImageView imgLocal;
    private String url;
    private String title;
    //是否显示标题
    private boolean showTitle;
    //是否显示返回
    private boolean showBack;

    //页面埋点名称
    private String mLogUpPageName;

    //本地图片id
    private int imgResId;

    private ValueCallback<Uri[]> mUploadCallbackAboveL;
    private ValueCallback<Uri> mUploadCallbackBelow;

    private Dialog pictureFromDialog;

    private String jsObjName = "JScallAndroidObj";

    //正常带标题栏网页
    public static void startWeb(Context context, String title, String url) {
        Intent intent = new Intent(context, EulixWebViewActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("url", url);
        intent.putExtra("showTitle", true);
        intent.putExtra("showBack", true);
        context.startActivity(intent);
    }

    //显示无标题，但是带返回按键网页
    public static void startWebNoTitleWithBack(Context context, String url) {
        Intent intent = new Intent(context, EulixWebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("showTitle", false);
        intent.putExtra("showBack", true);
        context.startActivity(intent);
    }

    //显示无标题，但是带返回按键本地图片
    public static void startNoTitleLocalImgWithBack(Context context, int resId) {
        Intent intent = new Intent(context, EulixWebViewActivity.class);
        intent.putExtra("resId", resId);
        intent.putExtra("showTitle", false);
        intent.putExtra("showBack", true);
        context.startActivity(intent);
    }

    //显示无标题栏，全屏展示网页
    public static void startWebNoTitle(Context context, String url, String jsObjName) {
        Intent intent = new Intent(context, EulixWebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("showTitle", false);
        intent.putExtra("showBack", false);
        intent.putExtra("jsObjName", jsObjName);
        context.startActivity(intent);
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_web_view);
        titleBar = findViewById(R.id.title_bar);
        mWebView = findViewById(R.id.web_view);
        progressBar = findViewById(R.id.progress_bar);
        scrollView = findViewById(R.id.img_scroll_view);
        imgLocal = findViewById(R.id.img_local);

        WebSettings settings = mWebView.getSettings();
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setUserAgentString(settings.getUserAgentString() + ConstantField.WEB_VIEW_USER_AGENT_EXTEND);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            }
        }

        if (imgResId != -1) {
            mWebView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);
            imgLocal.setImageResource(imgResId);
        } else {
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }

                @Override
                public void onLoadResource(WebView view, String url) {
                    super.onLoadResource(view, url);
                }

                @Override
                public void onPageFinished(WebView view, String url) {//页面加载完成
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    progressBar.setVisibility(View.VISIBLE);
                }
            });

            mWebView.addJavascriptInterface(new EulixJavascriptInterface(this), jsObjName);

            mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    progressBar.setProgress(newProgress);
                }

                /**
                 * 16(Android 4.1.2) <= API <= 20(Android 4.4W.2)回调此方法
                 */
                public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                    mUploadCallbackBelow = uploadMsg;
                    showChoosePictureDialog();
                }

                /**
                 * API >= 21(Android 5.0.1)回调此方法
                 */
                @Override
                public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> valueCallback, FileChooserParams fileChooserParams) {
                    mUploadCallbackAboveL = valueCallback;
                    String[] acceptTypes = fileChooserParams.getAcceptTypes();
                    if (acceptTypes.length > 0) {
                        Logger.e("zfy", "类型:" + acceptTypes[0]);
                        showChoosePictureDialog();
                    }
                    return true;
                }
            });

        }
    }

    private void showChoosePictureDialog() {
        if (pictureFromDialog == null) {
            pictureFromDialog = new BottomDialog(this);
            pictureFromDialog.setCancelable(false);
            View view = View.inflate(this, R.layout.picture_from_choose_dialog_layout, null);
            pictureFromDialog.setContentView(view);

            TextView btnFromCamera = view.findViewById(R.id.btn_camera);
            TextView btnFromAlbum = view.findViewById(R.id.btn_album);
            TextView btnCancel = view.findViewById(R.id.btn_cancel);
            btnFromCamera.setOnClickListener((v) -> {
                if (PermissionUtils.isPermissionGranted(EulixWebViewActivity.this, PermissionUtils.PERMISSION_CAMERA)) {
                    CameraUtil.openCamera(this);
                    pictureFromDialog.dismiss();
                } else {
                    PermissionUtils.requestPermissionWithNotice(EulixWebViewActivity.this, PermissionUtils.PERMISSION_CAMERA, new ResultCallback() {
                        @Override
                        public void onResult(boolean result, String extraMsg) {
                            if (result) {
                                CameraUtil.openCamera(EulixWebViewActivity.this);
                                pictureFromDialog.dismiss();
                            } else {
                                if (mUploadCallbackBelow != null) {
                                    mUploadCallbackBelow.onReceiveValue(null);
                                } else if (mUploadCallbackAboveL != null) {
                                    mUploadCallbackAboveL.onReceiveValue(null);
                                }
                            }
                        }
                    });
                }
            });
            btnFromAlbum.setOnClickListener((v) -> {
                if (PermissionUtils.isPermissionGranted(EulixWebViewActivity.this, PermissionUtils.PERMISSION_WRITE_STORAGE)) {
                    CameraUtil.openGallery(this);
                    pictureFromDialog.dismiss();
                } else {
                    PermissionUtils.requestPermissionWithNotice(EulixWebViewActivity.this, PermissionUtils.PERMISSION_WRITE_STORAGE, new ResultCallback() {
                        @Override
                        public void onResult(boolean result, String extraMsg) {
                            if (result) {
                                CameraUtil.openGallery(EulixWebViewActivity.this);
                                pictureFromDialog.dismiss();
                            } else {
                                if (mUploadCallbackBelow != null) {
                                    mUploadCallbackBelow.onReceiveValue(null);
                                } else if (mUploadCallbackAboveL != null) {
                                    mUploadCallbackAboveL.onReceiveValue(null);
                                }
                            }
                        }
                    });
                }
            });
            btnCancel.setOnClickListener((v) -> {
                pictureFromDialog.dismiss();
                if (mUploadCallbackBelow != null) {
                    mUploadCallbackBelow.onReceiveValue(null);
                } else if (mUploadCallbackAboveL != null) {
                    mUploadCallbackAboveL.onReceiveValue(null);
                }
            });

        }
        pictureFromDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String path = "";
        switch (requestCode) {
            case CameraUtil.REQUEST_GALLERY_CODE:
                if (data == null) {
                    break;
                }
                path = data.getStringExtra("path");
                Logger.d("zfy", "图片地址：" + path);
                break;
            case CameraUtil.REQUEST_CAMERA_CODE:
                path = CameraUtil.getCameraBack();
                Logger.d("zfy", "照片地址：" + path);
                break;
            default:
                break;
        }

        File photoFile = new File(path);
        Uri uri = null;
        if (photoFile.exists()) {
            uri = ShareUtil.getFileUri(EulixWebViewActivity.this, photoFile);

        }
        if (mUploadCallbackBelow != null) {
            mUploadCallbackBelow.onReceiveValue(uri);
            mUploadCallbackBelow = null;
        } else if (mUploadCallbackAboveL != null) {
            mUploadCallbackAboveL.onReceiveValue(uri == null ? null : new Uri[]{uri});
            mUploadCallbackAboveL = null;
        }
    }

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mWebView.canGoBack() && keyCode == KeyEvent.KEYCODE_BACK) {
            mWebView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        url = intent.getStringExtra("url");
        title = intent.getStringExtra("title");
        showTitle = intent.getBooleanExtra("showTitle", true);
        showBack = intent.getBooleanExtra("showBack", false);
        imgResId = intent.getIntExtra("resId", -1);
        if (!TextUtils.isEmpty(intent.getStringExtra("jsObjName"))){
            jsObjName = intent.getStringExtra("jsObjName");
        }
        Logger.d("zfy", "webview url=" + url);

        if (!TextUtils.isEmpty(url)) {
            Logger.d("zfy", "webview url=" + url);
        } else if (imgResId != -1) {
            //展示本地图片
        } else {
            finish();
        }
    }

    @Override
    public void initViewData() {
        if (showTitle) {
            titleBar.setTitle(title);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mWebView.getLayoutParams();
            layoutParams.topMargin = getResources().getDimensionPixelOffset(R.dimen.dp_52);
            mWebView.setLayoutParams(layoutParams);
        } else {
            if (showBack) {
                titleBar.setTitle(null);
            } else {
                titleBar.setVisibility(View.GONE);
            }
        }
        if (!TextUtils.isEmpty(url)) {
            mWebView.loadUrl(url);
        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initLocale();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.destroy();
        }
    }

    @Override
    public void initEvent() {

    }

    @NotNull
    @Override
    public WebViewPresenter createPresenter() {
        return new WebViewPresenter();
    }

    /**
     * js通信接口
     */
    public class EulixJavascriptInterface {
        private Context context;

        public EulixJavascriptInterface(Context context) {
            this.context = context;
        }

        /**
         * 点击退出
         */
        @JavascriptInterface
        public void onClickExit() {
            Logger.d("zfy", "web click exit");
            finish();
        }

        @JavascriptInterface
        public void jsCallNativeMethod(String message) {
            Logger.d("zfy", "js call native method: " + message);
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
            String finalMethod = method;
            new Handler(Looper.getMainLooper()).post(()->{
                if (finalMethod != null) {
                    switch (finalMethod) {
                        case "setNativeTitle":
                            //do nothing
                            break;
                        case "onClickExit":
                            if (context instanceof Activity){
                                ((Activity) context).finish();
                            }
                            break;
                        default:
                            break;
                    }
                }
            });
        }
    }
}