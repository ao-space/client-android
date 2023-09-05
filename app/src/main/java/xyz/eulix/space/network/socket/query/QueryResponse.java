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

import xyz.eulix.space.network.socket.BaseResponse;

/**
 * @author: chenjiawei
 * date: 2021/6/21 17:16
 */
public class QueryResponse extends BaseResponse {
    private QueryResponseResult result;

    public QueryResponseResult getResult() {
        return result;
    }

    public void setResult(QueryResponseResult result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "QueryResponse{" +
                "result=" + result +
                ", messageId='" + messageId + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}
