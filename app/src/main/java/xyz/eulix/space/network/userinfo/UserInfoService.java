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

package xyz.eulix.space.network.userinfo;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import xyz.eulix.space.network.gateway.CreateTokenInfo;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * date: 2021/6/22 16:30
 */
public interface UserInfoService {
    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.PERSONALINFO_SHOW_API)
    Observable<GetUserInfoResponseBody> getUserInfoOld(@Header("clientUUID") String clientUUID);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.PERSONALINFO_SHOW_API)
    Observable<AccountInfoResult> getUserInfo(@Header("Request-Id") String requestId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.PERSONALINFO_UPDATE_API)
    Observable<UpdateUserInfoResponseBody> updateUserInfoOld(@Header("clientUUID") String clientUUID, @Body UpdateUserInfoRsq updateUserInfoRsq);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.PERSONALINFO_UPDATE_API)
    Observable<AccountInfoResult> updateUserInfo(@Header("Request-Id") String requestId, @Body UpdateUserInfoRsq updateUserInfoRsq);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.MEMBER_USED_STORAGE_API)
    Observable<MemberUsedStorageResponseBody> getMemberUsedStorage(@Header("Request-Id") String requestId, @Query("aoid") String aoId);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.MEMBER_LIST_API)
    Observable<AccountInfoResult> getMemberList(@Header("Request-Id") String requestId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.UPDATE_MEMBER_NAME_API)
    Observable<MemberNameUpdateResult> updateMemberName(@Header("Request-Id") String requestId, @Body MemberNameUpdateInfo memberNameUpdateInfo);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.GATEWAY_CREATE_MEMBER_API)
    Observable<MemberCreateResult> createMember(@Header("Request-Id") String requestId, @Query("aoId") String aoId, @Body MemberCreateInfo memberCreateInfo);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.GATEWAY_REVOKE_MEMBER_API)
    Observable<RevokeMemberResponseBody> revokeMember(@Header("Request-Id") String requestId, @Body CreateTokenInfo createTokenInfo);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.GATEWAY_REVOKE_ADMIN_API)
    Observable<RevokeMemberResponseBody> revokeAdmin(@Header("Request-Id") String requestId, @Body CreateTokenInfo createTokenInfo);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.TERMINAL_INFO_API)
    Observable<TerminalListResponse> getTerminalList(@Header("Request-Id") String requestId, @Query("aoid") String aoId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @DELETE(ConstantField.URL.TERMINAL_INFO_DELETE_API)
    Observable<TerminalOfflineResponse> offlineTerminal(@Header("Request-Id") String requestId, @Query("aoid") String aoId, @Query("clientUUID") String clientUuid);
}
