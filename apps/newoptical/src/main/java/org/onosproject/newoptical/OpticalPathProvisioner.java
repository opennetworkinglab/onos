/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Bandwidth;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerTracker;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.newoptical.api.OpticalConnectivityId;
import org.onosproject.newoptical.api.OpticalPathEvent;
import org.onosproject.newoptical.api.OpticalPathListener;
import org.onosproject.newoptical.api.OpticalPathService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.config.basics.BandwidthCapacity;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

/**
 * Main component to configure optical connectivity.
 */
@Beta
@Service
@Component(immediate = true)
public class OpticalPathProvisioner
        extends AbstractListenerManager<OpticalPathEvent, OpticalPathListener>
        implements OpticalPathService {
    protected static final Logger log = LoggerFactory.getLogger(OpticalPathProvisioner.class);

    private static final String OPTICAL_CONNECTIVITY_ID_COUNTER = "optical-connectivity-id";
    private static final String LINKPATH_MAP_NAME = "newoptical-linkpath";
    private static final String CONNECTIVITY_MAP_NAME = "newoptical-connectivity";
    private static final String CROSSCONNECTLINK_SET_NAME = "newoptical-crossconnectlink";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;


    private ApplicationId appId;

    private AtomicCounter idCounter;

    private ListenerTracker listeners;

    private InternalStoreListener storeListener = new InternalStoreListener();

    private ConsistentMap<PacketLinkRealizedByOptical, OpticalConnectivity> linkPathMap;

    private ConsistentMap<OpticalConnectivityId, OpticalConnectivity> connectivityMap;

    // Map of cross connect link and installed path which uses the link
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
    protected void activate() {
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

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        linkPathMap.removeListener(storeListener);
        listeners.removeListeners();
        eventDispatcher.removeSink(OpticalPathEvent.class);

        log.info("Stopped");
    }

    @Override
    public OpticalConnectivityId setupConnectivity(ConnectPoint ingress, ConnectPoint egress,
                                                   Bandwidth bandwidth, Duration latency) {
        checkNotNull(ingress);
        checkNotNull(egress);
        log.info("setupConnectivity({}, {}, {}, {})", ingress, egress, bandwidth, latency);

        bandwidth = (bandwidth == null) ? Bandwidth.bps(0) : bandwidth;

        Set<Path> paths = pathService.getPaths(ingress.deviceId(), egress.deviceId(),
                new BandwidthLinkWeight(bandwidth));
        if (paths.isEmpty()) {
            log.warn("Unable to find multi-layer path.");
            return null;
        }

        // Search path with available cross connect points
        for (Path path : paths) {
            OpticalConnectivityId id = setupPath(path, bandwidth, latency);
            if (id != null) {
                log.info("Assigned OpticalConnectivityId: {}", id);
                return id;
            }
        }

        log.info("setupConnectivity({}, {}, {}, {}) failed.", ingress, egress, bandwidth, latency);

        return null;
    }

    @Override
    public OpticalConnectivityId setupPath(Path path, Bandwidth bandwidth, Duration latency) {
        checkNotNull(path);
        log.info("setupPath({}, {}, {})", path, bandwidth, latency);

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
                        .bidirectional(true)
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
                        .bidirectional(true)
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
     * Verifies if given device type is in packet layer, i.e., switch or router device.
     *
     * @param type device type
     * @return true if in packet layer, false otherwise
     */
    private boolean isTransportLayer(Device.Type type) {
        return type == Device.Type.ROADM || type == Device.Type.OTN || type == Device.Type.ROADM_OTN;
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
     * Updates bandwidth resource of given connect point.
     *
     * @param cp Connect point
     * @param bandwidth New bandwidth
     */
    private void updatePortBandwidth(ConnectPoint cp, Bandwidth bandwidth) {
        log.debug("update Port {} Bandwidth {}", cp, bandwidth);
        BandwidthCapacity bwCapacity = networkConfigService.addConfig(cp, BandwidthCapacity.class);
        bwCapacity.capacity(bandwidth).apply();
    }

    /**
     * Updates usage information of bandwidth based on connectivity which is established.
     * @param connectivity Optical connectivity
     */
    private void updateBandwidthUsage(OpticalConnectivity connectivity) {
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

    private class BandwidthLinkWeight implements LinkWeight {
        private Bandwidth bandwidth = null;

        public BandwidthLinkWeight(Bandwidth bandwidth) {
            this.bandwidth = bandwidth;
        }

        @Override
        public double weight(TopologyEdge edge) {
            Link l = edge.link();

            // Avoid inactive links
            if (l.state() == Link.State.INACTIVE) {
                return -1.0;
            }

            // Avoid cross connect links with used ports
            if (isCrossConnectLink(l) && usedCrossConnectLinkSet.contains(l)) {
                return -1.0;
            }

            // Check availability of bandwidth
            if (bandwidth != null) {
                if (hasEnoughBandwidth(l.src()) && hasEnoughBandwidth(l.dst())) {
                    return 1.0;
                } else {
                    return -1.0;
                }
            } else {
                // TODO needs to differentiate optical and packet?
                if (l.type() == Link.Type.OPTICAL) {
                    // Transport links
                    return 1.0;
                } else {
                    // Packet links
                    return 1.0;
                }
            }
        }

        private boolean hasEnoughBandwidth(ConnectPoint cp) {
            if (cp.elementId() instanceof DeviceId) {
                Device device =  deviceService.getDevice(cp.deviceId());
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
                    return resourceService.isAvailable(resource);
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
                    log.info("Intent {} installed.", event.subject());
                    updateCrossConnectLink(event.subject());
                    break;
                case WITHDRAWN:
                    log.info("Intent {} withdrawn.", event.subject());
                    removeCrossConnectLinks(event.subject());
                    break;
                case FAILED:
                    log.info("Intent {} failed.", event.subject());
                    break;
                default:
                    break;
            }
        }

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
                            updatePortBandwidth(packetSrc, bw);
                            updatePortBandwidth(packetDst, bw);

                            // Updates link status in distributed map
                            linkPathMap.computeIfPresent(e.getKey(), (link, connectivity) ->
                                    e.getValue().value().setLinkEstablished(packetSrc, packetDst, true));
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
                            updatePortBandwidth(packetSrc, bw);
                            updatePortBandwidth(packetDst, bw);

                            // Updates link status in distributed map
                            linkPathMap.computeIfPresent(e.getKey(), (link, connectivity) ->
                                    e.getValue().value().setLinkEstablished(packetSrc, packetDst, false));
                        }
                    });
        }

        private void removeXcLinkUsage(ConnectPoint cp) {
            Optional<Link> link = linkService.getLinks(cp).stream()
                    .filter(usedCrossConnectLinkSet::contains)
                    .findAny();

            if (!link.isPresent()) {
                log.warn("Cross connect point {} has no cross connect link.", cp);
                return;
            }

            usedCrossConnectLinkSet.remove(link.get());
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

