/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onlab.packet.TpPort;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.routing.config.BgpConfig;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Manages connectivity between peers by tunnelling BGP traffic through
 * OpenFlow packet-ins and packet-outs.
 */
public class TunnellingConnectivityManager {

    private static final short BGP_PORT = 179;
    private final Logger log = getLogger(getClass());
    private final ApplicationId appId;

    private final BgpConfig.BgpSpeakerConfig bgpSpeaker;

    private final PacketService packetService;
    private final InterfaceService interfaceService;
    private final FlowObjectiveService flowObjectiveService;

    private final BgpProcessor processor = new BgpProcessor();

    public TunnellingConnectivityManager(ApplicationId appId,
                                         BgpConfig bgpConfig,
                                         InterfaceService interfaceService,
                                         PacketService packetService,
                                         FlowObjectiveService flowObjectiveService) {
        this.appId = appId;
        this.interfaceService = interfaceService;
        this.packetService = packetService;
        this.flowObjectiveService = flowObjectiveService;

        Optional<BgpConfig.BgpSpeakerConfig> bgpSpeaker =
                bgpConfig.bgpSpeakers().stream().findAny();

        if (!bgpSpeaker.isPresent()) {
            throw new IllegalArgumentException("Must have at least one BGP speaker configured");
        }

        this.bgpSpeaker = bgpSpeaker.get();

    }

    public void start() {
        packetService.addProcessor(processor, PacketProcessor.director(3));
    }

    public void stop() {
        packetService.removeProcessor(processor);
        // Should revoke packet requests in the future
    }

    /**
     * Pushes the flow rules for forwarding BGP TCP packets to controller.
     * It is called when switches are connected and available.
     */
    public void notifySwitchAvailable() {
        // control plane OVS is available, push default flows
        TrafficSelector selectorDst = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(TpPort.tpPort(BGP_PORT))
                .build();

        TrafficSelector selectorSrc = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpSrc(TpPort.tpPort(BGP_PORT))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .punt()
                .build();

        ForwardingObjective puntSrc = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withSelector(selectorSrc)
                .withTreatment(treatment)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .add();
        flowObjectiveService.forward(bgpSpeaker.connectPoint().deviceId(),
                puntSrc);

        ForwardingObjective puntDst = DefaultForwardingObjective.builder()
                .fromApp(appId)
                .makePermanent()
                .withSelector(selectorDst)
                .withTreatment(treatment)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .add();
        flowObjectiveService.forward(bgpSpeaker.connectPoint().deviceId(),
                puntDst);
        log.info("Sent punt forwarding objective to {}", bgpSpeaker.connectPoint().deviceId());
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
            if (bgpSpeaker.peers().contains(dstAddress)) {
                Interface intf = interfaceService.getMatchingInterface(dstAddress);
                if (intf != null) {
                    outputPort = intf.connectPoint();
                }
            }
        } else {
            Set<Interface> interfaces =
                    interfaceService.getInterfacesByPort(context.inPacket().receivedFrom());

            if (interfaces.stream()
                    .flatMap(intf -> intf.ipAddressesList().stream())
                    .anyMatch(ia -> ia.ipAddress().equals(dstAddress))) {
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
