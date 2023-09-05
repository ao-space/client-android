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

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/28 11:31
 */
public class UserInfo {
    private boolean isAdmin;
    private String userId;
    private String nickName;
    private String signature;
    private String avatarPath;
    private long userCreateTimestamp;
    private long usedSize;
    private long totalSize;
    private String deviceInfo;
    private String userDomain;
    private String avatarMD5;

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
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

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    public String getAvatarMD5() {
        return avatarMD5;
    }

    public void setAvatarMD5(String avatarMD5) {
        this.avatarMD5 = avatarMD5;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "isAdmin=" + isAdmin +
                ", userId='" + userId + '\'' +
                ", nickName='" + nickName + '\'' +
                ", signature='" + signature + '\'' +
                ", avatarPath='" + avatarPath + '\'' +
                ", userCreateTimestamp=" + userCreateTimestamp +
                ", usedSize=" + usedSize +
                ", totalSize=" + totalSize +
                ", deviceInfo='" + deviceInfo + '\'' +
                ", userDomain='" + userDomain + '\'' +
                ", avatarMD5='" + avatarMD5 + '\'' +
                '}';
    }
}
