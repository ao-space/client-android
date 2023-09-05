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
import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/22 15:02
 */
public class NotificationAllResult implements EulixKeep, Serializable {
    @SerializedName("notification")
    private List<GetNotificationResult> notification;
    @SerializedName("pageInfo")
    private PageInfo pageInfo;

    public List<GetNotificationResult> getNotification() {
        return notification;
    }

    public void setNotification(List<GetNotificationResult> notification) {
        this.notification = notification;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    @Override
    public String toString() {
        return "NotificationAllResult{" +
                "notification=" + notification +
                ", pageInfo=" + pageInfo +
                '}';
    }
}
