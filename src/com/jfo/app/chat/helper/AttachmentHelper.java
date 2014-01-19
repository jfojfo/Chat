package com.jfo.app.chat.helper;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import android.text.TextUtils;

public class AttachmentHelper {
    private static ConcurrentHashMap<Long, Float> mProgressMap = new ConcurrentHashMap<Long, Float>();

    public static void setProgress(long attachmentId, float percent) {
        mProgressMap.put(attachmentId, percent);
    }
    
    public static float getProgress(long attachmentId) {
        Float ret = mProgressMap.get(attachmentId);
        if (ret == null)
            return 0;
        return ret;
    }

    public static void removeProgress(long attachmentId) {
        mProgressMap.remove(attachmentId);
    }

    public static int getFileType(String filename) {
        String name = filename;
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex < 0)
            return FILE_TYPE_FILE;

        String end = name.substring(dotIndex, name.length()).toLowerCase();
        if (TextUtils.isEmpty(end))
            return FILE_TYPE_FILE;

        Integer t = sFileMap.get(end);
        if (t != null)
            return t;
        return FILE_TYPE_FILE;
    }

    public static String getFileMIME(String filename) {
        int type = getFileType(filename);
        switch (type) {
        case FILE_TYPE_AUDIO:
            return "audio/*";
        case FILE_TYPE_VIDEO:
            return "video/*";
        case FILE_TYPE_IMAGE:
            return "image/*";
        case FILE_TYPE_APK:
            return "application/vnd.android.package-archive";
        case FILE_TYPE_DOCUMENT:
            return "text/*";
        }
        return "application/*";
    }
    
    public static final int FILE_TYPE_FILE = 0;
    public static final int FILE_TYPE_AUDIO = 1;
    public static final int FILE_TYPE_VIDEO = 2;
    public static final int FILE_TYPE_IMAGE = 3;
    public static final int FILE_TYPE_APK = 4;
    public static final int FILE_TYPE_DOCUMENT = 5;

    private static final HashMap<String, Integer> sFileMap;

    static {
        sFileMap = new HashMap<String, Integer>();
        sFileMap.put(".3gp", FILE_TYPE_VIDEO);
        sFileMap.put(".asf", FILE_TYPE_VIDEO);
        sFileMap.put(".avi", FILE_TYPE_VIDEO);
        sFileMap.put(".m4u", FILE_TYPE_VIDEO);
        sFileMap.put(".m4v", FILE_TYPE_VIDEO);
        sFileMap.put(".mov", FILE_TYPE_VIDEO);
        sFileMap.put(".mp4", FILE_TYPE_VIDEO);
        sFileMap.put(".mpe", FILE_TYPE_VIDEO);
        sFileMap.put(".mpeg", FILE_TYPE_VIDEO);
        sFileMap.put(".mpg", FILE_TYPE_VIDEO);
        sFileMap.put(".mpg4", FILE_TYPE_VIDEO);
        sFileMap.put(".rmvb", FILE_TYPE_VIDEO);
        sFileMap.put(".wmv", FILE_TYPE_VIDEO);

        sFileMap.put(".m3u", FILE_TYPE_AUDIO);
        sFileMap.put(".m4a", FILE_TYPE_AUDIO);
        sFileMap.put(".m4b", FILE_TYPE_AUDIO);
        sFileMap.put(".m4p", FILE_TYPE_AUDIO);
        sFileMap.put(".mp2", FILE_TYPE_AUDIO);
        sFileMap.put(".mp3", FILE_TYPE_AUDIO);
        sFileMap.put(".mpga", FILE_TYPE_AUDIO);
        sFileMap.put(".ogg", FILE_TYPE_AUDIO);
        sFileMap.put(".wav", FILE_TYPE_AUDIO);
        sFileMap.put(".wma", FILE_TYPE_AUDIO);
        sFileMap.put(".aac", FILE_TYPE_AUDIO);
        sFileMap.put(".amr", FILE_TYPE_AUDIO);

        sFileMap.put(".apk", FILE_TYPE_APK);

        sFileMap.put(".bmp", FILE_TYPE_IMAGE);
        sFileMap.put(".gif", FILE_TYPE_IMAGE);
        sFileMap.put(".jpeg", FILE_TYPE_IMAGE);
        sFileMap.put(".jpg", FILE_TYPE_IMAGE);
        sFileMap.put(".png", FILE_TYPE_IMAGE);
        sFileMap.put(".tif", FILE_TYPE_IMAGE);

        sFileMap.put(".txt", FILE_TYPE_DOCUMENT);
        sFileMap.put(".epub", FILE_TYPE_DOCUMENT);
        sFileMap.put(".umd", FILE_TYPE_DOCUMENT);
        sFileMap.put(".pdf", FILE_TYPE_DOCUMENT);
        sFileMap.put(".ps", FILE_TYPE_DOCUMENT);
        sFileMap.put(".doc", FILE_TYPE_DOCUMENT);
        sFileMap.put(".ppt", FILE_TYPE_DOCUMENT);
        sFileMap.put(".xls", FILE_TYPE_DOCUMENT);
        sFileMap.put(".docx", FILE_TYPE_DOCUMENT);
        sFileMap.put(".pptx", FILE_TYPE_DOCUMENT);
        sFileMap.put(".xlsx", FILE_TYPE_DOCUMENT);
        sFileMap.put(".zip", FILE_TYPE_DOCUMENT);
        sFileMap.put(".rar", FILE_TYPE_DOCUMENT);
        sFileMap.put(".tar", FILE_TYPE_DOCUMENT);
        sFileMap.put(".z", FILE_TYPE_DOCUMENT);

        sFileMap.put("", FILE_TYPE_FILE);
    }

}
