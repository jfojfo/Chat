package com.jfo.app.chat;

import com.jfo.app.chat.connection.ConnectionManager;
import com.jfo.app.chat.service.ChatService;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;


public class MainActivity extends FragmentActivity {

    private Fragment mMsgListFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initFragment();
        ConnectionManager.getInstance().autoLogin(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    
    private void initFragment() {
        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
//        mMsgListFragment = new MessageListFragment();
        mMsgListFragment = new ChatFragment();
        ft.add(R.id.content, mMsgListFragment);
        ft.commit();
    }

}
