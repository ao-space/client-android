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

package xyz.eulix.space.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.event.LanStatusEvent;
import xyz.eulix.space.event.NetworkStateEvent;
import xyz.eulix.space.event.TransferListNetworkEvent;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;

/**
 * Author:      Zhu Fuyu
 * Description: 网络状态监听
 * History:     2021/10/9
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private int changeTime = 0;
    private String lastNetworkType;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            Logger.d("zfy", "network change action:" + intent.getAction());
            String currentNetworkType = NetUtils.getNetworkType(EulixSpaceApplication.getContext());
            if (currentNetworkType.equals(lastNetworkType)) {
                Logger.d("zfy", "network type not change");
                return;
            }
            lastNetworkType = currentNetworkType;
            changeTime++;
            Logger.d("zfy", "networkChangeTime = " + changeTime);
            if (changeTime <= 1) {
                return;
            }
            Logger.d("zfy", "network change:" + NetUtils.getNetworkType(context));

            if (!NetUtils.isNetAvailable(context)) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    ((EulixSpaceApplication) EulixSpaceApplication.getContext()).showNetworkDisconnect();
                }, 5000);
            }

            if (NetUtils.isWifiConnected(context)) {
                Logger.d("zfy", "wifi connect");
                EventBusUtil.post(new NetworkStateEvent());
                EventBusUtil.post(new TransferListNetworkEvent());
                //wifi连接，刷新局域网状态
                new Handler(Looper.getMainLooper()).post(() -> {
                    LanManager.getInstance().startPollCheckTask();
                });
                ConstantField.sIAllowTransferWithMobileData = false;
            } else if (NetUtils.isMobileNetWork(context)) {
                //wifi断开，切换为手机流量
                LanManager.getInstance().setLanEnable(false);
                EventBusUtil.post(new LanStatusEvent(false));
                if (TransferTaskManager.getInstance().getTransferringCountWithoutFailed() > 0) {
                    ((EulixSpaceApplication) EulixSpaceApplication.getContext()).confirmTransferMobileData();
                }
            } else {
                //网络断开
                LanManager.getInstance().setLanEnable(false);
                EventBusUtil.post(new LanStatusEvent(false));
            }
        }
    }
}
