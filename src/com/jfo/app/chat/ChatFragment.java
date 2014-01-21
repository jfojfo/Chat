package com.jfo.app.chat;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.jfo.app.chat.connection.ChatMsg;
import com.jfo.app.chat.connection.ConnectionManager;
import com.jfo.app.chat.connection.FileMsg;
import com.jfo.app.chat.db.DBAttachment;
import com.jfo.app.chat.db.DBMessage;
import com.jfo.app.chat.helper.AttachmentHelper;
import com.jfo.app.chat.helper.DeferHelper.MyDefer;
import com.jfo.app.chat.helper.DeferHelper.RunnableWithDefer;
import com.jfo.app.chat.helper.FilePicker;
import com.jfo.app.chat.proto.BDUploadFileResult;
import com.jfo.app.chat.provider.ChatDataStructs.MessageColumns;
import com.libs.defer.Defer.Func;
import com.libs.defer.Defer.Promise;
import com.libs.utils.Utils;
import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.ViewUtils;
import com.lidroid.xutils.db.sqlite.CursorUtils;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.util.LogUtils;
import com.lidroid.xutils.view.annotation.ViewInject;
import com.lidroid.xutils.view.annotation.event.OnClick;
import com.lidroid.xutils.view.annotation.event.OnItemClick;

public class ChatFragment extends Fragment {
    public static final String EXTRA_USER = "user";
    public static final String EXTRA_THREAD_ID = "thread_id";
    
    @ViewInject(R.id.edit)
    private EditText mEdit;
    
    @ViewInject(R.id.list)
    private ListView mList;
    
    private ChatListAdapter mAdapter;
    
    private String mUser;
    private int mThreadID;
    
    private static final int ITEM_DELETE = 1;
    private static final int ITEM_RESEND = 2;
    
    private DbUtils db = ConnectionManager.getInstance().getDB();
    private AttachmentLoader mAttachmentLoader;

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
        mAttachmentLoader = new AttachmentLoader(getActivity());
        mAdapter.setAttachmentLoader(mAttachmentLoader);
    }

    private void initIntentData(Intent intent) {
        mUser = intent.getStringExtra(EXTRA_USER);
        mThreadID = intent.getIntExtra(EXTRA_THREAD_ID, 0);
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
        // TODO move following code to proper place
        if (mThreadID != 0) {
            ConnectionManager.getInstance().dbOp(new RunnableWithDefer() {
                
                @Override
                public void run() {
                    FragmentActivity activity = getActivity();
                    if (activity != null /*&& !activity.isFinishing()*/) {
                        // mark all as read
                        ContentValues values = new ContentValues();
                        values.put(MessageColumns.READ, 1);
                        activity.getContentResolver().update(MessageColumns.CONTENT_URI, 
                                values, MessageColumns.THREAD_ID + "=" + mThreadID, null);
                        getDefer().resolve();
                    } else {
                        LogUtils.d("activity finished");
                        getDefer().reject();
                    }
                }
            }).done(new Func() {
                
                @Override
                public void call(Object... args) {
                    LogUtils.d("update read status done");
                }
            });
        }
        getLoaderManager().destroyLoader(0);
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
        final ChatMsg chatMsg = new ChatMsg();
        chatMsg.setAddress(mUser);
        chatMsg.setBody(text);
        chatMsg.setThreadID(mThreadID);
        ConnectionManager.getInstance().sendMessage(getActivity(), chatMsg).done(new Func() {
            
            @Override
            public void call(Object... args) {
                mThreadID = chatMsg.getThreadID();
                LogUtils.d("send msg success");
            }
        }).fail(new Func() {
            
            @Override
            public void call(Object... args) {
                LogUtils.d("send msg fail");
            }
        });
        mEdit.setText("");
    }
    
    @OnClick(R.id.btnSendFile)
    public void onSendFile(View view) {
        new FilePicker(getActivity()).pick().done(new Func() {
            
            @Override
            public void call(Object... args) {
                String file = (String) args[0];
                onSendFile(file);
            }
        });
    }

    private void onSendFile(String file) {
        final FileMsg fileMsg = new FileMsg();
        fileMsg.setAddress(mUser);
        fileMsg.setThreadID(mThreadID);
        fileMsg.setFile(file);
        fileMsg.setBody(FilenameUtils.getName(fileMsg.getFile()));
        ConnectionManager.getInstance().sendFile(getActivity(), fileMsg).done(new Func() {
            
            @Override
            public void call(Object... args) {
                AttachmentHelper.removeProgress(fileMsg.getAttachmentId());
                Utils.showMessage(getActivity(), "send file success");
            }
        }).fail(new Func() {
            
            @Override
            public void call(Object... args) {
                AttachmentHelper.removeProgress(fileMsg.getAttachmentId());
                Utils.showMessage(getActivity(), "send file fail");
            }
        }).progress(new Func() {
            
            @Override
            public void call(Object... args) {
                FileMsg fileMsg = (FileMsg) args[0];
                long total = (Long) args[1];
                long curr = (Long) args[2];
                // LogUtils.d(String.format("total:%d, curr:%d", total, curr));
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        int position = info.position - mList.getHeaderViewsCount();
        DBMessage dbmsg = mAdapter.getItem(position);
        int status = dbmsg.getStatus();
        if (status == MessageColumns.STATUS_FAIL
                || status == MessageColumns.STATUS_FAIL_UPLOADING) {
            menu.add(0, ITEM_RESEND, 0, "重新发送");
        }
        menu.add(0, ITEM_DELETE, 0, "删除");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position - mList.getHeaderViewsCount();
        DBMessage dbmsg = mAdapter.getItem(position);

        switch (item.getItemId()) {
        case ITEM_DELETE: {
            final long id = dbmsg.getId();
            ConnectionManager.getInstance().dbOp(new RunnableWithDefer() {
                
                @Override
                public void run() {
                    getActivity().getContentResolver().delete(ContentUris.withAppendedId(
                            MessageColumns.CONTENT_URI, id), null, null);
                }
            });
            break;
        }
        case ITEM_RESEND: {
            if (dbmsg.getMedia_type() == MessageColumns.MEDIA_NORMAL) {
                resendMsg(dbmsg);
            } else if (dbmsg.getMedia_type() == MessageColumns.MEDIA_FILE) {
                resendFile(dbmsg);
            }
            break;
        }
        }
        return super.onContextItemSelected(item);
    }

    private void resendMsg(DBMessage dbmsg) {
        int id = dbmsg.getId();
        String address = dbmsg.getAddress();
        int threadID = dbmsg.getThread_id();
        String body = dbmsg.getBody();
        ChatMsg chatMsg = new ChatMsg();
        chatMsg.setMsgID(id);
        chatMsg.setThreadID(threadID);
        chatMsg.setAddress(address);
        chatMsg.setBody(body);
        ConnectionManager.getInstance().sendMessage(getActivity(), chatMsg).done(new Func() {
            
            @Override
            public void call(Object... args) {
                LogUtils.d("resend msg success");
            }
        }).fail(new Func() {
            
            @Override
            public void call(Object... args) {
                LogUtils.d("resend msg fail");
            }
        });
    }
    
    private void resendFile(final DBMessage dbmsg) {
        mAttachmentLoader.load(dbmsg.getId()).done(new Func() {
            
            @Override
            public void call(Object... args) {
                DBAttachment attachment = (DBAttachment) args[0];
                if (attachment != null && attachment.getLocal_path() != null) {
                    FileMsg fileMsg = new FileMsg();
                    fileMsg.setMsgID(dbmsg.getId());
                    fileMsg.setThreadID(dbmsg.getThread_id());
                    fileMsg.setAddress(dbmsg.getAddress());
                    fileMsg.setBody(dbmsg.getBody());
                    fileMsg.setAttachmentId(attachment.getId());
                    fileMsg.setFile(attachment.getLocal_path());
                    BDUploadFileResult info = new BDUploadFileResult();
                    info.ctime = attachment.getCreate_time();
                    info.mtime = attachment.getModify_time();
                    info.md5 = attachment.getMd5();
                    info.size = attachment.getSize();
                    info.path = attachment.getUrl();
                    fileMsg.setInfo(info);
                    ConnectionManager.getInstance().sendFile(getActivity(), fileMsg).done(new Func() {
                        
                        @Override
                        public void call(Object... args) {
                            Utils.showMessage(getActivity(), "resend file success");
                        }
                    }).fail(new Func() {
                        
                        @Override
                        public void call(Object... args) {
                            Utils.showMessage(getActivity(), "resend file fail");
                        }
                    }).progress(new Func() {
                        
                        @Override
                        public void call(Object... args) {
                            FileMsg fileMsg = (FileMsg) args[0];
                            long total = (Long) args[1];
                            long curr = (Long) args[2];
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
    }
    
    @OnItemClick(R.id.list)
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final DBMessage dbmsg = mAdapter.getItem(position);
        if (dbmsg.getMedia_type() == MessageColumns.MEDIA_FILE) {
            int status = dbmsg.getStatus();
            if (status == MessageColumns.STATUS_PENDING_TO_DOWNLOAD
                    || status == MessageColumns.STATUS_FAIL_DOWNLOADING) {
                mAttachmentLoader.load(dbmsg.getId()).done(new Func() {
                    
                    @Override
                    public void call(Object... args) {
                        DBAttachment attachment = (DBAttachment) args[0];
                        FileMsg fileMsg = new FileMsg();
                        fileMsg.setAddress(dbmsg.getAddress());
                        fileMsg.setBody(dbmsg.getBody());
                        fileMsg.setDate(dbmsg.getDate());
                        fileMsg.setMsgID(dbmsg.getId());
                        fileMsg.setMediaType(dbmsg.getMedia_type());
                        fileMsg.setRead(dbmsg.getRead());
                        fileMsg.setThreadID(dbmsg.getThread_id());
                        fileMsg.setStatus(dbmsg.getStatus());
                        fileMsg.setFile(dbmsg.getBody());
                        fileMsg.setAttachmentId(attachment.getId());
                        BDUploadFileResult info = new BDUploadFileResult();
                        info.path = attachment.getUrl();
                        info.size = attachment.getSize();
                        info.md5 = attachment.getMd5();
                        info.ctime = attachment.getCreate_time();
                        info.mtime = attachment.getModify_time();
                        fileMsg.setInfo(info);
                        ConnectionManager.getInstance().downloadFile(getActivity(), fileMsg).done(new Func() {
                            
                            @Override
                            public void call(Object... args) {
                                Utils.showMessage(getActivity(), "download attachment success");
                            }
                        }).fail(new Func() {
                            
                            @Override
                            public void call(Object... args) {
                                Utils.showMessage(getActivity(), "download attachment fail");
                            }
                        }).progress(new Func() {
                            
                            @Override
                            public void call(Object... args) {
                                FileMsg fileMsg = (FileMsg) args[0];
                                long total = (Long) args[1];
                                long curr = (Long) args[2];
                                // LogUtils.d(String.format("total:%d, curr:%d", total, curr));
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
            } else if (status == MessageColumns.STATUS_IDLE) {
                mAttachmentLoader.load(dbmsg.getId()).done(new Func() {
                    
                    @Override
                    public void call(Object... args) {
                        DBAttachment attachment = (DBAttachment) args[0];
                        if (attachment != null && attachment.getLocal_path() != null) {
                            try {
                                Uri uri = Uri.fromFile(new File(attachment.getLocal_path()));
                                Intent intent = new Intent( Intent.ACTION_VIEW );
                                intent.setDataAndType(uri, AttachmentHelper.getFileMIME(attachment.getLocal_path()));
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Utils.showMessage(getActivity(), "Cannot find application to open file");
                            }
                        }
                    }
                });
            }
        }
    }
    
    
    private static class ChatListAdapter extends CursorAdapter {
        private int screenWidth;
        private AttachmentLoader mAttachmentLoader;

        public ChatListAdapter(Context context, Cursor c) {
            super(context, c, 0);
            screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        }
        
        public void setAttachmentLoader(AttachmentLoader loader) {
            mAttachmentLoader = loader;
        }

        @Override
        public DBMessage getItem(int position) {
            Cursor cursor = (Cursor) super.getItem(position);
            if (cursor == null)
                return null;
            return CursorUtils.getEntity(ConnectionManager.getInstance().getDB(), 
                    cursor, DBMessage.class, CursorUtils.FindCacheSequence.getSeq());
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            DBMessage dbmsg = getItem(cursor.getPosition());
            String body = dbmsg.getBody();
            int type = dbmsg.getType();
            int status = dbmsg.getStatus();
            int mediaType = dbmsg.getMedia_type();
            
            ViewHolder holder = (ViewHolder) view.getTag();
            IndicatorViewHolder indicatorHolder = (IndicatorViewHolder) holder.indicator.getTag();

            RelativeLayout.LayoutParams params = (LayoutParams) holder.container.getLayoutParams();
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
            }

            holder.tvMsg.setVisibility(View.VISIBLE);
            holder.tvMsg.setText(body);
            indicatorHolder.failIcon.setVisibility(View.GONE);
            indicatorHolder.progress.setVisibility(View.GONE);
            holder.fileItem.setVisibility(View.GONE);

            if (status == MessageColumns.STATUS_FAIL) {
                indicatorHolder.failIcon.setVisibility(View.VISIBLE);
                holder.bubble.setBackgroundResource(R.drawable.bg_out_circle_brown_normal);
            }

            if (mediaType == MessageColumns.MEDIA_FILE) {
                bindViewForFile(view, context, dbmsg);
            }
        }
        
        private void bindViewForFile(View view, Context context, final DBMessage dbmsg) {
            final int id = dbmsg.getId();

            final ViewHolder holder = (ViewHolder) view.getTag();
            final FileViewHolder fileHolder = (FileViewHolder) holder.fileItem.getTag();
            final IndicatorViewHolder indicatorHolder = (IndicatorViewHolder) holder.indicator.getTag();

            holder.tvMsg.setVisibility(View.GONE);
            holder.fileItem.setVisibility(View.VISIBLE);

            mAttachmentLoader.loadForView(view, id).done(new Func() {
                
                @Override
                public void call(Object... args) {
                    DBAttachment attachment = (DBAttachment) args[0];
                    if (attachment == null)
                        return;
                    fileHolder.name.setText(attachment.getName());
                    fileHolder.size.setText("("
                            + Utils.byteCountToDisplaySize(attachment.getSize())
                            + ")");

                    fileHolder.icon.setImageResource(R.drawable.file_normal_btn);
                    int status = dbmsg.getStatus();
                    if (status == MessageColumns.STATUS_SENDING ||
                            status == MessageColumns.STATUS_DOWNLOADING) {
                        float percent = AttachmentHelper
                                .getProgress(attachment.getId());
                        int ipercent = (int) (percent * 100);
                        indicatorHolder.progress.setVisibility(View.VISIBLE);
                        indicatorHolder.percent
                                .setText(String.format("%d%%", ipercent));
                    } else if (status == MessageColumns.STATUS_IDLE) {
                        fileHolder.icon.setImageResource(R.drawable.file_ok_btn);
                    } else if (status == MessageColumns.STATUS_PENDING_TO_DOWNLOAD) {
                        fileHolder.icon.setImageResource(R.drawable.file_download_btn);
                    } else if (status == MessageColumns.STATUS_FAIL_DOWNLOADING) {
                        fileHolder.icon.setImageResource(R.drawable.file_download_btn);
                    } else if (status == MessageColumns.STATUS_FAIL
                            || status == MessageColumns.STATUS_FAIL_UPLOADING) {
                        fileHolder.icon.setImageResource(R.drawable.file_fail);
                    }
                }
            });
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.chat_list_item, parent, false);
            ViewHolder holder = new ViewHolder();
            holder.container = (ViewGroup) view.findViewById(R.id.container);
            holder.tvMsg = (TextView) view.findViewById(R.id.text);
            holder.bubble = (ViewGroup) view.findViewById(R.id.bg_bubble);

            holder.indicator = (ViewGroup) view.findViewById(R.id.indicator);
            IndicatorViewHolder indicatorHolder = new IndicatorViewHolder();
            indicatorHolder.failIcon = view.findViewById(R.id.fail);
            indicatorHolder.progress = view.findViewById(R.id.progress);
            indicatorHolder.percent = (TextView) view.findViewById(R.id.percent);
            holder.indicator.setTag(indicatorHolder);

            holder.fileItem = view.findViewById(R.id.item_file);
            FileViewHolder fileHolder = new FileViewHolder();
            fileHolder.icon = (ImageView) holder.fileItem.findViewById(R.id.icon);
            fileHolder.name = (TextView) holder.fileItem.findViewById(R.id.filename);
            fileHolder.size = (TextView) holder.fileItem.findViewById(R.id.size);
            holder.fileItem.setTag(fileHolder);

            view.setTag(holder);
            return view;
        }

        private static class ViewHolder  {
            ViewGroup container;
            TextView tvMsg;
            ViewGroup bubble;
            ViewGroup indicator;
            View fileItem;
        }
        
        private static class IndicatorViewHolder {
            View failIcon;
            View progress;
            TextView percent;
        }
        
        private static class FileViewHolder {
            ImageView icon;
            TextView name;
            TextView size;
        }
    }
    
    private static class AttachmentLoader {
        private HashMap<Integer, DBAttachment> mAttachmentMap = new HashMap<Integer, DBAttachment>();
        private final ConcurrentHashMap<View, Integer> mPendingRequests = new ConcurrentHashMap<View, Integer>();
        private Context mContext;
        
        public AttachmentLoader(Context context) {
            mContext = context;
        }
        
        public Promise loadForView(final View view, final int msgId) {
            final MyDefer defer = new MyDefer((Activity)mContext);

            DBAttachment attachment = getCachedAttachment(msgId);
            if (attachment != null) {
                defer.resolve(attachment);
                return defer.promise();
            }
            
            Integer oldId = mPendingRequests.get(view);
            if (oldId != null && oldId == msgId) {
                LogUtils.d("already loading");
                return defer.promise();
            }
            
            mPendingRequests.put(view, msgId);
            doLoad(msgId).done(new Func() {
                
                @Override
                public void call(Object... args) {
                    // this is run in UI thread
                    DBAttachment dbatt = (DBAttachment) args[0];
                    cacheAttachment(msgId, dbatt);
                    LogUtils.d(dbatt.toString());
                    Integer pendingId = mPendingRequests.remove(view);
                    if (pendingId != null && pendingId == msgId)
                        defer.resolve(dbatt);
                }
            });
            return defer.promise();
        }

        public Promise load(final int msgId) {
            final MyDefer defer = new MyDefer((Activity)mContext);
            DBAttachment attachment = getCachedAttachment(msgId);
            if (attachment != null) {
                defer.resolve(attachment);
                return defer.promise();
            }
            doLoad(msgId).done(new Func() {
                
                @Override
                public void call(Object... args) {
                    DBAttachment dbatt = (DBAttachment) args[0];
                    cacheAttachment(msgId, dbatt);
                    LogUtils.d(dbatt.toString());
                    defer.resolve(dbatt);
                }
            });
            return defer.promise();
        }
        
        private Promise doLoad(final int msgId) {
            return ConnectionManager.getInstance().dbOp(new RunnableWithDefer((Activity)mContext) {

                @Override
                public void run() {
                    try {
                        DbUtils db = ConnectionManager.getInstance().getDB();
                        DBAttachment attachment = db.findFirst(Selector
                                .from(DBAttachment.class)
                                .where("message_id", "=", msgId));
                        getDefer().resolve(attachment);
                        return;
                    } catch (DbException e) {
                        e.printStackTrace();
                    }
                    getDefer().reject();
                }
            });
        }

        public DBAttachment getCachedAttachment(int msgId) {
            return mAttachmentMap.get(msgId);
        }
        
        public void cacheAttachment(int msgId, DBAttachment att) {
            mAttachmentMap.put(msgId, att);
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
        MessageColumns.MEDIA_TYPE,
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
            FragmentActivity activity = getActivity();
            if (activity == null || activity.isFinishing()) {
                LogUtils.d("activity finished");
                return;
            }
            mAdapter.changeCursor(cursor);
            if (mThreadID == 0 && cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();
                mThreadID = cursor.getInt(cursor.getColumnIndex(MessageColumns.THREAD_ID));
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            LogUtils.d("onLoaderReset()");
            mAdapter.changeCursor(null);
        }

    };
    
}
