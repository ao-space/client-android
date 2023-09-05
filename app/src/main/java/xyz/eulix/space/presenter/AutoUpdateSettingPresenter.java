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

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.network.upgrade.IGetUpgradeConfigCallback;
import xyz.eulix.space.network.upgrade.UpgradeUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PreferenceUtil;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/10/27
 */
public class AutoUpdateSettingPresenter extends AbsPresenter<AutoUpdateSettingPresenter.IAutoUpdateSetting> {
    public interface IAutoUpdateSetting extends IBaseView {
        void setResult(boolean autoDownload, boolean autoInstall);

        void setFailed(boolean isError);
    }

    public void setUpgradeConfig(boolean autoDownload, boolean autoInstall) {
        UpgradeUtils.setSystemAutoUpgradeConfig(context, autoDownload, autoInstall, (result, extraMsg) -> {
            if (result != null && result) {
                getAutoUpgradeConfig();
            } else {
                iView.setFailed((result == null));
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
                iView.setResult(autoDownload, autoInstall);
            }

            @Override
            public void onError(String msg) {
                Logger.d("zfy", "get system auto upgrade config error:" + msg);
                iView.setFailed(true);
            }
        });
    }
}
