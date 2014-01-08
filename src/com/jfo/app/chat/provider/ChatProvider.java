package com.jfo.app.chat.provider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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
import com.jfo.app.chat.provider.ChatDataStructs.ThreadsColumns;
import com.jfo.app.chat.provider.ChatDataStructs.ThreadsHelper;
import com.libs.utils.LogUtil;
import com.lidroid.xutils.util.LogUtils;

public class ChatProvider extends ContentProvider {
    private static final String TAG = ChatProvider.class.getSimpleName();

    private static final String DATABASE_NAME = "chat.db";
    private static final int DATABASE_VERSION = 1;
    private static final HashMap<String, String> sMsgProjectionMap;
    private static final HashMap<String, String> sThreadsProjectionMap;
    private static final int MESSAGE = 10;
    private static final int MESSAGE_ID = 11;
    private static final int THREADS = 20;
    private static final int THREADS_ID = 21;
    private static final int QUERY_THREAD_ID = 31;
    private static final UriMatcher URI_MATCHER;

//    private static final String UPDATE_THREAD_COUNT =
//            " UPDATE " + ThreadsColumns.TABLE_NAME + 
//            "  SET " + ThreadsColumns.MESSAGE_COUNT + " = " +
//            "   (SELECT COUNT(" + MessageColumns._ID + ") FROM " + MessageColumns.TABLE_NAME +
//            "    WHERE " + MessageColumns.THREAD_ID + " = " + "NEW." + MessageColumns.THREAD_ID + ")" +
//            " WHERE " + ThreadsColumns._ID + " = " + " NEW." + MessageColumns.THREAD_ID + "; ";
//    private static final String UPDATE_THREADS_ON_INSERT_MESSAGE =
//            "CREATE TRIGGER update_threads_on_insert_message " +
//            " AFTER INSERT ON " + MessageColumns.TABLE_NAME +
//            " BEGIN " +
//            "  UPDATE " + ThreadsColumns.TABLE_NAME + 
//            "   SET " + ThreadsColumns.SNIPPET + " = NEW." + MessageColumns.BODY +
//            "   WHERE " + ThreadsColumns._ID + " = NEW." + MessageColumns.THREAD_ID + ";" +
//            " END";
    private static final String UPDATE_THREADS_ON_INSERT_MESSAGE =
            "CREATE TRIGGER update_threads_on_insert_message " +
            " AFTER INSERT ON message " +
            " BEGIN " +
            "  UPDATE threads SET " +
            "   snippet=NEW.body," +
            "   date=NEW.date," +
            "   message_count=(SELECT COUNT(_id) FROM message WHERE thread_id=NEW.thread_id)," +
            "   unread_message_count=(SELECT COUNT(_id) FROM message WHERE thread_id=NEW.thread_id AND read=0)," +
            "   read= CASE (SELECT COUNT(_id) FROM message WHERE read=0 AND thread_id=NEW.thread_id) WHEN 0 THEN 1 ELSE 0 END" +
            "  WHERE _id=NEW.thread_id;" +
            " END";
    private static final String UPDATE_THREADS_ON_DELETE_MESSAGE =
            "CREATE TRIGGER update_threads_on_delete_message " +
            " AFTER DELETE ON message " +
            " BEGIN " +
            "  UPDATE threads SET " +
            "   snippet=(SELECT body FROM message WHERE thread_id=OLD.thread_id ORDER BY date DESC LIMIT 1)," +
            "   date=(SELECT date FROM message WHERE thread_id=OLD.thread_id ORDER BY date DESC LIMIT 1)," +
            "   message_count=(SELECT COUNT(_id) FROM message WHERE thread_id=OLD.thread_id)," +
            "   unread_message_count=(SELECT COUNT(_id) FROM message WHERE thread_id=OLD.thread_id AND read=0)," +
            "   read= CASE (SELECT COUNT(_id) FROM message WHERE read=0 AND thread_id=OLD.thread_id) WHEN 0 THEN 1 ELSE 0 END" +
            "  WHERE _id=OLD.thread_id;" +
            " END";
    private static final String UPDATE_THREADS_ON_UPDATE_MESSAGE =
            "CREATE TRIGGER update_threads_on_update_message " +
            " AFTER UPDATE OF read ON message " +
            " BEGIN " +
            "  UPDATE threads SET " +
            "   unread_message_count=(SELECT COUNT(_id) FROM message WHERE thread_id=NEW.thread_id AND read=0)," +
            "   read= CASE (SELECT COUNT(_id) FROM message WHERE read=0 AND thread_id=NEW.thread_id) WHEN 0 THEN 1 ELSE 0 END" +
            "  WHERE _id=NEW.thread_id;" +
            " END";

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
            db.execSQL(ThreadsColumns.SQL_CREATE);
            db.execSQL(UPDATE_THREADS_ON_INSERT_MESSAGE);
            db.execSQL(UPDATE_THREADS_ON_DELETE_MESSAGE);
            db.execSQL(UPDATE_THREADS_ON_UPDATE_MESSAGE);
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
        case MESSAGE_ID:
            qb.appendWhere(MessageColumns._ID + "=" + uri.getLastPathSegment());
        case MESSAGE:
            qb.setTables(MessageColumns.TABLE_NAME);
//            qb.setProjectionMap(sMsgProjectionMap);
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = MessageColumns.DEFAULT_SORT_ORDER;
            } else {
                orderBy = sortOrder;
            }
            break;
        case QUERY_THREAD_ID:
            String recipient = uri.getQueryParameter("recipient");
            return getThreadId(recipient);
        case THREADS_ID:
            qb.appendWhere(ThreadsColumns._ID + "=" + uri.getLastPathSegment());
        case THREADS:
            qb.setTables(ThreadsColumns.TABLE_NAME);
//            qb.setProjectionMap(sThreadsProjectionMap);
            if (TextUtils.isEmpty(sortOrder)) {
                orderBy = ThreadsColumns.DEFAULT_SORT_ORDER;
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
        case MESSAGE_ID:
            return MessageColumns.CONTENT_TYPE;
        case THREADS:
        case THREADS_ID:
            return ThreadsColumns.CONTENT_TYPE;
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

        String tableName = null, nullColumnHack = null;
        final int match = URI_MATCHER.match(uri);
        switch (match) {
        case MESSAGE:
            tableName = MessageColumns.TABLE_NAME;
            nullColumnHack = MessageColumns.ADDRESS;
            break;
        case THREADS:
            tableName = ThreadsColumns.TABLE_NAME;
            break;
        }

        if (tableName != null) {
            long rowId = db.insert(tableName, nullColumnHack, values);
            if (rowId > 0) {
                Uri result = ContentUris.withAppendedId(uri, rowId);
                getContext().getContentResolver().notifyChange(result, null);
                if (match == MESSAGE) {
//                    Long threadId = initialValues.getAsLong(MessageColumns.THREAD_ID);
//                    if (threadId != null)
//                        updateThread(db, threadId);
                    getContext().getContentResolver().notifyChange(ThreadsColumns.CONTENT_URI, null);
                }
                return result;
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
        String table = null;
        final int match = URI_MATCHER.match(uri);
        switch (match) {
        case MESSAGE_ID:
            where = MessageColumns._ID + "=" + uri.getLastPathSegment()
                + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
        case MESSAGE:
            table = MessageColumns.TABLE_NAME;
            break;
        case THREADS_ID:
            where = ThreadsColumns._ID + "=" + uri.getLastPathSegment()
                + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
        case THREADS:
            table = ThreadsColumns.TABLE_NAME;
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (table != null) {
            count = db.delete(table, where, whereArgs);
            if (count > 0) {
                getContext().getContentResolver().notifyChange(uri, null);
                if (match == MESSAGE_ID || match == MESSAGE) {
                    db.delete(ThreadsColumns.TABLE_NAME, 
                            ThreadsColumns._ID + " NOT IN (SELECT DISTINCT " + 
                            MessageColumns.THREAD_ID + " FROM " + MessageColumns.TABLE_NAME + ")", null);
                    getContext().getContentResolver().notifyChange(ThreadsColumns.CONTENT_URI, null);
                }
            }
        }
        
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
        final int match = URI_MATCHER.match(uri);
        switch (match) {
        case MESSAGE_ID:
            where = MessageColumns._ID + "=" + uri.getLastPathSegment()
                + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
        case MESSAGE:
            count = db.update(MessageColumns.TABLE_NAME, values, where, whereArgs);
            break;
        case THREADS_ID:
            where = ThreadsColumns._ID + "=" + uri.getLastPathSegment()
                + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
        case THREADS:
            count = db.update(ThreadsColumns.TABLE_NAME, values, where, whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            if (match == MESSAGE_ID || match == MESSAGE) {
                getContext().getContentResolver().notifyChange(ThreadsColumns.CONTENT_URI, null);
            }
        }
        return count;
    }

    private synchronized Cursor getThreadId(String recipient) {
        String[] selectionArgs = new String[] { recipient };
        final String THREAD_QUERY = "SELECT " + ThreadsColumns._ID + 
                " FROM " + ThreadsColumns.TABLE_NAME
                + " WHERE " + ThreadsColumns.RECIPIENTS + "=?";

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(THREAD_QUERY, selectionArgs);

        if (cursor.getCount() == 0) {
            cursor.close();

            LogUtils.d("getThreadId: create new thread_id for recipients " + recipient);
            insertThread(recipient);

            db = mOpenHelper.getReadableDatabase();  // In case insertThread closed it
            cursor = db.rawQuery(THREAD_QUERY, selectionArgs);
        }
        
        if (cursor.getCount() > 1) {
            LogUtils.w("getThreadId: why is cursorCount=" + cursor.getCount());
        }

        return cursor;
    }
    
    private void insertThread(String recipient) {
        ContentValues values = new ContentValues(4);

        long date = System.currentTimeMillis();
        values.put(ThreadsColumns.DATE, date - date % 1000);
        values.put(ThreadsColumns.RECIPIENTS, recipient);
        values.put(ThreadsColumns.MESSAGE_COUNT, 0);

        long result = mOpenHelper.getWritableDatabase().insert(
                ThreadsColumns.TABLE_NAME, null, values);
        LogUtils.d("insertThread: created new thread_id " + result +
                " for recipientIds " + recipient);

        getContext().getContentResolver().notifyChange(ThreadsColumns.CONTENT_URI, null);
    }

    private void updateThread(SQLiteDatabase db, long threadId) {
        int rows = db.delete(ThreadsColumns.TABLE_NAME,
                "_id = ? AND _id NOT IN " +
                "  (SELECT " + MessageColumns.THREAD_ID + " FROM " + MessageColumns.TABLE_NAME + ")",
                new String[]{ String.valueOf(threadId) });
        if (rows <= 0) {
        db.execSQL(
                " UPDATE threads SET message_count = " +
                "   (SELECT COUNT(_id) FROM message WHERE thread_id = " + threadId + ")" +
                " WHERE _id = " + threadId + ";"
             );
        db.execSQL(
                " UPDATE threads SET unread_message_count = " +
                "   (SELECT COUNT(_id) FROM message " + 
                        " WHERE thread_id = " + threadId + " AND read=0)" +
                " WHERE _id = " + threadId + ";"
             );
        db.execSQL(
                " UPDATE threads SET " +
                " date=" +
                "   (SELECT date FROM message WHERE thread_id=" + threadId + " ORDER BY date DESC LIMIT 1)," +
                " snippet=" +
                "   (SELECT body FROM message WHERE thread_id=" + threadId + " ORDER BY date DESC LIMIT 1)" +
                " WHERE _id=" + threadId + ";"
             );
        }
        getContext().getContentResolver().notifyChange(ThreadsColumns.CONTENT_URI, null);
    }
    
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(ChatDataStructs.AUTHORITY, MessageColumns.TABLE_NAME, MESSAGE);
        URI_MATCHER.addURI(ChatDataStructs.AUTHORITY, MessageColumns.TABLE_NAME + "/#", MESSAGE_ID);
        URI_MATCHER.addURI(ChatDataStructs.AUTHORITY, ThreadsColumns.TABLE_NAME, THREADS);
        URI_MATCHER.addURI(ChatDataStructs.AUTHORITY, ThreadsColumns.TABLE_NAME + "/#", THREADS_ID);
        URI_MATCHER.addURI(ChatDataStructs.AUTHORITY, ThreadsHelper.THREAD_ID_QUERY_NAME, QUERY_THREAD_ID);

        sMsgProjectionMap = new HashMap<String, String>();
        sMsgProjectionMap.put(MessageColumns._ID, MessageColumns._ID);
        sMsgProjectionMap.put(MessageColumns.ADDRESS, MessageColumns.ADDRESS);
        sMsgProjectionMap.put(MessageColumns.THREAD_ID, MessageColumns.THREAD_ID);
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

        sThreadsProjectionMap = new HashMap<String, String>();
        sThreadsProjectionMap.put(ThreadsColumns._ID, ThreadsColumns._ID);
        sThreadsProjectionMap.put(ThreadsColumns.RECIPIENTS, ThreadsColumns.RECIPIENTS);
        sThreadsProjectionMap.put(ThreadsColumns.DATE, ThreadsColumns.DATE);
        sThreadsProjectionMap.put(ThreadsColumns.MESSAGE_COUNT, ThreadsColumns.MESSAGE_COUNT);
        sThreadsProjectionMap.put(ThreadsColumns.UNREAD_MESSAGE_COUNT, ThreadsColumns.UNREAD_MESSAGE_COUNT);
        sThreadsProjectionMap.put(ThreadsColumns.READ, ThreadsColumns.READ);
        sThreadsProjectionMap.put(ThreadsColumns.SNIPPET, ThreadsColumns.SNIPPET);
        // TODO add other columns ...
    }
}
