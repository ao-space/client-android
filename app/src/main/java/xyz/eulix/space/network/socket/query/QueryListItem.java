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

package xyz.eulix.space.network.socket.query;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * date: 2021/6/21 17:12
 */
public class QueryListItem implements EulixKeep {
    private int id;
    private String createdAt;
    private String updatedAt;
    private int version;
    private String messageId;
    private String title;
    private String body;
    private String clientUUID;
    private Object extParameters;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getClientUUID() {
        return clientUUID;
    }

    public void setClientUUID(String clientUUID) {
        this.clientUUID = clientUUID;
    }

    public Object getExtParameters() {
        return extParameters;
    }

    public void setExtParameters(Object extParameters) {
        this.extParameters = extParameters;
    }

    @Override
    public String toString() {
        return "QueryListItem{" +
                "id=" + id +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", version=" + version +
                ", messageId='" + messageId + '\'' +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", clientUUID='" + clientUUID + '\'' +
                ", extParameters=" + extParameters +
                '}';
    }
}
