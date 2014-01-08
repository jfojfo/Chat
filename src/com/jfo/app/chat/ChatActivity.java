package com.jfo.app.chat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class ChatActivity extends FragmentActivity {
    
    private Fragment mChatListFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initFragment();
    }

    private void initFragment() {
        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        mChatListFragment = new ChatFragment();
        ft.add(R.id.content, mChatListFragment);
        ft.commit();
    }

}
