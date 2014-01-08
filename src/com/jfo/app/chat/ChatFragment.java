package com.jfo.app.chat;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.jfo.app.chat.connection.ChatMsg;
import com.jfo.app.chat.connection.ConnectionManager;
import com.jfo.app.chat.provider.ChatDataStructs.MessageColumns;
import com.jfo.app.chat.provider.ChatDataStructs.ThreadsHelper;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.util.LogUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;

public class ChatFragment extends Fragment {
    public static final String EXTRA_USER = "user";
    public static final String EXTRA_THREAD_ID = "thread_id";
    
    @ViewInject(R.id.edit)
    private EditText mEdit;
    
    @ViewInject(R.id.list)
    private ListView mList;
    
    private ChatListAdapter mAdapter;
    
    private String mUser;
    private long mThreadID;
    
    private static final int ITEM_DELETE = 1;
    private static final int ITEM_RESEND = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        ViewUtils.inject(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initIntentData(getActivity().getIntent());
        mAdapter = new ChatListAdapter(getActivity(), null);
        mList.setAdapter(mAdapter);
        registerForContextMenu(mList);
        getLoaderManager().initLoader(0, null, mLoader);
    }

    private void initIntentData(Intent intent) {
        mUser = intent.getStringExtra(EXTRA_USER);
        mThreadID = intent.getLongExtra(EXTRA_THREAD_ID, 0);
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
        unregisterForContextMenu(mList);
        getLoaderManager().destroyLoader(0);
        if (mThreadID != 0) {
            ContentValues values = new ContentValues();
            values.put(MessageColumns.READ, 1);
            getActivity().getContentResolver().update(MessageColumns.CONTENT_URI, 
                    values, MessageColumns.THREAD_ID + "=" + mThreadID, null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            getActivity().finish();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.btnSend)
    public void onSend(View view) {
        String text = mEdit.getText().toString();
        ChatMsg chatMsg = new ChatMsg();
        chatMsg.setAddress(mUser);
        chatMsg.setBody(text);
        if (mThreadID == 0) {
            mThreadID = ThreadsHelper.getOrCreateThreadId(getActivity(), mUser);
        }
        chatMsg.setThreadID(mThreadID);
        ConnectionManager.getInstance().sendMessage(chatMsg);
        mEdit.setText("");
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        int position = info.position - mList.getHeaderViewsCount();
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        int status = cursor.getInt(cursor.getColumnIndex(MessageColumns.STATUS));
        if (status == MessageColumns.STATUS_FAIL) {
            menu.add(0, ITEM_RESEND, 0, "重新发送");
        }
        menu.add(0, ITEM_DELETE, 0, "删除");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position - mList.getHeaderViewsCount();
        Cursor cursor = (Cursor) mAdapter.getItem(position);

        switch (item.getItemId()) {
        case ITEM_DELETE: {
            long id = cursor.getLong(cursor.getColumnIndex(MessageColumns._ID));
            getActivity().getContentResolver().delete(ContentUris.withAppendedId(
                    MessageColumns.CONTENT_URI, id), null, null);
            break;
        }
        case ITEM_RESEND: {
            long id = cursor.getLong(cursor.getColumnIndex(MessageColumns._ID));
            String address = cursor.getString(cursor.getColumnIndex(MessageColumns.ADDRESS));
            long threadID = cursor.getLong(cursor.getColumnIndex(MessageColumns.THREAD_ID));
            String body = cursor.getString(cursor.getColumnIndex(MessageColumns.BODY));
            ChatMsg chatMsg = new ChatMsg();
            chatMsg.setMsgID(id);
            chatMsg.setAddress(address);
            chatMsg.setBody(body);
            chatMsg.setThreadID(threadID);
            ConnectionManager.getInstance().resendMessage(chatMsg);
            break;
        }
        }
        return super.onContextItemSelected(item);
    }

    private static class ChatListAdapter extends CursorAdapter {
        int screenWidth;

        public ChatListAdapter(Context context, Cursor c) {
            super(context, c, 0);
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
                    PROJECTION, MessageColumns.ADDRESS + "=?", new String[]{ mUser }, 
                    MessageColumns.DATE + " asc");
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            LogUtils.d("onLoadFinished()");
            mAdapter.changeCursor(cursor);
            if (mThreadID == 0 && cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                mThreadID = cursor.getLong(cursor.getColumnIndex(MessageColumns.THREAD_ID));
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            LogUtils.d("onLoaderReset()");
            mAdapter.changeCursor(null);
        }

    };
    
}
