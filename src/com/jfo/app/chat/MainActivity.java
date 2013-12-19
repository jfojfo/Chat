package com.jfo.app.chat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.jfo.app.chat.service.ChatService;
import com.libs.utils.Utils;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Utils.getStringPref(this, Constants.PREF_USERNAME, null) == null) {
            startActivity(new Intent(this, LoginActivity_.class));
        }
        ChatService.startService(this);
        finish();
    }

}
