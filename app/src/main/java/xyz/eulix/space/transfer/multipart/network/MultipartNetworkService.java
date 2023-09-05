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

package xyz.eulix.space.transfer.multipart.network;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import xyz.eulix.space.network.files.BaseResponseBody;
import xyz.eulix.space.network.files.GetFileListResponseBody;
import xyz.eulix.space.transfer.multipart.bean.GetCertResponseBody;
import xyz.eulix.space.transfer.multipart.bean.UploadCompleteResponseBody;
import xyz.eulix.space.transfer.multipart.bean.UploadCreateRequestBody;
import xyz.eulix.space.transfer.multipart.bean.UploadCreateResponseBody;
import xyz.eulix.space.transfer.multipart.bean.UploadIdRequestBody;
import xyz.eulix.space.transfer.multipart.bean.UploadListResponseBody;
import xyz.eulix.space.util.ConstantField;

/**
 * Author:      Zhu Fuyu
 * Description: 分片上传相关接口
 * History:     2022/2/21
 */
public interface MultipartNetworkService {

    //查询恢复状态
    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.MULTI_UPLOAD_CREATE_API)
    Observable<UploadCreateResponseBody> createUpload(@Body UploadCreateRequestBody createRequestBody);

    //获取已上传列表
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.MULTI_UPLOAD_LIST_API)
    Observable<UploadListResponseBody> listUpload(@Query("uploadId") String uploadId);

    //合并已上传片段
    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.MULTI_UPLOAD_COMPLETE_API)
    Observable<UploadCompleteResponseBody> completeUpload(@Body UploadIdRequestBody requestBody);

    //删除已上传片段
    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.MULTI_UPLOAD_DELETE_API)
    Observable<BaseResponseBody> deleteUpload(@Body UploadIdRequestBody requestBody);

    //获取Https自签名证书
    @Headers({"Content-Type: application/json","Accept: application/json"})
    @GET(ConstantField.URL.GET_HTTPS_CERT_API)
    Observable<GetCertResponseBody> getHttpsCert();
}
