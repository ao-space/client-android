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

package xyz.eulix.space.bridge;

import java.util.List;

import xyz.eulix.space.abs.AbsBridge;
import xyz.eulix.space.bean.WLANItem;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.network.agent.net.NetworkAdapter;
import xyz.eulix.space.network.agent.net.NetworkStatusResult;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/28 15:41
 */
public class DistributeNetworkBridge extends AbsBridge {
    public static final int PROGRESS_PAIR = 0;
    public static final int PROGRESS_INITIALIZE = PROGRESS_PAIR + 1;
    public static final int PROGRESS_BIND_COMMUNICATION_PROGRESS = PROGRESS_INITIALIZE + 1;
    public static final int PROGRESS_BIND_SPACE_CREATE = PROGRESS_BIND_COMMUNICATION_PROGRESS + 1;
    public static final int PROGRESS_SPACE_READY_CHECK = PROGRESS_BIND_SPACE_CREATE + 1;
    public static final int PROGRESS_DISK_RECOGNITION = PROGRESS_SPACE_READY_CHECK + 1;
    public static final int PROGRESS_DISK_MANAGEMENT_LIST = PROGRESS_DISK_RECOGNITION + 1;
    private static final DistributeNetworkBridge INSTANCE = new DistributeNetworkBridge();

    public interface DistributeNetworkSourceCallback extends SourceCallback {
        void requestWlanList();
        void requestAccessNetwork();
        void setNetworkConfig(String dns1, String dns2, String ipv6DNS1, String ipv6DNS2, List<NetworkAdapter> networkAdapters);
        void ignoreNetworkConfig(String wifiName);
        void distributeWlan(String ssid, String address, String password);
        void handlePairing();
        void handleInitializeProgress();
        void handleSpaceReadyCheckProgress();
        void distributeNetworkCallback();
        void handleBindResult(boolean isHandle);
    }

    public interface DistributeNetworkSinkCallback extends SinkCallback {
        void handleWlanListResult(List<WLANItem> wlanItemList);
        void handleAccessNetworkResult(int code, String source, NetworkStatusResult networkStatusResult);
        void handleSetNetworkConfigResult(int code, String source);
        void handleIgnoreNetworkConfigResult(int code, String source);
        void handleDistributionResult(String ssid, List<String> ipAddresses, boolean isSuccess);
        void pairingResultCallback(boolean isSuccess, Integer code);
        void handleSpaceReadyCheck(int code, String source, ReadyCheckResult result);
        void handleProgressState(int state);
        void handleDisconnect();
    }

    private DistributeNetworkBridge() {}

    public static DistributeNetworkBridge getInstance() {
        return INSTANCE;
    }

    public void getWlanList() {
        if (mSourceCallback != null && mSourceCallback instanceof DistributeNetworkSourceCallback) {
            ((DistributeNetworkSourceCallback) mSourceCallback).requestWlanList();
        }
    }

    public void getAccessNetwork() {
        if (mSourceCallback != null && mSourceCallback instanceof DistributeNetworkSourceCallback) {
            ((DistributeNetworkSourceCallback) mSourceCallback).requestAccessNetwork();
        }
    }

    public void setNetworkConfig(String dns1, String dns2, String ipv6DNS1, String ipv6DNS2, List<NetworkAdapter> networkAdapters) {
        if (mSourceCallback != null && mSourceCallback instanceof DistributeNetworkSourceCallback) {
            ((DistributeNetworkSourceCallback) mSourceCallback).setNetworkConfig(dns1, dns2, ipv6DNS1, ipv6DNS2, networkAdapters);
        }
    }

    public void ignoreNetworkConfig(String wifiName) {
        if (mSourceCallback != null && mSourceCallback instanceof DistributeNetworkSourceCallback) {
            ((DistributeNetworkSourceCallback) mSourceCallback).ignoreNetworkConfig(wifiName);
        }
    }

    public void selectWlan(String ssid, String address, String password) {
        if (mSourceCallback != null && mSourceCallback instanceof DistributeNetworkSourceCallback) {
            ((DistributeNetworkSourceCallback) mSourceCallback).distributeWlan(ssid, address, password);
        }
    }

    public void pairing() {
        if (mSourceCallback != null && mSourceCallback instanceof DistributeNetworkSourceCallback) {
            ((DistributeNetworkSourceCallback) mSourceCallback).handlePairing();
        }
    }

    public void initializeProgress() {
        if (mSourceCallback != null && mSourceCallback instanceof DistributeNetworkSourceCallback) {
            ((DistributeNetworkSourceCallback) mSourceCallback).handleInitializeProgress();
        }
    }

    public void spaceReadyCheckProgress() {
        if (mSourceCallback != null && mSourceCallback instanceof DistributeNetworkSourceCallback) {
            ((DistributeNetworkSourceCallback) mSourceCallback).handleSpaceReadyCheckProgress();
        }
    }

    public void handleDistributeNetwork() {
        if (mSourceCallback != null && mSourceCallback instanceof DistributeNetworkSourceCallback) {
            ((DistributeNetworkSourceCallback) mSourceCallback).distributeNetworkCallback();
        }
    }

    public void handleBindResult(boolean isHandle) {
        if (mSourceCallback != null && mSourceCallback instanceof DistributeNetworkSourceCallback) {
            ((DistributeNetworkSourceCallback) mSourceCallback).handleBindResult(isHandle);
        }
    }

    public void setWlanList(List<WLANItem> wlanItemList) {
        if (mSinkCallback != null && mSinkCallback instanceof DistributeNetworkSinkCallback) {
            ((DistributeNetworkSinkCallback) mSinkCallback).handleWlanListResult(wlanItemList);
        }
    }

    public void setAccessNetwork(int code, String source, NetworkStatusResult networkStatusResult) {
        if (mSinkCallback != null && mSinkCallback instanceof DistributeNetworkSinkCallback) {
            ((DistributeNetworkSinkCallback) mSinkCallback).handleAccessNetworkResult(code, source, networkStatusResult);
        }
    }

    public void handleSetNetworkConfigResult(int code, String source) {
        if (mSinkCallback != null && mSinkCallback instanceof DistributeNetworkSinkCallback) {
            ((DistributeNetworkSinkCallback) mSinkCallback).handleSetNetworkConfigResult(code, source);
        }
    }

    public void handleIgnoreNetworkConfigResult(int code, String source) {
        if (mSinkCallback != null && mSinkCallback instanceof DistributeNetworkSinkCallback) {
            ((DistributeNetworkSinkCallback) mSinkCallback).handleIgnoreNetworkConfigResult(code, source);
        }
    }

    public void distributeNetworkResult(String ssid, List<String> ipAddresses, boolean isSuccess) {
        if (mSinkCallback != null && mSinkCallback instanceof DistributeNetworkSinkCallback) {
            ((DistributeNetworkSinkCallback) mSinkCallback).handleDistributionResult(ssid, ipAddresses, isSuccess);
        }
    }

    public void pairingResult(boolean isSuccess, Integer code) {
        if (mSinkCallback != null && mSinkCallback instanceof DistributeNetworkSinkCallback) {
            ((DistributeNetworkSinkCallback) mSinkCallback).pairingResultCallback(isSuccess, code);
        }
    }

    public void spaceReadyCheck(int code, String source, ReadyCheckResult result) {
        if (mSinkCallback != null && mSinkCallback instanceof DistributeNetworkSinkCallback) {
            ((DistributeNetworkSinkCallback) mSinkCallback).handleSpaceReadyCheck(code, source, result);
        }
    }

    public void progressStateChange(int state) {
        if (mSinkCallback != null && mSinkCallback instanceof DistributeNetworkSinkCallback) {
            ((DistributeNetworkSinkCallback) mSinkCallback).handleProgressState(state);
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof DistributeNetworkSinkCallback) {
            ((DistributeNetworkSinkCallback) mSinkCallback).handleDisconnect();
        }
    }
}
