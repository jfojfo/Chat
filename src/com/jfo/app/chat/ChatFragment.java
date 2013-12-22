package com.jfo.app.chat;

import org.jivesoftware.smack.packet.Message;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.jfo.app.chat.connection.ConnectionManager;
import com.jfo.app.chat.connection.JMessage;
import com.jfo.app.chat.provider.ChatDataStructs.MessageColumns;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.util.LogUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

public class ChatFragment extends Fragment {
    @ViewInject(R.id.edit)
    private EditText mEdit;
    
    @ViewInject(R.id.list)
    private ListView mList;
    
    private ChatListAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_chat, container, false);
        ViewUtils.inject(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new ChatListAdapter(getActivity(), null, 0);
        mList.setAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, mLoader);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getLoaderManager().destroyLoader(0);
    }

    @OnClick(R.id.btnSend)
    public void onSend(View view) {
        String text = mEdit.getText().toString();
        JMessage jmsg = new JMessage();
        jmsg.setAddress("test2@" + ConnectionManager.XMPP_SERVER);
        jmsg.setBody(text);
        ConnectionManager.getInstance().sendMessage(jmsg);
        mEdit.setText("");
    }
    
    private static class ChatListAdapter extends CursorAdapter {
        int screenWidth;

        public ChatListAdapter(Context context, Cursor c, int flag) {
            super(context, c, flag);
            screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String body = cursor.getString(cursor.getColumnIndex(MessageColumns.BODY));
            int type = cursor.getInt(cursor.getColumnIndex(MessageColumns.TYPE));
            int status = cursor.getInt(cursor.getColumnIndex(MessageColumns.STATUS));
            
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.tvMsg.setText(body);

            holder.failIcon.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (LayoutParams) holder.bubble.getLayoutParams();
            if (type == MessageColumns.TYPE_INBOX) {
                holder.bubble.setBackgroundResource(R.drawable.bg_in_circle);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
                params.leftMargin = 0;
                params.rightMargin = (int) (screenWidth * 0.2);
            } else if (type == MessageColumns.TYPE_OUTBOX) {
                holder.bubble.setBackgroundResource(R.drawable.bg_out_circle_green);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
                params.leftMargin = (int) (screenWidth * 0.2);
                params.rightMargin = 0;
                if (status == MessageColumns.STATUS_FAIL) {
                    holder.failIcon.setVisibility(View.VISIBLE);
                    holder.bubble.setBackgroundResource(R.drawable.bg_out_circle_brown_normal);
                }
            }
            holder.bubble.setLayoutParams(params);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.chat_list_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.tvMsg = (TextView) view.findViewById(R.id.text);
            holder.bubble = (ViewGroup) view.findViewById(R.id.bg_bubble);
            holder.failIcon = view.findViewById(R.id.fail);
            view.setTag(holder);
            return view;
        }

        private static class ViewHolder  {
            TextView tvMsg;
            ViewGroup bubble;
            View failIcon;
        }
    }
    
    private static String[] PROJECTION = {
        MessageColumns.ADDRESS,
        MessageColumns.BODY,
        MessageColumns.DATE,
        MessageColumns.READ,
        MessageColumns.PROTOCOL,
        MessageColumns.SMS_ID,
        MessageColumns.STATUS,
        MessageColumns.SUBJECT,
        MessageColumns.THREAD_ID,
        MessageColumns.TYPE,
        MessageColumns._ID,
        MessageColumns.EXPAND_DATA1,
        MessageColumns.EXPAND_DATA2,
        MessageColumns.EXPAND_DATA3,
        MessageColumns.EXPAND_DATA4,
        MessageColumns.EXPAND_DATA5,
    };
    private LoaderCallbacks<Cursor> mLoader = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int arg0, Bundle bundle) {
            LogUtils.d("onCreateLoader()");
            return new CursorLoader(getActivity(), MessageColumns.CONTENT_URI,
                    PROJECTION, null, null, MessageColumns.DATE + " asc");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            LogUtils.d("onLoadFinished()");
            mAdapter.changeCursor(cursor);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            LogUtils.d("onLoaderReset()");
            mAdapter.changeCursor(null);
        }

    };
    
}
