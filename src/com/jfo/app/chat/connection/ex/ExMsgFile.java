package com.jfo.app.chat.connection.ex;

import org.jivesoftware.smack.packet.PacketExtension;

import com.jfo.app.chat.proto.BDUploadFileResult;

public class ExMsgFile implements PacketExtension {
    private BDUploadFileResult info;

    public BDUploadFileResult getInfo() {
        return info;
    }

    public void setInfo(BDUploadFileResult info) {
        this.info = info;
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
        return String.format(
                "<%1$s xmlns=\"%2$s\">" +
                    "<path>%3$s</path>" +
                    "<size>%4$d</size>" +
                    "<ctime>%5$d</ctime>" +
                    "<mtime>%6$d</mtime>" +
                    "<md5>%7$s</md5>" +
        		"</%1$s>",
                getElementName(), 
                getNamespace(),
                info.path,
                info.size,
                info.ctime,
                info.mtime,
                info.md5
        );
        
    }

}
