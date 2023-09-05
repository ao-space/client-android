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

package xyz.eulix.space.network.video;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import xyz.eulix.space.network.files.BaseResponseBody;
import xyz.eulix.space.util.ConstantField;

/**
 * Author:      Zhu Fuyu
 * Description: 视频播放相关接口
 * History:     2023/1/5
 */
public interface VideoNetService {

    //查询是否支持
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.MEDIA_VOD_CHECK)
    Observable<BaseResponseBody> videoCheck(@Query("uuid") String uuid);

}
