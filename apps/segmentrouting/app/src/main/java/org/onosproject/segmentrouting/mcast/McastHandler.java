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

import com.google.common.base.Objects;
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
import org.onosproject.net.Device;
import org.onosproject.net.HostId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.Port;
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
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
    // Internal elements
    private static final Logger log = LoggerFactory.getLogger(McastHandler.class);
    private final SegmentRoutingManager srManager;
    private final TopologyService topologyService;
    private final McastUtils mcastUtils;
    private final ConsistentMap<McastStoreKey, NextObjective> mcastNextObjStore;
    private final ConsistentMap<McastRoleStoreKey, McastRole> mcastRoleStore;
    private final DistributedSet<McastFilteringObjStoreKey> mcastFilteringObjStore;
    // Stability threshold for Mcast. Seconds
    private static final long MCAST_STABLITY_THRESHOLD = 5;
    // Verify interval for Mcast bucket corrector
    private static final long MCAST_VERIFY_INTERVAL = 30;
    // Max verify that can be processed at the same time
    private static final int MAX_VERIFY_ON_FLIGHT = 10;
    // Last change done
    private AtomicReference<Instant> lastMcastChange = new AtomicReference<>(Instant.now());
    // Last bucker corrector execution
    private AtomicReference<Instant> lastBktCorrExecution = new AtomicReference<>(Instant.now());
    // Executors for mcast bucket corrector and for the events
    private ScheduledExecutorService mcastCorrector
            = newScheduledThreadPool(1, groupedThreads("onos", "m-corrector", log));
    private ScheduledExecutorService mcastWorker
            = newScheduledThreadPool(1, groupedThreads("onos", "m-worker-%d", log));

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
                .register(new McastStoreKeySerializer(), McastStoreKey.class);
        mcastNextObjStore = srManager.storageService
                .<McastStoreKey, NextObjective>consistentMapBuilder()
                .withName("onos-mcast-nextobj-store")
                .withSerializer(Serializer.using(mcastKryo.build("McastHandler-NextObj")))
                .build();
        mcastKryo = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(new McastRoleStoreKeySerializer(), McastRoleStoreKey.class)
                .register(McastRole.class);
        mcastRoleStore = srManager.storageService
                .<McastRoleStoreKey, McastRole>consistentMapBuilder()
                .withName("onos-mcast-role-store")
                .withSerializer(Serializer.using(mcastKryo.build("McastHandler-Role")))
                .build();
        mcastKryo = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(new McastFilteringObjStoreSerializer(), McastFilteringObjStoreKey.class);
        mcastFilteringObjStore = srManager.storageService
                .<McastFilteringObjStoreKey>setBuilder()
                .withName("onos-mcast-filtering-store")
                .withSerializer(Serializer.using(mcastKryo.build("McastHandler-FilteringObj")))
                .build()
                .asDistributedSet();
        mcastUtils = new McastUtils(srManager, coreAppId, log);
        // Init the executor for the buckets corrector
        mcastCorrector.scheduleWithFixedDelay(new McastBucketCorrector(), 10,
                                              MCAST_VERIFY_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Determines if mcast in the network has been stable in the last
     * MCAST_STABLITY_THRESHOLD seconds, by comparing the current time
     * to the last mcast change timestamp.
     *
     * @return true if stable
     */
    private boolean isMcastStable() {
        long last = (long) (lastMcastChange.get().toEpochMilli() / 1000.0);
        long now = (long) (Instant.now().toEpochMilli() / 1000.0);
        log.trace("Multicast stable since {}s", now - last);
        return (now - last) > MCAST_STABLITY_THRESHOLD;
    }

    /**
     * Assures there are always MCAST_VERIFY_INTERVAL seconds between each execution,
     * by comparing the current time with the last corrector execution.
     *
     * @return true if stable
     */
    private boolean wasBktCorrRunning() {
        long last = (long) (lastBktCorrExecution.get().toEpochMilli() / 1000.0);
        long now = (long) (Instant.now().toEpochMilli() / 1000.0);
        log.trace("McastBucketCorrector executed {}s ago", now - last);
        return (now - last) < MCAST_VERIFY_INTERVAL;
    }

    /**
     * Read initial multicast configuration from mcast store.
     */
    public void init() {
        mcastWorker.execute(this::initInternal);
    }

    private void initInternal() {
        lastMcastChange.set(Instant.now());
        srManager.multicastRouteService.getRoutes().forEach(mcastRoute -> {
            log.debug("Init group {}", mcastRoute.group());
            if (!mcastUtils.isLeader(mcastRoute.group())) {
                log.debug("Skip {} due to lack of leadership", mcastRoute.group());
                return;
            }
            McastRouteData mcastRouteData = srManager.multicastRouteService.routeData(mcastRoute);
            // For each source process the mcast tree
            srManager.multicastRouteService.sources(mcastRoute).forEach(source -> {
                Map<ConnectPoint, List<ConnectPoint>> mcastPaths = Maps.newHashMap();
                Set<DeviceId> visited = Sets.newHashSet();
                List<ConnectPoint> currentPath = Lists.newArrayList(source);
                mcastUtils.buildMcastPaths(mcastNextObjStore.asJavaMap(), source.deviceId(), visited, mcastPaths,
                                           currentPath, mcastRoute.group(), source);
                // Get all the sinks and process them
                Set<ConnectPoint> sinks = processSinksToBeAdded(source, mcastRoute.group(),
                                                                mcastRouteData.sinks());
                // Filter out all the working sinks, we do not want to move them
                // TODO we need a better way to distinguish flows coming from different sources
                sinks = sinks.stream()
                        .filter(sink -> !mcastPaths.containsKey(sink) ||
                                !isSinkForSource(mcastRoute.group(), sink, source))
                        .collect(Collectors.toSet());
                if (sinks.isEmpty()) {
                    log.debug("Skip {} for source {} nothing to do", mcastRoute.group(), source);
                    return;
                }
                Map<ConnectPoint, List<Path>> mcasTree = computeSinkMcastTree(mcastRoute.group(),
                                                                              source.deviceId(), sinks);
                mcasTree.forEach((sink, paths) -> processSinkAddedInternal(source, sink,
                                                                           mcastRoute.group(), paths));
            });
        });
    }

    /**
     * Clean up when deactivating the application.
     */
    public void terminate() {
        mcastCorrector.shutdown();
        mcastWorker.shutdown();
        mcastNextObjStore.destroy();
        mcastRoleStore.destroy();
        mcastFilteringObjStore.destroy();
        mcastUtils.terminate();
        log.info("Terminated");
    }

    /**
     * Processes the SOURCE_ADDED, SOURCE_UPDATED, SINK_ADDED,
     * SINK_REMOVED, ROUTE_ADDED and ROUTE_REMOVED events.
     *
     * @param event the multicast event to be processed
     */
    public void processMcastEvent(McastEvent event) {
        mcastWorker.execute(() -> processMcastEventInternal(event));
    }

    private void processMcastEventInternal(McastEvent event) {
        lastMcastChange.set(Instant.now());
        // Current subject is null, for ROUTE_REMOVED events
        final McastRouteUpdate mcastUpdate = event.subject();
        final McastRouteUpdate mcastPrevUpdate = event.prevSubject();
        IpAddress mcastIp = mcastPrevUpdate.route().group();
        Set<ConnectPoint> prevSinks = mcastPrevUpdate.sinks()
                .values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        Set<ConnectPoint> prevSources = mcastPrevUpdate.sources()
                .values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        Set<ConnectPoint> sources;
        // Events handling
        if (event.type() == ROUTE_ADDED) {
            processRouteAddedInternal(mcastUpdate.route().group());
        } else if (event.type() == ROUTE_REMOVED) {
            processRouteRemovedInternal(prevSources, mcastIp);
        } else if (event.type() == SOURCES_ADDED) {
            // Current subject and prev just differ for the source connect points
            sources = mcastUpdate.sources()
                    .values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            Set<ConnectPoint> sourcesToBeAdded = Sets.difference(sources, prevSources);
            processSourcesAddedInternal(sourcesToBeAdded, mcastIp, mcastUpdate.sinks());
        } else if (event.type() == SOURCES_REMOVED) {
            // Current subject and prev just differ for the source connect points
            sources = mcastUpdate.sources()
                    .values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            Set<ConnectPoint> sourcesToBeRemoved = Sets.difference(prevSources, sources);
            processSourcesRemovedInternal(sourcesToBeRemoved, sources, mcastIp, mcastUpdate.sinks());
        } else if (event.type() == SINKS_ADDED) {
            processSinksAddedInternal(prevSources, mcastIp, mcastUpdate.sinks(), prevSinks);
        } else if (event.type() == SINKS_REMOVED) {
            processSinksRemovedInternal(prevSources, mcastIp, mcastUpdate.sinks(), mcastPrevUpdate.sinks());
        } else {
            log.warn("Event {} not handled", event);
        }
    }

    /**
     * Process the SOURCES_ADDED event.
     *
     * @param sources the sources connect point
     * @param mcastIp the group address
     * @param sinks the sinks connect points
     */
    private void processSourcesAddedInternal(Set<ConnectPoint> sources, IpAddress mcastIp,
                                             Map<HostId, Set<ConnectPoint>> sinks) {
        lastMcastChange.set(Instant.now());
        log.info("Processing sources added {} for group {}", sources, mcastIp);
        if (!mcastUtils.isLeader(mcastIp)) {
            log.debug("Skip {} due to lack of leadership", mcastIp);
            return;
        }
        if (sources.isEmpty()) {
            log.debug("Skip {} due to empty sources to be added", mcastIp);
            return;
        }
        sources.forEach(source -> {
            Set<ConnectPoint> sinksToBeAdded = processSinksToBeAdded(source, mcastIp, sinks);
            Map<ConnectPoint, List<Path>> mcasTree = computeSinkMcastTree(mcastIp, source.deviceId(),
                                                                          sinksToBeAdded);
            mcasTree.forEach((sink, paths) -> processSinkAddedInternal(source, sink, mcastIp, paths));
        });
    }

    /**
     * Process the SOURCES_REMOVED event.
     *
     * @param sourcesToBeRemoved the source connect points to be removed
     * @param remainingSources the remainig source connect points
     * @param mcastIp the group address
     * @param sinks the sinks connect points
     */
    private void processSourcesRemovedInternal(Set<ConnectPoint> sourcesToBeRemoved,
                                               Set<ConnectPoint> remainingSources,
                                               IpAddress mcastIp,
                                               Map<HostId, Set<ConnectPoint>> sinks) {
        lastMcastChange.set(Instant.now());
        log.info("Processing sources removed {} for group {}", sourcesToBeRemoved, mcastIp);
        if (!mcastUtils.isLeader(mcastIp)) {
            log.debug("Skip {} due to lack of leadership", mcastIp);
            return;
        }
        if (remainingSources.isEmpty()) {
            log.debug("There are no more sources for {}", mcastIp);
            processRouteRemovedInternal(sourcesToBeRemoved, mcastIp);
            return;
        }
        // Skip offline devices
        Set<ConnectPoint> candidateSources = sourcesToBeRemoved.stream()
                .filter(source -> srManager.deviceService.isAvailable(source.deviceId()))
                .collect(Collectors.toSet());
        if (candidateSources.isEmpty()) {
            log.debug("Skip {} due to empty sources to be removed", mcastIp);
            return;
        }
        // Let's heal the trees
        Set<Link> remainingLinks = Sets.newHashSet();
        Map<ConnectPoint, Set<Link>> candidateLinks = Maps.newHashMap();
        Map<ConnectPoint, Set<ConnectPoint>> candidateSinks = Maps.newHashMap();
        Set<ConnectPoint> totalSources = Sets.newHashSet(candidateSources);
        totalSources.addAll(remainingSources);
        // Calculate all the links used by the sources
        totalSources.forEach(source -> {
            Set<ConnectPoint> currentSinks = sinks.values()
                    .stream().flatMap(Collection::stream)
                    .filter(sink -> isSinkForSource(mcastIp, sink, source))
                    .collect(Collectors.toSet());
            candidateSinks.put(source, currentSinks);
            currentSinks.forEach(currentSink -> {
                Optional<Path> currentPath = getPath(source.deviceId(), currentSink.deviceId(),
                                                     mcastIp, null, source);
                if (currentPath.isPresent()) {
                    if (!candidateSources.contains(source)) {
                        remainingLinks.addAll(currentPath.get().links());
                    } else {
                        candidateLinks.put(source, Sets.newHashSet(currentPath.get().links()));
                    }
                }
            });
        });
        // Clean transit links
        candidateLinks.forEach((source, currentCandidateLinks) -> {
            Set<Link> linksToBeRemoved = Sets.difference(currentCandidateLinks, remainingLinks)
                    .immutableCopy();
            if (!linksToBeRemoved.isEmpty()) {
                currentCandidateLinks.forEach(link -> {
                    DeviceId srcLink = link.src().deviceId();
                    // Remove ports only on links to be removed
                    if (linksToBeRemoved.contains(link)) {
                        removePortFromDevice(link.src().deviceId(), link.src().port(), mcastIp,
                                             mcastUtils.assignedVlan(srcLink.equals(source.deviceId()) ?
                                                                             source : null));
                    }
                    // Remove role on the candidate links
                    mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, srcLink, source));
                });
            }
        });
        // Clean ingress and egress
        candidateSources.forEach(source -> {
            Set<ConnectPoint> currentSinks = candidateSinks.get(source);
            currentSinks.forEach(currentSink -> {
                VlanId assignedVlan = mcastUtils.assignedVlan(source.deviceId().equals(currentSink.deviceId()) ?
                                                                      source : null);
                // Sinks co-located with the source
                if (source.deviceId().equals(currentSink.deviceId())) {
                    if (source.port().equals(currentSink.port())) {
                        log.warn("Skip {} since sink {} is on the same port of source {}. Abort",
                                 mcastIp, currentSink, source);
                        return;
                    }
                    // We need to check against the other sources and if it is
                    // necessary remove the port from the device - no overlap
                    Set<VlanId> otherVlans = remainingSources.stream()
                            // Only sources co-located and having this sink
                            .filter(remainingSource -> remainingSource.deviceId()
                                    .equals(source.deviceId()) && candidateSinks.get(remainingSource)
                                    .contains(currentSink))
                            .map(remainingSource -> mcastUtils.assignedVlan(
                                    remainingSource.deviceId().equals(currentSink.deviceId()) ?
                                            remainingSource : null)).collect(Collectors.toSet());
                    if (!otherVlans.contains(assignedVlan)) {
                        removePortFromDevice(currentSink.deviceId(), currentSink.port(),
                                             mcastIp, assignedVlan);
                    }
                    mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, currentSink.deviceId(),
                                                                source));
                    return;
                }
                Set<VlanId> otherVlans = remainingSources.stream()
                        .filter(remainingSource -> candidateSinks.get(remainingSource)
                                .contains(currentSink))
                        .map(remainingSource -> mcastUtils.assignedVlan(
                                remainingSource.deviceId().equals(currentSink.deviceId()) ?
                                        remainingSource : null)).collect(Collectors.toSet());
                // Sinks on other leaves
                if (!otherVlans.contains(assignedVlan)) {
                    removePortFromDevice(currentSink.deviceId(), currentSink.port(),
                                         mcastIp, assignedVlan);
                }
                mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, currentSink.deviceId(),
                                                            source));
            });
        });
    }

    /**
     * Process the ROUTE_ADDED event.
     *
     * @param mcastIp the group address
     */
    private void processRouteAddedInternal(IpAddress mcastIp) {
        lastMcastChange.set(Instant.now());
        log.info("Processing route added for Multicast group {}", mcastIp);
        // Just elect a new leader
        mcastUtils.isLeader(mcastIp);
    }

    /**
     * Removes the entire mcast tree related to this group.
     * @param sources the source connect points
     * @param mcastIp multicast group IP address
     */
    private void processRouteRemovedInternal(Set<ConnectPoint> sources, IpAddress mcastIp) {
        lastMcastChange.set(Instant.now());
        log.info("Processing route removed for group {}", mcastIp);
        if (!mcastUtils.isLeader(mcastIp)) {
            log.debug("Skip {} due to lack of leadership", mcastIp);
            mcastUtils.withdrawLeader(mcastIp);
            return;
        }
        sources.forEach(source -> {
            // Find out the ingress, transit and egress device of the affected group
            DeviceId ingressDevice = getDevice(mcastIp, INGRESS, source)
                    .stream().findFirst().orElse(null);
            Set<DeviceId> transitDevices = getDevice(mcastIp, TRANSIT, source);
            Set<DeviceId> egressDevices = getDevice(mcastIp, EGRESS, source);
            // If there are no egress and transit devices, sinks could be only on the ingress
            if (!egressDevices.isEmpty()) {
                egressDevices.forEach(deviceId -> {
                    removeGroupFromDevice(deviceId, mcastIp, mcastUtils.assignedVlan(null));
                    mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, deviceId, source));
                });
            }
            if (!transitDevices.isEmpty()) {
                transitDevices.forEach(deviceId -> {
                    removeGroupFromDevice(deviceId, mcastIp, mcastUtils.assignedVlan(null));
                    mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, deviceId, source));
                });
            }
            if (ingressDevice != null) {
                removeGroupFromDevice(ingressDevice, mcastIp, mcastUtils.assignedVlan(source));
                mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, ingressDevice, source));
            }
        });
        // Finally, withdraw the leadership
        mcastUtils.withdrawLeader(mcastIp);
    }

    /**
     * Process sinks to be removed.
     *
     * @param sources the source connect points
     * @param mcastIp the ip address of the group
     * @param newSinks the new sinks to be processed
     * @param prevSinks the previous sinks
     */
    private void processSinksRemovedInternal(Set<ConnectPoint> sources, IpAddress mcastIp,
                                             Map<HostId, Set<ConnectPoint>> newSinks,
                                             Map<HostId, Set<ConnectPoint>> prevSinks) {
        lastMcastChange.set(Instant.now());
        log.info("Processing sinks removed for group {} and for sources {}",
                  mcastIp, sources);
        if (!mcastUtils.isLeader(mcastIp)) {
            log.debug("Skip {} due to lack of leadership", mcastIp);
            return;
        }
        Map<ConnectPoint, Map<ConnectPoint, Optional<Path>>> treesToBeRemoved = Maps.newHashMap();
        Map<ConnectPoint, Set<ConnectPoint>> treesToBeAdded = Maps.newHashMap();
        sources.forEach(source -> {
            // Save the path associated to the sinks to be removed
            Set<ConnectPoint> candidateSinks = processSinksToBeRemoved(mcastIp, prevSinks,
                                                                         newSinks, source);
            // Skip offline devices
            Set<ConnectPoint> sinksToBeRemoved = candidateSinks.stream()
                    .filter(sink -> srManager.deviceService.isAvailable(sink.deviceId()))
                    .collect(Collectors.toSet());
            Map<ConnectPoint, Optional<Path>> treeToBeRemoved = Maps.newHashMap();
            sinksToBeRemoved.forEach(sink -> treeToBeRemoved.put(sink, getPath(source.deviceId(),
                                                                               sink.deviceId(), mcastIp,
                                                                               null, source)));
            treesToBeRemoved.put(source, treeToBeRemoved);
            // Recover the dual-homed sinks
            Set<ConnectPoint> sinksToBeRecovered = processSinksToBeRecovered(mcastIp, newSinks,
                                                                             prevSinks, source);
            treesToBeAdded.put(source, sinksToBeRecovered);
        });
        // Remove the sinks taking into account the multiple sources and the original paths
        treesToBeRemoved.forEach((source, tree) ->
            tree.forEach((sink, path) -> processSinkRemovedInternal(source, sink, mcastIp, path)));
        // Add new sinks according to the recovery procedure
        treesToBeAdded.forEach((source, sinks) ->
            sinks.forEach(sink -> processSinkAddedInternal(source, sink, mcastIp, null)));
    }

    /**
     * Removes a path from source to sink for given multicast group.
     *
     * @param source connect point of the multicast source
     * @param sink connection point of the multicast sink
     * @param mcastIp multicast group IP address
     * @param mcastPath path associated to the sink
     */
    private void processSinkRemovedInternal(ConnectPoint source, ConnectPoint sink,
                                            IpAddress mcastIp, Optional<Path> mcastPath) {
        lastMcastChange.set(Instant.now());
        log.info("Processing sink removed {} for group {} and for source {}", sink, mcastIp, source);
        boolean isLast;
        // When source and sink are on the same device
        if (source.deviceId().equals(sink.deviceId())) {
            // Source and sink are on even the same port. There must be something wrong.
            if (source.port().equals(sink.port())) {
                log.warn("Skip {} since sink {} is on the same port of source {}. Abort", mcastIp, sink, source);
                return;
            }
            isLast = removePortFromDevice(sink.deviceId(), sink.port(), mcastIp, mcastUtils.assignedVlan(source));
            if (isLast) {
                mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, sink.deviceId(), source));
            }
            return;
        }
        // Process the egress device
        isLast = removePortFromDevice(sink.deviceId(), sink.port(), mcastIp, mcastUtils.assignedVlan(null));
        if (isLast) {
            mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, sink.deviceId(), source));
        }
        // If this is the last sink on the device, also update upstream
        if (mcastPath.isPresent()) {
            List<Link> links = Lists.newArrayList(mcastPath.get().links());
            Collections.reverse(links);
            for (Link link : links) {
                if (isLast) {
                    isLast = removePortFromDevice(link.src().deviceId(), link.src().port(), mcastIp,
                    mcastUtils.assignedVlan(link.src().deviceId().equals(source.deviceId()) ? source : null));
                    if (isLast) {
                        mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, link.src().deviceId(), source));
                    }
                }
            }
        } else {
            log.warn("Unable to find a path from {} to {}. Abort sinkRemoved", source.deviceId(), sink.deviceId());
        }
    }

    /**
     * Process sinks to be added.
     *
     * @param sources the source connect points
     * @param mcastIp the group IP
     * @param newSinks the new sinks to be processed
     * @param allPrevSinks all previous sinks
     */
    private void processSinksAddedInternal(Set<ConnectPoint> sources, IpAddress mcastIp,
                                           Map<HostId, Set<ConnectPoint>> newSinks,
                                           Set<ConnectPoint> allPrevSinks) {
        lastMcastChange.set(Instant.now());
        log.info("Processing sinks added for group {} and for sources {}", mcastIp, sources);
        if (!mcastUtils.isLeader(mcastIp)) {
            log.debug("Skip {} due to lack of leadership", mcastIp);
            return;
        }
        sources.forEach(source -> {
            Set<ConnectPoint> sinksToBeAdded = processSinksToBeAdded(source, mcastIp, newSinks);
            sinksToBeAdded = Sets.difference(sinksToBeAdded, allPrevSinks);
            sinksToBeAdded.forEach(sink -> processSinkAddedInternal(source, sink, mcastIp, null));
        });
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
        lastMcastChange.set(Instant.now());
        log.info("Processing sink added {} for group {} and for source {}", sink, mcastIp, source);
        // Process the ingress device
        McastFilteringObjStoreKey mcastFilterObjStoreKey = new McastFilteringObjStoreKey(source,
                                                           mcastUtils.assignedVlan(source), mcastIp.isIp4());
        addFilterToDevice(mcastFilterObjStoreKey, mcastIp, INGRESS);
        if (source.deviceId().equals(sink.deviceId())) {
            if (source.port().equals(sink.port())) {
                log.warn("Skip {} since sink {} is on the same port of source {}. Abort",
                         mcastIp, sink, source);
                return;
            }
            addPortToDevice(sink.deviceId(), sink.port(), mcastIp, mcastUtils.assignedVlan(source));
            mcastRoleStore.put(new McastRoleStoreKey(mcastIp, sink.deviceId(), source), INGRESS);
            return;
        }
        // Find a path. If present, create/update groups and flows for each hop
        Optional<Path> mcastPath = getPath(source.deviceId(), sink.deviceId(), mcastIp, allPaths, source);
        if (mcastPath.isPresent()) {
            List<Link> links = mcastPath.get().links();
            // Setup mcast role for ingress
            mcastRoleStore.put(new McastRoleStoreKey(mcastIp, source.deviceId(), source), INGRESS);
            // Setup properly the transit forwarding
            links.forEach(link -> {
                addPortToDevice(link.src().deviceId(), link.src().port(), mcastIp,
                                mcastUtils.assignedVlan(link.src().deviceId()
                                                                .equals(source.deviceId()) ? source : null));
                McastFilteringObjStoreKey filteringKey = new McastFilteringObjStoreKey(link.dst(),
                                                                   mcastUtils.assignedVlan(null), mcastIp.isIp4());
                addFilterToDevice(filteringKey, mcastIp, null);
            });
            // Setup mcast role for the transit
            links.stream()
                    .filter(link -> !link.dst().deviceId().equals(sink.deviceId()))
                    .forEach(link -> {
                        log.trace("Transit links {}", link);
                        mcastRoleStore.put(new McastRoleStoreKey(mcastIp, link.dst().deviceId(),
                                source), TRANSIT);
                    });
            // Process the egress device
            addPortToDevice(sink.deviceId(), sink.port(), mcastIp, mcastUtils.assignedVlan(null));
            // Setup mcast role for egress
            mcastRoleStore.put(new McastRoleStoreKey(mcastIp, sink.deviceId(), source), EGRESS);
        } else {
            log.warn("Unable to find a path from {} to {}. Abort sinkAdded", source.deviceId(), sink.deviceId());
        }
    }

    /**
     * Processes the PORT_UPDATED event.
     *
     * @param affectedDevice Affected device
     * @param affectedPort Affected port
     */
    public void processPortUpdate(Device affectedDevice, Port affectedPort) {
        mcastWorker.execute(() -> processPortUpdateInternal(affectedDevice, affectedPort));
    }

    private void processPortUpdateInternal(Device affectedDevice, Port affectedPort) {
        // Clean the filtering obj store. Edge port case.
        lastMcastChange.set(Instant.now());
        ConnectPoint portDown = new ConnectPoint(affectedDevice.id(), affectedPort.number());
        if (!affectedPort.isEnabled()) {
            log.info("Processing port down {}", portDown);
            updateFilterObjStoreByPort(portDown);
        }
    }

    /**
     * Processes the LINK_DOWN event.
     *
     * @param linkDown Link that is going down
     */
    public void processLinkDown(Link linkDown) {
        mcastWorker.execute(() -> processLinkDownInternal(linkDown));
    }

    private void processLinkDownInternal(Link linkDown) {
        lastMcastChange.set(Instant.now());
        // Get mcast groups affected by the link going down
        Set<IpAddress> affectedGroups = getAffectedGroups(linkDown);
        log.info("Processing link down {} for groups {}", linkDown, affectedGroups);
        affectedGroups.forEach(mcastIp -> {
            log.debug("Processing link down {} for group {}", linkDown, mcastIp);
            recoverFailure(mcastIp, linkDown);
        });
    }

    /**
     * Process the DEVICE_DOWN event.
     *
     * @param deviceDown device going down
     */
    public void processDeviceDown(DeviceId deviceDown) {
        mcastWorker.execute(() -> processDeviceDownInternal(deviceDown));
    }

    private void processDeviceDownInternal(DeviceId deviceDown) {
        lastMcastChange.set(Instant.now());
        // Get the mcast groups affected by the device going down
        Set<IpAddress> affectedGroups = getAffectedGroups(deviceDown);
        log.info("Processing device down {} for groups {}", deviceDown, affectedGroups);
        updateFilterObjStoreByDevice(deviceDown);
        affectedGroups.forEach(mcastIp -> {
            log.debug("Processing device down {} for group {}", deviceDown, mcastIp);
            recoverFailure(mcastIp, deviceDown);
        });
    }

    /**
     * General failure recovery procedure.
     *
     * @param mcastIp the group to recover
     * @param failedElement the failed element
     */
    private void recoverFailure(IpAddress mcastIp, Object failedElement) {
        // TODO Optimize when the group editing is in place
        if (!mcastUtils.isLeader(mcastIp)) {
            log.debug("Skip {} due to lack of leadership", mcastIp);
            return;
        }
        // Do not proceed if the sources of this group are missing
        Set<ConnectPoint> sources = getSources(mcastIp);
        if (sources.isEmpty()) {
            log.warn("Missing sources for group {}", mcastIp);
            return;
        }
        // Find out the ingress devices of the affected group
        // If sinks are in other leafs, we have ingress, transit, egress, and source
        // If sinks are in the same leaf, we have just ingress and source
        Set<DeviceId> ingressDevices = getDevice(mcastIp, INGRESS);
        if (ingressDevices.isEmpty()) {
            log.warn("Missing ingress devices for group {}", mcastIp);
            return;
        }
        // For each tree, delete ingress-transit part
        sources.forEach(source -> {
            Set<DeviceId> transitDevices = getDevice(mcastIp, TRANSIT, source);
            transitDevices.forEach(transitDevice -> {
                removeGroupFromDevice(transitDevice, mcastIp, mcastUtils.assignedVlan(null));
                mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, transitDevice, source));
            });
        });
        removeIngressTransitPorts(mcastIp, ingressDevices, sources);
        // TODO Evaluate the possibility of building optimize trees between sources
        Map<DeviceId, Set<ConnectPoint>> notRecovered = Maps.newHashMap();
        sources.forEach(source -> {
            Set<DeviceId> notRecoveredInternal = Sets.newHashSet();
            DeviceId ingressDevice = ingressDevices.stream()
                    .filter(deviceId -> deviceId.equals(source.deviceId())).findFirst().orElse(null);
            // Clean also the ingress
            if (failedElement instanceof DeviceId && ingressDevice.equals(failedElement)) {
                removeGroupFromDevice((DeviceId) failedElement, mcastIp, mcastUtils.assignedVlan(source));
                mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, (DeviceId) failedElement, source));
            }
            if (ingressDevice == null) {
                log.warn("Skip failure recovery - " +
                                 "Missing ingress for source {} and group {}", source, mcastIp);
                return;
            }
            Set<DeviceId> egressDevices = getDevice(mcastIp, EGRESS, source);
            Map<DeviceId, List<Path>> mcastTree = computeMcastTree(mcastIp, ingressDevice, egressDevices);
            // We have to verify, if there are egresses without paths
            mcastTree.forEach((egressDevice, paths) -> {
                Optional<Path> mcastPath = getPath(ingressDevice, egressDevice,
                                                   mcastIp, paths, source);
                // No paths, we have to try with alternative location
                if (!mcastPath.isPresent()) {
                    notRecovered.compute(egressDevice, (deviceId, listSources) -> {
                        listSources = listSources == null ? Sets.newHashSet() : listSources;
                        listSources.add(source);
                        return listSources;
                    });
                    notRecoveredInternal.add(egressDevice);
                }
            });
            // Fast path, we can recover all the locations
            if (notRecoveredInternal.isEmpty()) {
                mcastTree.forEach((egressDevice, paths) -> {
                    Optional<Path> mcastPath = getPath(ingressDevice, egressDevice,
                                                       mcastIp, paths, source);
                    if (mcastPath.isPresent()) {
                        installPath(mcastIp, source, mcastPath.get());
                    }
                });
            } else {
                // Let's try to recover using alternative locations
                recoverSinks(egressDevices, notRecoveredInternal, mcastIp,
                             ingressDevice, source);
            }
        });
        // Finally remove the egresses not recovered
        notRecovered.forEach((egressDevice, listSources) -> {
            Set<ConnectPoint> currentSources = getSources(mcastIp, egressDevice, EGRESS);
            if (Objects.equal(currentSources, listSources)) {
                log.warn("Fail to recover egress device {} from {} failure {}",
                         egressDevice, failedElement instanceof Link ? "Link" : "Device", failedElement);
                removeGroupFromDevice(egressDevice, mcastIp, mcastUtils.assignedVlan(null));
            }
            listSources.forEach(source -> mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, egressDevice, source)));
        });
    }

    /**
     * Try to recover sinks using alternate locations.
     *
     * @param egressDevices the original egress devices
     * @param notRecovered the devices not recovered
     * @param mcastIp the group address
     * @param ingressDevice the ingress device
     * @param source the source connect point
     */
    private void recoverSinks(Set<DeviceId> egressDevices, Set<DeviceId> notRecovered,
                              IpAddress mcastIp, DeviceId ingressDevice, ConnectPoint source) {
        log.debug("Processing recover sinks for group {} and for source {}",
                  mcastIp, source);
        Set<DeviceId> recovered = Sets.difference(egressDevices, notRecovered);
        Set<ConnectPoint> totalAffectedSinks = Sets.newHashSet();
        Set<ConnectPoint> totalSinks = Sets.newHashSet();
        // Let's compute all the affected sinks and all the sinks
        notRecovered.forEach(deviceId -> {
            totalAffectedSinks.addAll(
                    mcastUtils.getAffectedSinks(deviceId, mcastIp).values().stream()
                            .flatMap(Collection::stream)
                            .filter(connectPoint -> connectPoint.deviceId().equals(deviceId))
                            .collect(Collectors.toSet())
            );
            totalSinks.addAll(
                    mcastUtils.getAffectedSinks(deviceId, mcastIp).values().stream()
                            .flatMap(Collection::stream).collect(Collectors.toSet())
            );
        });
        Set<ConnectPoint> sinksToBeAdded = Sets.difference(totalSinks, totalAffectedSinks);
        Set<DeviceId> newEgressDevices = sinksToBeAdded.stream()
                .map(ConnectPoint::deviceId).collect(Collectors.toSet());
        newEgressDevices.addAll(recovered);
        Set<DeviceId> copyNewEgressDevices = ImmutableSet.copyOf(newEgressDevices);
        newEgressDevices = newEgressDevices.stream()
                .filter(deviceId -> !deviceId.equals(ingressDevice)).collect(Collectors.toSet());
        Map<DeviceId, List<Path>> mcastTree = computeMcastTree(mcastIp, ingressDevice, newEgressDevices);
        // if the source was originally in the new locations, add new sinks
        if (copyNewEgressDevices.contains(ingressDevice)) {
            sinksToBeAdded.stream()
                    .filter(connectPoint -> connectPoint.deviceId().equals(ingressDevice))
                    .forEach(sink -> processSinkAddedInternal(source, sink, mcastIp, ImmutableList.of()));
        }
        // Construct a new path for each egress device
        mcastTree.forEach((egressDevice, paths) -> {
            Optional<Path> mcastPath = getPath(ingressDevice, egressDevice, mcastIp, paths, source);
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
     * @param source the source connect point
     * @return the set of the sinks to be removed
     */
    private Set<ConnectPoint> processSinksToBeRemoved(IpAddress mcastIp,
                                                      Map<HostId, Set<ConnectPoint>> prevsinks,
                                                      Map<HostId, Set<ConnectPoint>> newSinks,
                                                      ConnectPoint source) {
        final Set<ConnectPoint> sinksToBeProcessed = Sets.newHashSet();
        log.debug("Processing sinks to be removed for Multicast group {}, source {}",
                  mcastIp, source);
        prevsinks.forEach(((hostId, connectPoints) -> {
            // We have to check with the existing flows
            ConnectPoint sinkToBeProcessed = connectPoints.stream()
                    .filter(connectPoint -> isSinkForSource(mcastIp, connectPoint, source))
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
     * @param source the source connect point
     * @return the set of the sinks to be processed
     */
    private Set<ConnectPoint> processSinksToBeRecovered(IpAddress mcastIp,
                                                        Map<HostId, Set<ConnectPoint>> newSinks,
                                                        Map<HostId, Set<ConnectPoint>> prevSinks,
                                                        ConnectPoint source) {
        final Set<ConnectPoint> sinksToBeProcessed = Sets.newHashSet();
        log.debug("Processing sinks to be recovered for Multicast group {}, source {}",
                  mcastIp, source);
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
                        .filter(connectPoint -> !isSinkForSource(mcastIp, connectPoint, source))
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
        final Set<ConnectPoint> sinksToBeProcessed = Sets.newHashSet();
        log.debug("Processing sinks to be added for Multicast group {}, source {}",
                  mcastIp, source);
        sinks.forEach(((hostId, connectPoints) -> {
            // If it has more than 2 locations
            if (connectPoints.size() > 2 || connectPoints.size() == 0) {
                log.debug("Skip {} since sink {} has {} locations",
                         mcastIp, hostId, connectPoints.size());
                return;
            }
            // If it has one location, just use it
            if (connectPoints.size() == 1) {
                sinksToBeProcessed.add(connectPoints.stream().findFirst().orElse(null));
                return;
            }
            // We prefer to reuse existing flows
            ConnectPoint sinkToBeProcessed = connectPoints.stream()
                    .filter(connectPoint -> {
                        if (!isSinkForGroup(mcastIp, connectPoint, source)) {
                            return false;
                        }
                        if (!isSinkReachable(mcastIp, connectPoint, source)) {
                            return false;
                        }
                        ConnectPoint other = connectPoints.stream()
                                .filter(remaining -> !remaining.equals(connectPoint))
                                .findFirst().orElse(null);
                        // We are already serving the sink
                        return !isSinkForSource(mcastIp, other, source);
                    }).findFirst().orElse(null);

            if (sinkToBeProcessed != null) {
                sinksToBeProcessed.add(sinkToBeProcessed);
                return;
            }
            // Otherwise we prefer to reuse existing egresses
            Set<DeviceId> egresses = getDevice(mcastIp, EGRESS, source);
            sinkToBeProcessed = connectPoints.stream()
                    .filter(connectPoint -> {
                        if (!egresses.contains(connectPoint.deviceId())) {
                            return false;
                        }
                        if (!isSinkReachable(mcastIp, connectPoint, source)) {
                            return false;
                        }
                        ConnectPoint other = connectPoints.stream()
                                .filter(remaining -> !remaining.equals(connectPoint))
                                .findFirst().orElse(null);
                        return !isSinkForSource(mcastIp, other, source);
                    }).findFirst().orElse(null);
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
            // Finally, we randomly pick a new location if it is reachable
            sinkToBeProcessed = connectPoints.stream()
                    .filter(connectPoint -> {
                        if (!isSinkReachable(mcastIp, connectPoint, source)) {
                            return false;
                        }
                        ConnectPoint other = connectPoints.stream()
                                .filter(remaining -> !remaining.equals(connectPoint))
                                .findFirst().orElse(null);
                        return !isSinkForSource(mcastIp, other, source);
                    }).findFirst().orElse(null);
            if (sinkToBeProcessed != null) {
                sinksToBeProcessed.add(sinkToBeProcessed);
            }
        }));
        return sinksToBeProcessed;
    }

    /**
     * Utility method to remove all the ingress transit ports.
     *
     * @param mcastIp the group ip
     * @param ingressDevices the ingress devices
     * @param sources the source connect points
     */
    private void removeIngressTransitPorts(IpAddress mcastIp, Set<DeviceId> ingressDevices,
                                           Set<ConnectPoint> sources) {
        Map<ConnectPoint, Set<PortNumber>> ingressTransitPorts = Maps.newHashMap();
        sources.forEach(source -> {
            DeviceId ingressDevice = ingressDevices.stream()
                    .filter(deviceId -> deviceId.equals(source.deviceId()))
                    .findFirst().orElse(null);
            if (ingressDevice == null) {
                log.warn("Skip removeIngressTransitPorts - " +
                                 "Missing ingress for source {} and group {}",
                         source, mcastIp);
                return;
            }
            Set<PortNumber> ingressTransitPort = ingressTransitPort(mcastIp, ingressDevice, source);
            if (ingressTransitPort.isEmpty()) {
                log.warn("No transit ports to remove on device {}", ingressDevice);
                return;
            }
            ingressTransitPorts.put(source, ingressTransitPort);
        });
        ingressTransitPorts.forEach((source, ports) -> ports.forEach(ingressTransitPort -> {
            DeviceId ingressDevice = ingressDevices.stream()
                    .filter(deviceId -> deviceId.equals(source.deviceId()))
                    .findFirst().orElse(null);
            boolean isLast = removePortFromDevice(ingressDevice, ingressTransitPort,
                                                  mcastIp, mcastUtils.assignedVlan(source));
            if (isLast) {
                mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, ingressDevice, source));
            }
        }));
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
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, deviceId, assignedVlan);
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
            // Create, store and apply the new nextObj and fwdObj
            ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully add {} on {}/{}, vlan {}",
                        mcastIp, deviceId, port.toLong(), assignedVlan),
                (objective, error) -> {
                    log.warn("Failed to add {} on {}/{}, vlan {}: {}",
                            mcastIp, deviceId, port.toLong(), assignedVlan, error);
                    srManager.invalidateNextObj(objective.id());
                });
            ForwardingObjective fwdObj = mcastUtils.fwdObjBuilder(mcastIp, assignedVlan,
                                                          newNextObj.id()).add(context);
            srManager.flowObjectiveService.next(deviceId, newNextObj);
            srManager.flowObjectiveService.forward(deviceId, fwdObj);
        } else {
            // This device already serves some subscribers of this mcast group
            NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();
            // Stop if the port is already in the nextobj
            Set<PortNumber> existingPorts = mcastUtils.getPorts(nextObj.next());
            if (existingPorts.contains(port)) {
                log.debug("Port {}/{} already exists for {}. Abort", deviceId, port, mcastIp);
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
            // no need to update the flow here since we have updated the nextobjective/group
            // the existing flow will keep pointing to the updated nextobj
            srManager.flowObjectiveService.next(deviceId, newNextObj);
        }
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
                new McastStoreKey(mcastIp, deviceId, assignedVlan);
        // This device is not serving this multicast group
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            return true;
        }
        NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();
        Set<PortNumber> existingPorts = mcastUtils.getPorts(nextObj.next());
        // This port does not serve this multicast group
        if (!existingPorts.contains(port)) {
            if (!existingPorts.isEmpty()) {
                log.debug("{} is not serving {} on port {}. Abort.", deviceId, mcastIp, port);
                return false;
            }
            return true;
        }
        // Copy and modify the ImmutableSet
        existingPorts = Sets.newHashSet(existingPorts);
        existingPorts.remove(port);
        NextObjective newNextObj;
        ObjectiveContext context;
        ForwardingObjective fwdObj;
        if (existingPorts.isEmpty()) {
            context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Successfully remove {} on {}/{}, vlan {}",
                            mcastIp, deviceId, port.toLong(), assignedVlan),
                    (objective, error) -> log.warn("Failed to remove {} on {}/{}, vlan {}: {}",
                                    mcastIp, deviceId, port.toLong(), assignedVlan, error));
            fwdObj = mcastUtils.fwdObjBuilder(mcastIp, assignedVlan, nextObj.id()).remove(context);
            srManager.flowObjectiveService.forward(deviceId, fwdObj);
            mcastNextObjStore.remove(mcastStoreKey);
        } else {
            // Here we store the next objective with the remaining port
            newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                        existingPorts, nextObj.id()).removeFromExisting();
            mcastNextObjStore.put(mcastStoreKey, newNextObj);
             // Let's modify the next objective removing the bucket
            newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                    ImmutableSet.of(port), nextObj.id()).removeFromExisting();
            // no need to update the flow here since we have updated the next objective + group
            // the existing flow will keep pointing to the updated nextobj
            srManager.flowObjectiveService.next(deviceId, newNextObj);
        }
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
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, deviceId, assignedVlan);
        // This device is not serving this multicast group
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            log.debug("{} is not serving {}. Abort.", deviceId, mcastIp);
            return;
        }
        NextObjective nextObj = mcastNextObjStore.get(mcastStoreKey).value();
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully remove {} on {}, vlan {}",
                        mcastIp, deviceId, assignedVlan),
                (objective, error) -> log.warn("Failed to remove {} on {}, vlan {}: {}",
                                mcastIp, deviceId, assignedVlan, error));
        ForwardingObjective fwdObj = mcastUtils.fwdObjBuilder(mcastIp, assignedVlan, nextObj.id()).remove(context);
        srManager.flowObjectiveService.forward(deviceId, fwdObj);
        mcastNextObjStore.remove(mcastStoreKey);
    }

    private void installPath(IpAddress mcastIp, ConnectPoint source, Path mcastPath) {
        List<Link> links = mcastPath.links();
        // Setup new ingress mcast role
        mcastRoleStore.put(new McastRoleStoreKey(mcastIp, links.get(0).src().deviceId(), source),
                           INGRESS);
        // For each link, modify the next on the source device adding the src port
        // and a new filter objective on the destination port
        links.forEach(link -> {
            addPortToDevice(link.src().deviceId(), link.src().port(), mcastIp,
                            mcastUtils.assignedVlan(link.src().deviceId().equals(source.deviceId()) ? source : null));
            McastFilteringObjStoreKey mcastFilterObjStoreKey = new McastFilteringObjStoreKey(link.dst(),
                    mcastUtils.assignedVlan(null), mcastIp.isIp4());
            addFilterToDevice(mcastFilterObjStoreKey, mcastIp, null);
        });
        // Setup mcast role for the transit
        links.stream()
                .filter(link -> !link.src().deviceId().equals(source.deviceId()))
                .forEach(link -> mcastRoleStore.put(new McastRoleStoreKey(mcastIp, link.src().deviceId(), source),
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
        int minLength = Integer.MAX_VALUE;
        int length;
        List<Path> currentPaths;
        // Verify the source can still reach all the egresses
        for (DeviceId egress : egresses) {
            // From the source we cannot reach all the sinks
            // just continue and let's figure out after
            currentPaths = availablePaths.get(egress);
            if (currentPaths.isEmpty()) {
                continue;
            }
            // Get the length of the first one available, update the min length
            length = currentPaths.get(0).links().size();
            if (length < minLength) {
                minLength = length;
            }
        }
        // If there are no paths
        if (minLength == Integer.MAX_VALUE) {
            return Collections.emptySet();
        }
        int index = 0;
        Set<Link> sharedLinks = Sets.newHashSet();
        Set<Link> currentSharedLinks;
        Set<Link> currentLinks;
        DeviceId egressToRemove = null;
        // Let's find out the shared links
        while (index < minLength) {
            // Initialize the intersection with the paths related to the first egress
            currentPaths = availablePaths.get(egresses.stream().findFirst().orElse(null));
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
        // If the shared links is empty and there are egress let's retry another time with less sinks,
        // we can still build optimal subtrees
        if (sharedLinks.isEmpty() && egresses.size() > 1 && egressToRemove != null) {
            egresses.remove(egressToRemove);
            sharedLinks = exploreMcastTree(egresses, availablePaths);
        }
        return sharedLinks;
    }

    /**
     * Build Mcast tree having as root the given source and as leaves the given egress points.
     *
     * @param mcastIp multicast group
     * @param source source of the tree
     * @param sinks leaves of the tree
     * @return the computed Mcast tree
     */
    private Map<ConnectPoint, List<Path>> computeSinkMcastTree(IpAddress mcastIp,
                                                               DeviceId source,
                                                               Set<ConnectPoint> sinks) {
        // Get the egress devices, remove source from the egress if present
        Set<DeviceId> egresses = sinks.stream().map(ConnectPoint::deviceId)
                .filter(deviceId -> !deviceId.equals(source)).collect(Collectors.toSet());
        Map<DeviceId, List<Path>> mcastTree = computeMcastTree(mcastIp, source, egresses);
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
     * @param mcastIp multicast group
     * @param source source of the tree
     * @param egresses leaves of the tree
     * @return the computed Mcast tree
     */
    private Map<DeviceId, List<Path>> computeMcastTree(IpAddress mcastIp,
                                                       DeviceId source,
                                                       Set<DeviceId> egresses) {
        log.debug("Computing tree for Multicast group {}, source {} and leafs {}",
                  mcastIp, source, egresses);
        // Pre-compute all the paths
        Map<DeviceId, List<Path>> availablePaths = Maps.newHashMap();
        egresses.forEach(egress -> availablePaths.put(egress, getPaths(source, egress,
                                                                       Collections.emptySet())));
        // Explore the topology looking for shared links amongst the egresses
        Set<Link> linksToEnforce = exploreMcastTree(Sets.newHashSet(egresses), availablePaths);
        // Build the final paths enforcing the shared links between egress devices
        availablePaths.clear();
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
        final Topology currentTopology = topologyService.currentTopology();
        final LinkWeigher linkWeigher = new SRLinkWeigher(srManager, src, linksToEnforce);
        List<Path> allPaths = Lists.newArrayList(topologyService.getPaths(currentTopology, src, dst, linkWeigher));
        log.trace("{} path(s) found from {} to {}", allPaths.size(), src, dst);
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
    private Optional<Path> getPath(DeviceId src, DeviceId dst, IpAddress mcastIp,
                                   List<Path> allPaths, ConnectPoint source) {
        if (allPaths == null) {
            allPaths = getPaths(src, dst, Collections.emptySet());
        }
        if (allPaths.isEmpty()) {
            return Optional.empty();
        }
        // Create a map index of suitability-to-list of paths. For example
        // a path in the list associated to the index 1 shares only the
        // first hop and it is less suitable of a path belonging to the index
        // 2 that shares leaf-spine.
        Map<Integer, List<Path>> eligiblePaths = Maps.newHashMap();
        int nhop;
        McastStoreKey mcastStoreKey;
        PortNumber srcPort;
        Set<PortNumber> existingPorts;
        NextObjective nextObj;
        for (Path path : allPaths) {
            if (!src.equals(path.links().get(0).src().deviceId())) {
                continue;
            }
            nhop = 0;
            // Iterate over the links
            for (Link hop : path.links()) {
                VlanId assignedVlan = mcastUtils.assignedVlan(hop.src().deviceId().equals(src) ?
                                                                      source : null);
                mcastStoreKey = new McastStoreKey(mcastIp, hop.src().deviceId(), assignedVlan);
                // It does not exist in the store, go to the next link
                if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
                    continue;
                }
                nextObj = mcastNextObjStore.get(mcastStoreKey).value();
                existingPorts = mcastUtils.getPorts(nextObj.next());
                srcPort = hop.src().port();
                // the src port is not used as output, go to the next link
                if (!existingPorts.contains(srcPort)) {
                    continue;
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
        if (eligiblePaths.isEmpty()) {
            log.trace("No eligiblePath(s) found from {} to {}", src, dst);
            Collections.shuffle(allPaths);
            return allPaths.stream().findFirst();
        }
        // Let's take the best ones
        Integer bestIndex = eligiblePaths.keySet().stream()
                .sorted(Comparator.reverseOrder()).findFirst().orElse(null);
        List<Path> bestPaths = eligiblePaths.get(bestIndex);
        log.trace("{} eligiblePath(s) found from {} to {}",
                  bestPaths.size(), src, dst);
        Collections.shuffle(bestPaths);
        return bestPaths.stream().findFirst();
    }

    /**
     * Gets device(s) of given role and of given source in given multicast tree.
     *
     * @param mcastIp multicast IP
     * @param role multicast role
     * @param source source connect point
     * @return set of device ID or empty set if not found
     */
    private Set<DeviceId> getDevice(IpAddress mcastIp, McastRole role, ConnectPoint source) {
        return mcastRoleStore.entrySet().stream()
                .filter(entry -> entry.getKey().mcastIp().equals(mcastIp) &&
                        entry.getKey().source().equals(source) &&
                        entry.getValue().value() == role)
                .map(Entry::getKey).map(McastRoleStoreKey::deviceId).collect(Collectors.toSet());
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
                .map(Entry::getKey).map(McastRoleStoreKey::deviceId).collect(Collectors.toSet());
    }

    /**
     * Gets source(s) of given role, given device in given multicast group.
     *
     * @param mcastIp multicast IP
     * @param deviceId device id
     * @param role multicast role
     * @return set of device ID or empty set if not found
     */
    private Set<ConnectPoint> getSources(IpAddress mcastIp, DeviceId deviceId, McastRole role) {
        return mcastRoleStore.entrySet().stream()
                .filter(entry -> entry.getKey().mcastIp().equals(mcastIp) &&
                        entry.getKey().deviceId().equals(deviceId) && entry.getValue().value() == role)
                .map(Entry::getKey).map(McastRoleStoreKey::source).collect(Collectors.toSet());
    }

    /**
     * Gets source(s) of given multicast group.
     *
     * @param mcastIp multicast IP
     * @return set of device ID or empty set if not found
     */
    private Set<ConnectPoint> getSources(IpAddress mcastIp) {
        return mcastRoleStore.entrySet().stream()
                .filter(entry -> entry.getKey().mcastIp().equals(mcastIp))
                .map(Entry::getKey).map(McastRoleStoreKey::source).collect(Collectors.toSet());
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
                .map(Entry::getKey).map(McastStoreKey::mcastIp).collect(Collectors.toSet());
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
     * @param ingressDevice the ingress device
     * @param source the source connect point
     * @return spine-facing port on ingress device
     */
    private Set<PortNumber> ingressTransitPort(IpAddress mcastIp, DeviceId ingressDevice,
                                               ConnectPoint source) {
        ImmutableSet.Builder<PortNumber> portBuilder = ImmutableSet.builder();
        if (ingressDevice != null) {
            Versioned<NextObjective> nextObjVers = mcastNextObjStore.get(new McastStoreKey(mcastIp, ingressDevice,
                                                                          mcastUtils.assignedVlan(source)));
            if (nextObjVers == null) {
                log.warn("Absent next objective for {}", new McastStoreKey(mcastIp, ingressDevice,
                        mcastUtils.assignedVlan(source)));
                return portBuilder.build();
            }
            NextObjective nextObj = nextObjVers.value();
            Set<PortNumber> ports = mcastUtils.getPorts(nextObj.next());
            // Let's find out all the ingress-transit ports
            for (PortNumber port : ports) {
                // Spine-facing port should have no subnet and no xconnect
                if (srManager.deviceConfiguration() != null &&
                        srManager.deviceConfiguration().getPortSubnets(ingressDevice, port).isEmpty() &&
                        (srManager.xconnectService == null ||
                        !srManager.xconnectService.hasXconnect(new ConnectPoint(ingressDevice, port)))) {
                    portBuilder.add(port);
                }
            }
        }
        return portBuilder.build();
    }

    /**
     * Verify if a given connect point is sink for this group.
     *
     * @param mcastIp group address
     * @param connectPoint connect point to be verified
     * @param source source connect point
     * @return true if the connect point is sink of the group
     */
    private boolean isSinkForGroup(IpAddress mcastIp, ConnectPoint connectPoint,
                                   ConnectPoint source) {
        VlanId assignedVlan = mcastUtils.assignedVlan(connectPoint.deviceId().equals(source.deviceId()) ?
                                                              source : null);
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, connectPoint.deviceId(), assignedVlan);
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            return false;
        }
        NextObjective mcastNext = mcastNextObjStore.get(mcastStoreKey).value();
        return mcastUtils.getPorts(mcastNext.next()).contains(connectPoint.port());
    }

    /**
     * Verify if a given connect point is sink for this group and for this source.
     *
     * @param mcastIp group address
     * @param connectPoint connect point to be verified
     * @param source source connect point
     * @return true if the connect point is sink of the group
     */
    private boolean isSinkForSource(IpAddress mcastIp, ConnectPoint connectPoint,
                                    ConnectPoint source) {
        boolean isSink = isSinkForGroup(mcastIp, connectPoint, source);
        DeviceId device;
        if (connectPoint.deviceId().equals(source.deviceId())) {
            device = getDevice(mcastIp, INGRESS, source).stream()
                    .filter(deviceId -> deviceId.equals(connectPoint.deviceId()))
                    .findFirst().orElse(null);
        } else {
            device = getDevice(mcastIp, EGRESS, source).stream()
                    .filter(deviceId -> deviceId.equals(connectPoint.deviceId()))
                    .findFirst().orElse(null);
        }
        return isSink && device != null;
    }

    /**
     * Verify if a sink is reachable from this source.
     *
     * @param mcastIp group address
     * @param sink connect point to be verified
     * @param source source connect point
     * @return true if the connect point is reachable from the source
     */
    private boolean isSinkReachable(IpAddress mcastIp, ConnectPoint sink,
                                    ConnectPoint source) {
        return sink.deviceId().equals(source.deviceId()) ||
                getPath(source.deviceId(), sink.deviceId(), mcastIp, null, source).isPresent();
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
        mcastWorker.execute(() -> updateFilterToDeviceInternal(deviceId, portNum, vlanId, install));
    }

    private void updateFilterToDeviceInternal(DeviceId deviceId, PortNumber portNum,
                                              VlanId vlanId, boolean install) {
        lastMcastChange.set(Instant.now());
        // Iterates over the route and updates properly the filtering objective on the source device.
        srManager.multicastRouteService.getRoutes().forEach(mcastRoute -> {
            log.debug("Update filter for {}", mcastRoute.group());
            if (!mcastUtils.isLeader(mcastRoute.group())) {
                log.debug("Skip {} due to lack of leadership", mcastRoute.group());
                return;
            }
            // Get the sources and for each one update properly the filtering objectives
            Set<ConnectPoint> sources = srManager.multicastRouteService.sources(mcastRoute);
            sources.forEach(source -> {
                if (source.deviceId().equals(deviceId) && source.port().equals(portNum)) {
                    if (install) {
                        McastFilteringObjStoreKey mcastFilterObjStoreKey = new McastFilteringObjStoreKey(source,
                                                                             vlanId, mcastRoute.group().isIp4());
                        addFilterToDevice(mcastFilterObjStoreKey, mcastRoute.group(), INGRESS);
                    } else {
                        mcastUtils.removeFilterToDevice(deviceId, portNum, vlanId, mcastRoute.group(), null);
                    }
                }
            });
        });
    }

    /**
     * Add filtering to the device if needed.
     *
     * @param filterObjStoreKey the filtering obj key
     * @param mcastIp the multicast group
     * @param mcastRole the multicast role
     */
    private void addFilterToDevice(McastFilteringObjStoreKey filterObjStoreKey,
                                   IpAddress mcastIp,
                                   McastRole mcastRole) {
        if (!containsFilterInTheDevice(filterObjStoreKey)) {
            // if this is the first sink for this group/device
            // match additionally on mac
            log.debug("Filtering not available for device {}, vlan {} and {}",
                      filterObjStoreKey.ingressCP().deviceId(), filterObjStoreKey.vlanId(),
                      filterObjStoreKey.isIpv4() ? "IPv4" : "IPv6");
            mcastUtils.addFilterToDevice(filterObjStoreKey.ingressCP().deviceId(),
                                         filterObjStoreKey.ingressCP().port(),
                                         filterObjStoreKey.vlanId(), mcastIp,
                                         mcastRole, true);
            mcastFilteringObjStore.add(filterObjStoreKey);
        } else if (!mcastFilteringObjStore.contains(filterObjStoreKey)) {
            // match only vlan
            log.debug("Filtering not available for connect point {}, vlan {} and {}",
                      filterObjStoreKey.ingressCP(), filterObjStoreKey.vlanId(),
                      filterObjStoreKey.isIpv4() ? "IPv4" : "IPv6");
            mcastUtils.addFilterToDevice(filterObjStoreKey.ingressCP().deviceId(),
                                         filterObjStoreKey.ingressCP().port(),
                                         filterObjStoreKey.vlanId(), mcastIp,
                                         mcastRole, false);
            mcastFilteringObjStore.add(filterObjStoreKey);
        } else {
            // do nothing
            log.debug("Filtering already present. Abort");
        }
    }

    /**
     * Verify if there are related filtering obj in the device.
     *
     * @param filteringKey the filtering obj key
     * @return true if related filtering obj are found
     */
    private boolean containsFilterInTheDevice(McastFilteringObjStoreKey filteringKey) {
        // check if filters are already added on the device
        McastFilteringObjStoreKey key = mcastFilteringObjStore.stream()
                .filter(mcastFilteringKey ->
                                mcastFilteringKey.ingressCP().deviceId().equals(filteringKey.ingressCP().deviceId())
                                        && mcastFilteringKey.isIpv4() == filteringKey.isIpv4()
                                        && mcastFilteringKey.vlanId().equals(filteringKey.vlanId())
                ).findFirst().orElse(null);
        // we are interested to filt obj on the same device, same vlan and same ip type
        return key != null;
    }

    /**
     * Update the filtering objective store upon device failure.
     *
     * @param affectedDevice the affected device
     */
    private void updateFilterObjStoreByDevice(DeviceId affectedDevice) {
        // purge the related filter objective key
        Set<McastFilteringObjStoreKey> filterObjs = Sets.newHashSet(mcastFilteringObjStore);
        Iterator<McastFilteringObjStoreKey> filterIterator = filterObjs.iterator();
        McastFilteringObjStoreKey filterKey;
        while (filterIterator.hasNext()) {
            filterKey = filterIterator.next();
            if (filterKey.ingressCP().deviceId().equals(affectedDevice)) {
                mcastFilteringObjStore.remove(filterKey);
            }
        }
    }

    /**
     * Update the filtering objective store upon port failure.
     *
     * @param affectedPort the affected port
     */
    private void updateFilterObjStoreByPort(ConnectPoint affectedPort) {
        // purge the related filter objective key
        Set<McastFilteringObjStoreKey> filterObjs = Sets.newHashSet(mcastFilteringObjStore);
        Iterator<McastFilteringObjStoreKey> filterIterator = filterObjs.iterator();
        McastFilteringObjStoreKey filterKey;
        while (filterIterator.hasNext()) {
            filterKey = filterIterator.next();
            if (filterKey.ingressCP().equals(affectedPort)) {
                mcastFilteringObjStore.remove(filterKey);
            }
        }
    }

    /**
     * Performs bucket verification operation for all mcast groups in the devices.
     * Firstly, it verifies that mcast is stable before trying verification operation.
     * Verification consists in creating new nexts with VERIFY operation. Actually,
     * the operation is totally delegated to the driver.
     */
    private final class McastBucketCorrector implements Runnable {
        private final AtomicInteger verifyOnFlight = new AtomicInteger(0);
        // Define the context used for the back pressure mechanism
        private final ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> {
                    synchronized (verifyOnFlight) {
                        log.trace("Verify {} done", objective.id());
                        verifyOnFlight.updateAndGet(i -> i > 0 ? i - 1 : i);
                        verifyOnFlight.notify();
                    }
                },
                (objective, error) -> {
                    synchronized (verifyOnFlight) {
                        log.trace("Verify {} error {}", objective.id(), error);
                        verifyOnFlight.updateAndGet(i -> i > 0 ? i - 1 : i);
                        verifyOnFlight.notify();
                    }
                });

        @Override
        public void run() {
            try {
                // Iterates over the routes and verify the related next objectives
                for (McastRoute mcastRoute : srManager.multicastRouteService.getRoutes()) {
                    if (!isMcastStable() || wasBktCorrRunning()) {
                        return;
                    }
                    IpAddress mcastIp = mcastRoute.group();
                    log.trace("Running mcast buckets corrector for mcast group: {}", mcastIp);
                    // Verify leadership on the operation
                    if (!mcastUtils.isLeader(mcastIp)) {
                        log.trace("Skip {} due to lack of leadership", mcastIp);
                        continue;
                    }
                    // Get sources and sinks from Mcast Route Service and warn about errors
                    Set<ConnectPoint> sources = mcastUtils.getSources(mcastIp);
                    Set<ConnectPoint> sinks = mcastUtils.getSinks(mcastIp).values().stream()
                            .flatMap(Collection::stream).collect(Collectors.toSet());
                    // Do not proceed if sources of this group are missing
                    if (sources.isEmpty()) {
                        if (!sinks.isEmpty()) {
                            log.warn("Unable to run buckets corrector. " +
                                     "Missing source {} for group {}", sources, mcastIp);
                        }
                        continue;
                    }
                    // For each group we get current information in the store
                    // and issue a check of the next objectives in place
                    Set<McastStoreKey> processedKeys = Sets.newHashSet();
                    for (ConnectPoint source : sources) {
                        Set<DeviceId> ingressDevices = getDevice(mcastIp, INGRESS, source);
                        Set<DeviceId> transitDevices = getDevice(mcastIp, TRANSIT, source);
                        Set<DeviceId> egressDevices = getDevice(mcastIp, EGRESS, source);
                        // Do not proceed if ingress devices are missing
                        if (ingressDevices.isEmpty()) {
                            if (!sinks.isEmpty()) {
                                log.warn("Unable to run buckets corrector. " +
                                "Missing ingress {} for source {} and for group {}",
                                         ingressDevices, source, mcastIp);
                            }
                            continue;
                        }
                        // Create the set of the devices to be processed
                        ImmutableSet.Builder<DeviceId> devicesBuilder = ImmutableSet.builder();
                        devicesBuilder.addAll(ingressDevices);
                        if (!transitDevices.isEmpty()) {
                            devicesBuilder.addAll(transitDevices);
                        }
                        if (!egressDevices.isEmpty()) {
                            devicesBuilder.addAll(egressDevices);
                        }
                        Set<DeviceId> devicesToProcess = devicesBuilder.build();
                        for (DeviceId deviceId : devicesToProcess) {
                            synchronized (verifyOnFlight) {
                                while (verifyOnFlight.get() == MAX_VERIFY_ON_FLIGHT) {
                                    verifyOnFlight.wait();
                                }
                            }
                            VlanId assignedVlan = mcastUtils.assignedVlan(deviceId.equals(source.deviceId()) ?
                                                                                  source : null);
                            McastStoreKey currentKey = new McastStoreKey(mcastIp, deviceId, assignedVlan);
                            // Check if we already processed this next - trees merge at some point
                            if (processedKeys.contains(currentKey)) {
                                continue;
                            }
                            // Verify the nextobjective or skip to next device
                            if (mcastNextObjStore.containsKey(currentKey)) {
                                NextObjective currentNext = mcastNextObjStore.get(currentKey).value();
                                // Rebuild the next objective using assigned vlan
                                currentNext = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                            mcastUtils.getPorts(currentNext.next()), currentNext.id()).verify(context);
                                // Send to the flowobjective service
                                srManager.flowObjectiveService.next(deviceId, currentNext);
                                verifyOnFlight.incrementAndGet();
                                log.trace("Verify on flight {}", verifyOnFlight);
                                processedKeys.add(currentKey);
                            } else {
                                log.warn("Unable to run buckets corrector. " +
                                         "Missing next for {}, for source {} and for group {}",
                                         deviceId, source, mcastIp);
                            }
                        }
                    }
                    // Let's wait the group before start the next one
                    synchronized (verifyOnFlight) {
                        while (verifyOnFlight.get() > 0) {
                            verifyOnFlight.wait();
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.warn("BktCorr has been interrupted");
            } finally {
                lastBktCorrExecution.set(Instant.now());
            }
        }
    }

    /**
     * Returns the associated next ids to the mcast groups or to the single
     * group if mcastIp is present.
     *
     * @param mcastIp the group ip
     * @return the mapping mcastIp-device to next id
     */
    public Map<McastStoreKey, Integer> getNextIds(IpAddress mcastIp) {
        if (mcastIp != null) {
            return mcastNextObjStore.entrySet().stream()
                    .filter(mcastEntry -> mcastIp.equals(mcastEntry.getKey().mcastIp()))
                    .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().value().id()));
        }
        return mcastNextObjStore.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().value().id()));
    }

    /**
     * Removes given next ID from mcast next id store.
     *
     * @param nextId next id
     */
    public void removeNextId(int nextId) {
        mcastNextObjStore.entrySet().forEach(e -> {
            if (e.getValue().value().id() == nextId) {
                mcastNextObjStore.remove(e.getKey());
            }
        });
    }

    /**
     * Returns the associated roles to the mcast groups.
     *
     * @param mcastIp the group ip
     * @param sourcecp the source connect point
     * @return the mapping mcastIp-device to mcast role
     */
    public Map<McastRoleStoreKey, McastRole> getMcastRoles(IpAddress mcastIp,
                                                       ConnectPoint sourcecp) {
        if (mcastIp != null) {
            Map<McastRoleStoreKey, McastRole> roles = mcastRoleStore.entrySet().stream()
                    .filter(mcastEntry -> mcastIp.equals(mcastEntry.getKey().mcastIp()))
                    .collect(Collectors.toMap(entry -> new McastRoleStoreKey(entry.getKey().mcastIp(),
                     entry.getKey().deviceId(), entry.getKey().source()), entry -> entry.getValue().value()));
            if (sourcecp != null) {
                roles = roles.entrySet().stream()
                        .filter(mcastEntry -> sourcecp.equals(mcastEntry.getKey().source()))
                        .collect(Collectors.toMap(entry -> new McastRoleStoreKey(entry.getKey().mcastIp(),
                         entry.getKey().deviceId(), entry.getKey().source()), Entry::getValue));
            }
            return roles;
        }
        return mcastRoleStore.entrySet().stream()
                .collect(Collectors.toMap(entry -> new McastRoleStoreKey(entry.getKey().mcastIp(),
                 entry.getKey().deviceId(), entry.getKey().source()), entry -> entry.getValue().value()));
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
        Set<ConnectPoint> sources = mcastUtils.getSources(mcastIp);
        if (sourcecp != null) {
            sources = sources.stream()
                    .filter(source -> source.equals(sourcecp)).collect(Collectors.toSet());
        }
        if (!sources.isEmpty()) {
            sources.forEach(source -> {
                Map<ConnectPoint, List<ConnectPoint>> mcastPaths = Maps.newHashMap();
                Set<DeviceId> visited = Sets.newHashSet();
                List<ConnectPoint> currentPath = Lists.newArrayList(source);
                mcastUtils.buildMcastPaths(mcastNextObjStore.asJavaMap(), source.deviceId(), visited, mcastPaths,
                        currentPath, mcastIp, source);
                mcastPaths.forEach(mcastTrees::put);
            });
        }
        return mcastTrees;
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

    /**
     * Returns the mcast filtering obj.
     *
     * @return the mapping group-node
     */
    public Map<DeviceId, List<McastFilteringObjStoreKey>> getMcastFilters() {
        Map<DeviceId, List<McastFilteringObjStoreKey>> mapping = Maps.newHashMap();
        Set<McastFilteringObjStoreKey> currentKeys = Sets.newHashSet(mcastFilteringObjStore);
        currentKeys.forEach(filteringObjStoreKey ->
            mapping.compute(filteringObjStoreKey.ingressCP().deviceId(), (k, v) -> {
                List<McastFilteringObjStoreKey> values = v;
                if (values == null) {
                    values = Lists.newArrayList();
                }
                values.add(filteringObjStoreKey);
                return values;
            })
        );
        return mapping;
    }
}
