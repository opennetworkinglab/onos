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
package org.onosproject.segmentrouting;

import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MPLS;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.ndp.NeighborSolicitation;
import org.onosproject.incubator.net.neighbour.NeighbourMessageContext;
import org.onosproject.incubator.net.neighbour.NeighbourMessageType;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Handler of ICMP packets that responses or forwards ICMP packets that
 * are sent to the controller.
 */
public class IcmpHandler extends SegmentRoutingNeighbourHandler {

    private static Logger log = LoggerFactory.getLogger(IcmpHandler.class);

    /**
     * Creates an IcmpHandler object.
     *
     * @param srManager SegmentRoutingManager object
     */
    public IcmpHandler(SegmentRoutingManager srManager) {
        super(srManager);
    }

    /**
     * Utility function to send packet out.
     *
     * @param outport the output port
     * @param payload the packet to send
     * @param sid the segment id
     * @param destIpAddress the destination ip address
     * @param allowedHops the hop limit/ttl
     */
    private void sendPacketOut(ConnectPoint outport,
                               Ethernet payload,
                               int sid,
                               IpAddress destIpAddress,
                               byte allowedHops) {
        int destSid;
        if (destIpAddress.isIp4()) {
            destSid = config.getIPv4SegmentId(payload.getDestinationMAC());
        } else {
            destSid = config.getIPv6SegmentId(payload.getDestinationMAC());
        }

        if (sid == -1 || destSid == sid ||
                config.inSameSubnet(outport.deviceId(), destIpAddress)) {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder().
                    setOutput(outport.port()).build();
            OutboundPacket packet = new DefaultOutboundPacket(outport.deviceId(),
                                                              treatment, ByteBuffer.wrap(payload.serialize()));
            srManager.packetService.emit(packet);
        } else {
            log.debug("Send a MPLS packet as a ICMP response");
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(outport.port())
                    .build();

            payload.setEtherType(Ethernet.MPLS_UNICAST);
            MPLS mplsPkt = new MPLS();
            mplsPkt.setLabel(sid);
            mplsPkt.setTtl(allowedHops);
            mplsPkt.setPayload(payload.getPayload());
            payload.setPayload(mplsPkt);

            OutboundPacket packet = new DefaultOutboundPacket(outport.deviceId(),
                                                              treatment, ByteBuffer.wrap(payload.serialize()));

            srManager.packetService.emit(packet);
        }
    }

    //////////////////////////////////////
    //     ICMP Echo/Reply Protocol     //
    //////////////////////////////////////

    /**
     * Process incoming ICMP packet.
     * If it is an ICMP request to router, then sends an ICMP response.
     * Otherwise ignore the packet.
     *
     * @param eth inbound ICMP packet
     * @param inPort the input port
     */
    public void processIcmp(Ethernet eth, ConnectPoint inPort) {
        DeviceId deviceId = inPort.deviceId();
        IPv4 ipv4Packet = (IPv4) eth.getPayload();
        Ip4Address destinationAddress = Ip4Address.valueOf(ipv4Packet.getDestinationAddress());
        Set<IpAddress> gatewayIpAddresses = config.getPortIPs(deviceId);
        IpAddress routerIp;
        try {
            routerIp = config.getRouterIpv4(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting processPacketIn.");
            return;
        }
        // ICMP to the router IP or gateway IP
        if (((ICMP) ipv4Packet.getPayload()).getIcmpType() == ICMP.TYPE_ECHO_REQUEST &&
                (destinationAddress.equals(routerIp.getIp4Address()) ||
                        gatewayIpAddresses.contains(destinationAddress))) {
            sendIcmpResponse(eth, inPort);
        } else {
            log.trace("Ignore ICMP that targets for {}", destinationAddress);
        }
        // We remove the packet from the queue
        srManager.ipHandler.dequeuePacket(ipv4Packet, destinationAddress);
    }

    /**
     * Sends an ICMP reply message.
     *
     * @param icmpRequest the original ICMP request
     * @param outport the output port where the ICMP reply should be sent to
     */
    private void sendIcmpResponse(Ethernet icmpRequest, ConnectPoint outport) {
        Ethernet icmpReplyEth = ICMP.buildIcmpReply(icmpRequest);
        IPv4 icmpRequestIpv4 = (IPv4) icmpRequest.getPayload();
        IPv4 icmpReplyIpv4 = (IPv4) icmpReplyEth.getPayload();
        Ip4Address destIpAddress = Ip4Address.valueOf(icmpRequestIpv4.getSourceAddress());
        Ip4Address destRouterAddress = config.getRouterIpAddressForASubnetHost(destIpAddress);

        // Note: Source IP of the ICMP request doesn't belong to any configured subnet.
        //       The source might be an indirectly attached host (e.g. behind a router)
        //       Lookup the route store for the nexthop instead.
        if (destRouterAddress == null) {
            Optional<ResolvedRoute> nexthop = srManager.routeService.longestPrefixLookup(destIpAddress);
            if (nexthop.isPresent()) {
                try {
                    destRouterAddress = config.getRouterIpv4(nexthop.get().location().deviceId());
                } catch (DeviceConfigNotFoundException e) {
                    log.warn("Device config not found. Abort ICMP processing");
                    return;
                }
            }
        }

        int destSid = config.getIPv4SegmentId(destRouterAddress);
        if (destSid < 0) {
            log.warn("Failed to lookup SID of the switch that {} attaches to. " +
                    "Unable to process ICMP request.", destIpAddress);
            return;
        }
        sendPacketOut(outport, icmpReplyEth, destSid, destIpAddress, icmpReplyIpv4.getTtl());
    }

    ///////////////////////////////////////////
    //      ICMPv6 Echo/Reply Protocol       //
    ///////////////////////////////////////////

    /**
     * Process incoming ICMPv6 packet.
     * If it is an ICMPv6 request to router, then sends an ICMPv6 response.
     * Otherwise ignore the packet.
     *
     * @param eth the incoming ICMPv6 packet
     * @param inPort the input port
     */
    public void processIcmpv6(Ethernet eth, ConnectPoint inPort) {
        DeviceId deviceId = inPort.deviceId();
        IPv6 ipv6Packet = (IPv6) eth.getPayload();
        Ip6Address destinationAddress = Ip6Address.valueOf(ipv6Packet.getDestinationAddress());
        Set<IpAddress> gatewayIpAddresses = config.getPortIPs(deviceId);
        IpAddress routerIp;
        try {
            routerIp = config.getRouterIpv6(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting processPacketIn.");
            return;
        }
        ICMP6 icmp6 = (ICMP6) ipv6Packet.getPayload();
        // ICMP to the router IP or gateway IP
        if (icmp6.getIcmpType()  == ICMP6.ECHO_REQUEST &&
                (destinationAddress.equals(routerIp.getIp6Address()) ||
                        gatewayIpAddresses.contains(destinationAddress))) {
            sendIcmpv6Response(eth, inPort);
        } else {
            log.trace("Ignore ICMPv6 that targets for {}", destinationAddress);
        }
    }

    /**
     * Sends an ICMPv6 reply message.
     *
     * Note: we assume that packets sending from the edge switches to the hosts
     * have untagged VLAN.
     * @param ethRequest the original ICMP request
     * @param outport the output port where the ICMP reply should be sent to
     */
    private void sendIcmpv6Response(Ethernet ethRequest, ConnectPoint outport) {
        // Note: We assume that packets arrive at the edge switches have untagged VLAN.
        Ethernet ethReply = ICMP6.buildIcmp6Reply(ethRequest);
        IPv6 icmpRequestIpv6 = (IPv6) ethRequest.getPayload();
        IPv6 icmpReplyIpv6 = (IPv6) ethRequest.getPayload();
        Ip6Address destIpAddress = Ip6Address.valueOf(icmpRequestIpv6.getSourceAddress());
        Ip6Address destRouterAddress = config.getRouterIpAddressForASubnetHost(destIpAddress);

        // Note: Source IP of the ICMP request doesn't belong to any configured subnet.
        //       The source might be an indirect host behind a router.
        //       Lookup the route store for the nexthop instead.
        if (destRouterAddress == null) {
            Optional<ResolvedRoute> nexthop = srManager.routeService.longestPrefixLookup(destIpAddress);
            if (nexthop.isPresent()) {
                try {
                    destRouterAddress = config.getRouterIpv6(nexthop.get().location().deviceId());
                } catch (DeviceConfigNotFoundException e) {
                    log.warn("Device config not found. Abort ICMPv6 processing");
                    return;
                }
            }
        }

        int sid = config.getIPv6SegmentId(destRouterAddress);
        if (sid < 0) {
            log.warn("Failed to lookup SID of the switch that {} attaches to. " +
                    "Unable to process ICMPv6 request.", destIpAddress);
            return;
        }
        sendPacketOut(outport, ethReply, sid, destIpAddress, icmpReplyIpv6.getHopLimit());
    }

    ///////////////////////////////////////////
    //  ICMPv6 Neighbour Discovery Protocol  //
    ///////////////////////////////////////////

    /**
     * Process incoming NDP packet.
     *
     * If it is an NDP request for the router or for the gateway, then sends a NDP reply.
     * If it is an NDP request to unknown host flood in the subnet.
     * If it is an NDP packet to known host forward the packet to the host.
     *
     * FIXME If the NDP packets use link local addresses we fail.
     *
     * @param pkt inbound packet
     * @param hostService the host service
     */
    public void processPacketIn(NeighbourMessageContext pkt, HostService hostService) {
        // First we validate the ndp packet
        SegmentRoutingAppConfig appConfig = srManager.cfgService
                .getConfig(srManager.appId, SegmentRoutingAppConfig.class);
        if (appConfig != null && appConfig.suppressSubnet().contains(pkt.inPort())) {
            // Ignore NDP packets come from suppressed ports
            pkt.drop();
            return;
        }

        if (pkt.type() == NeighbourMessageType.REQUEST) {
            handleNdpRequest(pkt, hostService);
        } else {
            handleNdpReply(pkt, hostService);
        }

    }

    /**
     * Helper method to handle the ndp requests.
     *
     * @param pkt the ndp packet request and context information
     * @param hostService the host service
     */
    private void handleNdpRequest(NeighbourMessageContext pkt, HostService hostService) {
        // ND request for the gateway. We have to reply on behalf of the gateway.
        if (isNdpForGateway(pkt)) {
            log.trace("Sending NDP reply on behalf of gateway IP for pkt: {}", pkt);
            sendResponse(pkt, config.getRouterMacForAGatewayIp(pkt.target()), hostService);
        } else {
            // NOTE: Ignore NDP packets except those target for the router
            //       We will reconsider enabling this when we have host learning support
            /*
            // ND request for an host. We do a search by Ip.
            Set<Host> hosts = hostService.getHostsByIp(pkt.target());
            // Possible misconfiguration ? In future this case
            // should be handled we can have same hosts in different VLANs.
            if (hosts.size() > 1) {
                log.warn("More than one host with IP {}", pkt.target());
            }
            Host targetHost = hosts.stream().findFirst().orElse(null);
            // If we know the host forward to its attachment point.
            if (targetHost != null) {
                log.debug("Forward NDP request to the target host");
                pkt.forward(targetHost.location());
            } else {
                // Flood otherwise.
                log.debug("Flood NDP request to the target subnet");
                flood(pkt);
            }
            */
        }
    }

    /**
     * Helper method to handle the ndp replies.
     *
     * @param pkt the ndp packet reply and context information
     * @param hostService the host service
     */
    private void handleNdpReply(NeighbourMessageContext pkt, HostService hostService) {
        if (isNdpForGateway(pkt)) {
            log.debug("Forwarding all the ip packets we stored");
            Ip6Address hostIpAddress = pkt.sender().getIp6Address();
            srManager.ipHandler.forwardPackets(pkt.inPort().deviceId(), hostIpAddress);
        } else {
            // NOTE: Ignore NDP packets except those target for the router
            //       We will reconsider enabling this when we have host learning support
            /*
            HostId hostId = HostId.hostId(pkt.dstMac(), pkt.vlan());
            Host targetHost = hostService.getHost(hostId);
            if (targetHost != null) {
                log.debug("Forwarding the reply to the host");
                pkt.forward(targetHost.location());
            } else {
                // We don't have to flood towards spine facing ports.
                if (pkt.vlan().equals(SegmentRoutingManager.INTERNAL_VLAN)) {
                    return;
                }
                log.debug("Flooding the reply to the subnet");
                flood(pkt);
            }
            */
        }
    }

    /**
     * Utility to verify if the ND are for the gateway.
     *
     * @param pkt the ndp packet
     * @return true if the ndp is for the gateway. False otherwise
     */
    private boolean isNdpForGateway(NeighbourMessageContext pkt) {
        DeviceId deviceId = pkt.inPort().deviceId();
        Set<IpAddress> gatewayIpAddresses = null;
        try {
            if (pkt.target().equals(config.getRouterIpv6(deviceId))) {
                return true;
            }
            gatewayIpAddresses = config.getPortIPs(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting check for router IP in processing ndp");
        }

        return gatewayIpAddresses != null && gatewayIpAddresses.stream()
                .filter(IpAddress::isIp6)
                .anyMatch(gatewayIp -> gatewayIp.equals(pkt.target()) ||
                        Arrays.equals(IPv6.getSolicitNodeAddress(gatewayIp.toOctets()),
                                pkt.target().toOctets())
        );
    }

    /**
     * Sends a NDP request for the target IP address to all ports except in-port.
     *
     * @param deviceId Switch device ID
     * @param targetAddress target IP address for ARP
     * @param inPort in-port
     */
    public void sendNdpRequest(DeviceId deviceId, IpAddress targetAddress, ConnectPoint inPort) {
        byte[] senderMacAddress = new byte[MacAddress.MAC_ADDRESS_LENGTH];
        byte[] senderIpAddress = new byte[Ip6Address.BYTE_LENGTH];
        // Retrieves device info.
        if (!getSenderInfo(senderMacAddress, senderIpAddress, deviceId, targetAddress)) {
            log.warn("Aborting sendNdpRequest, we cannot get all the information needed");
            return;
        }
        // We have to compute the dst mac address and dst ip address.
        byte[] dstIp = IPv6.getSolicitNodeAddress(targetAddress.toOctets());
        byte[] dstMac = IPv6.getMCastMacAddress(dstIp);
        // Creates the request.
        Ethernet ndpRequest = NeighborSolicitation.buildNdpSolicit(
                targetAddress.toOctets(),
                senderIpAddress,
                dstIp,
                senderMacAddress,
                dstMac,
                VlanId.NONE
        );
        flood(ndpRequest, inPort, targetAddress);
    }
}
