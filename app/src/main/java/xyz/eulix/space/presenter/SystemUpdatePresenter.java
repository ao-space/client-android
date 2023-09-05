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

import android.text.TextUtils;

import java.util.Timer;
import java.util.TimerTask;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxVersionCheckEvent;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.interfaces.ResultWithNullCallback;
import xyz.eulix.space.network.gateway.GatewayUtil;
import xyz.eulix.space.network.gateway.IVersionCheckCallback;
import xyz.eulix.space.network.gateway.VersionCheckResponseBody;
import xyz.eulix.space.network.upgrade.ICheckUpgradeStatusCallback;
import xyz.eulix.space.network.upgrade.IGetUpgradeConfigCallback;
import xyz.eulix.space.network.upgrade.UpgradeStatusResponseBody;
import xyz.eulix.space.network.upgrade.UpgradeUtils;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PreferenceUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 系统升级Presenter
 * History:     2021/10/26
 */
public class SystemUpdatePresenter extends AbsPresenter<SystemUpdatePresenter.ISystemUpdate> {

    private Timer checkInfoTimer;
    private TimerTask pollingTask;
    private static final long POLLING_INTERVAL_TIME = 5000;    //轮询间隔 ms

    //升级状态 0：初始状态；1：已开始升级；2:已开始轮询（可调通接口）；3：容器正在重启（无法调通接口）
    public static final int STATE_NORMAL = 0;
    public static final int STATE_UPGRADING = 1;
    public static final int STATE_POLLING = 2;
    public static final int STATE_DOCKER_RESTART = 3;
    public static int sUpgradeState = STATE_NORMAL;

    //第一次查询失败的时间
    private long mFirstCheckErrorTime = -1;
    //重启查询限制时间
    private static final long RESTARTING_CHECK_LIMIT_TIME = 10 * 60 * 1000;

    public interface ISystemUpdate extends IBaseView {
        void refreshConfigState(boolean isOpen);

        void onCheckInfoResult(Boolean result, String status);

        void onCheckVersionError();

        void onGetCurrentVersion(String currentVersion);
    }

    public boolean isSupportSystemUpdate() {
        boolean isSupport = true;
        DeviceAbility deviceAbility = EulixSpaceDBUtil.getActiveDeviceAbility(context, false);
        if (deviceAbility != null) {
            Boolean isUpgradeApiSupport = deviceAbility.getUpgradeApiSupport();
            if (isUpgradeApiSupport != null) {
                isSupport = isUpgradeApiSupport;
            }
        }
        return isSupport;
    }

    //检查盒子系统版本更新
    public void checkBoxVersion(ResultCallbackObj callback) {
        GatewayUtil.checkVersionBoxOrApp(context, true, new IVersionCheckCallback() {
            @Override
            public void onResult(VersionCheckResponseBody responseBody) {
                if (responseBody != null && responseBody.results != null) {
                    VersionCheckResponseBody.Results results = responseBody.results;
                    Logger.d("zfy", "result newVersionExist:" + results.newVersionExist);

                    if (results.newVersionExist && results.latestBoxPkg != null) {
                        ConstantField.boxVersionCheckBody = results;
                        EventBusUtil.post(new BoxVersionCheckEvent());
                        callback.onResult(true, null);
                        return;
                    }
                } else if (responseBody != null && responseBody.getCodeInt() == ConstantField.ErrorCode.PRODUCT_PLATFORM_CONNECT_ERROR) {
                    ConstantField.boxVersionCheckBody = null;
                    EventBusUtil.post(new BoxVersionCheckEvent());
                    callback.onError(String.valueOf(ConstantField.ErrorCode.PRODUCT_PLATFORM_CONNECT_ERROR));
                    return;
                }
                ConstantField.boxVersionCheckBody = null;
                EventBusUtil.post(new BoxVersionCheckEvent());
                callback.onResult(false, null);
            }

            @Override
            public void onError(String msg) {
                Logger.d("zfy", "checkVersion error:" + msg);
                ConstantField.boxVersionCheckBody = null;
                EventBusUtil.post(new BoxVersionCheckEvent());
                iView.onCheckVersionError();
            }
        });
    }

    //获取自动升级配置
    public void getAutoUpgradeConfig() {
        UpgradeUtils.getSystemAutoUpgradeConfig(context, new IGetUpgradeConfigCallback() {
            @Override
            public void onResult(boolean autoDownload, boolean autoInstall) {
                Logger.d("zfy", "autoDownload = " + autoDownload + ",autoInstall = " + autoInstall);
                PreferenceUtil.saveUpgradeAutoDownload(context, autoDownload);
                PreferenceUtil.saveUpgradeAutoInstall(context, autoInstall);
                iView.refreshConfigState(autoDownload || autoInstall);
            }

            @Override
            public void onError(String msg) {
                Logger.d("zfy", "get system auto upgrade config error:" + msg);
            }
        });
    }

    public void getCurrentBoxVersion() {
        GatewayUtil.getCurrentBoxVersion(context, (result, extraMsg) -> {
            if (result && !TextUtils.isEmpty(extraMsg)) {
                Logger.d("zfy", "get current box version:" + extraMsg);
                iView.onGetCurrentVersion(extraMsg);
            }
        });
    }

    //查询升级状态
    public void checkUpgradeStatusOnce(ResultCallback callback) {
        UpgradeUtils.checkUpgradeStatus(context, new ICheckUpgradeStatusCallback() {
            @Override
            public void onResult(UpgradeStatusResponseBody upgradeStatusResponseBody) {
                Logger.d("zfy", "check upgrade status:" + upgradeStatusResponseBody.status);
                if (!TextUtils.isEmpty(upgradeStatusResponseBody.status)) {
                    callback.onResult(true, upgradeStatusResponseBody.status);
                } else {
                    callback.onResult(false, "");
                }
            }

            @Override
            public void onError(String msg) {
                callback.onResult(false, "");
            }
        });
    }

    //查询升级状态
    public void checkUpgradeStatus() {
        UpgradeUtils.checkUpgradeStatus(context, new ICheckUpgradeStatusCallback() {
            @Override
            public void onResult(UpgradeStatusResponseBody upgradeStatusResponseBody) {
                Logger.d("zfy", "check upgrade status:" + upgradeStatusResponseBody.status);
                if (sUpgradeState == STATE_NORMAL) {
                    //方式重复接收
                    return;
                } else if (sUpgradeState == STATE_UPGRADING) {
                    sUpgradeState = STATE_POLLING;
                }
                if (!TextUtils.isEmpty(upgradeStatusResponseBody.status)) {
                    if (upgradeStatusResponseBody.status.equals(ConstantField.UpgradeStatus.STATUS_UPPED)
                            || upgradeStatusResponseBody.status.equals(ConstantField.UpgradeStatus.STATUS_UP_ERR)
                            || upgradeStatusResponseBody.status.equals(ConstantField.UpgradeStatus.STATUS_PULL_ERR)) {
                        sUpgradeState = STATE_NORMAL;
                        cancelPollingCheck();
                        mFirstCheckErrorTime = -1;
                    }
                    iView.onCheckInfoResult(true, upgradeStatusResponseBody.status);
                } else {
                    cancelPollingCheck();
                    iView.onCheckInfoResult(false, "");
                }
            }

            @Override
            public void onError(String msg) {
                Logger.d("zfy", "check upgrade status error:" + msg);
                if (sUpgradeState >= STATE_POLLING && !TextUtils.isEmpty(msg)) {
                    if (msg.equals("closed")) {
                        //开始正常轮询，网络断开，说明镜像开始重启，延迟轮询时间
                        if (sUpgradeState == STATE_POLLING) {
                            sUpgradeState = STATE_DOCKER_RESTART;
                            Logger.d("zfy", "docker begin restart");
                            if (mFirstCheckErrorTime == -1) {
                                //记录开始重启时间
                                mFirstCheckErrorTime = System.currentTimeMillis();
                            }
                        } else if (sUpgradeState == STATE_DOCKER_RESTART) {
                            Logger.d("zfy", "docker is restarting");
                            if (mFirstCheckErrorTime > -1 && System.currentTimeMillis() - mFirstCheckErrorTime > RESTARTING_CHECK_LIMIT_TIME) {
                                Logger.d("zfy", "restart check over limit time!stop task");
                                mFirstCheckErrorTime = -1;
                                cancelPollingCheck();
                                iView.onCheckInfoResult(false, "");
                            }
                        }
                    } else {
                        Logger.d("zfy", "check error:" + msg);
                    }
                } else {
                    cancelPollingCheck();
                    iView.onCheckInfoResult(null, "");
                }
            }
        });
    }

    //开始升级
    public void startUpgrade(boolean isPull) {
        iView.onCheckInfoResult(true, isPull ? ConstantField.UpgradeStatus.STATUS_PULLING : ConstantField.UpgradeStatus.STATUS_UPPING);
        UpgradeUtils.startUpgrade(context, isPull, ConstantField.boxVersionCheckBody.latestBoxPkg.pkgVersion, new ResultWithNullCallback() {
            @Override
            public void onResult(Boolean result, String extraMsg) {
                if (result != null && result) {
                    Logger.d("zfy", "start upgrade success");
                    if (sUpgradeState < STATE_UPGRADING) {
                        sUpgradeState = STATE_UPGRADING;
                    }
                    starPollingCheckTask();
                } else {
                    iView.onCheckInfoResult(result, extraMsg);
                }
            }
        });
    }

    //开启轮询，查询任务状态及进度
    public void starPollingCheckTask() {
        cancelPollingCheck();
        checkInfoTimer = new Timer();
        pollingTask = new TimerTask() {
            @Override
            public void run() {
                checkUpgradeStatus();
            }
        };
        checkInfoTimer.schedule(pollingTask, 0L, POLLING_INTERVAL_TIME);
    }

    //取消轮询
    public void cancelPollingCheck() {
        Logger.d("zfy", "cancelPollingCheck");
        if (pollingTask != null) {
            pollingTask.cancel();
            pollingTask = null;
        }
        if (checkInfoTimer != null) {
            checkInfoTimer.cancel();
            checkInfoTimer = null;
        }
    }

}
