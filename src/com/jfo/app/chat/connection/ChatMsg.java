package com.jfo.app.chat.connection;

import org.jivesoftware.smack.packet.Message;

import com.jfo.app.chat.helper.G;

public class ChatMsg {
    private String address;
    private String body;
    private long date;
    private int type;
    private int read;
    private int status;
    private int mediaType;

    private int msgID;
    private int threadID;

    public int getMsgID() {
        return msgID;
    }

    public void setMsgID(int msgID) {
        this.msgID = msgID;
    }

    public int getThreadID() {
        return threadID;
    }

    public void setThreadID(int threadID) {
        this.threadID = threadID;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getMediaType() {
        return mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    public String toJson() {
        return G.toJson(this);
    }

    public XMPPMsg toXMPP() {
        final XMPPMsg xmppMsg = new XMPPMsg();
        xmppMsg.setFrom(ConnectionManager.getInstance().getConnection().getUser());
        xmppMsg.setTo(getAddress() + "@" + ConnectionManager.XMPP_SERVER);
        xmppMsg.setBody(getBody());
        xmppMsg.setType(Message.Type.chat);
        return xmppMsg;
    }
}
