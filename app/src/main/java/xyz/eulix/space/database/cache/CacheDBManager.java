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
import android.text.TextUtils;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.database.cache.model.CacheInfoItem;
import xyz.eulix.space.database.cache.model.CacheItemFactory;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 缓存数据库管理类
 * History:     2022/9/30
 */
public class CacheDBManager {
    private static final String TAG = "CacheDBManager";

    private static CacheDBManager sInstance = null;

    private Context mContext = null;

    private CacheDBHelper mDBHelper;

    private SQLiteDatabase db;


    private String AND_ = " AND ";
    private static final String DESC_STR = " desc";
    private static final String ASC_STR = " asc";

    private CacheDBManager(Context c) {
        mContext = c;
        if (mDBHelper == null) {
            mDBHelper = new CacheDBHelper(mContext);
        }
    }

    /**
     * 获得一个数据库管理实例 模块内部使用
     *
     * @param c
     * @return
     */
    public static CacheDBManager getInstance(Context c) {
        if (sInstance == null) {
            sInstance = new CacheDBManager(c);
        }

        sInstance.initDB();

        return sInstance;
    }

    /**
     * 初始化数据库 ，在使用原始基本数据库操作之前必须执行此方法。
     *
     * @return
     */
    public boolean initDB() {
        boolean result = true;

        try {
            mDBHelper.open();
            db = mDBHelper.getSQLiteDb();
        } catch (SQLException e) {
            Logger.d(TAG, e.getMessage());
            result = false;
        }

        return result;
    }

    /**
     * 关闭数据库
     */
    public void closeDB() {
        mDBHelper.close();
    }

    /**
     * 基本查询方法
     *
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    public Cursor queryCacheInfo(String[] projection, String selection,
                                 String[] selectionArgs, String sortOrder) {
        String where = CacheHelper.KEY_ACCOUNT + " = '" + EulixSpaceDBUtil.queryAvailableBoxUuid(EulixSpaceApplication.getContext()) + EulixSpaceDBUtil.getCurrentUserId(EulixSpaceApplication.getContext()) + "'";
        if (!TextUtils.isEmpty(selection)) {
            where = where + " and " + selection;
        }
        Cursor cursor = null;
        try {
            cursor = mDBHelper.queryCacheInfo(projection, where, selectionArgs,
                    sortOrder);
        } catch (SQLException e) {
            closeDB();
            initDB();
            cursor = mDBHelper.queryCacheInfo(projection, selection, selectionArgs,
                    sortOrder);
        }

        return cursor;
    }


    /* ===========以下是插入接口============ */

    /**
     * 基本插入接口
     *
     * @param values
     * @return
     */
    public long insertCacheInfo(ContentValues values) {
        long result = -1;
        values.put(CacheHelper.KEY_ACCOUNT, EulixSpaceDBUtil.queryAvailableBoxUuid(EulixSpaceApplication.getContext()) + EulixSpaceDBUtil.getCurrentUserId(EulixSpaceApplication.getContext()));
        result = mDBHelper.insertCacheInfo(values);

        return result;
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
        String where = CacheHelper.KEY_ACCOUNT + " = '" + EulixSpaceDBUtil.queryAvailableBoxUuid(EulixSpaceApplication.getContext()) + EulixSpaceDBUtil.getCurrentUserId(EulixSpaceApplication.getContext()) + "'";
        if (!TextUtils.isEmpty(selection)) {
            where = where + " and " + selection;
        }
        return mDBHelper.updateCacheInfo(values, where, selectionArgs);
    }

    public void clearTable() {
        mDBHelper.clearTableData();
    }

    public void createTable() {
        mDBHelper.createTable();
    }

    //升级account值
    public long upgradeAccountValue(String oldAccount, String newAccount) {
        int result = -1;
        if (TextUtils.isEmpty(oldAccount)) {
            return result;
        }

        String where = CacheHelper.KEY_ACCOUNT + " = '" + oldAccount + "'";

        ContentValues cv = new ContentValues();
        cv.put(CacheHelper.KEY_ACCOUNT, newAccount);

        int accountCache = mDBHelper.updateCacheInfo(cv, where, null);
        Logger.d("zfy", "updateCacheInfo count = " + accountCache);
        if (accountCache > -1) {
            result = accountCache;
        }

        //新建contentValue，防止cv复用被内部修改
        ContentValues cvPhoto = new ContentValues();
        cvPhoto.put(CacheHelper.KEY_ACCOUNT, newAccount);

        ContentValues cvAlbum = new ContentValues();
        cvAlbum.put(CacheHelper.KEY_ACCOUNT, newAccount);

        return result;
    }

    public long deleteByAccount(String account) {
        int result = -1;
        String where = CacheHelper.KEY_ACCOUNT + " = '" + account + "'";
        int accountCache = mDBHelper.deleteCacheInfo(where, null);
        if (accountCache > -1) {
            if (result > -1) {
                result += accountCache;
            } else {
                result = accountCache;
            }
        }
        return result;
    }
}
