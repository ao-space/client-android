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
import android.os.SystemClock;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.database.cache.CacheHelper;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.event.TransferSizeEvent;
import xyz.eulix.space.transfer.event.TransferStateEvent;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.transfer.model.TransferItemFactory;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 传输数据库管理类
 * History:     2021/8/26
 */
public class TransferDBManager {
    private static final String TAG = "TransferDB";
    //进度刷新时间间隔
    private long MIN_SIZE_INTERVAL = 100;

    private static TransferDBManager sInstance = null;

    private Context mContext = null;

    private TransferDBHelper mDBHelper;

    private SQLiteDatabase db;


    private long lastUpdateTime = 0;

    private String AND_ = " AND ";
    private static final String DESC_STR = " desc";
    private static final String ASC_STR = " asc";

    private TransferDBManager(Context c) {
        mContext = c;
        if (mDBHelper == null) {
            mDBHelper = new TransferDBHelper(mContext);
        }
    }

    /**
     * 获得一个数据库管理实例 模块内部使用
     *
     * @param c
     * @return
     */
    public static TransferDBManager getInstance(Context c) {
        if (sInstance == null) {
            sInstance = new TransferDBManager(c);
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
            // TODO: handle exception
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
    public Cursor query(String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String where = TransferHelper.KEY_ACCOUNT + " = '" + EulixSpaceDBUtil.queryAvailableBoxUuid(EulixSpaceApplication.getContext()) + EulixSpaceDBUtil.getCurrentUserId(EulixSpaceApplication.getContext()) + "'";
        if (!TextUtils.isEmpty(selection)) {
            where = where + " and " + selection;
        }
        Cursor cursor = null;
        try {
            cursor = mDBHelper.query(projection, where, selectionArgs,
                    sortOrder);
        } catch (SQLException e) {
            // TODO: handle exception
            closeDB();
            initDB();
            cursor = mDBHelper.query(projection, selection, selectionArgs,
                    sortOrder);
        }

        return cursor;
    }

    /**
     * 根据唯一标识符查找传输数据
     *
     * @param uniqueTag 唯一标识符（下载：uuid；上传：localPath+remotePath+fileName
     * @return 如果不存在，则返回null.
     */
    public TransferItem queryByUniqueTag(String uniqueTag, int transferType) {
        TransferItem item = null;
        if (TextUtils.isEmpty(uniqueTag)) {
            return item;
        }

        String where = TransferHelper.KEY_EXT1 + " = '" + uniqueTag + "'" +
                AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;

        Cursor cursor = null;

        try {
            cursor = query(null, where, null, null);

            if (cursor == null || cursor.getCount() <= 0) {
                return item;
            }

            cursor.moveToFirst();
            item = TransferItemFactory.create(cursor);
        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
        return item;
    }


    public TransferItem queryByName(String name, int transferType) {
        TransferItem item = null;
        if (TextUtils.isEmpty(name)) {
            return item;
        }

        String where = TransferHelper.KEY_KEY_NAME + " = '" + name + "'" +
                AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;

        Cursor cursor = null;

        try {
            cursor = query(null, where, null, null);

            if (cursor == null || cursor.getCount() <= 0) {
                return item;
            }

            cursor.moveToFirst();
            item = TransferItemFactory.create(cursor);
        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
        return item;
    }

    //根据uuid查找已传输完成的任务
    public ArrayList<TransferItem> queryFinishItemsByUUID(String uuid) {
        ArrayList<TransferItem> list = null;

        String where = TransferHelper.KEY_UUID + " = '" + uuid + "'" +
                AND_ + TransferHelper.KEY_STATE + " = " + TransferHelper.STATE_FINISH
                + AND_ + " (" + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_DOWNLOAD
                + " OR " + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_UPLOAD
                + " OR " + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_CACHE
                + " OR " + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_SYNC + ")";

        Cursor cursor = null;

        try {
            cursor = query(null, where, null, null);

            if (cursor == null || cursor.getCount() <= 0) {
                return list;
            }
            list = new ArrayList<>();
            cursor.moveToFirst();
            do {
                list.add(TransferItemFactory.create(cursor));
            } while (cursor.moveToNext());

        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
        return list;
    }

    //根据md5找已下载/缓存完成的任务
    public ArrayList<TransferItem> queryDownloadFinishItemsByMd5(String md5) {
        ArrayList<TransferItem> list = null;

        String where = TransferHelper.KEY_STATE + " = " + TransferHelper.STATE_FINISH
                + AND_ + " (" + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_DOWNLOAD
                + " OR " + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_UPLOAD + ") "
                + AND_ + TransferHelper.KEY_MD5 + " = '" + md5 + "'";

        Cursor cursor = null;

        try {
            cursor = query(null, where, null, null);

            if (cursor == null || cursor.getCount() <= 0) {
                return list;
            }
            list = new ArrayList<>();
            cursor.moveToFirst();
            do {
                list.add(TransferItemFactory.create(cursor));
            } while (cursor.moveToNext());

        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
        return list;
    }

    public ArrayList<TransferItem> queryByState(int state) {
        ArrayList<TransferItem> list = null;
        String where = TransferHelper.KEY_STATE + " = " + state;

        Cursor cursor = null;
        try {
            cursor = query(null, where, null, null);

            if (cursor == null || cursor.getCount() <= 0) {
                return list;
            }

            cursor.moveToFirst();

            // 将所有任务取出
            list = new ArrayList<TransferItem>();
            do {
                list.add(TransferItemFactory.create(cursor));
            } while (cursor.moveToNext());

        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return list;
    }

    /**
     * 查询所有传输中的任务数量（缓存下载不计数）
     *
     * @return 返回任务列表，如果没有则返回null
     */
    public int queryTransferringCounts() {
        ArrayList<TransferItem> list = null;
        String where = TransferHelper.KEY_STATE + " = " + TransferHelper.STATE_DOING
                + " OR " + TransferHelper.KEY_STATE + " = " + TransferHelper.STATE_VERIFY
                + AND_ + " (" + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_DOWNLOAD
                + " OR " + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_UPLOAD + ")";
        Cursor cursor = null;
        try {
            cursor = query(null, where, null, null);

            if (cursor == null || cursor.getCount() <= 0) {
                return 0;
            } else {
                list = new ArrayList<>();
                do {
                    list.add(TransferItemFactory.create(cursor));
                } while (cursor.moveToNext());
                return list.size();
            }
        } catch (Exception ex) {
            Logger.d(TAG, ex.getMessage());
            return 0;
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
    }

    /**
     * 查询所有未完成的任务，KEY_STATE > DownHelper.STATE_FINISH
     *
     * @return 返回任务列表，如果没有则返回null
     */
    public ArrayList<TransferItem> queryUnfinishedTasks(int transferType) {
        ArrayList<TransferItem> list = null;
        String where = TransferHelper.KEY_STATE + " <> " + TransferHelper.STATE_FINISH
                + AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;

        Cursor cursor = null;
        try {
            cursor = query(null, where, null, TransferHelper.KEY_CREATE_TIME + ASC_STR);

            if (cursor == null || cursor.getCount() <= 0) {
                return list;
            }

            cursor.moveToFirst();

            // 将所有任务取出
            list = new ArrayList<TransferItem>();
            do {
                list.add(TransferItemFactory.create(cursor));
            } while (cursor.moveToNext());

        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }

        return list;
    }

    /**
     * 查询所已完成的任务，KEY_STATE > DownHelper.STATE_FINISH
     *
     * @return 返回任务列表，如果没有则返回null
     */
    public ArrayList<TransferItem> queryFinishedTasks(int transferType) {
        ArrayList<TransferItem> list = null;
        String where = TransferHelper.KEY_STATE + " = " + TransferHelper.STATE_FINISH
                + AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;

        String sortOrder = TransferHelper.KEY_UPDATE_TIME + " desc ";
        Cursor cursor = null;
        try {
            cursor = query(null, where, null, sortOrder);

            if (cursor == null || cursor.getCount() <= 0) {
                return list;
            }

            cursor.moveToFirst();

            // 将所有任务取出
            list = new ArrayList<TransferItem>();
            do {
                list.add(TransferItemFactory.create(cursor));
            } while (cursor.moveToNext());

        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }

        return list;
    }

    public int queryDoingTasksCounts() {
        String where = TransferHelper.KEY_STATE + " <> " + TransferHelper.STATE_FINISH
                + AND_ + " (" + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_DOWNLOAD
                + " OR " + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_UPLOAD +")";

        Cursor cursor = null;
        try {
            cursor = query(null, where, null, null);

            if (cursor != null) {
                return cursor.getCount();
            }
        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
        return 0;
    }

    public int queryTasksCounts(int transferType) {
        String where = TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;

        Cursor cursor = null;
        try {
            cursor = query(null, where, null, null);

            if (cursor != null) {
                return cursor.getCount();
            }
        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
        return 0;
    }

    //获取随后一个同步的图片数据
    public TransferItem queryLastImageSyncItem() {
        TransferItem item = null;

        String where = TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_SYNC +
                AND_ + TransferHelper.KEY_FILE_MIMETYPE + " like '%image%'";

        Cursor cursor = null;

        try {
            cursor = query(null, where, null, TransferHelper.KEY_UPDATE_TIME + DESC_STR);

            if (cursor == null || cursor.getCount() <= 0) {
                return item;
            }

            cursor.moveToFirst();
            item = TransferItemFactory.create(cursor);
        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
        return item;
    }

    //获取随后一个同步的图片数据
    public TransferItem queryLastVideoSyncItem() {
        TransferItem item = null;

        String where = TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_SYNC +
                AND_ + TransferHelper.KEY_FILE_MIMETYPE + " like '%video%'";

        Cursor cursor = null;

        try {
            cursor = query(null, where, null, TransferHelper.KEY_UPDATE_TIME + DESC_STR);

            if (cursor == null || cursor.getCount() <= 0) {
                return item;
            }

            cursor.moveToFirst();
            item = TransferItemFactory.create(cursor);
        } catch (SQLException ex) {
            Logger.d(TAG, ex.getMessage());
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
        return item;
    }

    /* ===========以下是插入接口============ */

    /**
     * 基本插入接口
     *
     * @param values
     * @return
     */
    public long insert(ContentValues values) {
        return mDBHelper.insert(values);
    }

    /**
     * 通过下载模型插入
     *
     * @param item
     * @return
     */
    public long insert(TransferItem item) {
        long result = -1;

        if (item == null) {
            return result;
        }

        ContentValues cv = new ContentValues();
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

        long rowId = insert(cv);
        if (rowId != -1) {
            if (item.state == TransferHelper.STATE_QUEUE) {
//                DownEventManager.postEvent(new DownStateEvent(
//                        new DownStateEvent.Builder(item.keyName, item.state)));
//            } else if (item.state == DownHelper.STATE_PAUSE) {Tags(item.keyName, item.state, item.tag));
//                DownEventManager.postEvent(new DownStateEvent.Builder(
//                        item.keyName, item.state).putTag(item.tag).build());
            }
        }

        return rowId;
    }

    public long insertList(List<TransferItem> list) {
        long result = -1;
        if (list == null || list.isEmpty()) {
            return result;
        }
        result = mDBHelper.insertList(list);

        return result;
    }

    /* ===========以下是更新接口============ */

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
        String where = TransferHelper.KEY_ACCOUNT + " = '" + EulixSpaceDBUtil.queryAvailableBoxUuid(EulixSpaceApplication.getContext()) + EulixSpaceDBUtil.getCurrentUserId(EulixSpaceApplication.getContext()) + "'";
        if (!TextUtils.isEmpty(selection)) {
            where = where + " and " + selection;
        }
        return mDBHelper.update(values, where, selectionArgs);
    }

    /**
     * 更新指定id的记录
     *
     * @param id
     * @param values
     * @param selection
     * @param selectionArgs
     * @return
     */
    public int updateById(long id, ContentValues values, String selection,
                          String[] selectionArgs) {
        String where = "id=" + id;
        if (!TextUtils.isEmpty(selection)) {
            where = where + " and" + selection;
        }

        return mDBHelper.update(values, where, selectionArgs);
    }

    /**
     * 更新指定文件名的已经下载的大小
     *
     * @param keyName
     * @param totalSize
     * @return
     */
    public int updateTotalSize(String keyName, long totalSize) {
        int result = -1;
        if (TextUtils.isEmpty(keyName) || totalSize < 0) {
            return result;
        }

        String where = TransferHelper.KEY_KEY_NAME + " = '" + keyName + "'";

        ContentValues cv = new ContentValues(1);
        cv.put(TransferHelper.KEY_SIZE_TOTAL, totalSize);

        result = update(cv, where, null);

        return result;
    }

    /**
     * 更新指定文件名的已经下载的大小
     *
     * @param keyName
     * @param currentSize
     * @return
     */
    public int updateTransferSize(String keyName, int transferType, long currentSize, long totalSize, boolean isFeedBack, String uniqueTag) {
        int result = -1;
        if (TextUtils.isEmpty(keyName) || currentSize < 0) {
            return result;
        }

        String where = TransferHelper.KEY_EXT1 + " = '" + uniqueTag + "'"
                + AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;

        ContentValues cv = new ContentValues();
        cv.put(TransferHelper.KEY_SIZE_CURRENT, currentSize);
        cv.put(TransferHelper.KEY_SIZE_TOTAL, totalSize > 0L ? totalSize : 1L);

        result = update(cv, where, null);

        long now = SystemClock.elapsedRealtime();
        long timeDelta = now - lastUpdateTime;
        if (timeDelta > MIN_SIZE_INTERVAL && isFeedBack) {
            EventBusUtil.post(new TransferSizeEvent(keyName, transferType, currentSize, totalSize, uniqueTag));
            lastUpdateTime = now;
        }
        return result;
    }

    /**
     * 更新指定任务的传输状态
     *
     * @param keyName
     * @param state
     * @param code
     * @param isFeedBack
     * @return
     */
    public int updateTransferState(String keyName, int transferType, int state, int code,
                                   String uuid, boolean isFeedBack, String uniqueTag) {
        int result = -1;
        if (TextUtils.isEmpty(uniqueTag)) {
            return result;
        }

        String where = TransferHelper.KEY_EXT1 + " = '" + uniqueTag + "'"
                + AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;

        ContentValues cv = new ContentValues();
        cv.put(TransferHelper.KEY_STATE, state);

        if (state == TransferHelper.STATE_ERROR && code != 0) {
            cv.put(TransferHelper.KEY_ERROR_CODE, code);
        }
        if (!TextUtils.isEmpty(uuid)) {
            cv.put(TransferHelper.KEY_UUID, uuid);
        }

        result = update(cv, where, null);

        if (isFeedBack) {
            Logger.d("zfy", "post transfer state event:" + state);
            EventBusUtil.post(new TransferStateEvent(keyName, transferType, state, uniqueTag));
        }

        return result;
    }

    /**
     * 更新指定任务的传输状态
     *
     * @param uniqueTag
     * @param remotePath
     * @return
     */
    public int updateTransferRemotePath(String uniqueTag, String remotePath) {
        int result = -1;
        if (TextUtils.isEmpty(uniqueTag)) {
            return result;
        }

        String where = TransferHelper.KEY_EXT1 + " = '" + uniqueTag + "'";

        ContentValues cv = new ContentValues();
        cv.put(TransferHelper.KEY_REMOTE_PATH, remotePath);

        result = update(cv, where, null);
        return result;
    }


    //更新传输任务信息
    public int updateTransferInfo(String uniqueTag, TransferItem newItem, boolean isFeedBack) {
        int result = -1;
        if (TextUtils.isEmpty(uniqueTag)) {
            return result;
        }

        String where = TransferHelper.KEY_EXT1 + " = '" + uniqueTag + "'";

        ContentValues cv = new ContentValues();
        cv.put(TransferHelper.KEY_STATE, newItem.state);
        cv.put(TransferHelper.KEY_ERROR_CODE, newItem.errorCode);
        cv.put(TransferHelper.KEY_SIZE_TOTAL, newItem.totalSize);
        cv.put(TransferHelper.KEY_MD5, newItem.md5);
        cv.put(TransferHelper.KEY_SIZE_CURRENT, newItem.currentSize);
        cv.put(TransferHelper.KEY_CREATE_TIME, newItem.createTime);
        cv.put(TransferHelper.KEY_UPDATE_TIME, System.currentTimeMillis());
        cv.put(TransferHelper.KEY_EXT2, newItem.ext2);
        cv.put(TransferHelper.KEY_EXT3, newItem.ext3);

        result = update(cv, where, null);

        if (isFeedBack) {
            EventBusUtil.post(new TransferStateEvent(newItem.keyName, newItem.transferType, newItem.state, uniqueTag));
        }

        return result;
    }


    /**
     * 批量更新状态
     *
     * @param transferType
     * @param oldState
     * @param newState
     * @return
     */
    public int changeAllState(int transferType, int oldState, int newState) {
        int result = -1;

        String where = TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType
                + AND_ + TransferHelper.KEY_STATE + " = " + oldState;

        ContentValues cv = new ContentValues();
        cv.put(TransferHelper.KEY_STATE, newState);

        result = update(cv, where, null);

//        if (isFeedBack) {
//            EventBusUtil.post(new TransferStateEvent(newItem.keyName, newItem.transferType, newItem.state, uniqueTag));
//        }

        return result;
    }

    /**
     * 更新指定文件名的uuid
     *
     * @param keyName
     * @param uuid
     * @return
     */
    public int updateUUID(String keyName, String uuid) {
        int result = -1;
        if (TextUtils.isEmpty(keyName) || TextUtils.isEmpty(uuid)) {
            return result;
        }

        String where = TransferHelper.KEY_KEY_NAME + " = '" + keyName + "'";

        ContentValues cv = new ContentValues(1);
        cv.put(TransferHelper.KEY_UUID, uuid);

        result = update(cv, where, null);

        return result;
    }

    /**
     * 更新传输类型、地址
     *
     * @param keyName
     * @param oldTransferType
     * @param newTransferType
     * @param localPath
     * @return
     */
    public int updateTransferType(String keyName, int oldTransferType, int newTransferType, String localPath, String newUniqueTag) {
        int result = -1;
        if (TextUtils.isEmpty(keyName)) {
            return result;
        }

        String where = TransferHelper.KEY_KEY_NAME + " = '" + keyName + "'"
                + AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + oldTransferType;

        ContentValues cv = new ContentValues(1);
        cv.put(TransferHelper.KEY_TRANSFER_TYPE, newTransferType);
        if (!TextUtils.isEmpty(localPath)) {
            cv.put(TransferHelper.KEY_LOCAL_PATH, localPath);
        }

        if (!TextUtils.isEmpty(newUniqueTag)) {
            cv.put(TransferHelper.KEY_EXT1, newUniqueTag);
        }

        long changeTime = System.currentTimeMillis();
        cv.put(TransferHelper.KEY_CREATE_TIME, changeTime);
        cv.put(TransferHelper.KEY_UPDATE_TIME, changeTime);

        result = update(cv, where, null);

        return result;
    }

    /* ===========以下是删除接口============ */

    /**
     * 基本删除操作
     *
     * @param selection
     * @param selectionArgs
     * @return
     */
    public int delete(String selection, String[] selectionArgs) {
        String where = TransferHelper.KEY_ACCOUNT + " = '" + EulixSpaceDBUtil.queryAvailableBoxUuid(EulixSpaceApplication.getContext()) + EulixSpaceDBUtil.getCurrentUserId(EulixSpaceApplication.getContext()) + "'";
        if (!TextUtils.isEmpty(selection)) {
            where = where + " and " + selection;
        }
        return mDBHelper.delete(where, selectionArgs);
    }

    /**
     * 根据id删除数据
     *
     * @param id
     * @param selection
     * @param selectionArgs
     * @return
     */
    public int deleteById(long id, String selection, String[] selectionArgs) {
        String where = "id=" + id;
        if (!TextUtils.isEmpty(selection)) {
            where = where + " and" + selection;
        }
        return mDBHelper.delete(where, selectionArgs);
    }

    /**
     * 根据文件名删除数据
     *
     * @param keyName
     * @return
     */
    public int deleteByKeyName(String keyName, int transferType) {
        if (TextUtils.isEmpty(keyName)) {
            return 0;
        }

        String where = TransferHelper.KEY_KEY_NAME + " = '" + keyName + "'"
                + AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;
        return delete(where, null);
    }

    /**
     * 根据文件名、本地路径、远端路径删除上传数据
     *
     * @param keyName
     * @return
     */
    public int deleteUploadItem(String keyName, String localPath, String remotePath) {
        String where = TransferHelper.KEY_KEY_NAME + " = '" + keyName + "'"
                + AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + TransferHelper.TYPE_UPLOAD
                + AND_ + TransferHelper.KEY_LOCAL_PATH + " = '" + localPath + "'"
                + AND_ + TransferHelper.KEY_REMOTE_PATH + " = '" + remotePath + "'";
        return delete(where, null);
    }

    /**
     * 根据uniqueTag删除数据
     *
     * @return
     */
    public int deleteByUniqueTag(String uniqueTag, int type) {
        String where = TransferHelper.KEY_EXT1 + " = '" + uniqueTag + "'"
                + AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + type;
        return delete(where, null);
    }

    /**
     * 根据uuid删除数据
     *
     * @param uuid
     * @return
     */
    public int deleteByUUID(String uuid, int transferType) {
        if (TextUtils.isEmpty(uuid)) {
            return 0;
        }

        String where = TransferHelper.KEY_UUID + " = '" + uuid + "'"
                + AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;
        return delete(where, null);
    }

    /**
     * 根据类型删除数据
     *
     * @param transferType
     * @return
     */
    public int deleteByType(int transferType) {

        String where = TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;
        return delete(where, null);
    }

    /**
     * 根据类型、状态删除数据
     *
     * @param transferType
     * @return
     */
    public int deleteByTypeAndState(int transferType, int state) {

        String where = TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType
                + AND_ + TransferHelper.KEY_STATE + " = " + state;
        return delete(where, null);
    }

    /**
     * 设置所有传输中的任务为失败。
     * （因暂不支持断点续传，应用进程失败后无法恢复传输）
     *
     * @return
     */
    public int setAllTransferringItemsFail(int transferType) {
        int result = -1;

        String where = TransferHelper.KEY_STATE + " = " + TransferHelper.STATE_DOING + ""
                + AND_ + TransferHelper.KEY_TRANSFER_TYPE + " = " + transferType;

        ContentValues cv = new ContentValues(2);
        cv.put(TransferHelper.KEY_STATE, TransferHelper.STATE_ERROR);
        cv.put(TransferHelper.KEY_ERROR_CODE, -1);

        result = update(cv, where, null);

        return result;
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

        result = mDBHelper.update(cv, where, null);
        return result;
    }

    public long deleteByAccount(String account) {
        String where = CacheHelper.KEY_ACCOUNT + " = '" + account + "'";
        return mDBHelper.delete(where, null);
    }
}
