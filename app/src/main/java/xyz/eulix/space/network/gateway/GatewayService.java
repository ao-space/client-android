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

package xyz.eulix.space.network.gateway;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * date: 2021/7/6 18:04
 */
public interface GatewayService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.SPACE_STATUS_API)
    Observable<SpaceStatusResult> getSpaceStatus();

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.SPACE_POLL_API)
    Observable<SpacePollResult> getSpacePoll(@Header("Request-Id") String requestId, @Query("accessToken") String accessToken, @Query("count") int count);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.CREATE_AUTH_TOKEN_API)
    Observable<CreateTokenResult> createAuthToken(@Header("Request-Id") String requestId, @Body CreateTokenInfo createTokenInfo);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.REFRESH_AUTH_TOKEN_API)
    Observable<CreateTokenResult> refreshAuthToken(@Header("Request-Id") String requestId, @Body RefreshTokenInfo refreshTokenInfo);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.REFRESH_BOX_KEY_AUTH_TOKEN_API)
    Observable<CreateTokenResult> refreshLoginAuthToken(@Header("Request-Id") String requestId, @Query("tmpEncryptedSecret") String tmpEncryptedSecret, @Body RefreshTokenInfo refreshTokenInfo);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.VERIFY_AUTH_TOKEN_API)
    Observable<ResponseBody> verifyAuthToken(@Query("access-token") String accessToken, @Header("Request-Id") String requestId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.AUTH_AUTO_LOGIN_CONFIRM_API)
    Observable<AuthAutoLoginConfirmResponseBody> authAutoLoginConfirm(@Header("Request-Id") String requestId, @Body AuthAutoLoginConfirmRequestBody authAutoLoginConfirmRequestBody);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.GATEWAY_VERSION_BOX)
    Observable<VersionCheckResponseBody> boxVersionCheck(@Header("Request-Id") String requestId, @Query("appName") String appName, @Query("appType") String appType, @Query("version") String appVersion);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.GATEWAY_VERSION_APP)
    Observable<VersionCheckResponseBody> appVersionCheck(@Header("Request-Id") String requestId, @Query("appName") String appName, @Query("appType") String appType, @Query("version") String appVersion, @Query("channelCode") String channelCode);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.GATEWAY_CURRENT_BOX_VERSION)
    Observable<CurrentBoxVersionResponseBody> getCurrentBoxVersion(@Header("Request-Id") String requestId);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.GATEWAY_VERSION_COMPATIBLE)
    Observable<VersionCompatibleResponseBody> getVersionCompatible(@Header("Request-Id") String requestId, @Query("appName") String appName, @Query("appType") String appType, @Query("version") String version, @Query("channelCode") String channelCode);

}
