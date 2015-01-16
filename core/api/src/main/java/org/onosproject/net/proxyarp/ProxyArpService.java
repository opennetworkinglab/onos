/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.proxyarp;

import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.packet.PacketContext;

/**
 * Service for processing arp requests on behalf of applications.
 */
// TODO: move to the peer host package
public interface ProxyArpService {

    /**
     * Returns whether this particular IPv4 address is known to the system.
     *
     * @param addr an IPv4 address
     * @return true if know, false otherwise
     */
    boolean isKnown(Ip4Address addr);

    /**
     * Sends a reply for a given request. If the host is not known then the arp
     * will be flooded at all edge ports.
     *
     * @param eth an arp request
     * @param inPort the port the request was received on
     */
    void reply(Ethernet eth, ConnectPoint inPort);

    /**
     * Forwards an ARP request to its destination. Floods at the edge the ARP request if the
     * destination is not known.
     *
     * @param eth an ethernet frame containing an ARP request.
     * @param inPort the port the request was received on
     */
    void forward(Ethernet eth, ConnectPoint inPort);

    /**
     * Handles a arp packet.
     * Replies to arp requests and forwards request to the  right place.
     * @param context the packet context to handle
     * @return true if handled, false otherwise.
     */
    boolean handleArp(PacketContext context);

}
