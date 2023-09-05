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

package xyz.eulix.space.bean.bind;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.agent.PairingBoxInfo;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/12/1 11:51
 */
public class PairingBoxResults implements EulixKeep {
    private String code;
    private String requestId;
    private String message;
    private PairingBoxInfo results;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PairingBoxInfo getResults() {
        return results;
    }

    public void setResults(PairingBoxInfo results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "PairingBoxResults{" +
                "code='" + code + '\'' +
                ", requestId='" + requestId + '\'' +
                ", message='" + message + '\'' +
                ", results=" + results +
                '}';
    }
}
