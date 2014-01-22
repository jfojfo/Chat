package com.jfo.app.chat.connection.ex;

import org.jivesoftware.smack.packet.PacketExtension;

import com.jfo.app.chat.connection.FileMsg;
import com.jfo.app.chat.db.DBAttachment;
import com.jfo.app.chat.proto.BDUploadFileResult;

public class XMPPFileExtension implements PacketExtension {
    private FileMsg fileMsg;

    public FileMsg getFileMsg() {
        return fileMsg;
    }

    public void setFileMsg(FileMsg fileMsg) {
        this.fileMsg = fileMsg;
    }

    @Override
    public String getElementName() {
        return "x";
    }

    @Override
    public String getNamespace() {
        return "jfo:x:file";
    }

    @Override
    public String toXML() {
        DBAttachment dbatt = fileMsg.getAttachment();
        return String.format("<%1$s xmlns=\"%2$s\">" + "<url>%3$s</url>"
                + "<size>%4$d</size>" + "<ctime>%5$d</ctime>"
                + "<mtime>%6$d</mtime>" + "<md5>%7$s</md5>"
                + "<file>%8$s</file>"
                + "</%1$s>",
                getElementName(), getNamespace(), dbatt.getUrl(), dbatt.getSize(),
                dbatt.getCreate_time(), dbatt.getModify_time(), dbatt.getMd5(),
                dbatt.getName());
    }

}
