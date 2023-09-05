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

package xyz.eulix.space.network.box;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/19 16:55
 */
public interface BKeyService {
    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.AUTH_BOX_KEY_CREATE_API)
    Observable<BKeyCreateResponseBody> obtainAuthCode(@Header("Request-Id") String requestId, @Body BKeyCreate bKeyCreate);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.AUTH_BOX_LOGIN_AUTH_KEY_CREATE_API)
    Observable<BoxLoginAuthCodeResponseBody> obtainBoxLoginAuthCode(@Header("Request-Id") String requestId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.AUTH_BOX_KEY_POLL_API)
    Observable<BKeyPollResponseBody> obtainAuthResult(@Header("Request-Id") String requestId, @Query("bkey") String bKey, @Query("autoLogin") boolean isAutoLogin);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.AUTH_BOX_LONGIN_AUTH_KEY_POLL_API)
    Observable<BKeyCheckResponseBody> obtainBoxLoginAuthResult(@Header("Request-Id") String requestId, @Query("bkey") String bKey, @Query("autoLogin") boolean isAutoLogin);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.AUTH_BOX_LONGIN_BKEY_VERIFY_API)
    Observable<BKeyCheckResponseBody> bKeyVerify(@Header("Request-Id") String requestId, @Query("bkey") String bKey);
}
