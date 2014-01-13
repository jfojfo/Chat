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
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.provider.ProviderManager;
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
import com.jfo.app.chat.connection.iq.ExMsgIQ;
import com.jfo.app.chat.connection.iq.ExMsgIQProvider;
import com.jfo.app.chat.provider.ChatDataStructs.MessageColumns;
import com.jfo.app.chat.provider.ChatDataStructs.ThreadsHelper;
import com.jfo.app.chat.service.ChatService;
import com.libs.defer.Defer;
import com.libs.defer.Defer.Func;
import com.libs.defer.Defer.Promise;
import com.libs.utils.Utils;
import com.lidroid.xutils.util.LogUtils;

public class ConnectionManager {
    public static final String XMPP_SERVER = "xmpp.pickbox.me";
    public static final int XMPP_PORT = 5222;
    private static ConnectionManager mConnectionManager = new ConnectionManager();

    private Context mContext;
    private XMPPConnection mConnection;
    private BlockingQueue<ChatMsg> mSendQueue, mWaitingQueue;
    private BlockingQueue<Packet> mIncommingMsgQueue;
    private Thread mSendThread, mReceiveThread;
    private HandlerThread mConnThread;
    private Handler mConnHandler;
    private static final int MSG_CONNECT = 1;
    private static final int MSG_LOGIN = 2;
    private static final int MSG_REGISTER = 3;
    private static final int MSG_RECONNECT = 4;
    private static final int MSG_REQ_ROSTER = 5;

    static {
        ProviderManager.getInstance().addIQProvider("query", "jfo:iq:exmsg", new ExMsgIQProvider());
    }

    public static ConnectionManager getInstance() {
        return mConnectionManager;
    }

    private ConnectionManager() {
    	
    }

    // init in Service
    public void init(Context context) {
        mContext = context;

        mSendQueue = new LinkedBlockingQueue<ChatMsg>();
        mWaitingQueue = new LinkedBlockingQueue<ChatMsg>();
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
        config.setDebuggerEnabled(true);
        config.setCompressionEnabled(true);
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        config.setSASLAuthenticationEnabled(false);
        config.setReconnectionAllowed(false);
//        config.setSendPresence(false);
//        config.setRosterLoadedAtLogin(false);
        mConnection = new XMPPConnection(config);
        mConnection.addPacketListener(new XMPPPacketListener(mContext), null);
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

    public XMPPConnection getConnection() {
        return mConnection;
    }

    public void test() {
    	mConnection.sendPacket(new ExMsgIQ());
    }

    public void requestRoster() {
//        mConnection.sendPacket(new RosterPacket());
    	mConnHandler.sendEmptyMessage(MSG_REQ_ROSTER);
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
            case MSG_REQ_ROSTER:
            	mConnection.getRoster();
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

    public void sendMessage(final ChatMsg chatMsg) {
        LogUtils.d("send msg, threadID:" + chatMsg.getThreadID());
        ContentValues values = new ContentValues();
        values.put(MessageColumns.ADDRESS, chatMsg.getAddress());
        values.put(MessageColumns.BODY, chatMsg.getBody());
        values.put(MessageColumns.DATE, System.currentTimeMillis());
        values.put(MessageColumns.READ, 1);
        values.put(MessageColumns.TYPE, MessageColumns.TYPE_OUTBOX);
        values.put(MessageColumns.STATUS, MessageColumns.STATUS_SENDING);
        values.put(MessageColumns.THREAD_ID, chatMsg.getThreadID());
        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = resolver.insert(MessageColumns.CONTENT_URI, values);
        LogUtils.d(uri.toString());
        chatMsg.setMsgID(Integer.valueOf(uri.getLastPathSegment()));

        if (!mSendQueue.offer(chatMsg)) {
            BackgroundExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        mSendQueue.put(chatMsg);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void resendMessage(final ChatMsg chatMsg) {
        LogUtils.d("re-send msg, threadID:" + chatMsg.getThreadID());
        if (!mSendQueue.offer(chatMsg)) {
            BackgroundExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        mSendQueue.put(chatMsg);
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
            this.quit = true;
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
                    ChatMsg chatMsg = mSendQueue.take();
                    Message message = new Message();
                    message.setFrom(mConnection.getUser());
                    message.setTo(chatMsg.getAddress() + "@" + ConnectionManager.XMPP_SERVER);
                    message.setBody(chatMsg.getBody());
                    message.setType(Message.Type.chat);
                    try {
                        mConnection.sendPacket(message);

                        ContentValues values = new ContentValues();
                        values.put(MessageColumns.STATUS, MessageColumns.STATUS_IDLE);
                        ContentResolver resolver = mContext.getContentResolver();
                        Uri uri = Uri.withAppendedPath(MessageColumns.CONTENT_URI, String.valueOf(chatMsg.getMsgID()));
                        resolver.update(uri, values, null, null);
                    } catch (Exception e) {
                        LogUtils.e("bad connection, need to reconnect");
                        reconnect();

                        ContentValues values = new ContentValues();
                        values.put(MessageColumns.STATUS, MessageColumns.STATUS_FAIL);
                        ContentResolver resolver = mContext.getContentResolver();
                        Uri uri = Uri.withAppendedPath(MessageColumns.CONTENT_URI, String.valueOf(chatMsg.getMsgID()));
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

                    String user = StringUtils.parseName(message.getFrom());
                    long threadID = ThreadsHelper.getOrCreateThreadId(mContext, user);
                    LogUtils.d("recv msg, threadid:" + threadID);
                    ContentValues values = new ContentValues();
                    values.put(MessageColumns.ADDRESS, user);
                    values.put(MessageColumns.THREAD_ID, threadID);
                    values.put(MessageColumns.BODY, message.getBody());
                    values.put(MessageColumns.DATE, System.currentTimeMillis());
                    values.put(MessageColumns.READ, 0);
                    values.put(MessageColumns.TYPE, MessageColumns.TYPE_INBOX);
                    values.put(MessageColumns.STATUS, MessageColumns.STATUS_IDLE);
                    ContentResolver resolver = mContext.getContentResolver();
                    resolver.insert(MessageColumns.CONTENT_URI, values);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
