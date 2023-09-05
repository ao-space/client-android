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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 缓存数据库实现类
 * History:     2022/9/30
 */
public class CacheDBHelper {
    private static final String LOG_TAG = "CacheDb";

    private static final String DATABASE_NAME = "eulixcache";// 数据库名

    public static final String TABLE_CACHE_INFO = "cacheinfo";

    private static final int DATABASE_VERSION = 2; // 数据库版本号

    private final Context context;

    private DBOpenHelper mDBOpenHelper;

    private SQLiteDatabase db;

    private Object sycObj = new Object();
    private static String TEXT_ = " text,";
    private static String INTEGER_ = " integer,";

    /**
     * 创建缓存信息表
     *
     * @version 2
     */
    private static final String CREATE_TABLE_CACHE_INFO = "create table if not exists "
            + TABLE_CACHE_INFO
            + " ("
            + CacheHelper.KEY_ID
            + " integer primary key autoincrement, " // version2
            + CacheHelper.KEY_ACCOUNT + TEXT_
            + CacheHelper.KEY_INSTALLED_APP + TEXT_
            + CacheHelper.KEY_EXT1 + TEXT_
            + CacheHelper.KEY_EXT2 + TEXT_
            + CacheHelper.KEY_EXT3 + TEXT_
            + CacheHelper.KEY_EXT4 + TEXT_
            + CacheHelper.KEY_BAK + " text"
            + ");";
    private static final String DROP_TABLE_CACHE_INFO = "DROP TABLE IF EXISTS " + TABLE_CACHE_INFO;

    public CacheDBHelper(Context ctx) {
        context = ctx;
        mDBOpenHelper = new DBOpenHelper(context);
    }

    /**
     * 打开数据库
     *
     * @return
     * @throws SQLException
     */
    public void open() throws SQLException {
        if (db == null) {
            if (mDBOpenHelper == null) {
                mDBOpenHelper = new DBOpenHelper(context);
            }

            db = mDBOpenHelper.getWritableDatabase();
        }
    }

    /**
     * 关闭数据库
     */
    public void close() {
//        mDBOpenHelper.close();
//        db = null;
    }

    /**
     * 暴露sqlitedb对象
     *
     * @return
     */
    public SQLiteDatabase getSQLiteDb() {
        return db;
    }

    /**
     * 基本查询接口
     *
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    public Cursor queryCacheInfo(String[] projection, String selection,
                                 String[] selectionArgs, String sortOrder) {
        open();
        return db.query(TABLE_CACHE_INFO, projection, selection, selectionArgs, null,
                null, sortOrder);
    }


    /**
     * 清除表数据
     */
    public void clearTableData() {
        open();
        db.execSQL(DROP_TABLE_CACHE_INFO);
    }

    /**
     * 清除表数据
     */
    public void createTable() {
        open();
        try {
            db.execSQL(CREATE_TABLE_CACHE_INFO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long insertCacheInfo(ContentValues values) {
        open();
        synchronized (sycObj) {
            try {
                return db.insert(TABLE_CACHE_INFO, null, values);
            } catch (SQLException e) {
                // TODO: handle exception
                Logger.d(LOG_TAG, e.getMessage());
                return -1;
            }
        }
    }


    /**
     * 基本更新接口
     *
     * @param values
     * @param selection
     * @param selectionArgs
     * @return
     */

    public int updateCacheInfo(ContentValues values, String selection,
                               String[] selectionArgs) {
        open();
        synchronized (sycObj) {
            try {
                return db.update(TABLE_CACHE_INFO, values, selection, selectionArgs);
            } catch (SQLException e) {
                Logger.d(LOG_TAG, e.getMessage());
                return -1;
            }
        }
    }


    /**
     * 基本删除接口
     *
     * @param selection
     * @param selectionArgs
     * @return
     */
    public int deleteCacheInfo(String selection, String[] selectionArgs) {
        open();
        synchronized (sycObj) {
            try {
                return db.delete(TABLE_CACHE_INFO, selection, selectionArgs);
            } catch (SQLException e) {
                Logger.d(LOG_TAG, e.getMessage());
                return -1;
            }

        }
    }


    private static class DBOpenHelper extends SQLiteOpenHelper {

        DBOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_CACHE_INFO);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                upgradeToVersion2(db);
            }
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion > newVersion) {
                // 数据库降级清空数据
                db.execSQL(DROP_TABLE_CACHE_INFO);
                db.execSQL(CREATE_TABLE_CACHE_INFO);
                Logger.d(LOG_TAG, "Upgrading database from version " + oldVersion
                        + " to " + newVersion + ", which will destroy all old data");
            }

        }

        private void upgradeToVersion2(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_CACHE_INFO);
        }
    }
}