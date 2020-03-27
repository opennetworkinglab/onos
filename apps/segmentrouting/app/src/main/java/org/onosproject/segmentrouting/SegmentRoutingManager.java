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
package org.onosproject.segmentrouting;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mcast.api.McastEvent;
import org.onosproject.mcast.api.McastListener;
import org.onosproject.mcast.api.MulticastRouteService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigException;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.InterfaceConfig;
import org.onosproject.net.config.basics.McastConfig;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostProbingService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intent.WorkPartitionService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.RouteEvent;
import org.onosproject.routeservice.RouteListener;
import org.onosproject.routeservice.RouteService;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.onosproject.segmentrouting.config.SegmentRoutingDeviceConfig;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.onosproject.segmentrouting.grouphandler.DestinationSet;
import org.onosproject.segmentrouting.grouphandler.NextNeighbors;
import org.onosproject.segmentrouting.mcast.McastFilteringObjStoreKey;
import org.onosproject.segmentrouting.mcast.McastHandler;
import org.onosproject.segmentrouting.mcast.McastRole;
import org.onosproject.segmentrouting.mcast.McastRoleStoreKey;
import org.onosproject.segmentrouting.mcast.McastStoreKey;
import org.onosproject.segmentrouting.pwaas.DefaultL2Tunnel;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelHandler;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2Tunnel;
import org.onosproject.segmentrouting.pwaas.L2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.L2TunnelHandler;
import org.onosproject.segmentrouting.pwaas.L2TunnelPolicy;
import org.onosproject.segmentrouting.storekey.DestinationSetNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.MacVlanNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.PortNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.VlanNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.XConnectStoreKey;
import org.onosproject.segmentrouting.xconnect.api.XconnectService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static org.onlab.packet.Ethernet.TYPE_ARP;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_REGISTERED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UNREGISTERED;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.ACTIVE_PROBING_DEFAULT;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.DEFAULT_INTERNAL_VLAN_DEFAULT;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.PROP_ACTIVE_PROBING;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.PROP_DEFAULT_INTERNAL_VLAN;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.PROP_PW_TRANSPORT_VLAN;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.PROP_RESPOND_TO_UNKNOWN_HOSTS;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.PROP_ROUTE_DOUBLE_TAGGED_HOSTS;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.PROP_ROUTE_SIMPLIFICATION;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.PROP_SINGLE_HOMED_DOWN;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.PROP_SYMMETRIC_PROBING;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.PW_TRANSPORT_VLAN_DEFAULT;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.RESPOND_TO_UNKNOWN_HOSTS_DEFAULT;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.ROUTE_DOUBLE_TAGGED_HOSTS_DEFAULT;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.ROUTE_SIMPLIFICATION_DEFAULT;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.SINGLE_HOMED_DOWN_DEFAULT;
import static org.onosproject.segmentrouting.OsgiPropertyConstants.SYMMETRIC_PROBING_DEFAULT;

/**
 * Segment routing manager.
 */
@Component(
    immediate = true,
    service = SegmentRoutingService.class,
    property = {
        PROP_ACTIVE_PROBING + ":Boolean=" + ACTIVE_PROBING_DEFAULT,
        PROP_SINGLE_HOMED_DOWN + ":Boolean=" + SINGLE_HOMED_DOWN_DEFAULT,
        PROP_RESPOND_TO_UNKNOWN_HOSTS + ":Boolean=" + RESPOND_TO_UNKNOWN_HOSTS_DEFAULT,
        PROP_ROUTE_DOUBLE_TAGGED_HOSTS + ":Boolean=" + ROUTE_DOUBLE_TAGGED_HOSTS_DEFAULT,
        PROP_DEFAULT_INTERNAL_VLAN + ":Integer=" + DEFAULT_INTERNAL_VLAN_DEFAULT,
        PROP_PW_TRANSPORT_VLAN + ":Integer=" + PW_TRANSPORT_VLAN_DEFAULT,
        PROP_SYMMETRIC_PROBING + ":Boolean=" + SYMMETRIC_PROBING_DEFAULT,
        PROP_ROUTE_SIMPLIFICATION + ":Boolean=" + ROUTE_SIMPLIFICATION_DEFAULT
    }
)
public class SegmentRoutingManager implements SegmentRoutingService {

    private static Logger log = LoggerFactory.getLogger(SegmentRoutingManager.class);
    private static final String NOT_MASTER = "Current instance is not the master of {}. Ignore.";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ComponentConfigService compCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public NeighbourResolutionService neighbourResolutionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    HostProbingService probingService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    DeviceAdminService deviceAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public MulticastRouteService multicastRouteService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public RouteService routeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public WorkPartitionService workPartitionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC)
    public volatile XconnectService xconnectService;

    /** Enable active probing to discover dual-homed hosts. */
    boolean activeProbing = ACTIVE_PROBING_DEFAULT;

    /** Enable only send probe on the same port number of the pair device. */
    boolean symmetricProbing = SYMMETRIC_PROBING_DEFAULT;

    /** Enable administratively taking down single-homed hosts. */
    boolean singleHomedDown = SINGLE_HOMED_DOWN_DEFAULT;

    /** Enable this to respond to ARP/NDP requests from unknown hosts. */
    boolean respondToUnknownHosts = RESPOND_TO_UNKNOWN_HOSTS_DEFAULT;

    /** Program flows and groups to pop and route double tagged hosts. */
    boolean routeDoubleTaggedHosts = ROUTE_DOUBLE_TAGGED_HOSTS_DEFAULT;

    /** internal vlan assigned by default to unconfigured ports. */
    private int defaultInternalVlan = DEFAULT_INTERNAL_VLAN_DEFAULT;

    /** vlan used for transport of pseudowires between switches. */
    private int pwTransportVlan = PW_TRANSPORT_VLAN_DEFAULT;

    /** Enabling route simplification. */
    boolean  routeSimplification = ROUTE_SIMPLIFICATION_DEFAULT;

    ArpHandler arpHandler = null;
    IcmpHandler icmpHandler = null;
    IpHandler ipHandler = null;
    RoutingRulePopulator routingRulePopulator = null;
    ApplicationId appId;
    DeviceConfiguration deviceConfiguration = null;

    DefaultRoutingHandler defaultRoutingHandler = null;
    private TunnelHandler tunnelHandler = null;
    private PolicyHandler policyHandler = null;
    private InternalPacketProcessor processor = null;
    private InternalLinkListener linkListener = null;
    private InternalDeviceListener deviceListener = null;
    private AppConfigHandler appCfgHandler = null;
    McastHandler mcastHandler = null;
    HostHandler hostHandler = null;
    private RouteHandler routeHandler = null;
    LinkHandler linkHandler = null;
    private SegmentRoutingNeighbourDispatcher neighbourHandler = null;
    private DefaultL2TunnelHandler l2TunnelHandler = null;
    private TopologyHandler topologyHandler = null;
    private final InternalHostListener hostListener = new InternalHostListener();
    private final InternalConfigListener cfgListener = new InternalConfigListener(this);
    private final InternalMcastListener mcastListener = new InternalMcastListener();
    private final InternalRouteEventListener routeListener = new InternalRouteEventListener();
    private final InternalTopologyListener topologyListener = new InternalTopologyListener();
    private final InternalMastershipListener mastershipListener = new InternalMastershipListener();
    final InternalClusterListener clusterListener = new InternalClusterListener();
    //Completable future for network configuration process to buffer config events handling during activation
    private CompletableFuture<Boolean> networkConfigCompletion = null;
    private final Object networkConfigCompletionLock = new Object();
    private List<Event> queuedEvents = new CopyOnWriteArrayList<>();

    // Handles device, link, topology and network config events
    private ScheduledExecutorService mainEventExecutor;

    // Handles host, route and mcast events respectively
    private ScheduledExecutorService hostEventExecutor;
    private ScheduledExecutorService routeEventExecutor;
    private ScheduledExecutorService mcastEventExecutor;
    private ExecutorService packetExecutor;
    ExecutorService neighborExecutor;

    Map<DeviceId, DefaultGroupHandler> groupHandlerMap = new ConcurrentHashMap<>();
    /**
     * Per device next objective ID store with (device id + destination set) as key.
     * Used to keep track on MPLS group information.
     */
    private EventuallyConsistentMap<DestinationSetNextObjectiveStoreKey, NextNeighbors>
            dsNextObjStore = null;
    /**
     * Per device next objective ID store with (device id + vlanid) as key.
     * Used to keep track on L2 flood group information.
     */
    private EventuallyConsistentMap<VlanNextObjectiveStoreKey, Integer>
            vlanNextObjStore = null;
    /**
     * Per device next objective ID store with (device id + port + treatment + meta) as key.
     * Used to keep track on L2 interface group and L3 unicast group information for direct hosts.
     */
    private EventuallyConsistentMap<PortNextObjectiveStoreKey, Integer>
            portNextObjStore = null;

    /**
     * Per device next objective ID store with (device id + MAC address + vlan) as key.
     * Used to keep track of L3 unicast group for indirect hosts.
     */
    private EventuallyConsistentMap<MacVlanNextObjectiveStoreKey, Integer>
            macVlanNextObjStore = null;

    private EventuallyConsistentMap<String, Tunnel> tunnelStore = null;
    private EventuallyConsistentMap<String, Policy> policyStore = null;

    private AtomicBoolean programmingScheduled = new AtomicBoolean();

    private final ConfigFactory<DeviceId, SegmentRoutingDeviceConfig> deviceConfigFactory =
            new ConfigFactory<DeviceId, SegmentRoutingDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    SegmentRoutingDeviceConfig.class, "segmentrouting") {
                @Override
                public SegmentRoutingDeviceConfig createConfig() {
                    return new SegmentRoutingDeviceConfig();
                }
            };

    private final ConfigFactory<ApplicationId, SegmentRoutingAppConfig> appConfigFactory =
            new ConfigFactory<ApplicationId, SegmentRoutingAppConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY,
                    SegmentRoutingAppConfig.class, "segmentrouting") {
                @Override
                public SegmentRoutingAppConfig createConfig() {
                    return new SegmentRoutingAppConfig();
                }
            };

    private ConfigFactory<ApplicationId, McastConfig> mcastConfigFactory =
            new ConfigFactory<ApplicationId, McastConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY,
                    McastConfig.class, "multicast") {
                @Override
                public McastConfig createConfig() {
                    return new McastConfig();
                }
            };

    /**
     * Segment Routing App ID.
     */
    public static final String APP_NAME = "org.onosproject.segmentrouting";

    /**
     * Minumum and maximum value of dummy VLAN ID to be allocated.
     */
    public static final int MIN_DUMMY_VLAN_ID = 2;
    public static final int MAX_DUMMY_VLAN_ID = 4093;

    private static final int DEFAULT_POOL_SIZE = 32;

    Instant lastEdgePortEvent = Instant.EPOCH;

    @Activate
    protected void activate(ComponentContext context) {
        appId = coreService.registerApplication(APP_NAME);

        mainEventExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/sr", "event-main-%d", log));
        hostEventExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/sr", "event-host-%d", log));
        routeEventExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/sr", "event-route-%d", log));
        mcastEventExecutor = Executors.newSingleThreadScheduledExecutor(
                groupedThreads("onos/sr", "event-mcast-%d", log));
        packetExecutor = Executors.newSingleThreadExecutor(groupedThreads("onos/sr", "packet-%d", log));
        neighborExecutor = Executors.newFixedThreadPool(DEFAULT_POOL_SIZE,
                groupedThreads("onos/sr", "neighbor-%d", log));

        log.debug("Creating EC map nsnextobjectivestore");
        EventuallyConsistentMapBuilder<DestinationSetNextObjectiveStoreKey, NextNeighbors>
                nsNextObjMapBuilder = storageService.eventuallyConsistentMapBuilder();
        dsNextObjStore = nsNextObjMapBuilder
                .withName("nsnextobjectivestore")
                .withSerializer(createSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.trace("Current size {}", dsNextObjStore.size());

        log.debug("Creating EC map vlannextobjectivestore");
        EventuallyConsistentMapBuilder<VlanNextObjectiveStoreKey, Integer>
                vlanNextObjMapBuilder = storageService.eventuallyConsistentMapBuilder();
        vlanNextObjStore = vlanNextObjMapBuilder
                .withName("vlannextobjectivestore")
                .withSerializer(createSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        log.debug("Creating EC map macvlannextobjectivestore");
        EventuallyConsistentMapBuilder<MacVlanNextObjectiveStoreKey, Integer>
                macVlanNextObjMapBuilder = storageService.eventuallyConsistentMapBuilder();
        macVlanNextObjStore = macVlanNextObjMapBuilder
                .withName("macvlannextobjectivestore")
                .withSerializer(createSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        log.debug("Creating EC map subnetnextobjectivestore");
        EventuallyConsistentMapBuilder<PortNextObjectiveStoreKey, Integer>
                portNextObjMapBuilder = storageService.eventuallyConsistentMapBuilder();
        portNextObjStore = portNextObjMapBuilder
                .withName("portnextobjectivestore")
                .withSerializer(createSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        EventuallyConsistentMapBuilder<String, Tunnel> tunnelMapBuilder =
                storageService.eventuallyConsistentMapBuilder();
        tunnelStore = tunnelMapBuilder
                .withName("tunnelstore")
                .withSerializer(createSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        EventuallyConsistentMapBuilder<String, Policy> policyMapBuilder =
                storageService.eventuallyConsistentMapBuilder();
        policyStore = policyMapBuilder
                .withName("policystore")
                .withSerializer(createSerializer())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        processor = new InternalPacketProcessor();
        linkListener = new InternalLinkListener();
        deviceListener = new InternalDeviceListener();
        appCfgHandler = new AppConfigHandler(this);
        mcastHandler = new McastHandler(this);
        hostHandler = new HostHandler(this);
        linkHandler = new LinkHandler(this);
        routeHandler = new RouteHandler(this);
        neighbourHandler = new SegmentRoutingNeighbourDispatcher(this);
        l2TunnelHandler = new DefaultL2TunnelHandler(this);
        topologyHandler = new TopologyHandler(this);

        compCfgService.preSetProperty("org.onosproject.provider.host.impl.HostLocationProvider",
                                      "requestInterceptsEnabled", "false", false);
        compCfgService.preSetProperty("org.onosproject.net.neighbour.impl.NeighbourResolutionManager",
                                      "requestInterceptsEnabled", "false", false);
        compCfgService.preSetProperty("org.onosproject.dhcprelay.DhcpRelayManager",
                                      "arpEnabled", "false", false);
        compCfgService.preSetProperty("org.onosproject.net.host.impl.HostManager",
                                      "greedyLearningIpv6", "true", false);
        compCfgService.preSetProperty("org.onosproject.routing.cpr.ControlPlaneRedirectManager",
                                      "forceUnprovision", "true", false);
        compCfgService.preSetProperty("org.onosproject.routeservice.store.RouteStoreImpl",
                                      "distributed", "true", false);
        compCfgService.preSetProperty("org.onosproject.provider.host.impl.HostLocationProvider",
                                      "multihomingEnabled", "true", false);
        compCfgService.preSetProperty("org.onosproject.provider.lldp.impl.LldpLinkProvider",
                                      "staleLinkAge", "15000", false);
        compCfgService.preSetProperty("org.onosproject.net.host.impl.HostManager",
                                      "allowDuplicateIps", "false", false);
        // For P4 switches
        compCfgService.preSetProperty("org.onosproject.net.flow.impl.FlowRuleManager",
                                      "fallbackFlowPollFrequency", "4", false);
        compCfgService.preSetProperty("org.onosproject.net.group.impl.GroupManager",
                                      "fallbackGroupPollFrequency", "3", false);
        compCfgService.registerProperties(getClass());
        modified(context);

        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(deviceConfigFactory);
        cfgService.registerConfigFactory(appConfigFactory);
        cfgService.registerConfigFactory(mcastConfigFactory);
        log.info("Configuring network before adding listeners");

        cfgListener.configureNetwork();

        hostService.addListener(hostListener);
        packetService.addProcessor(processor, PacketProcessor.director(2));
        linkService.addListener(linkListener);
        deviceService.addListener(deviceListener);
        multicastRouteService.addListener(mcastListener);
        routeService.addListener(routeListener);
        topologyService.addListener(topologyListener);
        mastershipService.addListener(mastershipListener);
        clusterService.addListener(clusterListener);

        linkHandler.init();
        l2TunnelHandler.init();

        synchronized (networkConfigCompletionLock) {
            networkConfigCompletion.whenComplete((value, ex) -> {
                //setting to null for easier fall through
                networkConfigCompletion = null;
                //process all queued events
                queuedEvents.forEach(event -> {
                    mainEventExecutor.execute(new InternalEventHandler(event));
                });
            });
        }

        log.info("Started");
    }

    KryoNamespace.Builder createSerializer() {
        return new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(DestinationSetNextObjectiveStoreKey.class,
                          VlanNextObjectiveStoreKey.class,
                          DestinationSet.class,
                          DestinationSet.DestinationSetType.class,
                          NextNeighbors.class,
                          Tunnel.class,
                          DefaultTunnel.class,
                          Policy.class,
                          TunnelPolicy.class,
                          Policy.Type.class,
                          PortNextObjectiveStoreKey.class,
                          XConnectStoreKey.class,
                          L2Tunnel.class,
                          L2TunnelPolicy.class,
                          DefaultL2Tunnel.class,
                          DefaultL2TunnelPolicy.class,
                          MacVlanNextObjectiveStoreKey.class
                );
    }

    @Deactivate
    protected void deactivate() {
        mainEventExecutor.shutdown();
        hostEventExecutor.shutdown();
        routeEventExecutor.shutdown();
        mcastEventExecutor.shutdown();
        packetExecutor.shutdown();
        neighborExecutor.shutdown();

        mainEventExecutor = null;
        hostEventExecutor = null;
        routeEventExecutor = null;
        mcastEventExecutor = null;
        packetExecutor = null;
        neighborExecutor = null;

        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(deviceConfigFactory);
        cfgService.unregisterConfigFactory(appConfigFactory);
        cfgService.unregisterConfigFactory(mcastConfigFactory);
        compCfgService.unregisterProperties(getClass(), false);

        hostService.removeListener(hostListener);
        packetService.removeProcessor(processor);
        linkService.removeListener(linkListener);
        deviceService.removeListener(deviceListener);
        multicastRouteService.removeListener(mcastListener);
        routeService.removeListener(routeListener);
        topologyService.removeListener(topologyListener);
        mastershipService.removeListener(mastershipListener);
        clusterService.removeListener(clusterListener);

        neighbourResolutionService.unregisterNeighbourHandlers(appId);

        processor = null;
        linkListener = null;
        deviceListener = null;
        groupHandlerMap.forEach((k, v) -> v.shutdown());
        groupHandlerMap.clear();
        defaultRoutingHandler.shutdown();

        dsNextObjStore.destroy();
        vlanNextObjStore.destroy();
        macVlanNextObjStore.destroy();
        portNextObjStore.destroy();
        tunnelStore.destroy();
        policyStore.destroy();

        mcastHandler.terminate();
        hostHandler.terminate();
        log.info("Stopped");
    }

    @Modified
    private void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        if (properties == null) {
            return;
        }

        String strActiveProbing = Tools.get(properties, PROP_ACTIVE_PROBING);
        boolean expectActiveProbing = Boolean.parseBoolean(strActiveProbing);
        if (expectActiveProbing != activeProbing) {
            activeProbing = expectActiveProbing;
            log.info("{} active probing", activeProbing ? "Enabling" : "Disabling");
        }


        String strSymmetricProbing = Tools.get(properties, PROP_SYMMETRIC_PROBING);
        boolean expectSymmetricProbing = Boolean.parseBoolean(strSymmetricProbing);
        if (expectSymmetricProbing != symmetricProbing) {
            symmetricProbing = expectSymmetricProbing;
            log.info("{} symmetric probing", symmetricProbing ? "Enabling" : "Disabling");
        }

        String strSingleHomedDown = Tools.get(properties, PROP_SINGLE_HOMED_DOWN);
        boolean expectSingleHomedDown = Boolean.parseBoolean(strSingleHomedDown);
        if (expectSingleHomedDown != singleHomedDown) {
            singleHomedDown = expectSingleHomedDown;
            log.info("{} downing of single homed hosts for lost uplinks",
                     singleHomedDown ? "Enabling" : "Disabling");
            if (singleHomedDown && linkHandler != null) {
                hostService.getHosts().forEach(host -> host.locations()
                        .forEach(loc -> {
                            if (interfaceService.isConfigured(loc)) {
                                linkHandler.checkUplinksForHost(loc);
                            }
                        }));
            } else {
                log.warn("Disabling singleHomedDown does not re-enable already "
                        + "downed ports for single-homed hosts");
            }
        }

        String strRespondToUnknownHosts = Tools.get(properties, PROP_RESPOND_TO_UNKNOWN_HOSTS);
        boolean expectRespondToUnknownHosts = Boolean.parseBoolean(strRespondToUnknownHosts);
        if (expectRespondToUnknownHosts != respondToUnknownHosts) {
            respondToUnknownHosts = expectRespondToUnknownHosts;
            log.info("{} responding to ARPs/NDPs from unknown hosts", respondToUnknownHosts ? "Enabling" : "Disabling");
        }

        String strRouteDoubleTaggedHosts = Tools.get(properties, PROP_ROUTE_DOUBLE_TAGGED_HOSTS);
        boolean expectRouteDoubleTaggedHosts = Boolean.parseBoolean(strRouteDoubleTaggedHosts);
        if (expectRouteDoubleTaggedHosts != routeDoubleTaggedHosts) {
            routeDoubleTaggedHosts = expectRouteDoubleTaggedHosts;
            log.info("{} routing for double tagged hosts", routeDoubleTaggedHosts ? "Enabling" : "Disabling");

            if (routeDoubleTaggedHosts) {
                hostHandler.populateAllDoubleTaggedHost();
            } else {
                hostHandler.revokeAllDoubleTaggedHost();
            }
        }

        String strDefaultInternalVlan = Tools.get(properties, PROP_DEFAULT_INTERNAL_VLAN);
        int defIntVlan = Integer.parseInt(strDefaultInternalVlan);
        if (defIntVlan != defaultInternalVlan) {
            if (canUseVlanId(defIntVlan)) {
                log.warn("Default internal vlan value changed from {} to {}.. "
                        + "re-programming filtering rules, but NOT any groups already "
                        + "created with the former value", defaultInternalVlan, defIntVlan);
                VlanId oldDefIntVlan = VlanId.vlanId((short) defaultInternalVlan);
                defaultInternalVlan = defIntVlan;
                routingRulePopulator
                .updateSpecialVlanFilteringRules(true, oldDefIntVlan,
                                                 VlanId.vlanId((short) defIntVlan));
            } else {
                log.warn("Cannot change default internal vlan to unusable "
                        + "value {}", defIntVlan);
            }
        }

        String strPwTxpVlan = Tools.get(properties, PROP_PW_TRANSPORT_VLAN);
        int pwTxpVlan = Integer.parseInt(strPwTxpVlan);
        if (pwTxpVlan != pwTransportVlan) {
            if (canUseVlanId(pwTxpVlan)) {
                log.warn("Pseudowire transport vlan value changed from {} to {}.. "
                        + "re-programming filtering rules, but NOT any groups already "
                        + "created with the former value", pwTransportVlan,
                        pwTxpVlan);
                VlanId oldPwTxpVlan = VlanId.vlanId((short) pwTransportVlan);
                pwTransportVlan = pwTxpVlan;
                routingRulePopulator
                .updateSpecialVlanFilteringRules(false, oldPwTxpVlan,
                                                 VlanId.vlanId((short) pwTxpVlan));
            } else {
                log.warn("Cannot change pseudowire transport vlan to unusable "
                        + "value {}", pwTxpVlan);
            }
        }

        String strRouteSimplification = Tools.get(properties, PROP_ROUTE_SIMPLIFICATION);
        boolean expectRouteSimplification = Boolean.parseBoolean(strRouteSimplification);
        if (expectRouteSimplification != routeSimplification) {
            routeSimplification = expectRouteSimplification;
            log.info("{} route simplification", routeSimplification ? "Enabling" : "Disabling");
        }

    }

    /**
     * Returns true if given vlan id is not being used in the system currently,
     * either as one of the default system wide vlans or as one of the
     * configured interface vlans.
     *
     * @param vlanId given vlan id
     * @return true if vlan is not currently in use
     */
    public boolean canUseVlanId(int vlanId) {
        if (vlanId >= 4095 || vlanId <= 1) {
            log.error("Vlan id {} value is not in valid range 2 <--> 4094",
                      vlanId);
            return false;
        }

       VlanId vid = VlanId.vlanId((short) vlanId);
        if (getDefaultInternalVlan().equals(vid) || getPwTransportVlan().equals(vid)) {
            log.warn("Vlan id {} value is already in use system-wide. "
                    + "DefaultInternalVlan:{} PwTransportVlan:{} ", vlanId,
                     getDefaultInternalVlan(), getPwTransportVlan());
            return false;
        }

        if (interfaceService.inUse(vid)) {
            log.warn("Vlan id {} value is already in use on a configured "
                    + "interface in the system", vlanId);
            return false;
        }
        return true;
    }

    /**
     * Returns the VlanId assigned internally by default to unconfigured ports.
     *
     * @return the default internal vlan id
     */
    public VlanId getDefaultInternalVlan() {
        return VlanId.vlanId((short) defaultInternalVlan);
    }

    /**
     * Returns the Vlan id used to transport pseudowire traffic across the
     * network.
     *
     * @return the pseudowire transport vlan id
     */
    public VlanId getPwTransportVlan() {
        return VlanId.vlanId((short) pwTransportVlan);
    }

    @Override
    public List<Tunnel> getTunnels() {
        return tunnelHandler.getTunnels();
    }

    @Override
    public TunnelHandler.Result createTunnel(Tunnel tunnel) {
        return tunnelHandler.createTunnel(tunnel);
    }

    @Override
    public TunnelHandler.Result removeTunnel(Tunnel tunnel) {
        for (Policy policy: policyHandler.getPolicies()) {
            if (policy.type() == Policy.Type.TUNNEL_FLOW) {
                TunnelPolicy tunnelPolicy = (TunnelPolicy) policy;
                if (tunnelPolicy.tunnelId().equals(tunnel.id())) {
                    log.warn("Cannot remove the tunnel used by a policy");
                    return TunnelHandler.Result.TUNNEL_IN_USE;
                }
            }
        }
        return tunnelHandler.removeTunnel(tunnel);
    }

    @Override
    public PolicyHandler.Result removePolicy(Policy policy) {
        return policyHandler.removePolicy(policy);
    }

    @Override
    public PolicyHandler.Result createPolicy(Policy policy) {
        return policyHandler.createPolicy(policy);
    }

    @Override
    public List<Policy> getPolicies() {
        return policyHandler.getPolicies();
    }

    @Override
    public Set<L2TunnelDescription> getL2TunnelDescriptions(boolean pending) {
        return l2TunnelHandler.getL2Descriptions(pending);
    }

    @Override
    public List<L2Tunnel> getL2Tunnels() {
        return l2TunnelHandler.getL2Tunnels();
    }

    @Override
    public List<L2TunnelPolicy> getL2Policies() {
        return l2TunnelHandler.getL2Policies();
    }

    @Override
    @Deprecated
    public L2TunnelHandler.Result addPseudowiresBulk(List<DefaultL2TunnelDescription> bulkPseudowires) {

        // get both added and pending pseudowires
        List<L2TunnelDescription> pseudowires = new ArrayList<>();
        pseudowires.addAll(l2TunnelHandler.getL2Descriptions(false));
        pseudowires.addAll(l2TunnelHandler.getL2Descriptions(true));
        pseudowires.addAll(bulkPseudowires);

        Set<L2TunnelDescription> newPseudowires = new HashSet(bulkPseudowires);

        L2TunnelHandler.Result retRes = L2TunnelHandler.Result.SUCCESS;
        L2TunnelHandler.Result res;
        for (DefaultL2TunnelDescription pw : bulkPseudowires) {
            res = addPseudowire(pw);
            if (res != L2TunnelHandler.Result.SUCCESS) {
                log.error("Pseudowire with id {} can not be instantiated !", res);
                retRes = res;
            }
        }

        return retRes;
    }

    @Override
    public L2TunnelHandler.Result addPseudowire(L2TunnelDescription l2TunnelDescription) {
        return l2TunnelHandler.deployPseudowire(l2TunnelDescription);
    }

    @Override
    public L2TunnelHandler.Result removePseudowire(Integer pwId) {
        return l2TunnelHandler.tearDownPseudowire(pwId);
    }

    @Override
    public void rerouteNetwork() {
        cfgListener.configureNetwork();
    }

    @Override
    public Map<DeviceId, Set<IpPrefix>> getDeviceSubnetMap() {
        Map<DeviceId, Set<IpPrefix>> deviceSubnetMap = Maps.newHashMap();
        deviceConfiguration.getRouters().forEach(device ->
            deviceSubnetMap.put(device, deviceConfiguration.getSubnets(device)));
        return deviceSubnetMap;
    }


    @Override
    public ImmutableMap<DeviceId, EcmpShortestPathGraph> getCurrentEcmpSpg() {
        if (defaultRoutingHandler != null) {
            return defaultRoutingHandler.getCurrentEmcpSpgMap();
        } else {
            return null;
        }
    }

    @Override
    public ImmutableMap<DestinationSetNextObjectiveStoreKey, NextNeighbors> getDstNextObjStore() {
        if (dsNextObjStore != null) {
            return ImmutableMap.copyOf(dsNextObjStore.entrySet());
        } else {
            return ImmutableMap.of();
        }
    }

    @Override
    public ImmutableMap<VlanNextObjectiveStoreKey, Integer> getVlanNextObjStore() {
        if (vlanNextObjStore != null) {
            return ImmutableMap.copyOf(vlanNextObjStore.entrySet());
        } else {
            return ImmutableMap.of();
        }
    }

    @Override
    public ImmutableMap<MacVlanNextObjectiveStoreKey, Integer> getMacVlanNextObjStore() {
        if (macVlanNextObjStore != null) {
            return ImmutableMap.copyOf(macVlanNextObjStore.entrySet());
        } else {
            return ImmutableMap.of();
        }
    }

    @Override
    public ImmutableMap<PortNextObjectiveStoreKey, Integer> getPortNextObjStore() {
        if (portNextObjStore != null) {
            return ImmutableMap.copyOf(portNextObjStore.entrySet());
        } else {
            return ImmutableMap.of();
        }
    }

    @Override
    public ImmutableMap<String, NextObjective> getPwInitNext() {
        if (l2TunnelHandler != null) {
            return l2TunnelHandler.getInitNext();
        } else {
            return ImmutableMap.of();
        }
    }

    @Override
    public ImmutableMap<String, NextObjective> getPwTermNext() {
        if (l2TunnelHandler != null) {
            return l2TunnelHandler.getTermNext();
        } else {
            return ImmutableMap.of();
        }
    }

    @Override
    public void invalidateNextObj(int nextId) {
        if (dsNextObjStore != null) {
            dsNextObjStore.entrySet().forEach(e -> {
                if (e.getValue().nextId() == nextId) {
                    dsNextObjStore.remove(e.getKey());
                }
            });
        }
        if (vlanNextObjStore != null) {
            vlanNextObjStore.entrySet().forEach(e -> {
                if (e.getValue() == nextId) {
                    vlanNextObjStore.remove(e.getKey());
                }
            });
        }
        if (macVlanNextObjStore != null) {
            macVlanNextObjStore.entrySet().forEach(e -> {
                if (e.getValue() == nextId) {
                    macVlanNextObjStore.remove(e.getKey());
                }
            });
        }
        if (portNextObjStore != null) {
            portNextObjStore.entrySet().forEach(e -> {
                if (e.getValue() == nextId) {
                    portNextObjStore.remove(e.getKey());
                }
            });
        }
        if (mcastHandler != null) {
            mcastHandler.removeNextId(nextId);
        }
        if (l2TunnelHandler != null) {
            l2TunnelHandler.removeNextId(nextId);
        }
        if (xconnectService != null) {
            xconnectService.removeNextId(nextId);
        }
    }

    @Override
    public void verifyGroups(DeviceId id) {
        DefaultGroupHandler gh = groupHandlerMap.get(id);
        if (gh != null) {
            gh.triggerBucketCorrector();
        }
    }

    @Override
    public ImmutableMap<Link, Boolean> getSeenLinks() {
        return linkHandler.getSeenLinks();
    }

    @Override
    public ImmutableMap<DeviceId, Set<PortNumber>> getDownedPortState() {
        return linkHandler.getDownedPorts();
    }

    @Override
    public Map<McastStoreKey, Integer> getMcastNextIds(IpAddress mcastIp) {
        return mcastHandler.getNextIds(mcastIp);
    }

    @Override
    public Map<McastRoleStoreKey, McastRole> getMcastRoles(IpAddress mcastIp, ConnectPoint sourcecp) {
        return mcastHandler.getMcastRoles(mcastIp, sourcecp);
    }

    @Override
    public Multimap<ConnectPoint, List<ConnectPoint>> getMcastTrees(IpAddress mcastIp,
                                                                    ConnectPoint sourcecp) {
        return mcastHandler.getMcastTrees(mcastIp, sourcecp);
    }

    @Override
    public Map<IpAddress, NodeId> getMcastLeaders(IpAddress mcastIp) {
        return mcastHandler.getMcastLeaders(mcastIp);
    }

    @Override
    public Map<DeviceId, List<McastFilteringObjStoreKey>> getMcastFilters() {
        return mcastHandler.getMcastFilters();
    }

    @Override
    public Map<Set<DeviceId>, NodeId> getShouldProgram() {
        return defaultRoutingHandler == null ? ImmutableMap.of() :
                ImmutableMap.copyOf(defaultRoutingHandler.shouldProgram);
    }

    @Override
    public Map<DeviceId, Boolean> getShouldProgramCache() {
        return defaultRoutingHandler == null ? ImmutableMap.of() :
                ImmutableMap.copyOf(defaultRoutingHandler.shouldProgramCache);
    }

    @Override
    public boolean shouldProgram(DeviceId deviceId) {
        return defaultRoutingHandler.shouldProgram(deviceId);
    }

    @Override
    public ApplicationId appId() {
        return appId;
    }

    /**
     * Returns the device configuration.
     *
     * @return device configuration
     */
    public DeviceConfiguration deviceConfiguration() {
        return deviceConfiguration;
    }

    /**
     * Per device next objective ID store with (device id + destination set) as key.
     * Used to keep track on MPLS group information.
     *
     * @return next objective ID store
     */
    public EventuallyConsistentMap<DestinationSetNextObjectiveStoreKey, NextNeighbors>
                dsNextObjStore() {
        return dsNextObjStore;
    }

    /**
     * Per device next objective ID store with (device id + vlanid) as key.
     * Used to keep track on L2 flood group information.
     *
     * @return vlan next object store
     */
    public EventuallyConsistentMap<VlanNextObjectiveStoreKey, Integer> vlanNextObjStore() {
        return vlanNextObjStore;
    }

    /**
     * Per device next objective ID store with (device id + MAC address + vlan) as key.
     * Used to keep track on L3 Unicast group information for indirect hosts.
     *
     * @return mac vlan next object store
     */
    public EventuallyConsistentMap<MacVlanNextObjectiveStoreKey, Integer> macVlanNextObjStore() {
        return macVlanNextObjStore;
    }

    /**
     * Per device next objective ID store with (device id + port + treatment + meta) as key.
     * Used to keep track on L2 interface group and L3 unicast group information for direct hosts.
     *
     * @return port next object store.
     */
    public EventuallyConsistentMap<PortNextObjectiveStoreKey, Integer> portNextObjStore() {
        return portNextObjStore;
    }

    /**
     * Returns the MPLS-ECMP configuration which indicates whether ECMP on
     * labeled packets should be programmed or not.
     *
     * @return MPLS-ECMP value
     */
    public boolean getMplsEcmp() {
        SegmentRoutingAppConfig segmentRoutingAppConfig = cfgService
                .getConfig(this.appId, SegmentRoutingAppConfig.class);
        return segmentRoutingAppConfig != null && segmentRoutingAppConfig.mplsEcmp();
    }

    /**
     * Returns the tunnel object with the tunnel ID.
     *
     * @param tunnelId Tunnel ID
     * @return Tunnel reference
     */
    public Tunnel getTunnel(String tunnelId) {
        return tunnelHandler.getTunnel(tunnelId);
    }

    @Override
    public VlanId getInternalVlanId(ConnectPoint connectPoint) {
        VlanId untaggedVlanId = interfaceService.getUntaggedVlanId(connectPoint);
        VlanId nativeVlanId = interfaceService.getNativeVlanId(connectPoint);
        return untaggedVlanId != null ? untaggedVlanId : nativeVlanId;
    }

    @Override
    public Optional<DeviceId> getPairDeviceId(DeviceId deviceId) {
        SegmentRoutingDeviceConfig deviceConfig =
                cfgService.getConfig(deviceId, SegmentRoutingDeviceConfig.class);
        return Optional.ofNullable(deviceConfig).map(SegmentRoutingDeviceConfig::pairDeviceId);
    }

    @Override
    public Optional<PortNumber> getPairLocalPort(DeviceId deviceId) {
        SegmentRoutingDeviceConfig deviceConfig =
                cfgService.getConfig(deviceId, SegmentRoutingDeviceConfig.class);
        return Optional.ofNullable(deviceConfig).map(SegmentRoutingDeviceConfig::pairLocalPort);
    }

    /**
     * Returns locations of given resolved route.
     *
     * @param resolvedRoute resolved route
     * @return locations of nexthop. Might be empty if next hop is not found
     */
    public Set<ConnectPoint> nextHopLocations(ResolvedRoute resolvedRoute) {
        HostId hostId = HostId.hostId(resolvedRoute.nextHopMac(), resolvedRoute.nextHopVlan());
        return Optional.ofNullable(hostService.getHost(hostId))
                .map(Host::locations).orElse(Sets.newHashSet())
                .stream().map(l -> (ConnectPoint) l).collect(Collectors.toSet());
    }

    /**
     * Returns vlan port map of given device.
     *
     * @param deviceId device id
     * @return vlan-port multimap
     */
    public Multimap<VlanId, PortNumber> getVlanPortMap(DeviceId deviceId) {
        HashMultimap<VlanId, PortNumber> vlanPortMap = HashMultimap.create();

        interfaceService.getInterfaces().stream()
                .filter(intf -> intf.connectPoint().deviceId().equals(deviceId))
                .forEach(intf -> {
                    vlanPortMap.put(intf.vlanUntagged(), intf.connectPoint().port());
                    intf.vlanTagged().forEach(vlanTagged ->
                        vlanPortMap.put(vlanTagged, intf.connectPoint().port())
                    );
                    vlanPortMap.put(intf.vlanNative(), intf.connectPoint().port());
                });
        vlanPortMap.removeAll(VlanId.NONE);

        return vlanPortMap;
    }

    /**
     * Returns the next objective ID for the given vlan id. It is expected
     * that the next-objective has been pre-created from configuration.
     *
     * @param deviceId Device ID
     * @param vlanId VLAN ID
     * @return next objective ID or -1 if it was not found
     */
    int getVlanNextObjectiveId(DeviceId deviceId, VlanId vlanId) {
        if (groupHandlerMap.get(deviceId) != null) {
            log.trace("getVlanNextObjectiveId query in device {}", deviceId);
            return groupHandlerMap.get(deviceId).getVlanNextObjectiveId(vlanId);
        } else {
            log.warn("getVlanNextObjectiveId query - groupHandler for "
                    + "device {} not found", deviceId);
            return -1;
        }
    }

    /**
     * Returns the next objective ID for the given portNumber, given the treatment.
     * There could be multiple different treatments to the same outport, which
     * would result in different objectives. If the next object does not exist,
     * and should be created, a new one is created and its id is returned.
     *
     * @param deviceId Device ID
     * @param portNum port number on device for which NextObjective is queried
     * @param treatment the actions to apply on the packets (should include outport)
     * @param meta metadata passed into the creation of a Next Objective if necessary
     * @param createIfMissing true if a next object should be created if not found
     * @return next objective ID or -1 if an error occurred during retrieval or creation
     */
    public int getPortNextObjectiveId(DeviceId deviceId, PortNumber portNum,
                                      TrafficTreatment treatment,
                                      TrafficSelector meta,
                                      boolean createIfMissing) {
        DefaultGroupHandler ghdlr = groupHandlerMap.get(deviceId);
        if (ghdlr != null) {
            return ghdlr.getPortNextObjectiveId(portNum, treatment, meta, createIfMissing);
        } else {
            log.warn("getPortNextObjectiveId query - groupHandler for device {}"
                    + " not found", deviceId);
            return -1;
        }
    }

    /**
     * Returns the next Objective ID for the given mac and vlan, given the treatment.
     * There could be multiple different treatments to the same outport, which
     * would result in different objectives. If the next object does not exist,
     * and should be created, a new one is created and its id is returned.
     *
     * @param deviceId Device ID
     * @param macAddr mac of host for which Next ID is required.
     * @param vlanId vlan of host for which Next ID is required.
     * @param port port with which to create the Next Obj.
     * @param createIfMissing true if a next object should be created if not found
     * @return next objective ID or -1 if an error occurred during retrieval or creation
     */
    public int getMacVlanNextObjectiveId(DeviceId deviceId, MacAddress macAddr, VlanId vlanId,
                                      PortNumber port, boolean createIfMissing) {
        DefaultGroupHandler ghdlr = groupHandlerMap.get(deviceId);
        if (ghdlr != null) {
            return ghdlr.getMacVlanNextObjectiveId(macAddr, vlanId, port, createIfMissing);
        } else {
            log.warn("getMacVlanNextObjectiveId query - groupHandler for device {}"
                    + " not found", deviceId);
            return -1;
        }
    }

    /**
     * Updates the next objective for the given nextId .
     *
     * @param deviceId Device ID
     * @param hostMac mac of host for which Next obj is to be updated.
     * @param hostVlanId vlan of host for which Next obj is to be updated.
     * @param port port with which to update the Next Obj.
     * @param nextId of Next Obj which needs to be updated.
     */
    public void updateMacVlanTreatment(DeviceId deviceId, MacAddress hostMac,
                             VlanId hostVlanId, PortNumber port, int nextId) {
        // Check if we are the king of this device
        // just one instance should perform this update
        if (!defaultRoutingHandler.shouldProgram(deviceId)) {
            log.debug("This instance is not handling the routing towards the "
                              + "device {}", deviceId);
            return;
        }
        // Get the handler and perform the update
        DefaultGroupHandler ghdlr = groupHandlerMap.get(deviceId);
        if (ghdlr != null) {
            ghdlr.updateL3UcastGroupBucket(hostMac, hostVlanId, port, nextId);
        } else {
            log.warn("updateL3UcastGroupBucket query - groupHandler for device {}"
                    + " not found", deviceId);
        }
    }


    /**
     * Returns the group handler object for the specified device id.
     *
     * @param devId the device identifier
     * @return the groupHandler object for the device id, or null if not found
     */
    DefaultGroupHandler getGroupHandler(DeviceId devId) {
        return groupHandlerMap.get(devId);
    }

    /**
     * Returns the default routing handler object.
     *
     * @return the default routing handler object
     */
    public DefaultRoutingHandler getRoutingHandler() {
        return defaultRoutingHandler;
    }

    private class InternalPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            packetExecutor.execute(() -> processPacketInternal(context));
        }

        private void processPacketInternal(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();

            if (ethernet == null) {
                return;
            }

            log.trace("Rcvd pktin from {}: {}", context.inPacket().receivedFrom(),
                      ethernet);
            if (ethernet.getEtherType() == TYPE_ARP) {
                log.warn("Received unexpected ARP packet on {}",
                         context.inPacket().receivedFrom());
                log.trace("{}", ethernet);
                return;
            } else if (ethernet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipv4Packet = (IPv4) ethernet.getPayload();
                //ipHandler.addToPacketBuffer(ipv4Packet);
                if (ipv4Packet.getProtocol() == IPv4.PROTOCOL_ICMP) {
                    icmpHandler.processIcmp(ethernet, pkt.receivedFrom());
                } else {
                    // NOTE: We don't support IP learning at this moment so this
                    //       is not necessary. Also it causes duplication of DHCP packets.
                    // ipHandler.processPacketIn(ipv4Packet, pkt.receivedFrom());
                }
            } else if (ethernet.getEtherType() == Ethernet.TYPE_IPV6) {
                IPv6 ipv6Packet = (IPv6) ethernet.getPayload();
                //ipHandler.addToPacketBuffer(ipv6Packet);
                // We deal with the packet only if the packet is a ICMP6 ECHO/REPLY
                if (ipv6Packet.getNextHeader() == IPv6.PROTOCOL_ICMP6) {
                    ICMP6 icmp6Packet = (ICMP6) ipv6Packet.getPayload();
                    if (icmp6Packet.getIcmpType() == ICMP6.ECHO_REQUEST ||
                            icmp6Packet.getIcmpType() == ICMP6.ECHO_REPLY) {
                        icmpHandler.processIcmpv6(ethernet, pkt.receivedFrom());
                    } else {
                        log.trace("Received ICMPv6 0x{} - not handled",
                                Integer.toHexString(icmp6Packet.getIcmpType() & 0xff));
                    }
                } else {
                   // NOTE: We don't support IP learning at this moment so this
                   //       is not necessary. Also it causes duplication of DHCPv6 packets.
                   // ipHandler.processPacketIn(ipv6Packet, pkt.receivedFrom());
                }
            }
        }
    }

    private class InternalEventHandler implements Runnable {
        private Event event;

        InternalEventHandler(Event event) {
            this.event = event;
        }

        @Override
        public void run() {
            try {
                // TODO We should also change SR routing and PW to listen to TopologyEvents
                if (event.type() == LinkEvent.Type.LINK_ADDED ||
                        event.type() == LinkEvent.Type.LINK_UPDATED) {
                    linkHandler.processLinkAdded((Link) event.subject());
                } else if (event.type() == LinkEvent.Type.LINK_REMOVED) {
                    linkHandler.processLinkRemoved((Link) event.subject());
                } else if (event.type() == DeviceEvent.Type.DEVICE_ADDED ||
                        event.type() == DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED ||
                        event.type() == DeviceEvent.Type.DEVICE_UPDATED) {
                    DeviceId deviceId = ((Device) event.subject()).id();
                    if (deviceService.isAvailable(deviceId)) {
                        log.info("** DEVICE UP Processing device event {} "
                                + "for available device {}",
                                 event.type(), ((Device) event.subject()).id());
                        processDeviceAdded((Device) event.subject());
                    } else {
                        if (event.type() == DeviceEvent.Type.DEVICE_ADDED) {
                            // Note: For p4 devices, the device will be added but unavailable at the beginning.
                            //       The device will later on being marked as available once the pipeline is pushed
                            //       to the device.
                            log.info("** DEVICE ADDED but unavailable. Ignore");
                            return;
                        }
                        log.info(" ** DEVICE DOWN Processing device event {}"
                                + " for unavailable device {}",
                                 event.type(), ((Device) event.subject()).id());
                        processDeviceRemoved((Device) event.subject());
                    }
                } else if (event.type() == DeviceEvent.Type.PORT_ADDED) {
                    // typically these calls come when device is added first time
                    // so port filtering rules are handled at the device_added event.
                    // port added calls represent all ports on the device,
                    // enabled or not.
                    log.trace("** PORT ADDED {}/{} -> {}",
                              ((DeviceEvent) event).subject().id(),
                              ((DeviceEvent) event).port().number(),
                              event.type());
                } else if (event.type() == DeviceEvent.Type.PORT_UPDATED) {
                    // these calls happen for every subsequent event
                    // ports enabled, disabled, switch goes away, comes back
                    log.info("** PORT UPDATED {}/{} -> {}",
                             event.subject(),
                             ((DeviceEvent) event).port(),
                             event.type());
                    processPortUpdatedInternal(((Device) event.subject()),
                                       ((DeviceEvent) event).port());
                    mcastHandler.processPortUpdate(((Device) event.subject()),
                                                   ((DeviceEvent) event).port());
                } else if (event.type() == TopologyEvent.Type.TOPOLOGY_CHANGED) {
                    // Process topology event, needed for all modules relying on
                    // topology service for path computation
                    TopologyEvent topologyEvent = (TopologyEvent) event;
                    log.info("Processing topology event {}, topology age {}, reasons {}",
                             event.type(), topologyEvent.subject().time(),
                             topologyEvent.reasons().size());
                    topologyHandler.processTopologyChange(topologyEvent.reasons());
                } else if (event.type() == HostEvent.Type.HOST_ADDED) {
                    hostHandler.processHostAddedEvent((HostEvent) event);
                } else if (event.type() == HostEvent.Type.HOST_MOVED) {
                    hostHandler.processHostMovedEvent((HostEvent) event);
                    routeHandler.processHostMovedEvent((HostEvent) event);
                } else if (event.type() == HostEvent.Type.HOST_AUX_MOVED) {
                    hostHandler.processHostMovedEvent((HostEvent) event);
                    // TODO RouteHandler also needs to process this event in order to
                    //      support nexthops that has auxLocations
                } else if (event.type() == HostEvent.Type.HOST_REMOVED) {
                    hostHandler.processHostRemovedEvent((HostEvent) event);
                } else if (event.type() == HostEvent.Type.HOST_UPDATED) {
                    hostHandler.processHostUpdatedEvent((HostEvent) event);
                } else if (event.type() == RouteEvent.Type.ROUTE_ADDED) {
                    routeHandler.processRouteAdded((RouteEvent) event);
                } else if (event.type() == RouteEvent.Type.ROUTE_UPDATED) {
                    routeHandler.processRouteUpdated((RouteEvent) event);
                } else if (event.type() == RouteEvent.Type.ROUTE_REMOVED) {
                    routeHandler.processRouteRemoved((RouteEvent) event);
                } else if (event.type() == RouteEvent.Type.ALTERNATIVE_ROUTES_CHANGED) {
                    routeHandler.processAlternativeRoutesChanged((RouteEvent) event);
                } else if (event.type() == McastEvent.Type.SOURCES_ADDED ||
                        event.type() == McastEvent.Type.SOURCES_REMOVED ||
                        event.type() == McastEvent.Type.SINKS_ADDED ||
                        event.type() == McastEvent.Type.SINKS_REMOVED ||
                        event.type() == McastEvent.Type.ROUTE_ADDED ||
                        event.type() == McastEvent.Type.ROUTE_REMOVED) {
                    mcastHandler.processMcastEvent((McastEvent) event);
                } else if (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED) {
                    NetworkConfigEvent netcfgEvent = (NetworkConfigEvent) event;
                    Class configClass = netcfgEvent.configClass();
                    if (configClass.equals(SegmentRoutingAppConfig.class)) {
                        appCfgHandler.processAppConfigAdded(netcfgEvent);
                        log.info("App config event .. configuring network");
                        cfgListener.configureNetwork();
                    } else if (configClass.equals(SegmentRoutingDeviceConfig.class)) {
                        log.info("Segment Routing Device Config added for {}", event.subject());
                        cfgListener.configureNetwork();
                    } else if (configClass.equals(InterfaceConfig.class)) {
                        log.info("Interface Config added for {}", event.subject());
                        cfgListener.configureNetwork();
                    } else {
                        log.error("Unhandled config class: {}", configClass);
                    }
                } else if (event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) {
                    NetworkConfigEvent netcfgEvent = (NetworkConfigEvent) event;
                    Class configClass = netcfgEvent.configClass();
                    if (configClass.equals(SegmentRoutingAppConfig.class)) {
                        appCfgHandler.processAppConfigUpdated(netcfgEvent);
                        log.info("App config event .. configuring network");
                        cfgListener.configureNetwork();
                    } else if (configClass.equals(SegmentRoutingDeviceConfig.class)) {
                        log.info("Segment Routing Device Config updated for {}", event.subject());
                        createOrUpdateDeviceConfiguration();
                    } else if (configClass.equals(InterfaceConfig.class)) {
                        log.info("Interface Config updated for {}", event.subject());
                        createOrUpdateDeviceConfiguration();
                        updateInterface((InterfaceConfig) netcfgEvent.config().get(),
                                (InterfaceConfig) netcfgEvent.prevConfig().get());
                    } else {
                        log.error("Unhandled config class: {}", configClass);
                    }
                } else if (event.type() == NetworkConfigEvent.Type.CONFIG_REMOVED) {
                    NetworkConfigEvent netcfgEvent = (NetworkConfigEvent) event;
                    Class configClass = netcfgEvent.configClass();
                    if (configClass.equals(SegmentRoutingAppConfig.class)) {
                        appCfgHandler.processAppConfigRemoved(netcfgEvent);
                        log.info("App config event .. configuring network");
                        cfgListener.configureNetwork();
                    } else if (configClass.equals(SegmentRoutingDeviceConfig.class)) {
                        // TODO Handle sr device config removal
                        log.info("SegmentRoutingDeviceConfig removal is not handled in current implementation");
                    } else if (configClass.equals(InterfaceConfig.class)) {
                        // TODO Handle interface removal
                        log.info("InterfaceConfig removal is not handled in current implementation");
                    } else {
                        log.error("Unhandled config class: {}", configClass);
                    }
                } else if (event.type() == MastershipEvent.Type.MASTER_CHANGED) {
                    MastershipEvent me = (MastershipEvent) event;
                    DeviceId deviceId = me.subject();
                    Optional<DeviceId> pairDeviceId = getPairDeviceId(deviceId);
                    log.info(" ** MASTERSHIP CHANGED Invalidating shouldProgram cache"
                            + " for {}/pair={} due to change", deviceId, pairDeviceId);
                    defaultRoutingHandler.invalidateShouldProgramCache(deviceId);
                    pairDeviceId.ifPresent(defaultRoutingHandler::invalidateShouldProgramCache);
                    defaultRoutingHandler.checkFullRerouteForMasterChange(deviceId, me);
                } else {
                    log.warn("Unhandled event type: {}", event.type());
                }
            } catch (Exception e) {
                log.error("SegmentRouting event handler thread thrown an exception: {}",
                          e.getMessage(), e);
            }
        }
    }

    void processDeviceAdded(Device device) {
        log.info("** DEVICE ADDED with ID {}", device.id());

        // NOTE: Punt ARP/NDP even when the device is not configured.
        //       Host learning without network config is required for CORD config generator.
        routingRulePopulator.populateIpPunts(device.id());
        routingRulePopulator.populateArpNdpPunts(device.id());

        if (deviceConfiguration == null || !deviceConfiguration.isConfigured(device.id())) {
            log.warn("Device configuration unavailable. Device {} will be "
                    + "processed after configuration.", device.id());
            return;
        }
        processDeviceAddedInternal(device.id());
    }

    private void processDeviceAddedInternal(DeviceId deviceId) {
        // Irrespective of whether the local is a MASTER or not for this device,
        // we need to create a SR-group-handler instance. This is because in a
        // multi-instance setup, any instance can initiate forwarding/next-objectives
        // for any switch (even if this instance is a SLAVE or not even connected
        // to the switch). To handle this, a default-group-handler instance is necessary
        // per switch.
        log.debug("Current groupHandlerMap devs: {}", groupHandlerMap.keySet());
        if (groupHandlerMap.get(deviceId) == null) {
            DefaultGroupHandler groupHandler;
            try {
                groupHandler = DefaultGroupHandler.
                        createGroupHandler(deviceId,
                                appId,
                                deviceConfiguration,
                                linkService,
                                flowObjectiveService,
                                this);
            } catch (DeviceConfigNotFoundException e) {
                log.warn(e.getMessage() + " Aborting processDeviceAdded.");
                return;
            }
            log.debug("updating groupHandlerMap with new grpHdlr for device: {}",
                    deviceId);
            groupHandlerMap.put(deviceId, groupHandler);
        }

        if (mastershipService.isLocalMaster(deviceId)) {
            defaultRoutingHandler.populatePortAddressingRules(deviceId);
            defaultRoutingHandler.purgeSeenBeforeRoutes(deviceId);
            DefaultGroupHandler groupHandler = groupHandlerMap.get(deviceId);
            groupHandler.createGroupsFromVlanConfig();
            routingRulePopulator.populateSubnetBroadcastRule(deviceId);
        }

        appCfgHandler.init(deviceId);
        hostEventExecutor.execute(() -> hostHandler.init(deviceId));
        routeEventExecutor.execute(() -> routeHandler.init(deviceId));
    }

    private void processDeviceRemoved(Device device) {
        dsNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(device.id()))
                .forEach(entry -> dsNextObjStore.remove(entry.getKey()));
        vlanNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(device.id()))
                .forEach(entry -> vlanNextObjStore.remove(entry.getKey()));
        macVlanNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(device.id()))
                .forEach(entry -> macVlanNextObjStore.remove(entry.getKey()));
        portNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(device.id()))
                .forEach(entry -> portNextObjStore.remove(entry.getKey()));
        linkHandler.processDeviceRemoved(device);

        DefaultGroupHandler gh = groupHandlerMap.remove(device.id());
        if (gh != null) {
            gh.shutdown();
        }
        // Note that a switch going down is associated with all of its links
        // going down as well, but it is treated as a single switch down event
        // while the link-downs are ignored. We cannot rely on the ordering of
        // events - i.e we cannot expect all link-downs to come before the
        // switch down - so we purge all seen-links for the switch before
        // handling route-path changes for the switch-down
        defaultRoutingHandler
            .populateRoutingRulesForLinkStatusChange(null, null, device.id(), true);
        defaultRoutingHandler.purgeEcmpGraph(device.id());

        // Cleanup all internal groupHandler stores for this device. Should be
        // done after all rerouting or rehashing has been completed
        groupHandlerMap.entrySet()
            .forEach(entry -> entry.getValue().cleanUpForNeighborDown(device.id()));
    }

    /**
     * Purge the destinationSet nextObjective store of entries with this device
     * as key. Erases app-level knowledge of hashed groups in this device.
     *
     * @param devId the device identifier
     */
    void purgeHashedNextObjectiveStore(DeviceId devId) {
        log.debug("Purging hashed next-obj store for dev:{}", devId);
        dsNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(devId))
                .forEach(entry -> dsNextObjStore.remove(entry.getKey()));
    }

    private void processPortUpdatedInternal(Device device, Port port) {
        if (deviceConfiguration == null || !deviceConfiguration.isConfigured(device.id())) {
            log.warn("Device configuration uploading. Not handling port event for"
                    + "dev: {} port: {}", device.id(), port.number());
            return;
        }

        if (interfaceService.isConfigured(new ConnectPoint(device.id(), port.number()))) {
            lastEdgePortEvent = Instant.now();
        }

        if (!mastershipService.isLocalMaster(device.id()))  {
            log.debug("Not master for dev:{} .. not handling port updated event"
                    + "for port {}", device.id(), port.number());
            return;
        }
        processPortUpdated(device.id(), port);
    }

    /**
     * Adds or remove filtering rules for the given switchport. If switchport is
     * an edge facing port, additionally handles host probing and broadcast
     * rules. Must be called by local master of device.
     *
     * @param deviceId the device identifier
     * @param port the port to update
     */
    void processPortUpdated(DeviceId deviceId, Port port) {
        // first we handle filtering rules associated with the port
        if (port.isEnabled()) {
            log.info("Switchport {}/{} enabled..programming filters",
                     deviceId, port.number());
            routingRulePopulator.processSinglePortFilters(deviceId, port.number(), true);
        } else {
            log.info("Switchport {}/{} disabled..removing filters",
                     deviceId, port.number());
            routingRulePopulator.processSinglePortFilters(deviceId, port.number(), false);
        }

        // portUpdated calls are for ports that have gone down or up. For switch
        // to switch ports, link-events should take care of any re-routing or
        // group editing necessary for port up/down. Here we only process edge ports
        // that are already configured.
        ConnectPoint cp = new ConnectPoint(deviceId, port.number());
        VlanId untaggedVlan = interfaceService.getUntaggedVlanId(cp);
        VlanId nativeVlan = interfaceService.getNativeVlanId(cp);
        Set<VlanId> taggedVlans = interfaceService.getTaggedVlanId(cp);

        if (untaggedVlan == null && nativeVlan == null && taggedVlans.isEmpty()) {
            log.debug("Not handling port updated event for non-edge port (unconfigured) "
                    + "dev/port: {}/{}", deviceId, port.number());
            return;
        }
        if (untaggedVlan != null) {
            processEdgePort(deviceId, port, untaggedVlan, true);
        }
        if (nativeVlan != null) {
            processEdgePort(deviceId, port, nativeVlan, true);
        }
        if (!taggedVlans.isEmpty()) {
            taggedVlans.forEach(tag -> processEdgePort(deviceId, port, tag, false));
        }
    }

    private void processEdgePort(DeviceId deviceId, Port port, VlanId vlanId,
                                 boolean popVlan) {
        boolean portUp = port.isEnabled();
        if (portUp) {
            log.info("Device:EdgePort {}:{} is enabled in vlan: {}", deviceId,
                     port.number(), vlanId);
            hostEventExecutor.execute(() -> hostHandler.processPortUp(new ConnectPoint(deviceId, port.number())));
        } else {
            log.info("Device:EdgePort {}:{} is disabled in vlan: {}", deviceId,
                     port.number(), vlanId);
        }

        DefaultGroupHandler groupHandler = groupHandlerMap.get(deviceId);
        if (groupHandler != null) {
            groupHandler.processEdgePort(port.number(), vlanId, popVlan, portUp);
        } else {
            log.warn("Group handler not found for dev:{}. Not handling edge port"
                    + " {} event for port:{}", deviceId,
                    (portUp) ? "UP" : "DOWN", port.number());
        }
    }

    private void createOrUpdateDeviceConfiguration() {
        if (deviceConfiguration == null) {
            log.info("Creating new DeviceConfiguration");
            deviceConfiguration = new DeviceConfiguration(this);
        } else {
            log.info("Updating DeviceConfiguration");
            deviceConfiguration.updateConfig();
        }
    }

    private void createOrUpdateDefaultRoutingHandler() {
        if (defaultRoutingHandler == null) {
            log.info("Creating new DefaultRoutingHandler");
            defaultRoutingHandler = new DefaultRoutingHandler(this);
        } else {
            log.info("Updating DefaultRoutingHandler");
            defaultRoutingHandler.update(this);
        }
    }

    /**
     * Registers the given connect point with the NRS, this is necessary
     * to receive the NDP and ARP packets from the NRS.
     *
     * @param portToRegister connect point to register
     */
    public void registerConnectPoint(ConnectPoint portToRegister) {
        neighbourResolutionService.registerNeighbourHandler(
                portToRegister,
                neighbourHandler,
                appId
        );
    }

    private class InternalConfigListener implements NetworkConfigListener {
        private static final long PROGRAM_DELAY = 2;
        SegmentRoutingManager srManager;

        /**
         * Constructs the internal network config listener.
         *
         * @param srManager segment routing manager
         */
        InternalConfigListener(SegmentRoutingManager srManager) {
            this.srManager = srManager;
        }

        /**
         * Reads network config and initializes related data structure accordingly.
         */
        void configureNetwork() {
            log.info("Configuring network ...");

            // Setting handling of network configuration events completable future
            // The completable future is needed because of the async behaviour of the configureNetwork,
            // listener registration and event arrival
            // Enables us to buffer the events and execute them when the configure network is done.
            synchronized (networkConfigCompletionLock) {
                networkConfigCompletion = new CompletableFuture<>();

                // add a small delay to absorb multiple network config added notifications
                if (!programmingScheduled.get()) {
                    log.info("Buffering config calls for {} secs", PROGRAM_DELAY);
                    programmingScheduled.set(true);
                    mainEventExecutor.schedule(new ConfigChange(), PROGRAM_DELAY, TimeUnit.SECONDS);
                }

                createOrUpdateDeviceConfiguration();

                arpHandler = new ArpHandler(srManager);
                icmpHandler = new IcmpHandler(srManager);
                ipHandler = new IpHandler(srManager);
                routingRulePopulator = new RoutingRulePopulator(srManager);
                createOrUpdateDefaultRoutingHandler();

                tunnelHandler = new TunnelHandler(linkService, deviceConfiguration,
                    groupHandlerMap, tunnelStore);
                policyHandler = new PolicyHandler(appId, deviceConfiguration,
                    flowObjectiveService,
                    tunnelHandler, policyStore);
                networkConfigCompletion.complete(true);
            }

            mcastHandler.init();

        }

        @Override
        public void event(NetworkConfigEvent event) {
            if (mainEventExecutor == null) {
                return;
            }
            checkState(appCfgHandler != null, "NetworkConfigEventHandler is not initialized");
            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                case CONFIG_REMOVED:
                    log.trace("Schedule Network Config event {}", event);
                    if (networkConfigCompletion == null || networkConfigCompletion.isDone()) {
                        mainEventExecutor.execute(new InternalEventHandler(event));
                    } else {
                        queuedEvents.add(event);
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            if (event.type() == CONFIG_REGISTERED ||
                    event.type() == CONFIG_UNREGISTERED) {
                log.debug("Ignore event {} due to type mismatch", event);
                return false;
            }

            if (!event.configClass().equals(SegmentRoutingDeviceConfig.class) &&
                    !event.configClass().equals(SegmentRoutingAppConfig.class) &&
                    !event.configClass().equals(InterfaceConfig.class)) {
                log.debug("Ignore event {} due to class mismatch", event);
                return false;
            }

            return true;
        }

        private final class ConfigChange implements Runnable {
            @Override
            public void run() {
                programmingScheduled.set(false);
                log.info("Reacting to config changes after buffer delay");
                for (Device device : deviceService.getDevices()) {
                    processDeviceAdded(device);
                }
                defaultRoutingHandler.startPopulationProcess();
            }
        }
    }

    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            if (mainEventExecutor == null) {
                return;
            }
            if (event.type() == LinkEvent.Type.LINK_ADDED ||
                    event.type() == LinkEvent.Type.LINK_UPDATED ||
                    event.type() == LinkEvent.Type.LINK_REMOVED) {
                log.trace("Schedule Link event {}", event);
                if (networkConfigCompletion == null || networkConfigCompletion.isDone()) {
                    mainEventExecutor.execute(new InternalEventHandler(event));
                } else {
                    queuedEvents.add(event);
                }
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            if (mainEventExecutor == null) {
                return;
            }
            switch (event.type()) {
                case DEVICE_ADDED:
                case PORT_UPDATED:
                case PORT_ADDED:
                case DEVICE_UPDATED:
                case DEVICE_AVAILABILITY_CHANGED:
                    log.trace("Schedule Device event {}", event);
                    if (networkConfigCompletion == null || networkConfigCompletion.isDone()) {
                        mainEventExecutor.execute(new InternalEventHandler(event));
                    } else {
                        queuedEvents.add(event);
                    }
                    break;
                default:
            }
        }
    }

    private class InternalTopologyListener implements TopologyListener {
        @Override
        public void event(TopologyEvent event) {
            if (mainEventExecutor == null) {
                return;
            }
            switch (event.type()) {
                case TOPOLOGY_CHANGED:
                    log.trace("Schedule Topology event {}", event);
                    if (networkConfigCompletion == null || networkConfigCompletion.isDone()) {
                        mainEventExecutor.execute(new InternalEventHandler(event));
                    } else {
                        queuedEvents.add(event);
                    }
                    break;
                default:
            }
        }
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            if (hostEventExecutor == null) {
                return;
            }
            switch (event.type()) {
                case HOST_ADDED:
                case HOST_MOVED:
                case HOST_REMOVED:
                case HOST_UPDATED:
                    log.trace("Schedule Host event {}", event);
                    hostEventExecutor.execute(new InternalEventHandler(event));
                    break;
                default:
                    log.warn("Unsupported host event type: {}", event.type());
                    break;
            }
        }
    }

    private class InternalMcastListener implements McastListener {
        @Override
        public void event(McastEvent event) {
            if (mcastEventExecutor == null) {
                return;
            }
            switch (event.type()) {
                case SOURCES_ADDED:
                case SOURCES_REMOVED:
                case SINKS_ADDED:
                case SINKS_REMOVED:
                case ROUTE_REMOVED:
                case ROUTE_ADDED:
                    log.trace("Schedule Mcast event {}", event);
                    mcastEventExecutor.execute(new InternalEventHandler(event));
                    break;
                default:
                    log.warn("Unsupported mcast event type: {}", event.type());
                    break;
            }
        }
    }

    private class InternalRouteEventListener implements RouteListener {
        @Override
        public void event(RouteEvent event) {
            if (routeEventExecutor == null) {
                return;
            }
            switch (event.type()) {
                case ROUTE_ADDED:
                case ROUTE_UPDATED:
                case ROUTE_REMOVED:
                case ALTERNATIVE_ROUTES_CHANGED:
                    log.trace("Schedule Route event {}", event);
                    routeEventExecutor.execute(new InternalEventHandler(event));
                    break;
                default:
                    log.warn("Unsupported route event type: {}", event.type());
                    break;
            }
        }
    }

    private class InternalMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            if (mainEventExecutor == null) {
                return;
            }
            switch (event.type()) {
            case MASTER_CHANGED:
                log.debug("Mastership event: {}/{}", event.subject(),
                          event.roleInfo());
                mainEventExecutor.execute(new InternalEventHandler(event));
                break;
            case BACKUPS_CHANGED:
            case SUSPENDED:
            default:
                log.debug("Mastership event type {} not handled", event.type());
                break;
            }
        }
    }

    class InternalClusterListener implements ClusterEventListener {
        private Instant lastClusterEvent = Instant.EPOCH;

        long timeSinceLastClusterEvent() {
            return Instant.now().toEpochMilli() - lastClusterEvent.toEpochMilli();
        }

        @Override
        public void event(ClusterEvent event) {
            switch (event.type()) {
            case INSTANCE_ACTIVATED:
            case INSTANCE_ADDED:
            case INSTANCE_READY:
                log.debug("Cluster event {} ignored", event.type());
                break;
            case INSTANCE_DEACTIVATED:
            case INSTANCE_REMOVED:
                log.info("** Cluster event {}", event.type());
                lastClusterEvent = Instant.now();
                break;
            default:
                break;
            }

        }

    }

    private void updateInterface(InterfaceConfig conf, InterfaceConfig prevConf) {
        try {
            Set<Interface> intfs = conf.getInterfaces();
            Set<Interface> prevIntfs = prevConf.getInterfaces();

            // Now we only handle one interface config at each port.
            if (intfs.size() != 1 || prevIntfs.size() != 1) {
                log.warn("Interface update aborted - one at a time is allowed, " +
                                 "but {} / {}(prev) received.", intfs.size(), prevIntfs.size());
                return;
            }

            //The system is in an incoherent state, abort
            if (defaultRoutingHandler == null) {
                log.warn("Interface update aborted, defaultRoutingHandler is null");
                return;
            }

            Interface intf = intfs.stream().findFirst().get();
            Interface prevIntf = prevIntfs.stream().findFirst().get();

            DeviceId deviceId = intf.connectPoint().deviceId();
            PortNumber portNum = intf.connectPoint().port();

            removeSubnetConfig(prevIntf.connectPoint(),
                               Sets.difference(new HashSet<>(prevIntf.ipAddressesList()),
                                               new HashSet<>(intf.ipAddressesList())));

            if (!prevIntf.vlanNative().equals(VlanId.NONE)
                    && !prevIntf.vlanNative().equals(intf.vlanUntagged())
                    && !prevIntf.vlanNative().equals(intf.vlanNative())) {
                if (intf.vlanTagged().contains(prevIntf.vlanNative())) {
                    // Update filtering objective and L2IG group bucket
                    updatePortVlanTreatment(deviceId, portNum, prevIntf.vlanNative(), false);
                } else {
                    // RemoveVlanNative
                    updateVlanConfigInternal(deviceId, portNum, prevIntf.vlanNative(), true, false);
                }
            }

            if (!prevIntf.vlanUntagged().equals(VlanId.NONE)
                    && !prevIntf.vlanUntagged().equals(intf.vlanUntagged())
                    && !prevIntf.vlanUntagged().equals(intf.vlanNative())) {
                if (intf.vlanTagged().contains(prevIntf.vlanUntagged())) {
                    // Update filtering objective and L2IG group bucket
                    updatePortVlanTreatment(deviceId, portNum, prevIntf.vlanUntagged(), false);
                } else {
                    // RemoveVlanUntagged
                    updateVlanConfigInternal(deviceId, portNum, prevIntf.vlanUntagged(), true, false);
                }
            }

            if (!prevIntf.vlanTagged().isEmpty() && !intf.vlanTagged().equals(prevIntf.vlanTagged())) {
                // RemoveVlanTagged
                Sets.difference(prevIntf.vlanTagged(), intf.vlanTagged()).stream()
                        .filter(i -> !intf.vlanUntagged().equals(i))
                        .filter(i -> !intf.vlanNative().equals(i))
                        .forEach(vlanId -> updateVlanConfigInternal(
                                deviceId, portNum, vlanId, false, false));
            }

            if (!intf.vlanNative().equals(VlanId.NONE)
                    && !prevIntf.vlanNative().equals(intf.vlanNative())
                    && !prevIntf.vlanUntagged().equals(intf.vlanNative())) {
                if (prevIntf.vlanTagged().contains(intf.vlanNative())) {
                    // Update filtering objective and L2IG group bucket
                    updatePortVlanTreatment(deviceId, portNum, intf.vlanNative(), true);
                } else {
                    // AddVlanNative
                    updateVlanConfigInternal(deviceId, portNum, intf.vlanNative(), true, true);
                }
            }

            if (!intf.vlanTagged().isEmpty() && !intf.vlanTagged().equals(prevIntf.vlanTagged())) {
                // AddVlanTagged
                Sets.difference(intf.vlanTagged(), prevIntf.vlanTagged()).stream()
                        .filter(i -> !prevIntf.vlanUntagged().equals(i))
                        .filter(i -> !prevIntf.vlanNative().equals(i))
                        .forEach(vlanId -> updateVlanConfigInternal(
                                deviceId, portNum, vlanId, false, true)
                );
            }

            if (!intf.vlanUntagged().equals(VlanId.NONE)
                    && !prevIntf.vlanUntagged().equals(intf.vlanUntagged())
                    && !prevIntf.vlanNative().equals(intf.vlanUntagged())) {
                if (prevIntf.vlanTagged().contains(intf.vlanUntagged())) {
                    // Update filtering objective and L2IG group bucket
                    updatePortVlanTreatment(deviceId, portNum, intf.vlanUntagged(), true);
                } else {
                    // AddVlanUntagged
                    updateVlanConfigInternal(deviceId, portNum, intf.vlanUntagged(), true, true);
                }
            }
            addSubnetConfig(prevIntf.connectPoint(),
                            Sets.difference(new HashSet<>(intf.ipAddressesList()),
                                            new HashSet<>(prevIntf.ipAddressesList())));
        } catch (ConfigException e) {
            log.error("Error in configuration");
        }
    }

    private void updatePortVlanTreatment(DeviceId deviceId, PortNumber portNum,
                                         VlanId vlanId, boolean pushVlan) {
        DefaultGroupHandler grpHandler = getGroupHandler(deviceId);
        if (grpHandler == null) {
            log.warn("Failed to retrieve group handler for device {}", deviceId);
            return;
        }

        // Update filtering objective for a single port
        routingRulePopulator.updateSinglePortFilters(deviceId, portNum, !pushVlan, vlanId, false);
        routingRulePopulator.updateSinglePortFilters(deviceId, portNum, pushVlan, vlanId, true);

        if (getVlanNextObjectiveId(deviceId, vlanId) != -1) {
            // Update L2IG bucket of the port
            grpHandler.updateL2InterfaceGroupBucket(portNum, vlanId, pushVlan);
            // Update bridging and unicast routing rule for each host
            hostEventExecutor.execute(() -> hostHandler.processIntfVlanUpdatedEvent(deviceId, portNum,
                    vlanId, !pushVlan, false));
            hostEventExecutor.execute(() -> hostHandler.processIntfVlanUpdatedEvent(deviceId, portNum,
                    vlanId, pushVlan, true));
        } else {
            log.warn("Failed to retrieve next objective for vlan {} in device {}:{}", vlanId, deviceId, portNum);
        }
    }

    private void updateVlanConfigInternal(DeviceId deviceId, PortNumber portNum,
                                          VlanId vlanId, boolean pushVlan, boolean install) {
        DefaultGroupHandler grpHandler = getGroupHandler(deviceId);
        if (grpHandler == null) {
            log.warn("Failed to retrieve group handler for device {}", deviceId);
            return;
        }

        // Update filtering objective for a single port
        routingRulePopulator.updateSinglePortFilters(deviceId, portNum, pushVlan, vlanId, install);

        // Update filtering objective for multicast ingress port
        mcastHandler.updateFilterToDevice(deviceId, portNum, vlanId, install);

        int nextId = getVlanNextObjectiveId(deviceId, vlanId);

        if (nextId != -1 && !install) {
            // Remove L2 Bridging rule and L3 Unicast rule to the host
            hostEventExecutor.execute(() -> hostHandler.processIntfVlanUpdatedEvent(deviceId, portNum,
                    vlanId, pushVlan, install));
            // Remove broadcast forwarding rule and corresponding L2FG for VLAN
            // only if there is no port configured on that VLAN ID
            if (!getVlanPortMap(deviceId).containsKey(vlanId)) {
                // Remove broadcast forwarding rule for the VLAN
                routingRulePopulator.updateSubnetBroadcastRule(deviceId, vlanId, install);
                // Remove L2FG for VLAN
                grpHandler.removeBcastGroupFromVlan(deviceId, portNum, vlanId, pushVlan);
            } else {
                // Remove a single port from L2FG
                grpHandler.updateGroupFromVlanConfiguration(vlanId, portNum, nextId, install);
            }
            // Remove L2IG of the port
            grpHandler.removePortNextObjective(deviceId, portNum, vlanId, pushVlan);
        } else if (install) {
            // Create L2IG of the port
            grpHandler.createPortNextObjective(deviceId, portNum, vlanId, pushVlan);
            // Create L2 Bridging rule and L3 Unicast rule to the host
            hostEventExecutor.execute(() -> hostHandler.processIntfVlanUpdatedEvent(deviceId, portNum,
                    vlanId, pushVlan, install));
            if (nextId != -1) {
                // Add a single port to L2FG
                grpHandler.updateGroupFromVlanConfiguration(vlanId, portNum, nextId, install);
            } else {
                // Create L2FG for VLAN
                grpHandler.createBcastGroupFromVlan(vlanId, Collections.singleton(portNum));
                routingRulePopulator.updateSubnetBroadcastRule(deviceId, vlanId, install);
            }
        } else {
            log.warn("Failed to retrieve next objective for vlan {} in device {}:{}", vlanId, deviceId, portNum);
        }
    }

    private void removeSubnetConfig(ConnectPoint cp, Set<InterfaceIpAddress> ipAddressSet) {
        Set<IpPrefix> ipPrefixSet = ipAddressSet.stream().
                map(InterfaceIpAddress::subnetAddress).collect(Collectors.toSet());

        Set<InterfaceIpAddress> deviceIntfIpAddrs = interfaceService.getInterfaces().stream()
                .filter(intf -> intf.connectPoint().deviceId().equals(cp.deviceId()))
                .filter(intf -> !intf.connectPoint().equals(cp))
                .flatMap(intf -> intf.ipAddressesList().stream())
                .collect(Collectors.toSet());
        // 1. Partial subnet population
        // Remove routing rules for removed subnet from previous configuration,
        // which does not also exist in other interfaces in the same device
        Set<IpPrefix> deviceIpPrefixSet = deviceIntfIpAddrs.stream()
                .map(InterfaceIpAddress::subnetAddress)
                .collect(Collectors.toSet());

        Set<IpPrefix> subnetsToBeRevoked = ipPrefixSet.stream()
                .filter(ipPrefix -> !deviceIpPrefixSet.contains(ipPrefix))
                .collect(Collectors.toSet());

        // Check if any of the subnets to be revoked is configured in the pairDevice.
        // If any, repopulate the subnet with pairDevice connectPoint instead of revoking.
        Optional<DeviceId> pairDevice = getPairDeviceId(cp.deviceId());
        if (pairDevice.isPresent()) {
            Set<IpPrefix> pairDeviceIpPrefix = getDeviceSubnetMap().get(pairDevice.get());

            Set<IpPrefix> subnetsExistingInPairDevice = subnetsToBeRevoked.stream()
                    .filter(ipPrefix -> pairDeviceIpPrefix.contains(ipPrefix))
                    .collect(Collectors.toSet());

            // Update the subnets existing in pair device with pair device connect point.
            if (!subnetsExistingInPairDevice.isEmpty()) {
                // PortNumber of connect point is not relevant in populate subnet and hence providing as ANY.
                ConnectPoint pairDeviceCp = new ConnectPoint(pairDevice.get(), PortNumber.ANY);
                log.debug("Updating the subnets: {} with pairDevice connectPoint as it exists in the Pair device: {}",
                        subnetsExistingInPairDevice, pairDeviceCp);
                defaultRoutingHandler.populateSubnet(Collections.singleton(pairDeviceCp), subnetsExistingInPairDevice);
            }

            // Remove only the subnets that are not configured in the pairDevice.
            subnetsToBeRevoked = Sets.difference(subnetsToBeRevoked, subnetsExistingInPairDevice);
        }

        if (!subnetsToBeRevoked.isEmpty()) {
            log.debug("Removing subnets for connectPoint: {}, subnets: {}", cp, subnetsToBeRevoked);
            defaultRoutingHandler.revokeSubnet(subnetsToBeRevoked);
        }

        // 2. Interface IP punts
        // Remove IP punts for old Intf address
        Set<IpAddress> deviceIpAddrs = deviceIntfIpAddrs.stream()
                .map(InterfaceIpAddress::ipAddress)
                .collect(Collectors.toSet());
        ipAddressSet.stream()
                .map(InterfaceIpAddress::ipAddress)
                .filter(interfaceIpAddress -> !deviceIpAddrs.contains(interfaceIpAddress))
                .forEach(interfaceIpAddress ->
                                 routingRulePopulator.revokeSingleIpPunts(
                                         cp.deviceId(), interfaceIpAddress));

        // 3. Host unicast routing rule
        // Remove unicast routing rule
        hostEventExecutor.execute(() -> hostHandler.processIntfIpUpdatedEvent(cp, ipPrefixSet, false));
    }

    private void addSubnetConfig(ConnectPoint cp, Set<InterfaceIpAddress> ipAddressSet) {
        Set<IpPrefix> ipPrefixSet = ipAddressSet.stream().
                map(InterfaceIpAddress::subnetAddress).collect(Collectors.toSet());

        Set<InterfaceIpAddress> deviceIntfIpAddrs = interfaceService.getInterfaces().stream()
                .filter(intf -> intf.connectPoint().deviceId().equals(cp.deviceId()))
                .filter(intf -> !intf.connectPoint().equals(cp))
                .flatMap(intf -> intf.ipAddressesList().stream())
                .collect(Collectors.toSet());
        // 1. Partial subnet population
        // Add routing rules for newly added subnet, which does not also exist in
        // other interfaces in the same device
        Set<IpPrefix> deviceIpPrefixSet = deviceIntfIpAddrs.stream()
                .map(InterfaceIpAddress::subnetAddress)
                .collect(Collectors.toSet());
        Set<IpPrefix> subnetsToBePopulated = ipPrefixSet.stream()
                .filter(ipPrefix -> !deviceIpPrefixSet.contains(ipPrefix))
                .collect(Collectors.toSet());

        if (!subnetsToBePopulated.isEmpty()) {
            log.debug("Adding subnets for connectPoint: {}, subnets: {}", cp, subnetsToBePopulated);

            // check if pair-device has the same subnet configured?
            Optional<DeviceId> pairDevice = getPairDeviceId(cp.deviceId());
            if (pairDevice.isPresent()) {
                Set<IpPrefix> pairDeviceIpPrefix = getDeviceSubnetMap().get(pairDevice.get());

                Set<IpPrefix>  subnetsToBePopulatedAsDualHomed = subnetsToBePopulated.stream()
                        .filter(ipPrefix -> pairDeviceIpPrefix.contains(ipPrefix))
                        .collect(Collectors.toSet());
                Set<IpPrefix> subnetsToBePopulatedAsSingleHomed = Sets.difference(subnetsToBePopulated,
                        subnetsToBePopulatedAsDualHomed);

                if (!subnetsToBePopulatedAsSingleHomed.isEmpty()) {
                    defaultRoutingHandler.populateSubnet(
                            Collections.singleton(cp),
                            subnetsToBePopulatedAsSingleHomed);
                }

                if (!subnetsToBePopulatedAsDualHomed.isEmpty()) {
                    Set<ConnectPoint> cpts = new HashSet<>();
                    cpts.add(cp);
                    // As Subnets is DualHomed adding the pairDevice also as ConnectPoint.
                    // PortNumber of connect point is not relevant in populate subnet and hence providing as ANY.
                    ConnectPoint pairCp = new ConnectPoint(pairDevice.get(), PortNumber.ANY);
                    cpts.add(pairCp);

                    log.debug("Adding DualHomed subnets for connectPoint: {} and its pair device: {}, subnets: {}",
                            cp, pairCp, subnetsToBePopulatedAsDualHomed);

                    // populating the subnets as DualHomed
                    defaultRoutingHandler.populateSubnet(
                            cpts,
                            subnetsToBePopulated);

                    // revoking the subnets populated in the device as it is now Dualhomed.
                    defaultRoutingHandler.revokeSubnet(Collections.singleton(cp.deviceId()),
                            subnetsToBePopulatedAsDualHomed);
                }
            } else {
                defaultRoutingHandler.populateSubnet(
                        Collections.singleton(cp),
                        subnetsToBePopulated);
            }
        }

        // 2. Interface IP punts
        // Add IP punts for new Intf address
        Set<IpAddress> deviceIpAddrs = deviceIntfIpAddrs.stream()
                .map(InterfaceIpAddress::ipAddress)
                .collect(Collectors.toSet());
        ipAddressSet.stream()
                .map(InterfaceIpAddress::ipAddress)
                .filter(interfaceIpAddress -> !deviceIpAddrs.contains(interfaceIpAddress))
                .forEach(interfaceIpAddress ->
                                 routingRulePopulator.populateSingleIpPunts(
                                         cp.deviceId(), interfaceIpAddress));

        // 3. Host unicast routing rule
        // Add unicast routing rule
        hostEventExecutor.execute(() -> hostHandler.processIntfIpUpdatedEvent(cp, ipPrefixSet, true));
    }
}
