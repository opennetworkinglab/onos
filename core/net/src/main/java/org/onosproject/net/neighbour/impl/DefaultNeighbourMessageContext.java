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

import java.util.Objects;

import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.neighbour.NeighbourMessageActions;
import org.onosproject.net.neighbour.NeighbourMessageContext;
import org.onosproject.net.neighbour.NeighbourMessageType;
import org.onosproject.net.neighbour.NeighbourProtocol;

import static com.google.common.base.Preconditions.checkState;

/**
 * Default implementation of a neighbour message context.
 */

public class DefaultNeighbourMessageContext implements NeighbourMessageContext {

    private final NeighbourProtocol protocol;
    private final NeighbourMessageType type;

    private final IpAddress target;
    private final IpAddress sender;

    private final Ethernet eth;
    private final ConnectPoint inPort;

    private final NeighbourMessageActions actions;

    private boolean isRouter;

    /**
     * Creates a new neighbour message context.
     *
     * @param actions actions
     * @param eth ethernet frame
     * @param inPort incoming port
     * @param protocol message protocol
     * @param type message type
     * @param target target IP address
     * @param sender sender IP address
     */
    DefaultNeighbourMessageContext(NeighbourMessageActions actions,
                                   Ethernet eth, ConnectPoint inPort,
                                   NeighbourProtocol protocol,
                                   NeighbourMessageType type,
                                   IpAddress target, IpAddress sender) {
        this.actions = actions;
        this.eth = eth;
        this.inPort = inPort;
        this.protocol = protocol;
        this.type = type;
        this.target = target;
        this.sender = sender;
        this.isRouter = false;
    }

    @Override
    public ConnectPoint inPort() {
        return inPort;
    }

    @Override
    public Ethernet packet() {
        return eth;
    }

    @Override
    public NeighbourProtocol protocol() {
        return protocol;
    }

    @Override
    public NeighbourMessageType type() {
        return type;
    }

    @Override
    public VlanId vlan() {
        return VlanId.vlanId(eth.getVlanID());
    }

    @Override
    public MacAddress srcMac() {
        return eth.getSourceMAC();
    }

    @Override
    public MacAddress dstMac() {
        return eth.getDestinationMAC();
    }

    @Override
    public IpAddress target() {
        return target;
    }

    @Override
    public IpAddress sender() {
        return sender;
    }

    @Override
    public void forward(ConnectPoint outPort) {
        actions.forward(this, outPort);
    }

    @Override
    public void forward(Interface outIntf) {
        actions.forward(this, outIntf);
    }

    @Override
    public void reply(MacAddress targetMac) {
        checkState(type == NeighbourMessageType.REQUEST, "can only reply to requests");

        actions.reply(this, targetMac);
    }

    @Override
    public void flood() {
        actions.flood(this);
    }

    @Override
    public void drop() {
        actions.drop(this);
    }

    @Override
    public boolean isRouter() {
        return this.isRouter;
    }

    @Override
    public void setIsRouter(boolean isRouter) {
        this.isRouter = isRouter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, type, target, sender, eth, inPort, isRouter);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DefaultNeighbourMessageContext)) {
            return false;
        }

        DefaultNeighbourMessageContext that = (DefaultNeighbourMessageContext) obj;

        return Objects.equals(protocol, that.protocol) &&
                Objects.equals(type, that.type) &&
                Objects.equals(target, that.target) &&
                Objects.equals(sender, that.sender) &&
                Objects.equals(eth, that.eth) &&
                Objects.equals(inPort, that.inPort) &&
                Objects.equals(isRouter, that.isRouter);
    }

    /**
     * Attempts to create a MessageContext for the given Ethernet frame. If the
     * frame is a valid ARP or NDP request or response, a context will be
     * created.
     *
     * @param eth input Ethernet frame
     * @param inPort in port
     * @param actions actions to take
     * @return MessageContext if the packet was ARP or NDP, otherwise null
     */
    public static NeighbourMessageContext createContext(Ethernet eth,
                                                        ConnectPoint inPort,
                                                        NeighbourMessageActions actions) {
        if (eth.getEtherType() == Ethernet.TYPE_ARP) {
            return createArpContext(eth, inPort, actions);
        } else if (eth.getEtherType() == Ethernet.TYPE_IPV6) {
            return createNdpContext(eth, inPort, actions);
        }

        return null;
    }

    /**
     * Extracts context information from ARP packets.
     *
     * @param eth input Ethernet frame that is thought to be ARP
     * @param inPort in port
     * @param actions actions to take
     * @return MessageContext object if the packet was a valid ARP packet,
     * otherwise null
     */
    private static NeighbourMessageContext createArpContext(Ethernet eth,
                                                            ConnectPoint inPort,
                                                            NeighbourMessageActions actions) {
        if (eth.getEtherType() != Ethernet.TYPE_ARP) {
            return null;
        }

        ARP arp = (ARP) eth.getPayload();

        IpAddress target = Ip4Address.valueOf(arp.getTargetProtocolAddress());
        IpAddress sender = Ip4Address.valueOf(arp.getSenderProtocolAddress());

        NeighbourMessageType type;
        if (arp.getOpCode() == ARP.OP_REQUEST) {
            type = NeighbourMessageType.REQUEST;
        } else if (arp.getOpCode() == ARP.OP_REPLY) {
            type = NeighbourMessageType.REPLY;
        } else {
            return null;
        }

        return new DefaultNeighbourMessageContext(actions, eth, inPort,
                NeighbourProtocol.ARP, type, target, sender);
    }

    /**
     * Extracts context information from NDP packets.
     *
     * @param eth input Ethernet frame that is thought to be NDP
     * @param inPort in port
     * @param actions actions to take
     * @return MessageContext object if the packet was a valid NDP packet,
     * otherwise null
     */
    private static NeighbourMessageContext createNdpContext(Ethernet eth,
                                                            ConnectPoint inPort,
                                                            NeighbourMessageActions actions) {
        if (eth.getEtherType() != Ethernet.TYPE_IPV6) {
            return null;
        }
        IPv6 ipv6 = (IPv6) eth.getPayload();

        if (ipv6.getNextHeader() != IPv6.PROTOCOL_ICMP6) {
            return null;
        }
        ICMP6 icmpv6 = (ICMP6) ipv6.getPayload();

        IpAddress sender = Ip6Address.valueOf(ipv6.getSourceAddress());
        IpAddress target;

        NeighbourMessageType type;
        if (icmpv6.getIcmpType() == ICMP6.NEIGHBOR_SOLICITATION) {
            type = NeighbourMessageType.REQUEST;
            NeighborSolicitation nsol = (NeighborSolicitation) icmpv6.getPayload();
            target = Ip6Address.valueOf(nsol.getTargetAddress());
        } else if (icmpv6.getIcmpType() == ICMP6.NEIGHBOR_ADVERTISEMENT) {
            type = NeighbourMessageType.REPLY;
            /*
             * sender and target are the same in the reply.
             * We use as target the destination ip.
             */
            target = Ip6Address.valueOf(ipv6.getDestinationAddress());
        } else {
            return null;
        }

        return new DefaultNeighbourMessageContext(actions, eth, inPort,
                NeighbourProtocol.NDP, type, target, sender);
    }

}
