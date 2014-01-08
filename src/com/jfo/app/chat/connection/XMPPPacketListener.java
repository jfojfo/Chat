package com.jfo.app.chat.connection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.RosterPacket.Item;
import org.jivesoftware.smack.packet.RosterPacket.ItemStatus;
import org.jivesoftware.smack.packet.RosterPacket.ItemType;
import org.jivesoftware.smack.util.StringUtils;

import android.content.Context;

import com.jfo.app.chat.connection.iq.ExMsgIQ;
import com.jfo.app.chat.service.ChatService;
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
                processRoaster((RosterPacket) packet);
            } else if (packet instanceof ExMsgIQ) {
            	
            }
        } else if (packet instanceof Presence) {
            
        }
    }
    
    private void processRoaster(RosterPacket packet) {
        ArrayList<String> contacts = ChatService.mContacts;
        contacts.clear();
        HashSet<String> set = new HashSet<String>();
        
        Collection<Item> items = packet.getRosterItems();
        Iterator<Item> iter = items.iterator();
        while (iter.hasNext()) {
            Item item = iter.next();
            
            String user = item.getUser();
            String name = item.getName();
            Set<String> group = item.getGroupNames();
            ItemStatus status = item.getItemStatus();
            ItemType type = item.getItemType();
            
            LogUtils.d(item.toXML());
            if (set.contains(name)) {
                continue;
            }
            name = StringUtils.parseName(user);
            contacts.add(name);
            set.add(name);
        }
    }

}