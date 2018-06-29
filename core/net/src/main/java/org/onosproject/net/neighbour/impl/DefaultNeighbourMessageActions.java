/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.neighbour.impl;

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborDiscoveryOptions;
import org.onlab.util.Tools;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.neighbour.NeighbourMessageActions;
import org.onosproject.net.neighbour.NeighbourMessageContext;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketService;

import java.nio.ByteBuffer;

/**
 * Implementation of neighbour message actions.
 */
public class DefaultNeighbourMessageActions implements NeighbourMessageActions {

    private final EdgePortService edgeService;
    private final PacketService packetService;

    public DefaultNeighbourMessageActions(PacketService packetService,
                                          EdgePortService edgeService) {
        this.packetService = packetService;
        this.edgeService = edgeService;
    }

    @Override
    public void reply(NeighbourMessageContext context, MacAddress targetMac) {
        replyInternal(context, targetMac);
    }

    @Override
    public void forward(NeighbourMessageContext context, ConnectPoint outPort) {
        sendTo(context.packet(), outPort);
    }

    @Override
    public void forward(NeighbourMessageContext context, Interface outIntf) {
        Ethernet packetOut = context.packet().duplicate();
        if (outIntf.vlan().equals(VlanId.NONE)) {
            // The egress interface has no VLAN Id. Send out an untagged
            // packet
            packetOut.setVlanID(Ethernet.VLAN_UNTAGGED);
        } else {
            // The egress interface has a VLAN set. Send out a tagged packet
            packetOut.setVlanID(outIntf.vlan().toShort());
        }
        sendTo(packetOut, outIntf.connectPoint());
    }

    @Override
    public void flood(NeighbourMessageContext context) {
        Tools.stream(edgeService.getEdgePoints())
                .filter(cp -> !cp.equals(context.inPort()))
                .forEach(cp -> sendTo(context.packet(), cp));
    }

    @Override
    public void drop(NeighbourMessageContext context) {

    }

    private void replyInternal(NeighbourMessageContext context, MacAddress targetMac) {
        switch (context.protocol()) {
        case ARP:
            sendTo(ARP.buildArpReply((Ip4Address) context.target(),
                    targetMac, context.packet()), context.inPort());
            break;
        case NDP:
            sendTo(buildNdpReply((Ip6Address) context.target(), targetMac,
                    context.packet(), context.isRouter()), context.inPort());
            break;
        default:
            break;
        }
    }

    /**
     * Outputs a packet out a specific port.
     *
     * @param packet  the packet to send
     * @param outPort the port to send it out
     */
    private void sendTo(Ethernet packet, ConnectPoint outPort) {
        sendTo(ByteBuffer.wrap(packet.serialize()), outPort);
    }

    /**
     * Outputs a packet out a specific port.
     *
     * @param packet packet to send
     * @param outPort port to send it out
     */
    private void sendTo(ByteBuffer packet, ConnectPoint outPort) {
        if (!edgeService.isEdgePoint(outPort)) {
            // Sanity check to make sure we don't send the packet out an
            // internal port and create a loop (could happen due to
            // misconfiguration).
            return;
        }

        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        builder.setOutput(outPort.port());
        packetService.emit(new DefaultOutboundPacket(outPort.deviceId(),
                builder.build(), packet));
    }

    /**
     * Builds an NDP reply based on a request.
     *
     * @param srcIp   the IP address to use as the reply source
     * @param srcMac  the MAC address to use as the reply source
     * @param request the Neighbor Solicitation request we got
     * @param isRouter true if this reply is sent on behalf of a router
     * @return an Ethernet frame containing the Neighbor Advertisement reply
     */
    private Ethernet buildNdpReply(Ip6Address srcIp, MacAddress srcMac,
                                   Ethernet request, boolean isRouter) {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(request.getSourceMAC());
        eth.setSourceMACAddress(srcMac);
        eth.setEtherType(Ethernet.TYPE_IPV6);
        eth.setVlanID(request.getVlanID());

        IPv6 requestIp = (IPv6) request.getPayload();
        IPv6 ipv6 = new IPv6();
        ipv6.setSourceAddress(srcIp.toOctets());
        ipv6.setDestinationAddress(requestIp.getSourceAddress());
        ipv6.setHopLimit((byte) 255);

        ICMP6 icmp6 = new ICMP6();
        icmp6.setIcmpType(ICMP6.NEIGHBOR_ADVERTISEMENT);
        icmp6.setIcmpCode((byte) 0);

        NeighborAdvertisement nadv = new NeighborAdvertisement();
        nadv.setTargetAddress(srcIp.toOctets());
        nadv.setSolicitedFlag((byte) 1);
        nadv.setOverrideFlag((byte) 1);
        if (isRouter) {
            nadv.setRouterFlag((byte) 1);
        }
        nadv.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                srcMac.toBytes());

        icmp6.setPayload(nadv);
        ipv6.setPayload(icmp6);
        eth.setPayload(ipv6);
        return eth;
    }
}
