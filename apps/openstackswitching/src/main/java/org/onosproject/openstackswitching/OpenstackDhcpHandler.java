/*
* Copyright 2015 Open Networking Laboratory
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
package org.onosproject.openstackswitching;

import org.onosproject.net.packet.InboundPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It handles DHCP request packets.
 */
public class OpenstackDhcpHandler {

    private static Logger log = LoggerFactory
            .getLogger(OpenstackDhcpHandler.class);

    /**
     * Returns OpenstackDhcpHandler reference.
     */
    public OpenstackDhcpHandler() {

    }

    /**
     * Processes DHCP request packets.
     *
     * @param pkt DHCP request packet
     */
    public void processPacketIn(InboundPacket pkt) {
        log.warn("Received a DHCP packet");
    }
}
