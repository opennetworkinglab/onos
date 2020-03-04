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
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMultimap;
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
    private final McastUtils mcastUtils;
    private final ConsistentMap<McastStoreKey, NextObjective> mcastNextObjStore;
    private final ConsistentMap<McastRoleStoreKey, McastRole> mcastRoleStore;
    private final ConsistentMultimap<McastPathStoreKey, List<Link>> mcastPathStore;
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
        mcastKryo = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(new McastPathStoreKeySerializer(), McastPathStoreKey.class);
        mcastPathStore = srManager.storageService
                .<McastPathStoreKey, List<Link>>consistentMultimapBuilder()
                .withName("onos-mcast-path-store")
                .withSerializer(Serializer.using(mcastKryo.build("McastHandler-Path")))
                .build();
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
        srManager.multicastRouteService.getRoutes().forEach(mcastRoute -> {
            lastMcastChange.set(Instant.now());
            log.debug("Init group {}", mcastRoute.group());
            if (!mcastUtils.isLeader(mcastRoute.group())) {
                log.debug("Skip {} due to lack of leadership", mcastRoute.group());
                return;
            }
            McastRouteData mcastRouteData = srManager.multicastRouteService.routeData(mcastRoute);
            // For each source process the mcast tree
            srManager.multicastRouteService.sources(mcastRoute).forEach(source -> {
                McastPathStoreKey pathStoreKey = new McastPathStoreKey(mcastRoute.group(), source);
                Collection<? extends List<Link>> storedPaths = Versioned.valueOrElse(
                        mcastPathStore.get(pathStoreKey), Lists.newArrayList());
                Map<ConnectPoint, List<ConnectPoint>> mcastPaths = buildMcastPaths(storedPaths, mcastRoute.group(),
                                                                                   source);
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
                Map<ConnectPoint, List<Path>> mcasTree = mcastUtils.computeSinkMcastTree(mcastRoute.group(),
                                                                                         source.deviceId(), sinks);
                mcasTree.forEach((sink, paths) -> processSinkAddedInternal(source, sink, mcastRoute.group(),
                                                                             null));
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
        mcastPathStore.destroy();
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
            Map<ConnectPoint, List<Path>> mcasTree = mcastUtils.computeSinkMcastTree(mcastIp, source.deviceId(),
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
        // Let's heal the trees
        Set<Link> notAffectedLinks = Sets.newHashSet();
        Map<ConnectPoint, Set<Link>> affectedLinks = Maps.newHashMap();
        Map<ConnectPoint, Set<ConnectPoint>> candidateSinks = Maps.newHashMap();
        Set<ConnectPoint> totalSources = Sets.newHashSet(sourcesToBeRemoved);
        totalSources.addAll(remainingSources);
        // Calculate all the links used by the sources and the current sinks
        totalSources.forEach(source -> {
            Set<ConnectPoint> currentSinks = sinks.values()
                    .stream().flatMap(Collection::stream)
                    .filter(sink -> isSinkForSource(mcastIp, sink, source))
                    .collect(Collectors.toSet());
            candidateSinks.put(source, currentSinks);
            McastPathStoreKey pathStoreKey = new McastPathStoreKey(mcastIp, source);
            Collection<? extends List<Link>> storedPaths = Versioned.valueOrElse(
                    mcastPathStore.get(pathStoreKey), Lists.newArrayList());
            currentSinks.forEach(currentSink -> {
                Optional<? extends List<Link>> currentPath = mcastUtils.getStoredPath(currentSink.deviceId(),
                                                                                      storedPaths);
                if (currentPath.isPresent()) {
                    if (!sourcesToBeRemoved.contains(source)) {
                        notAffectedLinks.addAll(currentPath.get());
                    } else {
                        affectedLinks.compute(source, (k, v) -> {
                           v = v == null ? Sets.newHashSet() : v;
                           v.addAll(currentPath.get());
                           return v;
                        });
                    }
                }
            });
        });
        // Clean transit links
        affectedLinks.forEach((source, currentCandidateLinks) -> {
            Set<Link> linksToBeRemoved = Sets.difference(currentCandidateLinks, notAffectedLinks)
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
        sourcesToBeRemoved.forEach(source -> {
            Set<ConnectPoint> currentSinks = candidateSinks.get(source);
            McastPathStoreKey pathStoreKey = new McastPathStoreKey(mcastIp, source);
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
            // Clean the mcast paths
            mcastPathStore.removeAll(pathStoreKey);
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
            McastPathStoreKey pathStoreKey = new McastPathStoreKey(mcastIp, source);
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
            // Clean the mcast paths
            mcastPathStore.removeAll(pathStoreKey);
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
        Map<ConnectPoint, Map<ConnectPoint, Optional<? extends List<Link>>>> treesToBeRemoved = Maps.newHashMap();
        Map<ConnectPoint, Set<ConnectPoint>> treesToBeAdded = Maps.newHashMap();
        Set<Link> goodLinks = Sets.newHashSet();
        Map<ConnectPoint, Set<DeviceId>> goodDevicesBySource = Maps.newHashMap();
        sources.forEach(source -> {
            // Save the path associated to the sinks to be removed
            Set<ConnectPoint> sinksToBeRemoved = processSinksToBeRemoved(mcastIp, prevSinks,
                                                                         newSinks, source);
            Map<ConnectPoint, Optional<? extends List<Link>>> treeToBeRemoved = Maps.newHashMap();
            McastPathStoreKey pathStoreKey = new McastPathStoreKey(mcastIp, source);
            Collection<? extends List<Link>> storedPaths = Versioned.valueOrElse(
                    mcastPathStore.get(pathStoreKey), Lists.newArrayList());
            sinksToBeRemoved.forEach(sink -> treeToBeRemoved.put(sink, mcastUtils.getStoredPath(sink.deviceId(),
                                                                                                storedPaths)));
            treesToBeRemoved.put(source, treeToBeRemoved);
            // Save the good links and good devices
            Set<DeviceId> goodDevices = Sets.newHashSet();
            Set<DeviceId> totalDevices = Sets.newHashSet(getDevice(mcastIp, EGRESS, source));
            totalDevices.addAll(getDevice(mcastIp, INGRESS, source));
            Set<ConnectPoint> notAffectedSinks = Sets.newHashSet();
            // Compute good sinks
            totalDevices.forEach(device -> {
                Set<ConnectPoint> sinks = getSinks(mcastIp, device, source);
                notAffectedSinks.addAll(Sets.difference(sinks, sinksToBeRemoved));
            });
            // Compute good paths and good devices
            notAffectedSinks.forEach(notAffectedSink -> {
                Optional<? extends List<Link>> notAffectedPath = mcastUtils.getStoredPath(notAffectedSink.deviceId(),
                                                                                          storedPaths);
                if (notAffectedPath.isPresent()) {
                    List<Link> goodPath = notAffectedPath.get();
                    goodLinks.addAll(goodPath);
                    goodPath.forEach(link -> goodDevices.add(link.src().deviceId()));
                } else {
                    goodDevices.add(notAffectedSink.deviceId());
                }
            });
            goodDevicesBySource.compute(source, (k, v) -> {
                v = v == null ? Sets.newHashSet() : v;
                v.addAll(goodDevices);
                return v;
            });
            // Recover the dual-homed sinks
            Set<ConnectPoint> sinksToBeRecovered = processSinksToBeRecovered(mcastIp, newSinks,
                                                                             prevSinks, source);
            treesToBeAdded.put(source, sinksToBeRecovered);
        });
        // Remove the sinks taking into account the multiple sources and the original paths
        treesToBeRemoved.forEach((source, tree) ->
            tree.forEach((sink, path) -> processSinkRemovedInternal(source, sink, mcastIp, path,
                                                                    goodLinks, goodDevicesBySource.get(source))));
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
     * @param usedLinks links used by the other sinks
     * @param usedDevices devices used by other sinks
     */
    private void processSinkRemovedInternal(ConnectPoint source, ConnectPoint sink,
                                            IpAddress mcastIp, Optional<? extends List<Link>> mcastPath,
                                            Set<Link> usedLinks, Set<DeviceId> usedDevices) {

        log.info("Used links {}", usedLinks);
        log.info("Used devices {}", usedDevices);

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
            List<Link> links = Lists.newArrayList(mcastPath.get());
            if (isLast) {
                // Clean the path
                McastPathStoreKey pathStoreKey = new McastPathStoreKey(mcastIp, source);
                mcastPathStore.remove(pathStoreKey, mcastPath.get());
                Collections.reverse(links);
                for (Link link : links) {
                    // If nobody is using the port remove
                    if (!usedLinks.contains(link)) {
                        removePortFromDevice(link.src().deviceId(), link.src().port(), mcastIp,
                          mcastUtils.assignedVlan(link.src().deviceId().equals(source.deviceId()) ? source : null));
                    }
                    // If nobody is using the device
                    if (!usedDevices.contains(link.src().deviceId())) {
                        mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, link.src().deviceId(), source));
                    }
                }
            }
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
        Optional<Path> mcastPath = getPath(source.deviceId(), sink.deviceId(), mcastIp, allPaths);
        if (mcastPath.isPresent()) {
            List<Link> links = mcastPath.get().links();
            McastPathStoreKey pathStoreKey = new McastPathStoreKey(mcastIp, source);
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
                    .forEach(link -> mcastRoleStore.put(new McastRoleStoreKey(mcastIp, link.dst().deviceId(),
                                                                              source), TRANSIT));
            // Process the egress device
            addPortToDevice(sink.deviceId(), sink.port(), mcastIp, mcastUtils.assignedVlan(null));
            // Setup mcast role for egress
            mcastRoleStore.put(new McastRoleStoreKey(mcastIp, sink.deviceId(), source), EGRESS);
            // Store the used path
            mcastPathStore.put(pathStoreKey, links);
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
        // Get mcast groups affected by the link going down
        Set<IpAddress> affectedGroups = getAffectedGroups(linkDown);
        log.info("Processing link down {} for groups {}", linkDown, affectedGroups);
        affectedGroups.forEach(mcastIp -> {
            lastMcastChange.set(Instant.now());
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
        // Get the mcast groups affected by the device going down
        Set<IpAddress> affectedGroups = getAffectedGroups(deviceDown);
        log.info("Processing device down {} for groups {}", deviceDown, affectedGroups);
        updateFilterObjStoreByDevice(deviceDown);
        affectedGroups.forEach(mcastIp -> {
            lastMcastChange.set(Instant.now());
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
        // Do not proceed if we are not the leaders
        if (!mcastUtils.isLeader(mcastIp)) {
            log.debug("Skip {} due to lack of leadership", mcastIp);
            return;
        }
        // Skip if it is not an infra failure
        Set<DeviceId> transitDevices = getDevice(mcastIp, TRANSIT);
        if (!mcastUtils.isInfraFailure(transitDevices, failedElement)) {
            log.debug("Skip {} not an infrastructure failure", mcastIp);
            return;
        }
        // Do not proceed if the sources of this group are missing
        Set<ConnectPoint> sources = getSources(mcastIp);
        if (sources.isEmpty()) {
            log.warn("Missing sources for group {}", mcastIp);
            return;
        }
        // Get all the paths, affected paths, good links and good devices
        Set<List<Link>> storedPaths = getStoredPaths(mcastIp);
        Set<List<Link>> affectedPaths = mcastUtils.getAffectedPaths(storedPaths, failedElement);
        Set<Link> goodLinks = Sets.newHashSet();
        Map<DeviceId, Set<DeviceId>> goodDevicesBySource = Maps.newHashMap();
        Map<DeviceId, Set<ConnectPoint>> processedSourcesByEgress = Maps.newHashMap();
        Sets.difference(storedPaths, affectedPaths).forEach(goodPath -> {
            goodLinks.addAll(goodPath);
            DeviceId srcDevice = goodPath.get(0).src().deviceId();
            Set<DeviceId> goodDevices = Sets.newHashSet();
            goodPath.forEach(link -> goodDevices.add(link.src().deviceId()));
            goodDevicesBySource.compute(srcDevice, (k, v) -> {
                v = v == null ? Sets.newHashSet() : v;
                v.addAll(goodDevices);
                return v;
            });
        });
        affectedPaths.forEach(affectedPath -> {
            // TODO remove
            log.info("Good links {}", goodLinks);
            // TODO remove
            log.info("Good devices {}", goodDevicesBySource);
            // TODO trace
            log.info("Healing the path {}", affectedPath);
            DeviceId srcDevice = affectedPath.get(0).src().deviceId();
            DeviceId dstDevice = affectedPath.get(affectedPath.size() - 1).dst().deviceId();
            // Fix in one shot multiple sources
            Set<ConnectPoint> affectedSources = sources.stream()
                    .filter(device -> device.deviceId().equals(srcDevice))
                    .collect(Collectors.toSet());
            Set<ConnectPoint> processedSources = processedSourcesByEgress.getOrDefault(dstDevice,
                                                                                       Collections.emptySet());
            Optional<Path> alternativePath = getPath(srcDevice, dstDevice, mcastIp, null);
            // If an alternative is possible go ahead
            if (alternativePath.isPresent()) {
                // TODO trace
                log.info("Alternative path {}", alternativePath.get().links());
            } else {
                // Otherwise try to come up with an alternative
                // TODO trace
                log.info("No alternative path");
                Set<ConnectPoint> notAffectedSources = Sets.difference(sources, affectedSources);
                Set<ConnectPoint> remainingSources = Sets.difference(notAffectedSources, processedSources);
                alternativePath = recoverSinks(dstDevice, mcastIp, affectedSources, remainingSources);
                processedSourcesByEgress.compute(dstDevice, (k, v) -> {
                    v = v == null ? Sets.newHashSet() : v;
                    v.addAll(affectedSources);
                    return v;
                });
            }
            // Recover from the failure if possible
            Optional<Path> finalPath = alternativePath;
            affectedSources.forEach(affectedSource -> {
                // Update the mcastPath store
                McastPathStoreKey mcastPathStoreKey = new McastPathStoreKey(mcastIp, affectedSource);
                // Verify if there are local sinks
                Set<DeviceId> localSinks = getSinks(mcastIp, srcDevice, affectedSource).stream()
                        .map(ConnectPoint::deviceId)
                        .collect(Collectors.toSet());
                Set<DeviceId> goodDevices = goodDevicesBySource.compute(affectedSource.deviceId(), (k, v) -> {
                    v = v == null ? Sets.newHashSet() : v;
                    v.addAll(localSinks);
                    return v;
                });
                // TODO remove
                log.info("Good devices {}", goodDevicesBySource);
                Collection<? extends List<Link>> storedPathsBySource = Versioned.valueOrElse(
                        mcastPathStore.get(mcastPathStoreKey), Lists.newArrayList());
                Optional<? extends List<Link>> storedPath = storedPathsBySource.stream()
                        .filter(path -> path.equals(affectedPath))
                        .findFirst();
                // Remove bad links
                affectedPath.forEach(affectedLink -> {
                    DeviceId affectedDevice = affectedLink.src().deviceId();
                    // If there is overlap with good paths - skip it
                    if (!goodLinks.contains(affectedLink)) {
                        removePortFromDevice(affectedDevice, affectedLink.src().port(), mcastIp,
                            mcastUtils.assignedVlan(affectedDevice.equals(affectedSource.deviceId()) ?
                                                            affectedSource : null));
                    }
                    // Remove role on the affected links if last
                    if (!goodDevices.contains(affectedDevice)) {
                        mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, affectedDevice, affectedSource));
                    }
                });
                // Sometimes the removal fails for serialization issue
                // trying with the original object as workaround
                if (storedPath.isPresent()) {
                    mcastPathStore.remove(mcastPathStoreKey, storedPath.get());
                } else {
                    log.warn("Unable to find the corresponding path - trying removeal");
                    mcastPathStore.remove(mcastPathStoreKey, affectedPath);
                }
                // Program new links
                if (finalPath.isPresent()) {
                    List<Link> links = finalPath.get().links();
                    installPath(mcastIp, affectedSource, links);
                    mcastPathStore.put(mcastPathStoreKey, links);
                    links.forEach(link -> goodDevices.add(link.src().deviceId()));
                    goodDevicesBySource.compute(srcDevice, (k, v) -> {
                        v = v == null ? Sets.newHashSet() : v;
                        v.addAll(goodDevices);
                        return v;
                    });
                    goodLinks.addAll(finalPath.get().links());
                }
            });
        });
    }

    /**
     * Try to recover sinks using alternative locations.
     *
     * @param notRecovered the device not recovered
     * @param mcastIp the group address
     * @param affectedSources affected sources
     * @param goodSources sources not affected
     */
    private Optional<Path> recoverSinks(DeviceId notRecovered, IpAddress mcastIp,
                                    Set<ConnectPoint> affectedSources,
                                    Set<ConnectPoint> goodSources) {
        log.debug("Processing recover sinks on {} for group {}", notRecovered, mcastIp);
        Map<ConnectPoint, Set<ConnectPoint>> affectedSinksBySource = Maps.newHashMap();
        Map<ConnectPoint, Set<ConnectPoint>> sinksBySource = Maps.newHashMap();
        Set<ConnectPoint> sources = Sets.union(affectedSources, goodSources);
        // Hosts influenced by the failure
        Map<HostId, Set<ConnectPoint>> hostIdSetMap = mcastUtils.getAffectedSinks(notRecovered, mcastIp);
        // Locations influenced by the failure
        Set<ConnectPoint> affectedSinks = hostIdSetMap.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(connectPoint -> connectPoint.deviceId().equals(notRecovered))
                .collect(Collectors.toSet());
        // All locations
        Set<ConnectPoint> sinks = hostIdSetMap.values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        // Maps sinks with the sources
        sources.forEach(source -> {
            Set<ConnectPoint> currentSinks = affectedSinks.stream()
                    .filter(sink -> isSinkForSource(mcastIp, sink, source))
                    .collect(Collectors.toSet());
            affectedSinksBySource.put(source, currentSinks);
        });
        // Remove sinks one by one if they are not used by other sources
        affectedSources.forEach(affectedSource -> {
            Set<ConnectPoint> currentSinks = affectedSinksBySource.get(affectedSource);
            log.info("Current sinks {} for source {}", currentSinks, affectedSource);
            currentSinks.forEach(currentSink -> {
                VlanId assignedVlan = mcastUtils.assignedVlan(
                        affectedSource.deviceId().equals(currentSink.deviceId()) ? affectedSource : null);
                log.info("Assigned vlan {}", assignedVlan);
                Set<VlanId> otherVlans = goodSources.stream()
                        .filter(remainingSource -> affectedSinksBySource.get(remainingSource).contains(currentSink))
                        .map(remainingSource -> mcastUtils.assignedVlan(
                                remainingSource.deviceId().equals(currentSink.deviceId()) ? remainingSource : null))
                        .collect(Collectors.toSet());
                log.info("Other vlans {}", otherVlans);
                // Sinks on other leaves
                if (!otherVlans.contains(assignedVlan)) {
                    removePortFromDevice(currentSink.deviceId(), currentSink.port(), mcastIp, assignedVlan);
                }
                mcastRoleStore.remove(new McastRoleStoreKey(mcastIp, currentSink.deviceId(), affectedSource));
            });
        });
        // Get the sinks to be added and the new egress
        Set<DeviceId> newEgress = Sets.newHashSet();
        affectedSources.forEach(affectedSource -> {
            Set<ConnectPoint> currentSinks = affectedSinksBySource.get(affectedSource);
            Set<ConnectPoint> newSinks = Sets.difference(sinks, currentSinks);
            sinksBySource.put(affectedSource, newSinks);
            newSinks.stream()
                    .map(ConnectPoint::deviceId)
                    .forEach(newEgress::add);
        });
        log.info("newEgress {}", newEgress);
        // If there are more than one new egresses, return the problem
        if (newEgress.size() != 1) {
            log.warn("There are {} new egress, wrong configuration. Abort.", newEgress.size());
            return Optional.empty();
        }
        DeviceId egress = newEgress.stream()
                .findFirst()
                .orElse(null);
        DeviceId ingress = affectedSources.stream()
                .map(ConnectPoint::deviceId)
                .findFirst()
                .orElse(null);
        log.info("Ingress {}", ingress);
        if (ingress == null) {
            log.warn("No new ingress, wrong configuration. Abort.");
            return Optional.empty();
        }
        // Get an alternative path
        Optional<Path> alternativePath = getPath(ingress, egress, mcastIp, null);
        // If there are new path install sinks and return path
        if (alternativePath.isPresent()) {
            log.info("Alternative path {}", alternativePath.get().links());
            affectedSources.forEach(affectedSource -> {
                Set<ConnectPoint> newSinks = sinksBySource.get(affectedSource);
                newSinks.forEach(newSink -> {
                    addPortToDevice(newSink.deviceId(), newSink.port(), mcastIp, mcastUtils.assignedVlan(null));
                    mcastRoleStore.put(new McastRoleStoreKey(mcastIp, newSink.deviceId(), affectedSource), EGRESS);
                });
            });
            return alternativePath;
        }
        // No new path but sinks co-located with sources install sinks and return empty
        if (ingress.equals(egress)) {
            log.info("No Alternative path but sinks co-located");
            affectedSources.forEach(affectedSource -> {
                Set<ConnectPoint> newSinks = sinksBySource.get(affectedSource);
                newSinks.forEach(newSink -> {
                    if (affectedSource.port().equals(newSink.port())) {
                        log.warn("Skip {} since sink {} is on the same port of source {}. Abort",
                                 mcastIp, newSink, affectedSource);
                        return;
                    }
                    addPortToDevice(newSink.deviceId(), newSink.port(), mcastIp,
                                    mcastUtils.assignedVlan(affectedSource));
                    mcastRoleStore.put(new McastRoleStoreKey(mcastIp, newSink.deviceId(), affectedSource), INGRESS);
                });
            });
        }
        return Optional.empty();
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
            if (Objects.equal(HostId.NONE, hostId)) {
                //in this case connect points are single homed sinks.
                //just found the difference btw previous and new sinks for this source.
                Set<ConnectPoint> difference = Sets.difference(connectPoints, newSinks.get(hostId));
                sinksToBeProcessed.addAll(difference);
                return;
            }
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
            //add all connect points that are not tied with any host
            if (Objects.equal(HostId.NONE, hostId)) {
                sinksToBeProcessed.addAll(connectPoints);
                return;
            }
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
        // TODO trace
        log.info("Adding {} on {}/{} and vlan {}", mcastIp, deviceId, port, assignedVlan);
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, deviceId, assignedVlan);
        ImmutableSet.Builder<PortNumber> portBuilder = ImmutableSet.builder();
        NextObjective newNextObj;
        if (!mcastNextObjStore.containsKey(mcastStoreKey)) {
            // First time someone request this mcast group via this device
            portBuilder.add(port);
            // New nextObj
            if (!srManager.deviceConfiguration().isConfigured(deviceId)) {
                log.debug("Passing 0 as nextId for unconfigured device {}", deviceId);
                newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                            portBuilder.build(), 0).add();
            } else {
                newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                            portBuilder.build(), null).add();
            }
            // Store the new port
            mcastNextObjStore.put(mcastStoreKey, newNextObj);
            // Create, store and apply the new nextObj and fwdObj
            ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Successfully add {} on {}/{}, vlan {}",
                        mcastIp, deviceId, port.toLong(), assignedVlan),
                (objective, error) -> {
                    log.warn("Failed to add {} on {}/{}, vlan {}: {}",
                            mcastIp, deviceId, port.toLong(), assignedVlan, error);
                    // Schedule the removal using directly the key
                    mcastWorker.execute(() -> mcastNextObjStore.remove(mcastStoreKey));
                });
            ForwardingObjective fwdObj = mcastUtils.fwdObjBuilder(mcastIp, assignedVlan,
                                                          newNextObj.id()).add(context);
            if (!srManager.deviceConfiguration().isConfigured(deviceId)) {
                log.debug("skip next and forward flowobjective addition for device: {}", deviceId);
            } else {
                srManager.flowObjectiveService.next(deviceId, newNextObj);
                srManager.flowObjectiveService.forward(deviceId, fwdObj);
            }
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
            if (!srManager.deviceConfiguration().isConfigured(deviceId)) {
                log.debug("skip next flowobjective update for device: {}", deviceId);
            } else {
                // no need to update the flow here since we have updated the nextobjective/group
                // the existing flow will keep pointing to the updated nextobj
                srManager.flowObjectiveService.next(deviceId, newNextObj);
            }
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
        // TODO trace
        log.info("Removing {} on {}/{} and vlan {}", mcastIp, deviceId, port, assignedVlan);
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
            if (!srManager.deviceConfiguration().isConfigured(deviceId)) {
                log.debug("skip forward flowobjective removal for device: {}", deviceId);
            } else {
                srManager.flowObjectiveService.forward(deviceId, fwdObj);
            }
            mcastNextObjStore.remove(mcastStoreKey);
        } else {
            // Here we store the next objective with the remaining port
            newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                        existingPorts, nextObj.id()).removeFromExisting();
            mcastNextObjStore.put(mcastStoreKey, newNextObj);
             // Let's modify the next objective removing the bucket
            newNextObj = mcastUtils.nextObjBuilder(mcastIp, assignedVlan,
                                    ImmutableSet.of(port), nextObj.id()).removeFromExisting();
            if (!srManager.deviceConfiguration().isConfigured(deviceId)) {
                log.debug("skip next flowobjective update for device: {}", deviceId);
            } else {
                // no need to update the flow here since we have updated the next objective + group
                // the existing flow will keep pointing to the updated nextobj
                srManager.flowObjectiveService.next(deviceId, newNextObj);
            }
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
        // TODO trace
        log.info("Removing {} on {} and vlan {}", mcastIp, deviceId, assignedVlan);
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
        if (!srManager.deviceConfiguration().isConfigured(deviceId)) {
            log.debug("skip flow changes on unconfigured device: {}", deviceId);
        } else {
            ForwardingObjective fwdObj = mcastUtils.fwdObjBuilder(mcastIp, assignedVlan, nextObj.id()).remove(context);
            srManager.flowObjectiveService.forward(deviceId, fwdObj);
        }
        mcastNextObjStore.remove(mcastStoreKey);
    }

    private void installPath(IpAddress mcastIp, ConnectPoint source, List<Link> links) {
        if (links.isEmpty()) {
            log.warn("There is no link that can be used. Stopping installation.");
            return;
        }
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
     * Gets a path from src to dst.
     * If a path was allocated before, returns the allocated path.
     * Otherwise, randomly pick one from available paths.
     *
     * @param src source device ID
     * @param dst destination device ID
     * @param mcastIp multicast group
     * @param allPaths paths list
     *
     * @return an optional path from src to dst
     */
    private Optional<Path> getPath(DeviceId src, DeviceId dst,
                                   IpAddress mcastIp, List<Path> allPaths) {
        if (allPaths == null) {
            allPaths = mcastUtils.getPaths(src, dst, Collections.emptySet());
        }
        if (allPaths.isEmpty()) {
            return Optional.empty();
        }
        // Create a map index of suitability-to-list of paths. For example
        // a path in the list associated to the index 1 shares only one link
        // and it is less suitable of a path belonging to the index 2
        Map<Integer, List<Path>> eligiblePaths = Maps.newHashMap();
        int score;
        // Let's build the multicast tree
        Set<List<Link>> storedPaths = getStoredPaths(mcastIp);
        Set<Link> storedTree = storedPaths.stream()
                .flatMap(Collection::stream).collect(Collectors.toSet());
        log.trace("Stored tree {}", storedTree);
        Set<Link> pathLinks;
        for (Path path : allPaths) {
            if (!src.equals(path.links().get(0).src().deviceId())) {
                continue;
            }
            pathLinks = Sets.newHashSet(path.links());
            score = Sets.intersection(pathLinks, storedTree).size();
            // score defines the index
            if (score > 0) {
                eligiblePaths.compute(score, (index, paths) -> {
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
     * Gets stored paths of the group.
     *
     * @param mcastIp group address
     * @return a collection of paths
     */
    private Set<List<Link>> getStoredPaths(IpAddress mcastIp) {
        return mcastPathStore.stream()
                .filter(entry -> entry.getKey().mcastIp().equals(mcastIp))
                .map(Entry::getValue)
                .collect(Collectors.toSet());
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
     * Gets sink(s) of given multicast group.
     *
     * @param mcastIp multicast IP
     * @return set of connect point or empty set if not found
     */
    private Set<ConnectPoint> getSinks(IpAddress mcastIp, DeviceId device, ConnectPoint source) {
        McastPathStoreKey pathStoreKey = new McastPathStoreKey(mcastIp, source);
        Collection<? extends List<Link>> storedPaths = Versioned.valueOrElse(
                mcastPathStore.get(pathStoreKey), Lists.newArrayList());
        VlanId assignedVlan = mcastUtils.assignedVlan(device.equals(source.deviceId()) ? source : null);
        McastStoreKey mcastStoreKey = new McastStoreKey(mcastIp, device, assignedVlan);
        NextObjective nextObjective = Versioned.valueOrNull(mcastNextObjStore.get(mcastStoreKey));
        ImmutableSet.Builder<ConnectPoint> cpBuilder = ImmutableSet.builder();
        if (nextObjective != null) {
            Set<PortNumber> outputPorts = mcastUtils.getPorts(nextObjective.next());
            outputPorts.forEach(portNumber -> cpBuilder.add(new ConnectPoint(device, portNumber)));
        }
        Set<ConnectPoint> egressCp = cpBuilder.build();
        return egressCp.stream()
                .filter(connectPoint -> !mcastUtils.isInfraPort(connectPoint, storedPaths))
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
                getPath(source.deviceId(), sink.deviceId(), mcastIp, null).isPresent();
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
            log.debug("Filtering already present for connect point {}, vlan {} and {}. Abort",
                      filterObjStoreKey.ingressCP(), filterObjStoreKey.vlanId(),
                      filterObjStoreKey.isIpv4() ? "IPv4" : "IPv6");
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
                            if (!srManager.deviceConfiguration().isConfigured(deviceId)) {
                                log.trace("Skipping Bucket corrector for unconfigured device {}", deviceId);
                                continue;
                            }
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
        log.info("mcastNexts {}", mcastNextObjStore.size());
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
     * Build the mcast paths.
     *
     * @param storedPaths mcast tree
     * @param mcastIp the group ip
     * @param source the source
     */
    private Map<ConnectPoint, List<ConnectPoint>> buildMcastPaths(Collection<? extends List<Link>> storedPaths,
                                                                  IpAddress mcastIp, ConnectPoint source) {
        Map<ConnectPoint, List<ConnectPoint>> mcastTree = Maps.newHashMap();
        // Local sinks
        Set<ConnectPoint> localSinks = getSinks(mcastIp, source.deviceId(), source);
        localSinks.forEach(localSink -> mcastTree.put(localSink, Lists.newArrayList(localSink, source)));
        // Remote sinks
        storedPaths.forEach(path -> {
            List<Link> links = path;
            DeviceId egressDevice = links.get(links.size() - 1).dst().deviceId();
            Set<ConnectPoint> remoteSinks = getSinks(mcastIp, egressDevice, source);
            List<ConnectPoint> connectPoints = Lists.newArrayList(source);
            links.forEach(link -> {
                connectPoints.add(link.src());
                connectPoints.add(link.dst());
            });
            Collections.reverse(connectPoints);
            remoteSinks.forEach(remoteSink -> {
                List<ConnectPoint> finalPath = Lists.newArrayList(connectPoints);
                finalPath.add(0, remoteSink);
                mcastTree.put(remoteSink, finalPath);
            });
        });
        return mcastTree;
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
        log.info("mcastRoles {}", mcastRoleStore.size());
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
        // TODO remove
        log.info("{}", getStoredPaths(mcastIp));
        Multimap<ConnectPoint, List<ConnectPoint>> mcastTrees = HashMultimap.create();
        Set<ConnectPoint> sources = mcastUtils.getSources(mcastIp);
        if (sourcecp != null) {
            sources = sources.stream()
                    .filter(source -> source.equals(sourcecp)).collect(Collectors.toSet());
        }
        if (!sources.isEmpty()) {
            sources.forEach(source -> {
                McastPathStoreKey pathStoreKey = new McastPathStoreKey(mcastIp, source);
                Collection<? extends List<Link>> storedPaths = Versioned.valueOrElse(
                        mcastPathStore.get(pathStoreKey), Lists.newArrayList());
                // TODO remove
                log.info("Paths for group {} and source {} - {}", mcastIp, source, storedPaths.size());
                Map<ConnectPoint, List<ConnectPoint>> mcastTree = buildMcastPaths(storedPaths, mcastIp, source);
                mcastTree.forEach(mcastTrees::put);
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
        // TODO remove
        log.info("mcastFilters {}", mcastFilteringObjStore.size());
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
