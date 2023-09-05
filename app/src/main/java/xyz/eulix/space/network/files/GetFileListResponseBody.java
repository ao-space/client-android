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

package xyz.eulix.space.network.files;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * date: 2021/6/22 16:16
 */
public class GetFileListResponseBody extends BaseResponseBody implements EulixKeep {
    private GetFileListResponseResult results;

    public GetFileListResponseResult getResults() {
        return results;
    }

    public void setResults(GetFileListResponseResult results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "GetFileListResponseBody{" +
                "results=" + results +
                ", code=" + codeInt +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
