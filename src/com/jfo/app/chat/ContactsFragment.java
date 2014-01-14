package com.jfo.app.chat;

import java.util.ArrayList;

import org.jivesoftware.smack.packet.Presence;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.jfo.app.chat.connection.ConnectionManager;
import com.libs.defer.Defer.Func;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.util.LogUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnItemClick;

public class ContactsFragment extends Fragment {
    @ViewInject(R.id.list)
    private ListView mList;

    private ContactsAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new ContactsAdapter();
        mList.setAdapter(mAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        ViewUtils.inject(this, view);
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    public void refresh() {
        LogUtils.d("refresh contacts");
        ConnectionManager.getInstance().requestRoster(getActivity()).done(new Func() {
            
            @Override
            public void call(Object... args) {
                ArrayList<String> contacts = (ArrayList<String>) args[0];
                mAdapter.setContacts(contacts);
            }
        });
        ConnectionManager.getInstance().test();
    }
    
    @OnItemClick(R.id.list)
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String name = mAdapter.getItem(position);
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra(ChatFragment.EXTRA_USER, name);
        startActivity(intent);
    }

    private class ContactsAdapter extends BaseAdapter {
        private ArrayList<String> mContacts = new ArrayList<String>();

        public void setContacts(ArrayList<String> contacts) {
            mContacts = contacts;
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getActivity())
                        .inflate(R.layout.contacts_list_item, null);
            }
            String name = getItem(position);
            ((TextView)convertView.findViewById(R.id.name)).setText(name);
            convertView.findViewById(R.id.online).setVisibility(View.GONE);
            Presence status = ConnectionManager.getInstance().getConnection()
                    .getRoster().getPresence(name + "@" + ConnectionManager.XMPP_SERVER);
            if (status.isAvailable()) {
                convertView.findViewById(R.id.online).setVisibility(View.VISIBLE);
            }
            return convertView;
        }

        @Override
        public int getCount() {
            return mContacts.size();
        }

        @Override
        public String getItem(int position) {
            return mContacts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
        
    }

}
