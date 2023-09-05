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

package xyz.eulix.space.network.box;

import com.google.gson.annotations.SerializedName;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.files.BaseResponseBody;

/**
 * Author: 		Zhufy
 * Description: bKey校验、查询返回数据
 * History:		2022/6/1
 */
public class BKeyCheckResponseBody extends BaseResponseBody implements EulixKeep {
    @SerializedName("results")
    public boolean results;

    @Override
    public String toString() {
        return "BKeyVerifyResponseBody{" +
                "requestId='" + requestId + '\'' +
                ", results=" + results +
                ", codeInt=" + codeInt +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
