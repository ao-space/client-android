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

package xyz.eulix.space.network.notification;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/8 15:32
 */
public class GetNotificationResult implements EulixKeep, Serializable {
    @SerializedName("messageId")
    private String messageId;
    @SerializedName("clientUUID")
    private String clientUuid;
    @SerializedName("optType")
    private String optType;
    @SerializedName("requestId")
    private String requestId;
    @SerializedName("data")
    private String data;
    @SerializedName("read")
    private boolean read;
    @SerializedName("pushed")
    private Integer pushed;
    @SerializedName("createAt")
    private String createAt;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getOptType() {
        return optType;
    }

    public void setOptType(String optType) {
        this.optType = optType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public Integer getPushed() {
        return pushed;
    }

    public void setPushed(Integer pushed) {
        this.pushed = pushed;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    @Override
    public String toString() {
        return "GetNotificationResult{" +
                "messageId='" + messageId + '\'' +
                ", clientUuid='" + clientUuid + '\'' +
                ", optType='" + optType + '\'' +
                ", requestId='" + requestId + '\'' +
                ", data='" + data + '\'' +
                ", read=" + read +
                ", pushed=" + pushed +
                ", createAt='" + createAt + '\'' +
                '}';
    }
}
