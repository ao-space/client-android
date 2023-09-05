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

package xyz.eulix.space.transfer.multipart.bean;

import com.google.gson.annotations.SerializedName;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.files.BaseResponseBody;

/**
 * Author:      Zhu Fuyu
 * Description: 获取自签名证书响应
 * History:     2023/2/10
 */
public class GetCertResponseBody extends BaseResponseBody {

    //Base64编码后的证书
    @SerializedName("results")
    public Results results;

    public class Results implements EulixKeep{
        @SerializedName("cert")
        public String certBase64;

        @Override
        public String toString() {
            return "Results{" +
                    "certBase64='" + certBase64 + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "GetCertResponseBody{" +
                "codeInt=" + codeInt +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                ", results=" + results +
                '}';
    }
}
