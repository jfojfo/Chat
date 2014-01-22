package com.jfo.app.chat.connection;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
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
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket.ItemStatus;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.packet.VCard;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import com.googlecode.androidannotations.api.BackgroundExecutor;
import com.jfo.app.chat.Constants;
import com.jfo.app.chat.connection.ex.XMPPFileExtension;
import com.jfo.app.chat.connection.ex.XMPPFileExtensionProvider;
import com.jfo.app.chat.connection.iq.ExMsgIQ;
import com.jfo.app.chat.connection.iq.ExMsgIQProvider;
import com.jfo.app.chat.db.DBAttachment;
import com.jfo.app.chat.helper.AttachmentHelper;
import com.jfo.app.chat.helper.DeferHelper;
import com.jfo.app.chat.helper.DeferHelper.MyDefer;
import com.jfo.app.chat.helper.DeferHelper.RunnableWithDefer;
import com.jfo.app.chat.helper.G;
import com.jfo.app.chat.proto.BDError;
import com.jfo.app.chat.proto.BDUploadFileResult;
import com.jfo.app.chat.provider.ChatDataStructs.MessageColumns;
import com.jfo.app.chat.provider.ChatDataStructs.ThreadsHelper;
import com.jfo.app.chat.provider.ChatProvider;
import com.libs.defer.Defer.Func;
import com.libs.defer.Defer.Promise;
import com.libs.utils.Utils;
import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.db.sqlite.Selector;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.callback.FileDownloadHandler;
import com.lidroid.xutils.http.callback.RequestCallBackHandler;
import com.lidroid.xutils.http.callback.StringDownloadHandler;
import com.lidroid.xutils.http.client.HttpRequest;
import com.lidroid.xutils.http.client.entity.FileUploadEntity;
import com.lidroid.xutils.util.LogUtils;
import com.lidroid.xutils.util.OtherUtils;

public class ConnectionManager {
    public static final String XMPP_SERVER = "xmpp.pickbox.me";
    public static final int XMPP_PORT = 5222;
    private static ConnectionManager mConnectionManager = new ConnectionManager();

    private Context mContext;
    private XMPPConnection mConnection;
    private BlockingQueue<XMPPMsg> mSendQueue;
    private BlockingQueue<Packet> mIncommingMsgQueue;
    private BlockingQueue<Runnable> mDBOpQueue;
    private Thread mSendThread, mReceiveThread;
    private Thread mDBOpThread;
    private HandlerThread mConnThread;
    private Handler mConnHandler;
    private DbUtils db;


    static {
        ProviderManager pm = ProviderManager.getInstance();
        pm.addIQProvider("query", "jfo:iq:exmsg", new ExMsgIQProvider());
        pm.addExtensionProvider("x","jfo:x:file", new XMPPFileExtensionProvider());
    }

    public static ConnectionManager getInstance() {
        return mConnectionManager;
    }

    private ConnectionManager() {
    	
    }

    // init in Service
    public void init(Context context) {
        mContext = context;

        mSendQueue = new LinkedBlockingQueue<XMPPMsg>();
        mIncommingMsgQueue = new LinkedBlockingQueue<Packet>();
        mDBOpQueue = new LinkedBlockingQueue<Runnable>();

        mConnThread = new HandlerThread("conn-thread");
        mConnThread.start();
        mConnHandler = new Handler(mConnThread.getLooper());

        mSendThread = new SendThread("send-thread");
        mSendThread.start();
        mReceiveThread = new ReceiveThread("receive-thread");
        mReceiveThread.start();
        mDBOpThread = new DBOpThread("dbop-thread");
        mDBOpThread.start();

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

    public DbUtils getDB() {
        if (db == null) {
            db = DbUtils.create(mContext, ChatProvider.DATABASE_NAME);
            db.configAllowTransaction(true);
            db.configDebug(false);
        }
        return db;
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
                defer.resolve();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        defer.reject();
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        defer.reject();
    }
    
    public Promise logout(Activity activity) {
        final MyDefer defer = new MyDefer(activity);
        mConnHandler.post(new Runnable() {
            
            @Override
            public void run() {
                doLogout(defer);
            }
        });
        return defer.promise();
    }

    private void doLogout(MyDefer defer) {
        try {
            mConnection.disconnect();
            Utils.removePref(mContext, Constants.PREF_USERNAME);
            Utils.removePref(mContext, Constants.PREF_PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        defer.reject();
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
                        defer.resolve(contacts);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                defer.reject();
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
                    defer.resolve(avatarBytes);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        defer.reject();
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

    public Promise sendFile(Activity activity, final FileMsg fileMsg) {
        final MyDefer defer = new MyDefer(activity);
        doSendFile(defer, fileMsg);
        return defer.promise();
    }
    
    private void doSendFileOld(final MyDefer defer, String user, String path) {
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
                    defer.resolve();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        defer.reject();
    }

    private void doSendFile(final MyDefer defer, final FileMsg fileMsg) {
        dbOp(new RunnableWithDefer() {
            
            @Override
            public void run() {
                LogUtils.d("send file, threadID:" + fileMsg.getThread_id());
                ChatMsg chatMsg = fileMsg;
                if (chatMsg.getThread_id() == 0) {
                    chatMsg.setThread_id(ThreadsHelper.getOrCreateThreadId(mContext, chatMsg.getAddress()));
                    LogUtils.d("send file, create new threadID:" + chatMsg.getThread_id());
                }
                chatMsg.setDate(System.currentTimeMillis());
                chatMsg.setRead(1);
                chatMsg.setType(MessageColumns.TYPE_OUTBOX);
                chatMsg.setStatus(MessageColumns.STATUS_SENDING);
                chatMsg.setMedia_type(MessageColumns.MEDIA_FILE);
                Uri uri = DBOP.inserOrUpdateMsg(mContext, chatMsg);
                LogUtils.d(uri.toString());
                chatMsg.setId(Integer.valueOf(uri.getLastPathSegment()));
                
                DBAttachment dbatt = fileMsg.getAttachment();
                if (dbatt == null) {
                    dbatt = new DBAttachment();
                    dbatt.setName(FilenameUtils.getName(fileMsg.getFile()));
                    dbatt.setMessage_id(fileMsg.getId());
                    dbatt.setLocal_path(fileMsg.getFile());
                }
                File f = new File(fileMsg.getFile());
                if (f.exists())
                    dbatt.setSize(f.length());
                DBOP.insertOrUpdateAttachment(dbatt);
                LogUtils.d("insert attachemnt: " + dbatt.getId());
                
                fileMsg.setAttachment(dbatt);
                getDefer().resolve();
            }
        }).done(new Func() {
            
            @Override
            public void call(Object... args) {
                BackgroundExecutor.execute(new Runnable() {

                    @Override
                    public void run() {
                        DBAttachment dbatt = fileMsg.getAttachment();
                        if (TextUtils.isEmpty(dbatt.getUrl()))
                            uploadFile(defer, fileMsg);
                        else
                            doSendMsg(defer, fileMsg);
                    }
                });
            }
        }).fail(new Func() {
            
            @Override
            public void call(Object... args) {
                defer.reject();
            }
        });
    }
    
    private void uploadFile(final MyDefer defer, final FileMsg fileMsg) {
        String failMsg = "";
        try {
            DBAttachment dbatt = fileMsg.getAttachment();
            File file = new File(dbatt.getLocal_path());

            RequestParams params = new RequestParams();
            String name = UUID.randomUUID().toString();
            params.addQueryStringParameter("path", "/attachment/file/" + name);
            // TODO upload to Baidu PCS by post parameter
            // params.addBodyParameter("file", file);
            params.setBodyEntity(new FileUploadEntity(file, HTTP.OCTET_STREAM_TYPE));

            HttpRequest request = new HttpRequest(HttpRequest.HttpMethod.POST, Constants.URL_UPLOAD_FILE_OLD);
            request.setRequestParams(params, new RequestCallBackHandler() {

                @Override
                public boolean updateProgress(long total, long current, boolean forceUpdateUI) {
                    AttachmentHelper.setProgress(fileMsg.getAttachment().getId(), current, total);
                    defer.notify(fileMsg, total, current);
                    return true;
                }
            });
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
                        doUpdateAttachment(defer, fileMsg, uploadResult);
                        return;
                    }
                    failMsg = result;
                    BDError err = G.fromJson(result, BDError.class);
                    if (err != null) {
                        failMsg = err.error_msg + "(code:" + err.error_code + ")";
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileMsg.setStatus(MessageColumns.STATUS_FAIL_UPLOADING);
        DBOP.markStatus(mContext, fileMsg);
        defer.reject(failMsg);
    }

    private void doUpdateAttachment(final MyDefer defer, final FileMsg fileMsg, final BDUploadFileResult info) {
        dbOp(new RunnableWithDefer() {
            
            @Override
            public void run() {
                DBAttachment dbatt = fileMsg.getAttachment();
                dbatt.setUrl(info.path);
                dbatt.setMd5(info.md5);
                dbatt.setCreate_time(info.ctime);
                dbatt.setModify_time(info.mtime);
                dbatt.setSize(info.size);
                DBOP.insertOrUpdateAttachment(dbatt);
                getDefer().resolve();
            }
        }).done(new Func() {
            
            @Override
            public void call(Object... args) {
                doSendMsg(defer, fileMsg);
            }
        }).fail(new Func() {
            
            @Override
            public void call(Object... args) {
                defer.reject();
            }
        }).always(new Func() {
            
            @Override
            public void call(Object... args) {
                AttachmentHelper.removeProgress(fileMsg.getAttachment().getId());
            }
        });
    }
    
    public Promise downloadFile(Activity activity, final FileMsg fileMsg) {
        final MyDefer defer = new MyDefer(activity);
        dbOp(new RunnableWithDefer() {

            @Override
            public void run() {
                try {
                    DBAttachment dbatt = db.findFirst(Selector.from(
                            DBAttachment.class).where("message_id", "=",
                            fileMsg.getId()));
                    fileMsg.setAttachment(dbatt);
                    fileMsg.setStatus(MessageColumns.STATUS_DOWNLOADING);
                    DBOP.markStatus(mContext, fileMsg);
                    getDefer().resolve();
                } catch (DbException e) {
                    e.printStackTrace();
                }
            }
        }).done(new Func() {

            @Override
            public void call(Object... args) {
                BackgroundExecutor.execute(new Runnable() {

                    @Override
                    public void run() {
                        doGetFileUrl(defer, fileMsg);
                    }
                });
            }
        });
        return defer.promise();
    }
    
    private void doGetFileUrl(final MyDefer defer, FileMsg fileMsg) {
        DBAttachment dbatt = fileMsg.getAttachment();
        if (dbatt == null || TextUtils.isEmpty(dbatt.getUrl())) {
            fileMsg.setStatus(MessageColumns.STATUS_FAIL_DOWNLOADING);
            DBOP.markStatus(mContext, fileMsg);
            defer.reject();
            return;
        }
        try {
            RequestParams params = new RequestParams();
            params.addQueryStringParameter("path", dbatt.getUrl());
            HttpRequest request = new HttpRequest(HttpRequest.HttpMethod.GET, Constants.URL_GET_FILE_URL_OLD);
            request.setRequestParams(params);

            HttpClient client = new DefaultHttpClient();
            HttpResponse resp = client.execute(request);

            int code = resp.getStatusLine().getStatusCode();
            LogUtils.d("code: " + code);
            if (code == HttpStatus.SC_OK) {
                HttpEntity entity = resp.getEntity();
                String result = new StringDownloadHandler().handleEntity(entity, null, "utf8");
                LogUtils.d("result: " + result);
                doDownloadFile(defer, fileMsg, result);
                return;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileMsg.setStatus(MessageColumns.STATUS_FAIL_DOWNLOADING);
        DBOP.markStatus(mContext, fileMsg);
        defer.reject();
    }

    private void doDownloadFile(final MyDefer defer, final FileMsg fileMsg, String url) {
        final DBAttachment dbatt = fileMsg.getAttachment();
        String localPath = Constants.ATTACHMENT_DIR + "/" + dbatt.getName();
        try {
            RequestParams params = new RequestParams();
            HttpRequest request = new HttpRequest(HttpRequest.HttpMethod.GET, url);
            request.setRequestParams(params);

            HttpClient client = new DefaultHttpClient();
            HttpResponse resp = client.execute(request);

            int code = resp.getStatusLine().getStatusCode();
            LogUtils.d("code: " + code);
            if (code == HttpStatus.SC_OK) {
                HttpEntity entity = resp.getEntity();
                boolean autoResume = OtherUtils.isSupportRange(resp);
                String responseFileName = OtherUtils.getFileNameFromHttpResponse(resp);
                File result = new FileDownloadHandler().handleEntity(entity, new RequestCallBackHandler() {
                    
                    @Override
                    public boolean updateProgress(long total, long current,
                            boolean forceUpdateUI) {
                        float percent = 0.0f;
                        if (total != 0)
                            percent = (float) current / total;
                        AttachmentHelper.setProgress(dbatt.getId(), percent);
                        defer.notify(fileMsg, total, current);
                        return true;
                    }
                }, localPath, autoResume, null/*responseFileName*/);
                LogUtils.d("result: " + result.getName());

                fileMsg.setStatus(MessageColumns.STATUS_IDLE);
                DBOP.markStatus(mContext, fileMsg);
                
                dbatt.setLocal_path(localPath);
                DBOP.insertOrUpdateAttachment(dbatt);

                defer.resolve();
                return;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileMsg.setStatus(MessageColumns.STATUS_FAIL_DOWNLOADING);
        DBOP.markStatus(mContext, fileMsg);
        defer.reject();
    }

    public Promise dbOp(RunnableWithDefer action) {
        putToQueue(mDBOpQueue, action);
        return action.getDefer().promise();
    }
    
    public Promise sendMessage(Activity activity, final ChatMsg chatMsg) {
        final MyDefer defer = new MyDefer(activity);
        LogUtils.d("send msg, threadID:" + chatMsg.getThread_id());
        putToQueue(mDBOpQueue, new Runnable() {
            
            @Override
            public void run() {
                if (chatMsg.getThread_id() == 0) {
                    chatMsg.setThread_id(ThreadsHelper.getOrCreateThreadId(mContext, chatMsg.getAddress()));
                    LogUtils.d("send msg, create new threadID:" + chatMsg.getThread_id());
                }
                chatMsg.setDate(System.currentTimeMillis());
                chatMsg.setRead(1);
                chatMsg.setType(MessageColumns.TYPE_OUTBOX);
                chatMsg.setStatus(MessageColumns.STATUS_SENDING);
                chatMsg.setMedia_type(MessageColumns.MEDIA_NORMAL);
                Uri uri = DBOP.inserOrUpdateMsg(mContext, chatMsg);
                LogUtils.d(uri.toString());

                chatMsg.setId(Integer.valueOf(uri.getLastPathSegment()));
                doSendMsg(defer, chatMsg);
            }
        });
        return defer.promise();
    }

    public void doSendMsg(final MyDefer defer, final ChatMsg chatMsg) {
        final XMPPMsg xmppMsg = chatMsg.toXMPP();
        DeferHelper.wrapDefer(xmppMsg).done(new Func() {
            
            @Override
            public void call(Object... args) {
                chatMsg.setStatus(MessageColumns.STATUS_IDLE);
                DBOP.markStatus(mContext, chatMsg);
                defer.resolve();
            }
        }).fail(new Func() {
            
            @Override
            public void call(Object... args) {
                chatMsg.setStatus(MessageColumns.STATUS_FAIL);
                DBOP.markStatus(mContext, chatMsg);
                defer.reject();
            }
        });
        putToQueue(mSendQueue, xmppMsg);
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

    private class DBOpThread extends BaseThread {
        
        public DBOpThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (!isQuit()) {
                try {
                    Runnable action = takeFromQueue(mDBOpQueue);
                    action.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
                    final XMPPMsg xmppMsg = takeFromQueue(mSendQueue);
                    final MyDefer defer = DeferHelper.unwrapDefer(xmppMsg);
                    try {
                        mConnection.sendPacket(xmppMsg);
                    } catch (Exception e) {
                        LogUtils.e("bad connection, need to reconnect");
                        reconnect();
                        if (defer != null) {
                            defer.reject();
                        }
                        continue;
                    }
                    if (defer != null) {
                        defer.resolve();
                    }
                } catch (Exception e) {
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
                    final Packet packet = takeFromQueue(mIncommingMsgQueue);
                    final Message message = (Message) packet;
                    putToQueue(mDBOpQueue, new Runnable() {
                        
                        @Override
                        public void run() {
                            String user = StringUtils.parseName(message.getFrom());
                            int threadID = ThreadsHelper.getOrCreateThreadId(mContext, user);
                            LogUtils.d("recv msg, threadid:" + threadID);

                            ChatMsg chatMsg = new ChatMsg();
                            chatMsg.setAddress(user);
                            chatMsg.setThread_id(threadID);
                            chatMsg.setBody(message.getBody());
                            chatMsg.setDate(System.currentTimeMillis());
                            chatMsg.setRead(0);
                            chatMsg.setType(MessageColumns.TYPE_INBOX);
                            chatMsg.setStatus(MessageColumns.STATUS_IDLE);
                            chatMsg.setMedia_type(MessageColumns.MEDIA_NORMAL);
                            Uri uri = DBOP.inserOrUpdateMsg(mContext, chatMsg);
                            chatMsg.setId(Integer.valueOf(uri.getLastPathSegment()));
                            
                            Collection<PacketExtension> extensions = message.getExtensions();
                            for (PacketExtension ex : extensions) {
                                if (ex instanceof XMPPFileExtension) {
                                    chatMsg.setMedia_type(MessageColumns.MEDIA_FILE);
                                    chatMsg.setStatus(MessageColumns.STATUS_PENDING_TO_DOWNLOAD);
                                    DBOP.inserOrUpdateMsg(mContext, chatMsg);

                                    FileMsg fileMsg = ((XMPPFileExtension) ex).getFileMsg();
                                    DBAttachment dbatt = fileMsg.getAttachment();
                                    dbatt.setMessage_id(chatMsg.getId());
                                    DBOP.insertOrUpdateAttachment(dbatt);
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }
    
    private <T> void putToQueue(BlockingQueue<T> queue, T elem) {
        try {
            queue.put(elem);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private <T> T takeFromQueue(BlockingQueue<T> queue) {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
