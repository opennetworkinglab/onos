/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.NetFloatingIP;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handle ARP requests from gateway nodes.
 */
@Component(immediate = true)
public class OpenstackRoutingArpHandler {

    private final Logger log = getLogger(getClass());

    private static final String DEVICE_OWNER_ROUTER_GW = "network:router_gateway";
    private static final String DEVICE_OWNER_FLOATING_IP = "network:floatingip";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkAdminService osNetworkAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackRouterService osRouterService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final PacketProcessor packetProcessor = new InternalPacketProcessor();

    @Activate
    protected void activate() {
        packetService.addProcessor(packetProcessor, PacketProcessor.director(1));
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(packetProcessor);
        eventExecutor.shutdown();
        log.info("Stopped");
    }

    private void processArpPacket(PacketContext context, Ethernet ethernet) {
        ARP arp = (ARP) ethernet.getPayload();
        if (arp.getOpCode() == ARP.OP_REQUEST) {
            if (log.isTraceEnabled()) {
                log.trace("ARP request received from {} for {}",
                        Ip4Address.valueOf(arp.getSenderProtocolAddress()).toString(),
                        Ip4Address.valueOf(arp.getTargetProtocolAddress()).toString());
            }

            IpAddress targetIp = Ip4Address.valueOf(arp.getTargetProtocolAddress());

            MacAddress targetMac = null;

            NetFloatingIP floatingIP = osRouterService.floatingIps().stream()
                    .filter(ip -> ip.getFloatingIpAddress().equals(targetIp.toString()))
                    .findAny().orElse(null);

            //In case target ip is for associated floating ip, sets target mac to vm's.
            if (floatingIP != null && floatingIP.getPortId() != null) {
                targetMac = MacAddress.valueOf(osNetworkAdminService.port(floatingIP.getPortId()).getMacAddress());
            }

            if (isExternalGatewaySourceIp(targetIp.getIp4Address())) {
                targetMac = Constants.DEFAULT_GATEWAY_MAC;
            }

            if (targetMac == null) {
                log.trace("Unknown target ARP request for {}, ignore it", targetIp);
                return;
            }

            Ethernet ethReply = ARP.buildArpReply(targetIp.getIp4Address(),
                    targetMac, ethernet);


            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(context.inPacket().receivedFrom().port()).build();

            packetService.emit(new DefaultOutboundPacket(
                    context.inPacket().receivedFrom().deviceId(),
                    treatment,
                    ByteBuffer.wrap(ethReply.serialize())));

            context.block();
        } else if (arp.getOpCode() == ARP.OP_REPLY) {
            PortNumber receivedPortNum = context.inPacket().receivedFrom().port();
            log.debug("ARP reply ip: {}, mac: {}",
                    Ip4Address.valueOf(arp.getSenderProtocolAddress()),
                    MacAddress.valueOf(arp.getSenderHardwareAddress()));
            try {
                if (receivedPortNum.equals(
                        osNodeService.node(context.inPacket().receivedFrom().deviceId()).uplinkPortNum())) {
                    osNetworkAdminService.updateExternalPeerRouterMac(
                            Ip4Address.valueOf(arp.getSenderProtocolAddress()),
                            MacAddress.valueOf(arp.getSenderHardwareAddress()));
                }
            } catch (Exception e) {
                log.error("Exception occurred because of {}", e.toString());
            }
        }

    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            Set<DeviceId> gateways = osNodeService.completeNodes(GATEWAY)
                    .stream().map(OpenstackNode::intgBridge)
                    .collect(Collectors.toSet());

            if (!gateways.contains(context.inPacket().receivedFrom().deviceId())) {
                // return if the packet is not from gateway nodes
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();
            if (ethernet != null &&
                    ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                eventExecutor.execute(() -> processArpPacket(context, ethernet));
            }
        }
    }

    private boolean isExternalGatewaySourceIp(IpAddress targetIp) {
        return osNetworkAdminService.ports().stream()
                .filter(osPort -> Objects.equals(osPort.getDeviceOwner(),
                        DEVICE_OWNER_ROUTER_GW))
                .flatMap(osPort -> osPort.getFixedIps().stream())
                .anyMatch(ip -> IpAddress.valueOf(ip.getIpAddress()).equals(targetIp));
    }
}
