/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import org.onosproject.core.Permission;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.host.PortAddresses;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.proxyarp.ProxyArpService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.slf4j.LoggerFactory.getLogger;


@Component(immediate = true)
@Service
public class ProxyArpManager implements ProxyArpService {

    private final Logger log = getLogger(getClass());

    private static final String MAC_ADDR_NULL = "Mac address cannot be null.";
    private static final String REQUEST_NULL = "ARP or NDP request cannot be null.";
    private static final String REQUEST_NOT_ARP = "Ethernet frame does not contain ARP request.";
    private static final String NOT_ARP_REQUEST = "ARP is not a request.";
    private static final String NOT_ARP_REPLY = "ARP is not a reply.";

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

    /**
     * Listens to both device service and link service to determine
     * whether a port is internal or external.
     */
    @Activate
    public void activate() {
        log.info("Started");
    }


    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public boolean isKnown(IpAddress addr) {
        checkPermission(Permission.PACKET_READ);

        checkNotNull(addr, MAC_ADDR_NULL);
        Set<Host> hosts = hostService.getHostsByIp(addr);
        return !hosts.isEmpty();
    }

    @Override
    public void reply(Ethernet eth, ConnectPoint inPort) {
        checkPermission(Permission.PACKET_WRITE);

        checkNotNull(eth, REQUEST_NULL);

        if (eth.getEtherType() == Ethernet.TYPE_ARP) {
            replyArp(eth, inPort);
        } else if (eth.getEtherType() == Ethernet.TYPE_IPV6) {
            replyNdp(eth, inPort);
        }
    }

    private void replyArp(Ethernet eth, ConnectPoint inPort) {
        ARP arp = (ARP) eth.getPayload();
        checkArgument(arp.getOpCode() == ARP.OP_REQUEST, NOT_ARP_REQUEST);
        checkNotNull(inPort);
        Ip4Address targetAddress = Ip4Address.valueOf(arp.getTargetProtocolAddress());

        VlanId vlan = VlanId.vlanId(eth.getVlanID());

        if (isOutsidePort(inPort)) {
            // If the request came from outside the network, only reply if it was
            // for one of our external addresses.
            Set<PortAddresses> addressSet =
                    hostService.getAddressBindingsForPort(inPort);

            for (PortAddresses addresses : addressSet) {
                for (InterfaceIpAddress ia : addresses.ipAddresses()) {
                    if (ia.ipAddress().equals(targetAddress)) {
                        Ethernet arpReply =
                                ARP.buildArpReply(targetAddress, addresses.mac(), eth);
                        sendTo(arpReply, inPort);
                    }
                }
            }
            return;
        }

        // See if we have the target host in the host store

        Set<Host> hosts = hostService.getHostsByIp(targetAddress);

        Host dst = null;
        Host src = hostService.getHost(HostId.hostId(eth.getSourceMAC(),
                VlanId.vlanId(eth.getVlanID())));

        for (Host host : hosts) {
            if (host.vlan().equals(vlan)) {
                dst = host;
                break;
            }
        }

        if (src != null && dst != null) {
            // We know the target host so we can respond
            Ethernet arpReply = ARP.buildArpReply(targetAddress, dst.mac(), eth);
            sendTo(arpReply, inPort);
            return;
        }

        // If the source address matches one of our external addresses
        // it could be a request from an internal host to an external
        // address. Forward it over to the correct port.
        Ip4Address source =
                Ip4Address.valueOf(arp.getSenderProtocolAddress());
        Set<PortAddresses> sourceAddresses = findPortsInSubnet(source);
        boolean matched = false;
        for (PortAddresses pa : sourceAddresses) {
            for (InterfaceIpAddress ia : pa.ipAddresses()) {
                if (ia.ipAddress().equals(source) &&
                        pa.vlan().equals(vlan)) {
                    matched = true;
                    sendTo(eth, pa.connectPoint());
                    break;
                }
            }
        }

        if (matched) {
            return;
        }

        //
        // The request couldn't be resolved.
        // Flood the request on all ports except the incoming port.
        //
        flood(eth, inPort);
        return;
    }

    private void replyNdp(Ethernet eth, ConnectPoint inPort) {

        IPv6 ipv6 = (IPv6) eth.getPayload();
        ICMP6 icmpv6 = (ICMP6) ipv6.getPayload();
        NeighborSolicitation nsol = (NeighborSolicitation) icmpv6.getPayload();
        Ip6Address targetAddress = Ip6Address.valueOf(nsol.getTargetAddress());

        VlanId vlan = VlanId.vlanId(eth.getVlanID());

        // If the request came from outside the network, only reply if it was
        // for one of our external addresses.
        if (isOutsidePort(inPort)) {
            Set<PortAddresses> addressSet =
                    hostService.getAddressBindingsForPort(inPort);

            for (PortAddresses addresses : addressSet) {
                for (InterfaceIpAddress ia : addresses.ipAddresses()) {
                    if (ia.ipAddress().equals(targetAddress)) {
                        Ethernet ndpReply =
                                buildNdpReply(targetAddress, addresses.mac(), eth);
                        sendTo(ndpReply, inPort);
                    }
                }
            }
            return;
        } else {
            // If the source address matches one of our external addresses
            // it could be a request from an internal host to an external
            // address. Forward it over to the correct ports.
            Ip6Address source =
                    Ip6Address.valueOf(ipv6.getSourceAddress());
            Set<PortAddresses> sourceAddresses = findPortsInSubnet(source);
            boolean matched = false;
            for (PortAddresses pa : sourceAddresses) {
                for (InterfaceIpAddress ia : pa.ipAddresses()) {
                    if (ia.ipAddress().equals(source) &&
                            pa.vlan().equals(vlan)) {
                        matched = true;
                        sendTo(eth, pa.connectPoint());
                        break;
                    }
                }
            }

            if (matched) {
                return;
            }
        }

        // Continue with normal proxy ARP case

        Set<Host> hosts = hostService.getHostsByIp(targetAddress);

        Host dst = null;
        Host src = hostService.getHost(HostId.hostId(eth.getSourceMAC(),
                VlanId.vlanId(eth.getVlanID())));

        for (Host host : hosts) {
            if (host.vlan().equals(vlan)) {
                dst = host;
                break;
            }
        }

        if (src == null || dst == null) {
            //
            // The request couldn't be resolved.
            // Flood the request on all ports except the incoming ports.
            //
            flood(eth, inPort);
            return;
        }

        //
        // Reply on the port the request was received on
        //
        Ethernet ndpReply = buildNdpReply(targetAddress, dst.mac(), eth);
        sendTo(ndpReply, inPort);
    }
    //TODO checkpoint

    /**
     * Outputs the given packet out the given port.
     *
     * @param packet  the packet to send
     * @param outPort the port to send it out
     */
    private void sendTo(Ethernet packet, ConnectPoint outPort) {
        if (!edgeService.isEdgePoint(outPort)) {
            // Sanity check to make sure we don't send the packet out an
            // internal port and create a loop (could happen due to
            // misconfiguration).
            return;
        }

        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        builder.setOutput(outPort.port());
        packetService.emit(new DefaultOutboundPacket(outPort.deviceId(),
                builder.build(), ByteBuffer.wrap(packet.serialize())));
    }

    /**
     * Finds ports with an address in the subnet of the target address.
     *
     * @param target the target address to find a matching port for
     * @return a set of PortAddresses describing ports in the subnet
     */
    private Set<PortAddresses> findPortsInSubnet(IpAddress target) {
        Set<PortAddresses> result = new HashSet<>();
        for (PortAddresses addresses : hostService.getAddressBindings()) {
            result.addAll(addresses.ipAddresses().stream().filter(ia -> ia.subnetAddress().contains(target)).
                    map(ia -> addresses).collect(Collectors.toList()));
        }
        return result;
    }

    /**
     * Returns whether the given port is an outside-facing port with an IP
     * address configured.
     *
     * @param port the port to check
     * @return true if the port is an outside-facing port, otherwise false
     */
    private boolean isOutsidePort(ConnectPoint port) {
        //
        // TODO: Is this sufficient to identify outside-facing ports: just
        // having IP addresses on a port?
        //
        return !hostService.getAddressBindingsForPort(port).isEmpty();
    }

    @Override
    public void forward(Ethernet eth, ConnectPoint inPort) {
        checkPermission(Permission.PACKET_WRITE);

        checkNotNull(eth, REQUEST_NULL);

        Host h = hostService.getHost(HostId.hostId(eth.getDestinationMAC(),
                VlanId.vlanId(eth.getVlanID())));

        if (h == null) {
            flood(eth, inPort);
        } else {
            TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
            builder.setOutput(h.location().port());
            packetService.emit(new DefaultOutboundPacket(h.location().deviceId(),
                    builder.build(), ByteBuffer.wrap(eth.serialize())));
        }

    }

    @Override
    public boolean handlePacket(PacketContext context) {
        checkPermission(Permission.PACKET_WRITE);

        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();

        if (ethPkt == null) {
            return false;
        }
        if (ethPkt.getEtherType() == Ethernet.TYPE_ARP) {
            return handleArp(context, ethPkt);
        } else if (ethPkt.getEtherType() == Ethernet.TYPE_IPV6) {
            return handleNdp(context, ethPkt);
        }
        return false;
    }

    private boolean handleArp(PacketContext context, Ethernet ethPkt) {
        ARP arp = (ARP) ethPkt.getPayload();

        if (arp.getOpCode() == ARP.OP_REPLY) {
            forward(ethPkt, context.inPacket().receivedFrom());
        } else if (arp.getOpCode() == ARP.OP_REQUEST) {
            reply(ethPkt, context.inPacket().receivedFrom());
        } else {
            return false;
        }
        context.block();
        return true;
    }

    private boolean handleNdp(PacketContext context, Ethernet ethPkt) {
        IPv6 ipv6 = (IPv6) ethPkt.getPayload();

        if (ipv6.getNextHeader() != IPv6.PROTOCOL_ICMP6) {
            return false;
        }
        ICMP6 icmpv6 = (ICMP6) ipv6.getPayload();
        if (icmpv6.getIcmpType() == ICMP6.NEIGHBOR_ADVERTISEMENT) {
            forward(ethPkt, context.inPacket().receivedFrom());
        } else if (icmpv6.getIcmpType() == ICMP6.NEIGHBOR_SOLICITATION) {
            reply(ethPkt, context.inPacket().receivedFrom());
        } else {
            return false;
        }
        context.block();
        return true;
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
            if (isOutsidePort(connectPoint) || connectPoint.equals(inPort)) {
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
}
