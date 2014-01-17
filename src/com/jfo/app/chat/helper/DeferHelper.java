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

        @Override
        public synchronized void resolve(final Object... args) {
            Activity activity = mActivityRef.get();
            if (activity != null) {
                if (activity.isFinishing()) {
                    LogUtils.w("is finishing");
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MyDefer.super.resolve(args);
                    }
                });
            } else {
                super.resolve(args);
            }
        }

        @Override
        public synchronized void reject(final Object... args) {
            Activity activity = mActivityRef.get();
            if (activity != null) {
                if (activity.isFinishing()) {
                    LogUtils.w("is finishing");
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MyDefer.super.reject(args);
                    }
                });
            } else {
                super.reject(args);
            }
        }

        @Override
        public synchronized void notify(final Object... args) {
            Activity activity = mActivityRef.get();
            if (activity != null) {
                if (activity.isFinishing()) {
                    LogUtils.w("is finishing");
                    return;
                }
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MyDefer.super.notify(args);
                    }
                });
            } else {
                super.notify(args);
            }
        }

    }

    public static abstract class RunnableWithDefer implements Runnable {
        private MyDefer mDefer;

        public RunnableWithDefer() {
            mDefer = new MyDefer();
        }

        public RunnableWithDefer(MyDefer defer) {
            mDefer = defer;
        }

        public RunnableWithDefer(Activity activity) {
            mDefer = new MyDefer(activity);
        }

        public MyDefer getDefer() {
            return mDefer;
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
