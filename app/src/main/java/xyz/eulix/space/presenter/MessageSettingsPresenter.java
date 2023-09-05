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
import xyz.eulix.space.util.DataUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/4/20 17:56
 */
public class MessageSettingsPresenter extends AbsPresenter<MessageSettingsPresenter.IMessageSetting> {

    public interface IMessageSetting extends IBaseView {}

    public boolean isSystemMessageEnable() {
        return DataUtil.getSystemMessageEnable(context);
    }

    public boolean setSystemMessageEnable(boolean isSystemMessageEnable) {
        return DataUtil.setSystemMessageEnable(context, isSystemMessageEnable, true);
    }

    public boolean isBusinessMessageEnable() {
        return DataUtil.getBusinessMessageEnable(context);
    }

    public boolean setBusinessMessageEnable(boolean isBusinessMessageEnable) {
        return DataUtil.setBusinessMessageEnable(context, isBusinessMessageEnable, true);
    }
}
