package com.jfo.app.chat;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.jfo.app.chat.provider.ChatDataStructs.ThreadsColumns;
import com.libs.utils.StringConverter;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.util.LogUtils;
import com.lidroid.xutils.view.annotation.ViewInject;

public class InboxListFragment extends Fragment implements OnItemClickListener {

    @ViewInject(R.id.list)
    private ListView mList;
    
    private InboxListAdapter mAdapter;

    private static final int MENU_ITEM_NEW = 1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new InboxListAdapter(getActivity(), null);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        getLoaderManager().initLoader(0, null, mLoader);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inbox_list, container, false);
        ViewUtils.inject(this, view);
        return view;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem itemNew = menu.add(0, MENU_ITEM_NEW, 0, "New");
        itemNew.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_NEW:
            onNewChatClick();
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onNewChatClick() {
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(ChatFragment.EXTRA_USER, "test2");
        startActivity(intent);
    }

    private static class InboxListAdapter extends CursorAdapter {

        public InboxListAdapter(Context context, Cursor c) {
            super(context, c, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.inbox_list_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.name = (TextView) view.findViewById(R.id.name);
            holder.snippet = (TextView) view.findViewById(R.id.snip);
            holder.date = (TextView) view.findViewById(R.id.date);
            holder.unreadCount = (TextView) view.findViewById(R.id.unreadCount);
            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String snippet = cursor.getString(cursor.getColumnIndex(ThreadsColumns.SNIPPET));
            String recipient = cursor.getString(cursor.getColumnIndex(ThreadsColumns.RECIPIENTS));
            long date = cursor.getLong(cursor.getColumnIndex(ThreadsColumns.DATE));
            int unreadCount = cursor.getInt(cursor.getColumnIndex(ThreadsColumns.UNREAD_MESSAGE_COUNT));

            ViewHolder holder = (ViewHolder) view.getTag();
            holder.name.setText(recipient);
            holder.snippet.setText(snippet);
            holder.date.setText(StringConverter.formatTimestamp2Date(date));
            holder.unreadCount.setText(String.valueOf(unreadCount));
            
            if (unreadCount == 0) {
                holder.unreadCount.setVisibility(View.GONE);
            } else {
                holder.unreadCount.setVisibility(View.VISIBLE);
            }
        }

        private static class ViewHolder  {
            TextView name;
            TextView snippet;
            TextView date;
            TextView unreadCount;
        }
    }
    
    private static String[] PROJECTION = {
        ThreadsColumns._ID,
        ThreadsColumns.DATE,
        ThreadsColumns.MESSAGE_COUNT,
        ThreadsColumns.UNREAD_MESSAGE_COUNT,
        ThreadsColumns.READ,
        ThreadsColumns.RECIPIENTS,
        ThreadsColumns.SNIPPET,
    };
    private LoaderCallbacks<Cursor> mLoader = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int arg0, Bundle bundle) {
            LogUtils.d("onCreateLoader()");
            return new CursorLoader(getActivity(), ThreadsColumns.CONTENT_URI,
                    PROJECTION, null, null, null);
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = (Cursor) mAdapter.getItem(position);
        String user = cursor.getString(cursor.getColumnIndex(ThreadsColumns.RECIPIENTS));
        long threadId = cursor.getLong(cursor.getColumnIndex(ThreadsColumns._ID));
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(ChatFragment.EXTRA_USER, user);
        intent.putExtra(ChatFragment.EXTRA_THREAD_ID, threadId);
        startActivity(intent);
    }
}
