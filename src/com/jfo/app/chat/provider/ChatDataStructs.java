package com.jfo.app.chat.provider;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.lidroid.xutils.util.LogUtils;


public final class ChatDataStructs {

    public static final String AUTHORITY = "com.jfo.app.chat.provider.ChatDataStructs";

    public interface MessageColumns extends BaseColumns {
        public static final String TABLE_NAME = "message";

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + TABLE_NAME);
        
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.jfo.app.chat.message";

        /**
         * <P>Type: STRING</P>
         */
        public static final String ADDRESS = "address";

        /**
         * <P>Type: LONG</P>
         */
        public static final String THREAD_ID = "thread_id";

        /**
         * <P>Type: STRING</P>
         */
        public static final String BODY = "body";

        /**
         * <P>Type: LONG</P>
         */
        public static final String DATE = "date";

        /**
         * <P>Type: STRING</P>
         */
        public static final String SUBJECT = "subject";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String READ = "read";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String TYPE = "type";

        public static final int TYPE_OUTBOX = 1;
        public static final int TYPE_INBOX = 2;

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String STATUS = "status";

        public static final int STATUS_IDLE = 0;
        public static final int STATUS_FAIL = 1;
        public static final int STATUS_SENDING = 2;
        public static final int STATUS_PENDING_TO_DOWNLOAD = 3;
        public static final int STATUS_FAIL_UPLOADING = 4;

        /**
         * <P>Type: STRING</P>
         */
        public static final String PROTOCOL = "protocol";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String MEDIA_TYPE = "media_type";
        public static final int MEDIA_NORMAL = 0;
        public static final int MEDIA_FILE = 1;

        /**
         * 扩展字段
         */
        public static final String EXPAND_DATA1 = "e_d1";
        public static final String EXPAND_DATA2 = "e_d2";
        public static final String EXPAND_DATA3 = "e_d3";
        public static final String EXPAND_DATA4 = "e_d4";
        public static final String EXPAND_DATA5 = "e_d5";
        
        public static final String SQL_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ADDRESS + " TEXT, "
                + THREAD_ID + " INTEGER, "
                + SUBJECT + " TEXT, "
                + BODY + " TEXT, "
                + DATE + " INTEGER, "
                + READ + " INTEGER, "
                + TYPE + " INTEGER, "
                + STATUS + " INTEGER, "
                + PROTOCOL + " TEXT, "
                + MEDIA_TYPE + " INTEGER DEFAULT 0, "
                + EXPAND_DATA1 + " TEXT, "
                + EXPAND_DATA2 + " TEXT, "
                + EXPAND_DATA3 + " TEXT, "
                + EXPAND_DATA4 + " TEXT, "
                + EXPAND_DATA5 + " TEXT "
                + ");";

        public static final String DEFAULT_SORT_ORDER = "date desc";
    }

    public interface ThreadsColumns extends BaseColumns {
        public static final String TABLE_NAME = "threads";

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.jfo.app.chat.thread";

        /**
         * <P>Type: STRING</P>
         */
        public static final String RECIPIENTS = "recipients";

        /**
         * The message count of the thread.
         * <P>Type: INTEGER</P>
         */
        public static final String MESSAGE_COUNT = "message_count";

        /**
         * The unread message count of the thread.
         * <P>Type: INTEGER</P>
         */
        public static final String UNREAD_MESSAGE_COUNT = "unread_message_count";
        
        /**
         * The date at which the thread was created.
         *
         * <P>Type: INTEGER (long)</P>
         */
        public static final String DATE = "date";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String TYPE = "type";

        /**
         * Indicates whether all messages of the thread have been read.
         * <P>Type: INTEGER</P>
         */
        public static final String READ = "read";
        
        /**
         * The snippet of the latest message in the thread.
         * <P>Type: TEXT</P>
         */
        public static final String SNIPPET = "snippet";

        public static final String SQL_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + RECIPIENTS + " TEXT, "
                + MESSAGE_COUNT + " INTEGER DEFAULT 0, "
                + UNREAD_MESSAGE_COUNT + " INTEGER DEFAULT 0, "
                + DATE + " INTEGER DEFAULT 0, "
                + TYPE + " INTEGER DEFAULT 0, "
                + READ + " INTEGER DEFAULT 1, "
                + SNIPPET + " TEXT "
                + ");";

        public static final String DEFAULT_SORT_ORDER = "date desc";
    }
    
    public static final class ThreadsHelper implements ThreadsColumns {
        private static final String[] ID_PROJECTION = { BaseColumns._ID };
        public static final String THREAD_ID_QUERY_NAME = "threadID";
        private static final Uri THREAD_ID_CONTENT_URI = Uri.parse(
                "content://" + AUTHORITY + "/" + THREAD_ID_QUERY_NAME);
        
        public static int getOrCreateThreadId(Context context, String recipient) {
            Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();
            uriBuilder.appendQueryParameter("recipient", recipient);
            Uri uri = uriBuilder.build();
            Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                    uri, ID_PROJECTION, null, null, null);
            LogUtils.d("getOrCreateThreadId cursor cnt: " + cursor.getCount());
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getInt(0);
                    } else {
                        LogUtils.e("getOrCreateThreadId returned no rows!");
                    }
                } finally {
                    cursor.close();
                }
            }

            LogUtils.e("getOrCreateThreadId failed with uri " + uri.toString());
            throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
        }
    }

    public interface AttachmentsColumns extends BaseColumns {
        public static final String TABLE_NAME = "attachments";

        public static final Uri CONTENT_URI = Uri
                .parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.jfo.app.chat.attachment";

        /**
         * <P>Type: STRING</P>
         */
        public static final String NAME = "name";

        /**
         * <P>Type: STRING</P>
         */
        public static final String DESC = "desc";

        /**
         * <P>Type: STRING</P>
         */
        public static final String URL = "url";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String SIZE = "size";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String CREATE_TIME = "create_time";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String MODIFY_TIME = "modify_time";

        /**
         * <P>Type: STRING</P>
         */
        public static final String MD5 = "md5";
        
        /**
         * <P>Type: INTEGER</P>
         */
        public static final String TYPE = "type";

        /**
         * <P>Type: INTEGER</P>
         */
        public static final String MESSAGE_ID = "message_id";

        /**
         * <P>Type: STRING</P>
         */
        public static final String LOCAL_PATH = "local_path";

        
        public static final String SQL_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + NAME + " TEXT, "
                + DESC + " TEXT, "
                + URL + " TEXT, "
                + MD5 + " TEXT, "
                + SIZE + " INTEGER, "
                + CREATE_TIME + " INTEGER, "
                + MODIFY_TIME + " INTEGER, "
                + TYPE + " INTEGER, "
                + MESSAGE_ID + " INTEGER, "
                + LOCAL_PATH + " TEXT "
                + ");";
    }

}
