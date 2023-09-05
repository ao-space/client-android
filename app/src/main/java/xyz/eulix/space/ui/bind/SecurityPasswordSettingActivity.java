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

package xyz.eulix.space.ui.bind;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.EulixSpaceInitActivity;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bridge.SecurityPasswordBridge;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.PasswordInfo;
import xyz.eulix.space.network.agent.SetPasswordResults;
import xyz.eulix.space.presenter.SecurityPasswordSettingPresenter;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StatusBarUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/1 10:54
 */
public class SecurityPasswordSettingActivity extends AbsActivity<SecurityPasswordSettingPresenter.ISecurityPasswordSetting, SecurityPasswordSettingPresenter> implements SecurityPasswordSettingPresenter.ISecurityPasswordSetting, View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback, SecurityPasswordBridge.SecurityPasswordSinkCallback {
    private final int PASSWORD_LENGTH = 6;
    private String activityId;
    private ImageButton back;
    private TextView title;
    private EditText securityPassword;
    private EditText confirmPassword;
    private LinearLayout loadingButtonContainer;
    private LottieAnimationView loadingAnimation;
    private TextView loadingContent;
    private String mPassword;
    private SecurityPasswordSettingHandler mHandler;
    private InputMethodManager inputMethodManager;
    private AODeviceDiscoveryManager mManager;
    private long mExitTime = 0L;

    static class SecurityPasswordSettingHandler extends Handler {
        private WeakReference<SecurityPasswordSettingActivity> securityPasswordSettingActivityWeakReference;

        public SecurityPasswordSettingHandler(SecurityPasswordSettingActivity activity) {
            securityPasswordSettingActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            SecurityPasswordSettingActivity activity = securityPasswordSettingActivityWeakReference.get();
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
        setContentView(R.layout.activity_security_password_setting);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        securityPassword = findViewById(R.id.security_password);
        confirmPassword = findViewById(R.id.confirm_password);
        loadingButtonContainer = findViewById(R.id.loading_button_container);
        loadingAnimation = findViewById(R.id.loading_animation);
        loadingContent = findViewById(R.id.loading_content);
    }

    @Override
    public void initData() {
        mHandler = new SecurityPasswordSettingHandler(this);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        handleIntent(getIntent());
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
//        mBridge = SecurityPasswordBridge.getInstance();
//        mBridge.registerSinkCallback(this);
    }

    @Override
    public void initViewData() {
        back.setVisibility(View.GONE);
        title.setText(R.string.security_password_setting);
    }

    @Override
    public void initEvent() {
        loadingButtonContainer.setOnClickListener(this);
        setLoadingPattern(false, false);
        securityPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordLength();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > PASSWORD_LENGTH) {
                    securityPassword.setText(s.subSequence(0, PASSWORD_LENGTH));
                    Selection.setSelection(securityPassword.getText(), PASSWORD_LENGTH);
                }
            }
        });
        confirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordLength();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > PASSWORD_LENGTH) {
                    confirmPassword.setText(s.subSequence(0, PASSWORD_LENGTH));
                    Selection.setSelection(confirmPassword.getText(), PASSWORD_LENGTH);
                }
            }
        });
    }

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public SecurityPasswordSettingPresenter createPresenter() {
        return new SecurityPasswordSettingPresenter();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {

        }
    }

    private void checkPasswordLength() {
        if (securityPassword != null && confirmPassword != null) {
            setLoadingPattern(false, (securityPassword.getText().length() == PASSWORD_LENGTH
                    && securityPassword.getText().length() == confirmPassword.getText().length()));
        }
    }

    private void setLoadingPattern(boolean isLoading, boolean isEnable) {
        securityPassword.setEnabled(!isLoading);
        confirmPassword.setEnabled(!isLoading);
        if (loadingButtonContainer != null) {
            loadingButtonContainer.setClickable(!isLoading && isEnable);
            loadingButtonContainer.setBackgroundResource(isEnable
                    ? R.drawable.background_ff337aff_ff16b9ff_rectangle_10 : R.drawable.background_ffdfe0e5_rectangle_10);
        }
        if (loadingAnimation != null && loadingContent != null) {
            if (isLoading) {
                loadingContent.setText(R.string.next_step);
                loadingAnimation.setVisibility(View.VISIBLE);
                LottieUtil.loop(loadingAnimation, "loading_button.json");
            } else {
                LottieUtil.stop(loadingAnimation);
                loadingAnimation.setVisibility(View.GONE);
                loadingContent.setText(R.string.next_step);
            }
        }
    }

    /**
     * 收起软键盘
     */
    private void forceHideSoftInput() {
        if (inputMethodManager != null) {
            View view = getCurrentFocus();
            if (view != null) {
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void startInit() {
        Intent intent = new Intent(SecurityPasswordSettingActivity.this, EulixSpaceInitActivity.class);
        startActivity(intent);
    }

    private void confirmForceExit() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mExitTime > 2000) {
            showDefaultPureTextToast(R.string.app_exit_hint);
            mExitTime = currentTimeMillis;
        } else {
            EulixSpaceApplication.popAllOldActivity(null);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onBackPressed() {
        confirmForceExit();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            confirmForceExit();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onDestroy() {
        if (mManager != null) {
            mManager.unregisterCallback(activityId);
            mManager = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void handleResultCallback(boolean isSuccess) {
        if (mHandler != null) {
            mHandler.post(() -> {
                setLoadingPattern(false, true);
                if (isSuccess) {
                    startInit();
                    finish();
                } else {
                    showImageTextToast(R.drawable.toast_refuse, R.string.setting_fail);
                }
            });
        }
    }

    @Override
    public void bluetoothHandle(String oldPassword, String newPassword) {

    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.loading_button_container:
                    if (mManager != null) {
                        forceHideSoftInput();
                        String password1 = securityPassword.getText().toString();
                        String password2 = confirmPassword.getText().toString();
                        if (password1.equals(password2)) {
                            mPassword = password1;
                            setLoadingPattern(true, true);
                            PasswordInfo passwordInfo = new PasswordInfo();
                            passwordInfo.setPassword(password1);
                            mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SET_PASSWORD
                                    , new Gson().toJson(passwordInfo, PasswordInfo.class));
                        } else {
                            showImageTextToast(R.drawable.toast_wrong, R.string.inconsistent_input);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onFinish() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }

    @Override
    public void onResponse(int code, String source, int step, String bodyJson) {
        switch (step) {
            case AODeviceDiscoveryManager.STEP_SET_PASSWORD:
                SetPasswordResults setPasswordResults = null;
                if (bodyJson != null) {
                    try {
                        setPasswordResults = new Gson().fromJson(bodyJson, SetPasswordResults.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                int resultCode = 500;
                String resultSource = null;
                if (setPasswordResults != null) {
                    String codeValue = setPasswordResults.getCode();
                    resultCode = DataUtil.stringCodeToInt(codeValue);
                    resultSource = DataUtil.stringCodeGetSource(codeValue);
                }
                securityPasswordResult(resultCode, resultSource, true);
                break;
            default:
                break;
        }
    }

    @Override
    public void securityPasswordResult(int code, String source, Boolean isGranter) {
        if (mHandler != null) {
            mHandler.post(() -> {
                setLoadingPattern(false, true);
                boolean isSuccess = (code >= 0 && code < 400);
                if (isSuccess) {
                    if (mManager != null) {
                        mManager.setAdminPassword(mPassword);
                    }
                    startInit();
                    finish();
                } else {
                    showImageTextToast(R.drawable.toast_refuse, R.string.setting_fail);
                }
            });
        }
    }

    @Override
    public void newDeviceResetSecurityPasswordResult(String source, int code) {
        // Do nothing
    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }
}
