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

import android.bluetooth.BluetoothDevice;

import xyz.eulix.space.abs.AbsBridge;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/28 13:31
 */
public class FindBoxBridge extends AbsBridge {
    private static final FindBoxBridge INSTANCE = new FindBoxBridge();

    public interface FindBoxSourceCallback extends SourceCallback {
        void connectBox(BluetoothDevice bluetoothDevice, int paired);
    }

    public interface FindBoxSinkCallback extends SinkCallback {
        void handleBindResult(boolean isSuccess, boolean isFinish);
        void handleDisconnect();
    }

    private FindBoxBridge() {}

    public static FindBoxBridge getInstance() {
        return INSTANCE;
    }

    public void selectBox(BluetoothDevice device, int paired) {
        if (mSourceCallback != null && mSourceCallback instanceof FindBoxSourceCallback) {
            ((FindBoxSourceCallback) mSourceCallback).connectBox(device, paired);
        }
    }

    public boolean bindResult(boolean isSuccess, boolean isFinish) {
        boolean isHandle = false;
        if (mSinkCallback != null && mSinkCallback instanceof FindBoxSinkCallback) {
            isHandle = true;
            ((FindBoxSinkCallback) mSinkCallback).handleBindResult(isSuccess, isFinish);
        }
        return isHandle;
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof FindBoxSinkCallback) {
            ((FindBoxSinkCallback) mSinkCallback).handleDisconnect();
        }
    }
}
