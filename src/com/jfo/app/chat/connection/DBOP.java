package com.jfo.app.chat.connection;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.jfo.app.chat.proto.BDUploadFileResult;
import com.jfo.app.chat.provider.ChatDataStructs.AttachmentsColumns;
import com.jfo.app.chat.provider.ChatDataStructs.MessageColumns;

public class DBOP {

    public static Uri inserOrUpdateMsg(Context context, ChatMsg chatMsg) {
        ContentValues values = new ContentValues();
        values.put(MessageColumns.ADDRESS, chatMsg.getAddress());
        values.put(MessageColumns.BODY, chatMsg.getBody());
        values.put(MessageColumns.DATE, chatMsg.getDate());
        values.put(MessageColumns.READ, chatMsg.getRead());
        values.put(MessageColumns.TYPE, chatMsg.getType());
        values.put(MessageColumns.STATUS, chatMsg.getStatus());
        values.put(MessageColumns.THREAD_ID, chatMsg.getThreadID());
        values.put(MessageColumns.MEDIA_TYPE, chatMsg.getMediaType());
        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        if (chatMsg.getMsgID() == 0) {
            uri = resolver.insert(MessageColumns.CONTENT_URI, values);
        } else {
            uri = Uri.withAppendedPath(MessageColumns.CONTENT_URI, String.valueOf(chatMsg.getMsgID()));
            resolver.update(uri, values, null, null);
        }
        return uri;
    }

    public static void markStatus(Context context, ChatMsg chatMsg) {
        ContentValues values = new ContentValues();
        values.put(MessageColumns.STATUS, chatMsg.getStatus());
        ContentResolver resolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(MessageColumns.CONTENT_URI, String.valueOf(chatMsg.getMsgID()));
        resolver.update(uri, values, null, null);
    }

    public static Uri insertOrUpdateAttachment(Context context, FileMsg fileMsg) {
        ContentValues values = new ContentValues();
        values.put(AttachmentsColumns.NAME, FilenameUtils.getName(fileMsg.getFile()));
        values.put(AttachmentsColumns.MESSAGE_ID, fileMsg.getMsgID());
        if (!TextUtils.isEmpty(fileMsg.getFile())) {
            values.put(AttachmentsColumns.LOCAL_PATH, fileMsg.getFile());
            File f = new File(fileMsg.getFile());
            if (f.exists())
                values.put(AttachmentsColumns.SIZE, f.length());
        }
        BDUploadFileResult info = fileMsg.getInfo();
        if (info != null) {
            values.put(AttachmentsColumns.CREATE_TIME, info.ctime);
            values.put(AttachmentsColumns.MODIFY_TIME, info.mtime);
            values.put(AttachmentsColumns.MD5, info.md5);
            values.put(AttachmentsColumns.SIZE, info.size);
            values.put(AttachmentsColumns.URL, info.path);
        }
        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        if (fileMsg.getAttachmentId() == 0) {
            uri = resolver.insert(AttachmentsColumns.CONTENT_URI, values);
        } else {
            uri = Uri.withAppendedPath(AttachmentsColumns.CONTENT_URI, String.valueOf(fileMsg.getAttachmentId()));
            resolver.update(uri, values, null, null);
        }
        return uri;
    }

}
