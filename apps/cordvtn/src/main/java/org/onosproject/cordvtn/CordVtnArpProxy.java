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

import com.google.common.collect.Sets;
import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles ARP requests for virtual network service IPs.
 */
public class CordVtnArpProxy {
    protected final Logger log = getLogger(getClass());
    // TODO make gateway MAC address configurable
    private static final MacAddress DEFAULT_GATEWAY_MAC = MacAddress.valueOf("00:00:00:00:00:01");

    private final ApplicationId appId;
    private final PacketService packetService;

    private Set<Ip4Address> serviceIPs = Sets.newHashSet();

    /**
     * Default constructor.
     *
     * @param appId application id
     * @param packetService packet service
     */
    public CordVtnArpProxy(ApplicationId appId, PacketService packetService) {
        this.appId = appId;
        this.packetService = packetService;
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
     * Adds a given service IP address to be served.
     *
     * @param serviceIp service ip
     */
    public void addServiceIp(IpAddress serviceIp) {
        checkNotNull(serviceIp);
        serviceIPs.add(serviceIp.getIp4Address());
    }

    /**
     * Removes a given service IP address from this ARP proxy.
     *
     * @param serviceIp service ip
     */
    public void removeServiceIp(IpAddress serviceIp) {
        checkNotNull(serviceIp);
        serviceIPs.remove(serviceIp.getIp4Address());
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
        Ip4Address targetIp = Ip4Address.valueOf(arpPacket.getTargetProtocolAddress());

        if (arpPacket.getOpCode() != ARP.OP_REQUEST) {
           return;
        }

        if (!serviceIPs.contains(targetIp)) {
            return;
        }

        Ethernet ethReply = ARP.buildArpReply(
                targetIp,
                DEFAULT_GATEWAY_MAC,
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
}
