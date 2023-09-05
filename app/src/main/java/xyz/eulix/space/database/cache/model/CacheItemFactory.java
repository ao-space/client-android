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

package xyz.eulix.space.database.cache.model;

import android.database.Cursor;

import xyz.eulix.space.database.cache.CacheHelper;

/**
 * Author:      Zhu Fuyu
 * Description: 缓存数据模型的创建工厂
 * History:     2022/9/30
 */
public class CacheItemFactory {

    private CacheItemFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static CacheInfoItem createCacheInfo(Cursor cursor)
            throws IllegalArgumentException {
        CacheInfoItem item = new CacheInfoItem();

        try {
            item._id = cursor.getLong(cursor
                    .getColumnIndexOrThrow(CacheHelper.KEY_ID));
            item.installedApp = cursor.getString(cursor
                    .getColumnIndexOrThrow(CacheHelper.KEY_INSTALLED_APP));
            item.account = cursor.getString(cursor
                    .getColumnIndexOrThrow(CacheHelper.KEY_ACCOUNT));
            item.bak = cursor.getString(cursor
                    .getColumnIndexOrThrow(CacheHelper.KEY_BAK));
            item.ext1 = cursor.getString(cursor
                    .getColumnIndexOrThrow(CacheHelper.KEY_EXT1));
            item.ext2 = cursor.getString(cursor
                    .getColumnIndexOrThrow(CacheHelper.KEY_EXT2));
            item.ext3 = cursor.getString(cursor
                    .getColumnIndexOrThrow(CacheHelper.KEY_EXT3));
            item.ext4 = cursor.getString(cursor
                    .getColumnIndexOrThrow(CacheHelper.KEY_EXT4));
        } catch (IllegalArgumentException e) {
            throw e;
        }

        return item;
    }

}
