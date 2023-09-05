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

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 16:17
 */
public class EulixSpaceDBAdapter {
    private final static String THIS_FILE = "EulixSpaceDBAdapter";
    private final Context context;
    private DatabaseHelper databaseHelper;

    public EulixSpaceDBAdapter(Context aContext) {
        Logger.v(THIS_FILE, "construct AISettingsDBAdapter");
        context = aContext;
        databaseHelper = new DatabaseHelper(context);
    }

    public static class DatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 5;
        // Creation sql command
        private static final String TABLE_BOX_CREATE = "CREATE TABLE IF NOT EXISTS "
                + EulixSpaceDBManager.BOX_TABLE_NAME
                + " ("
                + EulixSpaceDBManager.FIELD_BOX_ID              + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + EulixSpaceDBManager.FIELD_BOX_UUID            + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_NAME            + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_INFO            + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY      + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION   + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_REGISTER        + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_DOMAIN          + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_BIND            + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_STATUS          + " TEXT,"
                + EulixSpaceDBManager.FILED_BOX_UPDATE_TIME     + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_TOKEN           + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_USER_INFO       + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_FILE_LIST       + " TEXT,"
                + EulixSpaceDBManager.FIELD_BOX_OTHER_INFO      + " TEXT"
                + ");";

        private static final String TABLE_PUSH_CREATE = "CREATE TABLE IF NOT EXISTS "
                + EulixSpaceDBManager.PUSH_TABLE_NAME
                + " ("
                + EulixSpaceDBManager.FIELD_PUSH_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID     + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_UUID           + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_BIND           + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_TYPE           + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_PRIORITY       + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_SOURCE         + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_CONSUME        + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_TITLE          + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_CONTENT        + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_RAW_DATA       + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME    + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP      + " TEXT,"
                + EulixSpaceDBManager.FIELD_PUSH_RESERVE        + " TEXT"
                + ");";

        private static final String TABLE_DID_CREATE = "CREATE TABLE IF NOT EXISTS "
                + EulixSpaceDBManager.DID_TABLE_NAME
                + " ("
                + EulixSpaceDBManager.FIELD_DID_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + EulixSpaceDBManager.FIELD_DID_UUID           + " TEXT,"
                + EulixSpaceDBManager.FIELD_DID_BIND           + " TEXT,"
                + EulixSpaceDBManager.FIELD_DID_AO_ID          + " TEXT,"
                + EulixSpaceDBManager.FIELD_DID_DOC_ENCODE     + " TEXT,"
                + EulixSpaceDBManager.FIELD_DID_DOCUMENT       + " TEXT,"
                + EulixSpaceDBManager.FIELD_DID_CREDENTIAL     + " TEXT,"
                + EulixSpaceDBManager.FIELD_DID_TIMESTAMP      + " TEXT,"
                + EulixSpaceDBManager.FIELD_DID_RESERVE        + " TEXT"
                + ");";

        private static final String TABLE_BOX_DROP = "DROP TABLE IF EXISTS " + EulixSpaceDBManager.BOX_TABLE_NAME;

        DatabaseHelper(Context context) {
            super(context, EulixSpaceDBManager.AUTHORITY, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // TODO Auto-generated method stub
            Logger.v(THIS_FILE, "DatabaseHelper onCreate");
            db.execSQL(TABLE_BOX_CREATE);
            db.execSQL(TABLE_PUSH_CREATE);
            db.execSQL(TABLE_DID_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO Auto-generated method stub
            Logger.v(THIS_FILE,
                    "DatabaseHelper onUpgrade database from version "
                            + oldVersion + " to " + newVersion);
            if (oldVersion <= 2) {
                db.execSQL(TABLE_BOX_DROP);
            }
            onCreate(db);
        }
    }

    private boolean opened = false;
    public EulixSpaceDBAdapter open() throws SQLException {
        databaseHelper.getWritableDatabase();
        opened = true;
        return this;
    }

    public void close() {
        databaseHelper.close();
        opened = false;
    }

    public boolean isOpen() {
        return opened;
    }
}
