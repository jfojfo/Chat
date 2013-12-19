package com.jfo.app.chat.connection;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;

import com.lidroid.xutils.util.LogUtils;

public class BasePacketListener implements PacketListener {

    @Override
    public void processPacket(Packet packet) {
        LogUtils.d(packet.toXML());
    }
    
}