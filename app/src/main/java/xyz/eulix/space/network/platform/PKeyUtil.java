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

package xyz.eulix.space.network.platform;

import java.util.UUID;

import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/18 14:25
 */
public class PKeyUtil {
    private static final String TAG = PKeyUtil.class.getSimpleName();

    private PKeyUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static void sendBoxInfoV2(String boxKey, String boxDomain, String boxPubKey, String platformKey, String lanDomain, String lanIp, PKeyBoxInfoCallback callback) {
        PKeyBoxInfoV2 pKeyBoxInfo = new PKeyBoxInfoV2();
        pKeyBoxInfo.setBkey(boxKey);
        pKeyBoxInfo.setBoxDomain(boxDomain);
        pKeyBoxInfo.setBoxPubKey(StringUtil.wrapPublicKey(boxPubKey));
        pKeyBoxInfo.setLanDomain(lanDomain);
        pKeyBoxInfo.setLanIp(lanIp);
        UUID requestId = UUID.randomUUID();
        PKeyManager.sendBoxInfoV2(platformKey, requestId.toString(), pKeyBoxInfo, new IPKeyBoxInfoCallback() {
            @Override
            public void onError(String msg) {
                Logger.e(TAG, "on error: " + msg);
                if (callback != null) {
                    callback.onError(msg);
                }
            }

            @Override
            public void onResult(int code) {
                Logger.i(TAG, "on result: " + code);
                if (code < 0) {
                    if (callback != null) {
                        callback.onError("");
                    }
                } else if (code < 300) {
                    if (callback != null) {
                        callback.onSuccess(code);
                    }
                } else {
                    if (callback != null) {
                        callback.onFailed(code);
                    }
                }
            }
        });
    }
}
