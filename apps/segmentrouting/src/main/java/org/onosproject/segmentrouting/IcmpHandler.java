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
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MPLS;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.neighbour.NeighbourMessageContext;
import org.onosproject.incubator.net.neighbour.NeighbourMessageType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handler of ICMP packets that responses or forwards ICMP packets that
 * are sent to the controller.
 */
public class IcmpHandler {

    private static Logger log = LoggerFactory.getLogger(IcmpHandler.class);
    private SegmentRoutingManager srManager;
    private DeviceConfiguration config;

    /**
     * Creates an IcmpHandler object.
     *
     * @param srManager SegmentRoutingManager object
     */
    public IcmpHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.config = checkNotNull(srManager.deviceConfiguration);
    }

    //////////////////////////////////////
    //     ICMP Echo/Reply Protocol     //
    //////////////////////////////////////

    /**
     * Process incoming ICMP packet.
     * If it is an ICMP request to router or known host, then sends an ICMP response.
     * If it is an ICMP packet to known host and forward the packet to the host.
     * If it is an ICMP packet to unknown host in a subnet, then sends an ARP request
     * to the subnet.
     *
     * @param pkt inbound packet
     */
    public void processPacketIn(InboundPacket pkt) {

        Ethernet ethernet = pkt.parsed();
        IPv4 ipv4 = (IPv4) ethernet.getPayload();

        ConnectPoint connectPoint = pkt.receivedFrom();
        DeviceId deviceId = connectPoint.deviceId();
        Ip4Address destinationAddress =
                Ip4Address.valueOf(ipv4.getDestinationAddress());
        Set<IpAddress> gatewayIpAddresses = config.getPortIPs(deviceId);
        IpAddress routerIp;
        try {
            routerIp = config.getRouterIpv4(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting processPacketIn.");
            return;
        }
        IpPrefix routerIpPrefix = IpPrefix.valueOf(routerIp, IpPrefix.MAX_INET_MASK_LENGTH);
        Ip4Address routerIpAddress = routerIpPrefix.getIp4Prefix().address();

        // ICMP to the router IP or gateway IP
        if (((ICMP) ipv4.getPayload()).getIcmpType() == ICMP.TYPE_ECHO_REQUEST &&
                (destinationAddress.equals(routerIpAddress) ||
                        gatewayIpAddresses.contains(destinationAddress))) {
            sendIcmpResponse(ethernet, connectPoint);

        // ICMP for any known host
        } else if (!srManager.hostService.getHostsByIp(destinationAddress).isEmpty()) {
            // TODO: known host packet should not be coming to controller - resend flows?
            srManager.ipHandler.forwardPackets(deviceId, destinationAddress);

        // ICMP for an unknown host in the subnet of the router
        } else if (config.inSameSubnet(deviceId, destinationAddress)) {
            srManager.arpHandler.sendArpRequest(deviceId, destinationAddress, connectPoint);

        // ICMP for an unknown host
        } else {
            log.debug("ICMP request for unknown host {} ", destinationAddress);
            // Do nothing
        }
    }

    /**
     * Sends an ICMP reply message.
     *
     * Note: we assume that packets sending from the edge switches to the hosts
     * have untagged VLAN.
     * @param icmpRequest the original ICMP request
     * @param outport the output port where the ICMP reply should be sent to
     */
    private void sendIcmpResponse(Ethernet icmpRequest, ConnectPoint outport) {
        // Note: We assume that packets arrive at the edge switches have
        // untagged VLAN.
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

        Ip4Address destIpAddress = Ip4Address.valueOf(icmpReplyIpv4.getDestinationAddress());
        Ip4Address destRouterAddress = config.getRouterIpAddressForASubnetHost(destIpAddress);
        int destSid = config.getIPv4SegmentId(destRouterAddress);
        if (destSid < 0) {
            log.warn("Cannot find the Segment ID for {}", destAddress);
            return;
        }

        sendPacketOut(outport, icmpReplyEth, destSid);

    }

    private void sendPacketOut(ConnectPoint outport, Ethernet payload, int destSid) {

        IPv4 ipPacket = (IPv4) payload.getPayload();
        Ip4Address destIpAddress = Ip4Address.valueOf(ipPacket.getDestinationAddress());

        if (destSid == -1 || config.getIPv4SegmentId(payload.getDestinationMAC()) == destSid ||
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
            mplsPkt.setLabel(destSid);
            mplsPkt.setTtl(((IPv4) payload.getPayload()).getTtl());
            mplsPkt.setPayload(payload.getPayload());
            payload.setPayload(mplsPkt);

            OutboundPacket packet = new DefaultOutboundPacket(outport.deviceId(),
                    treatment, ByteBuffer.wrap(payload.serialize()));

            srManager.packetService.emit(packet);
        }
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
        /*
         * First we validate the ndp packet
         */
        SegmentRoutingAppConfig appConfig = srManager.cfgService
                .getConfig(srManager.appId, SegmentRoutingAppConfig.class);
        if (appConfig != null && appConfig.suppressSubnet().contains(pkt.inPort())) {
            // Ignore NDP packets come from suppressed ports
            pkt.drop();
            return;
        }
        if (!validateSrcIp(pkt)) {
            log.debug("Ignore NDP packet discovered on {} with unexpected src ip address {}.",
                      pkt.inPort(), pkt.sender());
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
     * Utility function to verify if the src ip belongs to the same
     * subnet configured on the port it is seen.
     *
     * @param pkt the ndp packet and context information
     * @return true if the src ip is a valid address for the subnet configured
     * for the connect point
     */
    private boolean validateSrcIp(NeighbourMessageContext pkt) {
        ConnectPoint connectPoint = pkt.inPort();
        IpPrefix subnet = config.getPortIPv6Subnet(
                connectPoint.deviceId(),
                connectPoint.port()
        ).getIp6Prefix();
        return subnet != null && subnet.contains(pkt.sender());
    }

    /**
     * Helper method to handle the ndp requests.
     *
     * @param pkt the ndp packet request and context information
     * @param hostService the host service
     */
    private void handleNdpRequest(NeighbourMessageContext pkt, HostService hostService) {
        /*
         * ND request for the gateway. We have to reply on behalf
         * of the gateway.
         */
        if (isNdpForGateway(pkt)) {
            log.debug("Sending NDP reply on behalf of the router");
            sendNdpReply(pkt, config.getRouterMacForAGatewayIp(pkt.target()));
        } else {
            /*
             * ND request for an host. We do a search by Ip.
             */
            Set<Host> hosts = hostService.getHostsByIp(pkt.target());
            /*
             * Possible misconfiguration ? In future this case
             * should be handled we can have same hosts in different
             * vlans.
             */
            if (hosts.size() > 1) {
                log.warn("More than one host with IP {}", pkt.target());
            }
            Host targetHost = hosts.stream().findFirst().orElse(null);
            /*
             * If we know the host forward to its attachment
             * point.
             */
            if (targetHost != null) {
                log.debug("Forward NDP request to the target host");
                pkt.forward(targetHost.location());
            } else {
                /*
                 * Flood otherwise.
                 */
                log.debug("Flood NDP request to the target subnet");
                flood(pkt);
            }
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
            HostId hostId = HostId.hostId(pkt.dstMac(), pkt.vlan());
            Host targetHost = hostService.getHost(hostId);
            if (targetHost != null) {
                log.debug("Forwarding the reply to the host");
                pkt.forward(targetHost.location());
            } else {
                /*
                 * We don't have to flood towards spine facing ports.
                 */
                if (pkt.vlan().equals(VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET))) {
                    return;
                }
                log.debug("Flooding the reply to the subnet");
                flood(pkt);
            }
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
        if (gatewayIpAddresses != null &&
                gatewayIpAddresses.contains(pkt.target())) {
            return true;
        }
        return false;
    }

    /**
     * Utility to send a ND reply using the supplied information.
     *
     * @param pkt the ndp request
     * @param targetMac the target mac
     */
    private void sendNdpReply(NeighbourMessageContext pkt, MacAddress targetMac) {
        HostId dstId = HostId.hostId(pkt.srcMac(), pkt.vlan());
        Host dst = srManager.hostService.getHost(dstId);
        if (dst == null) {
            log.warn("Cannot send NDP response to host {} - does not exist in the store",
                     dstId);
            return;
        }
        pkt.reply(targetMac);
    }

    /*
     * Floods only on the port which have been configured with the subnet
     * of the target address. The in port is excluded.
     *
     * @param pkt the ndp packet and context information
     */
    private void flood(NeighbourMessageContext pkt) {
        try {
            srManager.deviceConfiguration
                    .getSubnetPortsMap(pkt.inPort().deviceId())
                    .forEach((subnet, ports) -> {
                        if (subnet.contains(pkt.target())) {
                            ports.stream()
                                    .filter(portNumber -> portNumber != pkt.inPort().port())
                                    .forEach(portNumber -> {
                                        ConnectPoint outPoint = new ConnectPoint(
                                                pkt.inPort().deviceId(),
                                                portNumber
                                        );
                                        pkt.forward(outPoint);
                                    });
                        }
                    });
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage()
                             + " Cannot flood in subnet as device config not available"
                             + " for device: " + pkt.inPort().deviceId());
        }
    }
}
