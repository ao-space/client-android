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

package xyz.eulix.space.util;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.EulixBoxTokenDetail;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.agent.DeviceUtil;
import xyz.eulix.space.network.agent.LocalIpInfoCallback;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/27 11:37
 */
public class GatewayUtils {
    private static final String TAG = GatewayUtils.class.getSimpleName();

    private GatewayUtils() {
        throw new AssertionError("not allow to be instantiation!");
    }

    private static GatewayCommunicationBase generateGatewayCommunication(List<Map<String, String>> boxValues) {
        GatewayCommunicationBase gatewayCommunicationBase = null;
        String boxUuid = null;
        String boxBind = null;
        String accessToken = null;
        String secretKey = null;
        String boxDomain = null;
        String boxToken = null;
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null) {
                    boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    boxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                    boxToken = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                    break;
                }
            }
        }
        if (boxUuid != null && !TextUtils.isEmpty(boxUuid) && boxBind != null && !TextUtils.isEmpty(boxBind) && !TextUtils.isEmpty(boxDomain) && !TextUtils.isEmpty(boxToken)) {
            Long tokenExpire = null;
            EulixBoxToken eulixBoxToken = null;
            if (boxToken != null) {
                try {
                    eulixBoxToken = new Gson().fromJson(boxToken, EulixBoxToken.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
            String transformation = null;
            String ivParams = null;
            if (eulixBoxToken != null) {
                tokenExpire = eulixBoxToken.getTokenExpire();
                long currentTimestamp = System.currentTimeMillis();
                Logger.i(TAG, "current date: " + FormatUtil.formatTime(currentTimestamp, ConstantField.TimeStampFormat.EMAIL_FORMAT)
                        + ", expire date: " + FormatUtil.formatTime(tokenExpire, ConstantField.TimeStampFormat.EMAIL_FORMAT));
                if (tokenExpire <= currentTimestamp) {
                    return null;
                }
                accessToken = eulixBoxToken.getAccessToken();
                secretKey = eulixBoxToken.getSecretKey();
                if (accessToken == null || secretKey == null) {
                    return null;
                }
                transformation = eulixBoxToken.getTransformation();
                ivParams = eulixBoxToken.getInitializationVector();
            }
            if (transformation == null) {
                transformation = ConstantField.Algorithm.Transformation.AES_CBC_PKCS5;
            }
            gatewayCommunicationBase = new GatewayCommunicationBase();
            gatewayCommunicationBase.setBoxUuid(boxUuid);
            gatewayCommunicationBase.setBoxBind(boxBind);
            gatewayCommunicationBase.setAccessToken(accessToken);
            gatewayCommunicationBase.setSecretKey(secretKey);
            gatewayCommunicationBase.setBoxDomain(boxDomain);
            gatewayCommunicationBase.setTokenExpire(tokenExpire);
            gatewayCommunicationBase.setTransformation(transformation);
            gatewayCommunicationBase.setIvParams(ivParams);
        }
        return gatewayCommunicationBase;
    }

    /**
     * 访问当前在线使用的盒子信息，如果不想支持离线使用（特殊使用场景），使用generateGatewayCommunication(context, false)
     * @param context
     * @return 盒子网关所用信息，盒子离线或者token失效为null
     */
    public static GatewayCommunicationBase generateGatewayCommunication(Context context) {
        return generateGatewayCommunication(context, true);
    }

    public static GatewayCommunicationBase generateGatewayCommunication(Context context, boolean isSupportOffline) {
        GatewayCommunicationBase gatewayCommunicationBase = null;
        if (context != null) {
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if ((boxValues == null || boxValues.size() <= 0) && isSupportOffline) {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            gatewayCommunicationBase = generateGatewayCommunication(boxValues);
        }
        return gatewayCommunicationBase;
    }

    public static GatewayCommunicationBase generateGatewayCommunication(Context context, String boxUuid, String boxBind) {
        GatewayCommunicationBase gatewayCommunicationBase = null;
        if (context != null && boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            gatewayCommunicationBase = generateGatewayCommunication(EulixSpaceDBUtil.queryBox(context, queryMap));
        } else {
            gatewayCommunicationBase = generateGatewayCommunication(context);
        }
        return gatewayCommunicationBase;
    }

    public static GatewayCommunicationBase generateLastGatewayCommunication(Context context) {
        GatewayCommunicationBase gatewayCommunicationBase = null;
        if (context != null) {
            EulixSpaceInfo eulixSpaceInfo = DataUtil.getLastEulixSpace(context);
            if (eulixSpaceInfo != null) {
                String boxUuid = eulixSpaceInfo.getBoxUuid();
                String boxBind = eulixSpaceInfo.getBoxBind();
                if (boxUuid != null && boxBind != null) {
                    String boxDomain = null;
                    String boxToken = null;
                    Map<String, String> queryMap = new HashMap<>();
                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                    List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext(), queryMap);
                    if (boxValues != null) {
                        for (Map<String, String> boxValue : boxValues) {
                            if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN) && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                                boxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                                boxToken = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                                break;
                            }
                        }
                    }
                    if (boxDomain != null && boxToken != null) {
                        EulixBoxToken eulixBoxToken = null;
                        try {
                            eulixBoxToken = new Gson().fromJson(boxToken, EulixBoxToken.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (eulixBoxToken != null) {
                            String accessToken = eulixBoxToken.getAccessToken();
                            String secretKey = eulixBoxToken.getSecretKey();
                            String transformation = eulixBoxToken.getTransformation();
                            String ivParams = eulixBoxToken.getInitializationVector();
                            if (transformation == null) {
                                transformation = ConstantField.Algorithm.Transformation.AES_CBC_PKCS5;
                            }
                            gatewayCommunicationBase = new GatewayCommunicationBase();
                            gatewayCommunicationBase.setBoxUuid(boxUuid);
                            gatewayCommunicationBase.setBoxBind(boxBind);
                            gatewayCommunicationBase.setAccessToken(accessToken);
                            gatewayCommunicationBase.setSecretKey(secretKey);
                            gatewayCommunicationBase.setBoxDomain(boxDomain);
                            gatewayCommunicationBase.setTransformation(transformation);
                            gatewayCommunicationBase.setIvParams(ivParams);
                        }
                    }
                }
            }
        }
        return gatewayCommunicationBase;
    }
}
