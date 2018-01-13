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

package org.onosproject.xmpp.core;

import org.dom4j.Document;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import java.net.InetSocketAddress;


/**
 * Abstracts XMPP device.
 */
public interface XmppDevice {

    /**
     * Returns an associated Netty channel for device.
     *
     * @return Netty Socket Channel
     */
    XmppSession getSession();

    /**
     * Returns an IP address of underlaying XMPP device/client.
     *
     * @return IP address
     */
    InetSocketAddress getIpAddress();

    /**
     * Register a device that has just connected to the system.
     *
     */
    void registerConnectedDevice();

    /**
     * Disconnects the device by closing the TCP connection and unregistering device.
     *
     */
    void disconnectDevice();


    /**
     * Sends a XMPP packet to the client.
     *
     * @param packet the XMPP packet to send
     */
    void sendPacket(Packet packet);

    /**
     * Method for sending raw XML data as XMPP packet.
     *
     * @param document the XML data
     */
    void writeRawXml(Document document);

    /**
     * Handle a XMPP packet from device.
     *
     * @param packet the XMPP packet
     */
    void handlePacket(Packet packet);

    /**
     * Sends a XMPP error to client.
     *
     * @param packetError XMPP error message
     */
    void sendError(PacketError packetError);
}
