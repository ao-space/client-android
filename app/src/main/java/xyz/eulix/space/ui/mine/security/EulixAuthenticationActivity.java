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
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.mine.security.AuthenticationAdapter;
import xyz.eulix.space.bridge.EulixAuthenticationBridge;
import xyz.eulix.space.event.SecurityOperationResultEvent;
import xyz.eulix.space.presenter.EulixAuthenticationPresenter;
import xyz.eulix.space.ui.bind.UnbindDeviceActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.StatusBarUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/11 10:16
 */
public class EulixAuthenticationActivity extends AbsActivity<EulixAuthenticationPresenter.IEulixAuthentication, EulixAuthenticationPresenter> implements EulixAuthenticationPresenter.IEulixAuthentication
        , View.OnClickListener, AuthenticationAdapter.OnItemClickListener, EulixAuthenticationBridge.EulixAuthenticationSinkCallback {
    private String mAuthenticationUuid;
    private ImageButton back;
    private TextView title;
    private RecyclerView authenticationList;
    private AuthenticationAdapter mAdapter;
    private List<Integer> mAuthenticationIndexList;
    private Boolean isSecurityFinish;
    private boolean isStart;
    private int hardwareIndex;
    private int securityFunction;
    private int hardwareFunction;
    private String emailAccount;
    private String baseUrl;
    private String bleKey;
    private String bleIv;
    private String tempBluetoothAddress;
    private String tempBluetoothDeviceName;
    private String forgetPasswordUuid;
    private String granterDataUuid;
    private String deviceHardwareInfoRequestUuid;
    private EulixAuthenticationBridge mBridge;
    private EulixAuthenticationHandler mHandler;

    static class EulixAuthenticationHandler extends Handler {
        private WeakReference<EulixAuthenticationActivity> eulixAuthenticationActivityWeakReference;

        public EulixAuthenticationHandler(EulixAuthenticationActivity activity) {
            eulixAuthenticationActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixAuthenticationActivity activity = eulixAuthenticationActivityWeakReference.get();
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
        setContentView(R.layout.activity_eulix_authentication);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        authenticationList = findViewById(R.id.authentication_list);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            hardwareIndex = intent.getIntExtra(ConstantField.HARDWARE_INDEX, 0);
            securityFunction = intent.getIntExtra(ConstantField.SECURITY_FUNCTION, 0);
            hardwareFunction = intent.getIntExtra(ConstantField.HARDWARE_FUNCTION, 0);
            if (intent.hasExtra(ConstantField.BASE_URL)) {
                baseUrl = intent.getStringExtra(ConstantField.BASE_URL);
            }
            if (intent.hasExtra(ConstantField.BLE_KEY)) {
                bleKey = intent.getStringExtra(ConstantField.BLE_KEY);
            }
            if (intent.hasExtra(ConstantField.BLE_IV)) {
                bleIv = intent.getStringExtra(ConstantField.BLE_IV);
            }
            if (intent.hasExtra(ConstantField.FORGET_PASSWORD_UUID)) {
                forgetPasswordUuid = intent.getStringExtra(ConstantField.FORGET_PASSWORD_UUID);
            }
            if (intent.hasExtra(ConstantField.GRANTER_DATA_UUID)) {
                granterDataUuid = intent.getStringExtra(ConstantField.GRANTER_DATA_UUID);
            }
        }
        mHandler = new EulixAuthenticationHandler(this);
        mAuthenticationUuid = UUID.randomUUID().toString();
        mAuthenticationIndexList = null;
        isSecurityFinish = null;
        if (hardwareIndex != 0) {
            mBridge = EulixAuthenticationBridge.getInstance();
            mBridge.registerSinkCallback(this);
        }
    }

    @Override
    public void initViewData() {
        title.setText(R.string.authentication);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        boolean hasSecurityEmail = false;
        if (presenter != null) {
            emailAccount = presenter.getSecurityEmail();
            hasSecurityEmail = (emailAccount != null);
        }
        switch (securityFunction) {
            case ConstantField.SecurityFunction.RESET_PASSWORD:
                mAuthenticationIndexList = new ArrayList<>();
                if (hasSecurityEmail) {
                    mAuthenticationIndexList.add(ConstantField.AuthenticationFunction.SECURITY_MAILBOX);
                }
                mAuthenticationIndexList.add(ConstantField.AuthenticationFunction.HARDWARE_DEVICE);
                break;
            case ConstantField.SecurityFunction.INITIALIZE_SECURITY_MAILBOX:
                mAuthenticationIndexList = new ArrayList<>();
                mAuthenticationIndexList.add(ConstantField.AuthenticationFunction.SECURITY_PASSWORD);
                mAuthenticationIndexList.add(ConstantField.AuthenticationFunction.HARDWARE_DEVICE);
                break;
            case ConstantField.SecurityFunction.CHANGE_SECURITY_MAILBOX:
                mAuthenticationIndexList = new ArrayList<>();
                if (hasSecurityEmail) {
                    mAuthenticationIndexList.add(ConstantField.AuthenticationFunction.OLD_SECURITY_MAILBOX);
                }
                mAuthenticationIndexList.add(ConstantField.AuthenticationFunction.SECURITY_PASSWORD);
                mAuthenticationIndexList.add(ConstantField.AuthenticationFunction.HARDWARE_DEVICE);
                break;
            case ConstantField.SecurityFunction.SWITCH_SPACE_PLATFORM:
                mAuthenticationIndexList = new ArrayList<>();
                mAuthenticationIndexList.add(ConstantField.AuthenticationFunction.HARDWARE_DEVICE);
                break;
            default:
                break;
        }
        if (mAuthenticationIndexList == null || mAuthenticationIndexList.isEmpty()) {
            finish();
        } else {
            mAdapter = new AuthenticationAdapter(this, mAuthenticationIndexList);
            mAdapter.setOnItemClickListener(this);
            authenticationList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
            authenticationList.addItemDecoration(new AuthenticationAdapter.ItemDecoration(RecyclerView.VERTICAL
                    , Math.round(getResources().getDimensionPixelSize(R.dimen.dp_10)), Color.TRANSPARENT));
            authenticationList.setAdapter(mAdapter);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (presenter != null) {
            emailAccount = presenter.getSecurityEmail();
        }
        isStart = true;
        securityFinish();
    }

    private void authenticationStart(int authentication) {
        switch (authentication) {
            case ConstantField.AuthenticationFunction.HARDWARE_DEVICE:
                if (hardwareIndex == 0) {
                    startLanDeviceVerification();
                } else {
                    isSecurityFinish = false;
                    if (mBridge != null) {
                        mBridge.followUp(true);
                    }
                    Intent intent = new Intent(EulixAuthenticationActivity.this, SimpleHardwareVerificationActivity.class);
                    intent.putExtra(ConstantField.HARDWARE_INDEX, hardwareIndex);
                    intent.putExtra(ConstantField.SECURITY_FUNCTION, securityFunction);
                    intent.putExtra(ConstantField.BASE_URL, baseUrl);
                    intent.putExtra(ConstantField.BLE_KEY, bleKey);
                    intent.putExtra(ConstantField.BLE_IV, bleIv);
                    intent.putExtra(ConstantField.AUTHENTICATION_UUID, mAuthenticationUuid);
                    intent.putExtra(ConstantField.AUTHENTICATION_FUNCTION, authentication);
                    if (granterDataUuid != null) {
                        intent.putExtra(ConstantField.GRANTER_DATA_UUID, granterDataUuid);
                    }
                    startActivity(intent);
                }
                break;
            case ConstantField.AuthenticationFunction.SECURITY_PASSWORD:
                if (mBridge != null) {
                    mBridge.followUp(false);
                }
                switch (securityFunction) {
                    case ConstantField.SecurityFunction.INITIALIZE_SECURITY_MAILBOX:
                    case ConstantField.SecurityFunction.CHANGE_SECURITY_MAILBOX:
                        long permitTimestamp = -1L;
                        if (presenter != null) {
                            permitTimestamp = presenter.getSecurityPasswordPermitTimestamp();
                        }
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
                            isSecurityFinish = false;
                            if (presenter != null) {
                                presenter.clearSecurityPasswordVerificationDenyTimestamp();
                            }
                            Intent unbindDeviceIntent = new Intent(EulixAuthenticationActivity.this, UnbindDeviceActivity.class);
                            unbindDeviceIntent.putExtra(ConstantField.SECURITY_FUNCTION, securityFunction);
                            unbindDeviceIntent.putExtra(ConstantField.AUTHENTICATION_UUID, mAuthenticationUuid);
                            unbindDeviceIntent.putExtra(ConstantField.AUTHENTICATION_FUNCTION, authentication);
                            startActivity(unbindDeviceIntent);
                        }
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }
    }

    private void startLanDeviceVerification() {
        isSecurityFinish = false;
        if (mBridge != null) {
            mBridge.followUp(true);
        }
        Intent hardwareDeviceVerifyIntent = new Intent(EulixAuthenticationActivity.this, LanVerificationActivity.class);
        hardwareDeviceVerifyIntent.putExtra(ConstantField.HARDWARE_FUNCTION, hardwareFunction);
        hardwareDeviceVerifyIntent.putExtra(ConstantField.SECURITY_FUNCTION, securityFunction);
        hardwareDeviceVerifyIntent.putExtra(ConstantField.AUTHENTICATION_UUID, mAuthenticationUuid);
        hardwareDeviceVerifyIntent.putExtra(ConstantField.AUTHENTICATION_FUNCTION, ConstantField.AuthenticationFunction.HARDWARE_DEVICE);
        if (forgetPasswordUuid != null) {
            hardwareDeviceVerifyIntent.putExtra(ConstantField.FORGET_PASSWORD_UUID, forgetPasswordUuid);
        }
        if (granterDataUuid != null) {
            hardwareDeviceVerifyIntent.putExtra(ConstantField.GRANTER_DATA_UUID, granterDataUuid);
        }
        startActivity(hardwareDeviceVerifyIntent);
    }

    private void securityFinish() {
        if (isStart && isSecurityFinish != null && isSecurityFinish) {
            isSecurityFinish = null;
            finish();
        }
    }

    @Override
    protected void onStop() {
        isStart = false;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mBridge != null) {
            mBridge.handleAuthenticationFinish();
            mBridge.unregisterSinkCallback();
            mBridge = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected int getActivityIndex() {
        return SECURITY_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public EulixAuthenticationPresenter createPresenter() {
        return new EulixAuthenticationPresenter();
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

    @Override
    public void onItemClick(View view, int position) {
        if (position >= 0 && mAuthenticationIndexList != null && mAuthenticationIndexList.size() > position) {
            authenticationStart(mAuthenticationIndexList.get(position));
        }
    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null && hardwareIndex == ConstantField.HardwareIndex.BLUETOOTH) {
            mHandler.post(this::finish);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SecurityOperationResultEvent event) {
        if (event != null) {
            String authenticationUuid = event.getAuthenticationUuid();
            if (authenticationUuid != null && authenticationUuid.equals(mAuthenticationUuid)) {
                if (event.isSuccess() && isSecurityFinish != null && !isSecurityFinish) {
                    isSecurityFinish = true;
                    securityFinish();
                }
                if (isSecurityFinish != null) {
                    if (!event.isSuccess()) {
                        isSecurityFinish = null;
                    } else if (!isSecurityFinish) {
                        isSecurityFinish = true;
                        securityFinish();
                    }
                }
            }
        }
    }
}
