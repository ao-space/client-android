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

package xyz.eulix.space.network.agent.disk;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import xyz.eulix.space.network.EulixBaseRequest;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.agent.AgentBaseResponse;
import xyz.eulix.space.network.agent.PasswordInfo;
import xyz.eulix.space.network.agent.SetPasswordResponse;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/1 16:47
 */
public interface DiskService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.SPACE_READY_CHECK_API)
    Observable<AgentBaseResponse> getSpaceReadyCheck();

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.DISK_RECOGNITION_API)
    Observable<AgentBaseResponse> getDiskRecognition();

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.DISK_INITIALIZE_API)
    Observable<EulixBaseResponse> diskInitialize(@Body EulixBaseRequest body);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.DISK_INITIALIZE_PROGRESS_API)
    Observable<AgentBaseResponse> getDiskInitializeProgress();

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.DISK_MANAGEMENT_LIST_API)
    Observable<AgentBaseResponse> getDiskManagementList();

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.SYSTEM_SHUTDOWN_API)
    Observable<EulixBaseResponse> eulixSystemShutdown();

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.SYSTEM_REBOOT_API)
    Observable<EulixBaseResponse> eulixSystemReboot();
}
