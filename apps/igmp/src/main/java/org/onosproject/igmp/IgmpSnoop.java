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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IGMP;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.olt.AccessDeviceConfig;
import org.onosproject.olt.AccessDeviceData;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

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
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfig;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MulticastRouteService multicastService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private Map<DeviceId, AccessDeviceData> oltData = new ConcurrentHashMap<>();

    private DeviceListener deviceListener = new InternalDeviceListener();
    private IgmpPacketProcessor processor = new IgmpPacketProcessor();
    private static ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.igmp");

        packetService.addProcessor(processor, PacketProcessor.director(1));

        networkConfig.getSubjects(DeviceId.class, AccessDeviceConfig.class).forEach(
                subject -> {
                    AccessDeviceConfig config = networkConfig.getConfig(subject,
                                                                        AccessDeviceConfig.class);
                    if (config != null) {
                        AccessDeviceData data = config.getOlt();
                        oltData.put(data.deviceId(), data);
                    }
                }
        );

        deviceService.addListener(deviceListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        deviceService.removeListener(deviceListener);
        log.info("Stopped");
    }

    private void processFilterObjective(DeviceId devId, Port port, boolean remove) {

        //TODO migrate to packet requests when packet service uses filtering objectives
        DefaultFilteringObjective.Builder builder = DefaultFilteringObjective.builder();

        builder = remove ? builder.deny() : builder.permit();

        FilteringObjective igmp = builder
                .withKey(Criteria.matchInPort(port.number()))
                .addCondition(Criteria.matchEthType(EthType.EtherType.IPV4.ethType()))
                .addCondition(Criteria.matchIPProtocol(IPv4.PROTOCOL_IGMP))
                .withMeta(DefaultTrafficTreatment.builder()
                                  .setOutput(PortNumber.CONTROLLER).build())
                .fromApp(appId)
                .withPriority(1000)
                .add(new ObjectiveContext() {
                    @Override
                    public void onSuccess(Objective objective) {
                        log.info("Igmp filter for {} on {} installed.",
                                 devId, port);
                    }

                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        log.info("Igmp filter for {} on {} failed because {}.",
                                 devId, port, error);
                    }
                });

        flowObjectiveService.filter(devId, igmp);
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



    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {

                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                case DEVICE_AVAILABILITY_CHANGED:
                case PORT_STATS_UPDATED:
                    break;
                case PORT_ADDED:
                    if (event.port().isEnabled()) {
                        processFilterObjective(event.subject().id(), event.port(), false);
                    }
                    break;
                case PORT_UPDATED:
                    if (event.port().isEnabled()) {
                        processFilterObjective(event.subject().id(), event.port(), false);
                    } else {
                        processFilterObjective(event.subject().id(), event.port(), true);
                    }
                    break;
                case PORT_REMOVED:
                    processFilterObjective(event.subject().id(), event.port(), false);
                    break;
                default:
                    log.warn("Unknown device event {}", event.type());
                    break;
            }

        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return oltData.containsKey(event.subject().id());
        }
    }
}
