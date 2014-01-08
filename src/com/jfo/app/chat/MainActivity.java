package com.jfo.app.chat;

import org.xbill.DNS.MFRecord;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.jfo.app.chat.connection.ConnectionManager;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;


public class MainActivity extends FragmentActivity {
    @ViewInject(R.id.tab_message)
    private View mTabMsg;
    
    @ViewInject(R.id.tab_contact)
    private View mTabContacts;
    
    private Fragment mMsgListFragment;
    private Fragment mContactsFragment;
    private Fragment mCurrFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewUtils.inject(this);
        if (savedInstanceState == null) {
            initFragment();
            ConnectionManager.getInstance().autoLogin(this);
        }
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
        mMsgListFragment = new InboxListFragment();
        mContactsFragment = new ContactsFragment();
        ft.add(R.id.content, mMsgListFragment);
        ft.add(R.id.content, mContactsFragment);
        ft.hide(mContactsFragment);
        ft.commit();
        mCurrFragment = mMsgListFragment;
    }
    
    @OnClick(R.id.tab_message)
    public void onTabMsgClick(View view) {
        if (mCurrFragment == mMsgListFragment)
            return;
        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.hide(mCurrFragment);
        ft.show(mMsgListFragment);
        mCurrFragment = mMsgListFragment;
        ft.commit();
    }

    @OnClick(R.id.tab_contact)
    public void onTabContactsClick(View view) {
        if (mCurrFragment == mContactsFragment)
            return;
        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.hide(mCurrFragment);
        ft.show(mContactsFragment);
        mCurrFragment = mContactsFragment;
        ft.commit();
        ConnectionManager.getInstance().requestRoster();
        ConnectionManager.getInstance().test();
    }

}
