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

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/5/7 15:14
 */
public class PushBean implements EulixKeep {
    private String messageId;
    private String boxUuid;
    private String boxBind;
    private String type;
    private int priority;
    private int source;
    private int consume;
    private String title;
    private String content;
    private String rawData;
    private String createAt;
    private long timestamp;
    private String reserve;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public void setBoxUuid(String boxUuid) {
        this.boxUuid = boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public void setBoxBind(String boxBind) {
        this.boxBind = boxBind;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getConsume() {
        return consume;
    }

    public void setConsume(int consume) {
        this.consume = consume;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReserve() {
        return reserve;
    }

    public void setReserve(String reserve) {
        this.reserve = reserve;
    }

    @Override
    public String toString() {
        return "PushBean{" +
                "messageId='" + messageId + '\'' +
                ", boxUuid='" + boxUuid + '\'' +
                ", boxBind='" + boxBind + '\'' +
                ", type='" + type + '\'' +
                ", priority=" + priority +
                ", source=" + source +
                ", consume=" + consume +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", rawData='" + rawData + '\'' +
                ", createAt='" + createAt + '\'' +
                ", timestamp=" + timestamp +
                ", reserve='" + reserve + '\'' +
                '}';
    }
}
