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

package xyz.eulix.space.bean;

import java.io.Serializable;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/29 14:05
 */
public class EulixUser implements Serializable {
    public static final int MENU_TYPE_LOGIN_MORE_SPACE = 1;
    // Space: box uuid; Member: client uuid
    private String uuid;
    // user avatar path
    private String avatarPath;
    // Only member use
    private boolean isAdmin;
    // user nickname
    private String nickName;
    // Only member use
    private boolean isMyself;
    // user ao id
    private String userId;
    // Only space use
    private String adminNickname;
    // Only member use
    private long userCreateTimestamp;
    // used size
    private long usedSize;
    // Only space use
    private int spaceState;
    // Only member use
    private String phoneModel;
    // Only space use
    private String bind;
    // user domain
    private String userDomain;
    // 是否被选中
    private boolean isSelected = false;
    // 局域网通道是否开启
    private Boolean isInternetAccess = null;
    // Only space use
    private int menuType;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public boolean isMyself() {
        return isMyself;
    }

    public void setMyself(boolean myself) {
        isMyself = myself;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAdminNickname() {
        return adminNickname;
    }

    public void setAdminNickname(String adminNickname) {
        this.adminNickname = adminNickname;
    }

    public long getUserCreateTimestamp() {
        return userCreateTimestamp;
    }

    public void setUserCreateTimestamp(long userCreateTimestamp) {
        this.userCreateTimestamp = userCreateTimestamp;
    }

    public long getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(long usedSize) {
        this.usedSize = usedSize;
    }

    public int getSpaceState() {
        return spaceState;
    }

    public void setSpaceState(int spaceState) {
        this.spaceState = spaceState;
    }

    public String getPhoneModel() {
        return phoneModel;
    }

    public void setPhoneModel(String phoneModel) {
        this.phoneModel = phoneModel;
    }

    public String getBind() {
        return bind;
    }

    public void setBind(String bind) {
        this.bind = bind;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public Boolean getInternetAccess() {
        return isInternetAccess;
    }

    public void setInternetAccess(Boolean internetAccess) {
        isInternetAccess = internetAccess;
    }

    public int getMenuType() {
        return menuType;
    }

    public void setMenuType(int menuType) {
        this.menuType = menuType;
    }

    @Override
    public String toString() {
        return "EulixUser{" +
                "uuid='" + uuid + '\'' +
                ", avatarPath='" + avatarPath + '\'' +
                ", isAdmin=" + isAdmin +
                ", nickName='" + nickName + '\'' +
                ", isMyself=" + isMyself +
                ", userId='" + userId + '\'' +
                ", adminNickname='" + adminNickname + '\'' +
                ", userCreateTimestamp=" + userCreateTimestamp +
                ", usedSize=" + usedSize +
                ", spaceState=" + spaceState +
                ", phoneModel='" + phoneModel + '\'' +
                ", bind='" + bind + '\'' +
                ", userDomain='" + userDomain + '\'' +
                ", isSelected=" + isSelected +
                ", isInternetAccess=" + isInternetAccess +
                ", menuType=" + menuType +
                '}';
    }
}
