/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.segmentrouting.mcast;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mcast.api.McastEvent;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.McastRouteData;
import org.onosproject.mcast.api.McastRouteUpdate;
import org.onosproject.net.HostId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.segmentrouting.SRLinkWeigher;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.storekey.McastStoreKey;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;

import static org.onosproject.mcast.api.McastEvent.Type.ROUTE_ADDED;
import static org.onosproject.mcast.api.McastEvent.Type.ROUTE_REMOVED;
import static org.onosproject.mcast.api.McastEvent.Type.SOURCES_ADDED;
import static org.onosproject.mcast.api.McastEvent.Type.SOURCES_REMOVED;
import static org.onosproject.mcast.api.McastEvent.Type.SINKS_ADDED;
import static org.onosproject.mcast.api.McastEvent.Type.SINKS_REMOVED;

import static org.onosproject.segmentrouting.mcast.McastRole.EGRESS;
import static org.onosproject.segmentrouting.mcast.McastRole.INGRESS;
import static org.onosproject.segmentrouting.mcast.McastRole.TRANSIT;

/**
 * Handles Multicast related events.
 */
public class McastHandler {
    // Logger instance
    private static final Logger log = LoggerFactory.getLogger(McastHandler.class);
    // Reference to srManager and most used internal objects
    private final SegmentRoutingManager srManager;
    private final TopologyService topologyService;
    private final McastUtils mcastUtils;
    // Internal store of the Mcast nextobjectives
    private final ConsistentMap<McastStoreKey, NextObjective> mcastNextObjStore;
    // Internal store of the Mcast roles
    private final ConsistentMap<McastStoreKey, McastRole> mcastRoleStore;

    // Wait time for the cache
    private static final int WAIT_TIME_MS = 1000;

    /**
     * The mcastEventCache is implemented to avoid race condition by giving more time to the
     * underlying subsystems to process previous calls.
     */
    private Cache<McastCacheKey, McastEvent> mcastEventCache = CacheBuilder.newBuilder()
            .expireAfterWrite(WAIT_TIME_MS, TimeUnit.MILLISECONDS)
            .removalListener((RemovalNotification<McastCacheKey, McastEvent> notification) -> {
                // Get group ip, sink and related event
                IpAddress mcastIp = notification.getKey().mcastIp();
                HostId sink = notification.getKey().sinkHost();
                McastEvent mcastEvent = notification.getValue();
                RemovalCause cause = notification.getCause();
                log.debug("mcastEventCache removal event. group={}, sink={}, mcastEvent={}, cause={}",
                          mcastIp, sink, mcastEvent, cause);
                // If it expires or it has been replaced, we deque the event
                switch (notification.getCause()) {
                    case REPLACED:
                    case EXPIRED:
                        dequeueMcastEvent(mcastEvent);
                        break;
                    default:
                        break;
                }
            }).build();

    private void enqueueMcastEvent(McastEvent mcastEvent) {
        // Retrieve, currentData, prevData and the group
        final McastRouteUpdate mcastRouteUpdate = mcastEvent.subject();
        final McastRouteUpdate mcastRoutePrevUpdate = mcastEvent.prevSubject();
        final IpAddress group = mcastRoutePrevUpdate.route().group();
        // Let's create the keys of the cache
        ImmutableSet.Builder<HostId> sinksBuilder = ImmutableSet.builder();
        if (mcastEvent.type() == SOURCES_ADDED ||
                mcastEvent.type() == SOURCES_REMOVED) {
            // FIXME To be addressed with multiple sources support
            sinksBuilder.addAll(Collections.emptySet());
        } else if (mcastEvent.type() == SINKS_ADDED) {
            // We need to process the host id one by one
            mcastRouteUpdate.sinks().forEach(((hostId, connectPoints) -> {
                // Get the previous locations and verify if there are changes
                Set<ConnectPoint> prevConnectPoints = mcastRoutePrevUpdate.sinks().get(hostId);
                Set<ConnectPoint> changes = Sets.difference(connectPoints, prevConnectPoints != null ?
                        prevConnectPoints : Collections.emptySet());
                if (!changes.isEmpty()) {
                    sinksBuilder.add(hostId);
                }
            }));
        } else if (mcastEvent.type() == SINKS_REMOVED) {
            // We need to process the host id one by one
            mcastRoutePrevUpdate.sinks().forEach(((hostId, connectPoints) -> {
                // Get the current locations and verify if there are changes
                Set<ConnectPoint> currentConnectPoints = mcastRouteUpdate.sinks().get(hostId);
                Set<ConnectPoint> changes = Sets.difference(connectPoints, currentConnectPoints != null ?
                        currentConnectPoints : Collections.emptySet());
                if (!changes.isEmpty()) {
                    sinksBuilder.add(hostId);
                }
            }));
        } else if (mcastEvent.type() == ROUTE_REMOVED) {
            // Current subject is null, just take the previous host ids
            sinksBuilder.addAll(mcastRoutePrevUpdate.sinks().keySet());
        }
        // Push the elements in the cache
        sinksBuilder.build().forEach(sink -> {
            McastCacheKey cacheKey = new McastCacheKey(group, sink);
            mcastEventCache.put(cacheKey, mcastEvent);
        });
    }

    private void dequeueMcastEvent(McastEvent mcastEvent) {
        // Get new and old data
        final McastRouteUpdate mcastUpdate = mcastEvent.subject();
        final McastRouteUpdate mcastPrevUpdate = mcastEvent.prevSubject();
        // Get source, mcast group
        // FIXME To be addressed with multiple sources support
        final ConnectPoint source = mcastPrevUpdate.sources()
                .stream()
                .findFirst()
                .orElse(null);
        IpAddress mcastIp = mcastPrevUpdate.route().group();
        // Get all the previous sinks
        Set<ConnectPoint> prevSinks = mcastPrevUpdate.sinks()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        // According to the event type let's call the proper method
        switch (mcastEvent.type()) {
            case SOURCES_ADDED:
                // FIXME To be addressed with multiple sources support
                // Get all the sinks
                //Set<ConnectPoint> sinks = mcastRouteInfo.sinks();
                // Compute the Mcast tree
                //Map<ConnectPoint, List<Path>> mcasTree = computeSinkMcastTree(source.deviceId(), sinks);
                // Process the given sinks using the pre-computed paths
                //mcasTree.forEach((sink, paths) -> processSinkAddedInternal(source, sink, mcastIp, paths));
                break;
            case SOURCES_REMOVED:
                // FIXME To be addressed with multiple sources support
                // Get old source
                //ConnectPoint oldSource = mcastEvent.prevSubject().source().orElse(null);
                // Just the first cached element will be processed
                //processSourceUpdatedInternal(mcastIp, source, oldSource);
                break;
            case ROUTE_REMOVED:
                // Process the route removed, just the first cached element will be processed
                processRouteRemovedInternal(source, mcastIp);
                break;
            case SINKS_ADDED:
                // FIXME To be addressed with multiple sources support
                processSinksAddedInternal(source, mcastIp,
                                          mcastUpdate.sinks(), prevSinks);
                break;
            case SINKS_REMOVED:
                // FIXME To be addressed with multiple sources support
                processSinksRemovedInternal(source, mcastIp,
                                            mcastUpdate.sinks(), mcastPrevUpdate.sinks());
                break;
            default:
                break;
        }
    }

    // Mcast lock to serialize local operations
    private final Lock mcastLock = new ReentrantLock();

    /**
     * Acquires the lock used when making mcast changes.
     */
    private void mcastLock() {
        mcastLock.lock();
    }

    /**
     * Releases the lock used when making mcast changes.
     */
    private void mcastUnlock() {
        mcastLock.unlock();
    }

    // Stability threshold for Mcast. Seconds
    private static final long MCAST_STABLITY_THRESHOLD = 5;
    // Last change done
    private Instant lastMcastChange = Instant.now();

    /**
     * Determines if mcast in the network has been stable in the last
     * MCAST_STABLITY_THRESHOLD seconds, by comparing the current time
     * to the last mcast change timestamp.
     *
     * @return true if stable
     */
    private boolean isMcastStable() {
        long last = (long) (lastMcastChange.toEpochMilli() / 1000.0);
        long now = (long) (Instant.now().toEpochMilli() / 1000.0);
        log.trace("Mcast stable since {}s", now - last);
        return (now - last) > MCAST_STABLITY_THRESHOLD;
    }

    // Verify interval for Mcast
    private static final long MCAST_VERIFY_INTERVAL = 30;

    // Executor for mcast bucket corrector
    private ScheduledExecutorService executorService
            = newScheduledThreadPool(1, groupedThreads("mcastWorker", "mcastWorker-%d", log));

    /**
     * Constructs the McastEventHandler.
     *
     * @param srManager Segment Routing manager
     */
    public McastHandler(SegmentRoutingManager srManager) {
        ApplicationId coreAppId = srManager.coreService.getAppId(CoreService.CORE_APP_NAME);
        this.srManager = srManager;
        this.topologyService = srManager.topologyService;
        KryoNamespace.Builder mcastKryo = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(McastStoreKey.class)
                .register(McastRole.class);
        mcastNextObjStore = srManager.storageService
                .<McastStoreKey, NextObjective>consistentMapBuilder()
                .withName("onos-mcast-nextobj-store")
                .withSerializer(Serializer.using(mcastKryo.build("McastHandler-NextObj")))
                .build();
        mcastRoleStore = srManager.storageService
                .<McastStoreKey, McastRole>consistentMapBuilder()
                .withName("onos-mcast-role-store")
                .withSerializer(Serializer.using(mcastKryo.build("McastHandler-Role")))
                .build();
        // Let's create McastUtils object
        mcastUtils = new McastUtils(srManager, coreAppId, log);
        // Init the executor service and the buckets corrector
        executorService.scheduleWithFixedDelay(new McastBucketCorrector(), 10,
                                               MCAST_VERIFY_INTERVAL, TimeUnit.SECONDS);
        // Schedule the clean up, this will allow the processing of the expired events
        executorService.scheduleAtFixedRate(mcastEventCache::cleanUp, 0,
                                            WAIT_TIME_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Read initial multicast from mcast store.
     */
    public void init() {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            srManager.multicastRouteService.getRoutes().forEach(mcastRoute -> {
                // Verify leadership on the operation
                if (!mcastUtils.isLeader(mcastRoute.group())) {
                    log.debug("Skip {} due to lack of leadership", mcastRoute.group());
                    return;
                }
                // FIXME To be addressed with multiple sources support
                ConnectPoint source = srManager.multicastRouteService.sources(mcastRoute)
                        .stream()
                        .findFirst()
                        .orElse(null);
                // Get all the sinks and process them
                McastRouteData mcastRouteData = srManager.multicastRouteService.routeData(mcastRoute);
                Set<ConnectPoint> sinks = processSinksToBeAdded(source, mcastRoute.group(), mcastRouteData.sinks());
                // Filter out all the working sinks, we do not want to move them
                sinks = sinks.stream()
                        .filter(sink -> {
                            McastStoreKey mcastKey = new McastStoreKey(mcastRoute.group(), sink.deviceId());
                            Versioned<NextObjective> verMcastNext = mcastNextObjStore.get(mcastKey);
                            return verMcastNext == null ||
                                    !mcastUtils.getPorts(verMcastNext.value().next()).contains(sink.port());
                        })
                        .collect(Collectors.toSet());
                // Compute the Mcast tree
                Map<ConnectPoint, List<Path>> mcasTree = computeSinkMcastTree(source.deviceId(), sinks);
                // Process the given sinks using the pre-computed paths
                mcasTree.forEach((sink, paths) -> processSinkAddedInternal(source, sink,
                                                                           mcastRoute.group(), paths));
            });
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Clean up when deactivating the application.
     */
    public void terminate() {
        mcastEventCache.invalidateAll();
        executorService.shutdown();
        mcastNextObjStore.destroy();
        mcastRoleStore.destroy();
        mcastUtils.terminate();
        log.info("Terminated");
    }

    /**
     * Processes the SOURCE_ADDED, SOURCE_UPDATED, SINK_ADDED,
     * SINK_REMOVED and ROUTE_REMOVED events.
     *
     * @param event McastEvent with SOURCE_ADDED type
     */
    public void processMcastEvent(McastEvent event) {
        log.info("process {}", event);
        // If it is a route added, we do not enqueue
        if (event.type() == ROUTE_ADDED) {
            // We need just to elect a leader
            processRouteAddedInternal(event.subject().route().group());
        } else {
            // Just enqueue for now
            enqueueMcastEvent(event);
        }
    }

    /**
     * Process the ROUTE_ADDED event.
     *
     * @param mcastIp the group address
     */
    private void processRouteAddedInternal(IpAddress mcastIp) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            log.debug("Processing route added for group {}", mcastIp);
            // Just elect a new leader
            mcastUtils.isLeader(mcastIp);
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Removes the entire mcast tree related to this group.
     *
     * @param mcastIp multicast group IP address
     */
    private void processRouteRemovedInternal(ConnectPoint source, IpAddress mcastIp) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            log.debug("Processing route removed for group {}", mcastIp);
            // Verify leadership on the operation
            if (!mcastUtils.isLeader(mcastIp)) {
                log.debug("Skip {} due to lack of leadership", mcastIp);
                mcastUtils.withdrawLeader(mcastIp);
                return;
            }

            // Find out the ingress, transit and egress device of the affected group
            DeviceId ingressDevice = getDevice(mcastIp, INGRESS)
                    .stream().findAny().orElse(null);
            Set<DeviceId> transitDevices = getDevice(mcastIp, TRANSIT);
            Set<DeviceId> egressDevices = getDevice(mcastIp, EGRESS);

            // If there are no egress devices, sinks could be only on the ingress
            if (!egressDevices.isEmpty()) {
                egressDevices.forEach(
                        deviceId -> removeGroupFromDevice(deviceId, mcastIp, mcastUtils.assignedVlan(null))
                );
            }
            // Transit could be empty if sinks are on the ingress
            if (!transitDevices.isEmpty()) {
                transitDevices.forEach(
                        deviceId -> removeGroupFromDevice(deviceId, mcastIp, mcastUtils.assignedVlan(null))
                );
            }
            // Ingress device should be not null
            if (ingressDevice != null) {
                removeGroupFromDevice(ingressDevice, mcastIp, mcastUtils.assignedVlan(source));
            }
        } finally {
            mcastUnlock();
        }
    }


    /**
     * Process sinks to be removed.
     *
     * @param source the source connect point
     * @param mcastIp the ip address of the group
     * @param newSinks the new sinks to be processed
     * @param prevSinks the previous sinks
     */
    private void processSinksRemovedInternal(ConnectPoint source, IpAddress mcastIp,
                                             Map<HostId, Set<ConnectPoint>> newSinks,
                                             Map<HostId, Set<ConnectPoint>> prevSinks) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Verify leadership on the operation
            if (!mcastUtils.isLeader(mcastIp)) {
                log.debug("Skip {} due to lack of leadership", mcastIp);
                return;
            }
            // Remove the previous ones
            Set<ConnectPoint> sinksToBeRemoved = processSinksToBeRemoved(mcastIp, prevSinks,
                                                                         newSinks);
            sinksToBeRemoved.forEach(sink -> processSinkRemovedInternal(source, sink, mcastIp));
            // Recover the dual-homed sinks
            Set<ConnectPoint> sinksToBeRecovered = processSinksToBeRecovered(mcastIp, newSinks,
                                                                             prevSinks);
            sinksToBeRecovered.forEach(sink -> processSinkAddedInternal(source, sink, mcastIp, null));
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Removes a path from source to sink for given multicast group.
     *
     * @param source connect point of the multicast source
     * @param sink connection point of the multicast sink
     * @param mcastIp multicast group IP address
     */
    private void processSinkRemovedInternal(ConnectPoint source, ConnectPoint sink,
                                            IpAddress mcastIp) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            boolean isLast;
            // When source and sink are on the same device
            if (source.deviceId().equals(sink.deviceId())) {
                // Source and sink are on even the same port. There must be something wrong.
                if (source.port().equals(sink.port())) {
                    log.warn("Skip {} since sink {} is on the same port of source {}. Abort",
                             mcastIp, sink, source);
                    return;
                }
                isLast = removePortFromDevice(sink.deviceId(), sink.port(), mcastIp, mcastUtils.assignedVlan(source));
                if (isLast) {
                    mcastRoleStore.remove(new McastStoreKey(mcastIp, sink.deviceId()));
                }
                return;
            }

            // Process the egress device
            isLast = removePortFromDevice(sink.deviceId(), sink.port(), mcastIp, mcastUtils.assignedVlan(null));
            if (isLast) {
                mcastRoleStore.remove(new McastStoreKey(mcastIp, sink.deviceId()));
            }

            // If this is the last sink on the device, also update upstream
            Optional<Path> mcastPath = getPath(source.deviceId(), sink.deviceId(),
                                               mcastIp, null);
            if (mcastPath.isPresent()) {
                List<Link> links = Lists.newArrayList(mcastPath.get().links());
                Collections.reverse(links);
                for (Link link : links) {
                    if (isLast) {
                        isLast = removePortFromDevice(
                                link.src().deviceId(),
                                link.src().port(),
                                mcastIp,
                                mcastUtils.assignedVlan(link.src().deviceId().equals(source.deviceId()) ?
                                                     source : null)
                        );
                        if (isLast) {
                            mcastRoleStore.remove(new McastStoreKey(mcastIp, link.src().deviceId()));
                        }
                    }
                }
            }
        } finally {
            mcastUnlock();
        }
    }


    /**
     * Process sinks to be added.
     *
     * @param source the source connect point
     * @param mcastIp the group IP
     * @param newSinks the new sinks to be processed
     * @param allPrevSinks all previous sinks
     */
    private void processSinksAddedInternal(ConnectPoint source, IpAddress mcastIp,
                                           Map<HostId, Set<ConnectPoint>> newSinks,
                                           Set<ConnectPoint> allPrevSinks) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Verify leadership on the operation
            if (!mcastUtils.isLeader(mcastIp)) {
                log.debug("Skip {} due to lack of leadership", mcastIp);
                return;
            }
            // Get the only sinks to be processed (new ones)
            Set<ConnectPoint> sinksToBeAdded = processSinksToBeAdded(source, mcastIp, newSinks);
            // Install new sinks
            sinksToBeAdded = Sets.difference(sinksToBeAdded, allPrevSinks);
            sinksToBeAdded.forEach(sink -> processSinkAddedInternal(source, sink, mcastIp, null));
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Establishes a path from source to sink for given multicast group.
     *
     * @param source connect point of the multicast source
     * @param sink connection point of the multicast sink
     * @param mcastIp multicast group IP address
     */
    private void processSinkAddedInternal(ConnectPoint source, ConnectPoint sink,
                                          IpAddress mcastIp, List<Path> allPaths) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Process the ingress device
            mcastUtils.addFilterToDevice(source.deviceId(), source.port(),
                              mcastUtils.assignedVlan(source), mcastIp, INGRESS);

            // When source and sink are on the same device
            if (source.deviceId().equals(sink.deviceId())) {
                // Source and sink are on even the same port. There must be something wrong.
                if (source.port().equals(sink.port())) {
                    log.warn("Skip {} since sink {} is on the same port of source {}. Abort",
                             mcastIp, sink, source);
                    return;
                }
                addPortToDevice(sink.deviceId(), sink.port(), mcastIp, mcastUtils.assignedVlan(source));
                mcastRoleStore.put(new McastStoreKey(mcastIp, sink.deviceId()), INGRESS);
                return;
            }

            // Find a path. If present, create/update groups and flows for each hop
            Optional<Path> mcastPath = getPath(source.deviceId(), sink.deviceId(),
                                               mcastIp, allPaths);
            if (mcastPath.isPresent()) {
                List<Link> links = mcastPath.get().links();

                // Setup mcast role for ingress
                mcastRoleStore.put(new McastStoreKey(mcastIp, source.deviceId()),
                                   INGRESS);

                // Setup properly the transit
                links.forEach(link -> {
                    addPortToDevice(link.src().deviceId(), link.src().port(), mcastIp,
                                    mcastUtils.assignedVlan(link.src().deviceId()
                                                                    .equals(source.deviceId()) ? source : null));
                    mcastUtils.addFilterToDevice(link.dst().deviceId(), link.dst().port(),
                                      mcastUtils.assignedVlan(null), mcastIp, null);
                });

                // Setup mcast role for the transit
                links.stream()
                        .filter(link -> !link.dst().deviceId().equals(sink.deviceId()))
                        .forEach(link -> mcastRoleStore.put(new McastStoreKey(mcastIp, link.dst().deviceId()),
                                                            TRANSIT));

                // Process the egress device
                addPortToDevice(sink.deviceId(), sink.port(), mcastIp, mcastUtils.assignedVlan(null));
                // Setup mcast role for egress
                mcastRoleStore.put(new McastStoreKey(mcastIp, sink.deviceId()),
                                   EGRESS);
            } else {
                log.warn("Unable to find a path from {} to {}. Abort sinkAdded",
                         source.deviceId(), sink.deviceId());
            }
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Processes the LINK_DOWN event.
     *
     * @param affectedLink Link that is going down
     */
    public void processLinkDown(Link affectedLink) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Get groups affected by the link down event
            getAffectedGroups(affectedLink).forEach(mcastIp -> {
                // TODO Optimize when the group editing is in place
                log.debug("Processing link down {} for group {}",
                          affectedLink, mcastIp);
                // Verify leadership on the operation
                if (!mcastUtils.isLeader(mcastIp)) {
                    log.debug("Skip {} due to lack of leadership", mcastIp);
                    return;
                }

                // Find out the ingress, transit and egress device of affected group
                DeviceId ingressDevice = getDevice(mcastIp, INGRESS)
                        .stream().findAny().orElse(null);
                Set<DeviceId> transitDevices = getDevice(mcastIp, TRANSIT);
                Set<DeviceId> egressDevices = getDevice(mcastIp, EGRESS);
                ConnectPoint source = mcastUtils.getSource(mcastIp);

                // Do not proceed if ingress device or source of this group are missing
                // If sinks are in other leafs, we have ingress, transit, egress, and source
                // If sinks are in the same leaf, we have just ingress and source
                if (ingressDevice == null || source == null) {
                    log.warn("Missing ingress {} or source {} for group {}",
                             ingressDevice, source, mcastIp);
                    return;
                }

                // Remove entire transit
                transitDevices.forEach(transitDevice ->
                                removeGroupFromDevice(transitDevice, mcastIp,
                                                      mcastUtils.assignedVlan(null)));

                // Remove transit-facing ports on the ingress device
                removeIngressTransitPorts(mcastIp, ingressDevice, source);

                // TODO create a shared procedure with DEVICE_DOWN
                // Compute mcast tree for the the egress devices
                Map<DeviceId, List<Path>> mcastTree = computeMcastTree(ingressDevice, egressDevices);

                // We have to verify, if there are egresses without paths
                Set<DeviceId> notRecovered = Sets.newHashSet();
                mcastTree.forEach((egressDevice, paths) -> {
                    // Let's check if there is at least a path
                    Optional<Path> mcastPath = getPath(ingressDevice, egressDevice,
                                                       mcastIp, paths);
                    // No paths, we have to try with alternative location
                    if (!mcastPath.isPresent()) {
                        notRecovered.add(egressDevice);
                        // We were not able to find an alternative path for this egress
                        log.warn("Fail to recover egress device {} from link failure {}",
                                 egressDevice, affectedLink);
                        removeGroupFromDevice(egressDevice, mcastIp,
                                              mcastUtils.assignedVlan(null));
                    }
                });

                // Fast path, we can recover all the locations
                if (notRecovered.isEmpty()) {
                    // Construct a new path for each egress device
                    mcastTree.forEach((egressDevice, paths) -> {
                        // We try to enforce the sinks path on the mcast tree
                        Optional<Path> mcastPath = getPath(ingressDevice, egressDevice,
                                                           mcastIp, paths);
                        // If a path is present, let's install it
                        if (mcastPath.isPresent()) {
                            installPath(mcastIp, source, mcastPath.get());
                        }
                    });
                } else {
                    // Let's try to recover using alternate
                    recoverSinks(egressDevices, notRecovered, mcastIp,
                                 ingressDevice, source, true);
                }
            });
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Process the DEVICE_DOWN event.
     *
     * @param deviceDown device going down
     */
    public void processDeviceDown(DeviceId deviceDown) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Get the mcast groups affected by the device going down
            getAffectedGroups(deviceDown).forEach(mcastIp -> {
                // TODO Optimize when the group editing is in place
                log.debug("Processing device down {} for group {}",
                          deviceDown, mcastIp);
                // Verify leadership on the operation
                if (!mcastUtils.isLeader(mcastIp)) {
                    log.debug("Skip {} due to lack of leadership", mcastIp);
                    return;
                }

                // Find out the ingress, transit and egress device of affected group
                DeviceId ingressDevice = getDevice(mcastIp, INGRESS)
                        .stream().findAny().orElse(null);
                Set<DeviceId> transitDevices = getDevice(mcastIp, TRANSIT);
                Set<DeviceId> egressDevices = getDevice(mcastIp, EGRESS);
                ConnectPoint source = mcastUtils.getSource(mcastIp);

                // Do not proceed if ingress device or source of this group are missing
                // If sinks are in other leafs, we have ingress, transit, egress, and source
                // If sinks are in the same leaf, we have just ingress and source
                if (ingressDevice == null || source == null) {
                    log.warn("Missing ingress {} or source {} for group {}",
                             ingressDevice, source, mcastIp);
                    return;
                }

                // If it exists, we have to remove it in any case
                if (!transitDevices.isEmpty()) {
                    // Remove entire transit
                    transitDevices.forEach(transitDevice ->
                                    removeGroupFromDevice(transitDevice, mcastIp,
                                                          mcastUtils.assignedVlan(null)));
                }
                // If the ingress is down
                if (ingressDevice.equals(deviceDown)) {
                    // Remove entire ingress
                    removeGroupFromDevice(ingressDevice, mcastIp, mcastUtils.assignedVlan(source));
                    // If other sinks different from the ingress exist
                    if (!egressDevices.isEmpty()) {
                        // Remove all the remaining egress
                        egressDevices.forEach(
                                egressDevice -> removeGroupFromDevice(egressDevice, mcastIp,
                                                                      mcastUtils.assignedVlan(null))
                        );
                    }
                } else {
                    // Egress or transit could be down at this point
                    // Get the ingress-transit ports if they exist
                    removeIngressTransitPorts(mcastIp, ingressDevice, source);

                    // One of the egress device is down
                    if (egressDevices.contains(deviceDown)) {
                        // Remove entire device down
                        removeGroupFromDevice(deviceDown, mcastIp, mcastUtils.assignedVlan(null));
                        // Remove the device down from egress
                        egressDevices.remove(deviceDown);
                        // If there are no more egress and ingress does not have sinks
                        if (egressDevices.isEmpty() && !hasSinks(ingressDevice, mcastIp)) {
                            // We have done
                            return;
                        }
                    }

                    // Compute mcast tree for the the egress devices
                    Map<DeviceId, List<Path>> mcastTree = computeMcastTree(ingressDevice, egressDevices);

                    // We have to verify, if there are egresses without paths
                    Set<DeviceId> notRecovered = Sets.newHashSet();
                    mcastTree.forEach((egressDevice, paths) -> {
                        // Let's check if there is at least a path
                        Optional<Path> mcastPath = getPath(ingressDevice, egressDevice,
                                                           mcastIp, paths);
                        // No paths, we have to try with alternative location
                        if (!mcastPath.isPresent()) {
                            notRecovered.add(egressDevice);
                            // We were not able to find an alternative path for this egress
                            log.warn("Fail to recover egress device {} from device down {}",
                                     egressDevice, deviceDown);
                            removeGroupFromDevice(egressDevice, mcastIp, mcastUtils.assignedVlan(null));
                        }
                    });

                    // Fast path, we can recover all the locations
                    if (notRecovered.isEmpty()) {
                        // Construct a new path for each egress device
                        mcastTree.forEach((egressDevice, paths) -> {
                            // We try to enforce the sinks path on the mcast tree
                            Optional<Path> mcastPath = getPath(ingressDevice, egressDevice,
                                                               mcastIp, paths);
                            // If a path is present, let's install it
                            if (mcastPath.isPresent()) {
                                installPath(mcastIp, source, mcastPath.get());
                            }
                        });
                    } else {
                        // Let's try to recover using alternate
                        recoverSinks(egressDevices, notRecovered, mcastIp,
                                     ingressDevice, source, false);
                    }
                }
            });
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Try to recover sinks using alternate locations.
     *
     * @param egressDevices the original egress devices
     * @param notRecovered the devices not recovered
     * @param mcastIp the group address
     * @param ingressDevice the ingress device
     * @param source the source connect point
     * @param isLinkFailure true if it is a link failure, otherwise false
     */
    private void recoverSinks(Set<DeviceId> egressDevices, Set<DeviceId> notRecovered,
                              IpAddress mcastIp, DeviceId ingressDevice, ConnectPoint source,
                              boolean isLinkFailure) {
        // Recovered devices
        Set<DeviceId> recovered = Sets.difference(egressDevices, notRecovered);
        // Total affected sinks
        Set<ConnectPoint> totalAffectedSinks = Sets.newHashSet();
        // Total sinks
        Set<ConnectPoint> totalSinks = Sets.newHashSet();
        // Let's compute all the affected sinks and all the sinks
        notRecovered.forEach(deviceId -> {
            totalAffectedSinks.addAll(
                    mcastUtils.getAffectedSinks(deviceId, mcastIp)
                            .values()
                            .stream()
                            .flatMap(Collection::stream)
                            .filter(connectPoint -> connectPoint.deviceId().equals(deviceId))
                            .collect(Collectors.toSet())
            );
            totalSinks.addAll(
                    mcastUtils.getAffectedSinks(deviceId, mcastIp)
                            .values()
                            .stream()
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet())
            );
        });

        // Sinks to be added
        Set<ConnectPoint> sinksToBeAdded = Sets.difference(totalSinks, totalAffectedSinks);
        // New egress devices, filtering out the source
        Set<DeviceId> newEgressDevice = sinksToBeAdded.stream()
                .map(ConnectPoint::deviceId)
                .collect(Collectors.toSet());
        // Let's add the devices recovered from the previous round
        newEgressDevice.addAll(recovered);
        // Let's do a copy of the new egresses and filter out the source
        Set<DeviceId> copyNewEgressDevice = ImmutableSet.copyOf(newEgressDevice);
        newEgressDevice = newEgressDevice.stream()
                .filter(deviceId -> !deviceId.equals(ingressDevice))
                .collect(Collectors.toSet());

        // Re-compute mcast tree for the the egress devices
        Map<DeviceId, List<Path>> mcastTree = computeMcastTree(ingressDevice, newEgressDevice);
        // if the source was originally in the new locations, add new sinks
        if (copyNewEgressDevice.contains(ingressDevice)) {
            sinksToBeAdded.stream()
                    .filter(connectPoint -> connectPoint.deviceId().equals(ingressDevice))
                    .forEach(sink -> processSinkAddedInternal(source, sink, mcastIp, ImmutableList.of()));
        }

        // Construct a new path for each egress device
        mcastTree.forEach((egressDevice, paths) -> {
            // We try to enforce the sinks path on the mcast tree
            Optional<Path> mcastPath = getPath(ingressDevice, egressDevice,
                                               mcastIp, paths);
            // If a path is present, let's install it
            if (mcastPath.isPresent()) {
                // Using recovery procedure
                if (recovered.contains(egressDevice)) {
                    installPath(mcastIp, source, mcastPath.get());
                } else {
                    // otherwise we need to threat as new sink
                    sinksToBeAdded.stream()
                            .filter(connectPoint -> connectPoint.deviceId().equals(egressDevice))
                            .forEach(sink -> processSinkAddedInternal(source, sink, mcastIp, paths));
                }
            } else {
                // We were not able to find an alternative path for this egress
                log.warn("Fail to recover egress device {} from {} failure",
                         egressDevice, isLinkFailure ? "Link" : "Device");
                removeGroupFromDevice(egressDevice, mcastIp, mcastUtils.assignedVlan(null));
            }
        });

    }

    /**
     * Process all the sinks related to a mcast group and return
     * the ones to be removed.
     *
     * @param mcastIp the group address
     * @param prevsinks the previous sinks to be evaluated
     * @param newSinks the new sinks to be evaluted
     * @return the set of the sinks to be removed
     */
    private Set<ConnectPoint> processSinksToBeRemoved(IpAddress mcastIp,
                                                      Map<HostId, Set<ConnectPoint>> prevsinks,
                                                      Map<HostId, Set<ConnectPoint>> newSinks) {
        // Iterate over the sinks in order to build the set
        // of the connect points to be removed from this group
        final Set<ConnectPoint> sinksToBeProcessed = Sets.newHashSet();
        prevsinks.forEach(((hostId, connectPoints) -> {
            // We have to check with the existing flows
            ConnectPoint sinkToBeProcessed = connectPoints.stream()
                    .filter(connectPoint -> isSink(mcastIp, connectPoint))
                    .findFirst().orElse(null);
            if (sinkToBeProcessed != null) {
                // If the host has been removed or location has been removed
                if (!newSinks.containsKey(hostId) ||
                        !newSinks.get(hostId).contains(sinkToBeProcessed)) {
                    sinksToBeProcessed.add(sinkToBeProcessed);
                }
            }
        }));
        // We have done, return the set
        return sinksToBeProcessed;
    }

    /**
     * Process new locations and return the set of sinks to be added
     * in the context of the recovery.
     *
     * @param newSinks the remaining sinks
     * @param prevSinks the previous sinks
     * @return the set of the sinks to be processed
     */
    private Set<ConnectPoint> processSinksToBeRecovered(IpAddress mcastIp,
                                                        Map<HostId, Set<ConnectPoint>> newSinks,
                                                        Map<HostId, Set<ConnectPoint>> prevSinks) {
        // Iterate over the sinks in order to build the set
        // of the connect points to be served by this group
        final Set<ConnectPoint> sinksToBeProcessed = Sets.newHashSet();
        newSinks.forEach((hostId, connectPoints) -> {
            // If it has more than 1 locations
            if (connectPoints.size() > 1 || connectPoints.size() == 0) {
                log.debug("Skip {} since sink {} has {} locations",
                         mcastIp, hostId, connectPoints.size());
                return;
            }
            // If previously it had two locations, we need to recover it
            // Filter out if the remaining location is already served
            if (prevSinks.containsKey(hostId) && prevSinks.get(hostId).size() == 2) {
                ConnectPoint sinkToBeProcessed = connectPoints.stream()
                        .filter(connectPoint -> !isSink(mcastIp, connectPoint))
                        .findFirst().orElse(null);
                if (sinkToBeProcessed != null) {
                    sinksToBeProcessed.add(sinkToBeProcessed);
                }
            }
        });
        return sinksToBeProcessed;
    }

    /**
     * Process all the sinks related to a mcast group and return
     * the ones to be processed.
     *
     * @param source the source connect point
     * @param mcastIp the group address
     * @param sinks the sinks to be evaluated
     * @return the set of the sinks to be processed
     */
    private Set<ConnectPoint> processSinksToBeAdded(ConnectPoint source, IpAddress mcastIp,
                                                    Map<HostId, Set<ConnectPoint>> sinks) {
        // Iterate over the sinks in order to build the set
        // of the connect points to be served by this group
        final Set<ConnectPoint> sinksToBeProcessed = Sets.newHashSet();
        sinks.forEach(((hostId, connectPoints) -> {
            // If it has more than 2 locations
            if (connectPoints.size() > 2 || connectPoints.size() == 0) {
                log.debug("Skip {} since sink {} has {} locations",
                         mcastIp, hostId, connectPoints.size());
                return;
            }
            // If it has one location, just use it
            if (connectPoints.size() == 1) {
                sinksToBeProcessed.add(connectPoints.stream()
                                               .findFirst().orElse(null));
                return;
            }
            // We prefer to reuse existing flows
            ConnectPoint sinkToBeProcessed = connectPoints.stream()
                    .filter(connectPoint -> isSink(mcastIp, connectPoint))
                    .findFirst().orElse(null);
            if (sinkToBeProcessed != null) {
                sinksToBeProcessed.add(sinkToBeProcessed);
                return;
            }
            // Otherwise we prefer to reuse existing egresses
            Set<DeviceId> egresses = getDevice(mcastIp, EGRESS);
            sinkToBeProcessed = connectPoints.stream()
                    .filter(connectPoint -> egresses.contains(connectPoint.deviceId()))
                    .findFirst().orElse(null);
            if (sinkToBeProcessed != null) {
                sinksToBeProcessed.add(sinkToBeProcessed);
                return;
            }
            // Otherwise we prefer a location co-located with the source (if it exists)
            sinkToBeProcessed = connectPoints.stream()
                    .filter(connectPoint -> connectPoint.deviceId().equals(source.deviceId()))
                    .findFirst().orElse(null);
            if (sinkToBeProcessed != null) {
                sinksToBeProcessed.add(sinkToBeProcessed);
                return;
            }
            // Finally, we randomly pick a new location
            sinksToBeProcessed.add(connectPoints.stream()
                                           .findFirst().orElse(null));
        }));
        // We have done, return the set
        return sinksToBeProcessed;
    }

    /**
     * Utility method to remove all the ingress transit ports.
     *
     * @param mcastIp the group ip
     * @param ingressDevice the ingress device for this group
     * @param source the source connect point
     */
    private void removeIngressTransitPorts(IpAddress mcastIp, DeviceId ingressDevice,
                                           ConnectPoint source) {
        Set<PortNumber> ingressTransitPorts = ingressTransitPort(mcastIp);
        ingressTransitPorts.forEach(ingressTransitPort -> {
            if (ingressTransitPort != null) {
                boolean isLast = removePortFromDevice(ingressDevice, ingressTransitPort,
                                                      mcastIp, mcastUtils.assignedVlan(source));
                if (isLast) {
                    mcastRoleStore.remove(new McastStoreKey(mcastIp, ingressDevice));
                }
            }
        });
    }

    /**
     * Adds a port to given multicast group on given device. This involves the
     * update of L3 multicast group and multicast routing table entry.
     *
     * @param deviceId device ID
     * @param port port to be added
     * @param mcastIp multicast group
     * @param assignedVlan assigned VLAN ID
     */
    private void addPortToDevice(DeviceId deviceId, PortNumber port,
                                 IpAddress mcastIp, VlanId assignedVlan) {
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, deviceId);
        ImmutableSet.Builder<PortNumber> portBuilder = ImmutableSet.builder();
        NextObjective newNextObj;
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            // First time someone request this mcast group via this device
            portBuilder.add(port);
            // New nextObj
            newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                        portBuilder.build(), null).add();
            // Store the new port
            mcastNextObjStore.put(mcastStoreKey, newNextObj);
        } else {
            // This device already serves some subscribers of this mcast group
            NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();
            // Stop if the port is already in the nextobj
            Set<PortNumber> existingPorts = mcastUtils.getPorts(nextObj.next());
            if (existingPorts.contains(port)) {
                log.info("NextObj for {}/{} already exists. Abort", deviceId, port);
                return;
            }
            // Let's add the port and reuse the previous one
            portBuilder.addAll(existingPorts).add(port);
            // Reuse previous nextObj
            newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                        portBuilder.build(), nextObj.id()).addToExisting();
            // Store the final next objective and send only the difference to the driver
            mcastNextObjStore.put(mcastStoreKey, newNextObj);
            // Add just the new port
            portBuilder = ImmutableSet.builder();
            portBuilder.add(port);
            newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                        portBuilder.build(), nextObj.id()).addToExisting();
        }
        // Create, store and apply the new nextObj and fwdObj
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully add {} on {}/{}, vlan {}",
                        mcastIp, deviceId, port.toLong(), assignedVlan),
                (objective, error) ->
                        log.warn("Failed to add {} on {}/{}, vlan {}: {}",
                                mcastIp, deviceId, port.toLong(), assignedVlan, error));
        ForwardingObjective fwdObj = mcastUtils.fwdObjBuilder(mcastIp, assignedVlan,
                                                              newNextObj.id()).add(context);
        srManager.flowObjectiveService.next(deviceId, newNextObj);
        srManager.flowObjectiveService.forward(deviceId, fwdObj);
    }

    /**
     * Removes a port from given multicast group on given device.
     * This involves the update of L3 multicast group and multicast routing
     * table entry.
     *
     * @param deviceId device ID
     * @param port port to be added
     * @param mcastIp multicast group
     * @param assignedVlan assigned VLAN ID
     * @return true if this is the last sink on this device
     */
    private boolean removePortFromDevice(DeviceId deviceId, PortNumber port,
                                         IpAddress mcastIp, VlanId assignedVlan) {
        McastStoreKey mcastStoreKey =
                new McastStoreKey(mcastIp, deviceId);
        // This device is not serving this multicast group
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            log.warn("{} is not serving {} on port {}. Abort.", deviceId, mcastIp, port);
            return false;
        }
        NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();

        Set<PortNumber> existingPorts = mcastUtils.getPorts(nextObj.next());
        // This port does not serve this multicast group
        if (!existingPorts.contains(port)) {
            log.warn("{} is not serving {} on port {}. Abort.", deviceId, mcastIp, port);
            return false;
        }
        // Copy and modify the ImmutableSet
        existingPorts = Sets.newHashSet(existingPorts);
        existingPorts.remove(port);

        NextObjective newNextObj;
        ObjectiveContext context;
        ForwardingObjective fwdObj;
        if (existingPorts.isEmpty()) {
            // If this is the last sink, remove flows and last bucket
            // NOTE: Rely on GroupStore garbage collection rather than explicitly
            //       remove L3MG since there might be other flows/groups refer to
            //       the same L2IG
            context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Successfully remove {} on {}/{}, vlan {}",
                            mcastIp, deviceId, port.toLong(), assignedVlan),
                    (objective, error) ->
                            log.warn("Failed to remove {} on {}/{}, vlan {}: {}",
                                    mcastIp, deviceId, port.toLong(), assignedVlan, error));
            fwdObj = mcastUtils.fwdObjBuilder(mcastIp, assignedVlan, nextObj.id()).remove(context);
            mcastNextObjStore.remove(mcastStoreKey);
        } else {
            // If this is not the last sink, update flows and groups
            context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Successfully update {} on {}/{}, vlan {}",
                            mcastIp, deviceId, port.toLong(), assignedVlan),
                    (objective, error) ->
                            log.warn("Failed to update {} on {}/{}, vlan {}: {}",
                                    mcastIp, deviceId, port.toLong(), assignedVlan, error));
            // Here we store the next objective with the remaining port
            newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                        existingPorts, nextObj.id()).removeFromExisting();
            fwdObj = mcastUtils.fwdObjBuilder(mcastIp, assignedVlan, newNextObj.id()).add(context);
            mcastNextObjStore.put(mcastStoreKey, newNextObj);
        }
        // Let's modify the next objective removing the bucket
        newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                    ImmutableSet.of(port), nextObj.id()).removeFromExisting();
        srManager.flowObjectiveService.next(deviceId, newNextObj);
        srManager.flowObjectiveService.forward(deviceId, fwdObj);
        return existingPorts.isEmpty();
    }

    /**
     * Removes entire group on given device.
     *
     * @param deviceId device ID
     * @param mcastIp multicast group to be removed
     * @param assignedVlan assigned VLAN ID
     */
    private void removeGroupFromDevice(DeviceId deviceId, IpAddress mcastIp,
                                       VlanId assignedVlan) {
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, deviceId);
        // This device is not serving this multicast group
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            log.warn("{} is not serving {}. Abort.", deviceId, mcastIp);
            return;
        }
        NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();
        // NOTE: Rely on GroupStore garbage collection rather than explicitly
        //       remove L3MG since there might be other flows/groups refer to
        //       the same L2IG
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully remove {} on {}, vlan {}",
                        mcastIp, deviceId, assignedVlan),
                (objective, error) ->
                        log.warn("Failed to remove {} on {}, vlan {}: {}",
                                mcastIp, deviceId, assignedVlan, error));
        ForwardingObjective fwdObj = mcastUtils.fwdObjBuilder(mcastIp, assignedVlan, nextObj.id()).remove(context);
        srManager.flowObjectiveService.forward(deviceId, fwdObj);
        mcastNextObjStore.remove(mcastStoreKey);
        mcastRoleStore.remove(mcastStoreKey);
    }

    private void installPath(IpAddress mcastIp, ConnectPoint source, Path mcastPath) {
        // Get Links
        List<Link> links = mcastPath.links();

        // Setup new ingress mcast role
        mcastRoleStore.put(new McastStoreKey(mcastIp, links.get(0).src().deviceId()),
                           INGRESS);

        // For each link, modify the next on the source device adding the src port
        // and a new filter objective on the destination port
        links.forEach(link -> {
            addPortToDevice(link.src().deviceId(), link.src().port(), mcastIp,
                            mcastUtils.assignedVlan(link.src().deviceId().equals(source.deviceId()) ? source : null));
            mcastUtils.addFilterToDevice(link.dst().deviceId(), link.dst().port(),
                              mcastUtils.assignedVlan(null), mcastIp, null);
        });

        // Setup mcast role for the transit
        links.stream()
                .filter(link -> !link.src().deviceId().equals(source.deviceId()))
                .forEach(link -> mcastRoleStore.put(new McastStoreKey(mcastIp, link.src().deviceId()),
                                                    TRANSIT));
    }

    /**
     * Go through all the paths, looking for shared links to be used
     * in the final path computation.
     *
     * @param egresses egress devices
     * @param availablePaths all the available paths towards the egress
     * @return shared links between egress devices
     */
    private Set<Link> exploreMcastTree(Set<DeviceId> egresses,
                                       Map<DeviceId, List<Path>> availablePaths) {
        // Length of the shortest path
        int minLength = Integer.MAX_VALUE;
        int length;
        // Current paths
        List<Path> currentPaths;
        // Verify the source can still reach all the egresses
        for (DeviceId egress : egresses) {
            // From the source we cannot reach all the sinks
            // just continue and let's figure out after
            currentPaths = availablePaths.get(egress);
            if (currentPaths.isEmpty()) {
                continue;
            }
            // Get the length of the first one available,
            // update the min length
            length = currentPaths.get(0).links().size();
            if (length < minLength) {
                minLength = length;
            }
        }
        // If there are no paths
        if (minLength == Integer.MAX_VALUE) {
            return Collections.emptySet();
        }
        // Iterate looking for shared links
        int index = 0;
        // Define the sets for the intersection
        Set<Link> sharedLinks = Sets.newHashSet();
        Set<Link> currentSharedLinks;
        Set<Link> currentLinks;
        DeviceId egressToRemove = null;
        // Let's find out the shared links
        while (index < minLength) {
            // Initialize the intersection with the paths related to the first egress
            currentPaths = availablePaths.get(
                    egresses.stream()
                            .findFirst()
                            .orElse(null)
            );
            currentSharedLinks = Sets.newHashSet();
            // Iterate over the paths and take the "index" links
            for (Path path : currentPaths) {
                currentSharedLinks.add(path.links().get(index));
            }
            // Iterate over the remaining egress
            for (DeviceId egress : egresses) {
                // Iterate over the paths and take the "index" links
                currentLinks = Sets.newHashSet();
                for (Path path : availablePaths.get(egress)) {
                    currentLinks.add(path.links().get(index));
                }
                // Do intersection
                currentSharedLinks = Sets.intersection(currentSharedLinks, currentLinks);
                // If there are no shared paths exit and record the device to remove
                // we have to retry with a subset of sinks
                if (currentSharedLinks.isEmpty()) {
                    egressToRemove = egress;
                    index = minLength;
                    break;
                }
            }
            sharedLinks.addAll(currentSharedLinks);
            index++;
        }
        // If the shared links is empty and there are egress
        // let's retry another time with less sinks, we can
        // still build optimal subtrees
        if (sharedLinks.isEmpty() && egresses.size() > 1 && egressToRemove != null) {
            egresses.remove(egressToRemove);
            sharedLinks = exploreMcastTree(egresses, availablePaths);
        }
        return sharedLinks;
    }

    /**
     * Build Mcast tree having as root the given source and as leaves the given egress points.
     *
     * @param source source of the tree
     * @param sinks leaves of the tree
     * @return the computed Mcast tree
     */
    private Map<ConnectPoint, List<Path>> computeSinkMcastTree(DeviceId source,
                                                               Set<ConnectPoint> sinks) {
        // Get the egress devices, remove source from the egress if present
        Set<DeviceId> egresses = sinks.stream()
                .map(ConnectPoint::deviceId)
                .filter(deviceId -> !deviceId.equals(source))
                .collect(Collectors.toSet());
        Map<DeviceId, List<Path>> mcastTree = computeMcastTree(source, egresses);
        // Build final tree and return it as it is
        final Map<ConnectPoint, List<Path>> finalTree = Maps.newHashMap();
        // We need to put back the source if it was originally present
        sinks.forEach(sink -> {
            List<Path> sinkPaths = mcastTree.get(sink.deviceId());
            finalTree.put(sink, sinkPaths != null ? sinkPaths : ImmutableList.of());
        });
        return finalTree;
    }

    /**
     * Build Mcast tree having as root the given source and as leaves the given egress.
     *
     * @param source source of the tree
     * @param egresses leaves of the tree
     * @return the computed Mcast tree
     */
    private Map<DeviceId, List<Path>> computeMcastTree(DeviceId source,
                                                       Set<DeviceId> egresses) {
        // Pre-compute all the paths
        Map<DeviceId, List<Path>> availablePaths = Maps.newHashMap();
        // No links to enforce
        egresses.forEach(egress -> availablePaths.put(egress, getPaths(source, egress,
                                                                       Collections.emptySet())));
        // Explore the topology looking for shared links amongst the egresses
        Set<Link> linksToEnforce = exploreMcastTree(Sets.newHashSet(egresses), availablePaths);
        // Remove all the paths from the previous computation
        availablePaths.clear();
        // Build the final paths enforcing the shared links between egress devices
        egresses.forEach(egress -> availablePaths.put(egress, getPaths(source, egress,
                                                                       linksToEnforce)));
        return availablePaths;
    }

    /**
     * Gets path from src to dst computed using the custom link weigher.
     *
     * @param src source device ID
     * @param dst destination device ID
     * @return list of paths from src to dst
     */
    private List<Path> getPaths(DeviceId src, DeviceId dst, Set<Link> linksToEnforce) {
        // Takes a snapshot of the topology
        final Topology currentTopology = topologyService.currentTopology();
        // Build a specific link weigher for this path computation
        final LinkWeigher linkWeigher = new SRLinkWeigher(srManager, src, linksToEnforce);
        // We will use our custom link weigher for our path
        // computations and build the list of valid paths
        List<Path> allPaths = Lists.newArrayList(
                topologyService.getPaths(currentTopology, src, dst, linkWeigher)
        );
        // If there are no valid paths, just exit
        log.debug("{} path(s) found from {} to {}", allPaths.size(), src, dst);
        return allPaths;
    }

    /**
     * Gets a path from src to dst.
     * If a path was allocated before, returns the allocated path.
     * Otherwise, randomly pick one from available paths.
     *
     * @param src source device ID
     * @param dst destination device ID
     * @param mcastIp multicast group
     * @param allPaths paths list
     * @return an optional path from src to dst
     */
    private Optional<Path> getPath(DeviceId src, DeviceId dst,
                                   IpAddress mcastIp, List<Path> allPaths) {
        // Firstly we get all the valid paths, if the supplied are null
        if (allPaths == null) {
            allPaths = getPaths(src, dst, Collections.emptySet());
        }

        // If there are no paths just exit
        if (allPaths.isEmpty()) {
            return Optional.empty();
        }

        // Create a map index of suitablity-to-list of paths. For example
        // a path in the list associated to the index 1 shares only the
        // first hop and it is less suitable of a path belonging to the index
        // 2 that shares leaf-spine.
        Map<Integer, List<Path>> eligiblePaths = Maps.newHashMap();
        // Some init steps
        int nhop;
        McastStoreKey mcastStoreKey;
        Link hop;
        PortNumber srcPort;
        Set<PortNumber> existingPorts;
        NextObjective nextObj;
        // Iterate over paths looking for eligible paths
        for (Path path : allPaths) {
            // Unlikely, it will happen...
            if (!src.equals(path.links().get(0).src().deviceId())) {
                continue;
            }
            nhop = 0;
            // Iterate over the links
            while (nhop < path.links().size()) {
                // Get the link and verify if a next related
                // to the src device exist in the store
                hop = path.links().get(nhop);
                mcastStoreKey = new McastStoreKey(mcastIp, hop.src().deviceId());
                // It does not exist in the store, exit
                if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
                    break;
                }
                // Get the output ports on the next
                nextObj = mcastNextObjStore.get(mcastStoreKey).value();
                existingPorts = mcastUtils.getPorts(nextObj.next());
                // And the src port on the link
                srcPort = hop.src().port();
                // the src port is not used as output, exit
                if (!existingPorts.contains(srcPort)) {
                    break;
                }
                nhop++;
            }
            // n_hop defines the index
            if (nhop > 0) {
                eligiblePaths.compute(nhop, (index, paths) -> {
                    paths = paths == null ? Lists.newArrayList() : paths;
                    paths.add(path);
                    return paths;
                });
            }
        }

        // No suitable paths
        if (eligiblePaths.isEmpty()) {
            log.debug("No eligiblePath(s) found from {} to {}", src, dst);
            // Otherwise, randomly pick a path
            Collections.shuffle(allPaths);
            return allPaths.stream().findFirst();
        }

        // Let's take the best ones
        Integer bestIndex = eligiblePaths.keySet()
                .stream()
                .sorted(Comparator.reverseOrder())
                .findFirst().orElse(null);
        List<Path> bestPaths = eligiblePaths.get(bestIndex);
        log.debug("{} eligiblePath(s) found from {} to {}",
                  bestPaths.size(), src, dst);
        // randomly pick a path on the highest index
        Collections.shuffle(bestPaths);
        return bestPaths.stream().findFirst();
    }

    /**
     * Gets device(s) of given role in given multicast group.
     *
     * @param mcastIp multicast IP
     * @param role multicast role
     * @return set of device ID or empty set if not found
     */
    private Set<DeviceId> getDevice(IpAddress mcastIp, McastRole role) {
        return mcastRoleStore.entrySet().stream()
                .filter(entry -> entry.getKey().mcastIp().equals(mcastIp) &&
                        entry.getValue().value() == role)
                .map(Entry::getKey).map(McastStoreKey::deviceId)
                .collect(Collectors.toSet());
    }

    /**
     * Gets groups which is affected by the link down event.
     *
     * @param link link going down
     * @return a set of multicast IpAddress
     */
    private Set<IpAddress> getAffectedGroups(Link link) {
        DeviceId deviceId = link.src().deviceId();
        PortNumber port = link.src().port();
        return mcastNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId) &&
                        mcastUtils.getPorts(entry.getValue().value().next()).contains(port))
                .map(Entry::getKey).map(McastStoreKey::mcastIp)
                .collect(Collectors.toSet());
    }

    /**
     * Gets groups which are affected by the device down event.
     *
     * @param deviceId device going down
     * @return a set of multicast IpAddress
     */
    private Set<IpAddress> getAffectedGroups(DeviceId deviceId) {
        return mcastNextObjStore.entrySet().stream()
                .filter(entry -> entry.getKey().deviceId().equals(deviceId))
                .map(Entry::getKey).map(McastStoreKey::mcastIp)
                .collect(Collectors.toSet());
    }

    /**
     * Gets the spine-facing port on ingress device of given multicast group.
     *
     * @param mcastIp multicast IP
     * @return spine-facing port on ingress device
     */
    private Set<PortNumber> ingressTransitPort(IpAddress mcastIp) {
        DeviceId ingressDevice = getDevice(mcastIp, INGRESS)
                .stream().findAny().orElse(null);
        ImmutableSet.Builder<PortNumber> portBuilder = ImmutableSet.builder();
        if (ingressDevice != null) {
            NextObjective nextObj = mcastNextObjStore
                    .get(new McastStoreKey(mcastIp, ingressDevice)).value();
            Set<PortNumber> ports = mcastUtils.getPorts(nextObj.next());
            // Let's find out all the ingress-transit ports
            for (PortNumber port : ports) {
                // Spine-facing port should have no subnet and no xconnect
                if (srManager.deviceConfiguration() != null &&
                        srManager.deviceConfiguration().getPortSubnets(ingressDevice, port).isEmpty() &&
                        !srManager.xConnectHandler.hasXConnect(new ConnectPoint(ingressDevice, port))) {
                    portBuilder.add(port);
                }
            }
        }
        return portBuilder.build();
    }

    /**
     * Verify if the given device has sinks
     * for the multicast group.
     *
     * @param deviceId device Id
     * @param mcastIp multicast IP
     * @return true if the device has sink for the group.
     * False otherwise.
     */
    private boolean hasSinks(DeviceId deviceId, IpAddress mcastIp) {
        if (deviceId != null) {
            // Get the nextobjective
            Versioned<NextObjective> versionedNextObj = mcastNextObjStore.get(
                    new McastStoreKey(mcastIp, deviceId)
            );
            // If it exists
            if (versionedNextObj != null) {
                NextObjective nextObj = versionedNextObj.value();
                // Retrieves all the output ports
                Set<PortNumber> ports = mcastUtils.getPorts(nextObj.next());
                // Tries to find at least one port that is not spine-facing
                for (PortNumber port : ports) {
                    // Spine-facing port should have no subnet and no xconnect
                    if (srManager.deviceConfiguration() != null &&
                            (!srManager.deviceConfiguration().getPortSubnets(deviceId, port).isEmpty() ||
                            srManager.xConnectHandler.hasXConnect(new ConnectPoint(deviceId, port)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Verify if a given connect point is sink for this group.
     *
     * @param mcastIp group address
     * @param connectPoint connect point to be verified
     * @return true if the connect point is sink of the group
     */
    private boolean isSink(IpAddress mcastIp, ConnectPoint connectPoint) {
        // Let's check if we are already serving that location
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, connectPoint.deviceId());
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            return false;
        }
        // Get next and check with the port
        NextObjective mcastNext = mcastNextObjStore.get(mcastStoreKey).value();
        return mcastUtils.getPorts(mcastNext.next()).contains(connectPoint.port());
    }

    /**
     * Updates filtering objective for given device and port.
     * It is called in general when the mcast config has been
     * changed.
     *
     * @param deviceId device ID
     * @param portNum ingress port number
     * @param vlanId assigned VLAN ID
     * @param install true to add, false to remove
     */
    public void updateFilterToDevice(DeviceId deviceId, PortNumber portNum,
                                        VlanId vlanId, boolean install) {
        lastMcastChange = Instant.now();
        mcastLock();
        try {
            // Iterates over the route and updates properly the filtering objective
            // on the source device.
            srManager.multicastRouteService.getRoutes().forEach(mcastRoute -> {
                log.debug("Update filter for {}", mcastRoute.group());
                // Verify leadership on the operation
                if (!mcastUtils.isLeader(mcastRoute.group())) {
                    log.debug("Skip {} due to lack of leadership", mcastRoute.group());
                    return;
                }
                // FIXME To be addressed with multiple sources support
                ConnectPoint source = srManager.multicastRouteService.sources(mcastRoute)
                        .stream()
                        .findFirst().orElse(null);
                if (source.deviceId().equals(deviceId) && source.port().equals(portNum)) {
                    if (install) {
                        mcastUtils.addFilterToDevice(deviceId, portNum, vlanId, mcastRoute.group(), INGRESS);
                    } else {
                        mcastUtils.removeFilterToDevice(deviceId, portNum, vlanId, mcastRoute.group(), null);
                    }
                }
            });
        } finally {
            mcastUnlock();
        }
    }

    /**
     * Performs bucket verification operation for all mcast groups in the devices.
     * Firstly, it verifies that mcast is stable before trying verification operation.
     * Verification consists in creating new nexts with VERIFY operation. Actually,
     * the operation is totally delegated to the driver.
     */
     private final class McastBucketCorrector implements Runnable {

        @Override
        public void run() {
            // Verify if the Mcast has been stable for MCAST_STABLITY_THRESHOLD
            if (!isMcastStable()) {
                return;
            }
            // Acquires lock
            mcastLock();
            try {
                // Iterates over the routes and verify the related next objectives
                srManager.multicastRouteService.getRoutes()
                    .stream()
                    .map(McastRoute::group)
                    .forEach(mcastIp -> {
                        log.trace("Running mcast buckets corrector for mcast group: {}",
                                  mcastIp);

                        // For each group we get current information in the store
                        // and issue a check of the next objectives in place
                        DeviceId ingressDevice = getDevice(mcastIp, INGRESS)
                                .stream().findAny().orElse(null);
                        Set<DeviceId> transitDevices = getDevice(mcastIp, TRANSIT);
                        Set<DeviceId> egressDevices = getDevice(mcastIp, EGRESS);
                        // Get source and sinks from Mcast Route Service and warn about errors
                        ConnectPoint source = mcastUtils.getSource(mcastIp);
                        Set<ConnectPoint> sinks = mcastUtils.getSinks(mcastIp).values().stream()
                                .flatMap(Collection::stream)
                                .collect(Collectors.toSet());

                        // Do not proceed if ingress device or source of this group are missing
                        if (ingressDevice == null || source == null) {
                            if (!sinks.isEmpty()) {
                                log.warn("Unable to run buckets corrector. " +
                                                 "Missing ingress {} or source {} for group {}",
                                         ingressDevice, source, mcastIp);
                            }
                            return;
                        }

                        // Continue only when this instance is the leader of the group
                        if (!mcastUtils.isLeader(mcastIp)) {
                            log.trace("Unable to run buckets corrector. " +
                                             "Skip {} due to lack of leadership", mcastIp);
                            return;
                        }

                        // Create the set of the devices to be processed
                        ImmutableSet.Builder<DeviceId> devicesBuilder = ImmutableSet.builder();
                        devicesBuilder.add(ingressDevice);
                        if (!transitDevices.isEmpty()) {
                            devicesBuilder.addAll(transitDevices);
                        }
                        if (!egressDevices.isEmpty()) {
                            devicesBuilder.addAll(egressDevices);
                        }
                        Set<DeviceId> devicesToProcess = devicesBuilder.build();

                        // Iterate over the devices
                        devicesToProcess.forEach(deviceId -> {
                            McastStoreKey currentKey = new McastStoreKey(mcastIp, deviceId);
                            // If next exists in our store verify related next objective
                            if (mcastNextObjStore.containsKey(currentKey)) {
                                NextObjective currentNext = mcastNextObjStore.get(currentKey).value();
                                // Get current ports
                                Set<PortNumber> currentPorts = mcastUtils.getPorts(currentNext.next());
                                // Rebuild the next objective
                                currentNext = mcastUtils.nextObjBuilder(
                                        mcastIp,
                                        mcastUtils.assignedVlan(deviceId.equals(source.deviceId()) ?
                                                                        source : null),
                                        currentPorts,
                                        currentNext.id()
                                ).verify();
                                // Send to the flowobjective service
                                srManager.flowObjectiveService.next(deviceId, currentNext);
                            } else {
                                log.warn("Unable to run buckets corrector. " +
                                                 "Missing next for {} and group {}",
                                         deviceId, mcastIp);
                            }
                        });

                    });
            } finally {
                // Finally, it releases the lock
                mcastUnlock();
            }

        }
    }

    public Map<McastStoreKey, Integer> getMcastNextIds(IpAddress mcastIp) {
        // If mcast ip is present
        if (mcastIp != null) {
            return mcastNextObjStore.entrySet().stream()
                    .filter(mcastEntry -> mcastIp.equals(mcastEntry.getKey().mcastIp()))
                    .collect(Collectors.toMap(Entry::getKey,
                                              entry -> entry.getValue().value().id()));
        }
        // Otherwise take all the groups
        return mcastNextObjStore.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey,
                                          entry -> entry.getValue().value().id()));
    }

    /**
     * Returns the associated roles to the mcast groups or to the single
     * group if mcastIp is present.
     *
     * @param mcastIp the group ip
     * @return the mapping mcastIp-device to mcast role
     *
     * @deprecated in 1.12 ("Magpie") release.
     */
    @Deprecated
    public Map<McastStoreKey, McastRole> getMcastRoles(IpAddress mcastIp) {
        // If mcast ip is present
        if (mcastIp != null) {
            return mcastRoleStore.entrySet().stream()
                    .filter(mcastEntry -> mcastIp.equals(mcastEntry.getKey().mcastIp()))
                    .collect(Collectors.toMap(Entry::getKey,
                                              entry -> entry.getValue().value()));
        }
        // Otherwise take all the groups
        return mcastRoleStore.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey,
                                          entry -> entry.getValue().value()));
    }

    /**
     * Returns the associated paths to the mcast group.
     *
     * @param mcastIp the group ip
     * @return the mapping egress point to mcast path
     *
     * @deprecated in 1.12 ("Magpie") release.
     */
    @Deprecated
    public Map<ConnectPoint, List<ConnectPoint>> getMcastPaths(IpAddress mcastIp) {
        Map<ConnectPoint, List<ConnectPoint>> mcastPaths = Maps.newHashMap();
        // Get the source
        ConnectPoint source = mcastUtils.getSource(mcastIp);
        // Source cannot be null, we don't know the starting point
        if (source != null) {
            // Init steps
            Set<DeviceId> visited = Sets.newHashSet();
            List<ConnectPoint> currentPath = Lists.newArrayList(
                    source
            );
            // Build recursively the mcast paths
            buildMcastPaths(source.deviceId(), visited, mcastPaths, currentPath, mcastIp);
        }
        return mcastPaths;
    }

    /**
     * Returns the associated trees to the mcast group.
     *
     * @param mcastIp the group ip
     * @param sourcecp the source connect point
     * @return the mapping egress point to mcast path
     */
    public Multimap<ConnectPoint, List<ConnectPoint>> getMcastTrees(IpAddress mcastIp,
                                                                    ConnectPoint sourcecp) {
        Multimap<ConnectPoint, List<ConnectPoint>> mcastTrees = HashMultimap.create();
        // Get the sources
        Set<ConnectPoint> sources = mcastUtils.getSources(mcastIp);

        // If we are providing the source, let's filter out
        if (sourcecp != null) {
            sources = sources.stream()
                    .filter(source -> source.equals(sourcecp))
                    .collect(Collectors.toSet());
        }

        // Source cannot be null, we don't know the starting point
        if (!sources.isEmpty()) {
            sources.forEach(source -> {
                // Init steps
                Map<ConnectPoint, List<ConnectPoint>> mcastPaths = Maps.newHashMap();
                Set<DeviceId> visited = Sets.newHashSet();
                List<ConnectPoint> currentPath = Lists.newArrayList(source);
                // Build recursively the mcast paths
                buildMcastPaths(source.deviceId(), visited, mcastPaths, currentPath, mcastIp);
                mcastPaths.forEach(mcastTrees::put);
            });
        }
        return mcastTrees;
    }

    /**
     * Build recursively the mcast paths.
     *
     * @param toVisit the node to visit
     * @param visited the visited nodes
     * @param mcastPaths the current mcast paths
     * @param currentPath the current path
     * @param mcastIp the group ip
     */
    private void buildMcastPaths(DeviceId toVisit, Set<DeviceId> visited,
                                 Map<ConnectPoint, List<ConnectPoint>> mcastPaths,
                                 List<ConnectPoint> currentPath, IpAddress mcastIp) {
        // If we have visited the node to visit
        // there is a loop
        if (visited.contains(toVisit)) {
            return;
        }
        // Visit next-hop
        visited.add(toVisit);
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, toVisit);
        // Looking for next-hops
        if (mcastNextObjStore.containsKey(mcastStoreKey)) {
            // Build egress connectpoints
            NextObjective nextObjective = mcastNextObjStore.get(mcastStoreKey).value();
            // Get Ports
            Set<PortNumber> outputPorts = mcastUtils.getPorts(nextObjective.next());
            // Build relative cps
            ImmutableSet.Builder<ConnectPoint> cpBuilder = ImmutableSet.builder();
            outputPorts.forEach(portNumber -> cpBuilder.add(new ConnectPoint(toVisit, portNumber)));
            Set<ConnectPoint> egressPoints = cpBuilder.build();
            // Define other variables for the next steps
            Set<Link> egressLinks;
            List<ConnectPoint> newCurrentPath;
            Set<DeviceId> newVisited;
            DeviceId newToVisit;
            for (ConnectPoint egressPoint : egressPoints) {
                egressLinks = srManager.linkService.getEgressLinks(egressPoint);
                // If it does not have egress links, stop
                if (egressLinks.isEmpty()) {
                    // Add the connect points to the path
                    newCurrentPath = Lists.newArrayList(currentPath);
                    newCurrentPath.add(0, egressPoint);
                    // Save in the map
                    mcastPaths.put(egressPoint, newCurrentPath);
                } else {
                    newVisited = Sets.newHashSet(visited);
                    // Iterate over the egress links for the next hops
                    for (Link egressLink : egressLinks) {
                        // Update to visit
                        newToVisit = egressLink.dst().deviceId();
                        // Add the connect points to the path
                        newCurrentPath = Lists.newArrayList(currentPath);
                        newCurrentPath.add(0, egressPoint);
                        newCurrentPath.add(0, egressLink.dst());
                        // Go to the next hop
                        buildMcastPaths(newToVisit, newVisited, mcastPaths, newCurrentPath, mcastIp);
                    }
                }
            }
        }
    }

    /**
     * Return the leaders of the mcast groups.
     *
     * @param mcastIp the group ip
     * @return the mapping group-node
     */
    public Map<IpAddress, NodeId> getMcastLeaders(IpAddress mcastIp) {
        return mcastUtils.getMcastLeaders(mcastIp);
    }
}
