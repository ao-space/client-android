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
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.presenter.GranterAuthorizationPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.StatusBarUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/5/12 9:13
 */
public class GranterAuthorizationActivity extends AbsActivity<GranterAuthorizationPresenter.IGranterAuthorization, GranterAuthorizationPresenter> implements GranterAuthorizationPresenter.IGranterAuthorization, View.OnClickListener {
    private ImageButton back;
    private TextView title;
    private ImageView granterAvatar;
    private TextView granterNickname;
    private TextView granterDomain;
    private CheckBox authorizationAutomaticLogin;
    private Button granterConfirmRequest;
    private Button granterCancelRequest;
    private String boxUuid;
    private String boxBind;
    private String aoId;
    private String loginClientUuid;
    private String terminalType;
    private String terminalMode;
    private String userDomain;
    private GranterAuthorizationHandler mHandler;

    static class GranterAuthorizationHandler extends Handler {
        private WeakReference<GranterAuthorizationActivity> granterAuthorizationActivityWeakReference;

        public GranterAuthorizationHandler(GranterAuthorizationActivity activity) {
            granterAuthorizationActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            GranterAuthorizationActivity activity = granterAuthorizationActivityWeakReference.get();
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

    @Override
    public void initView() {
        setContentView(R.layout.activity_granter_authorization);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        granterAvatar = findViewById(R.id.granter_avatar);
        granterNickname = findViewById(R.id.granter_nickname);
        granterDomain = findViewById(R.id.granter_domain);
        authorizationAutomaticLogin = findViewById(R.id.authorization_automatic_login);
        granterConfirmRequest = findViewById(R.id.granter_confirm_request);
        granterCancelRequest = findViewById(R.id.granter_cancel_request);
    }

    @Override
    public void initData() {
        // todo intent获取userDomain和isBrowser
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(ConstantField.BOX_UUID)) {
                boxUuid = intent.getStringExtra(ConstantField.BOX_UUID);
            }
            if (intent.hasExtra(ConstantField.BOX_BIND)) {
                boxBind = intent.getStringExtra(ConstantField.BOX_BIND);
            }
            if (intent.hasExtra(ConstantField.AO_ID)) {
                aoId = intent.getStringExtra(ConstantField.AO_ID);
            }
            if (intent.hasExtra(ConstantField.USER_DOMAIN)) {
                userDomain = intent.getStringExtra(ConstantField.USER_DOMAIN);
            }
            if (intent.hasExtra(ConstantField.CLIENT_UUID)) {
                loginClientUuid = intent.getStringExtra(ConstantField.CLIENT_UUID);
            }
            if (intent.hasExtra(ConstantField.TERMINAL_TYPE)) {
                terminalType = intent.getStringExtra(ConstantField.TERMINAL_TYPE);
            }
            if (intent.hasExtra(ConstantField.TERMINAL_MODE)) {
                terminalMode = intent.getStringExtra(ConstantField.TERMINAL_MODE);
            }
        }
        mHandler = new GranterAuthorizationHandler(this);
    }

    @Override
    public void initViewData() {
        title.setText(R.string.login);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        granterConfirmRequest.setOnClickListener(this);
        granterCancelRequest.setOnClickListener(this);
    }

    private String generateBaseUrl(String boxDomain) {
        String baseUrl = boxDomain;
        if (baseUrl == null) {
            baseUrl = "";
        } else {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (TextUtils.isEmpty(baseUrl)) {
                baseUrl = DebugUtil.getEnvironmentServices();
            } else {
                if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                    baseUrl = "https://" + baseUrl;
                }
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }
            }
        }
        return baseUrl;
    }

    private void handleAuthAutoLogin(boolean isConfirm) {
        if (presenter != null && boxUuid != null && boxBind != null && loginClientUuid != null && authorizationAutomaticLogin != null) {
            showLoading("");
            presenter.authAutoLogin(boxUuid, boxBind, loginClientUuid, isConfirm, authorizationAutomaticLogin.isChecked());
        }
    }

    private void prepareFinish() {
        if (presenter != null) {
            presenter.setGranterAuthorization(null, null, null);
        }
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (presenter != null) {
            UserInfo userInfo = null;
            if (boxUuid != null) {
                if (userDomain != null) {
                    userInfo = presenter.getGranterInfo(userDomain);
                } else if (aoId != null) {
                    userInfo = presenter.getGranterInfo(boxUuid, boxBind, aoId);
                }
            }
            AOSpaceAccessBean aoSpaceAccessBean = presenter.getSpecificAOSpaceAccessBean(boxUuid, boxBind);
            if (userInfo == null) {
                prepareFinish();
                finish();
            } else {
                String avatarPath = userInfo.getAvatarPath();
                String nickname = userInfo.getNickName();
                if (avatarPath == null) {
                    granterAvatar.setImageResource(R.drawable.icon_user_header_default);
                } else {
                    GlideUtil.loadCircleFromPath(avatarPath, granterAvatar);
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getString(R.string.granter_authorization_content_part_1));
                if (terminalType != null) {
                    switch (terminalType) {
                        case "android":
                        case "ios":
                            stringBuilder.append(getString(R.string.mobile_phone));
                            break;
                        case "web":
                            stringBuilder.append(getString(R.string.browser));
                            break;
                        default:
                            break;
                    }
                }
//                if (terminalMode != null) {
//                    stringBuilder.append(" ");
//                    stringBuilder.append(terminalMode);
//                    stringBuilder.append(" ");
//                }
                stringBuilder.append(getString(R.string.granter_authorization_content_part_2));
                if (nickname == null) {
                    granterNickname.setTextColor(getResources().getColor(R.color.black_ff333333));
                    granterNickname.setText(stringBuilder.toString());
                } else {
                    granterNickname.setText(Html.fromHtml(("<font color='#333333'>"
                            + stringBuilder.toString() + "</font><font color='#337aff'>"
                            + " " + nickname + " " + "</font><font color='#333333'>"
                            + getString(R.string.affiliate_eulix_space) + "</font>")));
                }
                boolean isShowGranterDomain = true;
                if (aoSpaceAccessBean != null) {
                    Boolean isInternetAccess = aoSpaceAccessBean.getInternetAccess();
                    isShowGranterDomain = (isInternetAccess == null || isInternetAccess);
                }
                granterDomain.setText(isShowGranterDomain ? generateBaseUrl(userInfo.getUserDomain()) : "");
            }
        }
    }

    @Override
    public void onBackPressed() {
        prepareFinish();
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            prepareFinish();
        }
        return super.onKeyDown(keyCode, event);
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
    protected int getActivityIndex() {
        return GRANT_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public GranterAuthorizationPresenter createPresenter() {
        return new GranterAuthorizationPresenter();
    }

    @Override
    public void loginConfirmResult(int code, boolean isSuccess, boolean isConfirm) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (isSuccess) {
                    prepareFinish();
                    finish();
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.operation_exception);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    prepareFinish();
                    finish();
                    break;
                case R.id.granter_confirm_request:
                    handleAuthAutoLogin(true);
                    break;
                case R.id.granter_cancel_request:
                    handleAuthAutoLogin(false);
                    break;
                default:
                    break;
            }
        }
    }
}
