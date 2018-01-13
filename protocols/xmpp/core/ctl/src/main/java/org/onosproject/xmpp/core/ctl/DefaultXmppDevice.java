/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.xmpp.core.ctl;


import com.google.common.base.Preconditions;
import org.dom4j.Document;
import org.dom4j.Element;
import org.onosproject.xmpp.core.XmppConstants;
import org.onosproject.xmpp.core.XmppDevice;
import org.onosproject.xmpp.core.XmppDeviceId;
import org.onosproject.xmpp.core.XmppSession;
import org.onosproject.xmpp.core.XmppDeviceAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import java.net.InetSocketAddress;


/**
 * Abstraction of XMPP client.
 */
public class DefaultXmppDevice implements XmppDevice {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected XmppSession session;
    protected XmppDeviceId deviceId;
    protected XmppDeviceAgent agent;

    public DefaultXmppDevice(XmppDeviceId xmppDeviceId, XmppDeviceAgent agent, XmppSession xmppSession) {
        this.deviceId = xmppDeviceId;
        setAgent(agent);
        setSession(xmppSession);
    }

    private void setAgent(XmppDeviceAgent agent) {
        if (this.agent == null) {
            this.agent = agent;
        }
    }

    public void setSession(XmppSession session) {
        if (this.session == null) {
            this.session = session;
        }
    }

    @Override
    public XmppSession getSession() {
        return this.session;
    }

    @Override
    public InetSocketAddress getIpAddress() {
        return this.session.remoteAddress();
    }

    @Override
    public void registerConnectedDevice() {
        this.agent.addConnectedDevice(deviceId, this);
    }

    @Override
    public void disconnectDevice() {
        this.session.closeSession();
        this.agent.removeConnectedDevice(deviceId);
    }

    @Override
    public void writeRawXml(Document document) {
        Element root = document.getRootElement();
        Packet packet = null;
        if (root.getName().equals("iq")) {
            packet = new IQ(root);
        } else if (root.getName().equals("message")) {
            packet = new Message(root);
        } else if (root.getName().equals("presence")) {
            packet = new Presence(root);
        }
        sendPacket(packet);
    }

    @Override
    public void sendPacket(Packet packet) {
        packet.setTo(this.deviceId.getJid());
        packet.setFrom(new JID(XmppConstants.SERVER_JID));
        Preconditions.checkNotNull(packet);
        if (this.session.isActive()) {
            this.session.sendPacket(packet);
        } else {
            logger.warn("Dropping XMPP packets for switch {} because channel is not connected: {}",
                    this.deviceId, packet);
        }
    }

    @Override
    public void handlePacket(Packet packet) {
        logger.info("HANDLING PACKET from " + deviceId);
        this.agent.processUpstreamEvent(deviceId, packet);
    }

    @Override
    public void sendError(PacketError packetError) {
        Packet packet = new IQ();
        packet.setTo(this.deviceId.getJid());
        packet.setFrom(new JID(XmppConstants.SERVER_JID));
        packet.setError(packetError);
        this.session.sendPacket(packet);
    }
}
