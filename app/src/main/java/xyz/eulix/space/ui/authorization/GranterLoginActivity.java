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

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.network.box.AuthCodeInfo;
import xyz.eulix.space.presenter.GranterLoginPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.view.CircleRingProgressBar;

/**
 * @author: chenjiawei
 * Description: 授权登录-绑定端-授权码展示页
 * date: 2021/8/10 15:09
 */
public class GranterLoginActivity extends AbsActivity<GranterLoginPresenter.IGranterLogin, GranterLoginPresenter> implements GranterLoginPresenter.IGranterLogin, View.OnClickListener {
    private static final int OBTAIN_AUTHORIZATION_RESULT_PERIOD = 1000;
    private static final int OBTAIN_AUTHORIZATION_RESULT = 1;
    public static final String KEY_IS_BOX_LOGIN = "key_is_box_login";
    private ImageButton back/*, close*/;
    private TextView title, authorizationLoginContent, authorizationCodeTitle, authorizationCodeHint;
    private CheckBox authorizationAutomaticLogin;
    private String qrValue = null;
    private String boxKey = null;
    private GranterLoginHandler mHandler;

    private TextView tvAuthorizationCode;
    private CircleRingProgressBar circlePgBar;
    private LinearLayout layoutAuthCode;
    private boolean isBoxLogin = false;

    private boolean hasSendBoxInfo = false;

    static class GranterLoginHandler extends Handler {
        private WeakReference<GranterLoginActivity> granterLoginActivityWeakReference;

        public GranterLoginHandler(GranterLoginActivity activity) {
            granterLoginActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            GranterLoginActivity activity = granterLoginActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case OBTAIN_AUTHORIZATION_RESULT:
                        String bkey;
                        if (activity.isBoxLogin) {
                            bkey = activity.qrValue;
                        } else {
                            bkey = activity.boxKey;
                        }
                        activity.presenter.obtainAuthResult(activity.isBoxLogin, bkey, activity.authorizationAutomaticLogin.isChecked());
                        sendEmptyMessageDelayed(OBTAIN_AUTHORIZATION_RESULT, OBTAIN_AUTHORIZATION_RESULT_PERIOD);
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
        setContentView(R.layout.granter_login_main);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        authorizationLoginContent = findViewById(R.id.authorization_login_content);
        authorizationCodeTitle = findViewById(R.id.authorization_code_title);
        authorizationAutomaticLogin = findViewById(R.id.authorization_automatic_login);
        authorizationCodeHint = findViewById(R.id.authorization_code_hint);

        tvAuthorizationCode = findViewById(R.id.authorization_code);
        circlePgBar = findViewById(R.id.circle_bar);
        layoutAuthCode = findViewById(R.id.layout_auth_code);
    }

    @Override
    public void initData() {
        mHandler = new GranterLoginHandler(this);
        handleIntent(getIntent());
    }

    @Override
    public void initViewData() {
        if (presenter != null && !presenter.isActiveDeviceBound()) {
            showImageTextToast(R.drawable.toast_refuse, R.string.operate_on_bind_device);
            finish();
        }
        title.setText(R.string.login_authorization);
        setAuthorizationLoginContent();
        setAuthorizationCodeVisibility(false);

        if (presenter != null) {
            showLoading("");
            if (!isBoxLogin) {
                presenter.obtainAuthCode(false);
            } else {
                if (!TextUtils.isEmpty(qrValue)) {
                    presenter.bKeyVerify(qrValue);
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.granter_fail_hint);
                    finish();
                }
            }
        }
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }


    @Override
    public void onBkeyVerifyResult(boolean result) {
        if (result) {
            presenter.obtainAuthCode(true);
        } else {
            closeLoading();
            showImageTextToast(R.drawable.toast_wrong, R.string.granter_fail_hint);
            finish();
        }
    }

    @Override
    protected int getActivityIndex() {
        return GRANT_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public GranterLoginPresenter createPresenter() {
        return new GranterLoginPresenter();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null && intent.hasExtra(ConstantField.PLATFORM_KEY)) {
            qrValue = intent.getStringExtra(ConstantField.PLATFORM_KEY);
        }
        isBoxLogin = getIntent().getBooleanExtra(KEY_IS_BOX_LOGIN, false);
    }

    private void setAuthorizationLoginContent() {
        String accountName = null;
        if (presenter != null) {
            accountName = presenter.getAccountName();
        }
        if (accountName == null) {
            authorizationLoginContent.setTextColor(getResources().getColor(R.color.black_ff333333));
            authorizationLoginContent.setText(R.string.login_authorization_content_part_2);
        } else {
            authorizationLoginContent.setText(Html.fromHtml(("<font color='#333333'>"
                    + getString(R.string.login_authorization_content_part_1) + "</font><font color='#337aff'>"
                    + " " + accountName + " " + "</font><font color='#333333'>"
                    + getString(R.string.login_authorization_content_part_2) + "</font>")));
        }
    }

    private void setAuthorizationCodeVisibility(boolean isVisible) {
        authorizationCodeTitle.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        layoutAuthCode.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        authorizationAutomaticLogin.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        authorizationCodeHint.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void boxInfoCallback(boolean isSuccess, String message) {
        hasSendBoxInfo = true;
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (isSuccess) {
                    while (mHandler.hasMessages(OBTAIN_AUTHORIZATION_RESULT)) {
                        mHandler.removeMessages(OBTAIN_AUTHORIZATION_RESULT);
                    }
                    setAuthorizationCodeVisibility(true);
                    mHandler.sendEmptyMessageDelayed(OBTAIN_AUTHORIZATION_RESULT, OBTAIN_AUTHORIZATION_RESULT_PERIOD);
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.granter_fail_hint);
                    finish();
                }
            });
        }
    }

    @Override
    public void authCodeCallback(AuthCodeInfo authCodeInfo) {
        if (mHandler != null) {
            mHandler.post(() -> {
                boolean isHandle = false;
                if (authCodeInfo != null) {
                    if (!TextUtils.isEmpty(authCodeInfo.getBkey())) {
                        boxKey = authCodeInfo.getBkey();
                    }
                    String authCode = authCodeInfo.getAuthCode();
                    if (authCode != null && presenter != null) {
                        isHandle = true;
                        int length = authCode.length();
                        tvAuthorizationCode.setText(authCode);
                        if (length > 6) {
                            tvAuthorizationCode.setLetterSpacing(0.1f);
                        }

                        //倒计时刷新逻辑
                        if (authCodeInfo.getAuthCodeTotalExpiresAt() != null) {
                            try {
                                int totalTime = Integer.parseInt(authCodeInfo.getAuthCodeTotalExpiresAt()) / 1000;
                                if (totalTime > 0) {
                                    circlePgBar.setVisibility(View.VISIBLE);
                                    int leftTime = Integer.parseInt(authCodeInfo.getAuthCodeExpiresAt()) / 1000;
                                    if (Integer.parseInt(authCodeInfo.getAuthCodeExpiresAt()) % 1000 > 0) {
                                        leftTime += 1;
                                    }
                                    if (leftTime > 0) {
                                        presenter.startCountdown(leftTime, totalTime);
                                    } else {
                                        //已失效，刷新
                                        presenter.obtainAuthCode(isBoxLogin);
                                    }
                                }
                            } catch (Exception e) {
                                Logger.e("zfy", e.getMessage());
                            }
                        }
                    }

                    if (isBoxLogin) {
                        boxInfoCallback(true, null);
                    }
                }

                if (!isBoxLogin && !hasSendBoxInfo && authCodeInfo != null) {
                    /*isHandle = */
                    while (mHandler.hasMessages(OBTAIN_AUTHORIZATION_RESULT)) {
                        mHandler.removeMessages(OBTAIN_AUTHORIZATION_RESULT);
                    }
                    presenter.sendBoxInfo(boxKey, qrValue, authCodeInfo.getLanDomain(), authCodeInfo.getLanIp());
                }

                if (!isHandle) {
                    closeLoading();
                    showImageTextToast(R.drawable.toast_wrong, R.string.granter_fail_hint);
                    finish();
//                    setButtonPattern(confirmLogin, true);
                }
            });
        }
    }

    @Override
    public void onLeftTimeRefresh(int leftTimeSecond, int progress) {
        circlePgBar.setTextAndProgress(leftTimeSecond + "", progress);
        if (leftTimeSecond == 0) {
            //刷新验证码
            circlePgBar.setTextAndProgress("0", 100);
            presenter.obtainAuthCode(true);
        }
    }

    @Override
    public void authResultCallback(boolean isSuccess) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (isSuccess) {
                    while (mHandler.hasMessages(OBTAIN_AUTHORIZATION_RESULT)) {
                        mHandler.removeMessages(OBTAIN_AUTHORIZATION_RESULT);
                    }
                    showImageTextToast(R.drawable.toast_right, R.string.authorization_success);
                    finish();
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                default:
                    break;
            }
        }
    }
}
