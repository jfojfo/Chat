package com.jfo.app.chat;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.jfo.app.chat.helper.AvatarPicker;
import com.libs.defer.Defer.Func;
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
            }
        });
    }

}
