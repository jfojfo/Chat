package com.jfo.app.chat.connection;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import com.googlecode.androidannotations.api.BackgroundExecutor;
import com.jfo.app.chat.Constants;
import com.jfo.app.chat.provider.ChatDataStructs.MessageColumns;
import com.jfo.app.chat.service.ChatService;
import com.libs.defer.Defer;
import com.libs.defer.Defer.Func;
import com.libs.defer.Defer.Promise;
import com.libs.utils.Utils;
import com.lidroid.xutils.util.LogUtils;

public class ConnectionManager {
    public static final String XMPP_SERVER = "192.168.10.103";
    public static final int XMPP_PORT = 5222;
    private static ConnectionManager mConnectionManager;

    private Context mContext;
    private XMPPConnection mConnection;
    private BlockingQueue<JMessage> mSendQueue, mWaitingQueue;
    private BlockingQueue<Packet> mIncommingMsgQueue;
    private Thread mSendThread, mReceiveThread;
    private HandlerThread mConnThread;
    private Handler mConnHandler;
    private static final int MSG_CONNECT = 1;
    private static final int MSG_LOGIN = 2;
    private static final int MSG_REGISTER = 3;
    private static final int MSG_RECONNECT = 4;

    public static ConnectionManager getInstance() {
        if (mConnectionManager == null) {
            synchronized (ConnectionManager.class) {
                if (mConnectionManager == null) {
                    mConnectionManager = new ConnectionManager();
                }
            }
        }
        return mConnectionManager;
    }

    private ConnectionManager() {
        mSendQueue = new LinkedBlockingQueue<JMessage>();
        mWaitingQueue = new LinkedBlockingQueue<JMessage>();
        mIncommingMsgQueue = new LinkedBlockingQueue<Packet>();

        mConnThread = new HandlerThread("conn-thread");
        mConnThread.start();
        mConnHandler = new Handler(mConnThread.getLooper(), mConnCallback);

        mSendThread = new SendThread("send-thread");
        mSendThread.start();
        mReceiveThread = new ReceiveThread("receive-thread");
        mReceiveThread.start();

        ConnectionConfiguration config = new ConnectionConfiguration(
                ConnectionManager.XMPP_SERVER, ConnectionManager.XMPP_PORT);
        // config.setDebuggerEnabled(true);
        config.setCompressionEnabled(true);
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        config.setSASLAuthenticationEnabled(false);
        config.setReconnectionAllowed(false);
        config.setRosterLoadedAtLogin(false);
        mConnection = new XMPPConnection(config);
        mConnection.addPacketListener(new BasePacketListener(mContext), null);
        mConnection.addConnectionListener(new ConnectionListener() {

            @Override
            public void reconnectionSuccessful() {
                LogUtils.d("reconnectionSuccessful");
            }

            @Override
            public void reconnectionFailed(Exception e) {
                LogUtils.d("reconnectionFailed");
            }

            @Override
            public void reconnectingIn(int seconds) {
                LogUtils.d("reconnectingIn");
            }

            @Override
            public void connectionClosedOnError(Exception e) {
                LogUtils.d("connectionClosedOnError");
            }

            @Override
            public void connectionClosed() {
                LogUtils.d("connectionClosed");
            }
        });
    }

    public void init(Context context) {
        mContext = context;
    }

    private Handler.Callback mConnCallback = new Handler.Callback() {

        @Override
        public boolean handleMessage(android.os.Message msg) {
            switch (msg.what) {
            case MSG_CONNECT:
                doConnect();
                break;
            case MSG_RECONNECT:
                doReConnect();
                break;
            }
            return true;
        }

        private void doConnect() {
            try {
                if (!mConnection.isConnected())
                    mConnection.connect();
            } catch (XMPPException e) {
                e.printStackTrace();
            }
        }

        private void doReConnect() {
            try {
                if (!mConnection.isConnected())
                    mConnection.connect();
                if (mConnection.isConnected()) {
                    if (!mConnection.isAuthenticated()) {
                        String name = Utils.getStringPref(mContext,
                                Constants.PREF_USERNAME, null);
                        String passwd = Utils.getStringPref(mContext,
                                Constants.PREF_PASSWORD, null);
                        mConnection.login(name, passwd);
                    }
                }
            } catch (XMPPException e) {
                e.printStackTrace();
            }
        }

    };

    private void doInit() {

        try {
            mConnection.connect();
            // connection.login("test2", "test2");

            // Presence presence = new Presence(Presence.Type.available);
            // presence.setStatus("Q我吧222");
            // connection.sendPacket(presence);
            //
            // ChatManager chatmanager = connection.getChatManager();
            // Chat newChat = chatmanager.createChat("test@192.168.1.108", new
            // MessageListener() {
            // public void processMessage(Chat chat, Message message) {
            // LogUtils.d("Received message: " + message);
            // try {
            // chat.sendMessage(message.getBody());
            // } catch (XMPPException e) {
            // e.printStackTrace();
            // }
            // }
            // });
            // newChat.sendMessage("Hello from test2...");
            //
            // Roster roster = connection.getRoster();
            // Collection<RosterEntry> entries = roster.getEntries();
            // for (RosterEntry entry : entries) {
            // LogUtils.d(entry.getUser());
            // }

        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    public Promise register(final String name, final String password) {
        final Defer defer = new Defer();
        BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!mConnection.isConnected())
                        mConnection.connect();
                    if (mConnection.isConnected()) {
                        AccountManager amgr = mConnection.getAccountManager();
                        amgr.createAccount(name, password);
                        defer.resolve();
                        return;
                    }
                    defer.reject();
                } catch (XMPPException e) {
                    e.printStackTrace();
                    defer.reject();
                }
            }
        });
        return defer.promise();
    }

    public Promise login(final String name, final String password) {
        final Defer defer = new Defer();
        BackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mConnection.isAuthenticated()) {
                        defer.resolve();
                        return;
                    }
                    if (!mConnection.isConnected())
                        mConnection.connect();
                    if (mConnection.isConnected()) {
                        mConnection.login(name, password);
                        if (mConnection.isAuthenticated()) {
                            defer.resolve();
                            return;
                        }
                    }
                    defer.reject();
                } catch (XMPPException e) {
                    e.printStackTrace();
                    defer.reject();
                }
            }
        });
        return defer.promise();
    }

    public void autoLogin(final Context context) {
        String name = Utils.getStringPref(context, Constants.PREF_USERNAME,
                null);
        String passwd = Utils.getStringPref(context, Constants.PREF_PASSWORD,
                null);
        if (name != null) {
            login(name, passwd).done(new Func() {
                @Override
                public void call(Object... args) {
                    Intent intent = new Intent(context, ChatService.class);
                    intent.setAction(ChatService.ACTION_SHOW_TOAST);
                    intent.putExtra(ChatService.EXTRA_TEXT, "login success");
                    context.startService(intent);
                }
            }).fail(new Func() {
                @Override
                public void call(Object... args) {
                    Intent intent = new Intent(context, ChatService.class);
                    intent.setAction(ChatService.ACTION_SHOW_TOAST);
                    intent.putExtra(ChatService.EXTRA_TEXT, "login fail");
                    context.startService(intent);
                }
            });
        }
    }
    
    public void reconnect() {
        mConnHandler.sendEmptyMessage(MSG_RECONNECT);
    }

    public void sendMessage(final JMessage jmsg) {
        ContentValues values = new ContentValues();
        values.put(MessageColumns.ADDRESS, jmsg.getAddress());
        values.put(MessageColumns.BODY, jmsg.getBody());
        values.put(MessageColumns.DATE, System.currentTimeMillis());
        values.put(MessageColumns.READ, 1);
        values.put(MessageColumns.TYPE, MessageColumns.TYPE_OUTBOX);
        values.put(MessageColumns.STATUS, MessageColumns.STATUS_SENDING);
        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = resolver.insert(MessageColumns.CONTENT_URI, values);
        LogUtils.d(uri.toString());
        jmsg.setLocalId(Integer.valueOf(uri.getLastPathSegment()));

        if (!mSendQueue.offer(jmsg)) {
            BackgroundExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        mSendQueue.put(jmsg);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void recvMessage(final Packet packet) {
        if (!mIncommingMsgQueue.offer(packet)) {
            BackgroundExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        mIncommingMsgQueue.put(packet);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private static class BaseThread extends Thread {
        private boolean quit = false;

        public BaseThread(String name) {
            super(name);
        }

        public void quit() {
            quit = true;
        }

        public boolean isQuit() {
            return quit;
        }
    }

    private class SendThread extends BaseThread {

        public SendThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (!isQuit()) {
                try {
                    JMessage jmsg = mSendQueue.take();
                    Message message = new Message();
                    message.setFrom(mConnection.getUser());
                    message.setTo(jmsg.getAddress());
                    message.setBody(jmsg.getBody());
                    message.setType(Message.Type.chat);
                    try {
                        mConnection.sendPacket(message);
                    } catch (Exception e) {
                        LogUtils.e("bad connection, need to reconnect");
                        reconnect();

                        ContentValues values = new ContentValues();
                        values.put(MessageColumns.STATUS,
                                MessageColumns.STATUS_FAIL);
                        ContentResolver resolver = mContext
                                .getContentResolver();
                        Uri uri = Uri.withAppendedPath(MessageColumns.CONTENT_URI, String.valueOf(jmsg.getLocalId()));
                        resolver.update(uri, values, null, null);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private class ReceiveThread extends BaseThread {

        public ReceiveThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (!isQuit()) {
                try {
                    Packet packet = mIncommingMsgQueue.take();
                    Message message = (Message) packet;

                    ContentValues values = new ContentValues();
                    values.put(MessageColumns.ADDRESS,
                            StringUtils.parseName(message.getFrom()));
                    values.put(MessageColumns.BODY, message.getBody());
                    values.put(MessageColumns.DATE, System.currentTimeMillis());
                    values.put(MessageColumns.READ, 0);
                    values.put(MessageColumns.TYPE, MessageColumns.TYPE_INBOX);
                    values.put(MessageColumns.STATUS,
                            MessageColumns.STATUS_IDEL);
                    ContentResolver resolver = mContext.getContentResolver();
                    resolver.insert(MessageColumns.CONTENT_URI, values);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
