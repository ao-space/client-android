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

package xyz.eulix.space.network.upgrade;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * Author:      Zhu Fuyu
 * Description: 开始升级请求体
 * History:     2021/11/8
 */
public class StartUpgradeReq implements EulixKeep {
    private String versionId;
    //当没有下载时， 是否先下载
    private boolean anew;

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public boolean isAnew() {
        return anew;
    }

    public void setAnew(boolean anew) {
        this.anew = anew;
    }
}
