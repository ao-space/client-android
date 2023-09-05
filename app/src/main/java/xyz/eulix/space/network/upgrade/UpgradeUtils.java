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

package xyz.eulix.space.network.upgrade;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.interfaces.ResultWithNullCallback;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * Author:      Zhu Fuyu
 * Description: 盒子系统升级相关
 * History:     2021/11/8
 */
public class UpgradeUtils {
    private static GatewayCommunicationBase lastGatewayCommunicationBase;

    //获取系统自动升级配置
    public static void getSystemAutoUpgradeConfig(Context context, IGetUpgradeConfigCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            try {
                ThreadPool.getInstance().execute(() -> UpgradeManager.getSystemAutoUpgradeConfig(
                        baseUrl, gatewayCommunicationBase.getAccessToken(),
                        gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(),
                        gatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0, callback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    //设置系统自动升级配置
    public static void setSystemAutoUpgradeConfig(Context context, boolean autoDownload, boolean autoInstall, @NonNull ResultWithNullCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            try {
                ThreadPool.getInstance().execute(() -> UpgradeManager.setSystemAutoUpgradeConfig(autoDownload, autoInstall,
                        baseUrl, gatewayCommunicationBase.getAccessToken(),
                        gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(),
                        gatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0, callback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else {
            callback.onResult(false, "");
        }

    }

    //获取系统自动升级配置
    public static void checkUpgradeStatus(Context context, ICheckUpgradeStatusCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            lastGatewayCommunicationBase = gatewayCommunicationBase;
            String baseUrl = Urls.getBaseUrl();
            try {
                ThreadPool.getInstance().execute(() -> UpgradeManager.checkUpgradeStatus(
                        baseUrl, gatewayCommunicationBase.getAccessToken(),
                        gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(),
                        gatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0, callback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else if (lastGatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            try {
                ThreadPool.getInstance().execute(() -> UpgradeManager.checkUpgradeStatus(
                        baseUrl, lastGatewayCommunicationBase.getAccessToken(),
                        lastGatewayCommunicationBase.getSecretKey(), lastGatewayCommunicationBase.getTransformation(),
                        lastGatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0, callback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }

    }

    //开始系统升级
    public static void startUpgrade(Context context, boolean isPull, String versionId, ResultWithNullCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            try {
                ThreadPool.getInstance().execute(() -> UpgradeManager.startUpgrade(context, isPull, versionId,
                        baseUrl, gatewayCommunicationBase.getAccessToken(),
                        gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(),
                        gatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0, callback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void getDeviceVersionDetailInfo(Context context, ResultCallbackObj callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            ThreadPool.getInstance().execute(() -> UpgradeManager.getDeviceVersionInfo(context, baseUrl, gatewayCommunicationBase.getAccessToken(),
                    gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(),
                    gatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0, callback));

        } else {
            if (callback != null){
                callback.onError("gatewayCommunicationBase is null");
            }
        }
    }
}
