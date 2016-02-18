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
package org.onosproject.cordvtn;

import com.google.common.collect.Maps;
import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles ARP requests for virtual network service IPs.
 */
public class CordVtnArpProxy {
    protected final Logger log = getLogger(getClass());

    private final ApplicationId appId;
    private final PacketService packetService;
    private final HostService hostService;

    private final Map<Ip4Address, MacAddress> gateways = Maps.newConcurrentMap();

    /**
     * Default constructor.
     *
     * @param appId application id
     * @param packetService packet service
     * @param hostService host service reference
     */
    public CordVtnArpProxy(ApplicationId appId, PacketService packetService, HostService hostService) {
        this.appId = appId;
        this.packetService = packetService;
        this.hostService = hostService;
    }

    /**
     * Requests ARP packet.
     */
    public void requestPacket() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .build();

        packetService.requestPackets(selector,
                                     PacketPriority.CONTROL,
                                     appId,
                                     Optional.empty());
    }

    /**
     * Cancels ARP packet.
     */
    public void cancelPacket() {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .build();

        packetService.cancelPackets(selector,
                                    PacketPriority.CONTROL,
                                    appId,
                                    Optional.empty());
    }

    /**
     * Adds a given gateway IP and MAC address to this ARP proxy.
     *
     * @param gatewayIp gateway ip address
     * @param gatewayMac gateway mac address
     */
    public void addGateway(IpAddress gatewayIp, MacAddress gatewayMac) {
        checkNotNull(gatewayIp);
        checkNotNull(gatewayMac);
        gateways.put(gatewayIp.getIp4Address(), gatewayMac);
    }

    /**
     * Removes a given service IP address from this ARP proxy.
     *
     * @param gatewayIp gateway ip address
     */
    public void removeGateway(IpAddress gatewayIp) {
        checkNotNull(gatewayIp);
        gateways.remove(gatewayIp.getIp4Address());
    }

    /**
     * Emits ARP reply with fake MAC address for a given ARP request.
     * It only handles requests for the registered service IPs, and the other
     * requests can be handled by other ARP handlers like openstackSwitching or
     * proxyArp, for example.
     *
     * @param context packet context
     * @param ethPacket ethernet packet
     */
    public void processArpPacket(PacketContext context, Ethernet ethPacket) {
        ARP arpPacket = (ARP) ethPacket.getPayload();
        if (arpPacket.getOpCode() != ARP.OP_REQUEST) {
           return;
        }

        Ip4Address targetIp = Ip4Address.valueOf(arpPacket.getTargetProtocolAddress());

        MacAddress gatewayMac = gateways.get(targetIp);
        MacAddress replyMac = gatewayMac != null ? gatewayMac : getMacFromHostService(targetIp);

        if (replyMac.equals(MacAddress.NONE)) {
            log.debug("Failed to find MAC for {}", targetIp.toString());
            context.block();
            return;
        }

        log.trace("Send ARP reply for {} with {}", targetIp.toString(), replyMac.toString());
        Ethernet ethReply = ARP.buildArpReply(
                targetIp,
                replyMac,
                ethPacket);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(context.inPacket().receivedFrom().port())
                .build();

        packetService.emit(new DefaultOutboundPacket(
                context.inPacket().receivedFrom().deviceId(),
                treatment,
                ByteBuffer.wrap(ethReply.serialize())));

        context.block();
    }

    /**
     * Emits gratuitous ARP when a gateway mac address has been changed.
     *
     * @param gatewayIp gateway ip address to update MAC
     * @param hosts set of hosts to send gratuitous ARP packet
     */
    public void sendGratuitousArpForGateway(IpAddress gatewayIp, Set<Host> hosts) {
        MacAddress gatewayMac = gateways.get(gatewayIp.getIp4Address());
        if (gatewayMac == null) {
            log.debug("Gateway {} is not registered to ARP proxy", gatewayIp.toString());
            return;
        }

        Ethernet ethArp = buildGratuitousArp(gatewayIp.getIp4Address(), gatewayMac);
        hosts.stream().forEach(host -> {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(host.location().port())
                    .build();

            packetService.emit(new DefaultOutboundPacket(
                    host.location().deviceId(),
                    treatment,
                    ByteBuffer.wrap(ethArp.serialize())));
        });
    }

    /**
     * Builds gratuitous ARP packet with a given IP and MAC address.
     *
     * @param ip ip address for TPA and SPA
     * @param mac new mac address
     * @return ethernet packet
     */
    private Ethernet buildGratuitousArp(IpAddress ip, MacAddress mac) {
        Ethernet eth = new Ethernet();

        eth.setEtherType(Ethernet.TYPE_ARP);
        eth.setSourceMACAddress(mac);
        eth.setDestinationMACAddress(MacAddress.BROADCAST);

        ARP arp = new ARP();
        arp.setOpCode(ARP.OP_REQUEST);
        arp.setHardwareType(ARP.HW_TYPE_ETHERNET);
        arp.setHardwareAddressLength((byte) Ethernet.DATALAYER_ADDRESS_LENGTH);
        arp.setProtocolType(ARP.PROTO_TYPE_IP);
        arp.setProtocolAddressLength((byte) Ip4Address.BYTE_LENGTH);

        arp.setSenderHardwareAddress(mac.toBytes());
        arp.setTargetHardwareAddress(MacAddress.BROADCAST.toBytes());
        arp.setSenderProtocolAddress(ip.getIp4Address().toOctets());
        arp.setTargetProtocolAddress(ip.getIp4Address().toOctets());

        eth.setPayload(arp);
        return eth;
    }

    /**
     * Returns MAC address of a host with a given target IP address by asking to
     * host service. It does not support overlapping IP.
     *
     * @param targetIp target ip
     * @return mac address, or NONE mac address if it fails to find the mac
     */
    private MacAddress getMacFromHostService(IpAddress targetIp) {
        checkNotNull(targetIp);

        Host host = hostService.getHostsByIp(targetIp)
                .stream()
                .findFirst()
                .orElse(null);

        if (host != null) {
            log.trace("Found MAC from host service for {}", targetIp.toString());
            return host.mac();
        } else {
            return MacAddress.NONE;
        }
    }
}
