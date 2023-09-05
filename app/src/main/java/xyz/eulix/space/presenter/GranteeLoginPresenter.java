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
import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.EulixBoxTokenDetail;
import xyz.eulix.space.bean.LoginInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.SpaceChangeEvent;
import xyz.eulix.space.network.gateway.AlgorithmConfig;
import xyz.eulix.space.network.gateway.BoxLanInfo;
import xyz.eulix.space.network.gateway.TransportationConfig;
import xyz.eulix.space.network.net.EulixNetUtil;
import xyz.eulix.space.network.net.InternetServiceConfigCallback;
import xyz.eulix.space.network.net.InternetServiceConfigResult;
import xyz.eulix.space.util.AlarmUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/10 16:59
 */
public class GranteeLoginPresenter extends AbsPresenter<GranteeLoginPresenter.IGranteeLogin> {
    private static final int SECOND_UNIT = 1000;
    private CountDownTimer countDownTimer;
    private AOSpaceAccessBean mAoSpaceAccessBean;
    public interface IGranteeLogin extends IBaseView {
        void countdownTime(int timeSecond);
        void internetServiceConfigCallback(LoginInfo loginInfo);
    }

    public void startCountdown(int timeSecond) {
        stopCountdown();
        countDownTimer = new CountDownTimer((timeSecond * SECOND_UNIT), SECOND_UNIT) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (iView != null) {
                    iView.countdownTime((1 + (int) (millisUntilFinished / SECOND_UNIT)));
                }
            }

            @Override
            public void onFinish() {
                if (iView != null) {
                    iView.countdownTime(0);
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

    /**
     * todo 后续有通道时删除该方法
     * @param loginInfo
     * @return
     */
    public String generateBoxDomain(LoginInfo loginInfo) {
        String boxDomain = null;
        String domain = null;
        String userDomain = null;
        String ipAddressUrl = null;
        if (loginInfo != null) {
            domain = loginInfo.getDomain();
            BoxLanInfo boxLanInfo = loginInfo.getBoxLanInfo();
            if (boxLanInfo != null) {
                userDomain = boxLanInfo.getUserDomain();
                String lanIp = boxLanInfo.getLanIp();
                String lanPort = boxLanInfo.getPort();
                StringBuilder ipAddressUrlBuilder = null;
                if (lanIp != null) {
                    ipAddressUrlBuilder = new StringBuilder();
                    boolean isIpv6 = false;
                    ipAddressUrlBuilder.append("http://");
                    if (lanIp.contains(":")) {
                        isIpv6 = true;
                    }
                    if (isIpv6) {
                        ipAddressUrlBuilder.append("[");
                    }
                    ipAddressUrlBuilder.append(lanIp);
                    if (isIpv6) {
                        ipAddressUrlBuilder.append("]");
                    }
                    if (lanPort != null) {
                        Integer port = null;
                        try {
                            port = Integer.parseInt(lanPort);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        if (port != null) {
                            ipAddressUrlBuilder.append(":");
                            ipAddressUrlBuilder.append(lanPort);
                        }
                    }
                    ipAddressUrlBuilder.append("/");
                }
                if (ipAddressUrlBuilder != null) {
                    ipAddressUrl = ipAddressUrlBuilder.toString();
                }
            }
        }
        if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(domain))) {
            boxDomain = domain;
        } else if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(userDomain))) {
            boxDomain = userDomain;
        } else if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(ipAddressUrl))) {
            boxDomain = ipAddressUrl;
        }
        return boxDomain;
    }

    /**
     * todo 后续有通道时删除该方法
     * @param loginInfo
     */
    public void getInternetServiceConfig(LoginInfo loginInfo, boolean isLanGrantee) {
        String boxDomain = null;
        String domain = null;
        String userDomain = null;
        String ipAddressUrl = null;
        String accessToken = null;
        String secret = null;
        String ivParams = null;
        String aoId = null;
        if (loginInfo != null) {
            accessToken = loginInfo.getAccessToken();
            secret = loginInfo.getSecretKey();
            domain = loginInfo.getDomain();
            AlgorithmConfig algorithmConfig = loginInfo.getAlgorithmConfig();
            if (algorithmConfig != null) {
                TransportationConfig transportationConfig = algorithmConfig.getTransportation();
                if (transportationConfig != null) {
                    ivParams = transportationConfig.getInitializationVector();
                }
            }
            BoxLanInfo boxLanInfo = loginInfo.getBoxLanInfo();
            if (boxLanInfo != null) {
                userDomain = boxLanInfo.getUserDomain();
                String lanIp = boxLanInfo.getLanIp();
                String lanPort = boxLanInfo.getPort();
                StringBuilder ipAddressUrlBuilder = null;
                if (lanIp != null) {
                    ipAddressUrlBuilder = new StringBuilder();
                    boolean isIpv6 = false;
                    ipAddressUrlBuilder.append("http://");
                    if (lanIp.contains(":")) {
                        isIpv6 = true;
                    }
                    if (isIpv6) {
                        ipAddressUrlBuilder.append("[");
                    }
                    ipAddressUrlBuilder.append(lanIp);
                    if (isIpv6) {
                        ipAddressUrlBuilder.append("]");
                    }
                    if (lanPort != null) {
                        Integer port = null;
                        try {
                            port = Integer.parseInt(lanPort);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        if (port != null) {
                            ipAddressUrlBuilder.append(":");
                            ipAddressUrlBuilder.append(lanPort);
                        }
                    }
                    ipAddressUrlBuilder.append("/");
                }
                if (ipAddressUrlBuilder != null) {
                    ipAddressUrl = ipAddressUrlBuilder.toString();
                }
            }
            aoId = loginInfo.getAoid();
        }
        if (isLanGrantee) {
            if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(ipAddressUrl))) {
                boxDomain = ipAddressUrl;
            }
        } else if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(domain))) {
            boxDomain = domain;
        } else if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(userDomain))) {
            boxDomain = userDomain;
        }
        if (TextUtils.isEmpty(aoId) || !FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(boxDomain))) {
            if (iView != null) {
                iView.internetServiceConfigCallback(loginInfo);
            }
            return;
        }
        String finalIpAddressUrl = ipAddressUrl;
        String finalDomain = domain;
        String finalUserDomain = userDomain;
        EulixNetUtil.getInternetServiceConfig(DataUtil.getClientUuid(context), aoId, boxDomain, accessToken, secret, ivParams, true, new InternetServiceConfigCallback() {
            @Override
            public void onSuccess(int code, String source, String message, String requestId, InternetServiceConfigResult result) {
                if (result != null) {
                    mAoSpaceAccessBean = new AOSpaceAccessBean();
                    mAoSpaceAccessBean.setLanAccess(result.getEnableLAN());
                    mAoSpaceAccessBean.setP2PAccess(result.getEnableP2P());
                    mAoSpaceAccessBean.setInternetAccess(result.getEnableInternetAccess());
                    mAoSpaceAccessBean.setPlatformApiBase(result.getPlatformApiBase());
                    if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(finalDomain))) {
                        mAoSpaceAccessBean.setUserDomain(finalDomain);
                    } else if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(finalUserDomain))) {
                        mAoSpaceAccessBean.setUserDomain(finalUserDomain);
                    }
                    if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(finalIpAddressUrl))) {
                        mAoSpaceAccessBean.setIpAddressUrl(finalIpAddressUrl);
                    }
                }
                if (iView != null) {
                    iView.internetServiceConfigCallback(loginInfo);
                }
            }

            @Override
            public void onFail(int code, String source, String message, String requestId) {
                if (iView != null) {
                    iView.internetServiceConfigCallback(loginInfo);
                }
            }

            @Override
            public void onError(String errMsg) {
                if (iView != null) {
                    iView.internetServiceConfigCallback(loginInfo);
                }
            }
        });
    }

    public boolean loginDevice(LoginInfo loginInfo, String boxPublicKey) {
        boolean result = false;
        if (loginInfo != null) {
            String boxDomain = null;
            String userDomain = null;
            String ipAddressUrl = null;
            String uuid = loginInfo.getBoxUUID();
            String aoId = loginInfo.getAoid();
            String name = loginInfo.getBoxName();
            String domain = loginInfo.getDomain();
            String accessToken = loginInfo.getAccessToken();
            String secretKey = loginInfo.getSecretKey();
            String expiresAt = loginInfo.getExpiresAt();
            String refreshToken = loginInfo.getRefreshToken();
            String autoLoginExpiresAt = loginInfo.getAutoLoginExpiresAt();
            BoxLanInfo boxLanInfo = loginInfo.getBoxLanInfo();
            if (boxLanInfo != null) {
                userDomain = boxLanInfo.getUserDomain();
                String lanIp = boxLanInfo.getLanIp();
                String lanPort = boxLanInfo.getPort();
                StringBuilder ipAddressUrlBuilder = null;
                if (lanIp != null) {
                    ipAddressUrlBuilder = new StringBuilder();
                    boolean isIpv6 = false;
                    ipAddressUrlBuilder.append("http://");
                    if (lanIp.contains(":")) {
                        isIpv6 = true;
                    }
                    if (isIpv6) {
                        ipAddressUrlBuilder.append("[");
                    }
                    ipAddressUrlBuilder.append(lanIp);
                    if (isIpv6) {
                        ipAddressUrlBuilder.append("]");
                    }
                    if (lanPort != null) {
                        Integer port = null;
                        try {
                            port = Integer.parseInt(lanPort);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        if (port != null) {
                            ipAddressUrlBuilder.append(":");
                            ipAddressUrlBuilder.append(lanPort);
                        }
                    }
                    ipAddressUrlBuilder.append("/");
                }
                if (ipAddressUrlBuilder != null) {
                    ipAddressUrl = ipAddressUrlBuilder.toString();
                }
            }
            // todo 后续需要通过新增字段来判断通道问题
            boolean isHandle = false;
            if (mAoSpaceAccessBean != null) {
                isHandle = true;
                Boolean isInternetAccess = mAoSpaceAccessBean.getInternetAccess();
                Boolean isLanAccess = mAoSpaceAccessBean.getLanAccess();
                if (isInternetAccess != null && isInternetAccess) {
                    boxDomain = mAoSpaceAccessBean.getUserDomain();
                } else if (isLanAccess != null && isLanAccess) {
                    boxDomain = mAoSpaceAccessBean.getIpAddressUrl();
                } else {
                    isHandle = false;
                }
            }
            if (!isHandle) {
                if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(domain))) {
                    boxDomain = domain;
                } else if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(userDomain))) {
                    boxDomain = userDomain;
                } else if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(ipAddressUrl))) {
                    boxDomain = ipAddressUrl;
                }
            }
            if (!StringUtil.isNonBlankString(boxPublicKey)) {
                String publicKey = loginInfo.getBoxPublicKey();
                if (publicKey != null) {
                    String nPublicKey = null;
                    try {
                        nPublicKey = StringUtil.unwrapPublicKey(URLDecoder.decode(publicKey, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (nPublicKey == null) {
                        nPublicKey = StringUtil.unwrapPublicKey(publicKey);
                    }
                    if (StringUtil.isNonBlankString(nPublicKey)) {
                        boxPublicKey = nPublicKey;
                    }
                }
            }
            long autoLoginExpire = System.currentTimeMillis();
            long autoLoginExpireTimestamp = -1L;
            if (autoLoginExpiresAt != null) {
                autoLoginExpireTimestamp = FormatUtil.parseFileApiTimestamp(autoLoginExpiresAt, ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
            }
            if (autoLoginExpireTimestamp >= 0) {
                autoLoginExpire = autoLoginExpireTimestamp;
            } else if (loginInfo.isAutoLogin()) {
                autoLoginExpire += (15 * 24 * 60 * 60 * 1000);
            }
            AlgorithmConfig algorithmConfig = loginInfo.getAlgorithmConfig();
            if (uuid != null && FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(boxDomain)) && !TextUtils.isEmpty(aoId)) {
                result = true;
                EulixSpaceDBUtil.readAppointPush(context, uuid, aoId, true);
                long currentTimestamp = System.currentTimeMillis();
                long expireTimestamp = FormatUtil.parseFileApiTimestamp(expiresAt
                        , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
                List<Map<String, String>> activeBoxValues = EulixSpaceDBUtil.queryBox(context
                        , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
                if (activeBoxValues != null) {
                    for (Map<String, String> activeBoxValue : activeBoxValues) {
                        if (activeBoxValue != null && activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                                && activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                            String activeBoxUuid = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                            String activeBoxBind = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                            Integer activeAlarmId = DataUtil.getTokenAlarmId(activeBoxUuid, activeBoxBind);
                            if (activeAlarmId != null) {
                                AlarmUtil.cancelAlarm(context, activeAlarmId);
                            }
                            Map<String, String> requestUseBoxValue = new HashMap<>();
                            requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, activeBoxUuid);
                            requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, activeBoxBind);
                            boolean isBind = ("1".equals(activeBoxBind) || "-1".equals(activeBoxBind));
                            requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(isBind
                                    ? ConstantField.EulixDeviceStatus.REQUEST_USE : ConstantField.EulixDeviceStatus.REQUEST_LOGIN));
                            EulixSpaceDBUtil.updateBox(context, requestUseBoxValue);
                        }
                    }
                }
                List<Map<String, String>> offlineUseBoxValues = EulixSpaceDBUtil.queryBox(context
                        , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
                if (offlineUseBoxValues != null) {
                    for (Map<String, String> offlineUseBox : offlineUseBoxValues) {
                        if (offlineUseBox != null && offlineUseBox.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                                && offlineUseBox.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                            String offlineUseBoxUuid = offlineUseBox.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                            String offlineUseBoxBind = offlineUseBox.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                            Map<String, String> offlineBoxValue = new HashMap<>();
                            offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, offlineUseBoxUuid);
                            offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, offlineUseBoxBind);
                            offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE));
                            EulixSpaceDBUtil.updateBox(context, offlineBoxValue);
                        }
                    }
                }
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, uuid);
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, aoId);
                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
                Map<String, String> boxValue = new HashMap<>();
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, uuid);
                if (name != null) {
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_NAME, name);
                }
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, boxDomain);
                EulixBoxToken boxToken = new EulixBoxToken();
                EulixBoxTokenDetail eulixBoxTokenDetail = new EulixBoxTokenDetail();
                eulixBoxTokenDetail.setBoxUuid(uuid);
                eulixBoxTokenDetail.setBoxBind(aoId);
                if (accessToken != null) {
                    boxToken.setAccessToken(accessToken);
                    eulixBoxTokenDetail.setAccessToken(accessToken);
                }
                if (secretKey != null) {
                    boxToken.setSecretKey(secretKey);
                    eulixBoxTokenDetail.setSecretKey(secretKey);
                }
                if (!TextUtils.isEmpty(expiresAt)) {
                    boxToken.setTokenExpire(expireTimestamp);
                    eulixBoxTokenDetail.setTokenExpire(expireTimestamp);
                }
                if (refreshToken != null) {
                    boxToken.setRefreshToken(refreshToken);
                    eulixBoxTokenDetail.setRefreshToken(refreshToken);
                }
                if (algorithmConfig != null) {
                    TransportationConfig transportationConfig = algorithmConfig.getTransportation();
                    if (transportationConfig != null) {
                        boxToken.setTransformation(transportationConfig.getTransformation());
                        boxToken.setInitializationVector(transportationConfig.getInitializationVector());
                        eulixBoxTokenDetail.setTransformation(transportationConfig.getTransformation());
                        eulixBoxTokenDetail.setInitializationVector(transportationConfig.getInitializationVector());
                    }
                }
                // todo 免扫码登录
                boxToken.setLoginValid(String.valueOf(autoLoginExpire));
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_TOKEN, new Gson().toJson(boxToken, EulixBoxToken.class));
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
                boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, aoId);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, boxPublicKey);

                // todo 新通道换成数据内的
                if (mAoSpaceAccessBean != null) {
                    EulixBoxInfo eulixBoxInfo = new EulixBoxInfo();
                    eulixBoxInfo.setAoSpaceAccessBean(mAoSpaceAccessBean);
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                }

                if (boxValues == null || boxValues.size() <= 0) {
                    EulixSpaceDBUtil.insertBox(context.getApplicationContext(), boxValue, 0);
                    BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(uuid, aoId, true);
                    EventBusUtil.post(boxInsertDeleteEvent);
                } else {
                    EulixSpaceDBUtil.updateBox(context.getApplicationContext(), boxValue);
                }
                DataUtil.setLastBoxToken(eulixBoxTokenDetail);
                DataUtil.setLastEulixSpace(context, uuid, aoId);
                Integer boxAlarmId = DataUtil.getTokenAlarmId(uuid, aoId);
                if (boxAlarmId != null) {
                    AlarmUtil.cancelAlarm(context, boxAlarmId);
                }

                // 免扫码登录
                boolean isAutoLogin = (autoLoginExpire > System.currentTimeMillis());
                if (isAutoLogin) {
                    int alarmId = AlarmUtil.getAlarmId();
                    DataUtil.setTokenAlarmId(uuid, aoId, alarmId);
                    long diffTimestamp = 60000L;
                    long realExpireTimestamp = (expireTimestamp < 0 ? autoLoginExpire : Math.min(autoLoginExpire, expireTimestamp));
                    if (realExpireTimestamp > currentTimestamp) {
                        AlarmUtil.setAlarm(context, realExpireTimestamp, alarmId, uuid, aoId, (diffTimestamp / 2));
                    } else {
                        AlarmUtil.setAlarm(context, (currentTimestamp + diffTimestamp), alarmId, uuid, aoId, (diffTimestamp / 2));
                    }
                }

                EulixSpaceDBUtil.offlineTemperateBox(context, uuid, aoId);
                EventBusUtil.post(new BoxOnlineRequestEvent(true));
                EventBusUtil.post(new SpaceChangeEvent(true));
            }
        }
        return result;
    }
}
