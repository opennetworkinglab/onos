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
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MPLS;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
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
        Set<Ip4Address> gatewayIpAddresses = config.getPortIPs(deviceId);
        Ip4Address routerIp;
        try {
            routerIp = config.getRouterIp(deviceId);
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
            sendICMPResponse(ethernet, connectPoint);

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
    private void sendICMPResponse(Ethernet icmpRequest, ConnectPoint outport) {
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
        int sid = config.getSegmentId(destRouterAddress);
        if (sid < 0) {
            log.warn("Cannot find the Segment ID for {}", destAddress);
            return;
        }

        sendPacketOut(outport, icmpReplyEth, sid);

    }

    private void sendPacketOut(ConnectPoint outport, Ethernet payload, int sid) {

        IPv4 ipPacket = (IPv4) payload.getPayload();
        Ip4Address destIpAddress = Ip4Address.valueOf(ipPacket.getDestinationAddress());

        if (sid == -1 || config.getSegmentId(payload.getDestinationMAC()) == sid ||
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
            mplsPkt.setTtl(((IPv4) payload.getPayload()).getTtl());
            mplsPkt.setPayload(payload.getPayload());
            payload.setPayload(mplsPkt);

            OutboundPacket packet = new DefaultOutboundPacket(outport.deviceId(),
                    treatment, ByteBuffer.wrap(payload.serialize()));

            srManager.packetService.emit(packet);
        }
    }


}
