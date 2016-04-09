/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pim.impl;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.PIM;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class will process PIM packets.
 */
public class PimPacketHandler {

    private final Logger log = getLogger(getClass());

    /**
     * Constructor for this class.
     */
    public PimPacketHandler() {
    }

    /**
     * Sanitize and process the packet.
     * TODO: replace ConnectPoint with PIMInterface when PIMInterface has been added.
     *
     * @param ethPkt the packet starting with the Ethernet header.
     * @param pimi the PIM Interface the packet arrived on.
     */
    public void processPacket(Ethernet ethPkt, PimInterface pimi) {
        checkNotNull(ethPkt);
        checkNotNull(pimi);

        // Sanitize the ethernet header to ensure it is IPv4.  IPv6 we'll deal with later
        if (ethPkt.getEtherType() != Ethernet.TYPE_IPV4) {
            return;
        }

        // Get the IP header
        IPv4 ip = (IPv4) ethPkt.getPayload();
        if (ip.getProtocol() != IPv4.PROTOCOL_PIM) {
            return;
        }

        // Get the address of our the neighbor that sent this packet to us.
        IpAddress nbraddr = IpAddress.valueOf(ip.getDestinationAddress());
        if (log.isTraceEnabled()) {
            log.trace("Packet {} received on port {}", nbraddr, pimi);
        }

        // Get the PIM header
        PIM pim = (PIM) ip.getPayload();
        checkNotNull(pim);

        // Process the pim packet
        switch (pim.getPimMsgType()) {
            case PIM.TYPE_HELLO:
                pimi.processHello(ethPkt);
                break;
            case PIM.TYPE_JOIN_PRUNE_REQUEST:
                pimi.processJoinPrune(ethPkt);
                log.debug("Received a PIM Join/Prune message");
                break;
            default:
                log.debug("Received unsupported PIM type: {}", pim.getPimMsgType());
                break;
        }
    }
}
