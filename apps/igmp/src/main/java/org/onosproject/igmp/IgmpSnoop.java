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
package org.onosproject.igmp;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IGMP;
import org.onlab.packet.IGMPMembership;
import org.onlab.packet.IGMPQuery;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.SafeRecurringTask;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.olt.AccessDeviceConfig;
import org.onosproject.olt.AccessDeviceData;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Internet Group Management Protocol.
 */
@Component(immediate = true)
public class IgmpSnoop {

    private final Logger log = getLogger(getClass());

    private static final String DEST_MAC = "01:00:5E:00:00:01";
    private static final String DEST_IP = "224.0.0.1";

    private static final int DEFAULT_QUERY_PERIOD_SECS = 60;
    private static final byte DEFAULT_IGMP_RESP_CODE = 100;

    @Property(name = "multicastAddress",
            label = "Define the multicast base range to listen to")
    private String multicastAddress = IpPrefix.IPV4_MULTICAST_PREFIX.toString();

    @Property(name = "queryPeriod", intValue = DEFAULT_QUERY_PERIOD_SECS,
            label = "Delay in seconds between successive query runs")
    private int queryPeriod = DEFAULT_QUERY_PERIOD_SECS;

    @Property(name = "maxRespCode", byteValue = DEFAULT_IGMP_RESP_CODE,
            label = "Maximum time allowed before sending a responding report")
    private byte maxRespCode = DEFAULT_IGMP_RESP_CODE;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfig;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MulticastRouteService multicastService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;


    private ScheduledFuture<?> queryTask;
    private final ScheduledExecutorService queryService =
            Executors.newSingleThreadScheduledExecutor(groupedThreads("onos/igmp-query",
                                                                      "membership-query"));

    private Map<DeviceId, AccessDeviceData> oltData = new ConcurrentHashMap<>();

    private Map<IpAddress, IpAddress> ssmTranslateTable = new ConcurrentHashMap<>();

    private DeviceListener deviceListener = new InternalDeviceListener();
    private IgmpPacketProcessor processor = new IgmpPacketProcessor();
    private static ApplicationId appId;

    private InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    private static final Class<AccessDeviceConfig> CONFIG_CLASS =
            AccessDeviceConfig.class;

    private static final Class<IgmpSsmTranslateConfig> SSM_TRANSLATE_CONFIG_CLASS =
            IgmpSsmTranslateConfig.class;

    private ConfigFactory<DeviceId, AccessDeviceConfig> configFactory =
            new ConfigFactory<DeviceId, AccessDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY, CONFIG_CLASS, "accessDevice") {
                @Override
                public AccessDeviceConfig createConfig() {
                    return new AccessDeviceConfig();
                }
            };

    private ConfigFactory<ApplicationId, IgmpSsmTranslateConfig> ssmTranslateConfigFactory =
            new ConfigFactory<ApplicationId, IgmpSsmTranslateConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, SSM_TRANSLATE_CONFIG_CLASS, "ssmTranslate", true) {
                @Override
                public IgmpSsmTranslateConfig createConfig() {
                    return new IgmpSsmTranslateConfig();
                }
            };


    private ByteBuffer queryPacket;


    @Activate
    public void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());
        modified(context);

        appId = coreService.registerApplication("org.onosproject.igmp");

        packetService.addProcessor(processor, PacketProcessor.director(1));

        networkConfig.registerConfigFactory(configFactory);
        networkConfig.registerConfigFactory(ssmTranslateConfigFactory);
        networkConfig.addListener(configListener);

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

        IgmpSsmTranslateConfig ssmTranslateConfig =
                networkConfig.getConfig(appId, IgmpSsmTranslateConfig.class);

        if (ssmTranslateConfig != null) {
            Collection<McastRoute> translations = ssmTranslateConfig.getSsmTranslations();
            for (McastRoute route : translations) {
                ssmTranslateTable.put(route.group(), route.source());
            }
        }

        oltData.keySet().stream()
                .flatMap(did -> deviceService.getPorts(did).stream())
                .filter(p -> !oltData.get(p.element().id()).uplink().equals(p.number()))
                .filter(p -> p.isEnabled())
                .forEach(p -> processFilterObjective((DeviceId) p.element().id(), p, false));

        deviceService.addListener(deviceListener);

        restartQueryTask();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        deviceService.removeListener(deviceListener);
        networkConfig.removeListener(configListener);
        networkConfig.unregisterConfigFactory(configFactory);
        networkConfig.unregisterConfigFactory(ssmTranslateConfigFactory);
        queryTask.cancel(true);
        queryService.shutdownNow();
        componentConfigService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();

        // TODO read multicastAddress from config
        String strQueryPeriod = Tools.get(properties, "queryPeriod");
        String strResponseCode = Tools.get(properties, "maxRespCode");
        try {
            byte newMaxRespCode = Byte.parseByte(strResponseCode);
            if (maxRespCode != newMaxRespCode) {
                maxRespCode = newMaxRespCode;
                queryPacket = buildQueryPacket();
            }

            int newQueryPeriod = Integer.parseInt(strQueryPeriod);
            if (newQueryPeriod != queryPeriod) {
                queryPeriod = newQueryPeriod;
                restartQueryTask();
            }

        } catch (NumberFormatException e) {
            log.warn("Error parsing config input", e);
        }

        log.info("queryPeriod set to {}", queryPeriod);
        log.info("maxRespCode set to {}", maxRespCode);
    }

    private void restartQueryTask() {
        if (queryTask != null) {
            queryTask.cancel(true);
        }
        queryPacket = buildQueryPacket();
        queryTask = queryService.scheduleWithFixedDelay(
                SafeRecurringTask.wrap(this::querySubscribers),
                0,
                queryPeriod,
                TimeUnit.SECONDS);
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
                .withPriority(10000)
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

    private void processMembership(IGMP pkt, ConnectPoint location) {
        pkt.getGroups().forEach(group -> {

            if (!(group instanceof IGMPMembership)) {
                log.warn("Wrong group type in IGMP membership");
                return;
            }

            IGMPMembership membership = (IGMPMembership) group;

            // TODO allow pulling source from IGMP packet
            IpAddress source = ssmTranslateTable.get(group.getGaddr());
            if (source == null) {
                log.warn("No source found in SSM translate table for {}", group.getGaddr());
                return;
            }

            McastRoute route = new McastRoute(source,
                    group.getGaddr(),
                    McastRoute.Type.IGMP);

            if (membership.getRecordType() == IGMPMembership.MODE_IS_INCLUDE ||
                    membership.getRecordType() == IGMPMembership.CHANGE_TO_INCLUDE_MODE) {

                multicastService.removeSink(route, location);
                // TODO remove route if all sinks are gone
            } else if (membership.getRecordType() == IGMPMembership.MODE_IS_EXCLUDE ||
                    membership.getRecordType() == IGMPMembership.CHANGE_TO_EXCLUDE_MODE) {

                multicastService.add(route);
                multicastService.addSink(route, location);
            }

        });
    }

    private ByteBuffer buildQueryPacket() {
        IGMP igmp = new IGMP();
        igmp.setIgmpType(IGMP.TYPE_IGMPV3_MEMBERSHIP_QUERY);
        igmp.setMaxRespCode(maxRespCode);

        IGMPQuery query = new IGMPQuery(IpAddress.valueOf("0.0.0.0"), 0);
        igmp.addGroup(query);

        IPv4 ip = new IPv4();
        ip.setDestinationAddress(DEST_IP);
        ip.setProtocol(IPv4.PROTOCOL_IGMP);
        ip.setSourceAddress("192.168.1.1");
        ip.setTtl((byte) 1);
        ip.setPayload(igmp);

        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(DEST_MAC);
        eth.setSourceMACAddress("DE:AD:BE:EF:BA:11");
        eth.setEtherType(Ethernet.TYPE_IPV4);

        eth.setPayload(ip);

        eth.setPad(true);

        return ByteBuffer.wrap(eth.serialize());
    }

    private void querySubscribers() {
        oltData.keySet().stream()
                .flatMap(did -> deviceService.getPorts(did).stream())
                .filter(p -> !oltData.get(p.element().id()).uplink().equals(p.number()))
                .filter(p -> p.isEnabled())
                .forEach(p -> {
                    TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                            .setOutput(p.number()).build();
                    packetService.emit(new DefaultOutboundPacket((DeviceId) p.element().id(),
                                                                 treatment, queryPacket));
                });
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


            // IPv6 MLD packets are handled by ICMP6. We'll only deal with IPv4.
            if (ethPkt.getEtherType() != Ethernet.TYPE_IPV4) {
                return;
            }

            IPv4 ip = (IPv4) ethPkt.getPayload();
            IpAddress gaddr = IpAddress.valueOf(ip.getDestinationAddress());
            IpAddress saddr = Ip4Address.valueOf(ip.getSourceAddress());
            log.trace("Packet ({}, {}) -> ingress port: {}", saddr, gaddr,
                      context.inPacket().receivedFrom());


            if (ip.getProtocol() != IPv4.PROTOCOL_IGMP ||
                    !IpPrefix.IPV4_MULTICAST_PREFIX.contains(gaddr)) {
                return;
            }

            if (IpPrefix.IPV4_MULTICAST_PREFIX.contains(saddr)) {
                log.debug("IGMP Picked up a packet with a multicast source address.");
                return;
            }

            IGMP igmp = (IGMP) ip.getPayload();
            switch (igmp.getIgmpType()) {
                case IGMP.TYPE_IGMPV3_MEMBERSHIP_REPORT:
                    processMembership(igmp, pkt.receivedFrom());
                    break;

                case IGMP.TYPE_IGMPV3_MEMBERSHIP_QUERY:
                    log.debug("Received a membership query {} from {}",
                              igmp, pkt.receivedFrom());
                    break;

                case IGMP.TYPE_IGMPV1_MEMBERSHIP_REPORT:
                case IGMP.TYPE_IGMPV2_MEMBERSHIP_REPORT:
                case IGMP.TYPE_IGMPV2_LEAVE_GROUP:
                    log.debug("IGMP version 1 & 2 message types are not currently supported. Message type: {}",
                              igmp.getIgmpType());
                    break;
                default:
                    log.warn("Unknown IGMP message type: {}", igmp.getIgmpType());
                    break;
            }
        }
    }


    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceId devId = event.subject().id();
            switch (event.type()) {

                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                case DEVICE_REMOVED:
                case DEVICE_SUSPENDED:
                case DEVICE_AVAILABILITY_CHANGED:
                case PORT_STATS_UPDATED:
                    break;
                case PORT_ADDED:
                    if (!oltData.get(devId).uplink().equals(event.port().number()) &&
                            event.port().isEnabled()) {
                        processFilterObjective(event.subject().id(), event.port(), false);
                    }
                    break;
                case PORT_UPDATED:
                    if (oltData.get(devId).uplink().equals(event.port().number())) {
                        break;
                    }
                    if (event.port().isEnabled()) {
                        processFilterObjective(event.subject().id(), event.port(), false);
                    } else {
                        processFilterObjective(event.subject().id(), event.port(), true);
                    }
                    break;
                case PORT_REMOVED:
                    processFilterObjective(event.subject().id(), event.port(), true);
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

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {

                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    if (event.configClass().equals(CONFIG_CLASS)) {
                        AccessDeviceConfig config =
                                networkConfig.getConfig((DeviceId) event.subject(), CONFIG_CLASS);
                        if (config != null) {
                            oltData.put(config.getOlt().deviceId(), config.getOlt());
                            provisionDefaultFlows((DeviceId) event.subject());
                        }
                    }
                    if (event.configClass().equals(SSM_TRANSLATE_CONFIG_CLASS)) {
                        IgmpSsmTranslateConfig config =
                                networkConfig.getConfig((ApplicationId) event.subject(),
                                        SSM_TRANSLATE_CONFIG_CLASS);

                        if (config != null) {
                            ssmTranslateTable.clear();
                            config.getSsmTranslations().forEach(
                                    route -> ssmTranslateTable.put(route.group(), route.source()));
                        }
                    }
                    break;
                case CONFIG_REGISTERED:
                case CONFIG_UNREGISTERED:
                    break;
                case CONFIG_REMOVED:
                    if (event.configClass().equals(SSM_TRANSLATE_CONFIG_CLASS)) {
                        ssmTranslateTable.clear();
                    } else if (event.configClass().equals(CONFIG_CLASS)) {
                        oltData.remove(event.subject());
                    }

                default:
                    break;
            }
        }
    }

    private void provisionDefaultFlows(DeviceId deviceId) {
        List<Port> ports = deviceService.getPorts(deviceId);

        ports.stream()
                .filter(p -> !oltData.get(p.element().id()).uplink().equals(p.number()))
                .filter(p -> p.isEnabled())
                .forEach(p -> processFilterObjective((DeviceId) p.element().id(), p, false));
    }
}
