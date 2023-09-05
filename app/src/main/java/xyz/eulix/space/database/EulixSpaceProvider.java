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

package xyz.eulix.space.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.database.DatabaseUtilsCompat;

import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 16:22
 */
public class EulixSpaceProvider extends ContentProvider {
    private final static String THIS_FILE = "EulixSpaceProvider";
    private EulixSpaceDBAdapter.DatabaseHelper mOpenHelper;
    private static final String UNKNOWN_URI_LOG = "Unknown URI ";
    private static final int BOX = 1, BOX_ID = 2, PUSH = 3, PUSH_ID = 4, DID = 5, DID_ID = 6;

    private static final UriMatcher URI_MATCHER;
    static {
        // Create and initialize URI matcher.
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(EulixSpaceDBManager.AUTHORITY, EulixSpaceDBManager.BOX_TABLE_NAME, BOX);
        URI_MATCHER.addURI(EulixSpaceDBManager.AUTHORITY, EulixSpaceDBManager.PUSH_TABLE_NAME, PUSH);
        URI_MATCHER.addURI(EulixSpaceDBManager.AUTHORITY, EulixSpaceDBManager.DID_TABLE_NAME, DID);
    }

    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        Logger.v(THIS_FILE, "EulixSpaceProvider onCreate");
        mOpenHelper = new EulixSpaceDBAdapter.DatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // TODO Auto-generated method stub
        Logger.v(THIS_FILE, "EulixSpaceProvider query uri = " + uri.toString());
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String finalSortOrder = sortOrder;
        String[] finalSelectionArgs = selectionArgs;
        String finalGrouping = null;
        String finalHaving = null;
        switch (URI_MATCHER.match(uri)) {
            case BOX:
                qb.setTables(EulixSpaceDBManager.BOX_TABLE_NAME);
                if (sortOrder == null) {
                    finalSortOrder = "_id" + " ASC";
                }
                break;
            case BOX_ID:
                qb.setTables(EulixSpaceDBManager.BOX_TABLE_NAME);
                qb.appendWhere("_id" + "=?");
                finalSelectionArgs = DatabaseUtilsCompat.appendSelectionArgs(selectionArgs, new String[] { uri.getLastPathSegment() });
                break;
            case PUSH:
                qb.setTables(EulixSpaceDBManager.PUSH_TABLE_NAME);
                if (sortOrder == null) {
                    finalSortOrder = "_id" + " ASC";
                }
                break;
            case PUSH_ID:
                qb.setTables(EulixSpaceDBManager.PUSH_TABLE_NAME);
                qb.appendWhere("_id" + "=?");
                finalSelectionArgs = DatabaseUtilsCompat.appendSelectionArgs(selectionArgs, new String[] { uri.getLastPathSegment() });
                break;
            case DID:
                qb.setTables(EulixSpaceDBManager.DID_TABLE_NAME);
                if (sortOrder == null) {
                    finalSortOrder = "_id" + " ASC";
                }
                break;
            case DID_ID:
                qb.setTables(EulixSpaceDBManager.DID_TABLE_NAME);
                qb.appendWhere("_id" + "=?");
                finalSelectionArgs = DatabaseUtilsCompat.appendSelectionArgs(selectionArgs, new String[] { uri.getLastPathSegment() });
                break;
            default:
                throw new RuntimeException("unknown uri " + uri.toString());
        }
        Cursor cursor = null;
        try {
            cursor = qb.query(db, projection, selection, finalSelectionArgs, finalGrouping, finalHaving,
                    finalSortOrder);
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // TODO Auto-generated method stub
        switch (URI_MATCHER.match(uri)) {
            case BOX:
                return EulixSpaceDBManager.BOX_CONTENT_TYPE;
            case BOX_ID:
                return EulixSpaceDBManager.BOX_CONTENT_ITEM_TYPE;
            case PUSH:
                return EulixSpaceDBManager.PUSH_CONTENT_TYPE;
            case PUSH_ID:
                return EulixSpaceDBManager.PUSH_CONTENT_ITEM_TYPE;
            case DID:
                return EulixSpaceDBManager.DID_CONTENT_TYPE;
            case DID_ID:
                return EulixSpaceDBManager.DID_CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        // TODO Auto-generated method stub
        Logger.v(THIS_FILE, "EulixSpaceProvider insert uri = " + uri.toString());
        String matchedTable = null;
        Uri baseInsertedUri = null;
        switch (URI_MATCHER.match(uri)) {
            case BOX:
            case BOX_ID:
                matchedTable = EulixSpaceDBManager.BOX_TABLE_NAME;
                baseInsertedUri = EulixSpaceDBManager.BOX_ID_URI_BASE;
                break;
            case PUSH:
            case PUSH_ID:
                matchedTable = EulixSpaceDBManager.PUSH_TABLE_NAME;
                baseInsertedUri = EulixSpaceDBManager.PUSH_ID_URI_BASE;
                break;
            case DID:
            case DID_ID:
                matchedTable = EulixSpaceDBManager.DID_TABLE_NAME;
                baseInsertedUri = EulixSpaceDBManager.DID_ID_URI_BASE;
                break;
            default:
                break;
        }

        if (matchedTable == null ) {
            throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
        }

        ContentValues mValues;

        if (values != null) {
            mValues = new ContentValues(values);
        } else {
            mValues = new ContentValues();
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(matchedTable, null, mValues);
        // If the insert succeeded, the row ID exists.
        if (rowId >= 0) {
            // TODO : for inserted account register it here
            Uri retUri = ContentUris.withAppendedId(baseInsertedUri, rowId);
            try {
                getContext().getContentResolver().notifyChange(retUri, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return retUri;
        } else {
            throw new SQLException("Failed to insert row into " + uri);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        Logger.v(THIS_FILE, "EulixSpaceProvider delete uri = " + uri.toString());
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;
        int count = 0;
        Uri retUri = uri;
        switch(URI_MATCHER.match(uri)) {
            case BOX:
                count = db.delete(EulixSpaceDBManager.BOX_TABLE_NAME, selection, selectionArgs);
                break;
            case BOX_ID:
                finalWhere = DatabaseUtilsCompat.concatenateWhere("_id" + " = " + ContentUris.parseId(uri), selection);
                count = db.delete(EulixSpaceDBManager.BOX_TABLE_NAME, finalWhere, selectionArgs);
                break;
            case PUSH:
                count = db.delete(EulixSpaceDBManager.PUSH_TABLE_NAME, selection, selectionArgs);
                break;
            case PUSH_ID:
                finalWhere = DatabaseUtilsCompat.concatenateWhere("_id" + " = " + ContentUris.parseId(uri), selection);
                count = db.delete(EulixSpaceDBManager.PUSH_TABLE_NAME, finalWhere, selectionArgs);
                break;
            case DID:
                count = db.delete(EulixSpaceDBManager.DID_TABLE_NAME, selection, selectionArgs);
                break;
            case DID_ID:
                finalWhere = DatabaseUtilsCompat.concatenateWhere("_id" + " = " + ContentUris.parseId(uri), selection);
                count = db.delete(EulixSpaceDBManager.DID_TABLE_NAME, finalWhere, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
        }
        try {
            getContext().getContentResolver().notifyChange(retUri, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        Logger.v(THIS_FILE, "EulixSpaceProvider update uri = " + uri.toString());
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;
        switch (URI_MATCHER.match(uri)) {
            case BOX:
                count = db.update(EulixSpaceDBManager.BOX_TABLE_NAME, values, selection, selectionArgs);
                break;
            case BOX_ID:
                finalWhere = DatabaseUtilsCompat.concatenateWhere("_id" + " = " + ContentUris.parseId(uri), selection);
                count = db.update(EulixSpaceDBManager.BOX_TABLE_NAME, values, finalWhere, selectionArgs);
                break;
            case PUSH:
                count = db.update(EulixSpaceDBManager.PUSH_TABLE_NAME, values, selection, selectionArgs);
                break;
            case PUSH_ID:
                finalWhere = DatabaseUtilsCompat.concatenateWhere("_id" + " = " + ContentUris.parseId(uri), selection);
                count = db.update(EulixSpaceDBManager.PUSH_TABLE_NAME, values, finalWhere, selectionArgs);
                break;
            case DID:
                count = db.update(EulixSpaceDBManager.DID_TABLE_NAME, values, selection, selectionArgs);
                break;
            case DID_ID:
                finalWhere = DatabaseUtilsCompat.concatenateWhere("_id" + " = " + ContentUris.parseId(uri), selection);
                count = db.update(EulixSpaceDBManager.DID_TABLE_NAME, values, finalWhere, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(UNKNOWN_URI_LOG + uri);
        }
        try {
            getContext().getContentResolver().notifyChange(uri, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }
}
