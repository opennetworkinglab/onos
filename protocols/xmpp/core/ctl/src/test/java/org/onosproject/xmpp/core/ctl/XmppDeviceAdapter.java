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

import org.dom4j.Document;
import org.onosproject.xmpp.core.XmppDevice;
import org.onosproject.xmpp.core.XmppSession;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import java.net.InetSocketAddress;

/**
 * Testing adapter for the XMPP device driver class.
 */
public class XmppDeviceAdapter implements XmppDevice {

    @java.lang.Override
    public XmppSession getSession() {
        return null;
    }

    @java.lang.Override
    public InetSocketAddress getIpAddress() {
        return null;
    }

    @java.lang.Override
    public void registerConnectedDevice() {

    }

    @java.lang.Override
    public void disconnectDevice() {

    }

    @java.lang.Override
    public void sendPacket(Packet packet) {

    }

    @java.lang.Override
    public void writeRawXml(Document document) {

    }

    @java.lang.Override
    public void handlePacket(Packet packet) {

    }

    @java.lang.Override
    public void sendError(PacketError packetError) {

    }
}
