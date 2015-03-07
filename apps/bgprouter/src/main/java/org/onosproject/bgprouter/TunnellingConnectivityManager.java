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
package org.onosproject.bgprouter;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TCP;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.routing.config.BgpPeer;
import org.onosproject.routing.config.BgpSpeaker;
import org.onosproject.routing.config.InterfaceAddress;
import org.onosproject.routing.config.RoutingConfigurationService;


/**
 * Manages connectivity between peers by tunnelling BGP traffic through
 * OpenFlow packet-ins and packet-outs.
 */
public class TunnellingConnectivityManager {

    private static final short BGP_PORT = 179;

    private final ApplicationId appId;

    private final BgpSpeaker bgpSpeaker;

    private final PacketService packetService;
    private final RoutingConfigurationService configService;

    private final BgpProcessor processor = new BgpProcessor();

    public TunnellingConnectivityManager(ApplicationId appId,
                                         RoutingConfigurationService configService,
                                         PacketService packetService,
                                         FlowRuleService flowService) {
        this.appId = appId;
        this.configService = configService;
        this.packetService = packetService;

        BgpSpeaker bgpSpeaker = null;
        for (BgpSpeaker speaker : configService.getBgpSpeakers().values()) {
            bgpSpeaker = speaker;
            break;
        }

        if (bgpSpeaker == null) {
            throw new IllegalArgumentException("Must have at least one BGP speaker configured");
        }

        this.bgpSpeaker = bgpSpeaker;

        TrafficSelector selectorDst = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(BGP_PORT)
                .build();

        TrafficSelector selectorSrc = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpSrc(BGP_PORT)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .punt()
                .build();

        FlowRuleOperations.Builder builder = FlowRuleOperations.builder();
        builder.add(new DefaultFlowRule(bgpSpeaker.connectPoint().deviceId(),
                                        selectorSrc, treatment, 0, appId, 0, true));
        builder.add(new DefaultFlowRule(bgpSpeaker.connectPoint().deviceId(),
                                        selectorDst, treatment, 0, appId, 0, true));
        flowService.apply(builder.build());
    }

    public void start() {
        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 3);
    }

    public void stop() {
        packetService.removeProcessor(processor);
        // Should revoke packet requests in the future
    }

    /**
     * Forwards a BGP packet to another connect point.
     *
     * @param context the packet context of the incoming packet
     */
    private void forward(PacketContext context) {
        ConnectPoint outputPort = null;

        IPv4 ipv4 = (IPv4) context.inPacket().parsed().getPayload();
        IpAddress dstAddress = IpAddress.valueOf(ipv4.getDestinationAddress());

        if (context.inPacket().receivedFrom().equals(bgpSpeaker.connectPoint())) {
            BgpPeer peer = configService.getBgpPeers().get(dstAddress);
            if (peer != null) {
                outputPort = peer.connectPoint();
            }
        }
        for (InterfaceAddress addr : bgpSpeaker.interfaceAddresses()) {
            if (addr.ipAddress().equals(dstAddress) && !context.inPacket()
                    .receivedFrom().equals(bgpSpeaker.connectPoint())) {
                outputPort = bgpSpeaker.connectPoint();
            }
        }

        if (outputPort != null) {
            TrafficTreatment t = DefaultTrafficTreatment.builder()
                    .setOutput(outputPort.port()).build();
            OutboundPacket o = new DefaultOutboundPacket(
                    outputPort.deviceId(), t, context.inPacket().unparsed());
            packetService.emit(o);
        }
    }

    /**
     * Packet processor responsible receiving and filtering BGP packets.
     */
    private class BgpProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }

            Ethernet packet = context.inPacket().parsed();

            if (packet == null) {
                return;
            }

            if (packet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) packet.getPayload();
                if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_TCP) {
                    TCP tcpPacket = (TCP) ipv4Packet.getPayload();

                    if (tcpPacket.getDestinationPort() == BGP_PORT ||
                            tcpPacket.getSourcePort() == BGP_PORT) {
                        forward(context);
                    }
                }
            }
        }
    }
}
