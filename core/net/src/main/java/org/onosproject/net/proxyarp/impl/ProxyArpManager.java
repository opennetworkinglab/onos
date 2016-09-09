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
package org.onosproject.net.proxyarp.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborAdvertisement;
import org.onlab.packet.ndp.NeighborDiscoveryOptions;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.proxyarp.ProxyArpService;
import org.onosproject.net.proxyarp.ProxyArpStore;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.VlanId.vlanId;
import static org.onosproject.net.HostId.hostId;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppPermission.Type.*;

/**
 * Implementation of the proxy ARP service.
 *
 * @deprecated in Hummingbird release
 */
@Deprecated
@Component(immediate = true)
@Service
public class ProxyArpManager implements ProxyArpService {

    private final Logger log = getLogger(getClass());

    private static final String MAC_ADDR_NULL = "MAC address cannot be null.";
    private static final String REQUEST_NULL = "ARP or NDP request cannot be null.";
    private static final String MSG_NOT_REQUEST = "Message is not an ARP or NDP request";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EdgePortService edgeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ProxyArpStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    private enum Protocol {
        ARP, NDP
    }

    private enum MessageType {
        REQUEST, REPLY
    }

    @Activate
    public void activate() {
        store.setDelegate(this::sendTo);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.setDelegate(null);
        log.info("Stopped");
    }

    @Override
    public boolean isKnown(IpAddress addr) {
        checkPermission(PACKET_READ);

        checkNotNull(addr, MAC_ADDR_NULL);
        Set<Host> hosts = hostService.getHostsByIp(addr);
        return !hosts.isEmpty();
    }

    @Override
    public void reply(Ethernet eth, ConnectPoint inPort) {
        checkPermission(PACKET_WRITE);

        checkNotNull(eth, REQUEST_NULL);

        MessageContext context = createContext(eth, inPort);
        if (context != null) {
            replyInternal(context);
        }
    }

    /**
     * Handles a request message.
     *
     * If the MAC address of the target is known, we can reply directly to the
     * requestor. Otherwise, we forward the request out other ports in an
     * attempt to find the correct host.
     *
     * @param context request message context to process
     */
    private void replyInternal(MessageContext context) {
        checkNotNull(context);
        checkArgument(context.type() == MessageType.REQUEST, MSG_NOT_REQUEST);

        if (hasIpAddress(context.inPort())) {
            // If the request came from outside the network, only reply if it was
            // for one of our external addresses.

            interfaceService.getInterfacesByPort(context.inPort())
                    .stream()
                    .filter(intf -> intf.ipAddresses()
                            .stream()
                            .anyMatch(ia -> ia.ipAddress().equals(context.target())))
                    .forEach(intf -> buildAndSendReply(context, intf.mac()));

            // Stop here and don't proxy ARPs if the port has an IP address
            return;
        }

        // See if we have the target host in the host store
        Set<Host> hosts = hostService.getHostsByIp(context.target());

        Host dst = null;
        Host src = hostService.getHost(hostId(context.srcMac(), context.vlan()));

        for (Host host : hosts) {
            if (host.vlan().equals(context.vlan())) {
                dst = host;
                break;
            }
        }

        if (src != null && dst != null) {
            // We know the target host so we can respond
            buildAndSendReply(context, dst.mac());
            return;
        }

        // If the source address matches one of our external addresses
        // it could be a request from an internal host to an external
        // address. Forward it over to the correct port.
        boolean matched = false;
        Set<Interface> interfaces = interfaceService.getInterfacesByIp(context.sender());
        for (Interface intf : interfaces) {
            if (intf.vlan().equals(context.vlan())) {
                matched = true;
                sendTo(context.packet(), intf.connectPoint());
                break;
            }
        }

        if (matched) {
            return;
        }

        // If the packets has a vlanId look if there are some other
        // interfaces in the configuration on the same vlan and broadcast
        // the packet out just of through those interfaces.
        VlanId vlanId = context.vlan();

        Set<Interface> filteredVlanInterfaces =
                filterVlanInterfacesNoIp(interfaceService.getInterfacesByVlan(vlanId));

        if (vlanId != null
        && !vlanId.equals(VlanId.NONE)
        && confContainsVlans(vlanId, context.inPort())) {
            vlanFlood(context.packet(), filteredVlanInterfaces, context.inPort);
            return;
        }

        // The request couldn't be resolved.
        // Flood the request on all ports except the incoming port.
        flood(context.packet(), context.inPort());
    }

    private Set<Interface> filterVlanInterfacesNoIp(Set<Interface> vlanInterfaces) {
        return vlanInterfaces
                .stream()
                .filter(intf -> intf.ipAddresses().isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * States if the interface configuration contains more than one interface configured
     * on a specific vlan, including the interface passed as argument.
     *
     * @param vlanId the vlanid to look for in the interface configuration
     * @param connectPoint the connect point to exclude from the search
     * @return true if interfaces are found. False otherwise
     */
    private boolean confContainsVlans(VlanId vlanId, ConnectPoint connectPoint) {
        Set<Interface> vlanInterfaces = interfaceService.getInterfacesByVlan(vlanId);
        return interfaceService.getInterfacesByVlan(vlanId)
                .stream()
                .anyMatch(intf -> intf.connectPoint().equals(connectPoint) && intf.ipAddresses().isEmpty())
                && vlanInterfaces.size() > 1;
    }

    /**
     * Builds and sends a reply message given a request context and the resolved
     * MAC address to answer with.
     *
     * @param context message context of request
     * @param targetMac MAC address to be given in the response
     */
    private void buildAndSendReply(MessageContext context, MacAddress targetMac) {
        switch (context.protocol()) {
        case ARP:
            sendTo(ARP.buildArpReply((Ip4Address) context.target(),
                    targetMac, context.packet()), context.inPort());
            break;
        case NDP:
            sendTo(buildNdpReply((Ip6Address) context.target(), targetMac,
                    context.packet()), context.inPort());
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
        sendTo(outPort, ByteBuffer.wrap(packet.serialize()));
    }

    /**
     * Outputs a packet out a specific port.
     *
     * @param outPort port to send it out
     * @param packet packet to send
     */
    private void sendTo(ConnectPoint outPort, ByteBuffer packet) {
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
     * Returns whether the given port has any IP addresses configured or not.
     *
     * @param connectPoint the port to check
     * @return true if the port has at least one IP address configured,
     * false otherwise
     */
    private boolean hasIpAddress(ConnectPoint connectPoint) {
        return interfaceService.getInterfacesByPort(connectPoint)
                .stream()
                .flatMap(intf -> intf.ipAddresses().stream())
                .findAny()
                .isPresent();
    }

    /**
     * Returns whether the given port has any VLAN configured or not.
     *
     * @param connectPoint the port to check
     * @return true if the port has at least one VLAN configured,
     * false otherwise
     */
    private boolean hasVlan(ConnectPoint connectPoint) {
        return interfaceService.getInterfacesByPort(connectPoint)
                .stream()
                .filter(intf -> !intf.vlan().equals(VlanId.NONE))
                .findAny()
                .isPresent();
    }

    @Override
    public void forward(Ethernet eth, ConnectPoint inPort) {
        checkPermission(PACKET_WRITE);

        checkNotNull(eth, REQUEST_NULL);

        Host h = hostService.getHost(hostId(eth.getDestinationMAC(),
                vlanId(eth.getVlanID())));

        if (h == null) {
            flood(eth, inPort);
        } else {
            Host subject = hostService.getHost(hostId(eth.getSourceMAC(),
                                                      vlanId(eth.getVlanID())));
            store.forward(h.location(), subject, ByteBuffer.wrap(eth.serialize()));
        }
    }

    @Override
    public boolean handlePacket(PacketContext context) {
        checkPermission(PACKET_WRITE);

        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();

        if (ethPkt == null) {
            return false;
        }

        MessageContext msgContext = createContext(ethPkt, pkt.receivedFrom());

        if (msgContext == null) {
            return false;
        }

        switch (msgContext.type()) {
        case REPLY:
            forward(msgContext.packet(), msgContext.inPort());
            break;
        case REQUEST:
            replyInternal(msgContext);
            break;
        default:
            return false;
        }

        context.block();
        return true;
    }

    /**
     * Flood the arp request at all edges on a specifc VLAN.
     *
     * @param request the arp request
     * @param dsts the destination interfaces
     * @param inPort the connect point the arp request was received on
     */
    private void vlanFlood(Ethernet request, Set<Interface> dsts, ConnectPoint inPort) {
        TrafficTreatment.Builder builder = null;
        ByteBuffer buf = ByteBuffer.wrap(request.serialize());

        for (Interface intf : dsts) {
            ConnectPoint cPoint = intf.connectPoint();
            if (cPoint.equals(inPort)) {
                continue;
            }

            builder = DefaultTrafficTreatment.builder();
            builder.setOutput(cPoint.port());
            packetService.emit(new DefaultOutboundPacket(cPoint.deviceId(),
                    builder.build(), buf));
        }
    }

    /**
     * Flood the arp request at all edges in the network.
     *
     * @param request the arp request
     * @param inPort  the connect point the arp request was received on
     */
    private void flood(Ethernet request, ConnectPoint inPort) {
        TrafficTreatment.Builder builder = null;
        ByteBuffer buf = ByteBuffer.wrap(request.serialize());

        for (ConnectPoint connectPoint : edgeService.getEdgePoints()) {
            if (hasIpAddress(connectPoint)
             || hasVlan(connectPoint)
             || connectPoint.equals(inPort)) {
                continue;
            }

            builder = DefaultTrafficTreatment.builder();
            builder.setOutput(connectPoint.port());
            packetService.emit(new DefaultOutboundPacket(connectPoint.deviceId(),
                                                         builder.build(), buf));
        }
    }

    /**
     * Builds an Neighbor Discovery reply based on a request.
     *
     * @param srcIp   the IP address to use as the reply source
     * @param srcMac  the MAC address to use as the reply source
     * @param request the Neighbor Solicitation request we got
     * @return an Ethernet frame containing the Neighbor Advertisement reply
     */
    private Ethernet buildNdpReply(Ip6Address srcIp, MacAddress srcMac,
                                   Ethernet request) {
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
        nadv.addOption(NeighborDiscoveryOptions.TYPE_TARGET_LL_ADDRESS,
                       srcMac.toBytes());

        icmp6.setPayload(nadv);
        ipv6.setPayload(icmp6);
        eth.setPayload(ipv6);
        return eth;
    }

    /**
     * Attempts to create a MessageContext for the given Ethernet frame. If the
     * frame is a valid ARP or NDP request or response, a context will be
     * created.
     *
     * @param eth input Ethernet frame
     * @param inPort in port
     * @return MessageContext if the packet was ARP or NDP, otherwise null
     */
    private MessageContext createContext(Ethernet eth, ConnectPoint inPort) {
        if (eth.getEtherType() == Ethernet.TYPE_ARP) {
            return createArpContext(eth, inPort);
        } else if (eth.getEtherType() == Ethernet.TYPE_IPV6) {
            return createNdpContext(eth, inPort);
        }

        return null;
    }

    /**
     * Extracts context information from ARP packets.
     *
     * @param eth input Ethernet frame that is thought to be ARP
     * @param inPort in port
     * @return MessageContext object if the packet was a valid ARP packet,
     * otherwise null
     */
    private MessageContext createArpContext(Ethernet eth, ConnectPoint inPort) {
        if (eth.getEtherType() != Ethernet.TYPE_ARP) {
            return null;
        }

        ARP arp = (ARP) eth.getPayload();

        IpAddress target = Ip4Address.valueOf(arp.getTargetProtocolAddress());
        IpAddress sender = Ip4Address.valueOf(arp.getSenderProtocolAddress());

        MessageType type;
        if (arp.getOpCode() == ARP.OP_REQUEST) {
            type = MessageType.REQUEST;
        } else if (arp.getOpCode() == ARP.OP_REPLY) {
            type = MessageType.REPLY;
        } else {
            return null;
        }

        return new MessageContext(eth, inPort, Protocol.ARP, type, target, sender);
    }

    /**
     * Extracts context information from NDP packets.
     *
     * @param eth input Ethernet frame that is thought to be NDP
     * @param inPort in port
     * @return MessageContext object if the packet was a valid NDP packet,
     * otherwise null
     */
    private MessageContext createNdpContext(Ethernet eth, ConnectPoint inPort) {
        if (eth.getEtherType() != Ethernet.TYPE_IPV6) {
            return null;
        }
        IPv6 ipv6 = (IPv6) eth.getPayload();

        if (ipv6.getNextHeader() != IPv6.PROTOCOL_ICMP6) {
            return null;
        }
        ICMP6 icmpv6 = (ICMP6) ipv6.getPayload();

        IpAddress sender = Ip6Address.valueOf(ipv6.getSourceAddress());
        IpAddress target = null;

        MessageType type;
        if (icmpv6.getIcmpType() == ICMP6.NEIGHBOR_SOLICITATION) {
            type = MessageType.REQUEST;
            NeighborSolicitation nsol = (NeighborSolicitation) icmpv6.getPayload();
            target = Ip6Address.valueOf(nsol.getTargetAddress());
        } else if (icmpv6.getIcmpType() == ICMP6.NEIGHBOR_ADVERTISEMENT) {
            type = MessageType.REPLY;
        } else {
            return null;
        }

        return new MessageContext(eth, inPort, Protocol.NDP, type, target, sender);
    }

    /**
     * Provides context information for a particular ARP or NDP message, with
     * a unified interface to access data regardless of protocol.
     */
    private class MessageContext {
        private Protocol protocol;
        private MessageType type;

        private IpAddress target;
        private IpAddress sender;

        private Ethernet eth;
        private ConnectPoint inPort;


        public MessageContext(Ethernet eth, ConnectPoint inPort,
                              Protocol protocol, MessageType type,
                              IpAddress target, IpAddress sender) {
            this.eth = eth;
            this.inPort = inPort;
            this.protocol = protocol;
            this.type = type;
            this.target = target;
            this.sender = sender;
        }

        public ConnectPoint inPort() {
            return inPort;
        }

        public Ethernet packet() {
            return eth;
        }

        public Protocol protocol() {
            return protocol;
        }

        public MessageType type() {
            return type;
        }

        public VlanId vlan() {
            return VlanId.vlanId(eth.getVlanID());
        }

        public MacAddress srcMac() {
            return MacAddress.valueOf(eth.getSourceMACAddress());
        }

        public IpAddress target() {
            return target;
        }

        public IpAddress sender() {
            return sender;
        }
    }
}
