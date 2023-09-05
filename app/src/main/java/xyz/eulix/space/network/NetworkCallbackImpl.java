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

package xyz.eulix.space.network;

import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/14 17:04
 */
public class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
    private static final String TAG = NetworkCallbackImpl.class.getSimpleName();
    private ConnectivityCallback connectivityCallback;

    public interface ConnectivityCallback {
        void onConnect(boolean isConnected);
    }

    public void registerCallback(ConnectivityCallback callback) {
        connectivityCallback = callback;
    }

    public void unregisterCallback() {
        connectivityCallback = null;
    }

    @Override
    public void onAvailable(@NonNull Network network) {
        super.onAvailable(network);
        Logger.d(TAG, "on available: " + network);
        if (connectivityCallback != null) {
            connectivityCallback.onConnect(true);
        }
    }

    @Override
    public void onLosing(@NonNull Network network, int maxMsToLive) {
        super.onLosing(network, maxMsToLive);
    }

    @Override
    public void onLost(@NonNull Network network) {
        super.onLost(network);
        Logger.d(TAG, "on lost: " + network);
        if (connectivityCallback != null) {
            connectivityCallback.onConnect(false);
        }
    }

    @Override
    public void onUnavailable() {
        super.onUnavailable();
    }

    @Override
    public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities);
    }

    @Override
    public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
        super.onLinkPropertiesChanged(network, linkProperties);
    }

    @Override
    public void onBlockedStatusChanged(@NonNull Network network, boolean blocked) {
        super.onBlockedStatusChanged(network, blocked);
    }
}
