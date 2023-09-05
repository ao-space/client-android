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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bridge.SimpleHardwareVerificationBridge;
import xyz.eulix.space.event.SecurityOperationResultEvent;
import xyz.eulix.space.presenter.SimpleHardwareVerificationPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StatusBarUtil;

/**
 * @author: chenjiawei
 * Description: 用于建立在已经通过硬件设备（如蓝牙、局域网）连接上后的通信
 * date: 2022/7/15 9:39
 */
public class SimpleHardwareVerificationActivity extends AbsActivity<SimpleHardwareVerificationPresenter.ISimpleHardwareVerification, SimpleHardwareVerificationPresenter> implements SimpleHardwareVerificationPresenter.ISimpleHardwareVerification
        , View.OnClickListener, SimpleHardwareVerificationBridge.SimpleHardwareVerificationSinkCallback {
    private ImageButton back;
    private TextView title;
    private LottieAnimationView boxSearching;
    private TextView hardwareSearchHint;
    private TextView hardwareVerificationHint;
    private Button startSearch;
    private Boolean isUIBoxSearching = null;
    private int hardwareIndex = 0;
    private int securityFunction = 0;
    private String authenticationUuid;
    private int authenticationFunction;
    private String baseUrl;
    private String bleKey;
    private String bleIv;
    private SimpleHardwareVerificationBridge mBridge;
    private SimpleHardwareVerificationHandler mHandler;

    static class SimpleHardwareVerificationHandler extends Handler {
        private WeakReference<SimpleHardwareVerificationActivity> simpleHardwareVerificationActivityWeakReference;

        public SimpleHardwareVerificationHandler(SimpleHardwareVerificationActivity activity) {
            simpleHardwareVerificationActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            SimpleHardwareVerificationActivity activity = simpleHardwareVerificationActivityWeakReference.get();
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
        setContentView(R.layout.activity_hardware_verification);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        boxSearching = findViewById(R.id.box_searching);
        hardwareSearchHint = findViewById(R.id.hardware_search_hint);
        hardwareVerificationHint = findViewById(R.id.hardware_verification_hint);
        startSearch = findViewById(R.id.start_search);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            hardwareIndex = intent.getIntExtra(ConstantField.HARDWARE_INDEX, 0);
            securityFunction = intent.getIntExtra(ConstantField.SECURITY_FUNCTION, 0);
            if (intent.hasExtra(ConstantField.BASE_URL)) {
                baseUrl = intent.getStringExtra(ConstantField.BASE_URL);
            }
            if (intent.hasExtra(ConstantField.BLE_KEY)) {
                bleKey = intent.getStringExtra(ConstantField.BLE_KEY);
            }
            if (intent.hasExtra(ConstantField.BLE_IV)) {
                bleIv = intent.getStringExtra(ConstantField.BLE_IV);
            }
            if (intent.hasExtra(ConstantField.AUTHENTICATION_UUID)) {
                authenticationUuid = intent.getStringExtra(ConstantField.AUTHENTICATION_UUID);
            }
            authenticationFunction = intent.getIntExtra(ConstantField.AUTHENTICATION_FUNCTION, 0);
        }
        mHandler = new SimpleHardwareVerificationHandler(this);
        mBridge = SimpleHardwareVerificationBridge.getInstance();
        mBridge.registerSinkCallback(this);
    }

    @Override
    public void initViewData() {
        title.setText(R.string.hardware_device_verify);
        startSearch.setVisibility(View.INVISIBLE);
        hardwareSearchHint.setTextColor(getResources().getColor(R.color.blue_ff337aff));
        hardwareSearchHint.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.dp_12));
        hardwareSearchHint.setTypeface(Typeface.DEFAULT);
        switch (hardwareIndex) {
            case ConstantField.HardwareIndex.BLUETOOTH:
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                String bluetoothVerifyHintPart1 = getString(R.string.bluetooth_verify_hint_part_1);
                String bluetoothVerifyHintPart2 = getString(R.string.bluetooth_verify_hint_part_2);
                String bluetoothVerifyHintPart3 = getString(R.string.bluetooth_verify_hint_part_3);
                String fullStop = getString(R.string.full_stop);
                spannableStringBuilder.append(bluetoothVerifyHintPart1);
                spannableStringBuilder.append(bluetoothVerifyHintPart2);
                spannableStringBuilder.append(fullStop);
                spannableStringBuilder.append(bluetoothVerifyHintPart3);
                StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
                int highlightStart = bluetoothVerifyHintPart1.length();
                int highlightEnd = (bluetoothVerifyHintPart1.length() + bluetoothVerifyHintPart2.length());
                spannableStringBuilder.setSpan(styleSpan, highlightStart, highlightEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                hardwareVerificationHint.setText(spannableStringBuilder);
                break;
            case ConstantField.HardwareIndex.LAN:
                hardwareVerificationHint.setGravity(Gravity.CENTER_HORIZONTAL);
                hardwareVerificationHint.setText(R.string.lan_verify_hint);
                break;
            default:
                break;
        }
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        setBoxSearching(true);
        setBoxSearingIndicator(0);
        switch (hardwareIndex) {
            case ConstantField.HardwareIndex.BLUETOOTH:
            case ConstantField.HardwareIndex.LAN:
                startSecurityFunction(securityFunction);
                break;
            default:
                break;
        }
    }

    private void setBoxSearching(boolean isWork) {
        boolean isUpdate = true;
        if (isUIBoxSearching != null && isWork == isUIBoxSearching) {
            isUpdate = false;
        }
        if (isUpdate) {
            if (isWork) {
                isUIBoxSearching = true;
                LottieUtil.loop(boxSearching, "search_box.json");
            } else {
                isUIBoxSearching = false;
                LottieUtil.stop(boxSearching, "search_box.json");
            }
        }
    }

    private void setBoxSearingIndicator(int index) {
        if (index < 0) {
            hardwareSearchHint.setVisibility(View.INVISIBLE);
        } else {
            boolean isShowHint = true;
            StringBuilder contentBuilder = new StringBuilder();
            switch (hardwareIndex) {
                case ConstantField.HardwareIndex.BLUETOOTH:
                    contentBuilder.append(getString(R.string.bluetooth_connecting_indicator));
                    break;
                case ConstantField.HardwareIndex.LAN:
                    contentBuilder.append(getString(R.string.lan_connecting_indicator));
                    break;
                default:
                    isShowHint = false;
                    break;
            }
//            while (index > 0) {
//                contentBuilder.append(".");
//                index -= 1;
//            }
            if (isShowHint) {
                hardwareSearchHint.setVisibility(View.VISIBLE);
                hardwareSearchHint.setText(contentBuilder.toString());
            } else {
                hardwareSearchHint.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void startSecurityFunction(int securityFunction) {
        switch (securityFunction) {
            case ConstantField.SecurityFunction.RESET_PASSWORD:
                resetSecurityPassword();
                break;
            default:
                break;
        }
    }

    private void resetSecurityPassword() {
        if (securityFunction == ConstantField.SecurityFunction.RESET_PASSWORD) {
            if (mBridge != null) {
                mBridge.startOrEndResetPassword(true);
            }
            Intent intent = new Intent(this, ModifySecurityPasswordActivity.class);
            intent.putExtra(ConstantField.RESET_PASSWORD, true);
            switch (hardwareIndex) {
                case ConstantField.HardwareIndex.BLUETOOTH:
                    intent.putExtra("bluetooth", true);
                    break;
                case ConstantField.HardwareIndex.LAN:
                    intent.putExtra("bluetooth", false);
                    break;
                default:
                    break;
            }
            if (baseUrl != null) {
                intent.putExtra(ConstantField.BASE_URL, baseUrl);
            }
            if (bleKey != null) {
                intent.putExtra(ConstantField.BLE_KEY, bleKey);
            }
            if (bleIv != null) {
                intent.putExtra(ConstantField.BLE_IV, bleIv);
            }
            intent.putExtra(ConstantField.AUTHENTICATION_FUNCTION, authenticationFunction);
            startActivityForResult(intent, ConstantField.RequestCode.RESET_SECURITY_PASSWORD_CODE);
        }
    }

    @Override
    protected void onDestroy() {
        if (mBridge != null) {
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
    public SimpleHardwareVerificationPresenter createPresenter() {
        return new SimpleHardwareVerificationPresenter();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantField.RequestCode.RESET_SECURITY_PASSWORD_CODE:
                if (mBridge != null) {
                    mBridge.startOrEndResetPassword(false);
                }
                EventBusUtil.post(new SecurityOperationResultEvent((resultCode == Activity.RESULT_OK), authenticationUuid));
                finish();
                break;
            default:
                break;
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

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }
}
