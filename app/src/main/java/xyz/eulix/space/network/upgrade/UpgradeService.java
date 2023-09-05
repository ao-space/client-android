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

package xyz.eulix.space.network.upgrade;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import xyz.eulix.space.bean.DeviceVersionInfoBean;
import xyz.eulix.space.bean.DeviceVersionInfoResponseBody;
import xyz.eulix.space.network.files.BaseResponseBody;
import xyz.eulix.space.util.ConstantField;

/**
 * Author:      Zhu Fuyu
 * Description: 盒子系统升级服务
 * History:     2021/11/8
 */
public interface UpgradeService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.AGENT_UPGRADE_CONFIG)
    Observable<UpgradeConfigResponseBody> getUpgradeConfig(@Header("Request-Id") String requestId);

    @Headers({"Accept: application/json"})
    @POST(ConstantField.URL.AGENT_UPGRADE_START_UPGRADE)
    Observable<BaseResponseBody> setUpgradeConfig(@Body SetUpgradeConfigReq setUpgradeConfigReq);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.AGENT_UPGRADE_STATUS)
    Observable<UpgradeStatusResponseBody> checkUpgradeStatus(@Header("Request-Id") String requestId);

    @Headers({"Accept: application/json"})
    @POST(ConstantField.URL.AGENT_UPGRADE_START_UPGRADE)
    Observable<UpgradeStatusResponseBody> startUpgrade(@Body StartUpgradeReq startUpgradeReq);

    @Headers({"Accept: application/json"})
    @POST(ConstantField.URL.AGENT_UPGRADE_START_PULL)
    Observable<UpgradeStatusResponseBody> startPull(@Body StartUpgradeReq startUpgradeReq);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.AGENT_DEVICE_INFO)
    Observable<DeviceVersionInfoResponseBody> getDeviceInfo(@Header("Request-Id") String requestId);

}
