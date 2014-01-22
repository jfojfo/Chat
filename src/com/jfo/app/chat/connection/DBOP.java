package com.jfo.app.chat.connection;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.jfo.app.chat.db.DBAttachment;
import com.jfo.app.chat.provider.ChatDataStructs.MessageColumns;
import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.exception.DbException;

public class DBOP {

    public static Uri inserOrUpdateMsg(Context context, ChatMsg chatMsg) {
        ContentValues values = new ContentValues();
        values.put(MessageColumns.ADDRESS, chatMsg.getAddress());
        values.put(MessageColumns.BODY, chatMsg.getBody());
        values.put(MessageColumns.DATE, chatMsg.getDate());
        values.put(MessageColumns.READ, chatMsg.getRead());
        values.put(MessageColumns.TYPE, chatMsg.getType());
        values.put(MessageColumns.STATUS, chatMsg.getStatus());
        values.put(MessageColumns.THREAD_ID, chatMsg.getThread_id());
        values.put(MessageColumns.MEDIA_TYPE, chatMsg.getMedia_type());
        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        if (chatMsg.getId() == 0) {
            uri = resolver.insert(MessageColumns.CONTENT_URI, values);
        } else {
            uri = Uri.withAppendedPath(MessageColumns.CONTENT_URI, String.valueOf(chatMsg.getId()));
            resolver.update(uri, values, null, null);
        }
        return uri;
    }

    public static void markStatus(Context context, ChatMsg chatMsg) {
        ContentValues values = new ContentValues();
        values.put(MessageColumns.STATUS, chatMsg.getStatus());
        ContentResolver resolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(MessageColumns.CONTENT_URI, String.valueOf(chatMsg.getId()));
        resolver.update(uri, values, null, null);
    }

    public static void insertOrUpdateAttachment(DBAttachment dbatt) {
        DbUtils db = ConnectionManager.getInstance().getDB();
        try {
            if (dbatt.getId() == 0) {
                db.saveBindingId(dbatt);
            } else {
                db.update(dbatt);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

}
