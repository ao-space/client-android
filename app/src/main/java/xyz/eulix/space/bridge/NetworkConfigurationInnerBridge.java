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
 * date: 2022/12/5 10:05
 */
public class NetworkConfigurationInnerBridge extends AbsBridge {
    private static final NetworkConfigurationInnerBridge INSTANCE = new NetworkConfigurationInnerBridge();

    public interface NetworkConfigurationInnerSourceCallback extends SourceCallback {
        void handleDestroy();
        void requestIpConfiguration(boolean isConnect, NetworkConfigDNSInfo networkConfigDNSInfo, NetworkAdapter networkAdapter);
    }

    public interface NetworkConfigurationInnerSinkCallback extends SinkCallback {
        void handleDisconnect();
        void handleNetworkConfigurationSetWifi(int code, String source);
    }

    private NetworkConfigurationInnerBridge() {}

    public static NetworkConfigurationInnerBridge getInstance() {
        return INSTANCE;
    }

    public void handleDestroy() {
        if (mSourceCallback != null && mSourceCallback instanceof NetworkConfigurationInnerSourceCallback) {
            ((NetworkConfigurationInnerSourceCallback) mSourceCallback).handleDestroy();
        }
    }

    public void requestIpConfiguration(boolean isConnect, NetworkConfigDNSInfo networkConfigDNSInfo, NetworkAdapter networkAdapter) {
        if (mSourceCallback != null && mSourceCallback instanceof NetworkConfigurationInnerSourceCallback) {
            ((NetworkConfigurationInnerSourceCallback) mSourceCallback).requestIpConfiguration(isConnect, networkConfigDNSInfo, networkAdapter);
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof NetworkConfigurationInnerSinkCallback) {
            ((NetworkConfigurationInnerSinkCallback) mSinkCallback).handleDisconnect();
        }
    }

    public void handleNetworkConfigurationSetWifi(int code, String source) {
        if (mSinkCallback != null && mSinkCallback instanceof NetworkConfigurationInnerSinkCallback) {
            ((NetworkConfigurationInnerSinkCallback) mSinkCallback).handleNetworkConfigurationSetWifi(code, source);
        }
    }
}
