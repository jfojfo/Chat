package com.jfo.app.chat.helper;

import java.util.concurrent.ConcurrentHashMap;

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

}
