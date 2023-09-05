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

package xyz.eulix.space.transfer;

/**
 * Author:      Zhu Fuyu
 * Description: 传输任务帮助类
 * History:     2021/8/26
 */
public class TransferHelper {

    private TransferHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 数据表字段名 主键
     */
    public static final String KEY_ID = "_id";

    /**
     * 数据表字段名 显示名称
     */
    public static final String KEY_SHOW_NAME = "s_name";

    /**
     * 可作为Key的任务名称 建议:apk->包名 其它->文件名
     */
    public static final String KEY_KEY_NAME = "k_name";

    /**
     * 传输类型（上传 or 下载）
     */
    public static final String KEY_TRANSFER_TYPE = "transfer_type";

    /**
     * 文件唯一识别符（上传：成功后返回；下载：文件对应 值)
     */
    public static final String KEY_UUID = "uuid";

    /**
     * 本地路径（上传：对应源文件路径；下载：对应存储路径）
     */
    public static final String KEY_LOCAL_PATH = "l_path";

    /**
     * 远端路径（上传：对应上传位置；下载：对应服务端存储路径）
     */
    public static final String KEY_REMOTE_PATH = "r_path";

    /**
     * 缓存路径（上传时存在，为临时加密文件存放路劲；下载不存在，直接解密数据流到本地路径）
     */
    public static final String KEY_CACHE_PATH = "c_path";

    /**
     * 文件后缀
     */
    public static final String KEY_FILE_SUFFIX = "suffix";

    /**
     * 文件MIME TYPE
     */
    public static final String KEY_FILE_MIMETYPE = "mime_type";

    /**
     * 数据表字段名 已经传输的大小
     */
    public static final String KEY_SIZE_CURRENT = "size_c";

    /**
     * 数据表字段名 总大小
     */
    public static final String KEY_SIZE_TOTAL = "size_t";

    /**
     * MD5校验码
     */
    public static final String KEY_MD5 = "md5";

    /**
     * 账号（用于多账号用户区分）
     */
    public static final String KEY_ACCOUNT = "account";

    /**
     * 数据表字段名 传输状态
     */
    public static final String KEY_STATE = "state";
    

    /**
     * 数据表字段名 下载错误码，仅state为STATE_ERROR时有效
     */
    public static final String KEY_ERROR_CODE = "error_code";

    /**
     * 数据表字段名 预留字段
     */
    public static final String KEY_BAK = "bak";

    /**
     * 额外字段1（存放uniqueTag）
     */
    public static final String KEY_EXT1 = "ext1";

    /**
     * 额外字段2 （存放缓存来源）
     */
    public static final String KEY_EXT2 = "ext2";

    /**
     * 额外字段3 （存放albumId）
     */
    public static final String KEY_EXT3 = "ext3";

    /**
     * 额外字段4
     */
    public static final String KEY_EXT4 = "ext4";

    /**
     * 任务优先级
     */
    public static final String KEY_PRIORITY = "priority";

    /**
     * 任务创建时间
     */
    public static final String KEY_CREATE_TIME = "c_time";

    /**
     * 任务更新时间
     */
    public static final String KEY_UPDATE_TIME = "u_time";

    /* ====================以下为传输类型=============== */

    /**
     * 传输类型 下载
     */
    public static final int TYPE_DOWNLOAD = 100;

    /**
     * 传输类型 上传
     */
    public static final int TYPE_UPLOAD = 101;

    /**
     * 传输类型 缓存（预览、分享时使用，不在传输列表展示）
     */
    public static final int TYPE_CACHE = 102;

    /**
     * 传输类型 同步（相册同步使用）
     */
    public static final int TYPE_SYNC = 103;

    /**
     * 文件预览（图片下载压缩图、office文件下载pdf预览）
     */
    public static final int TYPE_PREVIEW = 104;

    /* ====================以下为传输状态=============== */

    /**
     * 传输状态 完成
     */
    public static final int STATE_FINISH = 1000;

    /**
     * 传输状态 准备
     */
    public static final int STATE_PREPARE = 1005;

    /**
     * 传输状态 传输中
     */
    public static final int STATE_DOING = 1010;

    /**
     * 传输状态 文件校验中
     */
    public static final int STATE_VERIFY = 1015;

    /**
     * 传输状态 排队等待
     */
    public static final int STATE_QUEUE = 1020;

    /**
     * 传输状态 暂停
     */
    public static final int STATE_PAUSE = 1030;

    /**
     * 传输状态 暂停
     */
    public static final int STATE_PAUSING = 1035;

    /**
     * 传输状态 错误中断
     */
    public static final int STATE_ERROR = 1040;

    /**
     * 传输取消 数据库中不记录此状态
     */
    public static final int STATE_CANCEL = 1050;


    /* ====================以下为下载错误代码=============== */

    public static final int ERROR_CODE_NORMAL = -1;

    public static final int ERROR_CODE_IO = -100;

    public static final int ERROR_CODE_MEMORY = -101;

    public static final int ERROR_CODE_STORAGE_FULL = -102;

    public static final int ERROR_CODE_NETWORK = -200;


    /**
     * 存储位置 0系统目录下
     */
    public static final int LOCATION_ROM = 0;

    /**
     * 存储位置 1SDCARD中
     */
    public static final int LOCATION_SDCARD = 1;

    /**
     * 存储位置 2额外的存储位置或自定义的存储位置
     */
    public static final int LOCATION_EXT = 2;

    //来源-智能相册
    public static final String FROM_ALBUM = "from_album";
    //来源-文件系统
    public static final String FROM_FILE = "from_file";

}
