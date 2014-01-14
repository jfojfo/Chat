package com.jfo.app.chat.helper;

import java.lang.ref.WeakReference;

import android.app.Activity;

import com.libs.defer.Defer;
import com.lidroid.xutils.util.LogUtils;

public class DeferHelper {

    public static class MyDefer extends Defer {
        private WeakReference<Activity> mActivityRef = null;

        public MyDefer(Activity activity) {
            super();
            mActivityRef = new WeakReference<Activity>(activity);
        }

        public MyDefer() {
            this(null);
        }
    }

    public static void accept(final MyDefer defer, final Object... args) {
        Activity activity = defer.mActivityRef.get();
        if (activity != null) {
            if (activity.isFinishing()) {
                LogUtils.w("is finishing");
                return;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    defer.resolve(args);
                }
            });
        } else {
            defer.resolve(args);
        }
    }

    public static void deny(final MyDefer defer, final Object... args) {
        Activity activity = defer.mActivityRef.get();
        if (activity != null) {
            if (activity.isFinishing()) {
                LogUtils.w("is finishing");
                return;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    defer.reject(args);
                }
            });
        } else {
            defer.reject(args);
        }
    }

}
