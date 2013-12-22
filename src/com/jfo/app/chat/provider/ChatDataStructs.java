package com.jfo.app.chat.provider;

import android.net.Uri;
import android.provider.BaseColumns;


public final class ChatDataStructs {

    public static final String AUTHORITY = "com.jfo.app.chat.provider.ChatDataStructs";

    public static final class MessageColumns implements BaseColumns {
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
         * <P>Type: LONG</P>
         */

        public static final String SMS_ID = "sms_id";
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
        
        public static final int STATUS_IDEL = 0;
        public static final int STATUS_SENDING = 1;
        public static final int STATUS_FAIL = 2;
        
        /**
         * <P>Type: STRING</P>
         */
        public static final String PROTOCOL = "protocol";

        /**
         * 扩展字段
         */
        public static final String EXPAND_DATA1 = "e_d1";
        public static final String EXPAND_DATA2 = "e_d2";
        public static final String EXPAND_DATA3 = "e_d3";
        public static final String EXPAND_DATA4 = "e_d4";
        public static final String EXPAND_DATA5 = "e_d5";
        
        public static final String SQL_CREATE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                + " (" + _ID + " INTEGER PRIMARY KEY, "
                + ADDRESS + " TEXT, "
                + THREAD_ID + " INTEGER, "
                + SMS_ID + " INTEGER, "
                + SUBJECT + " TEXT, "
                + BODY + " TEXT, "
                + DATE + " INTEGER, "
                + READ + " INTEGER, "
                + TYPE + " INTEGER, "
                + STATUS + " INTEGER, "
                + PROTOCOL + " TEXT, "
                + EXPAND_DATA1 + " TEXT, "
                + EXPAND_DATA2 + " TEXT, "
                + EXPAND_DATA3 + " TEXT, "
                + EXPAND_DATA4 + " TEXT, "
                + EXPAND_DATA5 + " TEXT "
                + ");";

        public static final String DEFAULT_SORT_ORDER = "date desc";
    }

}
