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

import java.util.HashMap;

/**
 * It handles ARP packet from VMs.
 */
public class OpenstackArpHandler {

    private static Logger log = LoggerFactory
            .getLogger(OpenstackArpHandler.class);

    HashMap<String, OpenstackPort> openstackPortHashMap;

    /**
     * Returns OpenstackArpHandler reference.
     *
     * @param openstackPortMap
     */
    public OpenstackArpHandler(HashMap<String, OpenstackPort> openstackPortMap) {
        this.openstackPortHashMap = openstackPortMap;
    }

    /**
     * Processes ARP packets.
     *
     * @param pkt ARP request packet
     */
    public void processPacketIn(InboundPacket pkt) {
        log.warn("Received an ARP packet");
    }
}
