package com.jfo.app.chat.connection.ex;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

import com.jfo.app.chat.connection.FileMsg;
import com.jfo.app.chat.db.DBAttachment;

public class XMPPFileExtensionProvider implements PacketExtensionProvider {

    @Override
    public PacketExtension parseExtension(XmlPullParser parser)
            throws Exception {
        XMPPFileExtension exMsgFile = new XMPPFileExtension();
        FileMsg fileMsg = new FileMsg();
        DBAttachment dbatt = new DBAttachment();
        fileMsg.setAttachment(dbatt);

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("url"))
                    dbatt.setUrl(parser.nextText());
                if (parser.getName().equals("size"))
                    dbatt.setSize(Long.parseLong(parser.nextText()));
                if (parser.getName().equals("ctime"))
                    dbatt.setCreate_time(Long.parseLong(parser.nextText()));
                if (parser.getName().equals("mtime"))
                    dbatt.setModify_time(Long.parseLong(parser.nextText()));
                if (parser.getName().equals("md5"))
                    dbatt.setMd5(parser.nextText());
                if (parser.getName().equals("file"))
                    dbatt.setName(parser.nextText());
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("x")) {
                    done = true;
                }
            }
        }
        exMsgFile.setFileMsg(fileMsg);
        return exMsgFile;
    }

}
