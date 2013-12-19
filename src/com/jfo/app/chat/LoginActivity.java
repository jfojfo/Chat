package com.jfo.app.chat;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.UiThread;
import com.googlecode.androidannotations.annotations.ViewById;
import com.jfo.app.chat.connection.ConnectionManager;
import com.libs.defer.Defer.Func;
import com.libs.utils.Utils;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;

@EActivity
public class LoginActivity extends Activity {
    @ViewById(R.id.editName)
    EditText mName;

    @ViewById(R.id.editPassword)
    EditText mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @Click(R.id.btnRegister)
    public void onRegisterClick(View v) {
        String name = mName.getText().toString();
        String password = mPassword.getText().toString();
//        password = MD5.encodeString(password, null);
        ConnectionManager.getInstance().register(name, password).done(new Func() {
            @Override
            public void call(Object... args) {
                showMsg("register success");
            }
        }).fail(new Func() {
            @Override
            public void call(Object... args) {
                showMsg("register fail");
            }
        });
    }

    @Click(R.id.btnLogin)
    public void onLoginClick(View v) {
        String name = mName.getText().toString();
        String password = mPassword.getText().toString();
//        password = MD5.encodeString(password, null);
        ConnectionManager.getInstance().login(name, password).done(new Func() {
            @Override
            public void call(Object... args) {
                showMsg("login success");
            }
        }).fail(new Func() {
            @Override
            public void call(Object... args) {
                showMsg("login fail");
            }
        });
    }
    
    @UiThread
    protected void showMsg(String msg) {
        Utils.showMessage(this, msg);
    }

}
