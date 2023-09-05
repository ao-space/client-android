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

package xyz.eulix.space.ui.authorization;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.Map;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.GranteeInfo;
import xyz.eulix.space.bean.LoginInfo;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.network.gateway.BoxLanInfo;
import xyz.eulix.space.presenter.GranteeLoginPresenter;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;

/**
 * @author: chenjiawei
 * Description: 授权登录-被授权端二维码展示页面
 * date: 2021/8/10 17:01
 */
public class GranteeLoginActivity extends AbsActivity<GranteeLoginPresenter.IGranteeLogin, GranteeLoginPresenter> implements GranteeLoginPresenter.IGranteeLogin, View.OnClickListener {
    private static final String TAG = GranteeLoginActivity.class.getSimpleName();
    public static final String KEY_LAN_BOX_DOMAIN = "key_lan_bod_domain";
    private static final int SHOW_QR_CODE = 0;
    private static final int QR_CODE_EXPIRED = SHOW_QR_CODE - 1;
    private static final int QR_CODE_SCAN_SUCCEED = SHOW_QR_CODE + 1;
    private static final int INPUT_AUTHORIZATION_CODE = QR_CODE_SCAN_SUCCEED + 1;
    private static final int AUTHORIZATION_CODE_FAILED = INPUT_AUTHORIZATION_CODE + 1;
    private static final int AUTHORIZATION_CODE_EXPIRED = QR_CODE_EXPIRED - 1;
    private static final int COUNTDOWN = 0;
    private static final int REBUILD_WEB_VIEW = COUNTDOWN + 1;
    private ImageButton back;
    private TextView title, qrCodeStateLine1, qrCodeStateLine2, loginContent;
    private ImageView qrCode, qrCodeStatus;
    private TextView authorizationCodeError, countdownTV;
    private LinearLayout qrCodeContainer, authorizationCodeContainer, authorizationCodeHintContainer;
    private RelativeLayout granteeLoginContainer;
    private WebView granteeWeb;
    private TextView dialogTitle, dialogContent;
    private Button dialogConfirm;
    private View authorizationCodeTipDialogView, authorizationCodeExpiredDialogView;
    private Dialog authorizationCodeTipDialog, authorizationCodeExpiredDialog;
    private int mState;
    private int totalCountdown = 300;
    private GranteeLoginHandler mHandler;
    private String mBoxPublicKey;

    private TextView tvLanConnect;

    //是否为局域网盒子页面
    private boolean isBoxLogin = false;
    //盒子局域网domain
    private String mBoxLanDomain;

    static class GranteeLoginHandler extends Handler {
        private WeakReference<GranteeLoginActivity> granteeLoginActivityWeakReference;

        public GranteeLoginHandler(GranteeLoginActivity activity) {
            granteeLoginActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            GranteeLoginActivity activity = granteeLoginActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case COUNTDOWN:
                        int timeSecond = msg.arg1;
                        if (timeSecond <= 0) {
                            activity.changeState(AUTHORIZATION_CODE_EXPIRED);
                        }
                        if (activity.countdownTV != null) {
                            String timeContent = timeSecond + "s";
                            activity.countdownTV.setText(timeContent);
                        }
                        break;
                    case REBUILD_WEB_VIEW:
                        if (msg.obj instanceof String) {
                            activity.destroyWebView();
                            activity.createWebView((String) msg.obj);
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.grantee_login_main);
        granteeLoginContainer = findViewById(R.id.grantee_login_container);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        qrCode = findViewById(R.id.qr_code);
        qrCodeStatus = findViewById(R.id.qr_code_status);
        qrCodeStateLine1 = findViewById(R.id.qr_code_state_line_1);
        qrCodeStateLine2 = findViewById(R.id.qr_code_state_line_2);
        loginContent = findViewById(R.id.login_content);
        qrCodeContainer = findViewById(R.id.qr_code_container);
        authorizationCodeContainer = findViewById(R.id.authorization_code_container);
        authorizationCodeError = findViewById(R.id.authorization_code_error);
        countdownTV = findViewById(R.id.countdown);
        authorizationCodeHintContainer = findViewById(R.id.authorization_code_hint_container);

        authorizationCodeTipDialogView = LayoutInflater.from(this).inflate(R.layout.authorization_code_tip_dialog, null);
        authorizationCodeTipDialog = new Dialog(this, R.style.EulixDialog);
        authorizationCodeTipDialog.setCancelable(true);
        authorizationCodeTipDialog.setContentView(authorizationCodeTipDialogView);

        authorizationCodeExpiredDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_one_button_dialog, null);
        dialogTitle = authorizationCodeExpiredDialogView.findViewById(R.id.dialog_title);
        dialogContent = authorizationCodeExpiredDialogView.findViewById(R.id.dialog_content);
        dialogConfirm = authorizationCodeExpiredDialogView.findViewById(R.id.dialog_confirm);
        authorizationCodeExpiredDialog = new Dialog(this, R.style.EulixDialog);
        authorizationCodeExpiredDialog.setCancelable(false);
        authorizationCodeExpiredDialog.setContentView(authorizationCodeExpiredDialogView);

        tvLanConnect = findViewById(R.id.tv_lan_connect);
    }

    @Override
    public void initData() {
        mHandler = new GranteeLoginHandler(this);

        mBoxLanDomain = getIntent().getStringExtra(KEY_LAN_BOX_DOMAIN);
        if (!TextUtils.isEmpty(mBoxLanDomain)) {
            isBoxLogin = true;
        }
    }

    @Override
    public void initViewData() {
        title.setText(R.string.login);
        loginContent.setText(Html.fromHtml(("<font color='#333333'>"
                + getString(R.string.login_content_part_1) + "</font><font color='#337aff'>"
                + getString(R.string.login_content_part_2) + "</font><font color='#333333'>"
                + getString(R.string.login_content_part_3) + "</font>")));
        dialogTitle.setText(R.string.input_authorization_code_expired_title);
        dialogContent.setText(R.string.input_authorization_code_expired_content);
        dialogConfirm.setText(R.string.ok);

        if (!isBoxLogin) {
            String url = (DebugUtil.getOfficialEnvironmentWeb() + (FormatUtil.isChinese(FormatUtil.getLocale(this)
                    , false) ? ConstantField.URL.LOGIN_API : ConstantField.URL.EN_LOGIN_API))
                    + "?isOpensource=1";
            createWebView(url);
//        changeState(SHOW_QR_CODE);
        } else {
            tvLanConnect.setVisibility(View.GONE);
            //局域网盒子页面路径
            StringBuilder urlSb = new StringBuilder();
            urlSb.append(mBoxLanDomain);
            urlSb.append("/space/index.html#/qrLogin");
            urlSb.append("?language=");
            if (FormatUtil.isChinese(FormatUtil.getLocale(this), false)) {
                urlSb.append("zh-CN");
            } else {
                urlSb.append("en-US");
            }
            urlSb.append("&isOpensource=1");
            String boxLoginPageUrl = urlSb.toString();
            Logger.d("zfy", "boxLoginPageUrl=" + boxLoginPageUrl);
            createWebView(boxLoginPageUrl);
        }
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        qrCodeStateLine2.setOnClickListener(this);
        authorizationCodeHintContainer.setOnClickListener(this);
        dialogConfirm.setOnClickListener(this);
        authorizationCodeTipDialogView.setOnClickListener(v -> dismissAuthorizationCodeTipDialog());

        tvLanConnect.setOnClickListener(v -> {
            //局域网IP输入
            Intent intent = new Intent(GranteeLoginActivity.this, LanIpInputActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected int getActivityIndex() {
        return GRANT_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public GranteeLoginPresenter createPresenter() {
        return new GranteeLoginPresenter();
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
            webView.addJavascriptInterface(new GranteeJavascriptInterface(), "JScallAndroidObj");
        }
    }

    private void createWebView(String url) {
        granteeWeb = new WebView(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        granteeLoginContainer.addView(granteeWeb, layoutParams);
        granteeWeb.loadUrl(url);
        granteeWeb.setWebChromeClient(new WebChromeClient());
        granteeWeb.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) {
                    return super.shouldOverrideUrlLoading(view, request);
                } else {
                    Logger.d(TAG, "should override url loading: " + request.getUrl().toString());
                    if (mHandler != null) {
                        Message message = mHandler.obtainMessage(REBUILD_WEB_VIEW, request.getUrl().toString());
                        mHandler.sendMessage(message);
                    }
                    return true;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Logger.d(TAG, "page start: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Logger.d(TAG, "page finish: " + url);
                if (url != null && !url.startsWith(DebugUtil.getOfficialEnvironmentWeb())) {
                    Map<String, String> queryMap = FormatUtil.formatQueryParams(url);
                    if (queryMap != null) {
                        String publicKey = queryMap.get("publickey");
                        if (!TextUtils.isEmpty(publicKey)) {
                            if (tvLanConnect.getVisibility() == View.VISIBLE) {
                                tvLanConnect.setVisibility(View.GONE);
                            }
                            String publicKeyDecoder = null;
                            try {
                                publicKeyDecoder = URLDecoder.decode(publicKey, "UTF-8");
                                mBoxPublicKey = StringUtil.unwrapPublicKey(publicKeyDecoder);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        GranteeInfo granteeInfo = new GranteeInfo();
                        granteeInfo.setTerminalType("android");
                        granteeInfo.setTerminalMode(StringUtil.nullToEmpty(SystemUtil.getPhoneModel()));
                        String clientUUID = DataUtil.getClientUuid(getApplicationContext());
                        if (isBoxLogin) {
                            //加密clientUUID
                            String clientUUIDEncrypt = null;
                            if (!TextUtils.isEmpty(publicKey)) {
                                clientUUIDEncrypt = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                                        , null, clientUUID
                                        , mBoxPublicKey, null, null);
                            }
                            granteeInfo.setClientUUID(StringUtil.nullToEmpty(clientUUIDEncrypt != null ? clientUUIDEncrypt : clientUUID));
                        } else {
                            //不加密clientUUID
                            granteeInfo.setClientUUID(StringUtil.nullToEmpty(clientUUID));
                        }
                        String javascriptRequest = "javascript:setEulixosEnv('android', '" +
                                new Gson().toJson(granteeInfo, GranteeInfo.class) +
                                "')";
                        granteeWeb.evaluateJavascript(javascriptRequest, value -> {
                            Logger.d(TAG, "js value callback: " + value);
                        });
                    }
                }
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
        initWebSettings(granteeWeb);
        initLocale();
    }

    private void destroyWebView() {
        if (granteeWeb != null) {
            granteeWeb.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            granteeWeb.clearHistory();
            if (granteeLoginContainer != null) {
                granteeLoginContainer.removeView(granteeWeb);
            }
            granteeWeb.destroy();
            granteeWeb = null;
        }
    }

    private void changeState(int state) {
        switch (state) {
            case SHOW_QR_CODE:
                back.setVisibility(View.GONE);
                authorizationCodeContainer.setVisibility(View.GONE);
                qrCodeContainer.setVisibility(View.VISIBLE);
                qrCodeStatus.setVisibility(View.INVISIBLE);
                qrCodeStateLine1.setVisibility(View.INVISIBLE);
                qrCodeStateLine2.setClickable(false);
                qrCodeStateLine2.setVisibility(View.INVISIBLE);
                qrCode.setVisibility(View.VISIBLE);
                break;
            case QR_CODE_EXPIRED:
                back.setVisibility(View.GONE);
                authorizationCodeContainer.setVisibility(View.GONE);
                qrCodeContainer.setVisibility(View.VISIBLE);
                qrCode.setVisibility(View.GONE);
                qrCodeStatus.setVisibility(View.VISIBLE);
                qrCodeStatus.setImageResource(R.drawable.scan_expired_2x);
                qrCodeStateLine1.setVisibility(View.VISIBLE);
                qrCodeStateLine1.setText(R.string.qr_code_invalid);
                qrCodeStateLine2.setVisibility(View.VISIBLE);
                qrCodeStateLine2.setClickable(true);
                qrCodeStateLine2.setText(R.string.click_to_refresh);
                qrCodeStateLine2.setTextColor(getResources().getColor(R.color.blue_ff337aff));
                break;
            case QR_CODE_SCAN_SUCCEED:
                back.setVisibility(View.GONE);
                authorizationCodeContainer.setVisibility(View.GONE);
                qrCodeContainer.setVisibility(View.VISIBLE);
                qrCode.setVisibility(View.GONE);
                qrCodeStatus.setVisibility(View.VISIBLE);
                qrCodeStatus.setImageResource(R.drawable.scan_succeeded_2x);
                qrCodeStateLine1.setVisibility(View.VISIBLE);
                qrCodeStateLine1.setText(R.string.scan_success);
                qrCodeStateLine2.setVisibility(View.VISIBLE);
                qrCodeStateLine2.setClickable(false);
                qrCodeStateLine2.setText(R.string.scan_success_hint);
                qrCodeStateLine2.setTextColor(getResources().getColor(R.color.gray_ff85899c));
                break;
            case INPUT_AUTHORIZATION_CODE:
                back.setVisibility(View.VISIBLE);
                qrCodeContainer.setVisibility(View.GONE);
                authorizationCodeContainer.setVisibility(View.VISIBLE);
                authorizationCodeError.setVisibility(View.INVISIBLE);
                countdownTV.setVisibility(View.VISIBLE);
                presenter.startCountdown(totalCountdown);
                break;
            case AUTHORIZATION_CODE_FAILED:
                back.setVisibility(View.VISIBLE);
                qrCodeContainer.setVisibility(View.GONE);
                authorizationCodeContainer.setVisibility(View.VISIBLE);
                authorizationCodeError.setVisibility(View.VISIBLE);
                countdownTV.setVisibility(View.VISIBLE);
                break;
            case AUTHORIZATION_CODE_EXPIRED:
                back.setVisibility(View.VISIBLE);
                qrCodeContainer.setVisibility(View.GONE);
                authorizationCodeContainer.setVisibility(View.VISIBLE);
                authorizationCodeError.setVisibility(View.INVISIBLE);
                countdownTV.setText("");
                countdownTV.setVisibility(View.INVISIBLE);
                presenter.stopCountdown();
                dismissAuthorizationCodeTipDialog();
                showAuthorizationCodeExpiredDialog();
                break;
            default:
                break;
        }
        mState = state;
    }

    private void generateBoxPublicKey(String url) {
        if (url != null) {
            Uri uri = Uri.parse(url);
            if (uri != null) {
                String publicKey = uri.getQueryParameter("publickey");
                if (publicKey != null) {
                    String nPublicKey = StringUtil.unwrapPublicKey(publicKey);
                    if (StringUtil.isNonBlankString(nPublicKey)) {
                        mBoxPublicKey = nPublicKey;
                    }
                }
            }
        }
    }

    private void showAuthorizationCodeTipDialog() {
        if (authorizationCodeTipDialog != null && !authorizationCodeTipDialog.isShowing()) {
            authorizationCodeTipDialog.show();
            Window window = authorizationCodeTipDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259)
                        , getResources().getDimensionPixelSize(R.dimen.dp_480));
            }
        }
    }

    private void dismissAuthorizationCodeTipDialog() {
        if (authorizationCodeTipDialog != null && authorizationCodeTipDialog.isShowing()) {
            authorizationCodeTipDialog.dismiss();
        }
    }

    private void showAuthorizationCodeExpiredDialog() {
        if (authorizationCodeExpiredDialog != null && !authorizationCodeExpiredDialog.isShowing()) {
            authorizationCodeExpiredDialog.show();
            Window window = authorizationCodeExpiredDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissAuthorizationCodeExpiredDialog() {
        if (authorizationCodeExpiredDialog != null && authorizationCodeExpiredDialog.isShowing()) {
            authorizationCodeExpiredDialog.dismiss();
        }
    }

    private void enterHomepage(LoginInfo loginInfo) {
        AOSpaceUtil.prepareGoMain(getApplicationContext());
        EulixSpaceApplication.popAllOldActivity(this);
        if (isBoxLogin) {
            //局域网ip/域名登录设置局域网信息
            if (loginInfo != null && loginInfo.getBoxLanInfo() != null && !TextUtils.isEmpty(loginInfo.getBoxLanInfo().lanIp)) {
                BoxLanInfo boxLanInfo = loginInfo.getBoxLanInfo();
                LanManager.getInstance().setLanDomainInfo(boxLanInfo.lanIp, boxLanInfo.port, boxLanInfo.tlsPort);
                LanManager.getInstance().startPollCheckTask();
            } else if (NetUtils.isIpAddress(mBoxLanDomain)) {
                //输入ip登录
                String tmpBoxLanDomain = mBoxLanDomain;
                if (mBoxLanDomain.endsWith("/") && mBoxLanDomain.length() > 1) {
                    tmpBoxLanDomain = mBoxLanDomain.substring(0, mBoxLanDomain.length() - 1);
                }
                String lanIp = null;
                String port = null;
                if (tmpBoxLanDomain.contains(":") && (tmpBoxLanDomain.indexOf(":") != tmpBoxLanDomain.lastIndexOf(":"))) {
                    int portIndex = tmpBoxLanDomain.lastIndexOf(":");
                    port = tmpBoxLanDomain.substring(portIndex + 1);
                    tmpBoxLanDomain = tmpBoxLanDomain.substring(0, portIndex);
                }

                if (tmpBoxLanDomain.startsWith("https://")) {
                    lanIp = tmpBoxLanDomain.substring(8);
                } else if (tmpBoxLanDomain.startsWith("http://")) {
                    lanIp = tmpBoxLanDomain.substring(7);
                }
                LanManager.getInstance().setLanDomainInfo(lanIp, port, null);
                LanManager.getInstance().startPollCheckTask();
            }
        }
        Intent mainIntent = new Intent(GranteeLoginActivity.this, EulixMainActivity.class);
        startActivity(mainIntent);
        finish();
//        handleResult(true);
    }

    private void handleResult(boolean isOk) {
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void countdownTime(int timeSecond) {
        if (mHandler != null) {
            Message message = mHandler.obtainMessage(COUNTDOWN, timeSecond, 0);
            mHandler.sendMessage(message);
        }
    }

    @Override
    public void internetServiceConfigCallback(LoginInfo loginInfo) {
        if (mHandler != null) {
            mHandler.post(() -> {
                boolean result = false;
                if (presenter != null) {
                    result = presenter.loginDevice(loginInfo, mBoxPublicKey);
                }
                closeLoading();
                if (result) {
                    enterHomepage(loginInfo);
                } else {
                    showImageTextToast(R.drawable.toast_refuse, R.string.authorization_fail);
                    handleResult(false);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (granteeWeb != null) {
            granteeWeb.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (granteeWeb != null) {
            granteeWeb.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        presenter.stopCountdown();
        destroyWebView();
        super.onDestroy();
    }

    public class GranteeJavascriptInterface {
        public GranteeJavascriptInterface() {
        }

        @JavascriptInterface
        public void setLoginInfo(String loginInfo) {
            Logger.d(TAG, "login info: " + loginInfo);
            if (loginInfo != null) {
                LoginInfo info = null;
                try {
                    info = new Gson().fromJson(loginInfo, LoginInfo.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                if (info != null && mHandler != null) {
                    LoginInfo finalInfo = info;
                    mHandler.post(() -> {
                        boolean result = false;
                        if (presenter != null) {
                            // todo 通道开放后调用
//                            result = presenter.loginDevice(finalInfo, mBoxPublicKey);

                            result = true;
                            showLoading("");
                            presenter.getInternetServiceConfig(finalInfo, isBoxLogin);
                        }
                        if (result) {
                            // todo 通道开放后调用
//                            enterHomepage(finalInfo);
                        } else {
                            showImageTextToast(R.drawable.toast_refuse, R.string.authorization_fail);
                            handleResult(false);
                        }
                    });
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    handleResult(false);
                    break;
                case R.id.qr_code_state_line_2:
                    if (mState == QR_CODE_EXPIRED) {
                        changeState(SHOW_QR_CODE);
                    }
                    break;
                case R.id.authorization_code_hint_container:
                    showAuthorizationCodeTipDialog();
                    break;
                case R.id.dialog_confirm:
                    dismissAuthorizationCodeExpiredDialog();
                    changeState(INPUT_AUTHORIZATION_CODE);
                    break;
                default:
                    break;
            }
        }
    }
}