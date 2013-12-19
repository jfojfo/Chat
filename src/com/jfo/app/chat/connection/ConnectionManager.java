package com.jfo.app.chat.connection;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import com.libs.defer.Defer;
import com.libs.defer.Defer.Promise;

public class ConnectionManager {
    public static final String XMPP_SERVER = "192.168.1.108";
    public static final int XMPP_PORT = 5222;
    private static ConnectionManager mConnectionManager;
    private XMPPConnection mConnection;

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

    public void init() {
        new Thread() {
            @Override
            public void run() {
                doInit();
            }
        }.start();
    }

    private void doInit() {
        ConnectionConfiguration config = new ConnectionConfiguration(
                ConnectionManager.XMPP_SERVER, ConnectionManager.XMPP_PORT);
        config.setCompressionEnabled(true);
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        config.setSASLAuthenticationEnabled(false);

        XMPPConnection connection = new XMPPConnection(config);
        connection.addPacketListener(new BasePacketListener(), null);
        try {
            connection.connect();
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
        mConnection = connection;
    }

    public Promise register(final String name, final String password) {
        final Defer defer = new Defer();
        new Thread() {
            @Override
            public void run() {
                try {
                    if (mConnection != null && mConnection.isConnected()) {
                        AccountManager amgr = mConnection.getAccountManager();
                        amgr.createAccount(name, password);
                        defer.resolve();
                    } else {
                        defer.reject();
                    }
                } catch (XMPPException e) {
                    e.printStackTrace();
                    defer.reject();
                }
            }
        }.start();
        return defer.promise();
    }

    public Promise login(final String name, final String password) {
        final Defer defer = new Defer();
        new Thread() {
            @Override
            public void run() {
                try {
                    if (mConnection != null && mConnection.isConnected()
                            && !mConnection.isAuthenticated()) {
                        mConnection.login(name, password);
                        if (mConnection.isAuthenticated()) {
                            defer.resolve();
                        }
                    } else {
                        defer.reject();
                    }
                } catch (XMPPException e) {
                    e.printStackTrace();
                    defer.reject();
                }
            }
        }.start();
        return defer.promise();
    }

}
