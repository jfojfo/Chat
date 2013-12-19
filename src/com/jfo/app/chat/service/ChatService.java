package com.jfo.app.chat.service;

import com.jfo.app.chat.connection.ConnectionManager;
import com.lidroid.xutils.util.LogUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;


public class ChatService extends Service {
    public static final String ACTION_STOP_SERVICE = ChatService.class.getPackage().getName() + ".ACTION_STOP_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.d("service created.");
        ConnectionManager.getInstance().init();
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
            LogUtils.d("onStartCommand:" + action);
            if (ACTION_STOP_SERVICE.equals(action)) {
                stopSelf();
            } else {
                handleCommand(intent);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
    
    private void handleCommand(Intent intent) {
        final String action = intent.getAction();
    }

}
