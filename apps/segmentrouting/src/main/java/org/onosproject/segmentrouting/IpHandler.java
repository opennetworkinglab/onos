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
import org.onlab.packet.IP;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.IpAddress.Version.INET6;

/**
 * Handler of IP packets that forwards IP packets that are sent to the controller,
 * except the ICMP packets which are processed by @link{IcmpHandler}.
 */
public class IpHandler {

    private static Logger log = LoggerFactory.getLogger(IpHandler.class);
    private SegmentRoutingManager srManager;
    private DeviceConfiguration config;
    private ConcurrentHashMap<IpAddress, ConcurrentLinkedQueue<IP>> ipPacketQueue;

    /**
     * Creates an IpHandler object.
     *
     * @param srManager SegmentRoutingManager object
     */
    public IpHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.config = checkNotNull(srManager.deviceConfiguration);
        ipPacketQueue = new ConcurrentHashMap<>();
    }

    /**
     * Enqueues the packet using the destination address as key.
     *
     * @param ipPacket the ip packet to store
     * @param destinationAddress the destination address
     */
    private void enqueuePacket(IP ipPacket, IpAddress destinationAddress) {

        ipPacketQueue
            .computeIfAbsent(destinationAddress, a -> new ConcurrentLinkedQueue<>())
            .add(ipPacket);

    }

    /**
     * Dequeues the packet using the destination address as key.
     *
     * @param ipPacket the ip packet to remove
     * @param destinationAddress the destination address
     */
    public void dequeuePacket(IP ipPacket, IpAddress destinationAddress) {

        if (ipPacketQueue.get(destinationAddress) == null) {
            return;
        }
        ipPacketQueue.get(destinationAddress).remove(ipPacket);
    }

    /**
     * Forwards the packet to a given host and deque the packet.
     *
     * @param deviceId the target device
     * @param eth the packet to send
     * @param dest the target host
     */
    private void forwardToHost(DeviceId deviceId, Ethernet eth, Host dest) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().
                setOutput(dest.location().port()).build();
        OutboundPacket packet = new DefaultOutboundPacket(deviceId,
                                                          treatment, ByteBuffer.wrap(eth.serialize()));
        srManager.packetService.emit(packet);
    }

    //////////////////////
    //  IPv4 Handling  //
    ////////////////////

    /**
     * Processes incoming IP packets.
     *
     * If it is an IP packet for known host, then forward it to the host.
     * If it is an IP packet for unknown host in subnet, then send an ARP request
     * to the subnet.
     *
     * @param pkt incoming packet
     * @param connectPoint the target device
     */
    public void processPacketIn(IPv4 pkt, ConnectPoint connectPoint) {

        DeviceId deviceId = connectPoint.deviceId();
        Ip4Address destinationAddress = Ip4Address.valueOf(pkt.getDestinationAddress());

        // IP packet for know hosts
        if (!srManager.hostService.getHostsByIp(destinationAddress).isEmpty()) {
            forwardPackets(deviceId, destinationAddress);

        // IP packet for unknown host in one of the configured subnets of the router
        } else if (config.inSameSubnet(deviceId, destinationAddress)) {
            srManager.arpHandler.sendArpRequest(deviceId, destinationAddress, connectPoint);

        // IP packets for unknown host
        } else {
            log.debug("IPv4 packet for unknown host {} which is not in the subnet",
                    destinationAddress);
            // Do nothing
        }
    }

    /**
     * Adds the IP packet to a buffer.
     * The packets are forwarded to corresponding destination when the destination
     * MAC address is known via ARP response.
     *
     * @param ipPacket IP packet to add to the buffer
     */
    public void addToPacketBuffer(IPv4 ipPacket) {

        // Better not buffer TCP packets due to out-of-order packet transfer
        if (ipPacket.getProtocol() == IPv4.PROTOCOL_TCP) {
            return;
        }
        IpAddress destIpAddress = IpAddress.valueOf(ipPacket.getDestinationAddress());
        enqueuePacket(ipPacket, destIpAddress);
    }

    /**
     * Forwards IP packets in the buffer to the destination IP address.
     * It is called when the controller finds the destination MAC address
     * via ARP responses.
     *
     * @param deviceId switch device ID
     * @param destIpAddress destination IP address
     */
    public void forwardPackets(DeviceId deviceId, Ip4Address destIpAddress) {
        if (ipPacketQueue.get(destIpAddress) == null) {
            return;
        }
        for (IP ipPacket : ipPacketQueue.get(destIpAddress)) {
            if (ipPacket.getVersion() == ((byte) 4)) {
                IPv4 ipv4Packet = (IPv4) ipPacket;
                Ip4Address destAddress = Ip4Address.valueOf(ipv4Packet.getDestinationAddress());
                if (config.inSameSubnet(deviceId, destAddress)) {
                    ipv4Packet.setTtl((byte) (ipv4Packet.getTtl() - 1));
                    ipv4Packet.setChecksum((short) 0);
                    for (Host dest : srManager.hostService.getHostsByIp(destIpAddress)) {
                        Ethernet eth = new Ethernet();
                        eth.setDestinationMACAddress(dest.mac());
                        try {
                            eth.setSourceMACAddress(config.getDeviceMac(deviceId));
                        } catch (DeviceConfigNotFoundException e) {
                            log.warn(e.getMessage()
                                             + " Skipping forwardPackets for this destination.");
                            continue;
                        }
                        eth.setEtherType(Ethernet.TYPE_IPV4);
                        eth.setPayload(ipv4Packet);
                        forwardToHost(deviceId, eth, dest);
                        ipPacketQueue.get(destIpAddress).remove(ipPacket);
                    }
                    ipPacketQueue.get(destIpAddress).remove(ipPacket);
                }
            }
        }
    }

    //////////////////////
    //  IPv6 Handling  //
    ////////////////////

    /**
     * Processes incoming IPv6 packets.
     *
     * If it is an IPv6 packet for known host, then forward it to the host.
     * If it is an IPv6 packet for unknown host in subnet, then send an NDP request
     * to the subnet.
     *
     * @param pkt incoming packet
     * @param connectPoint the target device
     */
    public void processPacketIn(IPv6 pkt, ConnectPoint connectPoint) {

        DeviceId deviceId = connectPoint.deviceId();
        Ip6Address destinationAddress = Ip6Address.valueOf(pkt.getDestinationAddress());

        // IPv6 packet for know hosts
        if (!srManager.hostService.getHostsByIp(destinationAddress).isEmpty()) {
            forwardPackets(deviceId, destinationAddress);

            // IPv6 packet for unknown host in one of the configured subnets of the router
        } else if (config.inSameSubnet(deviceId, destinationAddress)) {
            srManager.icmpHandler.sendNdpRequest(deviceId, destinationAddress, connectPoint);

            // IPv6 packets for unknown host
        } else {
            log.debug("IPv6 packet for unknown host {} which is not in the subnet",
                      destinationAddress);
        }
    }

    /**
     * Adds the IPv6 packet to a buffer.
     * The packets are forwarded to corresponding destination when the destination
     * MAC address is known via NDP response.
     *
     * @param ipPacket IP packet to add to the buffer
     */
    public void addToPacketBuffer(IPv6 ipPacket) {

        // Better not buffer TCP packets due to out-of-order packet transfer
        if (ipPacket.getNextHeader() == IPv6.PROTOCOL_TCP) {
            return;
        }
        IpAddress destIpAddress = IpAddress.valueOf(INET6, ipPacket.getDestinationAddress());
        enqueuePacket(ipPacket, destIpAddress);
    }

    /**
     * Forwards IP packets in the buffer to the destination IP address.
     * It is called when the controller finds the destination MAC address
     * via NDP replies.
     *
     * @param deviceId the target device
     * @param destIpAddress the destination ip address
     */
    public void forwardPackets(DeviceId deviceId, Ip6Address destIpAddress) {
        if (ipPacketQueue.get(destIpAddress) == null) {
            return;
        }
        for (IP ipPacket : ipPacketQueue.get(destIpAddress)) {
            if (ipPacket.getVersion() == ((byte) 6)) {
                IPv6 ipv6Packet = (IPv6) ipPacket;
                Ip6Address destAddress = Ip6Address.valueOf(ipv6Packet.getDestinationAddress());
                if (config.inSameSubnet(deviceId, destAddress)) {
                    ipv6Packet.setHopLimit((byte) (ipv6Packet.getHopLimit() - 1));
                    for (Host dest : srManager.hostService.getHostsByIp(destIpAddress)) {
                        Ethernet eth = new Ethernet();
                        eth.setDestinationMACAddress(dest.mac());
                        try {
                            eth.setSourceMACAddress(config.getDeviceMac(deviceId));
                        } catch (DeviceConfigNotFoundException e) {
                            log.warn(e.getMessage()
                                             + " Skipping forwardPackets for this destination.");
                            continue;
                        }
                        eth.setEtherType(Ethernet.TYPE_IPV6);
                        eth.setPayload(ipv6Packet);
                        forwardToHost(deviceId, eth, dest);
                        ipPacketQueue.get(destIpAddress).remove(ipPacket);
                    }
                    ipPacketQueue.get(destIpAddress).remove(ipPacket);
                }
            }
            ipPacketQueue.get(destIpAddress).remove(ipPacket);
        }
    }

}
