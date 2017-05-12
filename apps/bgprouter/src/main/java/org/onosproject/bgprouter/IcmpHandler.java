/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.bgprouter;

import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class IcmpHandler {

    private static final Logger log = LoggerFactory.getLogger(IcmpHandler.class);

    private final PacketService packetService;
    private final InterfaceService interfaceService;

    private final IcmpProcessor processor = new IcmpProcessor();


    public IcmpHandler(InterfaceService interfaceService,
                       PacketService packetService) {
        this.interfaceService = interfaceService;
        this.packetService = packetService;
    }

    public void start() {
        packetService.addProcessor(processor, PacketProcessor.director(4));
    }

    public void stop() {
        packetService.removeProcessor(processor);
    }

    private void processPacketIn(InboundPacket pkt) {

        boolean ipMatches = false;
        Ethernet ethernet = pkt.parsed();
        IPv4 ipv4 = (IPv4) ethernet.getPayload();
        ConnectPoint connectPoint = pkt.receivedFrom();
        IpAddress destIpAddress = IpAddress.valueOf(ipv4.getDestinationAddress());
        Interface targetInterface = interfaceService.getMatchingInterface(destIpAddress);

        if (targetInterface == null) {
            log.trace("No matching interface for {}", destIpAddress);
            return;
        }

        for (InterfaceIpAddress interfaceIpAddress: targetInterface.ipAddressesList()) {
            if (interfaceIpAddress.ipAddress().equals(destIpAddress)) {
                ipMatches = true;
                break;
            }
        }

        if (((ICMP) ipv4.getPayload()).getIcmpType() == ICMP.TYPE_ECHO_REQUEST &&
                ipMatches) {
            sendIcmpResponse(ethernet, connectPoint);
        }
    }

    private void sendIcmpResponse(Ethernet icmpRequest, ConnectPoint outport) {

        Ethernet icmpReplyEth = new Ethernet();

        IPv4 icmpRequestIpv4 = (IPv4) icmpRequest.getPayload();
        IPv4 icmpReplyIpv4 = new IPv4();

        int destAddress = icmpRequestIpv4.getDestinationAddress();
        icmpReplyIpv4.setDestinationAddress(icmpRequestIpv4.getSourceAddress());
        icmpReplyIpv4.setSourceAddress(destAddress);
        icmpReplyIpv4.setTtl((byte) 64);
        icmpReplyIpv4.setChecksum((short) 0);

        ICMP icmpReply = new ICMP();
        icmpReply.setPayload(((ICMP) icmpRequestIpv4.getPayload()).getPayload());
        icmpReply.setIcmpType(ICMP.TYPE_ECHO_REPLY);
        icmpReply.setIcmpCode(ICMP.SUBTYPE_ECHO_REPLY);
        icmpReply.setChecksum((short) 0);

        icmpReplyIpv4.setPayload(icmpReply);

        icmpReplyEth.setPayload(icmpReplyIpv4);
        icmpReplyEth.setEtherType(Ethernet.TYPE_IPV4);
        icmpReplyEth.setDestinationMACAddress(icmpRequest.getSourceMACAddress());
        icmpReplyEth.setSourceMACAddress(icmpRequest.getDestinationMACAddress());
        icmpReplyEth.setVlanID(icmpRequest.getVlanID());

        sendPacketOut(outport, icmpReplyEth);

    }

    private void sendPacketOut(ConnectPoint outport, Ethernet payload) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().
                setOutput(outport.port()).build();
        OutboundPacket packet = new DefaultOutboundPacket(outport.deviceId(),
                treatment, ByteBuffer.wrap(payload.serialize()));
        packetService.emit(packet);
    }

    /**
     * Packet processor responsible receiving and filtering ICMP packets.
     */
    private class IcmpProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.

            if (context.isHandled()) {
                return;
            }

            Ethernet packet = context.inPacket().parsed();

            if (packet == null) {
                return;
            }

            if (packet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) packet.getPayload();
                if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_ICMP) {
                    processPacketIn(context.inPacket());
                }
            }
        }
    }

}
