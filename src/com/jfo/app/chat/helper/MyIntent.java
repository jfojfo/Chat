package com.jfo.app.chat.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class MyIntent extends Intent {

    public MyIntent() {
        super();
    }

    public MyIntent(Context packageContext, Class<?> cls) {
        super(packageContext, cls);
    }

    public MyIntent(Intent o) {
        super(o);
    }

    public MyIntent(String action, Uri uri, Context packageContext, Class<?> cls) {
        super(action, uri, packageContext, cls);
    }

    public MyIntent(String action, Uri uri) {
        super(action, uri);
    }

    public MyIntent(String action) {
        super(action);
    }

    public void activity(Context context) {
        context.startActivity(this);
    }

    public void activity(Activity context, int requestCode) {
        context.startActivityForResult(this, requestCode);
    }

    public void service(Context context) {
        context.startService(this);
    }

    public void broadcast(Context context) {
        context.sendBroadcast(this);
    }
}
