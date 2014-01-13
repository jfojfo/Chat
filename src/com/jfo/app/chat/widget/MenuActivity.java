package com.jfo.app.chat.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.jfo.app.chat.R;


public class MenuActivity extends Activity implements OnClickListener {
    private static final String TAG = MenuActivity.class.getSimpleName();
    private ViewGroup mMenuContainer;
    private Button mMenuBtnCancel;
    private TextView mMsgText;
    
    public static final String EXTRA_MENU_ITEMS = "menu_items";
    public static final String EXTRA_MENU_SHOW_CANCEL = "menu_show_cancel";
    public static final String EXTRA_MENU_MSG = "menu_msg";
    public static final String EXTRA_MENU_BTN_CANCEL_TEXT = "menu_btn_cancel_text";
    public static final String KEY_MENU_ID = "menu_id";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_layout);
        setResult(RESULT_CANCELED);
        mMenuContainer = (ViewGroup) findViewById(R.id.menu_container);
        mMenuBtnCancel = (Button) findViewById(R.id.menu_cancel);
        mMenuBtnCancel.setOnClickListener(this);
        mMenuBtnCancel.setTag(-1);
        mMsgText = (TextView) findViewById(R.id.msg);
        
        String[] menuItems = getIntent().getStringArrayExtra(EXTRA_MENU_ITEMS);
        initMenuContainer(mMenuContainer, menuItems);
        
        String msg = getIntent().getStringExtra(EXTRA_MENU_MSG);
        if (!TextUtils.isEmpty(msg))
            mMsgText.setText(msg);

        String cancelBtnText = getIntent().getStringExtra(EXTRA_MENU_BTN_CANCEL_TEXT);
        if (!TextUtils.isEmpty(cancelBtnText))
            mMenuBtnCancel.setText(cancelBtnText);
        
        boolean showCancel = getIntent().getBooleanExtra(EXTRA_MENU_SHOW_CANCEL, true);
        if (!showCancel) {
            mMenuBtnCancel.setVisibility(View.GONE);
        }
    }

    private void initMenuContainer(ViewGroup container, String[] menuItems) {
        LayoutParams cancelParam = (LayoutParams) mMenuBtnCancel.getLayoutParams();
        for (int i = 0; i < menuItems.length; i++) {
            Button btn = new Button(this);
            btn.setText(menuItems[i]);
            btn.setBackgroundResource(R.drawable.menu_btn);
            btn.setOnClickListener(this);
            btn.setTag(i);
            LayoutParams params = new LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            params.leftMargin = cancelParam.leftMargin;
            params.rightMargin = cancelParam.rightMargin;
            params.topMargin = cancelParam.topMargin;
            params.bottomMargin = cancelParam.bottomMargin;
            container.addView(btn, params);
        }
    }

    @Override
    public void onClick(View v) {
        int id = (Integer) v.getTag();
        if (id == -1) {
            setResult(RESULT_CANCELED);
        } else {
            Intent intent = new Intent();
            intent.putExtra(KEY_MENU_ID, id);
            setResult(RESULT_OK, intent);
        }
        finish();
    }
    
}
