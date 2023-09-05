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

package xyz.eulix.space.network.agent.net;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import xyz.eulix.space.network.EulixBaseRequest;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.agent.AgentBaseResponse;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/1 16:16
 */
public interface EulixNetService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.NETWORK_CONFIG_API)
    Observable<AgentBaseResponse> getNetworkConfig();

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NETWORK_CONFIG_API)
    Observable<EulixBaseResponse> setNetworkConfig(@Body EulixBaseRequest request);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NETWORK_IGNORE_API)
    Observable<EulixBaseResponse> ignoreNetwork(@Body EulixBaseRequest request);
}
