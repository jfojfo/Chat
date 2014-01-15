package com.jfo.app.chat.helper;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Activity;

import com.libs.defer.Defer;
import com.libs.defer.Defer.Promise;
import com.lidroid.xutils.util.LogUtils;

public class DeferHelper {
    private static ConcurrentHashMap<Object, MyDefer> mDeferMap = new ConcurrentHashMap<Object, MyDefer>();

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

    public static Promise wrapDefer(Object key) {
        return wrapDefer(key, null);
    }

    public static Promise wrapDefer(Object key, Activity activity) {
        MyDefer defer = new MyDefer(activity);
        mDeferMap.put(key, defer);
        return defer.promise();
    }

    public static MyDefer unwrapDefer(Object key) {
        return mDeferMap.remove(key);
    }

}
