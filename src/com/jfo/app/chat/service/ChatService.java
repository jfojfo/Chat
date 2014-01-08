package com.jfo.app.chat.service;

import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.jfo.app.chat.connection.ConnectionManager;
import com.libs.utils.Utils;
import com.lidroid.xutils.util.LogUtils;


public class ChatService extends Service {
    public static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
    public static final String ACTION_SHOW_TOAST = "ACTION_SHOW_TOAST";
    public static final String EXTRA_TEXT = "text";
    public static ArrayList<String> mContacts = new ArrayList<String>();

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d("service created.");
        ConnectionManager.getInstance().init(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d("service destroyed.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    public static void startService(Context context) {
        Intent service = new Intent(context, ChatService.class);
        context.startService(service);
    }
    
    public static void stopService(Context context) {
        Intent service = new Intent(context, ChatService.class);
        service.setAction(ACTION_STOP_SERVICE);
        context.startService(service);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("Received start id " + startId + ": " + intent);
        if (intent != null) {
            String action = intent.getAction();
            LogUtils.d("action:" + action);
            if (ACTION_STOP_SERVICE.equals(action)) {
                stopSelf();
            } else {
                handleCommand(intent);
            }
        }
        return START_STICKY;
    }
    
    private void handleCommand(Intent intent) {
        final String action = intent.getAction();
        if (ACTION_SHOW_TOAST.equals(action)) {
            String msg = intent.getStringExtra(EXTRA_TEXT);
            Utils.showMessage(this, msg);
        }
    }

}
