/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.reactive.routing;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ARP;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.slf4j.Logger;

import java.nio.ByteBuffer;

import static org.onlab.packet.Ethernet.TYPE_ARP;
import static org.onlab.packet.Ethernet.TYPE_IPV4;
import static org.onosproject.net.packet.PacketPriority.REACTIVE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This is reactive routing to handle 3 cases:
 * (1) one host wants to talk to another host, both two hosts are in
 * SDN network.
 * (2) one host in SDN network wants to talk to another host in Internet.
 * (3) one host from Internet wants to talk to another host in SDN network.
 */
@Component(immediate = true)
public class SdnIpReactiveRouting {

    private static final String APP_NAME = "org.onosproject.reactive.routing";
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RoutingService routingService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RoutingConfigurationService config;

    private ApplicationId appId;

    private ReactiveRoutingProcessor processor =
            new ReactiveRoutingProcessor();

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_NAME);
        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 2);
        requestIntercepts();
        log.info("SDN-IP Reactive Routing Started");
    }

    @Deactivate
    public void deactivate() {
        withdrawIntercepts();
        packetService.removeProcessor(processor);
        processor = null;
        log.info("SDN-IP Reactive Routing Stopped");
    }

    /**
     * Request packet in via the PacketService.
     */
    private void requestIntercepts() {
        //TODO: to support IPv6 later
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(TYPE_IPV4);
        packetService.requestPackets(selector.build(), REACTIVE, appId);
        selector.matchEthType(TYPE_ARP);
        packetService.requestPackets(selector.build(), REACTIVE, appId);
    }

    /**
     * Cancel request for packet in via PacketService.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(TYPE_IPV4);
        packetService.cancelPackets(selector.build(), REACTIVE, appId);
        selector = DefaultTrafficSelector.builder();
        selector.matchEthType(TYPE_ARP);
        packetService.cancelPackets(selector.build(), REACTIVE, appId);
    }

    private class ReactiveRoutingProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            if (ethPkt == null) {
                return;
            }
            ConnectPoint srcConnectPoint = pkt.receivedFrom();

            switch (EthType.EtherType.lookup(ethPkt.getEtherType())) {
            case ARP:
                ARP arpPacket = (ARP) ethPkt.getPayload();
                Ip4Address targetIpAddress = Ip4Address
                        .valueOf(arpPacket.getTargetProtocolAddress());
                // Only when it is an ARP request packet and the target IP
                // address is a virtual gateway IP address, then it will be
                // processed.
                if (arpPacket.getOpCode() == ARP.OP_REQUEST
                        && config.isVirtualGatewayIpAddress(targetIpAddress)) {
                    MacAddress gatewayMacAddress =
                            config.getVirtualGatewayMacAddress();
                    if (gatewayMacAddress == null) {
                        break;
                    }
                    Ethernet eth = ARP.buildArpReply(targetIpAddress,
                                                     gatewayMacAddress,
                                                     ethPkt);

                    TrafficTreatment.Builder builder =
                            DefaultTrafficTreatment.builder();
                    builder.setOutput(srcConnectPoint.port());
                    packetService.emit(new DefaultOutboundPacket(
                            srcConnectPoint.deviceId(),
                            builder.build(),
                            ByteBuffer.wrap(eth.serialize())));
                }
                break;
            case IPV4:
                // Parse packet
                IPv4 ipv4Packet = (IPv4) ethPkt.getPayload();
                IpAddress dstIp =
                        IpAddress.valueOf(ipv4Packet.getDestinationAddress());
                IpAddress srcIp =
                        IpAddress.valueOf(ipv4Packet.getSourceAddress());
                MacAddress srcMac = ethPkt.getSourceMAC();
                routingService.packetReactiveProcessor(dstIp, srcIp,
                                                       srcConnectPoint, srcMac);

                // TODO emit packet first or packetReactiveProcessor first
                ConnectPoint egressConnectPoint = null;
                egressConnectPoint = routingService.getEgressConnectPoint(dstIp);
                if (egressConnectPoint != null) {
                    forwardPacketToDst(context, egressConnectPoint);
                }
                break;
            default:
                break;
            }
        }
    }

    /**
     * Emits the specified packet onto the network.
     *
     * @param context      the packet context
     * @param connectPoint the connect point where the packet should be
     *                     sent out
     */
    private void forwardPacketToDst(PacketContext context,
                                    ConnectPoint connectPoint) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(connectPoint.port()).build();
        OutboundPacket packet =
                new DefaultOutboundPacket(connectPoint.deviceId(), treatment,
                                          context.inPacket().unparsed());
        packetService.emit(packet);
        log.trace("sending packet: {}", packet);
    }

}

