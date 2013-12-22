package com.jfo.app.chat.connection;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;

import com.jfo.app.chat.helper.G;

public class JMessage {
    private String address;
    private String body;
    private long date;
    private int type;
    private int read;
    private int status;
    
    private long localId;

    public long getLocalId() {
        return localId;
    }

    public void setLocalId(long localId) {
        this.localId = localId;
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

    public static JMessage from(Message message) {
        JMessage jmsg = new JMessage();
        jmsg.address = StringUtils.parseName(message.getFrom());
        jmsg.body = message.getBody();
        return jmsg;
    }
    
    public String toJson() {
        return G.toJson(this);
    }

}
