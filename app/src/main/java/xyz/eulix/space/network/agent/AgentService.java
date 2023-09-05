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
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import xyz.eulix.space.bean.bind.KeyExchangeReq;
import xyz.eulix.space.bean.bind.PairingBoxInfoEnc;
import xyz.eulix.space.bean.bind.PairingBoxResult;
import xyz.eulix.space.bean.bind.PubKeyExchangeReq;
import xyz.eulix.space.bean.bind.RvokInfo;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 10:36
 */
public interface AgentService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.AGENT_INFO_API)
    Observable<ResponseBody> getAgentInfo();

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.PUBLIC_KEY_EXCHANGE_API)
    Observable<PubKeyExchangeResponse> exchangePublicKey(@Body PubKeyExchangeReq body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.KEY_EXCHANGE_API)
    Observable<KeyExchangeResponse> exchangeSecretKey(@Body KeyExchangeReq body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.PAIRING_API)
    Observable<PairingResponseBody> pairing(@Body PairingClientInfo body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.PAIRING_API_V2)
    Observable<PairingResponseBody> pairingV2(@Body PairingClientInfo body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.PAIRING_API)
    Observable<PairingBoxResult> pairingV3(@Body PairingClientInfo body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.PAIRING_API)
    Observable<PairingBoxResult> pairing(@Body PairingBoxInfoEnc body);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.AUTH_INFO_API)
    Observable<AuthInfoRsp> getAuthInfo();

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.SET_PASSWORD_API)
    Observable<SetPasswordResponse> setPassword(@Body PasswordInfo body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.ADMIN_REVOKE_API)
    Observable<AdminRevokeResponse> revoke(@Body AdminRevokeReq body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.ADMIN_REVOKE_API)
    Observable<AdminRevokeResponse> revoke(@Body RvokInfo body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.RESET_API)
    Observable<BaseRsp> reset(@Body ResetClientReq body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.AGENT_PASSTHROUGH_API)
    Observable<AgentCallResponse> agentPassthrough(@Header("Request-Id") String requestId, @Body PassthroughRequest body);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.INITIAL_API)
    Observable<InitialRsp> initial();

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.INITIAL_API)
    Observable<InitialRsp> initial(@Body PasswordInfo passwordInfo);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.PAIR_INIT_API)
    Observable<PairInitResponse> pairInit();
}
