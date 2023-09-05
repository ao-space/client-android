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

package xyz.eulix.space.network.disk;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/8 15:37
 */
public interface DiskService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.DISK_MANAGEMENT_LIST_API)
    Observable<DiskManageListResponse> getDiskManagementList(@Header("Request-Id") String requestId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.SYSTEM_SHUTDOWN_API)
    Observable<EulixBaseResponse> eulixSystemShutdown(@Header("Request-Id") String requestId);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.DISK_MANAGEMENT_RAID_INFO_API)
    Observable<RaidInfoResponse> getDiskManagementRaidInfo(@Header("Request-Id") String requestId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.DISK_MANAGEMENT_EXPAND_API)
    Observable<EulixBaseResponse> diskExpand(@Header("Request-Id") String requestId, @Body DiskExpandRequest request);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.DISK_MANAGEMENT_EXPAND_PROGRESS_API)
    Observable<DiskExpandProgressResponse> getDiskExpandProgress(@Header("Request-Id") String requestId);
}
