package com.jfo.app.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.jfo.app.chat.connection.ConnectionManager;
import com.libs.defer.Defer.Func;
import com.libs.utils.Utils;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

public class LoginActivity extends Activity {
    @ViewInject(R.id.editName)
    private EditText mName;

    @ViewInject(R.id.editPassword)
    private EditText mPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ViewUtils.inject(this);
    }

    @OnClick(R.id.btnRegister)
    public void onRegisterClick(View v) {
        String name = mName.getText().toString();
        String password = mPassword.getText().toString();
        // password = MD5.encodeString(password, null);
        final ConnectionManager connMgr = ConnectionManager.getInstance();
        connMgr.register(name, password).done(new Func() {
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

    @OnClick(R.id.btnLogin)
    public void onLoginClick(View v) {
        String name = mName.getText().toString();
        String password = mPassword.getText().toString();
        // password = MD5.encodeString(password, null);
        ConnectionManager.getInstance().login(name, password).done(new Func() {
            @Override
            public void call(Object... args) {
                showMsg("login success");
                loginSuccess();
            }
        }).fail(new Func() {
            @Override
            public void call(Object... args) {
                showMsg("login fail");
            }
        });
    }

    private void showMsg(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utils.showMessage(LoginActivity.this, msg);
            }
        });
    }

    private void loginSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String name = mName.getText().toString();
                String password = mPassword.getText().toString();
                Utils.setStringPref(getApplicationContext(), Constants.PREF_USERNAME, name);
                Utils.setStringPref(getApplicationContext(), Constants.PREF_PASSWORD, password);
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
        });
    }
    
}
