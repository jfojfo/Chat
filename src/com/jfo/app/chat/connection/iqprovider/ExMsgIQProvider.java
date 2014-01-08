package com.jfo.app.chat.connection.iqprovider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import com.jfo.app.chat.connection.iq.ExMsgIQ;

public class ExMsgIQProvider implements IQProvider {

	@Override
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		ExMsgIQ iq = new ExMsgIQ();

		boolean done = false;
        while (!done) {
            int eventType = parser.getEventType();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("query")) {

                } else if (parser.getName().equals("echo")) {
                	iq.id = parser.getAttributeValue("", "id");
                	iq.content = parser.nextText();
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("query".equals(parser.getName())) {
                    done = true;
                    continue;
                }
            }
            eventType = parser.next();
        }

		return iq;
	}

}
