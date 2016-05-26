/*
 * Copyright 2016 Open Networking Laboratory
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
import com.google.common.collect.Sets;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Bandwidth;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
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
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private Map<PacketLinkRealizedByOptical, OpticalConnectivity> linkPathMap = new ConcurrentHashMap<>();

    // TODO this should be stored to distributed store
    private Map<OpticalConnectivityId, OpticalConnectivity> connectivities = new ConcurrentHashMap<>();

    // TODO this should be stored to distributed store
    // Map of cross connect link and installed path which uses the link
    private Set<Link> usedCrossConnectLinks = Sets.newConcurrentHashSet();

    @Activate
    protected void activate() {
        deviceService = opticalView(deviceService);
        appId = coreService.registerApplication("org.onosproject.newoptical");

        idCounter = storageService.atomicCounterBuilder()
                .withName(OPTICAL_CONNECTIVITY_ID_COUNTER)
                .withMeteringDisabled()
                .build()
                .asAtomicCounter();

        eventDispatcher.addSink(OpticalPathEvent.class, listenerRegistry);

        listeners = new ListenerTracker();
        listeners.addListener(linkService, new InternalLinkListener())
                .addListener(intentService, new InternalIntentListener());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
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

        // validate optical path
        List<Pair<ConnectPoint, ConnectPoint>> xcPointPairs = getCrossConnectPoints(path);
        if (!checkXcPoints(xcPointPairs)) {
            // Can't setup path if cross connect points are mismatched
            log.error("Failed to setup path because of mismatched cross connect points.");
            return null;
        }

        OpticalConnectivity connectivity = createConnectivity(path, bandwidth, latency);

        // create intents from cross connect points and set connectivity information
        List<Intent> intents = createIntents(xcPointPairs, connectivity);

        // store cross connect port usage
        path.links().stream().filter(this::isCrossConnectLink)
                .forEach(usedCrossConnectLinks::add);

        // Submit the intents
        for (Intent i : intents) {
            intentService.submit(i);
            log.debug("Submitted an intent: {}", i);
        }

        return connectivity.id();
    }

    private OpticalConnectivity createConnectivity(Path path, Bandwidth bandwidth, Duration latency) {
        OpticalConnectivityId id = OpticalConnectivityId.of(idCounter.getAndIncrement());
        OpticalConnectivity connectivity = new OpticalConnectivity(id, path, bandwidth, latency);

        // store connectivity information
        connectivities.put(connectivity.id(), connectivity);

        return connectivity;
    }

    @Override
    public boolean removeConnectivity(OpticalConnectivityId id) {
        log.info("removeConnectivity({})", id);
        OpticalConnectivity connectivity = connectivities.remove(id);

        if (connectivity == null) {
            log.info("OpticalConnectivity with id {} not found.", id);
            return false;
        }

        // TODO withdraw intent only if all of connectivities that use the optical path are withdrawn
        connectivity.getRealizingLinks().forEach(l -> {
            Intent intent = intentService.getIntent(l.realizingIntentKey());
            intentService.withdraw(intent);
        });

        return true;
    }

    @Override
    public Optional<List<Link>> getPath(OpticalConnectivityId id) {
        OpticalConnectivity connectivity = connectivities.get(id);
        if (connectivity == null) {
            log.info("OpticalConnectivity with id {} not found.", id);
            return Optional.empty();
        }

        return Optional.of(ImmutableList.copyOf(connectivity.links()));
    }

    /**
     * Returns list of (optical, packet) pairs of cross connection points of missing optical path sections.
     *
     * Scans the given multi-layer path and looks for sections that use cross connect links.
     * The ingress and egress points in the optical layer are combined to the packet layer ports, and
     * are returned in a list.
     *
     * @param path the multi-layer path
     * @return List of cross connect link's (packet port, optical port) pairs
     */
    private List<Pair<ConnectPoint, ConnectPoint>> getCrossConnectPoints(Path path) {
        List<Pair<ConnectPoint, ConnectPoint>> xcPointPairs = new LinkedList<>();
        boolean scanning = false;

        for (Link link : path.links()) {
            if (!isCrossConnectLink(link)) {
                continue;
            }

            if (scanning) {
                // link.src() is packet, link.dst() is optical
                xcPointPairs.add(Pair.of(checkNotNull(link.src()), checkNotNull(link.dst())));
                scanning = false;
            } else {
                // link.src() is optical, link.dst() is packet
                xcPointPairs.add(Pair.of(checkNotNull(link.dst()), checkNotNull(link.src())));
                scanning = true;
            }
        }

        return xcPointPairs;
    }

    /**
     * Checks if optical cross connect points are of same type.
     *
     * @param xcPointPairs list of cross connection points
     * @return true if cross connect point pairs are of same type, false otherwise
     */
    private boolean checkXcPoints(List<Pair<ConnectPoint, ConnectPoint>> xcPointPairs) {
        checkArgument(xcPointPairs.size() % 2 == 0);

        Iterator<Pair<ConnectPoint, ConnectPoint>> itr = xcPointPairs.iterator();

        while (itr.hasNext()) {
            // checkArgument at start ensures we'll always have pairs of connect points
            Pair<ConnectPoint, ConnectPoint> src = itr.next();
            Pair<ConnectPoint, ConnectPoint> dst = itr.next();

            Device.Type srcType = deviceService.getDevice(src.getKey().deviceId()).type();
            Device.Type dstType = deviceService.getDevice(dst.getKey().deviceId()).type();

            // Only support connections between identical port types
            if (srcType != dstType) {
                log.warn("Unsupported mix of cross connect points : {}, {}", srcType, dstType);
                return false;
            }
        }

        return true;
    }

    /**
     * Scans the list of cross connection points and returns a list of optical connectivity intents.
     * During the process, store intent ID and its realizing link information to given connectivity object.
     *
     * @param xcPointPairs list of cross connection points
     * @return list of optical connectivity intents
     */
    private List<Intent> createIntents(List<Pair<ConnectPoint, ConnectPoint>> xcPointPairs,
                                       OpticalConnectivity connectivity) {
        checkArgument(xcPointPairs.size() % 2 == 0);

        List<Intent> intents = new LinkedList<>();
        Iterator<Pair<ConnectPoint, ConnectPoint>> itr = xcPointPairs.iterator();

        while (itr.hasNext()) {
            // checkArgument at start ensures we'll always have pairs of connect points
            Pair<ConnectPoint, ConnectPoint> src = itr.next();
            Pair<ConnectPoint, ConnectPoint> dst = itr.next();

            Port srcPort = deviceService.getPort(src.getKey().deviceId(), src.getKey().port());
            Port dstPort = deviceService.getPort(dst.getKey().deviceId(), dst.getKey().port());

            if (srcPort instanceof OduCltPort && dstPort instanceof OduCltPort) {
                OduCltPort srcOCPort = (OduCltPort) srcPort;
                OduCltPort dstOCPort = (OduCltPort) dstPort;
                if (!srcOCPort.signalType().equals(dstOCPort.signalType())) {
                    continue;
                }

                // Create OTN circuit
                OpticalCircuitIntent circuitIntent = OpticalCircuitIntent.builder()
                        .appId(appId)
                        .src(src.getKey())
                        .dst(dst.getKey())
                        .signalType(srcOCPort.signalType())
                        .bidirectional(true)
                        .build();
                intents.add(circuitIntent);
                PacketLinkRealizedByOptical pLink = PacketLinkRealizedByOptical.create(src.getValue(), dst.getValue(),
                        circuitIntent);
                connectivity.addRealizingLink(pLink);
                linkPathMap.put(pLink, connectivity);
            } else if (srcPort instanceof OchPort && dstPort instanceof OchPort) {
                OchPort srcOchPort = (OchPort) srcPort;
                OchPort dstOchPort = (OchPort) dstPort;
                if (!srcOchPort.signalType().equals(dstOchPort.signalType())) {
                    continue;
                }

                // Create lightpath
                OpticalConnectivityIntent opticalIntent = OpticalConnectivityIntent.builder()
                        .appId(appId)
                        .src(src.getKey())
                        .dst(dst.getKey())
                        .signalType(srcOchPort.signalType())
                        .bidirectional(true)
                        .build();
                intents.add(opticalIntent);
                PacketLinkRealizedByOptical pLink = PacketLinkRealizedByOptical.create(src.getValue(), dst.getValue(),
                        opticalIntent);
                connectivity.addRealizingLink(pLink);
                linkPathMap.put(pLink, connectivity);
            } else {
                log.warn("Unsupported cross connect point types {} {}", srcPort.type(), dstPort.type());
                return Collections.emptyList();
            }
        }

        return intents;
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
     * @param cp Connect point
     * @param bandwidth New bandwidth
     */
    private void updatePortBandwidth(ConnectPoint cp, Bandwidth bandwidth) {
        NodeId localNode = clusterService.getLocalNode().id();
        NodeId sourceMaster = mastershipService.getMasterFor(cp.deviceId());
        if (localNode.equals(sourceMaster)) {
            log.debug("update Port {} Bandwidth {}", cp, bandwidth);
            BandwidthCapacity bwCapacity = networkConfigService.addConfig(cp, BandwidthCapacity.class);
            bwCapacity.capacity(bandwidth).apply();
        }
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
        OpticalConnectivityId connectivityId = connectivity.id();

        log.debug("releasing bandwidth allocated to {}", connectivityId);
        if (!resourceService.release(connectivityId)) {
            log.warn("Failed to release bandwidth allocated to {}",
                    connectivityId);
            // TODO any recovery?
        }
        log.debug("DONE releasing bandwidth for {}", connectivityId);
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
            if (isCrossConnectLink(l) && usedCrossConnectLinks.contains(l)) {
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
                        // Updates bandwidth of packet ports
                        updatePortBandwidth(packetSrc, bw);
                        updatePortBandwidth(packetDst, bw);

                        OpticalConnectivity connectivity = e.getValue();
                        connectivity.setLinkEstablished(packetSrc, packetDst);

                        if (e.getValue().isAllRealizingLinkEstablished()) {
                            updateBandwidthUsage(connectivity);

                            // Notifies listeners if all links are established
                            post(new OpticalPathEvent(OpticalPathEvent.Type.PATH_INSTALLED, e.getValue().id()));
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
                        // Updates bandwidth of packet ports
                        updatePortBandwidth(packetSrc, bw);
                        updatePortBandwidth(packetDst, bw);
                        OpticalConnectivity connectivity = e.getValue();
                        connectivity.setLinkRemoved(packetSrc, packetDst);

                        // Notifies listeners if all links are gone
                        if (e.getValue().isAllRealizingLinkNotEstablished()) {
                            releaseBandwidthUsage(connectivity);
                            post(new OpticalPathEvent(OpticalPathEvent.Type.PATH_REMOVED, e.getValue().id()));
                        }
                    });
        }

        private void removeXcLinkUsage(ConnectPoint cp) {
            Optional<Link> link = linkService.getLinks(cp).stream()
                    .filter(usedCrossConnectLinks::contains)
                    .findAny();

            if (!link.isPresent()) {
                log.warn("Cross connect point {} has no cross connect link.", cp);
                return;
            }

            usedCrossConnectLinks.remove(link.get());
        }
    }


    private class InternalLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            switch (event.type()) {
                case LINK_REMOVED:
                    Link link = event.subject();
                    Set<PacketLinkRealizedByOptical> pLinks = linkPathMap.keySet().stream()
                            .filter(l -> l.isBetween(link.src(), link.dst()) || l.isBetween(link.dst(), link.src()))
                            .collect(Collectors.toSet());

                    pLinks.forEach(l -> {
                        OpticalConnectivity c = linkPathMap.get(l);
                        // Notifies listeners if all links are gone
                        if (c.isAllRealizingLinkNotEstablished()) {
                            post(new OpticalPathEvent(OpticalPathEvent.Type.PATH_REMOVED, c.id()));
                        }
                        linkPathMap.remove(l);
                    });
                default:
                    break;
            }
        }
    }
}

