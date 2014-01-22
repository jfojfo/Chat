package com.jfo.app.chat.connection;

import com.jfo.app.chat.connection.ex.XMPPFileExtension;
import com.jfo.app.chat.db.DBAttachment;
import com.jfo.app.chat.db.DBMessage;

public class FileMsg extends ChatMsg {
    private String file;
    private DBAttachment attachment;

    public FileMsg() {
        super();
    }

    public FileMsg(DBMessage dbmsg) {
        super(dbmsg);
    }

    public DBAttachment getAttachment() {
        return attachment;
    }

    public void setAttachment(DBAttachment attachment) {
        this.attachment = attachment;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public XMPPMsg toXMPP() {
        XMPPMsg xmppMsg = super.toXMPP();
        XMPPFileExtension xmppFile = new XMPPFileExtension();
        xmppFile.setFileMsg(this);
        xmppMsg.addExtension(xmppFile);
        return xmppMsg;
    }
}
