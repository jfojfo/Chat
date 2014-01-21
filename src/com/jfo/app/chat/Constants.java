package com.jfo.app.chat;

import android.os.Environment;

public class Constants {
    public static final String PREF_USERNAME = "username";
    public static final String PREF_PASSWORD = "password";
    
    public static final String URL_UPLOAD_FILE = "http://apibox.duapp.com/UploadFile.php";
    public static final String URL_UPLOAD_FILE_OLD = "http://jfotest.duapp.com/chat/UploadFileOld.php";
    public static final String URL_GET_FILE_URL_OLD = "http://jfotest.duapp.com/chat/GetFileUrl.php";

    public static final String ROOT_DIR = Environment.getExternalStorageDirectory() + "/chat";
    public static final String ATTACHMENT_DIR = ROOT_DIR + "/attachment";

}