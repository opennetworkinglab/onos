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
package org.onosproject.openstackswitching.impl;

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstackswitching.OpenstackPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handles ARP packet from VMs.
 */
public class OpenstackArpHandler {

    private static Logger log = LoggerFactory
            .getLogger(OpenstackArpHandler.class);
    private PacketService packetService;
    private OpenstackRestHandler restHandler;
    private HostService hostService;

    /**
     * Returns OpenstackArpHandler reference.
     *
     * @param restHandler rest API handler reference
     * @param packetService PacketService reference
     * @param hostService host service
     */
    public OpenstackArpHandler(OpenstackRestHandler restHandler, PacketService packetService,
                               HostService hostService) {
        this.restHandler = checkNotNull(restHandler);
        this.packetService = packetService;
        this.hostService = hostService;
    }

    /**
     * Processes ARP request packets.
     * It checks if the target IP is owned by a known host first and then ask to
     * OpenStack if it's not. This ARP proxy does not support overlapping IP.
     *
     * @param pkt ARP request packet
     */
    public void processPacketIn(InboundPacket pkt) {
        Ethernet ethRequest = pkt.parsed();
        ARP arp = (ARP) ethRequest.getPayload();

        if (arp.getOpCode() != ARP.OP_REQUEST) {
            return;
        }

        IpAddress targetIp = Ip4Address.valueOf(arp.getTargetProtocolAddress());
        MacAddress dstMac = getMacFromHostService(targetIp);
        if (dstMac == null) {
            dstMac = getMacFromOpenstack(targetIp);
        }

        if (dstMac == null) {
            log.debug("Failed to find MAC address for {}", targetIp.toString());
            return;
        }

        Ethernet ethReply = ARP.buildArpReply(targetIp.getIp4Address(),
                                              dstMac,
                                              ethRequest);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(pkt.receivedFrom().port())
                .build();

        packetService.emit(new DefaultOutboundPacket(
                pkt.receivedFrom().deviceId(),
                treatment,
                ByteBuffer.wrap(ethReply.serialize())));
    }

    /**
     * Returns MAC address of a host with a given target IP address by asking to
     * OpenStack. It does not support overlapping IP.
     *
     * @param targetIp target ip address
     * @return mac address, or null if it fails to fetch the mac
     */
    private MacAddress getMacFromOpenstack(IpAddress targetIp) {
        checkNotNull(targetIp);

        OpenstackPort openstackPort = restHandler.getPorts()
                .stream()
                .filter(port -> port.fixedIps().containsValue(targetIp))
                .findFirst()
                .orElse(null);

        if (openstackPort != null) {
            log.debug("Found MAC from OpenStack for {}", targetIp.toString());
            return openstackPort.macAddress();
        } else {
            return null;
        }
    }

    /**
     * Returns MAC address of a host with a given target IP address by asking to
     * host service. It does not support overlapping IP.
     *
     * @param targetIp target ip
     * @return mac address, or null if it fails to find the mac
     */
    private MacAddress getMacFromHostService(IpAddress targetIp) {
        checkNotNull(targetIp);

        Host host = hostService.getHostsByIp(targetIp)
                .stream()
                .findFirst()
                .orElse(null);

        if (host != null) {
            log.debug("Found MAC from host service for {}", targetIp.toString());
            return host.mac();
        } else {
            return null;
        }
    }
}
