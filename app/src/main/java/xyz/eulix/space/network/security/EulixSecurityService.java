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

package xyz.eulix.space.network.security;

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
 * date: 2022/7/8 15:13
 */
public interface EulixSecurityService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.GET_DEVICE_HARDWARE_INFO_API)
    Observable<DeviceHardwareInfoResponse> getDeviceHardwareInfo(@Header("Request-Id") String requestId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.BINDER_MODIFY_SECURITY_PASSWORD_API)
    Observable<EulixBaseResponse> binderModifySecurityPassword(@Header("Request-Id") String requestId, @Body BinderModifySecurityPasswordRequest body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.VERIFY_SECURITY_PASSWORD_API)
    Observable<VerifySecurityResponse> verifySecurityPassword(@Header("Request-Id") String requestId, @Body VerifySecurityPasswordRequest body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.BINDER_RESET_SECURITY_PASSWORD_API)
    Observable<EulixBaseResponse> binderResetSecurityPassword(@Header("Request-Id") String requestId, @Body BinderResetSecurityPasswordRequest body);


    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.GRANTEE_APPLY_MODIFY_SECURITY_PASSWORD_API)
    Observable<EulixBaseResponse> granteeApplyModifySecurityPassword(@Header("Request-Id") String requestId, @Body GranteeApplySetSecurityPasswordRequest body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.BINDER_ACCEPT_MODIFY_SECURITY_PASSWORD_API)
    Observable<EulixBaseResponse> binderAcceptModifySecurityPassword(@Header("Request-Id") String requestId, @Body SecurityTokenAcceptRequest body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.GRANTEE_APPLY_RESET_SECURITY_PASSWORD_API)
    Observable<EulixBaseResponse> granteeApplyResetSecurityPassword(@Header("Request-Id") String requestId, @Body GranteeApplySetSecurityPasswordRequest body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.BINDER_ACCEPT_RESET_SECURITY_PASSWORD_API)
    Observable<EulixBaseResponse> binderAcceptResetSecurityPassword(@Header("Request-Id") String requestId, @Body SecurityTokenAcceptRequest body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.GRANTEE_MODIFY_SECURITY_PASSWORD_API)
    Observable<EulixBaseResponse> granteeModifySecurityPassword(@Header("Request-Id") String requestId, @Body GranteeModifySecurityPasswordRequest body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.GRANTEE_RESET_SECURITY_PASSWORD_API)
    Observable<EulixBaseResponse> granteeResetSecurityPassword(@Header("Request-Id") String requestId, @Body GranteeResetSecurityPasswordRequest body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.SECURITY_MESSAGE_POLL_API)
    Observable<SecurityMessagePollResponse> securityMessagePoll(@Header("Request-Id") String requestId, @Body SecurityMessagePollRequest body);
}
