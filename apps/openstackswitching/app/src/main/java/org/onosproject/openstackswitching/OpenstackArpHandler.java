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

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketService;
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

    /**
     * Returns OpenstackArpHandler reference.
     *
     * @param restHandler rest API handler reference
     * @param packetService PacketService reference
     */
    public OpenstackArpHandler(OpenstackRestHandler restHandler, PacketService packetService) {
        this.restHandler = checkNotNull(restHandler);
        this.packetService = packetService;
    }

    /**
     * Processes ARP packets.
     *
     * @param pkt ARP request packet
     */
    public void processPacketIn(InboundPacket pkt) {
        Ethernet ethernet = pkt.parsed();
        ARP arp = (ARP) ethernet.getPayload();

        if (arp.getOpCode() == ARP.OP_REQUEST) {
            byte[] srcMacAddress = arp.getSenderHardwareAddress();
            byte[] srcIPAddress = arp.getSenderProtocolAddress();
            byte[] dstIPAddress = arp.getTargetProtocolAddress();

            //Searches the Dst MAC Address based on openstackPortMap
            MacAddress macAddress = null;

            OpenstackPort openstackPort = restHandler.getPorts().stream().
                    filter(e -> e.fixedIps().containsValue(Ip4Address.valueOf(
                            dstIPAddress))).findAny().orElse(null);

            if (openstackPort != null) {
                macAddress = openstackPort.macAddress();
                log.debug("Found MACAddress: {}", macAddress.toString());
            } else {
                return;
            }

            //Creates a response packet
            ARP arpReply = new ARP();
            arpReply.setOpCode(ARP.OP_REPLY)
                    .setHardwareAddressLength(arp.getHardwareAddressLength())
                    .setHardwareType(arp.getHardwareType())
                    .setProtocolAddressLength(arp.getProtocolAddressLength())
                    .setProtocolType(arp.getProtocolType())
                    .setSenderHardwareAddress(macAddress.toBytes())
                    .setSenderProtocolAddress(dstIPAddress)
                    .setTargetHardwareAddress(srcMacAddress)
                    .setTargetProtocolAddress(srcIPAddress);

            //Sends a response packet
            ethernet.setDestinationMACAddress(srcMacAddress)
                    .setSourceMACAddress(macAddress)
                    .setEtherType(Ethernet.TYPE_ARP)
                    .setPayload(arpReply);

            TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
            builder.setOutput(pkt.receivedFrom().port());
            OutboundPacket packet = new DefaultOutboundPacket(pkt.receivedFrom().deviceId(),
                    builder.build(), ByteBuffer.wrap(ethernet.serialize()));
            packetService.emit(packet);
        }
    }
}
