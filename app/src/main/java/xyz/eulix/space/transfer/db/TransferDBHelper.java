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

package xyz.eulix.space.transfer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


import java.util.List;

import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 传输数据库实现类
 * History:     2021/8/26
 */
public class TransferDBHelper {
    private static final String LOG_TAG = "TransferDb";

    private static final String DATABASE_NAME = "eulixtransfer";// 数据库名

    public static final String TABLE_TASK = "transfer";
    
    private static final int DATABASE_VERSION = 1; // 数据库版本号

    private final Context context;

    private DBOpenHelper mDBOpenHelper;

    private SQLiteDatabase db;

    private Object sycObj = new Object();
    private static String TEXT_ = " text,";
    private static String INTEGER_ = " integer,";

    /**
     * 创建Task表
     *
     * @version 1
     */
    private static final String CREATE_TABLE_TASK = "create table if not exists "
            + TABLE_TASK
            + " ("
            + TransferHelper.KEY_ID
            + " integer primary key autoincrement, "
            + TransferHelper.KEY_SHOW_NAME + TEXT_
            + TransferHelper.KEY_KEY_NAME + TEXT_
            + TransferHelper.KEY_TRANSFER_TYPE + INTEGER_
            + TransferHelper.KEY_UUID + TEXT_
            + TransferHelper.KEY_LOCAL_PATH + TEXT_
            + TransferHelper.KEY_REMOTE_PATH + TEXT_
            + TransferHelper.KEY_CACHE_PATH + TEXT_
            + TransferHelper.KEY_FILE_SUFFIX + TEXT_
            + TransferHelper.KEY_FILE_MIMETYPE + TEXT_
            + TransferHelper.KEY_SIZE_CURRENT + TEXT_
            + TransferHelper.KEY_SIZE_TOTAL + TEXT_
            + TransferHelper.KEY_MD5 + TEXT_
            + TransferHelper.KEY_ACCOUNT + TEXT_
            + TransferHelper.KEY_STATE + INTEGER_
            + TransferHelper.KEY_ERROR_CODE + INTEGER_
            + TransferHelper.KEY_BAK + TEXT_
            + TransferHelper.KEY_EXT1 + TEXT_
            + TransferHelper.KEY_EXT2 + TEXT_
            + TransferHelper.KEY_EXT3 + TEXT_
            + TransferHelper.KEY_EXT4 + TEXT_
            + TransferHelper.KEY_PRIORITY + INTEGER_
            + TransferHelper.KEY_CREATE_TIME + TEXT_
            + TransferHelper.KEY_UPDATE_TIME + " text"
            + ");";

    private static final String DROP_TABLE_TASK = "DROP TABLE IF EXISTS " + TABLE_TASK;

    public TransferDBHelper(Context ctx) {
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
    public Cursor query(String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        open();
        return db.query(TABLE_TASK, projection, selection, selectionArgs, null,
                null, sortOrder);
    }

    /**
     * 通过SQL语句查询
     * @param sql
     * @param args
     */
    public void queryBySql(String sql, Object[] args) {
        open();
        db.execSQL(sql, args);
    }

    /**
     * 清除表数据
     */
    public void clearTableData(){
        open();
        db.execSQL(DROP_TABLE_TASK);
    }

    /**
     * 清除表数据
     */
    public void createTable(){
        open();
        try {
            db.execSQL(CREATE_TABLE_TASK);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    /**
     * 基本插入接口
     *
     * @param values
     * @return
     */
    public long insert(ContentValues values) {
        open();
        synchronized (sycObj) {
            try {
                values.put(TransferHelper.KEY_CREATE_TIME,
                        System.currentTimeMillis());
                return db.insert(TABLE_TASK, TransferHelper.KEY_SHOW_NAME, values);
            } catch (SQLException e) {
                // TODO: handle exception
                Logger.d(LOG_TAG, e.getMessage());
                return -1;
            }
        }
    }

    public long insertList(List<TransferItem> list){
        long result = -1;
        if (list == null){
            return -1;
        }
        open();
        synchronized (sycObj) {
            db.beginTransaction();

            try{

                ContentValues cv = new ContentValues();
                for(int i = 0 ; i < list.size() ; i++){
                    TransferItem item = list.get(i);
                    cv.put(TransferHelper.KEY_SHOW_NAME, item.showName);
                    cv.put(TransferHelper.KEY_KEY_NAME, item.keyName);
                    cv.put(TransferHelper.KEY_TRANSFER_TYPE, item.transferType);
                    cv.put(TransferHelper.KEY_FILE_SUFFIX, item.suffix);
                    cv.put(TransferHelper.KEY_FILE_MIMETYPE, item.mimeType);
                    cv.put(TransferHelper.KEY_UUID, item.uuid);
                    cv.put(TransferHelper.KEY_LOCAL_PATH, item.localPath);
                    cv.put(TransferHelper.KEY_REMOTE_PATH, item.remotePath);
                    cv.put(TransferHelper.KEY_CACHE_PATH, item.cachePath);
                    cv.put(TransferHelper.KEY_SIZE_CURRENT, item.currentSize);
                    cv.put(TransferHelper.KEY_SIZE_TOTAL, item.totalSize);
                    cv.put(TransferHelper.KEY_MD5, item.md5);
                    cv.put(TransferHelper.KEY_ACCOUNT, item.account);
                    cv.put(TransferHelper.KEY_STATE, item.state);
                    cv.put(TransferHelper.KEY_ERROR_CODE, item.errorCode);
                    cv.put(TransferHelper.KEY_BAK, item.bak);
                    cv.put(TransferHelper.KEY_EXT1, item.ext1);
                    cv.put(TransferHelper.KEY_EXT2, item.ext2);
                    cv.put(TransferHelper.KEY_EXT3, item.ext3);
                    cv.put(TransferHelper.KEY_EXT4, item.ext4);
                    cv.put(TransferHelper.KEY_PRIORITY, item.priority);
                    cv.put(TransferHelper.KEY_CREATE_TIME, item.createTime);
                    cv.put(TransferHelper.KEY_UPDATE_TIME, item.updateTime);
                    result = db.insert(TABLE_TASK, TransferHelper.KEY_SHOW_NAME, cv);
                    cv.clear();
                }

                db.setTransactionSuccessful();

            } finally {
                db.endTransaction();
            }
        }
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
    public int update(ContentValues values, String selection,
            String[] selectionArgs) {
        open();
        synchronized (sycObj) {
            try {
                values.put(TransferHelper.KEY_UPDATE_TIME,
                        System.currentTimeMillis());
                return db.update(TABLE_TASK, values, selection, selectionArgs);
            } catch (SQLException e) {
                // TODO: handle exception
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
    public int delete(String selection, String[] selectionArgs) {
        open();
        synchronized (sycObj) {
            try {
                return db.delete(TABLE_TASK, selection, selectionArgs);
            } catch (SQLException e) {
                // TODO: handle exception
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
            db.execSQL(CREATE_TABLE_TASK);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion > newVersion) {
                // 数据库降级清空数据
                db.execSQL(DROP_TABLE_TASK);
                db.execSQL(CREATE_TABLE_TASK);
                Logger.d(LOG_TAG, "Upgrading database from version " + oldVersion
                        + " to " + newVersion + ", which will destroy all old data");
            }
        }
    }
}