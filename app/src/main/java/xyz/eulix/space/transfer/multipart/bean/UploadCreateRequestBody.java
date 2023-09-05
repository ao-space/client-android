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

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * Author:      Zhu Fuyu
 * Description: 分片上传任务创建请求参数
 * History:     2022/2/21
 */
public class UploadCreateRequestBody implements Serializable, EulixKeep {

    //文件的hash值，文件hash计算分段大小固定为4M， hash 算法为 md5
    public String betag;
    //业务来源id， 0-默认值， 1-同步，2-智能相册
    public int businessId = 0;
    //文件创建时间
    public long createTime;
    //文件名
    public String fileName;
    //文件夹的id, 和 folderPath 二选1， folderId优先
//    public String folderId;
    //文件夹的路径，folderId与folderPath只能存在1个
    public String folderPath;
    //文件类型
    public String mime;
    //文件修改时间
    public long modifyTime;
    //文件长度，单位：Byte
    public long size;
    //相册id
    public int albumId = -1;
}
