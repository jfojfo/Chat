package com.jfo.app.chat.provider;

import java.util.HashMap;

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

import com.jfo.app.chat.provider.ChatDataStructs.MessageColumns;
import com.libs.utils.LogUtil;

public class ChatProvider extends ContentProvider {
    private static final String TAG = ChatProvider.class.getSimpleName();

    private static final String DATABASE_NAME = "chat.db";
    private static final int DATABASE_VERSION = 1;
    private static final HashMap<String, String> sMsgProjectionMap;
    private static final int MESSAGE = 10;
    private static final int MESSAGE_ID = 11;
    private static final UriMatcher URI_MATCHER;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if(LogUtil.DDBG){
                LogUtil.d(TAG, "DatabaseHelper create");
            }
            db.execSQL(MessageColumns.SQL_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if(LogUtil.DDBG){
                LogUtil.d(TAG, "Upgrading database from version " + oldVersion
                        + " to " + newVersion);
            }
        }

    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String limit = null;
        String orderBy = null;

        switch (URI_MATCHER.match(uri)) {
        case MESSAGE:
            qb.setTables(MessageColumns.TABLE_NAME);
            qb.setProjectionMap(sMsgProjectionMap);
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = MessageColumns.DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null,
                null, orderBy, limit);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
        case MESSAGE:
            return MessageColumns.CONTENT_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if(LogUtil.DDBG){
            LogUtil.d(TAG, "insert:" + uri.toString());
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (URI_MATCHER.match(uri)) {
        case MESSAGE: {
            long rowId = db.insert(MessageColumns.TABLE_NAME, MessageColumns.ADDRESS,
                    values);
            if (rowId > 0) {
                Uri result = ContentUris.withAppendedId(MessageColumns.CONTENT_URI,
                        rowId);
                getContext().getContentResolver().notifyChange(result, null);
                return result;
            }
        }
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if(LogUtil.DDBG){
            LogUtil.d(TAG, "bulkInsert:" + uri.toString());
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String table = null;
        String nullColumnHack = null;
        int match = URI_MATCHER.match(uri);
        if (match == MESSAGE) {
            table = MessageColumns.TABLE_NAME;
            nullColumnHack = MessageColumns.ADDRESS;
        }
        if (table == null)
            return 0;

        db.beginTransaction();
        try {
            for (ContentValues v : values) {
                long row = db.insert(table, nullColumnHack, v);
                if (row < 0)
                    return 0;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return values.length;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        if(LogUtil.DDBG){
            LogUtil.d(TAG, "delete:" + uri.toString() + "," + where);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (URI_MATCHER.match(uri)) {
        case MESSAGE:
            count = db.delete(MessageColumns.TABLE_NAME, where, whereArgs);
            break;
        case MESSAGE_ID:
            where = MessageColumns._ID + "=" + uri.getLastPathSegment()
                + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
            count = db.delete(MessageColumns.TABLE_NAME, where, whereArgs);
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        if(LogUtil.DDBG){
            LogUtil.d(TAG, "update:" + uri.toString() + "," + where);
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int count = 0;
        switch (URI_MATCHER.match(uri)) {
        case MESSAGE:
            count = db.update(MessageColumns.TABLE_NAME, values, where, whereArgs);
            break;
        case MESSAGE_ID:
            where = MessageColumns._ID + "=" + uri.getLastPathSegment()
                + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
            count = db.update(MessageColumns.TABLE_NAME, values, where, whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(ChatDataStructs.AUTHORITY, MessageColumns.TABLE_NAME, MESSAGE);
        URI_MATCHER.addURI(ChatDataStructs.AUTHORITY, MessageColumns.TABLE_NAME + "/#", MESSAGE_ID);

        sMsgProjectionMap = new HashMap<String, String>();
        sMsgProjectionMap.put(MessageColumns._ID, MessageColumns._ID);
        sMsgProjectionMap.put(MessageColumns.ADDRESS, MessageColumns.ADDRESS);
        sMsgProjectionMap.put(MessageColumns.THREAD_ID, MessageColumns.THREAD_ID);
        sMsgProjectionMap.put(MessageColumns.SMS_ID, MessageColumns.SMS_ID);
        sMsgProjectionMap.put(MessageColumns.BODY, MessageColumns.BODY);
        sMsgProjectionMap.put(MessageColumns.DATE, MessageColumns.DATE);
        sMsgProjectionMap.put(MessageColumns.EXPAND_DATA1, MessageColumns.EXPAND_DATA1);
        sMsgProjectionMap.put(MessageColumns.EXPAND_DATA2, MessageColumns.EXPAND_DATA2);
        sMsgProjectionMap.put(MessageColumns.EXPAND_DATA3, MessageColumns.EXPAND_DATA3);
        sMsgProjectionMap.put(MessageColumns.EXPAND_DATA4, MessageColumns.EXPAND_DATA4);
        sMsgProjectionMap.put(MessageColumns.EXPAND_DATA5, MessageColumns.EXPAND_DATA5);
        sMsgProjectionMap.put(MessageColumns.PROTOCOL, MessageColumns.PROTOCOL);
        sMsgProjectionMap.put(MessageColumns.READ, MessageColumns.READ);
        sMsgProjectionMap.put(MessageColumns.STATUS, MessageColumns.STATUS);
        sMsgProjectionMap.put(MessageColumns.SUBJECT, MessageColumns.SUBJECT);
        sMsgProjectionMap.put(MessageColumns.TYPE, MessageColumns.TYPE);

    }
}
