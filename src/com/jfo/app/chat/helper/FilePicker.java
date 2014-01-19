package com.jfo.app.chat.helper;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.jfo.app.chat.helper.DeferHelper.MyDefer;
import com.libs.defer.Defer;
import com.libs.defer.Defer.Promise;
import com.libs.utils.Utils;
import com.lidroid.xutils.util.LogUtils;

public class FilePicker {
    private static final int JOB_CHOOSE_FILE = 1;

    private FragmentActivity mContext;
    private Defer mDefer;

    public FilePicker(FragmentActivity context) {
        mContext = context;
    }

    public Promise pick() {
        mDefer = new MyDefer(mContext);
        addWrapperFragment();
        return mDefer.promise();
    }

    private void deny(final Object... args) {
        removeWrapperFragment();
        mDefer.reject(args);
    }

    private void accept(final Object... args) {
        removeWrapperFragment();
        mDefer.resolve(args);
    }

    private void addWrapperFragment() {
        final FragmentManager fm = mContext.getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.add(new WrapperFragment(), "FilePicker");
        ft.commit();
    }

    private void removeWrapperFragment() {
        final FragmentManager fm = mContext.getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.remove(fm.findFragmentByTag("FilePicker"));
        ft.commit();
    }

    private void showFileChooser(Fragment fragment) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            fragment.startActivityForResult(
                    Intent.createChooser(intent, "请选择一个要上传的文件"),
                    JOB_CHOOSE_FILE);
        } catch (android.content.ActivityNotFoundException ex) {
            Utils.showMessage(mContext, "请安装文件管理器");
        }
    }

    private class WrapperFragment extends Fragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            showFileChooser(this);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            LogUtils.d("requestCode:" + requestCode + ",resultCode:"
                    + resultCode);
            if (requestCode == JOB_CHOOSE_FILE) {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Uri uri = data.getData();
                    accept(uri.getPath());
                }
            }
        }

    }

}
