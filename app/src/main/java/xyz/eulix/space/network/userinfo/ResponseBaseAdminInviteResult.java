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

package xyz.eulix.space.network.userinfo;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.files.BaseResponseBody;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/12 18:39
 */
public class ResponseBaseAdminInviteResult extends BaseResponseBody implements Serializable, EulixKeep {

    @SerializedName("results")
    private AdminInviteResult results;

    public AdminInviteResult getResults() {
        return results;
    }

    public void setResults(AdminInviteResult results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "ResponseBaseAdminInviteResult{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                ", results=" + results +
                '}';
    }
}
