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

package xyz.eulix.space.presenter;

import android.os.CountDownTimer;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.network.box.AuthCodeInfo;
import xyz.eulix.space.network.box.BKeyCreateCallback;
import xyz.eulix.space.network.box.BKeyPollCallback;
import xyz.eulix.space.network.box.BKeyUtil;
import xyz.eulix.space.network.platform.PKeyBoxInfoCallback;
import xyz.eulix.space.network.platform.PKeyUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/10 15:08
 */
public class GranterLoginPresenter extends AbsPresenter<GranterLoginPresenter.IGranterLogin> {
    private CountDownTimer countDownTimer;
    private static final int SECOND_UNIT = 1000;
    private static final int timeSecond = 30;

    public interface IGranterLogin extends IBaseView {
        void boxInfoCallback(boolean isSuccess, String message);

        void authCodeCallback(AuthCodeInfo authCodeInfo);

        void authResultCallback(boolean isSuccess);

        //倒计时刷新
        void onLeftTimeRefresh(int leftTimeSecond, int progress);

        void onBkeyVerifyResult(boolean result);
    }

    private PKeyBoxInfoCallback pKeyBoxInfoCallback = new PKeyBoxInfoCallback() {
        @Override
        public void onError(String msg) {
            if (iView != null) {
                iView.boxInfoCallback(false, msg);
            }
        }

        @Override
        public void onFailed(int code) {
            if (iView != null) {
                iView.boxInfoCallback(false, String.valueOf(code));
            }
        }

        @Override
        public void onSuccess(int code) {
            if (iView != null) {
                iView.boxInfoCallback(true, String.valueOf(code));
            }
        }
    };

    private BKeyCreateCallback bKeyCreateCallback = new BKeyCreateCallback() {
        @Override
        public void onError(String msg) {
            if (iView != null) {
                iView.authCodeCallback(null);
            }
        }

        @Override
        public void onFailed(int code, String message) {
            if (iView != null) {
                iView.authCodeCallback(null);
            }
        }

        @Override
        public void onSuccess(int code, String message, AuthCodeInfo authCodeInfo) {
            if (iView != null) {
                iView.authCodeCallback(authCodeInfo);
            }
        }
    };

    private BKeyPollCallback bKeyPollCallback = new BKeyPollCallback() {
        @Override
        public void onError(String msg) {
            if (iView != null) {
                iView.authResultCallback(false);
            }
        }

        @Override
        public void onFailed(String requestId) {
            if (iView != null) {
                iView.authResultCallback(false);
            }
        }

        @Override
        public void onSuccess(String requestId) {
            if (iView != null) {
                iView.authResultCallback(true);
            }
        }
    };

    public boolean isActiveDeviceBound() {
        boolean isBound = false;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null) {
            String bindValue = null;
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null) {
                    bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    break;
                }
            }
            isBound = ("1".equals(bindValue) || "-1".equals(bindValue));
        }
        return isBound;
    }

    public String getAccountName() {
        String accountName = null;
        String clientUuid = DataUtil.getClientUuid(context);
        UserInfo userInfo = EulixSpaceDBUtil.getActiveUserInfo(context, clientUuid);
        if (userInfo != null && userInfo.isAdmin()) {
            accountName = userInfo.getNickName();
        }
        return accountName;
    }

    public boolean sendBoxInfo(String boxKey, String platformKey, String lanDomain, String lanIp) {
        boolean result = false;
        String boxDomainValue = null;
        String boxPublicKeyValue = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY)) {
                    String boxBindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    if ("1".equals(boxBindValue) || "-1".equals(boxBindValue)) {
                        boxDomainValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                        boxPublicKeyValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY);
                        break;
                    }
                }
            }
        }
        if (boxDomainValue != null && boxPublicKeyValue != null) {
            result = true;
            String boxDomain = boxDomainValue;
            String boxPubKey = boxPublicKeyValue;
            try {
//                ThreadPool.getInstance().execute(() -> PKeyUtil.sendBoxInfo(boxKey, boxDomain, boxPubKey, platformKey, pKeyBoxInfoCallback));
                ThreadPool.getInstance().execute(() -> PKeyUtil.sendBoxInfoV2(boxKey, boxDomain, boxPubKey, platformKey, lanDomain, lanIp, pKeyBoxInfoCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
        if (!result && iView != null) {
            iView.boxInfoCallback(false, null);
        }
        return result;
    }

    //校验bKey
    public void bKeyVerify(String bKey) {
        BKeyUtil.bKeyVerify(context, bKey, new ResultCallback() {
            @Override
            public void onResult(boolean result, String extraMsg) {
                iView.onBkeyVerifyResult(result);
            }
        });
    }

    //获取授权码
    public void obtainAuthCode(boolean isBoxLogin) {
        if (isBoxLogin) {
            BKeyUtil.obtainBoxLoginAuthCode(context, bKeyCreateCallback);
        } else {
            boolean result = false;
            String boxDomainValue = null;
            String accessTokenValue = null;
            String authKeyValue = null;
            String boxNameValue = null;
            String boxUUIDValue = null;
            String secretValue = null;
            String transformationValue = null;
            String ivParamsValue = null;
            String clientUUID = DataUtil.getClientUuid(context);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context
                    , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_NAME)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                        String boxBindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        if ("1".equals(boxBindValue) || "-1".equals(boxBindValue)) {
                            boxDomainValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                            authKeyValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION);
                            boxNameValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_NAME);
                            boxUUIDValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                            String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                            if (boxTokenValue != null) {
                                EulixBoxToken boxToken = null;
                                try {
                                    boxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (boxToken != null) {
                                    accessTokenValue = boxToken.getAccessToken();
                                    secretValue = boxToken.getSecretKey();
                                    transformationValue = boxToken.getTransformation();
                                    ivParamsValue = boxToken.getInitializationVector();
                                }
                            }
                            break;
                        }
                    }
                }
            }

            if (boxDomainValue != null && accessTokenValue != null && authKeyValue != null && clientUUID != null
                    && boxNameValue != null && boxUUIDValue != null && secretValue != null) {
                result = true;
                String boxDomain = boxDomainValue;
                String accessToken = accessTokenValue;
                String authKey = authKeyValue;
                String boxName = boxNameValue;
                String boxUUID = boxUUIDValue;
                String secret = secretValue;
                String transformation;
                if (transformationValue == null) {
                    transformation = ConstantField.Algorithm.Transformation.AES_CBC_PKCS5;
                } else {
                    transformation = transformationValue;
                }
                String ivParams = ivParamsValue;
                BKeyUtil.obtainAuthCode(context, boxDomain, accessToken, authKey, clientUUID, boxName
                        , boxUUID, secret, transformation, ivParams, true, bKeyCreateCallback);
            }
            if (!result && iView != null) {
                iView.authCodeCallback(null);
            }
        }
    }

    public void obtainAuthResult(boolean isBoxLogin, String boxKey, boolean isAutoLogin) {
        if (isBoxLogin) {
            BKeyUtil.obtainBoxLoginAuthResult(context, boxKey, isAutoLogin, bKeyPollCallback);
        } else {
            boolean result = false;
            String boxDomainValue = null;
            String boxUUID = null;
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context
                    , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)) {
                        String boxBindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        if ("1".equals(boxBindValue) || "-1".equals(boxBindValue)) {
                            boxUUID = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                            boxDomainValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                            break;
                        }
                    }
                }
            }
            if (boxDomainValue != null && boxUUID != null) {
                result = true;
                String boxDomain = boxDomainValue;
                BKeyUtil.obtainAuthResult(context, boxDomain, boxUUID, boxKey, isAutoLogin, false, bKeyPollCallback);
            }
            if (!result && iView != null) {
                iView.authResultCallback(false);
            }
        }
    }

    //开始倒计时
    public void startCountdown(int leftTime, int totalTime) {
        stopCountdown();
        countDownTimer = new CountDownTimer(((long) leftTime * SECOND_UNIT), SECOND_UNIT / 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (iView != null) {
                    //进度
                    int progress = 100 - (int) (millisUntilFinished * 100 / (totalTime * SECOND_UNIT));
                    int leftTime = 1 + (int) (millisUntilFinished / SECOND_UNIT);
                    iView.onLeftTimeRefresh(leftTime, progress);
                }
            }

            @Override
            public void onFinish() {
                if (iView != null) {
                    iView.onLeftTimeRefresh(0, 0);
                }
            }
        };
        countDownTimer.start();
    }

    public void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }
}
