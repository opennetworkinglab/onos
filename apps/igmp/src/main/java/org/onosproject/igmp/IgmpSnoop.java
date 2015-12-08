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
package org.onosproject.igmp;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.IGMP;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Internet Group Management Protocol.
 */
@Component(immediate = true)
public class IgmpSnoop {
    private final Logger log = getLogger(getClass());

    private static final String DEFAULT_MCAST_ADDR = "224.0.0.0/4";

    @Property(name = "multicastAddress",
            label = "Define the multicast base raneg to listen to")
    private String multicastAddress = DEFAULT_MCAST_ADDR;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfig;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MulticastRouteService multicastService;

    private IgmpPacketProcessor processor = new IgmpPacketProcessor();
    private static ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.igmp");

        packetService.addProcessor(processor, PacketProcessor.director(1));

        networkConfig.getSubjects(DeviceId.class, IgmpDeviceConfig.class).forEach(
                subject -> {
                    IgmpDeviceConfig config = networkConfig.getConfig(subject,
                                                                      IgmpDeviceConfig.class);
                    if (config != null) {
                        IgmpDeviceData data = config.getDevice();
                        submitPacketRequests(data.deviceId());
                    }
                }
        );

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }

    private void submitPacketRequests(DeviceId deviceId) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchIPProtocol(IPv4.PROTOCOL_IGMP);
        packetService.requestPackets(selector.build(),
                                     PacketPriority.REACTIVE,
                                     appId,
                                     Optional.of(deviceId));

    }

    /**
     * Packet processor responsible for handling IGMP packets.
     */
    private class IgmpPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();
            if (ethPkt == null) {
                return;
            }

            /*
             * IPv6 MLD packets are handled by ICMP6. We'll only deal
             * with IPv4.
             */
            if (ethPkt.getEtherType() != Ethernet.TYPE_IPV4) {
                return;
            }

            IPv4 ip = (IPv4) ethPkt.getPayload();
            IpAddress gaddr = IpAddress.valueOf(ip.getDestinationAddress());
            IpAddress saddr = Ip4Address.valueOf(ip.getSourceAddress());
            log.debug("Packet ({}, {}) -> ingress port: {}", saddr, gaddr,
                      context.inPacket().receivedFrom());


            if (ip.getProtocol() != IPv4.PROTOCOL_IGMP) {
                log.debug("IGMP Picked up a non IGMP packet.");
                return;
            }

            IpPrefix mcast = IpPrefix.valueOf(DEFAULT_MCAST_ADDR);
            if (!mcast.contains(gaddr)) {
                log.debug("IGMP Picked up a non multicast packet.");
                return;
            }

            if (mcast.contains(saddr)) {
                log.debug("IGMP Picked up a packet with a multicast source address.");
                return;
            }

            IGMP igmp = (IGMP) ip.getPayload();
            switch (igmp.getIgmpType()) {

                case IGMP.TYPE_IGMPV3_MEMBERSHIP_REPORT:
                    IGMPProcessMembership.processMembership(igmp, pkt.receivedFrom());
                    break;

                case IGMP.TYPE_IGMPV3_MEMBERSHIP_QUERY:
                    processQuery(igmp, pkt.receivedFrom());
                    break;

                case IGMP.TYPE_IGMPV1_MEMBERSHIP_REPORT:
                case IGMP.TYPE_IGMPV2_MEMBERSHIP_REPORT:
                case IGMP.TYPE_IGMPV2_LEAVE_GROUP:
                    log.debug("IGMP version 1 & 2 message types are not currently supported. Message type: " +
                                      igmp.getIgmpType());
                    break;

                default:
                    log.debug("Unkown IGMP message type: " + igmp.getIgmpType());
                    break;
            }
        }
    }

    private void processQuery(IGMP pkt, ConnectPoint location) {
        pkt.getGroups().forEach(group -> group.getSources().forEach(src -> {

            McastRoute route = new McastRoute(src,
                                              group.getGaddr(),
                                              McastRoute.Type.IGMP);
            multicastService.add(route);
            multicastService.addSink(route, location);

        }));
    }
}
