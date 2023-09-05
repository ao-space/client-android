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

package xyz.eulix.space.network.files;

import io.reactivex.Observable;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * date: 2021/6/22 16:30
 */
public interface FileListService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.FILE_LIST_API)
    Observable<GetFileListResponseBody> getFileList(@Query("uuid") String uuid, @Query("page") Integer page
            , @Query("pageSize") Integer pageSize, @Query("orderBy") String order, @Query("category") String category);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.FOLDER_INFO_API)
    Observable<FolderInfoResponseBody> getFolderInfo(@Query("uuid") String uuid);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.RENAME_FILE_API)
    Observable<FileRsp> modifyFile(@Query("uuid") String uuid, @Query("fileName") String fileName, @Body RenameFilesReq renameFilesReq);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.COPY_FILE_API)
    Observable<FileRsp> copyFile(@Body CopyFilesReq copyFilesReq);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.MOVE_FILE_API)
    Observable<FileRsp> moveFile(@Body MoveFilesReq moveFilesReq);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.DELETE_FILE_API)
    Observable<FileRsp> deleteFile(@Body FileUUIDs fileUUIDs);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @GET(ConstantField.URL.ASYNC_TASK_STATUS_API)
    Observable<AsyncTaskStatusResponseBody> checkAsyncTaskStatus(@Query("taskId") String taskId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.CREATE_FOLDER_API)
    Observable<NewFolderRsp> createFolder(@Body CreateFolderReq createFolderReq);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.SEARCH_FILE_API)
    Observable<GetFileListResponseBody> searchFile(@Query("uuid") String uuid, @Query("name") String name
            , @Query("category") String category, @Query("page") Integer page
            , @Query("pageSize") Integer pageSize, @Query("order") String order);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.FILE_INFO_API)
    Observable<UploadResponseBodyResult> getFileInfo(@Query("path") String path, @Query("name") String name);

    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.RECYCLED_LIST_API)
    Observable<RecycledListResponse> getRecycledList(@Query("page") Integer page, @Query("pageSize") Integer pageSize);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.RECYCLED_RESTORE_API)
    Observable<RecycledListResponse> restoreRecycled(@Body FileUUIDs fileUUIDs);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.RECYCLED_CLEAR_API)
    Observable<RecycledListResponse> clearRecycled(@Body FileUUIDs fileUUIDs);
}
