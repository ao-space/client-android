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

package xyz.eulix.space.ui.mine.developer;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.network.developer.DevelopOptionsSwitchInfo;
import xyz.eulix.space.presenter.DeveloperOptionsPresenter;
import xyz.eulix.space.ui.bind.UnbindDeviceActivity;
import xyz.eulix.space.ui.mine.security.EulixAuthenticationActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.StatusBarUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/17 11:31
 */
public class DeveloperOptionsActivity extends AbsActivity<DeveloperOptionsPresenter.IDeveloperOptions, DeveloperOptionsPresenter> implements DeveloperOptionsPresenter.IDeveloperOptions, View.OnClickListener {
    private TextView title;
    private ImageButton back;
    private ImageButton developerOptionsSwitch;
    private ScrollView developerOptionsContainer;
    private LinearLayout switchPlatformSpaceEnvironmentContainer;
    private RelativeLayout layoutSwitchPlatform;
    private TextView tvSwitchPlatformTitle;
    private TextView tvSwitchPlatformNotice;

    private LinearLayout openDeveloperOptionsContainer;
    private Boolean isDeveloperOptionSwitch;
    private boolean tempDeveloperOptionsSwitch;
    private int mIdentity;
    private DeveloperOptionsHandler mHandler;

    private boolean isSwitchPlatformEnable = false;

    static class DeveloperOptionsHandler extends Handler {
        private WeakReference<DeveloperOptionsActivity> developerOptionsActivityWeakReference;

        public DeveloperOptionsHandler(DeveloperOptionsActivity activity) {
            developerOptionsActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            DeveloperOptionsActivity activity = developerOptionsActivityWeakReference.get();
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
        setContentView(R.layout.activity_developer_options);
        title = findViewById(R.id.title);
        back = findViewById(R.id.back);
        developerOptionsSwitch = findViewById(R.id.developer_options_switch);
        developerOptionsContainer = findViewById(R.id.developer_options_container);
        switchPlatformSpaceEnvironmentContainer = findViewById(R.id.switch_space_platform_environment_container);
        layoutSwitchPlatform = findViewById(R.id.layout_switch_platform);
        tvSwitchPlatformTitle = findViewById(R.id.tv_switch_platform_title);
        tvSwitchPlatformNotice = findViewById(R.id.tv_switch_platform_notice);

        openDeveloperOptionsContainer = findViewById(R.id.open_developer_options_container);
    }

    @Override
    public void initData() {
        mHandler = new DeveloperOptionsHandler(this);
        isDeveloperOptionSwitch = null;
    }

    @Override
    public void initViewData() {
        title.setText(R.string.developer_options);

        openDeveloperOptionsContainer.setVisibility(View.GONE);
        developerOptionsContainer.setVisibility(View.GONE);
        int identity = ConstantField.UserIdentity.NO_IDENTITY;
        boolean isInternetAccessEnable = true;
        if (presenter != null) {
            identity = presenter.getIdentity();
            isInternetAccessEnable = presenter.isInternetAccessEnable();
        }
        mIdentity = identity;
        boolean isGranterAdmin = (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == identity);

        isSwitchPlatformEnable = isGranterAdmin && isInternetAccessEnable;
        if (isSwitchPlatformEnable) {
            tvSwitchPlatformTitle.setTextColor(getResources().getColor(R.color.black_ff333333));
            tvSwitchPlatformNotice.setVisibility(View.GONE);
        } else {
            tvSwitchPlatformTitle.setTextColor(getResources().getColor(R.color.c_ffbcbfcd));
            tvSwitchPlatformNotice.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        developerOptionsSwitch.setOnClickListener(this);
        switchPlatformSpaceEnvironmentContainer.setOnClickListener(this);

        int identity = ConstantField.UserIdentity.NO_IDENTITY;
        if (presenter != null) {
            identity = presenter.getIdentity();
        }
        mIdentity = identity;
        boolean isAdmin = (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == identity || ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE == identity);
        if (isAdmin && presenter != null) {
            if (isDeveloperOptionSwitch == null) {
                showLoading("");
            }
            presenter.getDeveloperOptionsSwitch(true);
        } else {
            showPureTextToast(R.string.service_exception_hint);
            finish();
        }
    }

    private void setDeveloperOptionsSwitchPattern() {
        boolean isGranterAdmin = (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == mIdentity);
        boolean isAdmin = (isGranterAdmin || ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE == mIdentity);
        if (isDeveloperOptionSwitch != null && developerOptionsSwitch != null) {
            if (isAdmin) {
                openDeveloperOptionsContainer.setVisibility(View.VISIBLE);
                developerOptionsSwitch.setClickable(isGranterAdmin);
                developerOptionsSwitch.setImageResource(isGranterAdmin ? (isDeveloperOptionSwitch
                        ? R.drawable.icon_checkbox_open : R.drawable.icon_checkbox_close) : (isDeveloperOptionSwitch
                        ? R.drawable.icon_checkbox_open_disable : R.drawable.icon_checkbox_close_disable));
                developerOptionsContainer.setVisibility(isDeveloperOptionSwitch ? View.VISIBLE : View.GONE);
            } else {
                openDeveloperOptionsContainer.setVisibility(View.GONE);
                developerOptionsContainer.setVisibility(View.GONE);
            }
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
        return DEVELOPER_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public DeveloperOptionsPresenter createPresenter() {
        return new DeveloperOptionsPresenter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantField.RequestCode.SECURITY_PASSWORD_VERIFICATION_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    showLoading("");
                    presenter.setDeveloperOptionsSwitch(true);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void getDeveloperOptionsSwitchCallback(int code, String source, DevelopOptionsSwitchInfo result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (isDeveloperOptionSwitch == null) {
                    closeLoading();
                }
                if (code >= 200 && code < 400 && result != null) {
                    String status = result.getStatus();
                    if (status != null) {
                        switch (status) {
                            case DevelopOptionsSwitchInfo.STATUS_ON:
                                isDeveloperOptionSwitch = true;
                                break;
                            case DevelopOptionsSwitchInfo.STATUS_OFF:
                                isDeveloperOptionSwitch = false;
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (isDeveloperOptionSwitch == null) {
                    showServerExceptionToast();
                    finish();
                } else {
                    setDeveloperOptionsSwitchPattern();
                }
            });
        }
    }

    @Override
    public void setDeveloperOptionsSwitchCallback(int code, String source, boolean isOn, Boolean result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (code >= 200 && code < 400 && result != null && result) {
                    isDeveloperOptionSwitch = isOn;
                    setDeveloperOptionsSwitchPattern();
                    showImageTextToast(R.drawable.toast_right, (isOn ? R.string.developer_options_on_success : R.string.developer_options_off_success));
                } else {
                    showImageTextToast(R.drawable.toast_wrong, (isOn ? R.string.on_fail : R.string.off_fail));
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
                case R.id.developer_options_switch:
                    if (presenter != null && isDeveloperOptionSwitch != null) {
                        if (isDeveloperOptionSwitch) {
                            showLoading("");
                            presenter.setDeveloperOptionsSwitch(false);
                        } else {
                            long permitTimestamp = presenter.getSecurityPasswordPermitTimestamp();
                            long currentTimestamp = System.currentTimeMillis();
                            if (permitTimestamp > currentTimestamp) {
                                long minute = (long) Math.ceil((permitTimestamp - currentTimestamp) * 1.0 / ConstantField.TimeUnit.MINUTE_UNIT);
                                if (minute > 0) {
                                    String stringBuilder = getString(R.string.common_retry_hint_minute_part_1) +
                                            minute +
                                            getString((Math.abs(minute) == 1L
                                                    ? R.string.common_retry_hint_minute_part_2_singular : R.string.common_retry_hint_minute_part_2_plural));
                                    showImageTextToast(R.drawable.toast_refuse, stringBuilder);
                                }
                            } else {
                                presenter.clearSecurityPasswordVerificationDenyTimestamp();
                                Intent intent = new Intent(DeveloperOptionsActivity.this, UnbindDeviceActivity.class);
                                intent.putExtra(ConstantField.SECURITY_FUNCTION, ConstantField.SecurityFunction.DeveloperOptionsSecurityFunction.OPEN_DEVELOPER_OPTIONS);
                                startActivityForResult(intent, ConstantField.RequestCode.SECURITY_PASSWORD_VERIFICATION_CODE);
                            }
                        }
                    }
                    break;
                case R.id.switch_space_platform_environment_container:
                    if (isSwitchPlatformEnable) {
                        Intent authenticationIntent = new Intent(DeveloperOptionsActivity.this, EulixAuthenticationActivity.class);
                        authenticationIntent.putExtra(ConstantField.HARDWARE_FUNCTION, ConstantField.HardwareFunction.SWITCH_SPACE_PLATFORM);
                        authenticationIntent.putExtra(ConstantField.SECURITY_FUNCTION, ConstantField.SecurityFunction.SWITCH_SPACE_PLATFORM);
                        startActivity(authenticationIntent);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
