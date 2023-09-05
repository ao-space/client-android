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

package xyz.eulix.space.manager;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.bean.ApplicationLockEventInfo;
import xyz.eulix.space.bean.ApplicationLockInfo;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.ApplicationLockEvent;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/3 18:52
 */
public class EulixBiometricManager {
    private static final String TAG = EulixBiometricManager.class.getSimpleName();
    private static final EulixBiometricManager INSTANCE = new EulixBiometricManager();
    private BiometricManager mBiometricManager;
    private static Map<String, BiometricPrompt> biometricPromptMap;

    private EulixBiometricManager() {
        mBiometricManager = BiometricManager.from(EulixSpaceApplication.getContext());
        biometricPromptMap = new ConcurrentHashMap<>();
    }

    public static EulixBiometricManager getInstance() {
        return INSTANCE;
    }

    public interface EulixBiometricAuthenticationCallback {
        void onError(int code, CharSequence errMsg, String responseId);
        void onResult(boolean isSuccess, String responseId);
    }

    private BiometricPrompt.AuthenticationCallback generateAuthenticationCallback(String requestId, EulixBiometricAuthenticationCallback callback) {
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Logger.d(TAG, "onAuthenticationError, error code: " + errorCode + ", error string: " + errString + ", id: " + requestId);
                if (callback != null) {
                    callback.onError(errorCode, errString, requestId);
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Logger.d(TAG, "onAuthenticationSucceeded: " + result.getAuthenticationType() + ", id: " + requestId);
                if (callback != null) {
                    callback.onResult(true, requestId);
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Logger.d(TAG, "onAuthenticationFailed: " + requestId);
                if (callback != null) {
                    callback.onResult(false, requestId);
                }
            }
        };
    }

    private BiometricPrompt generateBiometricPrompt(String requestId, @NonNull FragmentActivity activity, boolean isSubThread, EulixBiometricAuthenticationCallback callback) {
        BiometricPrompt.AuthenticationCallback authenticationCallback = generateAuthenticationCallback(requestId, callback);
        BiometricPrompt biometricPrompt;
        if (isSubThread) {
            ThreadPoolExecutor executor = ThreadPool.getInstance().getForeThreadPoolExecutor();
            if (executor != null && !executor.isShutdown()) {
                biometricPrompt = new BiometricPrompt(activity, executor, authenticationCallback);
            } else {
                biometricPrompt = new BiometricPrompt(activity, authenticationCallback);
            }
        } else {
            biometricPrompt = new BiometricPrompt(activity, authenticationCallback);
        }
        return biometricPrompt;
    }

    private BiometricPrompt generateBiometricPrompt(String requestId, @NonNull Fragment fragment, boolean isSubThread, EulixBiometricAuthenticationCallback callback) {
        BiometricPrompt.AuthenticationCallback authenticationCallback = generateAuthenticationCallback(requestId, callback);
        BiometricPrompt biometricPrompt;
        if (isSubThread) {
            ThreadPoolExecutor executor = ThreadPool.getInstance().getForeThreadPoolExecutor();
            if (executor != null && !executor.isShutdown()) {
                biometricPrompt = new BiometricPrompt(fragment, executor, authenticationCallback);
            } else {
                biometricPrompt = new BiometricPrompt(fragment, authenticationCallback);
            }
        } else {
            biometricPrompt = new BiometricPrompt(fragment, authenticationCallback);
        }
        return biometricPrompt;
    }

    private BiometricPrompt.PromptInfo generatePromptInfo(PromptInfoBean promptInfoBean) {
        BiometricPrompt.PromptInfo promptInfo = null;
        if (promptInfoBean != null) {
            String title = promptInfoBean.getTitle();
            String negativeButtonText = promptInfoBean.getNegativeButtonText();
            if (title != null && !TextUtils.isEmpty(title) && negativeButtonText != null && !TextUtils.isEmpty(negativeButtonText)) {
                promptInfo = new BiometricPrompt.PromptInfo.Builder()
                        .setTitle(title)
                        .setSubtitle(promptInfoBean.getSubtitle())
                        .setDescription(promptInfoBean.getDescription())
                        .setNegativeButtonText(negativeButtonText)
                        .build();
            }
        }
        return promptInfo;
    }

    public boolean canAuthenticate() {
        return canAuthenticate(false);
    }

    public boolean canAuthenticate(boolean isStrong) {
        boolean canAuthenticate = false;
        if (mBiometricManager != null) {
            canAuthenticate = (BiometricManager.BIOMETRIC_SUCCESS == mBiometricManager
                    .canAuthenticate(isStrong ? BiometricManager.Authenticators.BIOMETRIC_STRONG
                            : BiometricManager.Authenticators.BIOMETRIC_WEAK));
        }
        return canAuthenticate;
    }

    public Boolean canAuthenticateWithEnrolled() {
        return canAuthenticateWithEnrolled(false);
    }

    public Boolean canAuthenticateWithEnrolled(boolean isStrong) {
        Boolean canAuthenticate = null;
        if (mBiometricManager != null) {
            int authenticateStatus = mBiometricManager.canAuthenticate(isStrong ? BiometricManager.Authenticators.BIOMETRIC_STRONG
                            : BiometricManager.Authenticators.BIOMETRIC_WEAK);
            if (authenticateStatus == BiometricManager.BIOMETRIC_SUCCESS) {
                canAuthenticate = true;
            } else if (authenticateStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                canAuthenticate = false;
            }
        }
        return canAuthenticate;
    }

    public boolean authenticate(String requestId, @NonNull FragmentActivity activity, PromptInfoBean promptInfoBean, boolean isSubThread, EulixBiometricAuthenticationCallback callback) {
        boolean result = false;
        BiometricPrompt.PromptInfo promptInfo = generatePromptInfo(promptInfoBean);
        if (promptInfo != null) {
            result = true;
            BiometricPrompt biometricPrompt = generateBiometricPrompt(requestId, activity, isSubThread, callback);
            Logger.d("onAuth", "request authenticate callback: " + callback);
            authenticate(requestId, biometricPrompt, promptInfo);
        }
        return result;
    }

    public boolean authenticate(String requestId, @NonNull Fragment fragment, PromptInfoBean promptInfoBean, boolean isSubThread, EulixBiometricAuthenticationCallback callback) {
        boolean result = false;
        BiometricPrompt.PromptInfo promptInfo = generatePromptInfo(promptInfoBean);
        if (promptInfo != null) {
            result = true;
            BiometricPrompt biometricPrompt = generateBiometricPrompt(requestId, fragment, isSubThread, callback);
            authenticate(requestId, biometricPrompt, promptInfo);
        }
        return result;
    }

    private void authenticate(String requestId, @NonNull BiometricPrompt biometricPrompt, @NonNull BiometricPrompt.PromptInfo promptInfo) {
        if (requestId != null && biometricPromptMap != null) {
            biometricPromptMap.put(requestId, biometricPrompt);
        }
        Logger.d("onAuth", "request authenticate: " + requestId);
        biometricPrompt.authenticate(promptInfo);
    }

    public void cancelAuthenticate(String requestId, boolean isRealCancel) {
        if (requestId != null && biometricPromptMap != null && biometricPromptMap.containsKey(requestId)) {
            if (isRealCancel) {
                DataUtil.setApplicationLockEventInfoError(requestId, true);
                DataUtil.addCancelAuthentication(requestId);
                BiometricPrompt biometricPrompt = biometricPromptMap.get(requestId);
                if (biometricPrompt != null) {
                    biometricPrompt.cancelAuthentication();
                }
            } else {
                try {
                    biometricPromptMap.remove(requestId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 新增应用锁事件
     * @param requestId
     */
    public void setApplicationLockEventInfo(String requestId) {
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(EulixSpaceApplication.getContext(), true);
        String boxUuid = null;
        String boxBind = null;
        if (eulixBoxBaseInfo != null) {
            boxUuid = eulixBoxBaseInfo.getBoxUuid();
            boxBind = eulixBoxBaseInfo.getBoxBind();
        }
        setApplicationLockEventInfo(boxUuid, boxBind, requestId, true, false);
    }

    /**
     * 新增指定空间应用锁事件
     * @param boxUuid
     * @param boxBind
     * @param requestId
     * @param isNeedEvent
     */
    public void setApplicationLockEventInfo(String boxUuid, String boxBind, String requestId, boolean isNeedEvent, boolean isClick) {
        ApplicationLockInfo applicationLockInfo = getSpaceApplicationLock(boxUuid, boxBind);
        if (applicationLockInfo != null) {
            boolean isFingerprintUnlock = applicationLockInfo.isFingerprintUnlock();
            boolean isFaceUnlock = applicationLockInfo.isFaceUnlock();
            if (isFingerprintUnlock || isFaceUnlock) {
                if (requestId == null) {
                    requestId = UUID.randomUUID().toString();
                }
                ApplicationLockEventInfo applicationLockEventInfo = new ApplicationLockEventInfo(boxUuid, boxBind, requestId);
                applicationLockEventInfo.setFingerprintUnlock(isFingerprintUnlock);
                applicationLockEventInfo.setFaceUnlock(isFaceUnlock);
                Logger.d("onAuth", "prepare auth: " + applicationLockEventInfo.toString());
                List<String> cancelRequestIds = DataUtil.setApplicationLockEventInfo(applicationLockEventInfo, isClick);
                if (cancelRequestIds != null) {
                    for (String cancelRequestId : cancelRequestIds) {
                        if (cancelRequestId != null) {
                            Logger.d("onAuth", "false cancel: " + cancelRequestId);
                            cancelAuthenticate(cancelRequestId, false);
                        }
                    }
                }
                if (isNeedEvent) {
                    EventBusUtil.post(new ApplicationLockEvent(boxUuid, boxBind, requestId));
                }
            }
        }
    }

    public boolean getSpaceApplicationLockEnable(String boxUuid, String boxBind) {
        boolean isEnable = false;
        if (boxUuid != null && boxBind != null) {
            EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getBoxOtherInfo(EulixSpaceApplication.getContext(), boxUuid, boxBind);
            if (eulixBoxOtherInfo != null) {
                ApplicationLockInfo applicationLockInfo = eulixBoxOtherInfo.getApplicationLockInfo();
                if (applicationLockInfo != null) {
                    isEnable = (applicationLockInfo.isFingerprintUnlock() || applicationLockInfo.isFaceUnlock());
                }
            }
        }
        return isEnable;
    }

    /**
     * 获取指定空间应用锁事件
     * @param boxUuid
     * @param boxBind
     * @return
     */
    public ApplicationLockInfo getSpaceApplicationLock(String boxUuid, String boxBind) {
        ApplicationLockInfo applicationLockInfo = null;
        if (boxUuid != null && boxBind != null) {
            EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getBoxOtherInfo(EulixSpaceApplication.getContext(), boxUuid, boxBind);
            if (eulixBoxOtherInfo != null) {
                applicationLockInfo = eulixBoxOtherInfo.getApplicationLockInfo();
            }
        }
        return applicationLockInfo;
    }

    /**
     * 设置指定空间应用锁设置
     * @param boxUuid
     * @param boxBind
     * @param isFingerprintUnlock
     * @param isFaceUnlock
     */
    public void setSpaceApplicationLock(String boxUuid, String boxBind, boolean isFingerprintUnlock, boolean isFaceUnlock) {
        ApplicationLockInfo applicationLockInfo = new ApplicationLockInfo();
        applicationLockInfo.setFingerprintUnlock(isFingerprintUnlock);
        applicationLockInfo.setFaceUnlock(isFaceUnlock);
        setSpaceApplicationLock(boxUuid, boxBind, applicationLockInfo);
    }

    /**
     * 设置指定空间应用锁设置到数据库
     * @param boxUuid
     * @param boxBind
     * @param applicationLockInfo
     */
    public void setSpaceApplicationLock(String boxUuid, String boxBind, ApplicationLockInfo applicationLockInfo) {
        if (boxUuid != null && boxBind != null && applicationLockInfo != null) {
            boolean isHandle = false;
            JSONObject jsonObject = null;
            EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
            if (eulixSpaceDBBoxManager != null) {
                isHandle = true;
                jsonObject = new JSONObject();
                try {
                    jsonObject.put("applicationLockInfo", new Gson().toJson(applicationLockInfo, ApplicationLockInfo.class));
                } catch (JSONException e) {
                    e.printStackTrace();
                    isHandle = false;
                }
            }
            if (isHandle) {
                eulixSpaceDBBoxManager.updateBoxOtherInfo(jsonObject, null);
            } else {
                EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getBoxOtherInfo(EulixSpaceApplication.getContext(), boxUuid, boxBind);
                if (eulixBoxOtherInfo == null) {
                    eulixBoxOtherInfo = new EulixBoxOtherInfo();
                }
                eulixBoxOtherInfo.setApplicationLockInfo(applicationLockInfo);
                Map<String, String> boxValue = new HashMap<>();
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO, new Gson().toJson(eulixBoxOtherInfo, EulixBoxOtherInfo.class));
                EulixSpaceDBUtil.updateBox(EulixSpaceApplication.getContext(), boxValue);
            }
        }
    }

    public static class PromptInfoBean {
        private String title;
        private String negativeButtonText;
        private String subtitle;
        private String description;

        public PromptInfoBean(String title, String negativeButtonText) {
            this.title = title;
            this.negativeButtonText = negativeButtonText;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getNegativeButtonText() {
            return negativeButtonText;
        }
    }
}
