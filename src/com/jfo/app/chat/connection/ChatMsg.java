package com.jfo.app.chat.connection;

import org.jivesoftware.smack.packet.Message;

import com.jfo.app.chat.db.DBMessage;
import com.jfo.app.chat.helper.G;

public class ChatMsg extends DBMessage {

    public ChatMsg() {
        super();
    }

    public ChatMsg(DBMessage dbmsg) {
        super(dbmsg);
    }

    public String toJson() {
        return G.toJson(this);
    }

    public XMPPMsg toXMPP() {
        final XMPPMsg xmppMsg = new XMPPMsg();
        xmppMsg.setFrom(ConnectionManager.getInstance().getConnection()
                .getUser());
        xmppMsg.setTo(getAddress() + "@" + ConnectionManager.XMPP_SERVER);
        xmppMsg.setBody(getBody());
        xmppMsg.setType(Message.Type.chat);
        return xmppMsg;
    }
}
