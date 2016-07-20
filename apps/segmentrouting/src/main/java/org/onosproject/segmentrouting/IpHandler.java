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
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handler of IP packets that forwards IP packets that are sent to the controller,
 * except the ICMP packets which are processed by @link{IcmpHandler}.
 */
public class IpHandler {

    private static Logger log = LoggerFactory.getLogger(IpHandler.class);
    private SegmentRoutingManager srManager;
    private DeviceConfiguration config;
    private ConcurrentHashMap<Ip4Address, ConcurrentLinkedQueue<IPv4>> ipPacketQueue;

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
     * Processes incoming IP packets.
     *
     * If it is an IP packet for known host, then forward it to the host.
     * If it is an IP packet for unknown host in subnet, then send an ARP request
     * to the subnet.
     *
     * @param pkt incoming packet
     */
    public void processPacketIn(InboundPacket pkt) {
        Ethernet ethernet = pkt.parsed();
        IPv4 ipv4 = (IPv4) ethernet.getPayload();

        ConnectPoint connectPoint = pkt.receivedFrom();
        DeviceId deviceId = connectPoint.deviceId();
        Ip4Address destinationAddress =
                Ip4Address.valueOf(ipv4.getDestinationAddress());

        // IP packet for know hosts
        if (!srManager.hostService.getHostsByIp(destinationAddress).isEmpty()) {
            forwardPackets(deviceId, destinationAddress);

        // IP packet for unknown host in the subnet of the router
        } else if (config.inSameSubnet(deviceId, destinationAddress)) {
            srManager.arpHandler.sendArpRequest(deviceId, destinationAddress, connectPoint);

        // IP packets for unknown host
        } else {
            log.debug("ICMP request for unknown host {} which is not in the subnet",
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

        Ip4Address destIpAddress = Ip4Address.valueOf(ipPacket.getDestinationAddress());

        if (ipPacketQueue.get(destIpAddress) == null) {
            ConcurrentLinkedQueue<IPv4> queue = new ConcurrentLinkedQueue<>();
            queue.add(ipPacket);
            ipPacketQueue.put(destIpAddress, queue);
        } else {
            ipPacketQueue.get(destIpAddress).add(ipPacket);
        }
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

        for (IPv4 ipPacket : ipPacketQueue.get(destIpAddress)) {
            Ip4Address destAddress = Ip4Address.valueOf(ipPacket.getDestinationAddress());
            if (config.inSameSubnet(deviceId, destAddress)) {
                ipPacket.setTtl((byte) (ipPacket.getTtl() - 1));
                ipPacket.setChecksum((short) 0);
                for (Host dest: srManager.hostService.getHostsByIp(destIpAddress)) {
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
                    eth.setPayload(ipPacket);

                    TrafficTreatment treatment = DefaultTrafficTreatment.builder().
                            setOutput(dest.location().port()).build();
                    OutboundPacket packet = new DefaultOutboundPacket(deviceId,
                            treatment, ByteBuffer.wrap(eth.serialize()));
                    srManager.packetService.emit(packet);
                    ipPacketQueue.get(destIpAddress).remove(ipPacket);
                }
                ipPacketQueue.get(destIpAddress).remove(ipPacket);
            }
        }
    }

}
