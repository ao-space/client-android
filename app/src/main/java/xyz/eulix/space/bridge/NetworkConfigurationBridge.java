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

import xyz.eulix.space.abs.AbsBridge;
import xyz.eulix.space.bean.NetworkConfigDNSInfo;
import xyz.eulix.space.network.agent.net.NetworkAdapter;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/5 9:59
 */
public class NetworkConfigurationBridge extends AbsBridge {
    private static final NetworkConfigurationBridge INSTANCE = new NetworkConfigurationBridge();

    public interface NetworkConfigurationSourceCallback extends SourceCallback {
        void handleDestroy();
        void networkConfigurationSetWifi(String ssid, String address, String password, NetworkConfigDNSInfo tempNetworkConfigDNSInfo, NetworkAdapter tempNetworkAdapter);
        void networkConfigurationIgnoreWifi(String ssid, String address);
    }

    public interface NetworkConfigurationSinkCallback extends SinkCallback {
        NetworkAdapter getCurrentNetworkAdapter();
        void handleRefreshAccessNetwork(NetworkConfigDNSInfo networkConfigDNSInfo, NetworkAdapter networkAdapter);
        void handleDisconnect();
        void handleNetworkConfigurationSetWifi(int code, String source);
        void handleNetworkConfigurationIgnoreWifi(int code, String source);
    }

    private NetworkConfigurationBridge() {}

    public static NetworkConfigurationBridge getInstance() {
        return INSTANCE;
    }

    public void handleDestroy() {
        if (mSourceCallback != null && mSourceCallback instanceof NetworkConfigurationSourceCallback) {
            ((NetworkConfigurationSourceCallback) mSourceCallback).handleDestroy();
        }
    }

    public void networkConfigurationSetWifi(String ssid, String address, String password, NetworkConfigDNSInfo tempNetworkConfigDNSInfo, NetworkAdapter tempNetworkAdapter) {
        if (mSourceCallback != null && mSourceCallback instanceof NetworkConfigurationSourceCallback) {
            ((NetworkConfigurationSourceCallback) mSourceCallback).networkConfigurationSetWifi(ssid, address, password, tempNetworkConfigDNSInfo, tempNetworkAdapter);
        }
    }

    public void networkConfigurationIgnoreWifi(String ssid, String address) {
        if (mSourceCallback != null && mSourceCallback instanceof NetworkConfigurationSourceCallback) {
            ((NetworkConfigurationSourceCallback) mSourceCallback).networkConfigurationIgnoreWifi(ssid, address);
        }
    }

    public NetworkAdapter getCurrentNetworkAdapter() {
        NetworkAdapter adapter = null;
        if (mSinkCallback != null && mSinkCallback instanceof NetworkConfigurationSinkCallback) {
            adapter = ((NetworkConfigurationSinkCallback) mSinkCallback).getCurrentNetworkAdapter();
        }
        return adapter;
    }

    public void handleRefreshAccessNetwork(NetworkConfigDNSInfo networkConfigDNSInfo, NetworkAdapter networkAdapter) {
        if (mSinkCallback != null && mSinkCallback instanceof NetworkConfigurationSinkCallback) {
            ((NetworkConfigurationSinkCallback) mSinkCallback).handleRefreshAccessNetwork(networkConfigDNSInfo, networkAdapter);
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof NetworkConfigurationSinkCallback) {
            ((NetworkConfigurationSinkCallback) mSinkCallback).handleDisconnect();
        }
    }

    public void handleNetworkConfigurationSetWifi(int code, String source) {
        if (mSinkCallback != null && mSinkCallback instanceof NetworkConfigurationSinkCallback) {
            ((NetworkConfigurationSinkCallback) mSinkCallback).handleNetworkConfigurationSetWifi(code, source);
        }
    }

    public void handleNetworkConfigurationIgnoreWifi(int code, String source) {
        if (mSinkCallback != null && mSinkCallback instanceof NetworkConfigurationSinkCallback) {
            ((NetworkConfigurationSinkCallback) mSinkCallback).handleNetworkConfigurationIgnoreWifi(code, source);
        }
    }
}
