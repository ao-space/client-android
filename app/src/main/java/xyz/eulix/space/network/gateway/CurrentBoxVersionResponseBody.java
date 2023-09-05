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

package xyz.eulix.space.network.gateway;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.network.files.BaseResponseBody;

/**
 * Author:      Zhu Fuyu
 * Description: 获取当前盒子系统版本返回体
 * History:     2021/11/10
 */
public class CurrentBoxVersionResponseBody extends BaseResponseBody implements Serializable {

    @SerializedName("results")
    public String results;

    @Override
    public String toString() {
        return "CurrentBoxVersionResponseBody{" +
                "results='" + results + '\'' +
                ", codeInt=" + codeInt +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
