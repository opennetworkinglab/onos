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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.onlab.packet.EthType;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.onosproject.segmentrouting.storekey.DummyVlanIdStoreKey;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Default routing handler that is responsible for route computing and
 * routing rule population.
 */
public class DefaultRoutingHandler {
    private static final int MAX_CONSTANT_RETRY_ATTEMPTS = 5;
    private static final long RETRY_INTERVAL_MS = 250L;
    private static final int RETRY_INTERVAL_SCALE = 1;
    private static final long STABLITY_THRESHOLD = 10; //secs
    private static final long MASTER_CHANGE_DELAY = 1000; // ms
    private static final long PURGE_DELAY = 1000; // ms
    private static Logger log = LoggerFactory.getLogger(DefaultRoutingHandler.class);

    private SegmentRoutingManager srManager;
    private RoutingRulePopulator rulePopulator;
    private HashMap<DeviceId, EcmpShortestPathGraph> currentEcmpSpgMap;
    private HashMap<DeviceId, EcmpShortestPathGraph> updatedEcmpSpgMap;
    private DeviceConfiguration config;
    private final Lock statusLock = new ReentrantLock();
    private volatile Status populationStatus;
    private ScheduledExecutorService executorService
        = newScheduledThreadPool(1, groupedThreads("retryftr", "retry-%d", log));
    private ScheduledExecutorService executorServiceMstChg
        = newScheduledThreadPool(1, groupedThreads("masterChg", "mstch-%d", log));
    private ScheduledExecutorService executorServiceFRR
        = newScheduledThreadPool(1, groupedThreads("fullRR", "fullRR-%d", log));

    private Instant lastRoutingChange = Instant.EPOCH;
    private Instant lastFullReroute = Instant.EPOCH;

    // Distributed store to keep track of ONOS instance that should program the
    // device pair. There should be only one instance (the king) that programs the same pair.
    Map<Set<DeviceId>, NodeId> shouldProgram;
    Map<DeviceId, Boolean> shouldProgramCache;

    // Local store to keep track of all devices that this instance was responsible
    // for programming in the last run. Helps to determine if mastership changed
    // during a run - only relevant for programming as a result of topo change.
    Set<DeviceId> lastProgrammed;

    /**
     * Represents the default routing population status.
     */
    public enum Status {
        // population process is not started yet.
        IDLE,

        // population process started.
        STARTED,

        // population process was aborted due to errors, mostly for groups not
        // found.
        ABORTED,

        // population process was finished successfully.
        SUCCEEDED
    }

    /**
     * Creates a DefaultRoutingHandler object.
     *
     * @param srManager SegmentRoutingManager object
     */
    DefaultRoutingHandler(SegmentRoutingManager srManager) {
        this.shouldProgram = srManager.storageService.<Set<DeviceId>, NodeId>consistentMapBuilder()
                .withName("sr-should-program")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .withRelaxedReadConsistency()
                .build().asJavaMap();
        this.shouldProgramCache = Maps.newConcurrentMap();
        update(srManager);
    }

    /**
     * Updates a DefaultRoutingHandler object.
     *
     * @param srManager SegmentRoutingManager object
     */
    void update(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.rulePopulator = checkNotNull(srManager.routingRulePopulator);
        this.config = checkNotNull(srManager.deviceConfiguration);
        this.populationStatus = Status.IDLE;
        this.currentEcmpSpgMap = Maps.newHashMap();
        this.lastProgrammed = Sets.newConcurrentHashSet();
    }

    /**
     * Returns an immutable copy of the current ECMP shortest-path graph as
     * computed by this controller instance.
     *
     * @return immutable copy of the current ECMP graph
     */
    public ImmutableMap<DeviceId, EcmpShortestPathGraph> getCurrentEmcpSpgMap() {
        Builder<DeviceId, EcmpShortestPathGraph> builder = ImmutableMap.builder();
        currentEcmpSpgMap.entrySet().forEach(entry -> {
            if (entry.getValue() != null) {
                builder.put(entry.getKey(), entry.getValue());
            }
        });
        return builder.build();
    }

    /**
     * Acquires the lock used when making routing changes.
     */
    public void acquireRoutingLock() {
        statusLock.lock();
    }

    /**
     * Releases the lock used when making routing changes.
     */
    public void releaseRoutingLock() {
        statusLock.unlock();
    }

    /**
    * Determines if routing in the network has been stable in the last
    * STABLITY_THRESHOLD seconds, by comparing the current time to the last
    * routing change timestamp.
    *
    * @return true if stable
    */
   public boolean isRoutingStable() {
       long last = (long) (lastRoutingChange.toEpochMilli() / 1000.0);
       long now = (long) (Instant.now().toEpochMilli() / 1000.0);
       log.trace("Routing stable since {}s", now - last);
       return (now - last) > STABLITY_THRESHOLD;
   }

    /**
     * Gracefully shuts down the defaultRoutingHandler. Typically called when
     * the app is deactivated
     */
    public void shutdown() {
        executorService.shutdown();
        executorServiceMstChg.shutdown();
        executorServiceFRR.shutdown();
    }

    //////////////////////////////////////
    //  Route path handling
    //////////////////////////////////////

    /* The following three methods represent the three major ways in which
     * route-path handling is triggered in the network
     *      a) due to configuration change
     *      b) due to route-added event
     *      c) due to change in the topology
     */

    /**
     * Populates all routing rules to all switches. Typically triggered at
     * startup or after a configuration event.
     */
    public void populateAllRoutingRules() {
        lastRoutingChange = Instant.now();
        statusLock.lock();
        try {
            if (populationStatus == Status.STARTED) {
                log.warn("Previous rule population is not finished. Cannot"
                        + " proceed with populateAllRoutingRules");
                return;
            }

            populationStatus = Status.STARTED;
            rulePopulator.resetCounter();
            log.info("Starting to populate all routing rules");
            log.debug("populateAllRoutingRules: populationStatus is STARTED");

            // take a snapshot of the topology
            updatedEcmpSpgMap = new HashMap<>();
            Set<EdgePair> edgePairs = new HashSet<>();
            Set<ArrayList<DeviceId>> routeChanges = new HashSet<>();
            for (DeviceId dstSw : srManager.deviceConfiguration.getRouters()) {
                EcmpShortestPathGraph ecmpSpgUpdated =
                        new EcmpShortestPathGraph(dstSw, srManager);
                updatedEcmpSpgMap.put(dstSw, ecmpSpgUpdated);
                Optional<DeviceId> pairDev = srManager.getPairDeviceId(dstSw);
                if (pairDev.isPresent()) {
                    // pairDev may not be available yet, but we still need to add
                    ecmpSpgUpdated = new EcmpShortestPathGraph(pairDev.get(), srManager);
                    updatedEcmpSpgMap.put(pairDev.get(), ecmpSpgUpdated);
                    edgePairs.add(new EdgePair(dstSw, pairDev.get()));
                }

                if (!shouldProgram(dstSw)) {
                    lastProgrammed.remove(dstSw);
                    continue;
                } else {
                    lastProgrammed.add(dstSw);
                }
                // To do a full reroute, assume all route-paths have changed
                for (DeviceId dev : deviceAndItsPair(dstSw)) {
                    for (DeviceId targetSw : srManager.deviceConfiguration.getRouters()) {
                        if (targetSw.equals(dev)) {
                            continue;
                        }
                        routeChanges.add(Lists.newArrayList(targetSw, dev));
                    }
                }
            }

            if (!redoRouting(routeChanges, edgePairs, null)) {
                log.debug("populateAllRoutingRules: populationStatus is ABORTED");
                populationStatus = Status.ABORTED;
                log.warn("Failed to repopulate all routing rules.");
                return;
            }

            log.debug("populateAllRoutingRules: populationStatus is SUCCEEDED");
            populationStatus = Status.SUCCEEDED;
            log.info("Completed all routing rule population. Total # of rules pushed : {}",
                    rulePopulator.getCounter());
            return;
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Populate rules from all other edge devices to the connect-point(s)
     * specified for the given subnets.
     *
     * @param cpts connect point(s) of the subnets being added
     * @param subnets subnets being added
     */
    // XXX refactor
    protected void populateSubnet(Set<ConnectPoint> cpts, Set<IpPrefix> subnets) {
        if (cpts == null || cpts.size() < 1 || cpts.size() > 2) {
            log.warn("Skipping populateSubnet due to illegal size of connect points. {}", cpts);
            return;
        }

        lastRoutingChange = Instant.now();
        statusLock.lock();
        try {
           if (populationStatus == Status.STARTED) {
                log.warn("Previous rule population is not finished. Cannot"
                        + " proceed with routing rules for added routes");
                return;
            }
            populationStatus = Status.STARTED;
            rulePopulator.resetCounter();
            log.info("Starting to populate routing rules for added routes, subnets={}, cpts={}",
                    subnets, cpts);
            // In principle an update to a subnet/prefix should not require a
            // new ECMPspg calculation as it is not a topology event. As a
            // result, we use the current/existing ECMPspg in the updated map
            // used by the redoRouting method.
            if (updatedEcmpSpgMap == null) {
                updatedEcmpSpgMap = new HashMap<>();
            }
            currentEcmpSpgMap.entrySet().forEach(entry -> {
                updatedEcmpSpgMap.put(entry.getKey(), entry.getValue());
                if (log.isTraceEnabled()) {
                    log.trace("Root switch: {}", entry.getKey());
                    log.trace("  Current/Existing SPG: {}", entry.getValue());
                }
            });
            Set<EdgePair> edgePairs = new HashSet<>();
            Set<ArrayList<DeviceId>> routeChanges = new HashSet<>();
            boolean handleRouting = false;

            if (cpts.size() == 2) {
                // ensure connect points are edge-pairs
                Iterator<ConnectPoint> iter = cpts.iterator();
                DeviceId dev1 = iter.next().deviceId();
                Optional<DeviceId> pairDev = srManager.getPairDeviceId(dev1);
                if (pairDev.isPresent() && iter.next().deviceId().equals(pairDev.get())) {
                    edgePairs.add(new EdgePair(dev1, pairDev.get()));
                } else {
                    log.warn("Connectpoints {} for subnets {} not on "
                            + "pair-devices.. aborting populateSubnet", cpts, subnets);
                    populationStatus = Status.ABORTED;
                    return;
                }
                for (ConnectPoint cp : cpts) {
                    if (updatedEcmpSpgMap.get(cp.deviceId()) == null) {
                        EcmpShortestPathGraph ecmpSpgUpdated =
                            new EcmpShortestPathGraph(cp.deviceId(), srManager);
                        updatedEcmpSpgMap.put(cp.deviceId(), ecmpSpgUpdated);
                        log.warn("populateSubnet: no updated graph for dev:{}"
                                + " ... creating", cp.deviceId());
                    }
                    if (!shouldProgram(cp.deviceId())) {
                        continue;
                    }
                    handleRouting = true;
                }
            } else {
                // single connect point
                DeviceId dstSw = cpts.iterator().next().deviceId();
                if (updatedEcmpSpgMap.get(dstSw) == null) {
                    EcmpShortestPathGraph ecmpSpgUpdated =
                        new EcmpShortestPathGraph(dstSw, srManager);
                    updatedEcmpSpgMap.put(dstSw, ecmpSpgUpdated);
                    log.warn("populateSubnet: no updated graph for dev:{}"
                            + " ... creating", dstSw);
                }
                handleRouting = shouldProgram(dstSw);
            }

            if (!handleRouting) {
                log.debug("This instance is not handling ecmp routing to the "
                        + "connectPoint(s) {}", cpts);
                populationStatus = Status.ABORTED;
                return;
            }

            // if it gets here, this instance should handle routing for the
            // connectpoint(s). Assume all route-paths have to be updated to
            // the connectpoint(s) with the following exceptions
            // 1. if target is non-edge no need for routing rules
            // 2. if target is one of the connectpoints
            for (ConnectPoint cp : cpts) {
                DeviceId dstSw = cp.deviceId();
                for (Device targetSw : srManager.deviceService.getDevices()) {
                    boolean isEdge = false;
                    try {
                        isEdge = config.isEdgeDevice(targetSw.id());
                    } catch (DeviceConfigNotFoundException e) {
                        log.warn(e.getMessage() + "aborting populateSubnet on targetSw {}", targetSw.id());
                        continue;
                    }
                    Optional<DeviceId> pairDev = srManager.getPairDeviceId(dstSw);
                    if (dstSw.equals(targetSw.id()) || !isEdge ||
                            (cpts.size() == 2 && pairDev.isPresent() && targetSw.id().equals(pairDev.get()))) {
                        continue;
                    }
                    routeChanges.add(Lists.newArrayList(targetSw.id(), dstSw));
                }
            }

            if (!redoRouting(routeChanges, edgePairs, subnets)) {
                log.debug("populateSubnet: populationStatus is ABORTED");
                populationStatus = Status.ABORTED;
                log.warn("Failed to repopulate the rules for subnet.");
                return;
            }

            log.debug("populateSubnet: populationStatus is SUCCEEDED");
            populationStatus = Status.SUCCEEDED;
            log.info("Completed subnet population. Total # of rules pushed : {}",
                    rulePopulator.getCounter());
            return;

        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Populates the routing rules or makes hash group changes according to the
     * route-path changes due to link failure, switch failure or link up. This
     * method should only be called for one of these three possible event-types.
     * Note that when a switch goes away, all of its links fail as well, but
     * this is handled as a single switch removal event.
     *
     * @param linkDown the single failed link, or null for other conditions such
     *            as link-up or a removed switch
     * @param linkUp the single link up, or null for other conditions such as
     *            link-down or a removed switch
     * @param switchDown the removed switch, or null for other conditions such
     *            as link-down or link-up
     * @param seenBefore true if this event is for a linkUp or linkDown for a
     *            seen link
     */
    // TODO This method should be refactored into three separated methods
    public void populateRoutingRulesForLinkStatusChange(Link linkDown, Link linkUp,
                                                        DeviceId switchDown, boolean seenBefore) {
        if (Stream.of(linkDown, linkUp, switchDown).filter(Objects::nonNull)
                .count() != 1) {
            log.warn("Only one event can be handled for link status change .. aborting");
            return;
        }

        lastRoutingChange = Instant.now();
        statusLock.lock();
        try {

            if (populationStatus == Status.STARTED) {
                log.warn("Previous rule population is not finished. Cannot"
                        + " proceeed with routingRules for Topology change");
                return;
            }

            // Take snapshots of the topology
            updatedEcmpSpgMap = new HashMap<>();
            Set<EdgePair> edgePairs = new HashSet<>();
            for (Device sw : srManager.deviceService.getDevices()) {
                EcmpShortestPathGraph ecmpSpgUpdated =
                        new EcmpShortestPathGraph(sw.id(), srManager);
                updatedEcmpSpgMap.put(sw.id(), ecmpSpgUpdated);
                Optional<DeviceId> pairDev = srManager.getPairDeviceId(sw.id());
                if (pairDev.isPresent()) {
                    // pairDev may not be available yet, but we still need to add
                    ecmpSpgUpdated = new EcmpShortestPathGraph(pairDev.get(), srManager);
                    updatedEcmpSpgMap.put(pairDev.get(), ecmpSpgUpdated);
                    edgePairs.add(new EdgePair(sw.id(), pairDev.get()));
                }
            }

            log.info("Starting to populate routing rules from Topology change");

            Set<ArrayList<DeviceId>> routeChanges;
            log.debug("populateRoutingRulesForLinkStatusChange: "
                    + "populationStatus is STARTED");
            populationStatus = Status.STARTED;
            rulePopulator.resetCounter(); //XXX maybe useful to have a rehash ctr
            boolean hashGroupsChanged = false;
            // try optimized re-routing
            if (linkDown == null) {
                // either a linkUp or a switchDown - compute all route changes by
                // comparing all routes of existing ECMP SPG to new ECMP SPG
                routeChanges = computeRouteChange(switchDown);

                // deal with linkUp of a seen-before link
                if (linkUp != null && seenBefore) {
                    // link previously seen before
                    // do hash-bucket changes instead of a re-route
                    processHashGroupChange(routeChanges, false, null);
                    // clear out routesChanges so a re-route is not attempted
                    routeChanges = ImmutableSet.of();
                    hashGroupsChanged = true;
                }
                // for a linkUp of a never-seen-before link
                // let it fall through to a reroute of the routeChanges

                //deal with switchDown
                if (switchDown != null) {
                    processHashGroupChange(routeChanges, true, switchDown);
                    // clear out routesChanges so a re-route is not attempted
                    routeChanges = ImmutableSet.of();
                    hashGroupsChanged = true;
                }
            } else {
                // link has gone down
                // Compare existing ECMP SPG only with the link that went down
                routeChanges = computeDamagedRoutes(linkDown);
                processHashGroupChange(routeChanges, true, null);
                // clear out routesChanges so a re-route is not attempted
                routeChanges = ImmutableSet.of();
                hashGroupsChanged = true;
            }

            if (routeChanges.isEmpty()) {
                if (hashGroupsChanged) {
                    log.info("Hash-groups changed for link status change");
                } else {
                    log.info("No re-route or re-hash attempted for the link"
                            + " status change");
                    updatedEcmpSpgMap.keySet().forEach(devId -> {
                        currentEcmpSpgMap.put(devId, updatedEcmpSpgMap.get(devId));
                        log.debug("Updating ECMPspg for remaining dev:{}", devId);
                    });
                }
                log.debug("populateRoutingRulesForLinkStatusChange: populationStatus is SUCCEEDED");
                populationStatus = Status.SUCCEEDED;
                return;
            }

            // reroute of routeChanges
            if (redoRouting(routeChanges, edgePairs, null)) {
                log.debug("populateRoutingRulesForLinkStatusChange: populationStatus is SUCCEEDED");
                populationStatus = Status.SUCCEEDED;
                log.info("Completed repopulation of rules for link-status change."
                        + " # of rules populated : {}", rulePopulator.getCounter());
                return;
            } else {
                log.debug("populateRoutingRulesForLinkStatusChange: populationStatus is ABORTED");
                populationStatus = Status.ABORTED;
                log.warn("Failed to repopulate the rules for link status change.");
                return;
            }
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Processes a set a route-path changes by reprogramming routing rules and
     * creating new hash-groups or editing them if necessary. This method also
     * determines the next-hops for the route-path from the src-switch (target)
     * of the path towards the dst-switch of the path.
     *
     * @param routeChanges a set of route-path changes, where each route-path is
     *                     a list with its first element the src-switch (target)
     *                     of the path, and the second element the dst-switch of
     *                     the path.
     * @param edgePairs a set of edge-switches that are paired by configuration
     * @param subnets  a set of prefixes that need to be populated in the routing
     *                 table of the target switch in the route-path. Can be null,
     *                 in which case all the prefixes belonging to the dst-switch
     *                 will be populated in the target switch
     * @return true if successful in repopulating all routes
     */
    private boolean redoRouting(Set<ArrayList<DeviceId>> routeChanges,
                                Set<EdgePair> edgePairs, Set<IpPrefix> subnets) {
        // first make every entry two-elements
        Set<ArrayList<DeviceId>> changedRoutes = new HashSet<>();
        for (ArrayList<DeviceId> route : routeChanges) {
            if (route.size() == 1) {
                DeviceId dstSw = route.get(0);
                EcmpShortestPathGraph ec = updatedEcmpSpgMap.get(dstSw);
                if (ec == null) {
                    log.warn("No graph found for {} .. aborting redoRouting", dstSw);
                    return false;
                }
                ec.getAllLearnedSwitchesAndVia().keySet().forEach(key -> {
                    ec.getAllLearnedSwitchesAndVia().get(key).keySet().forEach(target -> {
                        changedRoutes.add(Lists.newArrayList(target, dstSw));
                    });
                });
            } else {
                DeviceId targetSw = route.get(0);
                DeviceId dstSw = route.get(1);
                changedRoutes.add(Lists.newArrayList(targetSw, dstSw));
            }
        }

        // now process changedRoutes according to edgePairs
        if (!redoRoutingEdgePairs(edgePairs, subnets, changedRoutes)) {
            return false; //abort routing and fail fast
        }

        // whatever is left in changedRoutes is now processed for individual dsts.
        Set<DeviceId> updatedDevices = Sets.newHashSet();
        if (!redoRoutingIndividualDests(subnets, changedRoutes,
                                        updatedDevices)) {
            return false; //abort routing and fail fast
        }

        // update ecmpSPG for all edge-pairs
        for (EdgePair ep : edgePairs) {
            currentEcmpSpgMap.put(ep.dev1, updatedEcmpSpgMap.get(ep.dev1));
            currentEcmpSpgMap.put(ep.dev2, updatedEcmpSpgMap.get(ep.dev2));
            log.debug("Updating ECMPspg for edge-pair:{}-{}", ep.dev1, ep.dev2);
        }

        // here is where we update all devices not touched by this instance
        updatedEcmpSpgMap.keySet().stream()
            .filter(devId -> !edgePairs.stream().anyMatch(ep -> ep.includes(devId)))
            .filter(devId -> !updatedDevices.contains(devId))
            .forEach(devId -> {
                currentEcmpSpgMap.put(devId, updatedEcmpSpgMap.get(devId));
                log.debug("Updating ECMPspg for remaining dev:{}", devId);
            });
        return true;
    }

    /**
     * Programs targetSw in the changedRoutes for given prefixes reachable by
     * an edgePair. If no prefixes are given, the method will use configured
     * subnets/prefixes. If some configured subnets belong only to a specific
     * destination in the edgePair, then the target switch will be programmed
     * only to that destination.
     *
     * @param edgePairs set of edge-pairs for which target will be programmed
     * @param subnets a set of prefixes that need to be populated in the routing
     *                 table of the target switch in the changedRoutes. Can be null,
     *                 in which case all the configured prefixes belonging to the
     *                 paired switches will be populated in the target switch
     * @param changedRoutes a set of route-path changes, where each route-path is
     *                     a list with its first element the src-switch (target)
     *                     of the path, and the second element the dst-switch of
     *                     the path.
     * @return true if successful
     */
    private boolean redoRoutingEdgePairs(Set<EdgePair> edgePairs,
                                      Set<IpPrefix> subnets,
                                      Set<ArrayList<DeviceId>> changedRoutes) {
        for (EdgePair ep : edgePairs) {
            // temp store for a target's changedRoutes to this edge-pair
            Map<DeviceId, Set<ArrayList<DeviceId>>> targetRoutes = new HashMap<>();
            Iterator<ArrayList<DeviceId>> i = changedRoutes.iterator();
            while (i.hasNext()) {
                ArrayList<DeviceId> route = i.next();
                DeviceId dstSw = route.get(1);
                if (ep.includes(dstSw)) {
                    // routeChange for edge pair found
                    // sort by target iff target is edge and remove from changedRoutes
                    DeviceId targetSw = route.get(0);
                    try {
                        if (!srManager.deviceConfiguration.isEdgeDevice(targetSw)) {
                            continue;
                        }
                    } catch (DeviceConfigNotFoundException e) {
                        log.warn(e.getMessage() + "aborting redoRouting");
                        return false;
                    }
                    // route is from another edge to this edge-pair
                    if (targetRoutes.containsKey(targetSw)) {
                        targetRoutes.get(targetSw).add(route);
                    } else {
                        Set<ArrayList<DeviceId>> temp = new HashSet<>();
                        temp.add(route);
                        targetRoutes.put(targetSw, temp);
                    }
                    i.remove();
                }
            }
            // so now for this edgepair we have a per target set of routechanges
            // process target->edgePair route
            for (Map.Entry<DeviceId, Set<ArrayList<DeviceId>>> entry :
                            targetRoutes.entrySet()) {
                log.debug("* redoRoutingDstPair Target:{} -> edge-pair {}",
                          entry.getKey(), ep);
                DeviceId targetSw = entry.getKey();
                Map<DeviceId, Set<DeviceId>> perDstNextHops = new HashMap<>();
                entry.getValue().forEach(route -> {
                    Set<DeviceId> nhops = getNextHops(route.get(0), route.get(1));
                    log.debug("route: target {} -> dst {} found with next-hops {}",
                              route.get(0), route.get(1), nhops);
                    perDstNextHops.put(route.get(1), nhops);
                });
                Set<IpPrefix> ipDev1 = (subnets == null) ? config.getSubnets(ep.dev1)
                                                         : subnets;
                Set<IpPrefix> ipDev2 = (subnets == null) ? config.getSubnets(ep.dev2)
                                                         : subnets;
                ipDev1 = (ipDev1 == null) ? Sets.newHashSet() : ipDev1;
                ipDev2 = (ipDev2 == null) ? Sets.newHashSet() : ipDev2;
                Set<DeviceId> nhDev1 = perDstNextHops.get(ep.dev1);
                Set<DeviceId> nhDev2 = perDstNextHops.get(ep.dev2);
                // handle routing to subnets common to edge-pair
                // only if the targetSw is not part of the edge-pair and there
                // exists a next hop to at least one of the devices in the edge-pair
                if (!ep.includes(targetSw)
                        && ((nhDev1 != null && !nhDev1.isEmpty())
                                || (nhDev2 != null && !nhDev2.isEmpty()))) {
                    if (!populateEcmpRoutingRulePartial(
                             targetSw,
                             ep.dev1, ep.dev2,
                             perDstNextHops,
                             Sets.intersection(ipDev1, ipDev2))) {
                        return false; // abort everything and fail fast
                    }
                }
                // handle routing to subnets that only belong to dev1 only if
                // a next-hop exists from the target to dev1
                Set<IpPrefix> onlyDev1Subnets = Sets.difference(ipDev1, ipDev2);
                if (!onlyDev1Subnets.isEmpty()
                        && nhDev1 != null  && !nhDev1.isEmpty()) {
                    Map<DeviceId, Set<DeviceId>> onlyDev1NextHops = new HashMap<>();
                    onlyDev1NextHops.put(ep.dev1, nhDev1);
                    if (!populateEcmpRoutingRulePartial(
                            targetSw,
                            ep.dev1, null,
                            onlyDev1NextHops,
                            onlyDev1Subnets)) {
                        return false; // abort everything and fail fast
                    }
                }
                // handle routing to subnets that only belong to dev2 only if
                // a next-hop exists from the target to dev2
                Set<IpPrefix> onlyDev2Subnets = Sets.difference(ipDev2, ipDev1);
                if (!onlyDev2Subnets.isEmpty()
                        && nhDev2 != null && !nhDev2.isEmpty()) {
                    Map<DeviceId, Set<DeviceId>> onlyDev2NextHops = new HashMap<>();
                    onlyDev2NextHops.put(ep.dev2, nhDev2);
                    if (!populateEcmpRoutingRulePartial(
                            targetSw,
                            ep.dev2, null,
                            onlyDev2NextHops,
                            onlyDev2Subnets)) {
                        return false; // abort everything and fail fast
                    }
                }
            }
            // if it gets here it has succeeded for all targets to this edge-pair
        }
        return true;
    }

    /**
     * Programs targetSw in the changedRoutes for given prefixes reachable by
     * a destination switch that is not part of an edge-pair.
     * If no prefixes are given, the method will use configured subnets/prefixes.
     *
     * @param subnets a set of prefixes that need to be populated in the routing
     *                 table of the target switch in the changedRoutes. Can be null,
     *                 in which case all the configured prefixes belonging to the
     *                 paired switches will be populated in the target switch
     * @param changedRoutes a set of route-path changes, where each route-path is
     *                     a list with its first element the src-switch (target)
     *                     of the path, and the second element the dst-switch of
     *                     the path.
     * @return true if successful
     */
    private boolean redoRoutingIndividualDests(Set<IpPrefix> subnets,
                                               Set<ArrayList<DeviceId>> changedRoutes,
                                               Set<DeviceId> updatedDevices) {
        // aggregate route-path changes for each dst device
        HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> routesBydevice =
                new HashMap<>();
        for (ArrayList<DeviceId> route: changedRoutes) {
            DeviceId dstSw = route.get(1);
            ArrayList<ArrayList<DeviceId>> deviceRoutes =
                    routesBydevice.get(dstSw);
            if (deviceRoutes == null) {
                deviceRoutes = new ArrayList<>();
                routesBydevice.put(dstSw, deviceRoutes);
            }
            deviceRoutes.add(route);
        }
        for (DeviceId impactedDstDevice : routesBydevice.keySet()) {
            ArrayList<ArrayList<DeviceId>> deviceRoutes =
                    routesBydevice.get(impactedDstDevice);
            for (ArrayList<DeviceId> route: deviceRoutes) {
                log.debug("* redoRoutingIndiDst Target: {} -> dst: {}",
                          route.get(0), route.get(1));
                DeviceId targetSw = route.get(0);
                DeviceId dstSw = route.get(1); // same as impactedDstDevice
                Set<DeviceId> nextHops = getNextHops(targetSw, dstSw);
                if (nextHops.isEmpty()) {
                    log.debug("Could not find next hop from target:{} --> dst {} "
                            + "skipping this route", targetSw, dstSw);
                    continue;
                }
                Map<DeviceId, Set<DeviceId>> nhops = new HashMap<>();
                nhops.put(dstSw, nextHops);
                if (!populateEcmpRoutingRulePartial(targetSw, dstSw, null, nhops,
                         (subnets == null) ? Sets.newHashSet() : subnets)) {
                    return false; // abort routing and fail fast
                }
                log.debug("Populating flow rules from target: {} to dst: {}"
                        + " is successful", targetSw, dstSw);
            }
            //Only if all the flows for all impacted routes to a
            //specific target are pushed successfully, update the
            //ECMP graph for that target. Or else the next event
            //would not see any changes in the ECMP graphs.
            //In another case, the target switch has gone away, so
            //routes can't be installed. In that case, the current map
            //is updated here, without any flows being pushed.
            currentEcmpSpgMap.put(impactedDstDevice,
                                  updatedEcmpSpgMap.get(impactedDstDevice));
            updatedDevices.add(impactedDstDevice);
            log.debug("Updating ECMPspg for impacted dev:{}", impactedDstDevice);
        }
        return true;
    }

    /**
     * Populate ECMP rules for subnets from target to destination via nexthops.
     *
     * @param targetSw Device ID of target switch in which rules will be programmed
     * @param destSw1 Device ID of final destination switch to which the rules will forward
     * @param destSw2 Device ID of paired destination switch to which the rules will forward
     *                A null deviceId indicates packets should only be sent to destSw1
     * @param nextHops Map of a set of next hops per destSw
     * @param subnets Subnets to be populated. If empty, populate all configured subnets.
     * @return true if it succeeds in populating rules
     */ // refactor
    private boolean populateEcmpRoutingRulePartial(DeviceId targetSw,
                                                   DeviceId destSw1,
                                                   DeviceId destSw2,
                                                   Map<DeviceId, Set<DeviceId>> nextHops,
                                                   Set<IpPrefix> subnets) {
        boolean result;
        // If both target switch and dest switch are edge routers, then set IP
        // rule for both subnet and router IP.
        boolean targetIsEdge;
        boolean dest1IsEdge;
        Ip4Address dest1RouterIpv4, dest2RouterIpv4 = null;
        Ip6Address dest1RouterIpv6, dest2RouterIpv6 = null;

        try {
            targetIsEdge = config.isEdgeDevice(targetSw);
            dest1IsEdge = config.isEdgeDevice(destSw1);
            dest1RouterIpv4 = config.getRouterIpv4(destSw1);
            dest1RouterIpv6 = config.getRouterIpv6(destSw1);
            if (destSw2 != null) {
                dest2RouterIpv4 = config.getRouterIpv4(destSw2);
                dest2RouterIpv6 = config.getRouterIpv6(destSw2);
            }
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateEcmpRoutingRulePartial.");
            return false;
        }

        if (targetIsEdge && dest1IsEdge) {
            subnets = (subnets != null && !subnets.isEmpty())
                            ? Sets.newHashSet(subnets)
                            : Sets.newHashSet(config.getSubnets(destSw1));
            // XXX - Rethink this - ignoring routerIPs in all other switches
            // even edge to edge switches
            /*subnets.add(dest1RouterIpv4.toIpPrefix());
            if (dest1RouterIpv6 != null) {
                subnets.add(dest1RouterIpv6.toIpPrefix());
            }
            if (destSw2 != null && dest2RouterIpv4 != null) {
                subnets.add(dest2RouterIpv4.toIpPrefix());
                if (dest2RouterIpv6 != null) {
                    subnets.add(dest2RouterIpv6.toIpPrefix());
                }
            }*/
            log.debug(". populateEcmpRoutingRulePartial in device {} towards {} {} "
                    + "for subnets {}", targetSw, destSw1,
                                        (destSw2 != null) ? ("& " + destSw2) : "",
                                        subnets);
            result = rulePopulator.populateIpRuleForSubnet(targetSw, subnets,
                                                           destSw1, destSw2,
                                                           nextHops);
            if (!result) {
                return false;
            }
        }

        if (!targetIsEdge && dest1IsEdge) {
            // MPLS rules in all non-edge target devices. These rules are for
            // individual destinations, even if the dsts are part of edge-pairs.
            log.debug(". populateEcmpRoutingRulePartial in device{} towards {} for "
                    + "all MPLS rules", targetSw, destSw1);
            result = rulePopulator.populateMplsRule(targetSw, destSw1,
                                                    nextHops.get(destSw1),
                                                    dest1RouterIpv4);
            if (!result) {
                return false;
            }
            if (dest1RouterIpv6 != null) {
                int v4sid = 0, v6sid = 0;
                try {
                    v4sid = config.getIPv4SegmentId(destSw1);
                    v6sid = config.getIPv6SegmentId(destSw1);
                } catch (DeviceConfigNotFoundException e) {
                    log.warn(e.getMessage());
                }
                if (v4sid != v6sid) {
                    result = rulePopulator.populateMplsRule(targetSw, destSw1,
                                                            nextHops.get(destSw1),
                                                            dest1RouterIpv6);
                    if (!result) {
                        return false;
                    }
                }
            }
        }

        if (!targetIsEdge && !dest1IsEdge) {
            // MPLS rules for inter-connected spines
            // can be merged with above if, left it here for clarity
            log.debug(". populateEcmpRoutingRulePartial in device{} towards {} for "
                              + "all MPLS rules", targetSw, destSw1);

            result = rulePopulator.populateMplsRule(targetSw, destSw1,
                                                        nextHops.get(destSw1),
                                                        dest1RouterIpv4);
            if (!result) {
                return false;
            }

            if (dest1RouterIpv6 != null) {
                int v4sid = 0, v6sid = 0;
                try {
                    v4sid = config.getIPv4SegmentId(destSw1);
                    v6sid = config.getIPv6SegmentId(destSw1);
                } catch (DeviceConfigNotFoundException e) {
                    log.warn(e.getMessage());
                }
                if (v4sid != v6sid) {
                    result = rulePopulator.populateMplsRule(targetSw, destSw1,
                                                            nextHops.get(destSw1),
                                                            dest1RouterIpv6);
                    if (!result) {
                        return false;
                    }
                }
           }
        }

        // To save on ECMP groups
        // avoid MPLS rules in non-edge-devices to non-edge-devices
        // avoid MPLS transit rules in edge-devices
        // avoid loopback IP rules in edge-devices to non-edge-devices
        return true;
    }

    /**
     * Processes a set a route-path changes by editing hash groups.
     *
     * @param routeChanges a set of route-path changes, where each route-path is
     *                     a list with its first element the src-switch of the path
     *                     and the second element the dst-switch of the path.
     * @param linkOrSwitchFailed true if the route changes are for a failed
     *                           switch or linkDown event
     * @param failedSwitch the switchId if the route changes are for a failed switch,
     *                     otherwise null
     */
    private void processHashGroupChange(Set<ArrayList<DeviceId>> routeChanges,
                                        boolean linkOrSwitchFailed,
                                        DeviceId failedSwitch) {
        Set<ArrayList<DeviceId>> changedRoutes = new HashSet<>();
        // first, ensure each routeChanges entry has two elements
        for (ArrayList<DeviceId> route : routeChanges) {
            if (route.size() == 1) {
                // route-path changes are from everyone else to this switch
                DeviceId dstSw = route.get(0);
                srManager.deviceService.getAvailableDevices().forEach(sw -> {
                    if (!sw.id().equals(dstSw)) {
                        changedRoutes.add(Lists.newArrayList(sw.id(), dstSw));
                    }
                });
            } else {
                changedRoutes.add(route);
            }
        }
        boolean someFailed = false;
        Set<DeviceId> updatedDevices = Sets.newHashSet();
        for (ArrayList<DeviceId> route : changedRoutes) {
            DeviceId targetSw = route.get(0);
            DeviceId dstSw = route.get(1);
            if (linkOrSwitchFailed) {
                boolean success = fixHashGroupsForRoute(route, true);
                // it's possible that we cannot fix hash groups for a route
                // if the target switch has failed. Nevertheless the ecmp graph
                // for the impacted switch must still be updated.
                if (!success && failedSwitch != null && targetSw.equals(failedSwitch)) {
                    currentEcmpSpgMap.put(dstSw, updatedEcmpSpgMap.get(dstSw));
                    currentEcmpSpgMap.remove(targetSw);
                    log.debug("Updating ECMPspg for dst:{} removing failed switch "
                            + "target:{}", dstSw, targetSw);
                    updatedDevices.add(targetSw);
                    updatedDevices.add(dstSw);
                    continue;
                }
                //linkfailed - update both sides
                if (success) {
                    currentEcmpSpgMap.put(targetSw, updatedEcmpSpgMap.get(targetSw));
                    currentEcmpSpgMap.put(dstSw, updatedEcmpSpgMap.get(dstSw));
                    log.debug("Updating ECMPspg for dst:{} and target:{} for linkdown"
                            + " or switchdown", dstSw, targetSw);
                    updatedDevices.add(targetSw);
                    updatedDevices.add(dstSw);
                } else {
                    someFailed = true;
                }
            } else {
                //linkup of seen before link
                boolean success = fixHashGroupsForRoute(route, false);
                if (success) {
                    currentEcmpSpgMap.put(targetSw, updatedEcmpSpgMap.get(targetSw));
                    currentEcmpSpgMap.put(dstSw, updatedEcmpSpgMap.get(dstSw));
                    log.debug("Updating ECMPspg for target:{} and dst:{} for linkup",
                              targetSw, dstSw);
                    updatedDevices.add(targetSw);
                    updatedDevices.add(dstSw);
                } else {
                    someFailed = true;
                }
            }
        }
        if (!someFailed) {
            // here is where we update all devices not touched by this instance
            updatedEcmpSpgMap.keySet().stream()
                .filter(devId -> !updatedDevices.contains(devId))
                .forEach(devId -> {
                    currentEcmpSpgMap.put(devId, updatedEcmpSpgMap.get(devId));
                    log.debug("Updating ECMPspg for remaining dev:{}", devId);
            });
        }
    }

    /**
     * Edits hash groups in the src-switch (targetSw) of a route-path by
     * calling the groupHandler to either add or remove buckets in an existing
     * hash group.
     *
     * @param route a single list representing a route-path where the first element
     *                  is the src-switch (targetSw) of the route-path and the
     *                  second element is the dst-switch
     * @param revoke true if buckets in the hash-groups need to be removed;
     *              false if buckets in the hash-groups need to be added
     * @return true if the hash group editing is successful
     */
    private boolean fixHashGroupsForRoute(ArrayList<DeviceId> route,
                                          boolean revoke) {
        DeviceId targetSw = route.get(0);
        if (route.size() < 2) {
            log.warn("Cannot fixHashGroupsForRoute - no dstSw in route {}", route);
            return false;
        }
        DeviceId destSw = route.get(1);
        log.debug("* processing fixHashGroupsForRoute: Target {} -> Dest {}",
                  targetSw, destSw);
        // figure out the new next hops at the targetSw towards the destSw
        Set<DeviceId> nextHops = getNextHops(targetSw, destSw);
        // call group handler to change hash group at targetSw
        DefaultGroupHandler grpHandler = srManager.getGroupHandler(targetSw);
        if (grpHandler == null) {
            log.warn("Cannot find grouphandler for dev:{} .. aborting"
                    + " {} hash group buckets for route:{} ", targetSw,
                    (revoke) ? "revoke" : "repopulate", route);
            return false;
        }
        log.debug("{} hash-groups buckets For Route {} -> {} to new next-hops {}",
                  (revoke) ? "revoke" : "repopulating",
                  targetSw, destSw, nextHops);
        return (revoke) ? grpHandler.fixHashGroups(targetSw, nextHops,
                                                       destSw, true)
                            : grpHandler.fixHashGroups(targetSw, nextHops,
                                                       destSw, false);
    }

    /**
     * Start the flow rule population process if it was never started. The
     * process finishes successfully when all flow rules are set and stops with
     * ABORTED status when any groups required for flows is not set yet.
     */
    public void startPopulationProcess() {
        statusLock.lock();
        try {
            if (populationStatus == Status.IDLE
                    || populationStatus == Status.SUCCEEDED
                    || populationStatus == Status.ABORTED) {
                populateAllRoutingRules();
            } else {
                log.warn("Not initiating startPopulationProcess as populationStatus is {}",
                         populationStatus);
            }
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Revoke rules of given subnet in all edge switches.
     *
     * @param subnets subnet being removed
     * @return true if succeed
     */
    protected boolean revokeSubnet(Set<IpPrefix> subnets) {
        statusLock.lock();
        try {
            return Sets.newHashSet(srManager.deviceService.getAvailableDevices()).stream()
                    .map(Device::id)
                    .filter(this::shouldProgram)
                    .allMatch(targetSw -> srManager.routingRulePopulator.revokeIpRuleForSubnet(targetSw, subnets));
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Populates IP rules for a route that has direct connection to the switch
     * if the current instance is the master of the switch.
     *
     * @param deviceId device ID of the device that next hop attaches to
     * @param prefix IP prefix of the route
     * @param hostMac MAC address of the next hop
     * @param hostVlanId Vlan ID of the nexthop
     * @param outPort port where the next hop attaches to
     */
    void populateRoute(DeviceId deviceId, IpPrefix prefix,
                       MacAddress hostMac, VlanId hostVlanId, PortNumber outPort) {
        if (shouldProgram(deviceId)) {
            srManager.routingRulePopulator.populateRoute(deviceId, prefix, hostMac, hostVlanId, outPort);
        }
    }

    /**
     * Removes IP rules for a route when the next hop is gone.
     * if the current instance is the master of the switch.
     *
     * @param deviceId device ID of the device that next hop attaches to
     * @param prefix IP prefix of the route
     * @param hostMac MAC address of the next hop
     * @param hostVlanId Vlan ID of the nexthop
     * @param outPort port that next hop attaches to
     */
    void revokeRoute(DeviceId deviceId, IpPrefix prefix,
                     MacAddress hostMac, VlanId hostVlanId, PortNumber outPort) {
        if (shouldProgram(deviceId)) {
            srManager.routingRulePopulator.revokeRoute(deviceId, prefix, hostMac, hostVlanId, outPort);
        }
    }

    void populateBridging(DeviceId deviceId, PortNumber port, MacAddress mac, VlanId vlanId) {
        if (shouldProgram(deviceId)) {
            srManager.routingRulePopulator.populateBridging(deviceId, port, mac, vlanId);
        }
    }

    void revokeBridging(DeviceId deviceId, PortNumber port, MacAddress mac, VlanId vlanId) {
        if (shouldProgram(deviceId)) {
            srManager.routingRulePopulator.revokeBridging(deviceId, port, mac, vlanId);
        }
    }

    void updateBridging(DeviceId deviceId, PortNumber portNum, MacAddress hostMac,
                        VlanId vlanId, boolean popVlan, boolean install) {
        if (shouldProgram(deviceId)) {
            srManager.routingRulePopulator.updateBridging(deviceId, portNum, hostMac, vlanId, popVlan, install);
        }
    }

    void updateFwdObj(DeviceId deviceId, PortNumber portNumber, IpPrefix prefix, MacAddress hostMac,
                      VlanId vlanId, boolean popVlan, boolean install) {
        if (shouldProgram(deviceId)) {
            srManager.routingRulePopulator.updateFwdObj(deviceId, portNumber, prefix, hostMac,
                    vlanId, popVlan, install);
        }
    }

    /**
     * Populates IP rules for a route when the next hop is double-tagged.
     *
     * @param deviceId  device ID that next hop attaches to
     * @param prefix    IP prefix of the route
     * @param hostMac   MAC address of the next hop
     * @param innerVlan Inner Vlan ID of the next hop
     * @param outerVlan Outer Vlan ID of the next hop
     * @param outerTpid Outer TPID of the next hop
     * @param outPort   port that the next hop attaches to
     */
    void populateDoubleTaggedRoute(DeviceId deviceId, IpPrefix prefix, MacAddress hostMac, VlanId innerVlan,
                                   VlanId outerVlan, EthType outerTpid, PortNumber outPort) {
        if (srManager.mastershipService.isLocalMaster(deviceId)) {
            VlanId dummyVlan = srManager.allocateDummyVlanId(
                    new ConnectPoint(deviceId, outPort), prefix.address());
            if (!dummyVlan.equals(VlanId.NONE)) {
                srManager.routingRulePopulator.populateDoubleTaggedRoute(
                        deviceId, prefix, hostMac, dummyVlan, innerVlan, outerVlan, outerTpid, outPort);
                srManager.routingRulePopulator.processDoubleTaggedFilter(
                        deviceId, outPort, outerVlan, innerVlan, true);
            } else {
                log.error("Failed to allocate dummy VLAN ID for host {} at {}/{}",
                          prefix.address(), deviceId, outPort);
            }
        }
    }

    /**
     * Revokes IP rules for a route when the next hop is double-tagged.
     *
     * @param deviceId  device ID that next hop attaches to
     * @param prefix    IP prefix of the route
     * @param hostMac   MAC address of the next hop
     * @param innerVlan Inner Vlan ID of the next hop
     * @param outerVlan Outer Vlan ID of the next hop
     * @param outerTpid Outer TPID of the next hop
     * @param outPort   port that the next hop attaches to
     */
    void revokeDoubleTaggedRoute(DeviceId deviceId, IpPrefix prefix, MacAddress hostMac, VlanId innerVlan,
                                 VlanId outerVlan, EthType outerTpid, PortNumber outPort) {
        // Revoke route either if this node have the mastership (when device is available) or
        // if this node is the leader (even when device is unavailable)
        if (!srManager.mastershipService.isLocalMaster(deviceId)) {
            if (srManager.deviceService.isAvailable(deviceId)) {
                // Master node will revoke specified rule.
                log.debug("This node is not a master for {}, stop revoking route.", deviceId);
                return;
            }

            // isLocalMaster will return false when the device is unavailable.
            // Verify if this node is the leader in that case.
            NodeId leader = srManager.leadershipService.runForLeadership(
                    deviceId.toString()).leaderNodeId();
            if (!srManager.clusterService.getLocalNode().id().equals(leader)) {
                // Leader node will revoke specified rule.
                log.debug("This node is not a master for {}, stop revoking route.", deviceId);
                return;
            }
        }

        VlanId dummyVlan = srManager.dummyVlanIdStore().get(new DummyVlanIdStoreKey(
                new ConnectPoint(deviceId, outPort), prefix.address()));
        if (dummyVlan == null) {
            log.error("Failed to get dummyVlanId for host {} at {}/{}.",
                      prefix.address(), deviceId, outPort);
        } else {
            srManager.routingRulePopulator.revokeDoubleTaggedRoute(
                    deviceId, prefix, hostMac, dummyVlan, innerVlan, outerVlan, outerTpid, outPort);
            srManager.routingRulePopulator.processDoubleTaggedFilter(
                    deviceId, outPort, outerVlan, innerVlan, false);
        }
    }


    /**
     * Remove ECMP graph entry for the given device. Typically called when
     * device is no longer available.
     *
     * @param deviceId the device for which graphs need to be purged
     */
    void purgeEcmpGraph(DeviceId deviceId) {
        statusLock.lock();
        try {
            if (populationStatus == Status.STARTED) {
                log.warn("Previous rule population is not finished. Cannot"
                        + " proceeed with purgeEcmpGraph for {}", deviceId);
                return;
            }
            log.debug("Updating ECMPspg for unavailable dev:{}", deviceId);
            currentEcmpSpgMap.remove(deviceId);
            if (updatedEcmpSpgMap != null) {
                updatedEcmpSpgMap.remove(deviceId);
            }
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Attempts a full reroute of route-paths if topology has changed relatively
     * close to a mastership change event. Does not do a reroute if mastership
     * change is due to reasons other than a ONOS cluster event - for example a
     * call to balance-masters, or a switch up/down event.
     *
     * @param devId the device identifier for which mastership has changed
     * @param me the mastership event
     */
    void checkFullRerouteForMasterChange(DeviceId devId, MastershipEvent me) {
        // give small delay to absorb mastership events that are caused by
        // device that has disconnected from cluster
        executorServiceMstChg.schedule(new MasterChange(devId, me),
                                       MASTER_CHANGE_DELAY, TimeUnit.MILLISECONDS);
    }

    protected final class MasterChange implements Runnable {
        private DeviceId devId;
        private MastershipEvent me;
        private static final long CLUSTER_EVENT_THRESHOLD = 4500; // ms
        private static final long DEVICE_EVENT_THRESHOLD = 2000; // ms
        private static final long EDGE_PORT_EVENT_THRESHOLD = 10000; //ms
        private static final long FULL_REROUTE_THRESHOLD = 10000; // ms

        MasterChange(DeviceId devId, MastershipEvent me) {
            this.devId = devId;
            this.me = me;
        }

        @Override
        public void run() {
            long lce = srManager.clusterListener.timeSinceLastClusterEvent();
            boolean clusterEvent = lce < CLUSTER_EVENT_THRESHOLD;

            // ignore event for lost switch if cluster event hasn't happened -
            // device down event will handle it
            if ((me.roleInfo().master() == null
                    || !srManager.deviceService.isAvailable(devId))
                    && !clusterEvent) {
                log.debug("Full reroute not required for lost device: {}/{} "
                        + "clusterEvent/timeSince: {}/{}",
                          devId, me.roleInfo(), clusterEvent, lce);
                return;
            }

            long update = srManager.deviceService.getLastUpdatedInstant(devId);
            long lde = Instant.now().toEpochMilli() - update;
            boolean deviceEvent = lde < DEVICE_EVENT_THRESHOLD;

            // ignore event for recently connected switch if cluster event hasn't
            // happened - link up events will handle it
            if (srManager.deviceService.isAvailable(devId) && deviceEvent
                    && !clusterEvent) {
                log.debug("Full reroute not required for recently available"
                        + " device: {}/{} deviceEvent/timeSince: {}/{} "
                        + "clusterEvent/timeSince: {}/{}",
                        devId, me.roleInfo(), deviceEvent, lde, clusterEvent, lce);
                return;
            }

            long lepe = Instant.now().toEpochMilli()
                    - srManager.lastEdgePortEvent.toEpochMilli();
            boolean edgePortEvent = lepe < EDGE_PORT_EVENT_THRESHOLD;

            // if it gets here, then mastership change is likely due to onos
            // instance failure, or network partition in onos cluster
            // normally a mastership change like this does not require re-programming
            // but if topology changes happen at the same time then we may miss events
            if (!isRoutingStable() && clusterEvent) {
                log.warn("Mastership changed for dev: {}/{} while programming route-paths "
                        + "due to clusterEvent {} ms ago .. attempting full reroute",
                         devId, me.roleInfo(), lce);
                if (srManager.mastershipService.isLocalMaster(devId)) {
                    // old master could have died when populating filters
                    populatePortAddressingRules(devId);
                }
                // old master could have died when creating groups
                // XXX right now we have no fine-grained way to only make changes
                // for the route paths affected by this device. Thus we do a
                // full reroute after purging all hash groups. We also try to do
                // it only once, irrespective of the number of devices
                // that changed mastership when their master instance died.
                long lfrr = Instant.now().toEpochMilli() - lastFullReroute.toEpochMilli();
                boolean doFullReroute = lfrr > FULL_REROUTE_THRESHOLD;
                if (doFullReroute) {
                    lastFullReroute = Instant.now();
                    for (Device dev : srManager.deviceService.getDevices()) {
                        if (shouldProgram(dev.id())) {
                            srManager.purgeHashedNextObjectiveStore(dev.id());
                        }
                    }
                    // give small delay to ensure entire store is purged
                    executorServiceFRR.schedule(new FullRerouteAfterPurge(),
                                                PURGE_DELAY,
                                                TimeUnit.MILLISECONDS);
                } else {
                    log.warn("Full reroute attempted {} ms ago .. skipping", lfrr);
                }

            } else if (edgePortEvent && clusterEvent) {
                log.warn("Mastership changed for dev: {}/{} due to clusterEvent {} ms ago "
                        + "while edge-port event happened {} ms ago "
                        + " .. reprogramming all edge-ports",
                         devId, me.roleInfo(), lce, lepe);
                if (shouldProgram(devId)) {
                    srManager.deviceService.getPorts(devId).stream()
                        .filter(p -> srManager.interfaceService
                                .isConfigured(new ConnectPoint(devId, p.number())))
                        .forEach(p -> srManager.processPortUpdated(devId, p));
                }

            } else {
                log.debug("Stable route-paths .. full reroute not attempted for "
                        + "mastership change {}/{} deviceEvent/timeSince: {}/{} "
                        + "clusterEvent/timeSince: {}/{}", devId, me.roleInfo(),
                        deviceEvent, lde, clusterEvent, lce);
            }
        }
    }

    /**
     * Performs a full reroute of routing rules in all the switches. Assumes
     * caller has purged hash groups from the nextObjective store, otherwise
     * re-uses ones available in the store.
     */
    protected final class FullRerouteAfterPurge implements Runnable {
        @Override
        public void run() {
            populateAllRoutingRules();
        }
    }


    //////////////////////////////////////
    //  Routing helper methods and classes
    //////////////////////////////////////

    /**
     * Computes set of affected routes due to failed link. Assumes previous ecmp
     * shortest-path graph exists for a switch in order to compute affected
     * routes. If such a graph does not exist, the method returns null.
     *
     * @param linkFail the failed link
     * @return the set of affected routes which may be empty if no routes were
     *         affected
     */
    private Set<ArrayList<DeviceId>> computeDamagedRoutes(Link linkFail) {
        Set<ArrayList<DeviceId>> routes = new HashSet<>();

        for (Device sw : srManager.deviceService.getDevices()) {
            log.debug("Computing the impacted routes for device {} due to link fail",
                      sw.id());
            if (!shouldProgram(sw.id())) {
                lastProgrammed.remove(sw.id());
                continue;
            }
            for (DeviceId rootSw : deviceAndItsPair(sw.id())) {
                // check for mastership change since last run
                if (!lastProgrammed.contains(sw.id())) {
                    log.warn("New responsibility for this node to program dev:{}"
                            + " ... nuking current ECMPspg", sw.id());
                    currentEcmpSpgMap.remove(sw.id());
                }
                lastProgrammed.add(sw.id());

                EcmpShortestPathGraph ecmpSpg = currentEcmpSpgMap.get(rootSw);
                if (ecmpSpg == null) {
                    log.warn("No existing ECMP graph for switch {}. Assuming "
                            + "all route-paths have changed towards it.", rootSw);
                    for (DeviceId targetSw : srManager.deviceConfiguration.getRouters()) {
                        if (targetSw.equals(rootSw)) {
                            continue;
                        }
                        routes.add(Lists.newArrayList(targetSw, rootSw));
                        log.debug("Impacted route:{}->{}", targetSw, rootSw);
                    }
                    continue;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Root switch: {}", rootSw);
                    log.debug("  Current/Existing SPG: {}", ecmpSpg);
                    log.debug("       New/Updated SPG: {}", updatedEcmpSpgMap.get(rootSw));
                }
                HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>>
                    switchVia = ecmpSpg.getAllLearnedSwitchesAndVia();
                // figure out if the broken link affected any route-paths in this graph
                for (Integer itrIdx : switchVia.keySet()) {
                    log.trace("Current/Exiting SPG Iterindex# {}", itrIdx);
                    HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap =
                            switchVia.get(itrIdx);
                    for (DeviceId targetSw : swViaMap.keySet()) {
                        log.trace("TargetSwitch {} --> RootSwitch {}",
                                  targetSw, rootSw);
                        for (ArrayList<DeviceId> via : swViaMap.get(targetSw)) {
                            log.trace(" Via:");
                            via.forEach(e -> log.trace("  {}", e));
                        }
                        Set<ArrayList<DeviceId>> subLinks =
                                computeLinks(targetSw, rootSw, swViaMap);
                        for (ArrayList<DeviceId> alink: subLinks) {
                            if ((alink.get(0).equals(linkFail.src().deviceId()) &&
                                    alink.get(1).equals(linkFail.dst().deviceId()))
                                    ||
                                    (alink.get(0).equals(linkFail.dst().deviceId()) &&
                                         alink.get(1).equals(linkFail.src().deviceId()))) {
                                log.debug("Impacted route:{}->{}", targetSw, rootSw);
                                ArrayList<DeviceId> aRoute = new ArrayList<>();
                                aRoute.add(targetSw); // switch with rules to populate
                                aRoute.add(rootSw); // towards this destination
                                routes.add(aRoute);
                                break;
                            }
                        }
                    }
                }

            }

        }
        return routes;
    }

    /**
     * Computes set of affected routes due to new links or failed switches.
     *
     * @param failedSwitch deviceId of failed switch if any
     * @return the set of affected routes which may be empty if no routes were
     *         affected
     */
    private Set<ArrayList<DeviceId>> computeRouteChange(DeviceId failedSwitch) {
        ImmutableSet.Builder<ArrayList<DeviceId>> changedRtBldr =
                ImmutableSet.builder();

        for (Device sw : srManager.deviceService.getDevices()) {
            log.debug("Computing the impacted routes for device {}", sw.id());
            if (!shouldProgram(sw.id())) {
                lastProgrammed.remove(sw.id());
                continue;
            }
            for (DeviceId rootSw : deviceAndItsPair(sw.id())) {
                if (log.isTraceEnabled()) {
                    log.trace("Device links for dev: {}", rootSw);
                    for (Link link: srManager.linkService.getDeviceLinks(rootSw)) {
                        log.trace("{} -> {} ", link.src().deviceId(),
                                  link.dst().deviceId());
                    }
                }
                // check for mastership change since last run
                if (!lastProgrammed.contains(sw.id())) {
                    log.warn("New responsibility for this node to program dev:{}"
                            + " ... nuking current ECMPspg", sw.id());
                    currentEcmpSpgMap.remove(sw.id());
                }
                lastProgrammed.add(sw.id());
                EcmpShortestPathGraph currEcmpSpg = currentEcmpSpgMap.get(rootSw);
                if (currEcmpSpg == null) {
                    log.debug("No existing ECMP graph for device {}.. adding self as "
                            + "changed route", rootSw);
                    changedRtBldr.add(Lists.newArrayList(rootSw));
                    continue;
                }
                EcmpShortestPathGraph newEcmpSpg = updatedEcmpSpgMap.get(rootSw);
                if (newEcmpSpg == null) {
                    log.warn("Cannot find updated ECMP graph for dev:{}", rootSw);
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Root switch: {}", rootSw);
                    log.debug("  Current/Existing SPG: {}", currEcmpSpg);
                    log.debug("       New/Updated SPG: {}", newEcmpSpg);
                }
                // first use the updated/new map to compare to current/existing map
                // as new links may have come up
                changedRtBldr.addAll(compareGraphs(newEcmpSpg, currEcmpSpg, rootSw));
                // then use the current/existing map to compare to updated/new map
                // as switch may have been removed
                changedRtBldr.addAll(compareGraphs(currEcmpSpg, newEcmpSpg, rootSw));
            }
        }

        // handle clearing state for a failed switch in case the switch does
        // not have a pair, or the pair is not available
        if (failedSwitch != null) {
            Optional<DeviceId> pairDev = srManager.getPairDeviceId(failedSwitch);
            if (!pairDev.isPresent() || !srManager.deviceService.isAvailable(pairDev.get())) {
                log.debug("Proxy Route changes to downed Sw:{}", failedSwitch);
                srManager.deviceService.getDevices().forEach(dev -> {
                    if (!dev.id().equals(failedSwitch) &&
                            srManager.mastershipService.isLocalMaster(dev.id())) {
                        log.debug(" : {}", dev.id());
                        changedRtBldr.add(Lists.newArrayList(dev.id(), failedSwitch));
                    }
                });
            }
        }

        Set<ArrayList<DeviceId>> changedRoutes = changedRtBldr.build();
        for (ArrayList<DeviceId> route: changedRoutes) {
            log.debug("Route changes Target -> Root");
            if (route.size() == 1) {
                log.debug(" : all -> {}", route.get(0));
            } else {
                log.debug(" : {} -> {}", route.get(0), route.get(1));
            }
        }
        return changedRoutes;
    }

    /**
     * For the root switch, searches all the target nodes reachable in the base
     * graph, and compares paths to the ones in the comp graph.
     *
     * @param base the graph that is indexed for all reachable target nodes
     *             from the root node
     * @param comp the graph that the base graph is compared to
     * @param rootSw  both ecmp graphs are calculated for the root node
     * @return all the routes that have changed in the base graph
     */
    private Set<ArrayList<DeviceId>> compareGraphs(EcmpShortestPathGraph base,
                                                   EcmpShortestPathGraph comp,
                                                   DeviceId rootSw) {
        ImmutableSet.Builder<ArrayList<DeviceId>> changedRoutesBuilder =
                ImmutableSet.builder();
        HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> baseMap =
                base.getAllLearnedSwitchesAndVia();
        HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> compMap =
                comp.getAllLearnedSwitchesAndVia();
        for (Integer itrIdx : baseMap.keySet()) {
            HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> baseViaMap =
                    baseMap.get(itrIdx);
            for (DeviceId targetSw : baseViaMap.keySet()) {
                ArrayList<ArrayList<DeviceId>> basePath = baseViaMap.get(targetSw);
                ArrayList<ArrayList<DeviceId>> compPath = getVia(compMap, targetSw);
                if ((compPath == null) || !basePath.equals(compPath)) {
                    log.trace("Impacted route:{} -> {}", targetSw, rootSw);
                    ArrayList<DeviceId> route = new ArrayList<>();
                    route.add(targetSw); // switch with rules to populate
                    route.add(rootSw); // towards this destination
                    changedRoutesBuilder.add(route);
                }
            }
        }
        return changedRoutesBuilder.build();
    }

    /**
     * Returns the ECMP paths traversed to reach the target switch.
     *
     * @param switchVia a per-iteration view of the ECMP graph for a root switch
     * @param targetSw the switch to reach from the root switch
     * @return the nodes traversed on ECMP paths to the target switch
     */
    private ArrayList<ArrayList<DeviceId>> getVia(HashMap<Integer, HashMap<DeviceId,
            ArrayList<ArrayList<DeviceId>>>> switchVia, DeviceId targetSw) {
        for (Integer itrIdx : switchVia.keySet()) {
            HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap =
                    switchVia.get(itrIdx);
            if (swViaMap.get(targetSw) == null) {
                continue;
            } else {
                return swViaMap.get(targetSw);
            }
        }

        return null;
    }

    /**
     * Utility method to break down a path from src to dst device into a collection
     * of links.
     *
     * @param src src device of the path
     * @param dst dst device of the path
     * @param viaMap path taken from src to dst device
     * @return collection of links in the path
     */
    private Set<ArrayList<DeviceId>> computeLinks(DeviceId src,
                                                  DeviceId dst,
                       HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> viaMap) {
        Set<ArrayList<DeviceId>> subLinks = Sets.newHashSet();
        for (ArrayList<DeviceId> via : viaMap.get(src)) {
            DeviceId linkSrc = src;
            DeviceId linkDst = dst;
            for (DeviceId viaDevice: via) {
                ArrayList<DeviceId> link = new ArrayList<>();
                linkDst = viaDevice;
                link.add(linkSrc);
                link.add(linkDst);
                subLinks.add(link);
                linkSrc = viaDevice;
            }
            ArrayList<DeviceId> link = new ArrayList<>();
            link.add(linkSrc);
            link.add(dst);
            subLinks.add(link);
        }

        return subLinks;
    }

    /**
     * Determines whether this controller instance should program the
     * given {@code deviceId}, based on mastership and pairDeviceId if one exists.
     * <p>
     * Once an instance is elected, it will be the only instance responsible for programming
     * both devices in the pair until it goes down.
     *
     * @param deviceId device identifier to consider for routing
     * @return true if current instance should handle the routing for given device
     */
    boolean shouldProgram(DeviceId deviceId) {
        Boolean cached = shouldProgramCache.get(deviceId);
        if (cached != null) {
            log.debug("shouldProgram dev:{} cached:{}", deviceId, cached);
            return cached;
        }

        Optional<DeviceId> pairDeviceId = srManager.getPairDeviceId(deviceId);

        NodeId currentNodeId = srManager.clusterService.getLocalNode().id();
        NodeId masterNodeId = srManager.mastershipService.getMasterFor(deviceId);
        Optional<NodeId> pairMasterNodeId = pairDeviceId.map(srManager.mastershipService::getMasterFor);
        log.debug("Evaluate shouldProgram {}/pair={}. currentNodeId={}, master={}, pairMaster={}",
                deviceId, pairDeviceId, currentNodeId, masterNodeId, pairMasterNodeId);

        // No pair device configured. Only handle when current instance is the master of the device
        if (!pairDeviceId.isPresent()) {
            log.debug("No pair device. currentNodeId={}, master={}", currentNodeId, masterNodeId);
            return currentNodeId.equals(masterNodeId);
        }

        // Should not handle if current instance is not the master of either switch
        if (!currentNodeId.equals(masterNodeId) &&
                !(pairMasterNodeId.isPresent() && currentNodeId.equals(pairMasterNodeId.get()))) {
            log.debug("Current nodeId {} is neither the master of target device {} nor pair device {}",
                    currentNodeId, deviceId, pairDeviceId);
            return false;
        }

        Set<DeviceId> key = Sets.newHashSet(deviceId, pairDeviceId.get());

        NodeId king = shouldProgram.compute(key, ((k, v) -> {
            if (v == null) {
                // There is no value in the map. Elect a node
                return elect(Lists.newArrayList(masterNodeId, pairMasterNodeId.orElse(null)));
            } else {
                if (v.equals(masterNodeId) || v.equals(pairMasterNodeId.orElse(null))) {
                    // Use the node in the map if it is still alive and is a master of any of the two switches
                    return v;
                } else {
                    // Previously elected node is no longer the master of either switch. Re-elect a node.
                    return elect(Lists.newArrayList(masterNodeId, pairMasterNodeId.orElse(null)));
                }
            }
        }));

        if (king != null) {
            log.debug("{} is king, should handle routing for {}/pair={}", king, deviceId, pairDeviceId);
            shouldProgramCache.put(deviceId, king.equals(currentNodeId));
            return king.equals(currentNodeId);
        } else {
            log.error("Fail to elect a king for {}/pair={}. Abort.", deviceId, pairDeviceId);
            shouldProgramCache.remove(deviceId);
            return false;
        }
    }

    /**
     * Elects a node who should take responsibility of programming devices.
     * @param nodeIds list of candidate node ID
     *
     * @return NodeId of the node that gets elected, or null if none of the node can be elected
     */
    private NodeId elect(List<NodeId> nodeIds) {
        // Remove all null elements. This could happen when some device has no master
        nodeIds.removeAll(Collections.singleton(null));
        nodeIds.sort(null);
        return nodeIds.size() == 0 ? null : nodeIds.get(0);
    }

    void invalidateShouldProgramCache(DeviceId deviceId) {
        shouldProgramCache.remove(deviceId);
    }

    /**
     * Returns a set of device ID, containing given device and its pair device if exist.
     *
     * @param deviceId Device ID
     * @return a set of device ID, containing given device and its pair device if exist.
     */
    private Set<DeviceId> deviceAndItsPair(DeviceId deviceId) {
        Set<DeviceId> ret = Sets.newHashSet(deviceId);
        srManager.getPairDeviceId(deviceId).ifPresent(ret::add);
        return ret;
    }

    /**
     * Returns the set of deviceIds which are the next hops from the targetSw
     * to the dstSw according to the latest ECMP spg.
     *
     * @param targetSw the switch for which the next-hops are desired
     * @param dstSw the switch to which the next-hops lead to from the targetSw
     * @return set of next hop deviceIds, could be empty if no next hops are found
     */
    private Set<DeviceId> getNextHops(DeviceId targetSw, DeviceId dstSw) {
        boolean targetIsEdge = false;
        try {
            targetIsEdge = srManager.deviceConfiguration.isEdgeDevice(targetSw);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + "Cannot determine if targetIsEdge {}.. "
                    + "continuing to getNextHops", targetSw);
        }

        EcmpShortestPathGraph ecmpSpg = updatedEcmpSpgMap.get(dstSw);
        if (ecmpSpg == null) {
            log.debug("No ecmpSpg found for dstSw: {}", dstSw);
            return ImmutableSet.of();
        }
        HashMap<Integer,
            HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchVia =
                ecmpSpg.getAllLearnedSwitchesAndVia();
        for (Integer itrIdx : switchVia.keySet()) {
            HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap =
                    switchVia.get(itrIdx);
            for (DeviceId target : swViaMap.keySet()) {
                if (!target.equals(targetSw)) {
                    continue;
                }
                // optimization for spines to not use leaves to get
                // to a spine or other leaves. Also leaves should not use other
                // leaves to get to the destination
                if ((!targetIsEdge && itrIdx > 1) || targetIsEdge) {
                    boolean pathdevIsEdge = false;
                    for (ArrayList<DeviceId> via : swViaMap.get(targetSw)) {
                        log.debug("Evaluating next-hop in path: {}", via);
                        for (DeviceId pathdev : via) {
                            try {
                                pathdevIsEdge = srManager.deviceConfiguration
                                        .isEdgeDevice(pathdev);
                            } catch (DeviceConfigNotFoundException e) {
                                log.warn(e.getMessage());
                            }
                            if (pathdevIsEdge) {
                                log.debug("Avoiding {} hop path for targetSw:{}"
                                        + " --> dstSw:{} which goes through an edge"
                                        + " device {} in path {}", itrIdx,
                                          targetSw, dstSw, pathdev, via);
                                return ImmutableSet.of();
                            }
                        }
                    }
                }
                Set<DeviceId> nextHops = new HashSet<>();
                for (ArrayList<DeviceId> via : swViaMap.get(targetSw)) {
                    if (via.isEmpty()) {
                        // the dstSw is the next-hop from the targetSw
                        nextHops.add(dstSw);
                    } else {
                        // first elem is next-hop in each ECMP path
                        nextHops.add(via.get(0));
                    }
                }
                log.debug("target {} --> dst: {} has next-hops:{}", targetSw,
                          dstSw, nextHops);
                return nextHops;
            }
        }
        log.debug("No next hops found for target:{} --> dst: {}", targetSw, dstSw);
        return ImmutableSet.of(); //no next-hops found
    }

    //////////////////////////////////////
    //  Filtering rule creation
    //////////////////////////////////////

    /**
     * Populates filtering rules for port, and punting rules
     * for gateway IPs, loopback IPs and arp/ndp traffic.
     * Should only be called by the master instance for this device/port.
     *
     * @param deviceId Switch ID to set the rules
     */
    void populatePortAddressingRules(DeviceId deviceId) {
        // Although device is added, sometimes device store does not have the
        // ports for this device yet. It results in missing filtering rules in the
        // switch. We will attempt it a few times. If it still does not work,
        // user can manually repopulate using CLI command sr-reroute-network
        PortFilterInfo firstRun = rulePopulator.populateVlanMacFilters(deviceId);
        if (firstRun == null) {
            firstRun = new PortFilterInfo(0, 0, 0);
        }
        executorService.schedule(new RetryFilters(deviceId, firstRun),
                                 RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * RetryFilters populates filtering objectives for a device and keeps retrying
     * till the number of ports filtered are constant for a predefined number
     * of attempts.
     */
    protected final class RetryFilters implements Runnable {
        int constantAttempts = MAX_CONSTANT_RETRY_ATTEMPTS;
        DeviceId devId;
        int counter;
        PortFilterInfo prevRun;

        private RetryFilters(DeviceId deviceId, PortFilterInfo previousRun) {
            devId = deviceId;
            prevRun = previousRun;
            counter = 0;
        }

        @Override
        public void run() {
            log.debug("RETRY FILTER ATTEMPT {} ** dev:{}", ++counter, devId);
            PortFilterInfo thisRun = rulePopulator.populateVlanMacFilters(devId);
            boolean sameResult = prevRun.equals(thisRun);
            log.debug("dev:{} prevRun:{} thisRun:{} sameResult:{}", devId, prevRun,
                      thisRun, sameResult);
            if (thisRun == null || !sameResult || (--constantAttempts > 0)) {
                // exponentially increasing intervals for retries
                executorService.schedule(this,
                    RETRY_INTERVAL_MS * (int) Math.pow(counter, RETRY_INTERVAL_SCALE),
                    TimeUnit.MILLISECONDS);
                if (!sameResult) {
                    constantAttempts = MAX_CONSTANT_RETRY_ATTEMPTS; //reset
                }
            }
            prevRun = (thisRun == null) ? prevRun : thisRun;
        }
    }
}
