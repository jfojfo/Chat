package com.jfo.app.chat.connection;

import com.jfo.app.chat.connection.ex.XMPPFileExtension;
import com.jfo.app.chat.proto.BDUploadFileResult;

public class FileMsg extends ChatMsg {
    private String file;
    private BDUploadFileResult info;
    private int attachmentId;

    public int getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(int attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public BDUploadFileResult getInfo() {
        return info;
    }

    public void setInfo(BDUploadFileResult info) {
        this.info = info;
    }

    public XMPPMsg toXMPP() {
        XMPPMsg xmppMsg = super.toXMPP();
        XMPPFileExtension xmppFile = new XMPPFileExtension();
        xmppFile.setInfo(getInfo());
        xmppMsg.addExtension(xmppFile);
        return xmppMsg;
    }
}
