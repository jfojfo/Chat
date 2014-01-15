package com.jfo.app.chat.connection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket.ItemStatus;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.MessageEventManager;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.packet.MessageEvent;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.provider.MessageEventProvider;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;

import com.googlecode.androidannotations.api.BackgroundExecutor;
import com.jfo.app.chat.Constants;
import com.jfo.app.chat.connection.ex.ExMsgFile;
import com.jfo.app.chat.connection.iq.ExMsgIQ;
import com.jfo.app.chat.connection.iq.ExMsgIQProvider;
import com.jfo.app.chat.helper.DeferHelper;
import com.jfo.app.chat.helper.DeferHelper.MyDefer;
import com.jfo.app.chat.helper.G;
import com.jfo.app.chat.proto.BDError;
import com.jfo.app.chat.proto.BDUploadFileResult;
import com.jfo.app.chat.provider.ChatDataStructs.MessageColumns;
import com.jfo.app.chat.provider.ChatDataStructs.ThreadsHelper;
import com.libs.defer.Defer.Promise;
import com.libs.utils.Utils;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.callback.StringDownloadHandler;
import com.lidroid.xutils.http.client.HttpRequest;
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
//    private static final int MSG_CONNECT = 1;
//    private static final int MSG_LOGIN = 2;
//    private static final int MSG_REGISTER = 3;
//    private static final int MSG_RECONNECT = 4;
//    private static final int MSG_REQ_ROSTER = 5;
//    private static final int MSG_FETCH_AVATAR = 6;
//    private static final int MSG_UPLOAD_AVATAR = 7;

    static {
        ProviderManager pm = ProviderManager.getInstance();
        pm.addIQProvider("query", "jfo:iq:exmsg", new ExMsgIQProvider());
        pm.addExtensionProvider("x","jfo:x:file", new MessageEventProvider());
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
        mConnHandler = new Handler(mConnThread.getLooper());

        mSendThread = new SendThread("send-thread");
        mSendThread.start();
        mReceiveThread = new ReceiveThread("receive-thread");
        mReceiveThread.start();

        // this will register providers in ConfigureProviderManager.java
        SmackAndroid.init(mContext);
        SmackConfiguration.setPacketReplyTimeout(20000);

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

    private boolean checkConnection() {
        return mConnection != null && mConnection.isConnected();
    }

    public void test() {
        if (!checkConnection())
            return;
        mConnection.sendPacket(new ExMsgIQ());
    }

    public Promise register(Activity activity, final String name, final String password) {
        final MyDefer defer = new MyDefer(activity);
        mConnHandler.post(new Runnable() {
            
            @Override
            public void run() {
                doRegister(defer, name, password);
            }
        });
        return defer.promise();
    }

    private void doRegister(final MyDefer defer, String name, String password) {
        try {
            if (!mConnection.isConnected())
                mConnection.connect();
            if (mConnection.isConnected()) {
                AccountManager amgr = mConnection.getAccountManager();
                amgr.createAccount(name, password);
                DeferHelper.accept(defer);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        DeferHelper.deny(defer);
    }
    
    public Promise autoLogin(final Activity activity) {
        String name = Utils.getStringPref(activity, Constants.PREF_USERNAME,
                null);
        String passwd = Utils.getStringPref(activity, Constants.PREF_PASSWORD,
                null);
        return login(activity, name, passwd);
    }
    
    public Promise login(Activity activity, final String name, final String password) {
        final MyDefer defer = new MyDefer(activity);
        mConnHandler.post(new Runnable() {
            
            @Override
            public void run() {
                doLogin(defer, name, password);
            }
        });
        return defer.promise();
    }

    public void doLogin(final MyDefer defer, String name, String password) {
        try {
            if (mConnection.isAuthenticated()) {
                DeferHelper.accept(defer);
                return;
            }
            if (!mConnection.isConnected())
                mConnection.connect();
            if (mConnection.isConnected()) {
                mConnection.login(name, password);
                if (mConnection.isAuthenticated()) {
                    DeferHelper.accept(defer);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        DeferHelper.deny(defer);
    }

    public Promise reconnect() {
        final MyDefer defer = new MyDefer();
        mConnHandler.post(new Runnable() {
            @Override
            public void run() {
                String name = Utils.getStringPref(mContext,
                        Constants.PREF_USERNAME, null);
                String passwd = Utils.getStringPref(mContext,
                        Constants.PREF_PASSWORD, null);
                doLogin(defer, name, passwd);
            }
        });
        return defer.promise();
    }

    public Promise requestRoster(Activity activity) {
        final MyDefer defer = new MyDefer(activity);
        mConnHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (checkConnection()) {
                        Roster roster = mConnection.getRoster();
                        ArrayList<String> contacts = processRoaster(roster);
                        DeferHelper.accept(defer, contacts);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                DeferHelper.deny(defer);
            }
        });
        return defer.promise();
    }

    private ArrayList<String> processRoaster(Roster roster) {
        HashSet<String> set = new HashSet<String>();
        ArrayList<String> contacts = new ArrayList<String>();
        
        Collection<RosterEntry> items = roster.getEntries();
        Iterator<RosterEntry> iter = items.iterator();
        while (iter.hasNext()) {
            RosterEntry item = iter.next();
            
            String user = item.getUser();
            String name = item.getName();
            Collection<RosterGroup> groups = item.getGroups();
            ItemStatus status = item.getStatus();
            ItemType type = item.getType();
            
            LogUtils.d(item.toString());
            if (set.contains(name)) {
                continue;
            }
            name = StringUtils.parseName(user);
            contacts.add(name);
            set.add(name);
        }
        return contacts;
    }

    public Promise fetchAvatar(Activity activity) {
        final MyDefer defer = new MyDefer(activity);
        mConnHandler.post(new Runnable() {
            @Override
            public void run() {
                BackgroundExecutor.execute(new Runnable() {

                    @Override
                    public void run() {
                        doFetchAvatar(defer);
                    }
                });
            }
        });
        return defer.promise();
    }

    private void doFetchAvatar(final MyDefer defer) {
        try {
            if (checkConnection()) {
                XMPPConnection conn = getConnection();
                VCard vcard = new VCard();
                vcard.load(conn);
                byte[] avatarBytes = vcard.getAvatar();
                if (avatarBytes != null) {
                    DeferHelper.accept(defer, avatarBytes);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        DeferHelper.deny(defer);
    }

    public Promise uploadAvatar(Activity activity, final byte[] bytes) {
        final MyDefer defer = new MyDefer(activity);
        mConnHandler.post(new Runnable() {
            @Override
            public void run() {
                BackgroundExecutor.execute(new Runnable() {

                    @Override
                    public void run() {
                        doUploadAvatar(defer, bytes);
                    }
                });
            }
        });
        return defer.promise();
    }

    private void doUploadAvatar(final MyDefer defer, byte[] bytes) {
        try {
            if (checkConnection()) {
                XMPPConnection conn = getConnection();
                VCard vcard = new VCard();
                vcard.setAvatar(bytes);
                vcard.save(conn);
                defer.resolve();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        defer.reject();
    }

    public Promise sendFile(Activity activity, final String user, final String path) {
        final MyDefer defer = new MyDefer(activity);
        mConnHandler.post(new Runnable() {
            @Override
            public void run() {
                BackgroundExecutor.execute(new Runnable() {

                    @Override
                    public void run() {
//                        doSendFile(defer, user, path);
                        doSendFile2(defer, user, path);
                    }
                });
            }
        });
        return defer.promise();
    }
    
    private void doSendFile(final MyDefer defer, String user, String path) {
        try {
            if (checkConnection()) {
                XMPPConnection conn = getConnection();
                Presence presence = conn.getRoster().getPresence(user + "@" + XMPP_SERVER);
                if (presence.isAvailable()) {
                    FileTransferManager ftmgr = new FileTransferManager(conn);
                    OutgoingFileTransfer transfer = ftmgr  
                            .createOutgoingFileTransfer(presence.getFrom());
                    transfer.sendFile(new File(path), "File");
                    while (!transfer.isDone()) {
                        
                    }
                    DeferHelper.accept(defer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        DeferHelper.deny(defer);
    }

    private void doSendFile2(final MyDefer defer, String user, String path) {
        try {
            if (checkConnection()) {
                XMPPConnection conn = getConnection();
                
                File file = new File(path);
                FileInputStream fis = new FileInputStream(file);
                FileChannel channel = fis.getChannel();
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                WritableByteChannel wchannel = Channels.newChannel(os);
                channel.transferTo(0, file.length(), wchannel);
                channel.close();
                wchannel.close();
                byte[] data = os.toByteArray();
                String encoded = Base64.encodeToString(data, 0);

                RequestParams params = new RequestParams();
                String name = UUID.randomUUID().toString();
                params.addQueryStringParameter("path", "/attachment/file/" + name);
                params.addBodyParameter("file", encoded);
                
                HttpRequest request = new HttpRequest(HttpRequest.HttpMethod.POST, Constants.URL_UPLOAD_FILE_OLD);
                request.setRequestParams(params, null);
                HttpClient client = new DefaultHttpClient();
                HttpResponse resp = client.execute(request);
                
                int code = resp.getStatusLine().getStatusCode();
                LogUtils.d("code: " + code);
                if (code == HttpStatus.SC_OK) {
                    HttpEntity entity = resp.getEntity();
                    String result = new StringDownloadHandler().handleEntity(entity, null, "utf8");
                    LogUtils.d("result: " + result);
                    if (result != null) {
                        BDUploadFileResult uploadResult = G.fromJson(result, BDUploadFileResult.class);
                        if (uploadResult != null) {
                            doSendMsgForFile(defer, uploadResult);
                            return;
                        }
                        BDError err = G.fromJson(result, BDError.class);
                        if (err != null) {
                            DeferHelper.deny(defer, err.error_msg + "(code:" + err.error_code + ")");
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        DeferHelper.deny(defer);
    }
    
    private void doSendMsgForFile(final MyDefer defer, BDUploadFileResult info) {
        try {
            Message chatMsg = new Message();
            ExMsgFile exMsgFile = new ExMsgFile();
            exMsgFile.setInfo(info);
            chatMsg.addExtension(exMsgFile);
            mSendQueue.put(chatMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }
        DeferHelper.deny(defer);
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
