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
import xyz.eulix.space.bean.LanServiceInfo;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/12/2 18:40
 */
public class LanFindBoxBridge extends AbsBridge {
    private static final LanFindBoxBridge INSTANCE = new LanFindBoxBridge();

    public interface LanFindBoxSourceCallback extends SourceCallback {
        void connectBox(LanServiceInfo serviceInfo, int paired);
    }

    public interface LanFindBoxSinkCallback extends SinkCallback {
        void handleBindResult(boolean isSuccess, boolean isFinish);
    }

    private LanFindBoxBridge() {}

    public static LanFindBoxBridge getInstance() {
        return INSTANCE;
    }

    public void selectBox(LanServiceInfo serviceInfo, int paired) {
        if (mSourceCallback != null && mSourceCallback instanceof LanFindBoxSourceCallback) {
            ((LanFindBoxSourceCallback) mSourceCallback).connectBox(serviceInfo, paired);
        }
    }

    public boolean bindResult(boolean isSuccess, boolean isFinish) {
        boolean isHandle = false;
        if (mSinkCallback != null && mSinkCallback instanceof LanFindBoxSinkCallback) {
            isHandle = true;
            ((LanFindBoxSinkCallback) mSinkCallback).handleBindResult(isSuccess, isFinish);
        }
        return isHandle;
    }
}
