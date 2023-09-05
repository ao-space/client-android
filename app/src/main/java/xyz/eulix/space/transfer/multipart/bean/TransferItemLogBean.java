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

import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Utils;

/**
 * Author:      Zhu Fuyu
 * Description: 传输任务日志对象
 * History:     2023/2/22
 */
public class TransferItemLogBean {
    //uuid
    public String uuid;
    //文件名称
    public String fileName;
    //文件大小
    public long fileSize;
    //任务类型
    public int transferType;
    //分片大小
    public long oneChunkSize;
    //分片并发数
    public long concurrentCount;
    //任务启动时间 ms
    public long taskStartTime;
    //任务结束时间
    public long taskEndTime;
    //betag计算启动时间
    public long betagStartTime;
    //betag计算结束时间
    public long betagEndTime;
    //创建传输任务开始时间
    public long createTaskStartTime;
    //创建传输任务结束时间
    public long createTaskEndTime;
    //计算分片信息开始时间
    public long chunksInfoCalStartTime;
    //计算分片信息结束时间
    public long chunksInfoCalEndTime;
    //分片数
    public long chunksCount;
    //传输开始时间
    public long transferStartTime;
    //传输结束时间
    public long transferEndTime;
    //合并开始时间
    public long completeStartTime;
    //合并结束时间
    public long completeEndTime;


    public String getLogStr() {
        StringBuilder sb = new StringBuilder();
        sb.append("[任务类型：");
        String typeStr;
        switch (transferType) {
            case TransferHelper.TYPE_UPLOAD:
                typeStr = "上传";
                break;
            case TransferHelper.TYPE_DOWNLOAD:
                typeStr = "下载";
                break;
            case TransferHelper.TYPE_CACHE:
                typeStr = "缓存";
                break;
            case TransferHelper.TYPE_SYNC:
                typeStr = "同步";
                break;
            default:
                typeStr = "未知";
        }
        sb.append(typeStr);
        sb.append("]名称：");
        sb.append(fileName);
        sb.append("，文件大小：");
        sb.append(FormatUtil.formatSize(fileSize, ConstantField.SizeUnit.FORMAT_2F));
        sb.append("，分片大小：");
        sb.append(FormatUtil.formatSize(oneChunkSize, ConstantField.SizeUnit.FORMAT_2F));
        sb.append("，分片并发数：");
        sb.append(concurrentCount);
        sb.append("，任务启动时间：");
        sb.append(FormatUtil.formatTime(taskStartTime, ConstantField.TimeStampFormat.DATE_FORMAT_ONE_DAY));
        sb.append("，总耗时：");
        if (taskEndTime > taskStartTime) {
            sb.append(FormatUtil.formatMillSecondToShowText(taskEndTime - taskStartTime));
        } else {
            sb.append("未完成");
        }
        if (transferType == TransferHelper.TYPE_UPLOAD) {
            sb.append("，betag计算耗时：");
            sb.append(FormatUtil.formatMillSecondToShowText(betagEndTime - betagStartTime));
            sb.append("，任务创建耗时：");
            sb.append(FormatUtil.formatMillSecondToShowText(createTaskEndTime - createTaskStartTime));
        }
        sb.append("，分片信息计算耗时：");
        sb.append(FormatUtil.formatMillSecondToShowText(chunksInfoCalEndTime - chunksInfoCalStartTime));
        sb.append("，传输开始时间：");
        sb.append(FormatUtil.formatTime(transferStartTime, ConstantField.TimeStampFormat.DATE_FORMAT_ONE_DAY));
        sb.append("，传输耗时：");
        if (transferEndTime > transferStartTime) {
            sb.append(FormatUtil.formatMillSecondToShowText(transferEndTime - transferStartTime));
        } else {
            sb.append("未完成");
        }
        sb.append("，传输平均速率：");
        if (transferEndTime > transferStartTime) {
            int transferSecond = (int) ((transferEndTime - transferStartTime) / 1000);
            long transPerSecond;
            if (transferSecond != 0) {
                transPerSecond = fileSize / transferSecond;
            } else {
                transPerSecond = (int) fileSize;
            }
            String speedStr = FormatUtil.formatSimpleSize(transPerSecond, ConstantField.SizeUnit.FORMAT_2F) + "/s";
            sb.append(speedStr);
        } else {
            sb.append("-");
        }
        sb.append("，合并分片耗时：");
        if (completeEndTime > completeStartTime) {
            sb.append(FormatUtil.formatMillSecondToShowText(completeEndTime - completeStartTime));
        } else {
            sb.append("-");
        }
        sb.append("，总体速率：");
        if (taskEndTime > taskStartTime) {
            int taskSecond = (int) ((taskEndTime - taskStartTime) / 1000);
            long totalPerSecond;
            if (taskSecond != 0) {
                totalPerSecond = fileSize / taskSecond;
            } else {
                totalPerSecond = fileSize;
            }
            String speedStr = FormatUtil.formatSimpleSize(totalPerSecond, ConstantField.SizeUnit.FORMAT_2F) + "/s";
            sb.append(speedStr);
        } else {
            sb.append("-");
        }
        sb.append("，分片数量：");
        sb.append(chunksCount);

        return sb.toString();
    }

}
