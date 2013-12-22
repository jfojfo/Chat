package com.jfo.app.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.jfo.app.chat.service.ChatService;
import com.libs.utils.Utils;

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Utils.getStringPref(this, Constants.PREF_USERNAME, null) == null) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        ChatService.startService(this);
        finish();
    }

}
