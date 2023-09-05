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
 * date: 2021/10/14 10:29
 */
public class InviteParams {
    private String subDomain;
    private String inviteCode;
    private String keyFingerPrint;
    private String account;
    private String member;
    private String create;
    private String expire;
    private String aoId;

    public String getSubDomain() {
        return subDomain;
    }

    public void setSubDomain(String subDomain) {
        this.subDomain = subDomain;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getKeyFingerPrint() {
        return keyFingerPrint;
    }

    public void setKeyFingerPrint(String keyFingerPrint) {
        this.keyFingerPrint = keyFingerPrint;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public String getCreate() {
        return create;
    }

    public void setCreate(String create) {
        this.create = create;
    }

    public String getExpire() {
        return expire;
    }

    public void setExpire(String expire) {
        this.expire = expire;
    }

    public String getAoId() {
        return aoId;
    }

    public void setAoId(String aoId) {
        this.aoId = aoId;
    }

    @Override
    public String toString() {
        return "InviteParams{" +
                "subDomain='" + subDomain + '\'' +
                ", inviteCode='" + inviteCode + '\'' +
                ", keyFingerPrint='" + keyFingerPrint + '\'' +
                ", account='" + account + '\'' +
                ", member='" + member + '\'' +
                ", create='" + create + '\'' +
                ", expire='" + expire + '\'' +
                ", aoId='" + aoId + '\'' +
                '}';
    }
}
