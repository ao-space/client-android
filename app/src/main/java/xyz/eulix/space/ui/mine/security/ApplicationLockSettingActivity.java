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

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.ApplicationLockInfo;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.presenter.ApplicationLockSettingPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StatusBarUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/3 18:15
 */
public class ApplicationLockSettingActivity extends AbsActivity<ApplicationLockSettingPresenter.IApplicationLockSetting, ApplicationLockSettingPresenter> implements ApplicationLockSettingPresenter.IApplicationLockSetting, View.OnClickListener {
    private static final int FINGERPRINT_UNLOCK = 1;
    private static final int FACE_UNLOCK = FINGERPRINT_UNLOCK + 1;
    private static final int FINGERPRINT_AND_FACE_UNLOCK = FACE_UNLOCK + 1;
    private static final int BIOMETRIC_UNLOCK = FINGERPRINT_AND_FACE_UNLOCK + 1;
    private int biometricFeature = 0;
    private Boolean isFingerprintUnlock;
    private Boolean isFaceUnlock;
    private Boolean isBiometricUnlock;
    private List<String> fingerprintAuthenticateRequestIds;
    private List<String> faceAuthenticateRequestIds;
    private List<String> biometricAuthenticateRequestIds;
    private ImageButton back;
    private TextView title;
    private LinearLayout biometricUnlockContainer;
    private ImageButton biometricUnlockSwitch;
    private LinearLayout fingerprintFaceUnlockContainer;
    private LinearLayout fingerprintUnlockContainer;
    private ImageButton fingerprintUnlockSwitch;
    private View fingerprintFaceUnlockSplit;
    private LinearLayout faceUnlockContainer;
    private ImageButton faceUnlockSwitch;
    private ApplicationLockSettingHandler mHandler;
    private Runnable resetClickFingerprintUnlockRunnable = () -> {
        if (fingerprintUnlockSwitch != null) {
            fingerprintUnlockSwitch.setEnabled(true);
        }};
    private Runnable resetClickFaceUnlockRunnable = () -> {
        if (faceUnlockSwitch != null) {
            faceUnlockSwitch.setEnabled(true);
        }};
    private Runnable resetClickBiometricUnlockRunnable = () -> {
        if (biometricUnlockSwitch != null) {
            biometricUnlockSwitch.setEnabled(true);
        }};

    static class ApplicationLockSettingHandler extends Handler {
        private WeakReference<ApplicationLockSettingActivity> applicationLockSettingActivityWeakReference;

        public ApplicationLockSettingHandler(ApplicationLockSettingActivity activity) {
            applicationLockSettingActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            ApplicationLockSettingActivity activity = applicationLockSettingActivityWeakReference.get();
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
        setContentView(R.layout.activity_application_lock_setting);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        biometricUnlockContainer = findViewById(R.id.biometric_unlock_container);
        biometricUnlockSwitch = findViewById(R.id.biometric_unlock_switch);
        fingerprintFaceUnlockContainer = findViewById(R.id.fingerprint_face_unlock_container);
        fingerprintUnlockContainer = findViewById(R.id.fingerprint_unlock_container);
        fingerprintUnlockSwitch = findViewById(R.id.fingerprint_unlock_switch);
        fingerprintFaceUnlockSplit = findViewById(R.id.fingerprint_face_unlock_split);
        faceUnlockContainer = findViewById(R.id.face_unlock_container);
        faceUnlockSwitch = findViewById(R.id.face_unlock_switch);
    }

    @Override
    public void initData() {
        mHandler = new ApplicationLockSettingHandler(this);
        isFingerprintUnlock = null;
        isFaceUnlock = null;
        isBiometricUnlock = null;
    }

    @Override
    public void initViewData() {
        title.setText(R.string.application_lock);
        if (presenter != null) {
            biometricFeature = presenter.getBiometricFeature();
            if (biometricFeature > 0) {
                biometricUnlockContainer.setVisibility((biometricFeature >= BIOMETRIC_UNLOCK) ? View.VISIBLE : View.GONE);
                fingerprintFaceUnlockContainer.setVisibility((biometricFeature < BIOMETRIC_UNLOCK) ? View.VISIBLE : View.GONE);
                fingerprintUnlockContainer.setVisibility((biometricFeature == FINGERPRINT_UNLOCK || biometricFeature == FINGERPRINT_AND_FACE_UNLOCK) ? View.VISIBLE : View.GONE);
                fingerprintFaceUnlockSplit.setVisibility((biometricFeature == FINGERPRINT_AND_FACE_UNLOCK) ? View.VISIBLE : View.GONE);
                faceUnlockContainer.setVisibility((biometricFeature == FACE_UNLOCK || biometricFeature == FINGERPRINT_AND_FACE_UNLOCK) ? View.VISIBLE : View.GONE);
            } else {
                finish();
            }
        }
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        biometricUnlockSwitch.setOnClickListener(this);
        fingerprintUnlockSwitch.setOnClickListener(this);
        faceUnlockSwitch.setOnClickListener(this);
    }

    private void refreshSwitchPattern() {
        if (presenter != null) {
            boolean fingerprintUnlock = false;
            boolean faceUnlock = false;
            ApplicationLockInfo applicationLockInfo = presenter.getApplicationLockInfo();
            if (applicationLockInfo != null) {
                fingerprintUnlock = applicationLockInfo.isFingerprintUnlock();
                faceUnlock = applicationLockInfo.isFaceUnlock();
            }
            updateSwitchPattern(fingerprintUnlock, faceUnlock);
        }
    }

    private void updateSwitchPattern(Boolean isFingerprintOn, Boolean isFaceOn) {
        switch (biometricFeature) {
            case FINGERPRINT_UNLOCK:
                if (isFingerprintOn != null) {
                    fingerprintUnlockSwitch.setImageResource(isFingerprintOn ? R.drawable.icon_checkbox_open : R.drawable.icon_checkbox_close);
                }
                isFingerprintUnlock = isFingerprintOn;
                break;
            case FACE_UNLOCK:
                if (isFaceOn != null) {
                    faceUnlockSwitch.setImageResource(isFaceOn ? R.drawable.icon_checkbox_open : R.drawable.icon_checkbox_close);
                }
                isFaceUnlock = isFaceOn;
                break;
            case FINGERPRINT_AND_FACE_UNLOCK:
                if (isFingerprintOn != null) {
                    fingerprintUnlockSwitch.setImageResource(isFingerprintOn ? R.drawable.icon_checkbox_open : R.drawable.icon_checkbox_close);
                }
                if (isFaceOn != null) {
                    faceUnlockSwitch.setImageResource(isFaceOn ? R.drawable.icon_checkbox_open : R.drawable.icon_checkbox_close);
                }
                isFingerprintUnlock = isFingerprintOn;
                isFaceUnlock = isFaceOn;
                break;
            case BIOMETRIC_UNLOCK:
                if (isFingerprintOn != null && isFaceOn != null) {
                    boolean isBiometricOn = (isFingerprintOn || isFaceOn);
                    biometricUnlockSwitch.setImageResource(isBiometricOn ? R.drawable.icon_checkbox_open : R.drawable.icon_checkbox_close);
                    isBiometricUnlock = isBiometricOn;
                }
                break;
            default:
                break;
        }
    }

    private void removeAuthenticationRequestId(String requestId, int authenticateFeature) {
        if (requestId != null) {
            switch (authenticateFeature) {
                case FINGERPRINT_UNLOCK:
                    if (fingerprintAuthenticateRequestIds != null && fingerprintAuthenticateRequestIds.contains(requestId)) {
                        Iterator<String> iterator = fingerprintAuthenticateRequestIds.iterator();
                        while (iterator.hasNext()) {
                            String id = iterator.next();
                            if (requestId.equals(id)) {
                                iterator.remove();
                            }
                        }
                    }
                    break;
                case FACE_UNLOCK:
                    if (faceAuthenticateRequestIds != null && faceAuthenticateRequestIds.contains(requestId)) {
                        Iterator<String> iterator = faceAuthenticateRequestIds.iterator();
                        while (iterator.hasNext()) {
                            String id = iterator.next();
                            if (requestId.equals(id)) {
                                iterator.remove();
                            }
                        }
                    }
                    break;
                case BIOMETRIC_UNLOCK:
                    if (biometricAuthenticateRequestIds != null && biometricAuthenticateRequestIds.contains(requestId)) {
                        Iterator<String> iterator = biometricAuthenticateRequestIds.iterator();
                        while (iterator.hasNext()) {
                            String id = iterator.next();
                            if (requestId.equals(id)) {
                                iterator.remove();
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void resetUnlockState() {
        if (mHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                while (mHandler.hasCallbacks(resetClickBiometricUnlockRunnable)) {
                    mHandler.removeCallbacks(resetClickBiometricUnlockRunnable);
                }
                while (mHandler.hasCallbacks(resetClickFingerprintUnlockRunnable)) {
                    mHandler.removeCallbacks(resetClickFingerprintUnlockRunnable);
                }
                while (mHandler.hasCallbacks(resetClickFaceUnlockRunnable)) {
                    mHandler.removeCallbacks(resetClickFaceUnlockRunnable);
                }
            } else {
                try {
                    mHandler.removeCallbacks(resetClickBiometricUnlockRunnable);
                    mHandler.removeCallbacks(resetClickFingerprintUnlockRunnable);
                    mHandler.removeCallbacks(resetClickFaceUnlockRunnable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (biometricUnlockSwitch != null) {
            biometricUnlockSwitch.setEnabled(true);
        }
        if (fingerprintUnlockSwitch != null) {
            fingerprintUnlockSwitch.setEnabled(true);
        }
        if (faceUnlockSwitch != null) {
            faceUnlockSwitch.setEnabled(true);
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
        refreshSwitchPattern();
        resetUnlockState();
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @NotNull
    @Override
    public ApplicationLockSettingPresenter createPresenter() {
        return new ApplicationLockSettingPresenter();
    }

    public void authenticateCallback(boolean isSuccess, Integer code, String message, String responseId, int authenticateFeature) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (isSuccess) {
                    switch (authenticateFeature) {
                        case FINGERPRINT_UNLOCK:
                            updateSwitchPattern(false, null);
                            if (presenter != null) {
                                presenter.setFingerprintLockInfo(false);
                            }
                            showImageTextToast(R.drawable.toast_right, R.string.fingerprint_unlock_off_success);
                            break;
                        case FACE_UNLOCK:
                            updateSwitchPattern(null, false);
                            if (presenter != null) {
                                presenter.setFaceLockInfo(false);
                            }
                            showImageTextToast(R.drawable.toast_right, R.string.face_unlock_off_success);
                            break;
                        case BIOMETRIC_UNLOCK:
                            updateSwitchPattern(false, false);
                            if (presenter != null) {
                                presenter.setApplicationLockInfo(false, false);
                            }
                            showImageTextToast(R.drawable.toast_right, R.string.biometric_unlock_off_success);
                            break;
                        default:
                            break;
                    }
                    removeAuthenticationRequestId(responseId, authenticateFeature);
                    resetUnlockState();
                } else if (code != null) {
                    boolean isNoneEnrolled = false;
                    if (code == BiometricPrompt.ERROR_NO_BIOMETRICS) {
                        DataUtil.resetApplicationLockEventInfo();
                        isNoneEnrolled = true;
                        switch (authenticateFeature) {
                            case FINGERPRINT_UNLOCK:
                                updateSwitchPattern(false, null);
                                if (presenter != null) {
                                    presenter.setFingerprintLockInfo(false);
                                }
                                break;
                            case FACE_UNLOCK:
                                updateSwitchPattern(null, false);
                                if (presenter != null) {
                                    presenter.setFaceLockInfo(false);
                                }
                                break;
                            case BIOMETRIC_UNLOCK:
                                updateSwitchPattern(false, false);
                                if (presenter != null) {
                                    presenter.setApplicationLockInfo(false, false);
                                }
                                break;
                            default:
                                break;
                        }
                    } else if (code == BiometricPrompt.ERROR_CANCELED || code == BiometricPrompt.ERROR_USER_CANCELED || code == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        handlePositiveCancelError(responseId);
                    }
                    if (message != null && !TextUtils.isEmpty(message)) {
                        showPureTextToast(message);
                    } else if (isNoneEnrolled) {
                        switch (authenticateFeature) {
                            case FINGERPRINT_UNLOCK:
                                showImageTextToast(R.drawable.toast_right, R.string.fingerprint_unlock_off_success);
                                break;
                            case FACE_UNLOCK:
                                showImageTextToast(R.drawable.toast_right, R.string.face_unlock_off_success);
                                break;
                            case BIOMETRIC_UNLOCK:
                                showImageTextToast(R.drawable.toast_right, R.string.biometric_unlock_off_success);
                                break;
                            default:
                                break;
                        }
                    }
                    removeAuthenticationRequestId(responseId, authenticateFeature);
                    resetUnlockState();
                }
            });
        }
    }

    private int handleAuthenticateEvent(String requestId, String responseId) {
        int authenticateFeature = 0;
        if (requestId != null && requestId.equals(responseId)) {
            if (biometricAuthenticateRequestIds != null && biometricAuthenticateRequestIds.contains(requestId)) {
                authenticateFeature = BIOMETRIC_UNLOCK;
            } else if (fingerprintAuthenticateRequestIds != null && fingerprintAuthenticateRequestIds.contains(requestId)) {
                authenticateFeature = FINGERPRINT_UNLOCK;
            } else if (faceAuthenticateRequestIds != null && faceAuthenticateRequestIds.contains(requestId)) {
                authenticateFeature = FACE_UNLOCK;
            }
        }
        return authenticateFeature;
    }

    @Override
    protected void handleNoBiometrics() {
        super.handleNoBiometrics();
        refreshSwitchPattern();
    }

    @Override
    public void authenticateResult(boolean isSuccess, String responseId, String requestId) {
        int authenticateFeature = handleAuthenticateEvent(requestId, responseId);
        if (authenticateFeature > 0) {
            authenticateCallback(isSuccess, null, null, responseId, authenticateFeature);
        } else {
            super.authenticateResult(isSuccess, responseId, requestId);
        }
    }

    @Override
    public void authenticateError(int code, CharSequence errMsg, String responseId, String requestId) {
        int authenticateFeature = handleAuthenticateEvent(requestId, responseId);
        if (authenticateFeature > 0) {
            DataUtil.resetApplicationLockEventInfo(requestId);
            authenticateCallback(false, code, (errMsg == null ? null : errMsg.toString().trim()), responseId, authenticateFeature);
        } else {
            super.authenticateError(code, errMsg, responseId, requestId);
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.biometric_unlock_switch:
                    if (isBiometricUnlock != null && presenter != null) {
                        if (!isBiometricUnlock) {
                            Boolean authenticateResult = presenter.canAuthenticateEnrolled();
                            if (authenticateResult == null) {
                                showImageTextToast(R.drawable.toast_wrong, R.string.biometric_unlock_on_fail);
                            } else if (authenticateResult) {
                                updateSwitchPattern(true, true);
                                presenter.setApplicationLockInfo(true, true);
                                showImageTextToast(R.drawable.toast_right, R.string.biometric_unlock_on_success);
                            } else {
                                showImageTextToast(R.drawable.toast_wrong, R.string.biometric_unlock_on_fail_no_biometric);
                            }
                        } else {
                            biometricUnlockSwitch.setEnabled(false);
                            if (biometricAuthenticateRequestIds == null) {
                                biometricAuthenticateRequestIds = new ArrayList<>();
                            }
                            String biometricAuthenticateRequestId = UUID.randomUUID().toString();
                            biometricAuthenticateRequestIds.add(biometricAuthenticateRequestId);
                            EulixBoxBaseInfo eulixBoxBaseInfo = presenter.authenticate(biometricAuthenticateRequestId);
                            if (eulixBoxBaseInfo != null) {
                                handleApplicationLock(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), true);
                                if (mHandler != null) {
                                    mHandler.postDelayed(resetClickBiometricUnlockRunnable, 2000);
                                } else {
                                    biometricUnlockSwitch.setEnabled(true);
                                }
                            } else {
                                removeAuthenticationRequestId(biometricAuthenticateRequestId, BIOMETRIC_UNLOCK);
                                biometricUnlockSwitch.setEnabled(true);
                            }
                        }
                    }
                    break;
                case R.id.fingerprint_unlock_switch:
                    if (isFingerprintUnlock != null && presenter != null) {
                        if (!isFingerprintUnlock) {
                            Boolean authenticateResult = presenter.canAuthenticateEnrolled();
                            if (authenticateResult == null) {
                                showImageTextToast(R.drawable.toast_wrong, R.string.fingerprint_unlock_on_fail);
                            } else if (authenticateResult) {
                                updateSwitchPattern(true, null);
                                presenter.setFingerprintLockInfo(true);
                                showImageTextToast(R.drawable.toast_right, R.string.fingerprint_unlock_on_success);
                            } else {
                                showImageTextToast(R.drawable.toast_wrong, R.string.fingerprint_unlock_on_fail_no_fingerprint);
                            }
                        } else {
                            fingerprintUnlockSwitch.setEnabled(false);
                            if (fingerprintAuthenticateRequestIds == null) {
                                fingerprintAuthenticateRequestIds = new ArrayList<>();
                            }
                            String fingerprintAuthenticateRequestId = UUID.randomUUID().toString();
                            fingerprintAuthenticateRequestIds.add(fingerprintAuthenticateRequestId);
                            Logger.d("onAuth", "prepare fingerprint auth: " + fingerprintAuthenticateRequestId);
                            EulixBoxBaseInfo eulixBoxBaseInfo = presenter.authenticate(fingerprintAuthenticateRequestId);
                            if (eulixBoxBaseInfo != null) {
                                handleApplicationLock(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), true);
                                if (mHandler != null) {
                                    mHandler.postDelayed(resetClickFingerprintUnlockRunnable, 2000);
                                } else {
                                    fingerprintUnlockSwitch.setEnabled(true);
                                }
                            } else {
                                removeAuthenticationRequestId(fingerprintAuthenticateRequestId, FINGERPRINT_UNLOCK);
                                fingerprintUnlockSwitch.setEnabled(true);
                            }
                        }
                    }
                    break;
                case R.id.face_unlock_switch:
                    if (isFaceUnlock != null && presenter != null) {
                        if (!isFaceUnlock) {
                            Boolean authenticateResult = presenter.canAuthenticateEnrolled();
                            if (authenticateResult == null) {
                                showImageTextToast(R.drawable.toast_wrong, R.string.face_unlock_on_fail);
                            } else if (authenticateResult) {
                                updateSwitchPattern(null, true);
                                presenter.setFaceLockInfo(true);
                                showImageTextToast(R.drawable.toast_right, R.string.face_unlock_on_success);
                            } else {
                                showImageTextToast(R.drawable.toast_wrong, R.string.face_unlock_on_fail_no_face);
                            }
                        } else {
                            faceUnlockSwitch.setEnabled(false);
                            if (faceAuthenticateRequestIds == null) {
                                faceAuthenticateRequestIds = new ArrayList<>();
                            }
                            String faceAuthenticateRequestId = UUID.randomUUID().toString();
                            faceAuthenticateRequestIds.add(faceAuthenticateRequestId);
                            EulixBoxBaseInfo eulixBoxBaseInfo = presenter.authenticate(faceAuthenticateRequestId);
                            if (eulixBoxBaseInfo != null) {
                                handleApplicationLock(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), true);
                                if (mHandler != null) {
                                    mHandler.postDelayed(resetClickFaceUnlockRunnable, 2000);
                                } else {
                                    faceUnlockSwitch.setEnabled(true);
                                }
                            } else {
                                removeAuthenticationRequestId(faceAuthenticateRequestId, FACE_UNLOCK);
                                faceUnlockSwitch.setEnabled(true);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
