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

package xyz.eulix.space.network.register;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * date: 2021/6/17 9:12
 */
public interface RegisterDeviceService {
    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.REGISTER_DEVICE_API)
    Observable<RegisterDeviceResponseBody> registerDevice(@Body RequestBody body);
}
