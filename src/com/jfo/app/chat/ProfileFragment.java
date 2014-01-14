package com.jfo.app.chat;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.jfo.app.chat.connection.ConnectionManager;
import com.jfo.app.chat.helper.AvatarPicker;
import com.libs.defer.Defer.Func;
import com.libs.utils.Utils;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

public class ProfileFragment extends Fragment {
    @ViewInject(R.id.avatar)
    private ImageView mAvatar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ConnectionManager.getInstance().fetchAvatar(getActivity()).done(new Func() {
            
            @Override
            public void call(Object... args) {
                byte[] data = (byte[]) args[0];
                Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                mAvatar.setImageBitmap(bm);
            }
        }).fail(new Func() {
            
            @Override
            public void call(Object... args) {
                Utils.showMessage(getActivity(), "fail to fetch avatar");
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        ViewUtils.inject(this, view);
        return view;
    }
    
    @OnClick(R.id.avatar)
    public void onAvatarClick(View view) {
        new AvatarPicker(getActivity()).pick().done(new Func() {
            
            @Override
            public void call(Object... args) {
                Bitmap bm = (Bitmap) args[0];
                mAvatar.setImageBitmap(bm);
                ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayStream);
                uploadPhoto(byteArrayStream.toByteArray());
            }
        });
    }

    private void uploadPhoto(byte[] bytes) {
        ConnectionManager.getInstance().uploadAvatar(getActivity(), bytes).done(new Func() {
            
            @Override
            public void call(Object... args) {
                Utils.showMessage(getActivity(), "upload photo success");
            }
        }).fail(new Func() {
            
            @Override
            public void call(Object... args) {
                Utils.showMessage(getActivity(), "upload photo fail");
            }
        });
    }

}
