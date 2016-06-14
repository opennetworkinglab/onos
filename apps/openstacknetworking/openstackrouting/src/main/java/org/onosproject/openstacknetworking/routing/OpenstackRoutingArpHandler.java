/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking.routing;

import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstacknetworking.OpenstackNetworkingConfig;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handle ARP packet sent from Openstack Gateway nodes.
 */
public class OpenstackRoutingArpHandler {
    protected final Logger log = getLogger(getClass());

    private final PacketService packetService;
    private final OpenstackInterfaceService openstackService;
    private final OpenstackNetworkingConfig config;
    private static final String NETWORK_ROUTER_GATEWAY = "network:router_gateway";
    private static final String NETWORK_FLOATING_IP = "network:floatingip";

    /**
     * Default constructor.
     *
     * @param packetService packet service
     * @param openstackService openstackInterface service
     * @param config openstackRoutingConfig
     */
    OpenstackRoutingArpHandler(PacketService packetService, OpenstackInterfaceService openstackService,
                               OpenstackNetworkingConfig config) {
        this.packetService = packetService;
        this.openstackService = checkNotNull(openstackService);
        this.config = checkNotNull(config);
    }

    /**
     * Requests ARP packet to GatewayNode.
     *
     * @param appId application id
     */
    public void requestPacket(ApplicationId appId) {

        TrafficSelector arpSelector = DefaultTrafficSelector.builder()
                .matchEthType(EthType.EtherType.ARP.ethType().toShort())
                .build();

        packetService.requestPackets(arpSelector,
                PacketPriority.CONTROL,
                appId,
                Optional.of(DeviceId.deviceId(config.gatewayBridgeId())));
    }

    /**
     * Handles ARP packet.
     *
     * @param context packet context
     * @param ethernet ethernet
     */
    public void processArpPacketFromRouter(PacketContext context, Ethernet ethernet) {
        checkNotNull(context, "context can not be null");
        checkNotNull(ethernet, "ethernet can not be null");


        ARP arp = (ARP) ethernet.getPayload();

        log.debug("arpEvent called from {} to {}",
                Ip4Address.valueOf(arp.getSenderProtocolAddress()).toString(),
                Ip4Address.valueOf(arp.getTargetProtocolAddress()).toString());

        if (arp.getOpCode() != ARP.OP_REQUEST) {
            return;
        }

        IpAddress targetIp = Ip4Address.valueOf(arp.getTargetProtocolAddress());

        if (getTargetMacForTargetIp(targetIp.getIp4Address()) == MacAddress.NONE) {
                return;
        }
        MacAddress targetMac = MacAddress.valueOf(config.gatewayExternalInterfaceMac());

        Ethernet ethReply = ARP.buildArpReply(targetIp.getIp4Address(),
                targetMac, ethernet);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(context.inPacket().receivedFrom().port())
                .build();

        packetService.emit(new DefaultOutboundPacket(
                context.inPacket().receivedFrom().deviceId(),
                treatment,
                ByteBuffer.wrap(ethReply.serialize())));
    }

    private MacAddress getTargetMacForTargetIp(Ip4Address targetIp) {
        OpenstackPort port = openstackService.ports().stream()
                .filter(p -> p.deviceOwner().equals(NETWORK_ROUTER_GATEWAY) ||
                             p.deviceOwner().equals(NETWORK_FLOATING_IP))
                .filter(p -> p.fixedIps().containsValue(targetIp.getIp4Address()))
                .findAny().orElse(null);

        if (port == null) {
            return MacAddress.NONE;
        }
        return port.macAddress();
    }
}
