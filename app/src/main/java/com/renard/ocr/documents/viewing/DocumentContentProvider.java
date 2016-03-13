/*
 * Copyright (C) 2012,2013 Renard Wellnitz.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.renard.ocr.documents.viewing;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class DocumentContentProvider extends ContentProvider {

    private final static String TAG = DocumentContentProvider.class.getSimpleName();
    private static final String AUTHORITY = "com.renard.ocr";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/documents");

    public static class Columns {

        public static final String ID = "_id";
        public static final String PARENT_ID = "parent_id";
        public static final String CREATED = "created";
        public static final String PHOTO_PATH = "photo_path";
        public static final String TITLE = "title";
        public static final String OCR_TEXT = "ocr_text";
        public static final String HOCR_TEXT = "hocr_text";
        public static final String PDF_URI = "pdf_uri";
        public static final String CHILD_COUNT = "child_count";
        public static final String OCR_LANG = "ocr_lang";
    }

    private static final UriMatcher sUriMatcher;
    private static final int DOCUMENT = 0;
    private static final int DOCUMENTS = 1;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "documents/#", DOCUMENT);
        sUriMatcher.addURI(AUTHORITY, "documents", DOCUMENTS);
    }

    private static class DBHelper extends SQLiteOpenHelper {

        private static final String TABLE_NAME = "documents";
        private static final int DATABASE_VERSION = 12;

        private static final String TABLE_CREATE = "CREATE TABLE " + TABLE_NAME + " (" + Columns.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Columns.PARENT_ID
                + " INTEGER DEFAULT -1, " + Columns.CREATED + " INTEGER, " + Columns.TITLE + " TEXT, " + Columns.PHOTO_PATH + " TEXT, " + Columns.PDF_URI + " TEXT, "
                + Columns.HOCR_TEXT + " TEXT, " + Columns.OCR_LANG + " TEXT, " + Columns.CHILD_COUNT + " INTEGER DEFAULT 0, " + Columns.OCR_TEXT + " TEXT);";

        private static final String ADD_OCR_LANG_COLUMN ="ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + Columns.OCR_LANG + " TEXT;";

        private static final String DATABASE_NAME = "Transactions";

        DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");

            if (oldVersion<=11){
                db.execSQL(ADD_OCR_LANG_COLUMN);
            }
        }

    }

    private DBHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new DBHelper(getContext());
        return true;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DBHelper.TABLE_NAME);
        String limit = null;

        switch (sUriMatcher.match(uri)) {
            case DOCUMENT:
                String id = uri.getLastPathSegment();
                selection = Columns.ID + "=?";
                selectionArgs = new String[]{id};
                break;
            case DOCUMENTS:
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = "created DESC";
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, Columns.ID, null, orderBy, limit);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case DOCUMENT:
                return "vnd.android.cursor.dir/vnd.ocr.document";
            case DOCUMENTS:
                return "vnd.android.cursor.item/vnd.ocr.documents";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        if (values != null) {
            values = new ContentValues(values);
        } else {
            values = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis());
        values.put(Columns.CREATED, now);
        // if (!values.containsKey(Columns.TITLE) &&
        // values.containsKey(Columns.OCR_TEXT)) {
        // values.put(Columns.TITLE,
        // Html.fromHtml(values.getAsString(Columns.OCR_TEXT)).toString());
        // }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            if (values.containsKey(Columns.PARENT_ID)) {
                // update child count of parent
                final String query = "UPDATE " + DBHelper.TABLE_NAME + " set " + Columns.CHILD_COUNT + "=" + Columns.CHILD_COUNT + "+1 WHERE _id="
                        + values.getAsString(Columns.PARENT_ID);
                Log.i(TAG, query);
                db.execSQL(query);
            }
            long rowId = db.insert(DBHelper.TABLE_NAME, null, values);
            if (rowId >= 0) {
                Uri entryUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(CONTENT_URI, null);
                db.setTransactionSuccessful();
                return entryUri;
            }
        } finally {
            db.endTransaction();
        }

        throw new SQLException("Failed to insert values into" + uri);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case DOCUMENTS:
                count = db.delete(DBHelper.TABLE_NAME, selection, selectionArgs);
                break;
            case DOCUMENT:
                String id = uri.getLastPathSegment();
                count = db.delete(DBHelper.TABLE_NAME, Columns.ID + "=?", new String[]{id});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case DOCUMENTS:
                count = db.update(DBHelper.TABLE_NAME, values, selection, selectionArgs);
                break;

            case DOCUMENT: {
                String id = uri.getLastPathSegment();
                count = db.update(DBHelper.TABLE_NAME, values, Columns.ID + "=?", new String[]{id});
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

}
