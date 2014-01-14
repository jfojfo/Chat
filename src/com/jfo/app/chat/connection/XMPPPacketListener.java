package com.jfo.app.chat.connection;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;

import android.content.Context;

import com.jfo.app.chat.connection.iq.ExMsgIQ;
import com.lidroid.xutils.util.LogUtils;

public class XMPPPacketListener implements PacketListener {
    
    private Context mContext;
    
    public XMPPPacketListener(Context context) {
        mContext = context;
    }

    @Override
    public void processPacket(Packet packet) {
        LogUtils.d(packet.toXML());
        if (packet instanceof Message) {
            Message msg = (Message) packet;
            Message.Type type = msg.getType();
            if (type == Message.Type.chat) {
                ConnectionManager.getInstance().recvMessage((Message)packet);
            }
        } else if (packet instanceof IQ) {
            if (packet instanceof RosterPacket) {
                
            } else if (packet instanceof ExMsgIQ) {
            	
            }
        } else if (packet instanceof Presence) {
            
        }
    }

}