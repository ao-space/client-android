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

package xyz.eulix.space.event;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/11 15:55
 */
public class MemberListEvent {
    private String boxUuid;
    private String boxBind;
    private String boxDomain;
    private String refreshId;
    private boolean isUpdateAvatar;

    public MemberListEvent(String boxUuid, String boxBind, String boxDomain) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.boxDomain = boxDomain;
        this.isUpdateAvatar = false;
    }

    public MemberListEvent(String boxUuid, String boxBind, String boxDomain, String refreshId) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.boxDomain = boxDomain;
        this.refreshId = refreshId;
        this.isUpdateAvatar = false;
    }

    public MemberListEvent(String boxUuid, String boxBind, String boxDomain, boolean isUpdateAvatar) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.boxDomain = boxDomain;
        this.isUpdateAvatar = isUpdateAvatar;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public String getBoxDomain() {
        return boxDomain;
    }

    public String getRefreshId() {
        return refreshId;
    }

    public boolean isUpdateAvatar() {
        return isUpdateAvatar;
    }
}
