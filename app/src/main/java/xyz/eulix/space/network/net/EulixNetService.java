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

package xyz.eulix.space.network.net;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.agent.net.NetworkConfigRequest;
import xyz.eulix.space.network.agent.net.NetworkIgnoreRequest;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/1 15:22
 */
public interface EulixNetService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.NETWORK_CONFIG_API)
    Observable<NetworkStatusResponse> getNetworkConfig(@Header("Request-Id") String requestId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NETWORK_CONFIG_API)
    Observable<EulixBaseResponse> setNetworkConfig(@Header("Request-Id") String requestId, @Body NetworkConfigRequest request);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NETWORK_IGNORE_API)
    Observable<EulixBaseResponse> ignoreNetwork(@Header("Request-Id") String requestId, @Body NetworkIgnoreRequest request);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.DEVICE_ABILITY_API)
    Observable<DeviceAbilityResponse> getDeviceAbility(@Header("Request-Id") String requestId);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.NETWORK_CHANNEL_INFO_API)
    Observable<ChannelInfoResponse> getNetworkChannelInfo(@Header("Request-Id") String requestId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NETWORK_CHANNEL_WAN_API)
    Observable<ChannelInfoResponse> setNetworkChannelWan(@Header("Request-Id") String requestId, @Body ChannelWanRequest request);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.BIND_INTERNET_SERVICE_CONFIG_API)
    Observable<InternetServiceConfigResponse> getInternetServiceConfig(@Header("Request-Id") String requestId, @Query("clientUUID") String clientUuid, @Query("aoId") String aoId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.BIND_INTERNET_SERVICE_CONFIG_API)
    Observable<InternetServiceConfigResponse> setInternetServiceConfig(@Header("Request-Id") String requestId, @Body InternetServiceConfigRequest request);
}
