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

package xyz.eulix.space.database.cache;

/**
 * Author:      Zhu Fuyu
 * Description: 缓存数据库帮助类，用来对外提供一些常量定义
 * History:     2022/9/30
 */
public class CacheHelper {

    private CacheHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 数据表字段名 主键
     */
    public static final String KEY_ID = "_id";

    /**
     * 文件名
     */
    public static final String KEY_FILE_NAME = "f_name";

    /**
     * 账号（用于多账号用户区分）
     */
    public static final String KEY_ACCOUNT = "account";

    /**
     * 数据表字段名 预留字段
     */
    public static final String KEY_BAK = "bak";

    /**
     * 额外字段1
     */
    public static final String KEY_EXT1 = "ext1";

    /**
     * 额外字段2
     */
    public static final String KEY_EXT2 = "ext2";

    /**
     * 额外字段3
     */
    public static final String KEY_EXT3 = "ext3";

    /**
     * 额外字段4
     */
    public static final String KEY_EXT4 = "ext4";

    /**
     * 已安装应用信息
     */
    public static final String KEY_INSTALLED_APP = "installed_app";


}
