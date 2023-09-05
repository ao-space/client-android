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

import androidx.fragment.app.FragmentActivity;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.ApplicationLockInfo;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.manager.EulixBiometricManager;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/3 18:16
 */
public class ApplicationLockSettingPresenter extends AbsPresenter<ApplicationLockSettingPresenter.IApplicationLockSetting> {


    public interface IApplicationLockSetting extends IBaseView {

    }

    public ApplicationLockInfo getApplicationLockInfo() {
        ApplicationLockInfo applicationLockInfo = null;
        generateEulixBiometricManager();
        if (eulixBiometricManager != null) {
            EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
            if (eulixBoxBaseInfo != null) {
                applicationLockInfo = eulixBiometricManager.getSpaceApplicationLock(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind());
            }
        }
        return applicationLockInfo;
    }

    public Boolean canAuthenticateEnrolled() {
        Boolean result = null;
        generateEulixBiometricManager();
        if (eulixBiometricManager != null) {
            result = eulixBiometricManager.canAuthenticateWithEnrolled();
        }
        return result;
    }

}
