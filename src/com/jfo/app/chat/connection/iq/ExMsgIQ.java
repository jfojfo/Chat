package com.jfo.app.chat.connection.iq;

import org.jivesoftware.smack.packet.IQ;

public class ExMsgIQ extends IQ {
	public String id;
	public String content;

	@Override
	public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<query xmlns=\"jfo:iq:exmsg\" >");
        buf.append("<echo");
        if (id != null) {
        	buf.append(" id=\"").append(id).append("\"");
        }
        buf.append(">");
        if (content != null) {
        	buf.append(content);
        }
        buf.append("</echo>");
        buf.append("</query>");
        return buf.toString();
	}

}
