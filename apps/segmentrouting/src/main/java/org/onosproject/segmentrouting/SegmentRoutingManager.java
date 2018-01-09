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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ICMP6;
import org.onlab.packet.IPv4;
import org.onlab.packet.IPv6;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.mastership.MastershipService;
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
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostLocationProbingService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.mcast.McastEvent;
import org.onosproject.net.mcast.McastListener;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.RouteEvent;
import org.onosproject.routeservice.RouteListener;
import org.onosproject.routeservice.RouteService;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.config.PwaasConfig;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
import org.onosproject.segmentrouting.config.SegmentRoutingDeviceConfig;
import org.onosproject.segmentrouting.config.XConnectConfig;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.onosproject.segmentrouting.grouphandler.DestinationSet;
import org.onosproject.segmentrouting.grouphandler.NextNeighbors;
import org.onosproject.segmentrouting.pwaas.DefaultL2Tunnel;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2TunnelHandler;
import org.onosproject.segmentrouting.storekey.DestinationSetNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.PortNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.VlanNextObjectiveStoreKey;
import org.onosproject.segmentrouting.storekey.XConnectStoreKey;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static org.onlab.packet.Ethernet.TYPE_ARP;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Segment routing manager.
 */
@Service
@Component(immediate = true)
public class SegmentRoutingManager implements SegmentRoutingService {

    private static Logger log = LoggerFactory.getLogger(SegmentRoutingManager.class);
    private static final String NOT_MASTER = "Current instance is not the master of {}. Ignore.";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private ComponentConfigService compCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private NeighbourResolutionService neighbourResolutionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    HostLocationProbingService probingService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    MulticastRouteService multicastRouteService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    RouteService routeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public InterfaceService interfaceService;

    @Property(name = "activeProbing", boolValue = true,
            label = "Enable active probing to discover dual-homed hosts.")
    boolean activeProbing = true;

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
    XConnectHandler xConnectHandler = null;
    private McastHandler mcastHandler = null;
    private HostHandler hostHandler = null;
    private RouteHandler routeHandler = null;
    private SegmentRoutingNeighbourDispatcher neighbourHandler = null;
    private L2TunnelHandler l2TunnelHandler = null;
    private InternalEventHandler eventHandler = new InternalEventHandler();
    private final InternalHostListener hostListener = new InternalHostListener();
    private final InternalConfigListener cfgListener = new InternalConfigListener(this);
    private final InternalMcastListener mcastListener = new InternalMcastListener();
    private final InternalRouteEventListener routeListener = new InternalRouteEventListener();

    private ScheduledExecutorService executorService = Executors
            .newScheduledThreadPool(1, groupedThreads("SegmentRoutingManager", "event-%d", log));

    @SuppressWarnings("unused")
    private static ScheduledFuture<?> eventHandlerFuture = null;
    @SuppressWarnings("rawtypes")
    private ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();
    private Map<DeviceId, DefaultGroupHandler> groupHandlerMap =
            new ConcurrentHashMap<>();
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
     * Used to keep track on L2 interface group and L3 unicast group information.
     */
    private EventuallyConsistentMap<PortNextObjectiveStoreKey, Integer>
            portNextObjStore = null;

    // Local store for all links seen and their present status, used for
    // optimized routing. The existence of the link in the keys is enough to know
    // if the link has been "seen-before" by this instance of the controller.
    // The boolean value indicates if the link is currently up or not.
    // XXX Currently the optimized routing logic depends on "forgetting" a link
    // when a switch goes down, but "remembering" it when only the link goes down.
    // Consider changing this logic so we can use the Link Service instead of
    // a local cache.
    private Map<Link, Boolean> seenLinks = new ConcurrentHashMap<>();

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

    private final ConfigFactory<ApplicationId, XConnectConfig> xConnectConfigFactory =
            new ConfigFactory<ApplicationId, XConnectConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY,
                    XConnectConfig.class, "xconnect") {
                @Override
                public XConnectConfig createConfig() {
                    return new XConnectConfig();
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

    private final ConfigFactory<ApplicationId, PwaasConfig> pwaasConfigFactory =
            new ConfigFactory<ApplicationId, PwaasConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY,
                    PwaasConfig.class, "pwaas") {
                @Override
                public PwaasConfig createConfig() {
                    return new PwaasConfig();
                }
            };

    private static final Object THREAD_SCHED_LOCK = new Object();
    private static int numOfEventsQueued = 0;
    private static int numOfEventsExecuted = 0;
    private static int numOfHandlerExecution = 0;
    private static int numOfHandlerScheduled = 0;

    /**
     * Segment Routing App ID.
     */
    public static final String APP_NAME = "org.onosproject.segmentrouting";

    /**
     * The default VLAN ID assigned to the interfaces without subnet config.
     */
    public static final VlanId INTERNAL_VLAN = VlanId.vlanId((short) 4094);

    @Activate
    protected void activate(ComponentContext context) {
        appId = coreService.registerApplication(APP_NAME);

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

        compCfgService.preSetProperty("org.onosproject.net.group.impl.GroupManager",
                                      "purgeOnDisconnection", "true");
        compCfgService.preSetProperty("org.onosproject.net.flow.impl.FlowRuleManager",
                                      "purgeOnDisconnection", "true");
        compCfgService.preSetProperty("org.onosproject.provider.host.impl.HostLocationProvider",
                                      "requestInterceptsEnabled", "false");
        compCfgService.preSetProperty("org.onosproject.net.neighbour.impl.NeighbourResolutionManager",
                                      "requestInterceptsEnabled", "false");
        compCfgService.preSetProperty("org.onosproject.dhcprelay.DhcpRelayManager",
                                      "arpEnabled", "false");
        compCfgService.preSetProperty("org.onosproject.net.host.impl.HostManager",
                                      "greedyLearningIpv6", "true");
        compCfgService.preSetProperty("org.onosproject.routing.cpr.ControlPlaneRedirectManager",
                                      "forceUnprovision", "true");
        compCfgService.preSetProperty("org.onosproject.routeservice.store.RouteStoreImpl",
                                      "distributed", "true");
        compCfgService.preSetProperty("org.onosproject.provider.host.impl.HostLocationProvider",
                                      "multihomingEnabled", "true");
        compCfgService.preSetProperty("org.onosproject.provider.lldp.impl.LldpLinkProvider",
                                      "staleLinkAge", "15000");
        compCfgService.preSetProperty("org.onosproject.net.host.impl.HostManager",
                                      "allowDuplicateIps", "false");
        compCfgService.registerProperties(getClass());
        modified(context);

        processor = new InternalPacketProcessor();
        linkListener = new InternalLinkListener();
        deviceListener = new InternalDeviceListener();
        appCfgHandler = new AppConfigHandler(this);
        xConnectHandler = new XConnectHandler(this);
        mcastHandler = new McastHandler(this);
        hostHandler = new HostHandler(this);
        routeHandler = new RouteHandler(this);
        neighbourHandler = new SegmentRoutingNeighbourDispatcher(this);
        l2TunnelHandler = new L2TunnelHandler(this);

        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(deviceConfigFactory);
        cfgService.registerConfigFactory(appConfigFactory);
        cfgService.registerConfigFactory(xConnectConfigFactory);
        cfgService.registerConfigFactory(mcastConfigFactory);
        cfgService.registerConfigFactory(pwaasConfigFactory);

        cfgListener.configureNetwork();

        hostService.addListener(hostListener);
        packetService.addProcessor(processor, PacketProcessor.director(2));
        linkService.addListener(linkListener);
        deviceService.addListener(deviceListener);
        multicastRouteService.addListener(mcastListener);
        routeService.addListener(routeListener);

        l2TunnelHandler.init();

        log.info("Started");
    }

    private KryoNamespace.Builder createSerializer() {
        return new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(DestinationSetNextObjectiveStoreKey.class,
                        VlanNextObjectiveStoreKey.class,
                        DestinationSet.class,
                        NextNeighbors.class,
                        Tunnel.class,
                        DefaultTunnel.class,
                        Policy.class,
                        TunnelPolicy.class,
                        Policy.Type.class,
                        PortNextObjectiveStoreKey.class,
                        XConnectStoreKey.class,
                        DefaultL2Tunnel.class,
                        DefaultL2TunnelPolicy.class
                );
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(deviceConfigFactory);
        cfgService.unregisterConfigFactory(appConfigFactory);
        cfgService.unregisterConfigFactory(xConnectConfigFactory);
        cfgService.unregisterConfigFactory(mcastConfigFactory);
        cfgService.unregisterConfigFactory(pwaasConfigFactory);
        compCfgService.unregisterProperties(getClass(), false);

        hostService.removeListener(hostListener);
        packetService.removeProcessor(processor);
        linkService.removeListener(linkListener);
        deviceService.removeListener(deviceListener);
        multicastRouteService.removeListener(mcastListener);
        routeService.removeListener(routeListener);

        neighbourResolutionService.unregisterNeighbourHandlers(appId);

        processor = null;
        linkListener = null;
        deviceListener = null;
        groupHandlerMap.clear();

        dsNextObjStore.destroy();
        vlanNextObjStore.destroy();
        portNextObjStore.destroy();
        tunnelStore.destroy();
        policyStore.destroy();
        log.info("Stopped");
    }

    @Modified
    private void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        if (properties == null) {
            return;
        }

        String strActiveProving = Tools.get(properties, "activeProbing");
        boolean expectActiveProbing = Boolean.parseBoolean(strActiveProving);

        if (expectActiveProbing != activeProbing) {
            activeProbing = expectActiveProbing;
            log.info("{} active probing", activeProbing ? "Enabling" : "Disabling");
        }
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
    public List<DefaultL2Tunnel> getL2Tunnels() {
        return l2TunnelHandler.getL2Tunnels();
    }

    @Override
    public List<DefaultL2TunnelPolicy> getL2Policies() {
        return l2TunnelHandler.getL2Policies();
    }

    @Override
    public L2TunnelHandler.Result addPseudowire(String tunnelId, String pwLabel, String cP1,
                                         String cP1InnerVlan, String cP1OuterVlan, String cP2,
                                         String cP2InnerVlan, String cP2OuterVlan,
                                         String mode, String sdTag) {
        // Try to inject an empty Pwaas config if it is not found for the first time
        PwaasConfig config = cfgService.getConfig(appId(), PwaasConfig.class);
        if (config == null) {
            log.debug("Pwaas config not found. Try to create an empty one.");
            cfgService.applyConfig(appId(), PwaasConfig.class, new ObjectMapper().createObjectNode());
            config = cfgService.getConfig(appId(), PwaasConfig.class);
        }

        ObjectNode object = config.addPseudowire(tunnelId, pwLabel,
                                                 cP1, cP1InnerVlan, cP1OuterVlan,
                                                 cP2, cP2InnerVlan, cP2OuterVlan,
                                                 mode, sdTag);
        if (object == null) {
            log.warn("Could not add pseudowire to the configuration!");
            return L2TunnelHandler.Result.ADDITION_ERROR;
        }

        // inform everyone about the valid change in the pw configuration
        cfgService.applyConfig(appId(), PwaasConfig.class, object);
        return L2TunnelHandler.Result.SUCCESS;
    }

    @Override
    public L2TunnelHandler.Result removePseudowire(String pwId) {

        PwaasConfig config = cfgService.getConfig(appId(), PwaasConfig.class);
        if (config == null) {
            log.warn("Configuration for Pwaas class could not be found!");
            return L2TunnelHandler.Result.CONFIG_NOT_FOUND;
        }

        ObjectNode object = config.removePseudowire(pwId);
        if (object == null) {
            log.warn("Could not delete pseudowire from configuration!");
            return L2TunnelHandler.Result.REMOVAL_ERROR;
        }

        // sanity check, this should never fail since we removed a pw
        // and we always check when we update the configuration
        config.isValid();

        // inform everyone
        cfgService.applyConfig(appId(), PwaasConfig.class, object);

        return L2TunnelHandler.Result.SUCCESS;
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
    public ImmutableMap<DestinationSetNextObjectiveStoreKey, NextNeighbors> getDestinationSet() {
        if (dsNextObjStore != null) {
            return ImmutableMap.copyOf(dsNextObjStore.entrySet());
        } else {
            return ImmutableMap.of();
        }
    }

    @Override
    public void verifyGroups(DeviceId id) {
        DefaultGroupHandler gh = groupHandlerMap.get(id);
        if (gh != null) {
            gh.triggerBucketCorrector();
        }
    }

    /**
     * Extracts the application ID from the manager.
     *
     * @return application ID
     */
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
     * Per device next objective ID store with (device id + port + treatment + meta) as key.
     * Used to keep track on L2 interface group and L3 unicast group information.
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

    // TODO Consider moving these to InterfaceService
    /**
     * Returns untagged VLAN configured on given connect point.
     * <p>
     * Only returns the first match if there are multiple untagged VLAN configured
     * on the connect point.
     *
     * @param connectPoint connect point
     * @return untagged VLAN or null if not configured
     */
    VlanId getUntaggedVlanId(ConnectPoint connectPoint) {
        return interfaceService.getInterfacesByPort(connectPoint).stream()
                .filter(intf -> !intf.vlanUntagged().equals(VlanId.NONE))
                .map(Interface::vlanUntagged)
                .findFirst().orElse(null);
    }

    /**
     * Returns tagged VLAN configured on given connect point.
     * <p>
     * Returns all matches if there are multiple tagged VLAN configured
     * on the connect point.
     *
     * @param connectPoint connect point
     * @return tagged VLAN or empty set if not configured
     */
    Set<VlanId> getTaggedVlanId(ConnectPoint connectPoint) {
        Set<Interface> interfaces = interfaceService.getInterfacesByPort(connectPoint);
        return interfaces.stream()
                .map(Interface::vlanTagged)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Returns native VLAN configured on given connect point.
     * <p>
     * Only returns the first match if there are multiple native VLAN configured
     * on the connect point.
     *
     * @param connectPoint connect point
     * @return native VLAN or null if not configured
     */
    VlanId getNativeVlanId(ConnectPoint connectPoint) {
        Set<Interface> interfaces = interfaceService.getInterfacesByPort(connectPoint);
        return interfaces.stream()
                .filter(intf -> !intf.vlanNative().equals(VlanId.NONE))
                .map(Interface::vlanNative)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns internal VLAN for untagged hosts on given connect point.
     * <p>
     * The internal VLAN is either vlan-untagged for an access port,
     * or vlan-native for a trunk port.
     *
     * @param connectPoint connect point
     * @return internal VLAN or null if both vlan-untagged and vlan-native are undefined
     */
    VlanId getInternalVlanId(ConnectPoint connectPoint) {
        VlanId untaggedVlanId = getUntaggedVlanId(connectPoint);
        VlanId nativeVlanId = getNativeVlanId(connectPoint);
        return untaggedVlanId != null ? untaggedVlanId : nativeVlanId;
    }

    /**
     * Returns optional pair device ID of given device.
     *
     * @param deviceId device ID
     * @return optional pair device ID. Might be empty if pair device is not configured
     */
    Optional<DeviceId> getPairDeviceId(DeviceId deviceId) {
        SegmentRoutingDeviceConfig deviceConfig =
                cfgService.getConfig(deviceId, SegmentRoutingDeviceConfig.class);
        return Optional.ofNullable(deviceConfig).map(SegmentRoutingDeviceConfig::pairDeviceId);
    }
    /**
     * Returns optional pair device local port of given device.
     *
     * @param deviceId device ID
     * @return optional pair device ID. Might be empty if pair device is not configured
     */
    Optional<PortNumber> getPairLocalPorts(DeviceId deviceId) {
        SegmentRoutingDeviceConfig deviceConfig =
                cfgService.getConfig(deviceId, SegmentRoutingDeviceConfig.class);
        return Optional.ofNullable(deviceConfig).map(SegmentRoutingDeviceConfig::pairLocalPort);
    }

    /**
     * Determine if current instance is the master of given connect point.
     *
     * @param cp connect point
     * @return true if current instance is the master of given connect point
     */
    public boolean isMasterOf(ConnectPoint cp) {
        boolean isMaster = mastershipService.isLocalMaster(cp.deviceId());
        if (!isMaster) {
            log.debug(NOT_MASTER, cp);
        }
        return isMaster;
    }

    /**
     * Returns locations of given resolved route.
     *
     * @param resolvedRoute resolved route
     * @return locations of nexthop. Might be empty if next hop is not found
     */
    Set<ConnectPoint> nextHopLocations(ResolvedRoute resolvedRoute) {
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

    /**
     * Returns true if this controller instance has seen this link before. The
     * link may not be currently up, but as long as the link had been seen before
     * this method will return true. The one exception is when the link was
     * indeed seen before, but this controller instance was forced to forget it
     * by a call to purgeSeenLink method.
     *
     * @param link the infrastructure link being queried
     * @return true if this controller instance has seen this link before
     */
    boolean isSeenLink(Link link) {
        return seenLinks.containsKey(link);
    }

    /**
     * Updates the seen link store. Updates can be for links that are currently
     * available or not.
     *
     * @param link the link to update in the seen-link local store
     * @param up the status of the link, true if up, false if down
     */
    void updateSeenLink(Link link, boolean up) {
        seenLinks.put(link, up);
    }

    /**
     * Returns the status of a seen-link (up or down). If the link has not
     * been seen-before, a null object is returned.
     *
     * @param link the infrastructure link being queried
     * @return null if the link was not seen-before;
     *         true if the seen-link is up;
     *         false if the seen-link is down
     */
    private Boolean isSeenLinkUp(Link link) {
        return seenLinks.get(link);
    }

    /**
     * Makes this controller instance forget a previously seen before link.
     *
     * @param link the infrastructure link to purge
     */
    private void purgeSeenLink(Link link) {
        seenLinks.remove(link);
    }

    /**
     * Returns the status of a link as parallel link. A parallel link
     * is defined as a link which has common src and dst switches as another
     * seen-link that is currently enabled. It is not necessary for the link being
     * queried to be a seen-link.
     *
     *  @param link the infrastructure link being queried
     *  @return true if a seen-link exists that is up, and shares the
     *          same src and dst switches as the link being queried
     */
    private boolean isParallelLink(Link link) {
        for (Entry<Link, Boolean> seen : seenLinks.entrySet()) {
            Link seenLink = seen.getKey();
           if (seenLink.equals(link)) {
               continue;
           }
           if (seenLink.src().deviceId().equals(link.src().deviceId()) &&
                   seenLink.dst().deviceId().equals(link.dst().deviceId()) &&
                   seen.getValue()) {
               return true;
           }
        }
        return false;
    }

    /**
     * Returns true if the link being queried is a bidirectional link. A bidi
     * link is defined as a link, whose reverse link - ie. the link in the reverse
     * direction - has been seen-before and is up. It is not necessary for the link
     * being queried to be a seen-link.
     *
     * @param link the infrastructure link being queried
     * @return true if another unidirectional link exists in the reverse direction,
     *              has been seen-before and is up
     */
    boolean isBidirectional(Link link) {
        Link reverseLink = linkService.getLink(link.dst(), link.src());
        if (reverseLink == null) {
            return false;
        }
        Boolean result = isSeenLinkUp(reverseLink);
        if (result == null) {
            return false;
        }
        return result.booleanValue();
    }

    /**
     * Determines if the given link should be avoided in routing calculations
     * by policy or design.
     *
     * @param link the infrastructure link being queried
     * @return true if link should be avoided
     */
    boolean avoidLink(Link link) {
        // XXX currently only avoids all pair-links. In the future can be
        // extended to avoid any generic link
        DeviceId src = link.src().deviceId();
        PortNumber srcPort = link.src().port();
        if (deviceConfiguration == null || !deviceConfiguration.isConfigured(src)) {
            log.warn("Device {} not configured..cannot avoid link {}", src, link);
            return false;
        }
        DeviceId pairDev;
        PortNumber pairLocalPort, pairRemotePort = null;
        try {
            pairDev = deviceConfiguration.getPairDeviceId(src);
            pairLocalPort = deviceConfiguration.getPairLocalPort(src);
            if (pairDev != null) {
                pairRemotePort = deviceConfiguration.getPairLocalPort(pairDev);
            }
        } catch (DeviceConfigNotFoundException e) {
            log.warn("Pair dev for dev {} not configured..cannot avoid link {}",
                     src, link);
            return false;
        }

        return srcPort.equals(pairLocalPort) &&
                link.dst().deviceId().equals(pairDev) &&
                link.dst().port().equals(pairRemotePort);
    }

    private class InternalPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {

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

    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            if (event.type() == LinkEvent.Type.LINK_ADDED ||
                    event.type() == LinkEvent.Type.LINK_UPDATED ||
                    event.type() == LinkEvent.Type.LINK_REMOVED) {
                log.debug("Event {} received from Link Service", event.type());
                scheduleEventHandlerIfNotScheduled(event);
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
            case DEVICE_ADDED:
            case PORT_UPDATED:
            case PORT_ADDED:
            case DEVICE_UPDATED:
            case DEVICE_AVAILABILITY_CHANGED:
                log.trace("Event {} received from Device Service", event.type());
                scheduleEventHandlerIfNotScheduled(event);
                break;
            default:
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void scheduleEventHandlerIfNotScheduled(Event event) {
        synchronized (THREAD_SCHED_LOCK) {
            eventQueue.add(event);
            numOfEventsQueued++;

            if ((numOfHandlerScheduled - numOfHandlerExecution) == 0) {
                //No pending scheduled event handling threads. So start a new one.
                eventHandlerFuture = executorService
                        .schedule(eventHandler, 100, TimeUnit.MILLISECONDS);
                numOfHandlerScheduled++;
            }
            log.trace("numOfEventsQueued {}, numOfEventHandlerScheduled {}",
                      numOfEventsQueued,
                      numOfHandlerScheduled);
        }
    }

    private class InternalEventHandler implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    @SuppressWarnings("rawtypes")
                    Event event;
                    synchronized (THREAD_SCHED_LOCK) {
                        if (!eventQueue.isEmpty()) {
                            event = eventQueue.poll();
                            numOfEventsExecuted++;
                        } else {
                            numOfHandlerExecution++;
                            log.debug("numOfHandlerExecution {} numOfEventsExecuted {}",
                                      numOfHandlerExecution, numOfEventsExecuted);
                            break;
                        }
                    }
                    if (event.type() == LinkEvent.Type.LINK_ADDED ||
                            event.type() == LinkEvent.Type.LINK_UPDATED) {
                        // Note: do not update seenLinks here, otherwise every
                        // link, even one seen for the first time, will be appear
                        // to be a previously seen link
                        processLinkAdded((Link) event.subject());
                    } else if (event.type() == LinkEvent.Type.LINK_REMOVED) {
                        Link linkRemoved = (Link) event.subject();
                        if (linkRemoved.type() == Link.Type.DIRECT) {
                            updateSeenLink(linkRemoved, false);
                        }
                        // device availability check helps to ensure that
                        // multiple link-removed events are actually treated as a
                        // single switch removed event. purgeSeenLink is necessary
                        // so we do rerouting (instead of rehashing) when switch
                        // comes back.
                        if (linkRemoved.src().elementId() instanceof DeviceId &&
                                !deviceService.isAvailable(linkRemoved.src().deviceId())) {
                            purgeSeenLink(linkRemoved);
                            continue;
                        }
                        if (linkRemoved.dst().elementId() instanceof DeviceId &&
                                !deviceService.isAvailable(linkRemoved.dst().deviceId())) {
                            purgeSeenLink(linkRemoved);
                            continue;
                        }
                        processLinkRemoved((Link) event.subject());
                    } else if (event.type() == DeviceEvent.Type.DEVICE_ADDED ||
                            event.type() == DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED ||
                            event.type() == DeviceEvent.Type.DEVICE_UPDATED) {
                        DeviceId deviceId = ((Device) event.subject()).id();
                        if (deviceService.isAvailable(deviceId)) {
                            log.info("Processing device event {} for available device {}",
                                     event.type(), ((Device) event.subject()).id());
                            processDeviceAdded((Device) event.subject());
                        } else {
                            log.info("Processing device event {} for unavailable device {}",
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
                        processPortUpdated(((Device) event.subject()),
                                           ((DeviceEvent) event).port());
                    } else {
                        log.warn("Unhandled event type: {}", event.type());
                    }
                }
            } catch (Exception e) {
                log.error("SegmentRouting event handler "
                        + "thread thrown an exception: {}", e);
            }
        }
    }

    private void processLinkAdded(Link link) {
        log.info("** LINK ADDED {}", link.toString());
        if (!isLinkValid(link)) {
            return;
        }
        if (!deviceConfiguration.isConfigured(link.src().deviceId())) {
            updateSeenLink(link, true);
            // XXX revisit - what about devicePortMap
            log.warn("Source device of this link is not configured.. "
                    + "not processing further");
            return;
        }

        //Irrespective of whether the local is a MASTER or not for this device,
        //create group handler instance and push default TTP flow rules if needed,
        //as in a multi-instance setup, instances can initiate groups for any device.
        DefaultGroupHandler groupHandler = groupHandlerMap.get(link.src()
                .deviceId());
        if (groupHandler != null) {
            groupHandler.portUpForLink(link);
        } else {
            // XXX revisit/cleanup
            Device device = deviceService.getDevice(link.src().deviceId());
            if (device != null) {
                log.warn("processLinkAdded: Link Added "
                        + "Notification without Device Added "
                        + "event, still handling it");
                processDeviceAdded(device);
                groupHandler = groupHandlerMap.get(link.src()
                                                   .deviceId());
                groupHandler.portUpForLink(link);
            }
        }

        /*// process link only if it is bidirectional
        if (!isBidirectional(link)) {
            log.debug("Link not bidirectional.. waiting for other direction "
                    + "src {} --> dst {} ", link.dst(), link.src());
            // note that if we are not processing for routing, it should at least
            // be considered a seen-link
            updateSeenLink(link, true);
            return;
        }
         TO DO this ensure that rehash is still done correctly even if link is
         not processed for rerouting - perhaps rehash in both directions when
         it ultimately becomes bidi?
        */

        log.debug("Starting optimized route population process for link "
                + "{} --> {}", link.src(), link.dst());
        boolean seenBefore = isSeenLink(link);
        defaultRoutingHandler.populateRoutingRulesForLinkStatusChange(null, link, null);

        // It's possible that linkUp causes no route-path change as ECMP graph does
        // not change if the link is a parallel link (same src-dst as another link.
        // However we still need to update ECMP hash groups to include new buckets
        // for the link that has come up.
        if (mastershipService.isLocalMaster(link.src().deviceId())) {
            if (!seenBefore && isParallelLink(link)) {
                // if link seen first time, we need to ensure hash-groups have all ports
                log.debug("Attempting retryHash for paralled first-time link {}", link);
                groupHandler.retryHash(link, false, true);
            } else {
                //seen before-link
                if (isParallelLink(link)) {
                    log.debug("Attempting retryHash for paralled seen-before "
                            + "link {}", link);
                    groupHandler.retryHash(link, false, false);
                }
            }
        }

        mcastHandler.init();
    }

    private void processLinkRemoved(Link link) {
        log.info("** LINK REMOVED {}", link.toString());
        if (!isLinkValid(link)) {
            return;
        }
        defaultRoutingHandler.populateRoutingRulesForLinkStatusChange(link, null, null);

        // update local groupHandler stores
        DefaultGroupHandler groupHandler = groupHandlerMap.get(link.src().deviceId());
        if (groupHandler != null) {
            if (mastershipService.isLocalMaster(link.src().deviceId()) &&
                    isParallelLink(link)) {
                log.debug("* retrying hash for parallel link removed:{}", link);
                groupHandler.retryHash(link, true, false);
            } else {
                log.debug("Not attempting retry-hash for link removed: {} .. {}", link,
                          (mastershipService.isLocalMaster(link.src().deviceId()))
                                  ? "not parallel" : "not master");
            }
            // ensure local stores are updated
            groupHandler.portDown(link.src().port());
        } else {
            log.warn("group handler not found for dev:{} when removing link: {}",
                     link.src().deviceId(), link);
        }

        mcastHandler.processLinkDown(link);
        l2TunnelHandler.processLinkDown(link);
    }

    private boolean isLinkValid(Link link) {
        if (link.type() != Link.Type.DIRECT) {
            // NOTE: A DIRECT link might be transiently marked as INDIRECT
            // if BDDP is received before LLDP. We can safely ignore that
            // until the LLDP is received and the link is marked as DIRECT.
            log.info("Ignore link {}->{}. Link type is {} instead of DIRECT.",
                     link.src(), link.dst(), link.type());
            return false;
        }
        DeviceId srcId = link.src().deviceId();
        DeviceId dstId = link.dst().deviceId();
        if (srcId.equals(dstId)) {
            log.warn("Links between ports on the same switch are not "
                    + "allowed .. ignoring link {}", link);
            return false;
        }
        try {
            if (!deviceConfiguration.isEdgeDevice(srcId)
                    && !deviceConfiguration.isEdgeDevice(dstId)) {
                // ignore links between spines
                // XXX revisit when handling multi-stage fabrics
                log.warn("Links between spines not allowed...ignoring "
                        + "link {}", link);
                return false;
            }
            if (deviceConfiguration.isEdgeDevice(srcId)
                    && deviceConfiguration.isEdgeDevice(dstId)) {
                // ignore links between leaves if they are not pair-links
                // XXX revisit if removing pair-link config or allowing more than
                // one pair-link
                if (deviceConfiguration.getPairDeviceId(srcId).equals(dstId)
                        && deviceConfiguration.getPairLocalPort(srcId)
                                .equals(link.src().port())
                        && deviceConfiguration.getPairLocalPort(dstId)
                                .equals(link.dst().port())) {
                    // found pair link - allow it
                    return true;
                } else {
                    log.warn("Links between leaves other than pair-links are "
                            + "not allowed...ignoring link {}", link);
                    return false;
                }
            }
        } catch (DeviceConfigNotFoundException e) {
            // We still want to count the links in seenLinks even though there
            // is no config. So we let it return true
            log.warn("Could not check validity of link {} as subtending devices "
                    + "are not yet configured", link);
        }
        return true;
    }

    private void processDeviceAdded(Device device) {
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
            hostHandler.init(deviceId);
            xConnectHandler.init(deviceId);
            DefaultGroupHandler groupHandler = groupHandlerMap.get(deviceId);
            groupHandler.createGroupsFromVlanConfig();
            routingRulePopulator.populateSubnetBroadcastRule(deviceId);
        }

        appCfgHandler.init(deviceId);
        routeHandler.init(deviceId);
    }

    private void processDeviceRemoved(Device device) {
        dsNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(device.id()))
                .forEach(entry -> dsNextObjStore.remove(entry.getKey()));
        vlanNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(device.id()))
                .forEach(entry -> vlanNextObjStore.remove(entry.getKey()));
        portNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(device.id()))
                .forEach(entry -> portNextObjStore.remove(entry.getKey()));

        seenLinks.keySet().removeIf(key -> key.src().deviceId().equals(device.id()) ||
                key.dst().deviceId().equals(device.id()));

        DefaultGroupHandler gh = groupHandlerMap.remove(device.id());
        if (gh != null) {
            gh.shutdown();
        }
        defaultRoutingHandler.purgeEcmpGraph(device.id());
        // Note that a switch going down is associated with all of its links
        // going down as well, but it is treated as a single switch down event
        // while the link-downs are ignored.
        defaultRoutingHandler
            .populateRoutingRulesForLinkStatusChange(null, null, device.id());
        mcastHandler.removeDevice(device.id());
        xConnectHandler.removeDevice(device.id());
    }

    private void processPortUpdated(Device device, Port port) {
        if (deviceConfiguration == null || !deviceConfiguration.isConfigured(device.id())) {
            log.warn("Device configuration uploading. Not handling port event for"
                    + "dev: {} port: {}", device.id(), port.number());
            return;
        }

        if (!mastershipService.isLocalMaster(device.id()))  {
            log.debug("Not master for dev:{} .. not handling port updated event"
                    + "for port {}", device.id(), port.number());
            return;
        }

        // first we handle filtering rules associated with the port
        if (port.isEnabled()) {
            log.info("Switchport {}/{} enabled..programming filters",
                     device.id(), port.number());
            routingRulePopulator.processSinglePortFilters(device.id(), port.number(), true);
        } else {
            log.info("Switchport {}/{} disabled..removing filters",
                     device.id(), port.number());
            routingRulePopulator.processSinglePortFilters(device.id(), port.number(), false);
        }

        // portUpdated calls are for ports that have gone down or up. For switch
        // to switch ports, link-events should take care of any re-routing or
        // group editing necessary for port up/down. Here we only process edge ports
        // that are already configured.
        ConnectPoint cp = new ConnectPoint(device.id(), port.number());
        VlanId untaggedVlan = getUntaggedVlanId(cp);
        VlanId nativeVlan = getNativeVlanId(cp);
        Set<VlanId> taggedVlans = getTaggedVlanId(cp);

        if (untaggedVlan == null && nativeVlan == null && taggedVlans.isEmpty()) {
            log.debug("Not handling port updated event for non-edge port (unconfigured) "
                    + "dev/port: {}/{}", device.id(), port.number());
            return;
        }
        if (untaggedVlan != null) {
            processEdgePort(device, port, untaggedVlan, true);
        }
        if (nativeVlan != null) {
            processEdgePort(device, port, nativeVlan, true);
        }
        if (!taggedVlans.isEmpty()) {
            taggedVlans.forEach(tag -> processEdgePort(device, port, tag, false));
        }
    }

    private void processEdgePort(Device device, Port port, VlanId vlanId,
                                 boolean popVlan) {
        boolean portUp = port.isEnabled();
        if (portUp) {
            log.info("Device:EdgePort {}:{} is enabled in vlan: {}", device.id(),
                     port.number(), vlanId);
            hostHandler.processPortUp(new ConnectPoint(device.id(), port.number()));
        } else {
            log.info("Device:EdgePort {}:{} is disabled in vlan: {}", device.id(),
                     port.number(), vlanId);
        }

        DefaultGroupHandler groupHandler = groupHandlerMap.get(device.id());
        if (groupHandler != null) {
            groupHandler.processEdgePort(port.number(), vlanId, popVlan, portUp);
        } else {
            log.warn("Group handler not found for dev:{}. Not handling edge port"
                    + " {} event for port:{}", device.id(),
                    (portUp) ? "UP" : "DOWN", port.number());
        }
    }

    private void createOrUpdateDeviceConfiguration() {
        if (deviceConfiguration == null) {
            deviceConfiguration = new DeviceConfiguration(this);
        } else {
            deviceConfiguration.updateConfig();
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
            createOrUpdateDeviceConfiguration();

            arpHandler = new ArpHandler(srManager);
            icmpHandler = new IcmpHandler(srManager);
            ipHandler = new IpHandler(srManager);
            routingRulePopulator = new RoutingRulePopulator(srManager);
            defaultRoutingHandler = new DefaultRoutingHandler(srManager);

            tunnelHandler = new TunnelHandler(linkService, deviceConfiguration,
                                              groupHandlerMap, tunnelStore);
            policyHandler = new PolicyHandler(appId, deviceConfiguration,
                                              flowObjectiveService,
                                              tunnelHandler, policyStore);
            // add a small delay to absorb multiple network config added notifications
            if (!programmingScheduled.get()) {
                programmingScheduled.set(true);
                executorService.schedule(new ConfigChange(), PROGRAM_DELAY,
                                         TimeUnit.SECONDS);
            }
            mcastHandler.init();
        }

        @Override
        public void event(NetworkConfigEvent event) {
            // TODO move this part to NetworkConfigEventHandler
            if (event.configClass().equals(SegmentRoutingDeviceConfig.class)) {
                switch (event.type()) {
                    case CONFIG_ADDED:
                        log.info("Segment Routing Device Config added for {}", event.subject());
                        configureNetwork();
                        break;
                    case CONFIG_UPDATED:
                        log.info("Segment Routing Config updated for {}", event.subject());
                        createOrUpdateDeviceConfiguration();
                        // TODO support dynamic configuration
                        break;
                    default:
                        break;
                }
            } else if (event.configClass().equals(InterfaceConfig.class)) {
                switch (event.type()) {
                    case CONFIG_ADDED:
                        log.info("Interface Config added for {}", event.subject());
                        configureNetwork();
                        break;
                    case CONFIG_UPDATED:
                        log.info("Interface Config updated for {}", event.subject());
                        createOrUpdateDeviceConfiguration();

                        // Following code will be uncommented when [CORD-634] is fully implemented.
                        // [CORD-634] Add dynamic config support for interfaces
                        updateInterface((InterfaceConfig) event.config().get(),
                                        (InterfaceConfig) event.prevConfig().get());
                        // TODO support dynamic configuration
                        break;
                    default:
                        break;
                }
            } else if (event.configClass().equals(SegmentRoutingAppConfig.class)) {
                checkState(appCfgHandler != null, "NetworkConfigEventHandler is not initialized");
                switch (event.type()) {
                    case CONFIG_ADDED:
                        appCfgHandler.processAppConfigAdded(event);
                        break;
                    case CONFIG_UPDATED:
                        appCfgHandler.processAppConfigUpdated(event);
                        break;
                    case CONFIG_REMOVED:
                        appCfgHandler.processAppConfigRemoved(event);
                        break;
                    default:
                        break;
                }
                configureNetwork();
            } else if (event.configClass().equals(XConnectConfig.class)) {
                checkState(xConnectHandler != null, "XConnectHandler is not initialized");
                switch (event.type()) {
                    case CONFIG_ADDED:
                        xConnectHandler.processXConnectConfigAdded(event);
                        break;
                    case CONFIG_UPDATED:
                        xConnectHandler.processXConnectConfigUpdated(event);
                        break;
                    case CONFIG_REMOVED:
                        xConnectHandler.processXConnectConfigRemoved(event);
                        break;
                    default:
                        break;
                }
            } else if (event.configClass().equals(PwaasConfig.class)) {
                checkState(l2TunnelHandler != null, "L2TunnelHandler is not initialized");
                switch (event.type()) {
                    case CONFIG_ADDED:
                        l2TunnelHandler.processPwaasConfigAdded(event);
                        break;
                    case CONFIG_UPDATED:
                        l2TunnelHandler.processPwaasConfigUpdated(event);
                        break;
                    case CONFIG_REMOVED:
                        l2TunnelHandler.processPwaasConfigRemoved(event);
                        break;
                    default:
                        break;
                }
            }
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            if (event.configClass().equals(SegmentRoutingDeviceConfig.class) ||
                    event.configClass().equals(SegmentRoutingAppConfig.class) ||
                    event.configClass().equals(InterfaceConfig.class) ||
                    event.configClass().equals(XConnectConfig.class) ||
                    event.configClass().equals(PwaasConfig.class)) {
                return true;
            }
            log.debug("Ignore irrelevant event class {}", event.configClass().getName());
            return false;
        }

        private final class ConfigChange implements Runnable {
            @Override
            public void run() {
                programmingScheduled.set(false);
                for (Device device : deviceService.getDevices()) {
                    processDeviceAdded(device);
                }
                defaultRoutingHandler.startPopulationProcess();
            }
        }
    }

    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
                case HOST_ADDED:
                    hostHandler.processHostAddedEvent(event);
                    break;
                case HOST_MOVED:
                    hostHandler.processHostMovedEvent(event);
                    routeHandler.processHostMovedEvent(event);
                    break;
                case HOST_REMOVED:
                    hostHandler.processHostRemovedEvent(event);
                    break;
                case HOST_UPDATED:
                    hostHandler.processHostUpdatedEvent(event);
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
            switch (event.type()) {
                case SOURCE_ADDED:
                    mcastHandler.processSourceAdded(event);
                    break;
                case SINK_ADDED:
                    mcastHandler.processSinkAdded(event);
                    break;
                case SINK_REMOVED:
                    mcastHandler.processSinkRemoved(event);
                    break;
                case ROUTE_ADDED:
                case ROUTE_REMOVED:
                default:
                    break;
            }
        }
    }

    private class InternalRouteEventListener implements RouteListener {
        @Override
        public void event(RouteEvent event) {
            switch (event.type()) {
                case ROUTE_ADDED:
                    routeHandler.processRouteAdded(event);
                    break;
                case ROUTE_UPDATED:
                    routeHandler.processRouteUpdated(event);
                    break;
                case ROUTE_REMOVED:
                    routeHandler.processRouteRemoved(event);
                    break;
                case ALTERNATIVE_ROUTES_CHANGED:
                    routeHandler.processAlternativeRoutesChanged(event);
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

            Interface intf = intfs.stream().findFirst().get();
            Interface prevIntf = prevIntfs.stream().findFirst().get();

            DeviceId deviceId = intf.connectPoint().deviceId();
            PortNumber portNum = intf.connectPoint().port();

            if (!mastershipService.isLocalMaster(deviceId)) {
                log.debug("CONFIG_UPDATED event for interfaces should be " +
                                  "handled by master node for device {}", deviceId);
                return;
            }

            removeSubnetConfig(prevIntf.connectPoint(),
                               Sets.difference(new HashSet<>(prevIntf.ipAddressesList()),
                                               new HashSet<>(intf.ipAddressesList())));

            if (prevIntf.vlanNative() != VlanId.NONE && !intf.vlanNative().equals(prevIntf.vlanNative())) {
                // RemoveVlanNative
                updateVlanConfigInternal(deviceId, portNum, prevIntf.vlanNative(), true, false);
            }

            if (!prevIntf.vlanTagged().isEmpty() && !intf.vlanTagged().equals(prevIntf.vlanTagged())) {
                // RemoveVlanTagged
                prevIntf.vlanTagged().stream().filter(i -> !intf.vlanTagged().contains(i)).forEach(
                        vlanId -> updateVlanConfigInternal(deviceId, portNum, vlanId, false, false)
                );
            }

            if (prevIntf.vlanUntagged() != VlanId.NONE && !intf.vlanUntagged().equals(prevIntf.vlanUntagged())) {
                // RemoveVlanUntagged
                updateVlanConfigInternal(deviceId, portNum, prevIntf.vlanUntagged(), true, false);
            }

            if (intf.vlanNative() != VlanId.NONE && !prevIntf.vlanNative().equals(intf.vlanNative())) {
                // AddVlanNative
                updateVlanConfigInternal(deviceId, portNum, intf.vlanNative(), true, true);
            }

            if (!intf.vlanTagged().isEmpty() && !intf.vlanTagged().equals(prevIntf.vlanTagged())) {
                // AddVlanTagged
                intf.vlanTagged().stream().filter(i -> !prevIntf.vlanTagged().contains(i)).forEach(
                        vlanId -> updateVlanConfigInternal(deviceId, portNum, vlanId, false, true)
                );
            }

            if (intf.vlanUntagged() != VlanId.NONE && !prevIntf.vlanUntagged().equals(intf.vlanUntagged())) {
                // AddVlanUntagged
                updateVlanConfigInternal(deviceId, portNum, intf.vlanUntagged(), true, true);
            }
            addSubnetConfig(prevIntf.connectPoint(),
                            Sets.difference(new HashSet<>(intf.ipAddressesList()),
                                            new HashSet<>(prevIntf.ipAddressesList())));
        } catch (ConfigException e) {
            log.error("Error in configuration");
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
            // Update next objective for a single port as an output port
            // Remove a single port from L2FG
            grpHandler.updateGroupFromVlanConfiguration(portNum, Collections.singleton(vlanId), nextId, install);
            // Remove L2 Bridging rule and L3 Unicast rule to the host
            hostHandler.processIntfVlanUpdatedEvent(deviceId, portNum, vlanId, pushVlan, install);
            // Remove broadcast forwarding rule and corresponding L2FG for VLAN
            // only if there is no port configured on that VLAN ID
            if (!getVlanPortMap(deviceId).containsKey(vlanId)) {
                // Remove broadcast forwarding rule for the VLAN
                routingRulePopulator.updateSubnetBroadcastRule(deviceId, vlanId, install);
                // Remove L2FG for VLAN
                grpHandler.removeBcastGroupFromVlan(deviceId, portNum, vlanId, pushVlan);
            } else {
                // Remove L2IG of the port
                grpHandler.removePortNextObjective(deviceId, portNum, vlanId, pushVlan);
            }
        } else if (install) {
            if (nextId != -1) {
                // Add a single port to L2FG
                grpHandler.updateGroupFromVlanConfiguration(portNum, Collections.singleton(vlanId), nextId, install);
            } else {
                // Create L2FG for VLAN
                grpHandler.createBcastGroupFromVlan(vlanId, Collections.singleton(portNum));
                routingRulePopulator.updateSubnetBroadcastRule(deviceId, vlanId, install);
            }
            hostHandler.processIntfVlanUpdatedEvent(deviceId, portNum, vlanId, pushVlan, install);
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

        defaultRoutingHandler.revokeSubnet(
                ipPrefixSet.stream()
                        .filter(ipPrefix -> !deviceIpPrefixSet.contains(ipPrefix))
                        .collect(Collectors.toSet()));

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
        hostHandler.processIntfIpUpdatedEvent(cp, ipPrefixSet, false);
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

        defaultRoutingHandler.populateSubnet(
                Collections.singleton(cp),
                ipPrefixSet.stream()
                        .filter(ipPrefix -> !deviceIpPrefixSet.contains(ipPrefix))
                        .collect(Collectors.toSet()));

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
        hostHandler.processIntfIpUpdatedEvent(cp, ipPrefixSet, true);
    }
}
