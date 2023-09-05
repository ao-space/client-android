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

package xyz.eulix.space.network.agent;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import xyz.eulix.space.bean.bind.WpwdInfo;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/18 10:05
 */
public interface DeviceService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.LOCAL_IPS_API)
    Observable<LocalIpInfo> getDeviceLocalIps();

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.NET_CONFIG_API)
    Observable<ScanWifiList> getDeviceWifiList();

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.NET_CONFIG_API)
    Observable<ScanRealWifiList> getDeviceRealWifiList();

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NET_CONFIG_API)
    Observable<NetworkConfigResponse> setDeviceWifi(@Body ConnectWifiReq connectWifiReq);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NET_CONFIG_API)
    Observable<NetworkConfigResponse> setDeviceWifi(@Body WpwdInfo wpwdInfo);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NET_CONFIG_API)
    Observable<NetworkConfigRealResponse> setDeviceRealWifi(@Body ConnectWifiReq connectWifiReq);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NET_CONFIG_API)
    Observable<NetworkConfigRealResponse> setDeviceRealWifi(@Body WpwdInfo wpwdInfo);
}
