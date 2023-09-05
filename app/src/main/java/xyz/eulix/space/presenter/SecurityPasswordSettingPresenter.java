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

import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.network.agent.AgentUtil;
import xyz.eulix.space.network.agent.ResetCallback;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/1 10:58
 */
public class SecurityPasswordSettingPresenter extends AbsPresenter<SecurityPasswordSettingPresenter.ISecurityPasswordSetting> {
    private ResetCallback resetCallback = new ResetCallback() {
        @Override
        public void onSuccess(String message, Integer code) {
            if (iView != null) {
                iView.handleResultCallback(code != null && code < 400);
            }
        }

        @Override
        public void onFailed(String message, Integer code) {
            if (iView != null) {
                iView.handleResultCallback(false);
            }
        }

        @Override
        public void onError(String msg) {
            if (iView != null) {
                iView.handleResultCallback(false);
            }
        }
    };

    public interface ISecurityPasswordSetting extends IBaseView {
        void handleResultCallback(boolean isSuccess);
        void bluetoothHandle(String oldPassword, String newPassword);
    }

    public void setPassword(String password, String baseUrl, boolean isBindDuplicate, boolean isBluetooth, String bleKey, String bleIv) {
        String oldPassword = (isBindDuplicate ? password : null);
        if (isBluetooth) {
            if (iView != null) {
                iView.bluetoothHandle(oldPassword, password);
            }
        } else {
            try {
                ThreadPool.getInstance().execute(() -> AgentUtil.setPassword(baseUrl, oldPassword
                        , password, bleKey, bleIv, resetCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
