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

package xyz.eulix.space.transfer.multipart.bean;

import com.google.gson.annotations.SerializedName;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.files.BaseResponseBody;
import xyz.eulix.space.network.files.FileListItem;

/**
 * Author:      Zhu Fuyu
 * Description: 分片上传-合并片段响应数据
 * History:     2022/2/22
 */
public class UploadCompleteResponseBody extends BaseResponseBody implements EulixKeep {
    @SerializedName("results")
    public FileListItem Results;
}
