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

package xyz.eulix.space.ui.mine.security;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.GranterSecurityAuthenticationBean;
import xyz.eulix.space.network.security.SecurityTokenResult;
import xyz.eulix.space.presenter.GranterSecurityPasswordAuthenticationPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.StatusBarUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/21 15:57
 */
public class GranterSecurityPasswordAuthenticationActivity extends AbsActivity<GranterSecurityPasswordAuthenticationPresenter.IGranterSecurityPasswordAuthentication, GranterSecurityPasswordAuthenticationPresenter> implements GranterSecurityPasswordAuthenticationPresenter.IGranterSecurityPasswordAuthentication, View.OnClickListener {
    private ImageButton back;
    private TextView title;
    private TextView authenticationContent;
    private Button granterConfirmAuthentication;
    private Button granterCancelAuthentication;
    private String boxUuid;
    private String boxBind;
    private String messageType;
    private String authClientUuid;
    private String securityToken;
    private String terminalMode;
    private GranterSecurityPasswordAuthenticationHandler mHandler;

    static class GranterSecurityPasswordAuthenticationHandler extends Handler {
        private WeakReference<GranterSecurityPasswordAuthenticationActivity> granterSecurityPasswordAuthenticationActivityWeakReference;

        public GranterSecurityPasswordAuthenticationHandler(GranterSecurityPasswordAuthenticationActivity activity) {
            granterSecurityPasswordAuthenticationActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            GranterSecurityPasswordAuthenticationActivity activity = granterSecurityPasswordAuthenticationActivityWeakReference.get();
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
        setContentView(R.layout.activity_granter_security_password_authentication);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        authenticationContent = findViewById(R.id.authentication_content);
        granterConfirmAuthentication = findViewById(R.id.granter_confirm_authentication);
        granterCancelAuthentication = findViewById(R.id.granter_cancel_authentication);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(ConstantField.BOX_UUID)) {
                boxUuid = intent.getStringExtra(ConstantField.BOX_UUID);
            }
            if (intent.hasExtra(ConstantField.BOX_BIND)) {
                boxBind = intent.getStringExtra(ConstantField.BOX_BIND);
            }
            if (intent.hasExtra(ConstantField.MESSAGE_TYPE)) {
                messageType = intent.getStringExtra(ConstantField.MESSAGE_TYPE);
            }
            if (intent.hasExtra(ConstantField.CLIENT_UUID)) {
                authClientUuid = intent.getStringExtra(ConstantField.CLIENT_UUID);
            }
            if (intent.hasExtra(ConstantField.TERMINAL_MODE)) {
                terminalMode = intent.getStringExtra(ConstantField.TERMINAL_MODE);
            }
        }
        mHandler = new GranterSecurityPasswordAuthenticationHandler(this);
    }

    @Override
    public void initViewData() {
        title.setText(R.string.authentication);
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        String granterSecurityPasswordAuthenticationContentPart1 = getString(R.string.granter_security_password_authentication_content_part_1);
        String granterSecurityPasswordAuthenticationContentPart2 = getString(R.string.granter_security_password_authentication_content_part_2);
        spannableStringBuilder.append(granterSecurityPasswordAuthenticationContentPart1);
        int highlightStart = -1;
        int highlightEnd = -1;
        if (terminalMode != null && !TextUtils.isEmpty(terminalMode)) {
            spannableStringBuilder.append(" ");
            highlightStart = spannableStringBuilder.length();
            spannableStringBuilder.append(terminalMode);
            highlightEnd = spannableStringBuilder.length();
            spannableStringBuilder.append(" ");
        }
        spannableStringBuilder.append(granterSecurityPasswordAuthenticationContentPart2);
        if (highlightStart >= 0 && highlightEnd > highlightStart) {
            ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getResources().getColor(R.color.blue_ff337aff));
            spannableStringBuilder.setSpan(foregroundColorSpan, highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        authenticationContent.setText(spannableStringBuilder);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        granterConfirmAuthentication.setOnClickListener(this);
        granterCancelAuthentication.setOnClickListener(this);
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    private void handleGranterAuthentication(boolean isConfirm) {
        GranterSecurityAuthenticationBean granterSecurityAuthenticationBean = DataUtil.getProcessGranterSecurityAuthenticationBean();
        String securityToken = null;
        String applyId = null;
        if (granterSecurityAuthenticationBean != null) {
            SecurityTokenResult securityTokenResult = granterSecurityAuthenticationBean.getSecurityTokenResult();
            if (securityTokenResult != null) {
                securityToken = securityTokenResult.getSecurityToken();
            }
            applyId = granterSecurityAuthenticationBean.getApplyId();
        }
        if (messageType != null && boxUuid != null && boxBind != null && securityToken != null && authClientUuid != null && presenter != null) {
            switch (messageType) {
                case ConstantField.PushType.SECURITY_PASSWORD_MODIFY_APPLY:
                    showLoading("");
                    presenter.acceptHandleSecurityPassword(boxUuid, boxBind, securityToken, authClientUuid, isConfirm, applyId, false);
                    break;
                case ConstantField.PushType.SECURITY_PASSWORD_RESET_APPLY:
                    showLoading("");
                    presenter.acceptHandleSecurityPassword(boxUuid, boxBind, securityToken, authClientUuid, isConfirm, applyId, true);
                    break;
                default:
                    break;
            }
        }
    }

    private void prepareFinish() {
        if (presenter != null) {
            presenter.setGranterSecurityAuthentication(null, null, null, null, null, null);
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
        return SECURITY_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public GranterSecurityPasswordAuthenticationPresenter createPresenter() {
        return new GranterSecurityPasswordAuthenticationPresenter();
    }

    @Override
    public void securityPasswordAuthenticationResult(int code, String source, boolean isAccept) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (code >= 200 && code < 300) {
                    prepareFinish();
                    finish();
                } else if (code == ConstantField.KnownError.AccountCommonError.ACCOUNT_403 && ConstantField.KnownSource.ACCOUNT.equals(source)) {
                    prepareFinish();
                    finish();
                } else if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
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
                case R.id.granter_confirm_authentication:
                    handleGranterAuthentication(true);
                    break;
                case R.id.granter_cancel_authentication:
                    handleGranterAuthentication(false);
                    break;
                default:
                    break;
            }
        }
    }
}
