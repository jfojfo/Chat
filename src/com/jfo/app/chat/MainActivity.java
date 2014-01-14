package com.jfo.app.chat;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import com.jfo.app.chat.connection.ConnectionManager;
import com.libs.defer.Defer.Func;
import com.libs.utils.Utils;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;


public class MainActivity extends FragmentActivity {
    @ViewInject(R.id.tab_message)
    private View mTabMsg;
    
    @ViewInject(R.id.tab_contact)
    private View mTabContacts;
    
    @ViewInject(R.id.tab_profile)
    private View mTabProfile;
    
    private View mLastSelectedView;

    private Fragment mMsgListFragment;
    private Fragment mContactsFragment;
    private Fragment mProfileFragment;
    private Fragment mCurrFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewUtils.inject(this);
        if (savedInstanceState == null) {
            initFragment();
            ConnectionManager.getInstance().autoLogin(this).done(new Func() {
                @Override
                public void call(Object... args) {
                    Utils.showMessage(MainActivity.this, "login success");
                }
            }).fail(new Func() {
                @Override
                public void call(Object... args) {
                    Utils.showMessage(MainActivity.this, "login fail");
                }
            });
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
        mProfileFragment = new ProfileFragment();
        ft.add(R.id.content, mMsgListFragment);
        ft.add(R.id.content, mContactsFragment);
        ft.add(R.id.content, mProfileFragment);
        ft.hide(mMsgListFragment);
        ft.hide(mContactsFragment);
        ft.hide(mProfileFragment);
        
        mCurrFragment = mMsgListFragment;
        ft.show(mCurrFragment);

        ft.commit();
        
        mLastSelectedView = mTabMsg;
        mLastSelectedView.setSelected(true);
    }
    
    private void setFragment(Fragment fragment) {
        if (mCurrFragment == fragment)
            return;
        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        ft.hide(mCurrFragment);
        mCurrFragment = fragment;
        ft.show(mCurrFragment);
        ft.commit();
        
        mCurrFragment.onResume();
    }
    
    private void updateTab(View tab) {
        mLastSelectedView.setSelected(false);
        mLastSelectedView = tab;
        mLastSelectedView.setSelected(true);
    }
    
    @OnClick(R.id.tab_message)
    public void onTabMsgClick(View view) {
        updateTab(view);
        setFragment(mMsgListFragment);
    }

    @OnClick(R.id.tab_contact)
    public void onTabContactsClick(View view) {
        updateTab(view);
        setFragment(mContactsFragment);
    }

    @OnClick(R.id.tab_profile)
    public void onTabProfileClick(View view) {
        updateTab(view);
        setFragment(mProfileFragment);
    }

}
