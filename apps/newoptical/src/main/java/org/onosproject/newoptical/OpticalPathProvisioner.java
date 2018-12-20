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
package org.onosproject.newoptical;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.graph.DefaultEdgeWeigher;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onlab.util.Bandwidth;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.event.ListenerTracker;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BandwidthCapacity;
import org.onosproject.net.config.basics.BasicLinkConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.newoptical.api.OpticalConnectivityId;
import org.onosproject.newoptical.api.OpticalPathEvent;
import org.onosproject.newoptical.api.OpticalPathListener;
import org.onosproject.newoptical.api.OpticalPathService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.LinkKey.linkKey;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;
import static org.onosproject.newoptical.OsgiPropertyConstants.MAX_PATHS;
import static org.onosproject.newoptical.OsgiPropertyConstants.MAX_PATHS_DEFAULT;

/**
 * Main component to configure optical connectivity.
 */
@Beta
@Component(
    immediate = true,
    service = OpticalPathService.class,
    property = {
        MAX_PATHS + ":Integer=" + MAX_PATHS_DEFAULT
    }
)
public class OpticalPathProvisioner
        extends AbstractListenerManager<OpticalPathEvent, OpticalPathListener>
        implements OpticalPathService {

    private static final Logger log = LoggerFactory.getLogger(OpticalPathProvisioner.class);

    /**
     * Bandwidth representing no bandwidth requirement specified.
     */
    private static final Bandwidth NO_BW_REQUIREMENT = Bandwidth.bps(0);

    private static final String OPTICAL_CONNECTIVITY_ID_COUNTER = "optical-connectivity-id";
    private static final String LINKPATH_MAP_NAME = "newoptical-linkpath";
    private static final String CONNECTIVITY_MAP_NAME = "newoptical-connectivity";
    private static final String CROSSCONNECTLINK_SET_NAME = "newoptical-crossconnectlink";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ResourceService resourceService;

    /** Maximum number of paths to consider for path provisioning. */
    private int maxPaths = MAX_PATHS_DEFAULT;

    private ApplicationId appId;

    private AtomicCounter idCounter;

    private ListenerTracker listeners;

    private InternalStoreListener storeListener = new InternalStoreListener();

    /**
     * Map from packet-layer link expected to be realized by some optical Intent to
     * OpticalConnectivity (~=top level intent over multi-layer topology).
     */
    private ConsistentMap<PacketLinkRealizedByOptical, OpticalConnectivity> linkPathMap;

    private ConsistentMap<OpticalConnectivityId, OpticalConnectivity> connectivityMap;

    // FIXME in the long run. This is effectively app's own resource subsystem
    /**
     * Set of cross connect link currently used.
     */
    private DistributedSet<Link> usedCrossConnectLinkSet;

    private static final KryoNamespace.Builder LINKPATH_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(PacketLinkRealizedByOptical.class)
            .register(OpticalConnectivityId.class)
            .register(OpticalConnectivity.class);

    private static final KryoNamespace.Builder CONNECTIVITY_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(PacketLinkRealizedByOptical.class)
            .register(OpticalConnectivityId.class)
            .register(OpticalConnectivity.class);

    private static final KryoNamespace.Builder CROSSCONNECTLINKS_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API);

    @Activate
    protected void activate(ComponentContext context) {
        deviceService = opticalView(deviceService);
        appId = coreService.registerApplication("org.onosproject.newoptical");

        idCounter = storageService.getAtomicCounter(OPTICAL_CONNECTIVITY_ID_COUNTER);

        linkPathMap = storageService.<PacketLinkRealizedByOptical, OpticalConnectivity>consistentMapBuilder()
                .withSerializer(Serializer.using(LINKPATH_SERIALIZER.build()))
                .withName(LINKPATH_MAP_NAME)
                .withApplicationId(appId)
                .build();

        connectivityMap = storageService.<OpticalConnectivityId, OpticalConnectivity>consistentMapBuilder()
                .withSerializer(Serializer.using(CONNECTIVITY_SERIALIZER.build()))
                .withName(CONNECTIVITY_MAP_NAME)
                .withApplicationId(appId)
                .build();

        usedCrossConnectLinkSet = storageService.<Link>setBuilder()
                .withSerializer(Serializer.using(CROSSCONNECTLINKS_SERIALIZER.build()))
                .withName(CROSSCONNECTLINK_SET_NAME)
                .withApplicationId(appId)
                .build()
                .asDistributedSet();

        eventDispatcher.addSink(OpticalPathEvent.class, listenerRegistry);

        listeners = new ListenerTracker();
        listeners.addListener(linkService, new InternalLinkListener())
                .addListener(intentService, new InternalIntentListener());

        linkPathMap.addListener(storeListener);

        readComponentConfiguration(context);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        linkPathMap.removeListener(storeListener);
        listeners.removeListeners();
        eventDispatcher.removeSink(OpticalPathEvent.class);

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        maxPaths = Tools.getIntegerProperty(properties, MAX_PATHS, MAX_PATHS_DEFAULT);
        log.info("Configured. Maximum paths to consider is configured to {}", maxPaths);
    }

    @Override
    public Collection<OpticalConnectivity> listConnectivity() {
        return connectivityMap.values().stream()
            .map(Versioned::value)
            .collect(ImmutableList.toImmutableList());
    }

    @Override
    public Set<Key> listIntents(OpticalConnectivityId id) {
        return linkPathMap.entrySet().stream()
            .filter(ent -> id.equals(ent.getValue().value().id()))
            .map(Entry::getKey)
            .map(PacketLinkRealizedByOptical::realizingIntentKey)
            .collect(Collectors.toSet());
    }

    /*
     * Request packet-layer connectivity between specified ports,
     * over packet-optical multi-layer infrastructure.
     *
     * Functionality-wise this is effectively submitting Packet-Optical
     * multi-layer P2P Intent.
     *
     * It computes multi-layer path meeting specified constraint,
     * and calls setupPath.
     */
    @Override
    public OpticalConnectivityId setupConnectivity(ConnectPoint ingress, ConnectPoint egress,
                                                   Bandwidth bandwidth, Duration latency) {
        checkNotNull(ingress);
        checkNotNull(egress);
        log.info("setupConnectivity({}, {}, {}, {})", ingress, egress, bandwidth, latency);

        Bandwidth bw = (bandwidth == null) ? NO_BW_REQUIREMENT : bandwidth;

        Stream<Path> paths = topologyService.getKShortestPaths(
                topologyService.currentTopology(),
                ingress.deviceId(), egress.deviceId(),
                new BandwidthLinkWeight(bandwidth));

        // Path service calculates from node to node, we're only interested in port to port
        Optional<OpticalConnectivityId> id =
                paths.filter(p -> p.src().equals(ingress) && p.dst().equals(egress))
                        .limit(maxPaths)
                        .map(p -> setupPath(p, bw, latency))
                        .filter(Objects::nonNull)
                        .findFirst();

        if (id.isPresent()) {
            log.info("Assigned OpticalConnectivityId: {}", id);
        } else {
            log.error("setupConnectivity({}, {}, {}, {}) failed.", ingress, egress, bandwidth, latency);
        }

        return id.orElse(null);
    }

    /*
     * Given a multi-layer path,
     * compute a set of segments which requires
     * OpticalConnectivity(~=OpticalConnectivityIntent or OpticalCircuitPath)
     * to provide packet-layer connectivity.
     */
    @Override
    public OpticalConnectivityId setupPath(Path path, Bandwidth bandwidth, Duration latency) {
        checkNotNull(path);
        log.debug("setupPath({}, {}, {})", path, bandwidth, latency);

        // map of cross connect points (optical port -> packet port)
        Map<ConnectPoint, ConnectPoint> crossConnectPointMap = new HashMap<>();

        // list of (src, dst) pair of optical ports between which optical path should be installed
        List<Pair<ConnectPoint, ConnectPoint>> crossConnectPoints = new ArrayList<>();

        // Scan path to find pairs of connect points between which optical intent is installed
        // opticalSrcPort works as a flag parameter to show scanning status
        ConnectPoint opticalSrcPort = null;
        for (Link link : path.links()) {
            if (!isCrossConnectLink(link)) {
                continue;
            }

            if (opticalSrcPort != null) {
                // opticalSrcPort!=null means src port was already found
                // in this case link.src() is optical layer, and link.dst() is packet layer

                // Check if types of src port and dst port matches
                Device srcDevice = checkNotNull(deviceService.getDevice(opticalSrcPort.deviceId()),
                        "Unknown device ID");
                Device dstDevice = checkNotNull(deviceService.getDevice(link.src().deviceId()),
                        "Unknown device ID");
                if (srcDevice.type() != dstDevice.type()) {
                    log.error("Unsupported mix of cross connect points : {}, {}",
                            srcDevice.type(), dstDevice.type());
                    return null;
                }

                // Update cross connect points map
                crossConnectPointMap.put(link.src(), link.dst());

                // Add optical ports pair to list
                crossConnectPoints.add(Pair.of(opticalSrcPort, link.src()));

                // Reset flag parameter
                opticalSrcPort = null;
            } else {
                // opticalSrcPort==null means src port was not found yet
                // in this case link.src() is packet layer, and link.dst() is optical layer

                // Update cross connect points map
                crossConnectPointMap.put(link.dst(), link.src());
                // Set opticalSrcPort to src of link (optical port)
                opticalSrcPort = link.dst();
            }
        }

        // create intents from cross connect points
        List<Intent> intents = createIntents(crossConnectPoints);
        if (intents.isEmpty()) {
            log.error("No intents produced from {}", crossConnectPoints);
            return null;
        }

        // create set of PacketLinkRealizedByOptical
        Set<PacketLinkRealizedByOptical> packetLinks = createPacketLinkSet(crossConnectPoints,
                intents, crossConnectPointMap);

        // create OpticalConnectivity object and store information to distributed store
        OpticalConnectivity connectivity = createConnectivity(path, bandwidth, latency, packetLinks);

        // store cross connect port usage
        path.links().stream().filter(this::isCrossConnectLink)
                .forEach(usedCrossConnectLinkSet::add);

        // Submit the intents
        for (Intent i : intents) {
            intentService.submit(i);
            log.debug("Submitted an intent: {}", i);
        }

        return connectivity.id();
    }

    private OpticalConnectivity createConnectivity(Path path, Bandwidth bandwidth, Duration latency,
                                                   Set<PacketLinkRealizedByOptical> links) {
        OpticalConnectivityId id = OpticalConnectivityId.of(idCounter.getAndIncrement());
        OpticalConnectivity connectivity = new OpticalConnectivity(id, path.links(), bandwidth, latency,
                links, Collections.emptySet());

        links.forEach(l -> linkPathMap.put(l, connectivity));

        // store connectivity information
        connectivityMap.put(connectivity.id(), connectivity);

        return connectivity;
    }

    @Override
    public boolean removeConnectivity(OpticalConnectivityId id) {
        log.info("removeConnectivity({})", id);
        Versioned<OpticalConnectivity> connectivity = connectivityMap.remove(id);

        if (connectivity == null) {
            log.info("OpticalConnectivity with id {} not found.", id);
            return false;
        }

        // TODO withdraw intent only if all of connectivities that use the optical path are withdrawn
        connectivity.value().getRealizingLinks().forEach(l -> {
            Intent intent = intentService.getIntent(l.realizingIntentKey());
            intentService.withdraw(intent);
        });

        return true;
    }

    @Override
    public Optional<List<Link>> getPath(OpticalConnectivityId id) {
        Versioned<OpticalConnectivity> connectivity = connectivityMap.get(id);
        if (connectivity == null) {
            log.info("OpticalConnectivity with id {} not found.", id);
            return Optional.empty();
        }

        return Optional.of(ImmutableList.copyOf(connectivity.value().links()));
    }

    /**
     * Scans the list of cross connection points and returns a list of optical connectivity intents.
     * During the process, save information about packet links to given set.
     *
     * @param crossConnectPoints list of (src, dst) pair between which optical path will be set up
     * @return list of optical connectivity intents
     */
    private List<Intent> createIntents(List<Pair<ConnectPoint, ConnectPoint>> crossConnectPoints) {
        List<Intent> intents = new LinkedList<>();
        Iterator<Pair<ConnectPoint, ConnectPoint>> itr = crossConnectPoints.iterator();

        while (itr.hasNext()) {
            // checkArgument at start ensures we'll always have pairs of connect points
            Pair<ConnectPoint, ConnectPoint> next = itr.next();
            ConnectPoint src = next.getLeft();
            ConnectPoint dst = next.getRight();

            Port srcPort = deviceService.getPort(src.deviceId(), src.port());
            Port dstPort = deviceService.getPort(dst.deviceId(), dst.port());

            if (srcPort instanceof OduCltPort && dstPort instanceof OduCltPort) {
                OduCltPort srcOCPort = (OduCltPort) srcPort;
                OduCltPort dstOCPort = (OduCltPort) dstPort;
                if (!srcOCPort.signalType().equals(dstOCPort.signalType())) {
                    continue;
                }

                // Create OTN circuit
                OpticalCircuitIntent circuitIntent = OpticalCircuitIntent.builder()
                        .appId(appId)
                        .src(src)
                        .dst(dst)
                        .signalType(srcOCPort.signalType())
                        .bidirectional(false)
                        .build();
                intents.add(circuitIntent);
            } else if (srcPort instanceof OchPort && dstPort instanceof OchPort) {
                OchPort srcOchPort = (OchPort) srcPort;
                OchPort dstOchPort = (OchPort) dstPort;
                if (!srcOchPort.signalType().equals(dstOchPort.signalType())) {
                    continue;
                }

                // Create lightpath
                OpticalConnectivityIntent opticalIntent = OpticalConnectivityIntent.builder()
                        .appId(appId)
                        .src(src)
                        .dst(dst)
                        .signalType(srcOchPort.signalType())
                        .bidirectional(false)
                        .build();
                intents.add(opticalIntent);
            } else {
                log.warn("Unsupported cross connect point types {} {}", srcPort.type(), dstPort.type());
                return Collections.emptyList();
            }
        }

        return intents;
    }

    private Set<PacketLinkRealizedByOptical> createPacketLinkSet(List<Pair<ConnectPoint, ConnectPoint>> connectPoints,
                                                                 List<Intent> intents,
                                                                 Map<ConnectPoint, ConnectPoint> crossConnectPoints) {
        checkArgument(connectPoints.size() == intents.size());

        Set<PacketLinkRealizedByOptical> pLinks = new HashSet<>();

        Iterator<Pair<ConnectPoint, ConnectPoint>> xcPointsItr = connectPoints.iterator();
        Iterator<Intent> intentItr = intents.iterator();
        while (xcPointsItr.hasNext()) {
            Pair<ConnectPoint, ConnectPoint> xcPoints = xcPointsItr.next();
            Intent intent = intentItr.next();

            ConnectPoint packetSrc = checkNotNull(crossConnectPoints.get(xcPoints.getLeft()));
            ConnectPoint packetDst = checkNotNull(crossConnectPoints.get(xcPoints.getRight()));

            if (intent instanceof OpticalConnectivityIntent) {
                pLinks.add(PacketLinkRealizedByOptical.create(packetSrc, packetDst,
                        (OpticalConnectivityIntent) intent));
            } else if (intent instanceof OpticalCircuitIntent) {
                pLinks.add(PacketLinkRealizedByOptical.create(packetSrc, packetDst,
                        (OpticalCircuitIntent) intent));
            } else {
                log.warn("Unexpected intent type: {}", intent.getClass());
            }
        }

        return pLinks;
    }

    /**
     * Verifies if given device type is in packet layer, i.e., ROADM, OTN or ROADM_OTN device.
     *
     * @param type device type
     * @return true if in packet layer, false otherwise
     */
    private boolean isPacketLayer(Device.Type type) {
        return type == Device.Type.SWITCH || type == Device.Type.ROUTER || type == Device.Type.VIRTUAL;
    }

    /**
     * Verifies if given device type is NOT in packet layer, i.e., switch or router device.
     *
     * @param type device type
     * @return true if in optical layer, false otherwise
     */
    private boolean isTransportLayer(Device.Type type) {
        return type == Device.Type.ROADM || type == Device.Type.OTN || type == Device.Type.ROADM_OTN ||
                type == Device.Type.OLS ||  type == Device.Type.TERMINAL_DEVICE;
    }

    /**
     * Verifies if given link forms a cross-connection between packet and optical layer.
     *
     * @param link the link
     * @return true if the link is a cross-connect link, false otherwise
     */
    private boolean isCrossConnectLink(Link link) {
        if (link.type() != Link.Type.OPTICAL) {
            return false;
        }

        Device.Type src = deviceService.getDevice(link.src().deviceId()).type();
        Device.Type dst = deviceService.getDevice(link.dst().deviceId()).type();

        return src != dst &&
                ((isPacketLayer(src) && isTransportLayer(dst)) || (isPacketLayer(dst) && isTransportLayer(src)));
    }

    /**
     * Updates bandwidth resource of given connect point to specified value.
     *
     * @param cp Connect point
     * @param bandwidth New bandwidth
     */
    private void setPortBandwidth(ConnectPoint cp, Bandwidth bandwidth) {
        log.debug("update Port {} Bandwidth {}", cp, bandwidth);
        BandwidthCapacity bwCapacity = networkConfigService.addConfig(cp, BandwidthCapacity.class);
        bwCapacity.capacity(bandwidth).apply();
    }

    /**
     * Updates usage information of bandwidth based on connectivity which is established.
     * @param connectivity Optical connectivity
     */
    private void updateBandwidthUsage(OpticalConnectivity connectivity) {
        if (NO_BW_REQUIREMENT.equals(connectivity.bandwidth())) {
            // no bandwidth requirement, nothing to allocate.
            return;
        }

        OpticalConnectivityId connectivityId = connectivity.id();

        List<Link> links = connectivity.links();

        List<Resource> resources = links.stream().flatMap(l -> Stream.of(l.src(), l.dst()))
                .filter(cp -> !isTransportLayer(deviceService.getDevice(cp.deviceId()).type()))
                .map(cp -> Resources.continuous(cp.deviceId(), cp.port(),
                        Bandwidth.class).resource(connectivity.bandwidth().bps()))
                .collect(Collectors.toList());

        log.debug("allocating bandwidth for {} : {}", connectivityId, resources);
        List<ResourceAllocation> allocations = resourceService.allocate(connectivityId, resources);
        if (allocations.isEmpty()) {
            log.warn("Failed to allocate bandwidth {} to {}",
                    connectivity.bandwidth().bps(), resources);
            // TODO any recovery?
        }
        log.debug("Done allocating bandwidth for {}", connectivityId);
    }

    /**
     * Release bandwidth allocated by given connectivity.
     * @param connectivity Optical connectivity
     */
    private void releaseBandwidthUsage(OpticalConnectivity connectivity) {
        if (connectivity.links().isEmpty()) {
            return;
        }
        if (NO_BW_REQUIREMENT.equals(connectivity.bandwidth())) {
            // no bandwidth requirement, nothing to release.
            return;
        }


        // release resource only if this node is the master for link head device
        if (mastershipService.isLocalMaster(connectivity.links().get(0).src().deviceId())) {
            OpticalConnectivityId connectivityId = connectivity.id();

            log.debug("releasing bandwidth allocated to {}", connectivityId);
            if (!resourceService.release(connectivityId)) {
                log.warn("Failed to release bandwidth allocated to {}",
                        connectivityId);
                // TODO any recovery?
            }
            log.debug("DONE releasing bandwidth for {}", connectivityId);
        }
    }

    private boolean linkDiscoveryEnabled(ConnectPoint cp) {
        // FIXME should check Device feature and configuration state.

        // short-term hack for ONS'17 time-frame,
        // only expect OF device to have link discovery.
        return "of".equals(cp.deviceId().uri().getScheme());
    }

    /**
     * Returns true if both connect point support for link discovery & enabled.
     *
     * @param cp1 port 1
     * @param cp2 port 2
     * @return true if both connect point support for link discovery & enabled.
     */
    private boolean linkDiscoveryEnabled(ConnectPoint cp1, ConnectPoint cp2) {
        return linkDiscoveryEnabled(cp1) && linkDiscoveryEnabled(cp2);
    }

    private class BandwidthLinkWeight extends DefaultEdgeWeigher<TopologyVertex, TopologyEdge> implements LinkWeigher {
        private Bandwidth bandwidth = null;

        public BandwidthLinkWeight(Bandwidth bandwidth) {
            this.bandwidth = bandwidth;
        }

        @Override
        public Weight weight(TopologyEdge edge) {
            Link l = edge.link();

            // Avoid inactive links
            if (l.state() == Link.State.INACTIVE) {
                log.trace("{} is not active", l);
                return ScalarWeight.NON_VIABLE_WEIGHT;
            }

            // Avoid cross connect links with used ports
            if (isCrossConnectLink(l) && usedCrossConnectLinkSet.contains(l)) {
                log.trace("Cross connect {} in use", l);
                return ScalarWeight.NON_VIABLE_WEIGHT;
            }

            // Check availability of bandwidth
            if (bandwidth != null && !NO_BW_REQUIREMENT.equals(bandwidth)) {
                if (hasEnoughBandwidth(l.src()) && hasEnoughBandwidth(l.dst())) {
                    return new ScalarWeight(1.0);
                } else {
                    log.trace("Not enough bandwidth on {}", l);
                    return ScalarWeight.NON_VIABLE_WEIGHT;
                }
            // Allow everything else
            } else {
                return new ScalarWeight(1.0);
            }
        }

        private boolean hasEnoughBandwidth(ConnectPoint cp) {
            if (cp.elementId() instanceof DeviceId) {
                Device device = deviceService.getDevice(cp.deviceId());
                Device.Type type = device.type();

                if (isTransportLayer(type)) {
                    // Check if the port has enough capacity
                    Port port = deviceService.getPort(cp.deviceId(), cp.port());
                    if (port instanceof OduCltPort || port instanceof OchPort) {
                        // Port with capacity
                        return bandwidth.bps() < port.portSpeed() * 1000000.0;
                    } else {
                        // Port without valid capacity (OMS port, etc.)
                        return true;
                    }
                } else {
                    // Check if enough amount of bandwidth resource remains
                    ContinuousResource resource = Resources.continuous(cp.deviceId(), cp.port(), Bandwidth.class)
                            .resource(bandwidth.bps());
                    try {
                        return resourceService.isAvailable(resource);
                    } catch (Exception e) {
                        log.error("Resource service failed checking availability of {}",
                                resource, e);
                        throw e;
                    }
                }
            }
            return false;
        }
    }

    public class InternalIntentListener implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            switch (event.type()) {
                case INSTALLED:
                    log.debug("Intent {} installed.", event.subject());
                    updateCrossConnectLink(event.subject());
                    break;
                case WITHDRAWN:
                    log.debug("Intent {} withdrawn.", event.subject());
                    removeCrossConnectLinks(event.subject());
                    break;
                case FAILED:
                    log.debug("Intent {} failed.", event.subject());
                    // TODO If it was one of it's own optical Intent,
                    // update link state
                    // TODO If it was packet P2P Intent, call setupConnectivity
                    break;
                default:
                    break;
            }
        }

        // TODO rename "CrossConnectLink"?
        /**
         * Update packet-layer link/port state once Intent is installed.
         *
         * @param intent which reached installed state
         */
        private void updateCrossConnectLink(Intent intent) {
            linkPathMap.entrySet().stream()
                    .filter(e -> e.getKey().realizingIntentKey().equals(intent.key()))
                    .forEach(e -> {
                        ConnectPoint packetSrc = e.getKey().src();
                        ConnectPoint packetDst = e.getKey().dst();
                        Bandwidth bw = e.getKey().bandwidth();

                        // reflect modification only if packetSrc is local_
                        if (mastershipService.isLocalMaster(packetSrc.deviceId())) {
                            // Updates bandwidth of packet ports
                            setPortBandwidth(packetSrc, bw);
                            setPortBandwidth(packetDst, bw);

                            // Updates link status in distributed map
                            linkPathMap.computeIfPresent(e.getKey(), (link, connectivity) ->
                                    e.getValue().value().setLinkEstablished(packetSrc, packetDst, true));


                            if (!linkDiscoveryEnabled(packetSrc, packetDst)) {
                                injectLink(packetSrc, packetDst);
                            }
                        }
                    });
        }

        private void removeCrossConnectLinks(Intent intent) {
            ConnectPoint src, dst;

            if (intent instanceof OpticalCircuitIntent) {
                OpticalCircuitIntent circuit = (OpticalCircuitIntent) intent;
                src = circuit.getSrc();
                dst = circuit.getDst();
            } else if (intent instanceof OpticalConnectivityIntent) {
                OpticalConnectivityIntent conn = (OpticalConnectivityIntent) intent;
                src = conn.getSrc();
                dst = conn.getDst();
            } else {
                return;
            }

            removeXcLinkUsage(src);
            removeXcLinkUsage(dst);

            // Set bandwidth of 0 to cross connect ports
            Bandwidth bw = Bandwidth.bps(0);
            linkPathMap.entrySet().stream()
                    .filter(e -> e.getKey().realizingIntentKey().equals(intent.key()))
                    .forEach(e -> {
                        ConnectPoint packetSrc = e.getKey().src();
                        ConnectPoint packetDst = e.getKey().dst();

                        // reflect modification only if packetSrc is local_
                        if (mastershipService.isLocalMaster(packetSrc.deviceId())) {
                            // Updates bandwidth of packet ports
                            setPortBandwidth(packetSrc, bw);
                            setPortBandwidth(packetDst, bw);

                            // Updates link status in distributed map
                            linkPathMap.computeIfPresent(e.getKey(), (link, connectivity) ->
                                    e.getValue().value().setLinkEstablished(packetSrc, packetDst, false));


                            if (!linkDiscoveryEnabled(packetSrc, packetDst)) {
                                removeInjectedLink(packetSrc, packetDst);
                            }
                        }
                    });
        }

        private void removeXcLinkUsage(ConnectPoint cp) {
            Optional<Link> link = linkService.getLinks(cp).stream()
                    .filter(usedCrossConnectLinkSet::contains)
                    .findAny();

            if (!link.isPresent()) {
                log.warn("Cross connect point {} has no cross connect link to release.", cp);
                return;
            }

            usedCrossConnectLinkSet.remove(link.get());
        }

        /**
         * Injects link between specified packet port.
         *
         * @param packetSrc port 1
         * @param packetDst port 2
         */
        private void injectLink(ConnectPoint packetSrc,
                                ConnectPoint packetDst) {
            // inject expected link or durable link
            // if packet device cannot advertise packet link
            try {
                // cannot call addConfig.
                // it will create default BasicLinkConfig,
                // which will end up advertising DIRECT links and
                // DIRECT Link type cannot transition from DIRECT to INDIRECT
                LinkKey lnkKey = linkKey(packetSrc, packetDst);
                BasicLinkConfig lnkCfg = networkConfigService
                        .getConfig(lnkKey, BasicLinkConfig.class);
                if (lnkCfg == null) {
                    lnkCfg = new BasicLinkConfig(lnkKey);
                }
                lnkCfg.isAllowed(true);
                lnkCfg.isDurable(true);
                lnkCfg.type(Link.Type.INDIRECT);
                lnkCfg.isBidirectional(false);
                // cannot call apply against manually created instance
                //lnkCfg.apply();
                networkConfigService.applyConfig(lnkKey,
                                                 BasicLinkConfig.class,
                                                 lnkCfg.node());

            } catch (Exception ex) {
                log.error("Applying BasicLinkConfig failed", ex);
            }
        }

        /**
         * Removes link injected between specified packet port.
         *
         * @param packetSrc port 1
         * @param packetDst port 2
         */
        private void removeInjectedLink(ConnectPoint packetSrc,
                                        ConnectPoint packetDst) {
            // remove expected link or durable link
            // if packet device cannot monitor packet link

            try {
                // hack to mark link off-line
                BasicLinkConfig lnkCfg = networkConfigService
                        .getConfig(linkKey(packetSrc, packetDst),
                                   BasicLinkConfig.class);
                lnkCfg.isAllowed(false);
                lnkCfg.apply();
            } catch (Exception ex) {
                log.error("Applying BasicLinkConfig failed", ex);
            }

            networkConfigService
                .removeConfig(linkKey(packetSrc, packetDst),
                              BasicLinkConfig.class);
        }
    }


    private class InternalLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            switch (event.type()) {
                case LINK_REMOVED:
                    Link link = event.subject();
                    // updates linkPathMap only if src device of link is local
                    if (!mastershipService.isLocalMaster(link.src().deviceId())) {
                        return;
                    }

                    // find all packet links that correspond to removed link
                    Set<PacketLinkRealizedByOptical> pLinks = linkPathMap.keySet().stream()
                            .filter(l -> l.isBetween(link.src(), link.dst()) || l.isBetween(link.dst(), link.src()))
                            .collect(Collectors.toSet());

                    pLinks.forEach(l -> {
                        // remove found packet links from distributed store
                        linkPathMap.computeIfPresent(l, (plink, conn) -> {
                            // Notifies listeners if all packet links are gone
                            if (conn.isAllRealizingLinkNotEstablished()) {
                                post(new OpticalPathEvent(OpticalPathEvent.Type.PATH_REMOVED, conn.id()));
                            }
                            return null;
                        });
                    });
                default:
                    break;
            }
        }
    }

    private class InternalStoreListener
            implements MapEventListener<PacketLinkRealizedByOptical, OpticalConnectivity> {

        @Override
        public void event(MapEvent<PacketLinkRealizedByOptical, OpticalConnectivity> event) {
            switch (event.type()) {
                case UPDATE:
                    OpticalConnectivity oldConnectivity = event.oldValue().value();
                    OpticalConnectivity newConnectivity = event.newValue().value();

                    if (!oldConnectivity.isAllRealizingLinkEstablished() &&
                            newConnectivity.isAllRealizingLinkEstablished()) {
                        // Notifies listeners if all links are established
                        updateBandwidthUsage(newConnectivity);
                        post(new OpticalPathEvent(OpticalPathEvent.Type.PATH_INSTALLED, newConnectivity.id()));
                    } else if (!oldConnectivity.isAllRealizingLinkNotEstablished() &&
                            newConnectivity.isAllRealizingLinkNotEstablished()) {
                        // Notifies listeners if all links are gone
                        releaseBandwidthUsage(newConnectivity);
                        post(new OpticalPathEvent(OpticalPathEvent.Type.PATH_REMOVED, newConnectivity.id()));
                    }

                    break;
                default:
                    break;
            }
        }

    }
}

