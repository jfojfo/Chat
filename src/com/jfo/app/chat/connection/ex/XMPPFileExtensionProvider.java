package com.jfo.app.chat.connection.ex;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

import com.jfo.app.chat.proto.BDUploadFileResult;

public class XMPPFileExtensionProvider implements PacketExtensionProvider {

    @Override
    public PacketExtension parseExtension(XmlPullParser parser)
            throws Exception {
        XMPPFileExtension exMsgFile = new XMPPFileExtension();
        BDUploadFileResult info = new BDUploadFileResult();

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("path"))
                    info.path = parser.nextText();
                if (parser.getName().equals("size"))
                    info.size = Long.parseLong(parser.nextText());
                if (parser.getName().equals("ctime"))
                    info.ctime = Long.parseLong(parser.nextText());
                if (parser.getName().equals("mtime"))
                    info.mtime = Long.parseLong(parser.nextText());
                if (parser.getName().equals("md5"))
                    info.md5 = parser.nextText();
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("x")) {
                    done = true;
                }
            }
        }
        exMsgFile.setInfo(info);
        return exMsgFile;
    }

}
