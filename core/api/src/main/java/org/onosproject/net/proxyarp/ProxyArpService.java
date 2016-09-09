/*
 * Copyright 2014-present Open Networking Laboratory
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
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.packet.PacketContext;

/**
 * Service for processing ARP or NDP requests on behalf of applications.
 *
 * @deprecated in Hummingbird release. Use NeighbourResolutionService instead.
 */
// TODO: move to the peer host package
@Deprecated
public interface ProxyArpService {

    /**
     * Returns whether this particular IP address is known to the system.
     *
     * @param addr an IP address
     * @return true if know, false otherwise
     */
    boolean isKnown(IpAddress addr);

    /**
     * Sends a reply for a given request. If the host is not known then the
     * arp or neighbor solicitation will be flooded at all edge ports.
     *
     * @param eth an arp or neighbor solicitation request
     * @param inPort the port the request was received on
     */
    void reply(Ethernet eth, ConnectPoint inPort);

    /**
     * Forwards an ARP or neighbor solicitation request to its destination.
     * Floods at the edg the request if the destination is not known.
     *
     * @param eth an ethernet frame containing an ARP or neighbor solicitation
     * request.
     * @param inPort the port the request was received on
     */
    void forward(Ethernet eth, ConnectPoint inPort);

    /**
     * Handles a arp or neighbor solicitation packet.
     * Replies to arp or neighbor solicitation requests and forwards request
     * to the right place.
     * @param context the packet context to handle
     * @return true if handled, false otherwise.
     */
    boolean handlePacket(PacketContext context);
}
