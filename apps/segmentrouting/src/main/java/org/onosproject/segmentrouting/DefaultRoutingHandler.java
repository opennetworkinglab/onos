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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.joda.time.DateTime;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Default routing handler that is responsible for route computing and
 * routing rule population.
 */
public class DefaultRoutingHandler {
    private static final int MAX_CONSTANT_RETRY_ATTEMPTS = 5;
    private static final int RETRY_INTERVAL_MS = 250;
    private static final int RETRY_INTERVAL_SCALE = 1;
    private static final long STABLITY_THRESHOLD = 10; //secs
    private static final int UPDATE_INTERVAL = 5; //secs
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
    private DateTime lastRoutingChange;

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
    public DefaultRoutingHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.rulePopulator = checkNotNull(srManager.routingRulePopulator);
        this.config = checkNotNull(srManager.deviceConfiguration);
        this.populationStatus = Status.IDLE;
        this.currentEcmpSpgMap = Maps.newHashMap();
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
       long last = (long) (lastRoutingChange.getMillis() / 1000.0);
       long now = (long) (DateTime.now().getMillis() / 1000.0);
       log.trace("Routing stable since {}s", now - last);
       return (now - last) > STABLITY_THRESHOLD;
   }


    //////////////////////////////////////
    //  Route path handling
    //////////////////////////////////////

    /* The following three methods represent the three major ways in routing
     * is triggered in the network
     *      a) due to configuration change
     *      b) due to route-added event
     *      c) due to change in the topology
     */

    /**
     * Populates all routing rules to all switches. Typically triggered at
     * startup or after a configuration event.
     */
    public void populateAllRoutingRules() {
        lastRoutingChange = DateTime.now();
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
            for (Device dstSw : srManager.deviceService.getDevices()) {
                EcmpShortestPathGraph ecmpSpgUpdated =
                        new EcmpShortestPathGraph(dstSw.id(), srManager);
                updatedEcmpSpgMap.put(dstSw.id(), ecmpSpgUpdated);
                DeviceId pairDev = getPairDev(dstSw.id());
                if (pairDev != null) {
                    // pairDev may not be available yet, but we still need to add
                    ecmpSpgUpdated = new EcmpShortestPathGraph(pairDev, srManager);
                    updatedEcmpSpgMap.put(pairDev, ecmpSpgUpdated);
                    edgePairs.add(new EdgePair(dstSw.id(), pairDev));
                }
                DeviceId ret = shouldHandleRouting(dstSw.id());
                if (ret == null) {
                    continue;
                }
                Set<DeviceId> devsToProcess = Sets.newHashSet(dstSw.id(), ret);
                // To do a full reroute, assume all routes have changed
                for (DeviceId dev : devsToProcess) {
                    for (Device targetSw : srManager.deviceService.getDevices()) {
                        if (targetSw.id().equals(dev)) {
                            continue;
                        }
                        routeChanges.add(Lists.newArrayList(targetSw.id(), dev));
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

        lastRoutingChange = DateTime.now();
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
            // Take snapshots of the topology
            updatedEcmpSpgMap = new HashMap<>();
            Set<EdgePair> edgePairs = new HashSet<>();
            Set<ArrayList<DeviceId>> routeChanges = new HashSet<>();
            boolean handleRouting = false;

            if (cpts.size() == 2) {
                // ensure connect points are edge-pairs
                Iterator<ConnectPoint> iter = cpts.iterator();
                DeviceId dev1 = iter.next().deviceId();
                DeviceId pairDev = getPairDev(dev1);
                if (iter.next().deviceId().equals(pairDev)) {
                    edgePairs.add(new EdgePair(dev1, pairDev));
                } else {
                    log.warn("Connectpoints {} for subnets {} not on "
                            + "pair-devices.. aborting populateSubnet", cpts, subnets);
                    populationStatus = Status.ABORTED;
                    return;
                }
                for (ConnectPoint cp : cpts) {
                    EcmpShortestPathGraph ecmpSpgUpdated =
                            new EcmpShortestPathGraph(cp.deviceId(), srManager);
                    updatedEcmpSpgMap.put(cp.deviceId(), ecmpSpgUpdated);
                    DeviceId retId = shouldHandleRouting(cp.deviceId());
                    if (retId == null) {
                        continue;
                    }
                    handleRouting = true;
                }
            } else {
                // single connect point
                DeviceId dstSw = cpts.iterator().next().deviceId();
                EcmpShortestPathGraph ecmpSpgUpdated =
                        new EcmpShortestPathGraph(dstSw, srManager);
                updatedEcmpSpgMap.put(dstSw, ecmpSpgUpdated);
                if (srManager.mastershipService.isLocalMaster(dstSw)) {
                    handleRouting = true;
                }
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
                        log.warn(e.getMessage() + "aborting populateSubnet");
                        populationStatus = Status.ABORTED;
                        return;
                    }
                    if (dstSw.equals(targetSw.id()) || !isEdge ||
                            (cpts.size() == 2 &&
                                targetSw.id().equals(getPairDev(dstSw)))) {
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
     * Note that when a switch goes away, all of its links fail as well,
     * but this is handled as a single switch removal event.
     *
     * @param linkDown the single failed link, or null for other conditions
     *                  such as link-up or a removed switch
     * @param linkUp the single link up, or null for other conditions such as
     *                  link-down or a removed switch
     * @param switchDown the removed switch, or null for other conditions such as
     *                  link-down or link-up
     */ // refactor
    public void populateRoutingRulesForLinkStatusChange(Link linkDown,
                                                           Link linkUp,
                                                           DeviceId switchDown) {
        if ((linkDown != null && (linkUp != null || switchDown != null)) ||
                (linkUp != null && (linkDown != null || switchDown != null)) ||
                (switchDown != null && (linkUp != null || linkDown != null))) {
            log.warn("Only one event can be handled for link status change .. aborting");
            return;
        }
        lastRoutingChange = DateTime.now();
        executorService.schedule(new UpdateMaps(), UPDATE_INTERVAL,
                                 TimeUnit.SECONDS);
        statusLock.lock();
        try {

            if (populationStatus == Status.STARTED) {
                log.warn("Previous rule population is not finished. Cannot"
                        + " proceeed with routingRules for Link Status change");
                return;
            }

            // Take snapshots of the topology
            updatedEcmpSpgMap = new HashMap<>();
            Set<EdgePair> edgePairs = new HashSet<>();
            for (Device sw : srManager.deviceService.getDevices()) {
                EcmpShortestPathGraph ecmpSpgUpdated =
                        new EcmpShortestPathGraph(sw.id(), srManager);
                updatedEcmpSpgMap.put(sw.id(), ecmpSpgUpdated);
                DeviceId pairDev = getPairDev(sw.id());
                if (pairDev != null) {
                    // pairDev may not be available yet, but we still need to add
                    ecmpSpgUpdated = new EcmpShortestPathGraph(pairDev, srManager);
                    updatedEcmpSpgMap.put(pairDev, ecmpSpgUpdated);
                    edgePairs.add(new EdgePair(sw.id(), pairDev));
                }
            }

            log.info("Starting to populate routing rules from link status change");

            Set<ArrayList<DeviceId>> routeChanges;
            log.debug("populateRoutingRulesForLinkStatusChange: "
                    + "populationStatus is STARTED");
            populationStatus = Status.STARTED;
            rulePopulator.resetCounter();
            // try optimized re-routing
            if (linkDown == null) {
                // either a linkUp or a switchDown - compute all route changes by
                // comparing all routes of existing ECMP SPG to new ECMP SPG
                routeChanges = computeRouteChange();

                // deal with linkUp of a seen-before link
                if (linkUp != null && srManager.isSeenLink(linkUp)) {
                    if (!srManager.isBidirectional(linkUp)) {
                        log.warn("Not a bidirectional link yet .. not "
                                + "processing link {}", linkUp);
                        srManager.updateSeenLink(linkUp, true);
                        populationStatus = Status.ABORTED;
                        return;
                    }
                    // link previously seen before
                    // do hash-bucket changes instead of a re-route
                    processHashGroupChange(routeChanges, false, null);
                    // clear out routesChanges so a re-route is not attempted
                    routeChanges = ImmutableSet.of();
                }
                // for a linkUp of a never-seen-before link
                // let it fall through to a reroute of the routeChanges

                // now that we are past the check for a previously seen link
                // it is safe to update the store for the linkUp
                if (linkUp != null) {
                    srManager.updateSeenLink(linkUp, true);
                }

                //deal with switchDown
                if (switchDown != null) {
                    processHashGroupChange(routeChanges, true, switchDown);
                    // clear out routesChanges so a re-route is not attempted
                    routeChanges = ImmutableSet.of();
                }
            } else {
                // link has gone down
                // Compare existing ECMP SPG only with the link that went down
                routeChanges = computeDamagedRoutes(linkDown);
                if (routeChanges != null) {
                    processHashGroupChange(routeChanges, true, null);
                    // clear out routesChanges so a re-route is not attempted
                    routeChanges = ImmutableSet.of();
                }
            }

            // do full re-routing if optimized routing returns null routeChanges
            if (routeChanges == null) {
                log.info("Optimized routing failed... opting for full reroute");
                populationStatus = Status.ABORTED;
                populateAllRoutingRules();
                return;
            }

            if (routeChanges.isEmpty()) {
                log.info("No re-route attempted for the link status change");
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
        if (!redoRoutingIndividualDests(subnets, changedRoutes)) {
            return false; //abort routing and fail fast
        }

        // update ecmpSPG for all edge-pairs
        for (EdgePair ep : edgePairs) {
            currentEcmpSpgMap.put(ep.dev1, updatedEcmpSpgMap.get(ep.dev1));
            currentEcmpSpgMap.put(ep.dev2, updatedEcmpSpgMap.get(ep.dev2));
            log.debug("Updating ECMPspg for edge-pair:{}-{}", ep.dev1, ep.dev2);
        }
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
                // handle routing to subnets common to edge-pair
                // only if the targetSw is not part of the edge-pair
                if (!ep.includes(targetSw)) {
                    if (!populateEcmpRoutingRulePartial(
                             targetSw,
                             ep.dev1, ep.dev2,
                             perDstNextHops,
                             Sets.intersection(ipDev1, ipDev2))) {
                        return false; // abort everything and fail fast
                    }
                }
                // handle routing to subnets that only belong to dev1
                Set<IpPrefix> onlyDev1Subnets = Sets.difference(ipDev1, ipDev2);
                if (!onlyDev1Subnets.isEmpty() && perDstNextHops.get(ep.dev1) != null) {
                    Map<DeviceId, Set<DeviceId>> onlyDev1NextHops = new HashMap<>();
                    onlyDev1NextHops.put(ep.dev1, perDstNextHops.get(ep.dev1));
                    if (!populateEcmpRoutingRulePartial(
                            targetSw,
                            ep.dev1, null,
                            onlyDev1NextHops,
                            onlyDev1Subnets)) {
                        return false; // abort everything and fail fast
                    }
                }
                // handle routing to subnets that only belong to dev2
                Set<IpPrefix> onlyDev2Subnets = Sets.difference(ipDev2, ipDev1);
                if (!onlyDev2Subnets.isEmpty() && perDstNextHops.get(ep.dev2) != null) {
                    Map<DeviceId, Set<DeviceId>> onlyDev2NextHops = new HashMap<>();
                    onlyDev2NextHops.put(ep.dev2, perDstNextHops.get(ep.dev2));
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
                                               Set<ArrayList<DeviceId>> changedRoutes) {
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
     * @param nextHops Map indication a list of next hops per destSw
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
            // XXX -  Rethink this
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
            /* XXX rethink this
            IpPrefix routerIpPrefix = destRouterIpv4.toIpPrefix();
            log.debug("* populateEcmpRoutingRulePartial in device {} towards {} "
                    + "for router IP {}", targetSw, destSw, routerIpPrefix);
            result = rulePopulator.populateIpRuleForRouter(targetSw, routerIpPrefix,
                                                           destSw, nextHops);
            if (!result) {
                return false;
            }
            // If present we deal with IPv6 loopback.
            if (destRouterIpv6 != null) {
                routerIpPrefix = destRouterIpv6.toIpPrefix();
                log.debug("* populateEcmpRoutingRulePartial in device {} towards {}"
                        + " for v6 router IP {}", targetSw, destSw, routerIpPrefix);
                result = rulePopulator.populateIpRuleForRouter(targetSw, routerIpPrefix,
                                                               destSw, nextHops);
                if (!result) {
                    return false;
                }
            }*/
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
                result = rulePopulator.populateMplsRule(targetSw, destSw1,
                                                        nextHops.get(destSw1),
                                                        dest1RouterIpv6);
                if (!result) {
                    return false;
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
                    continue;
                }
                //linkfailed - update both sides
                if (success) {
                    currentEcmpSpgMap.put(targetSw, updatedEcmpSpgMap.get(targetSw));
                    currentEcmpSpgMap.put(dstSw, updatedEcmpSpgMap.get(dstSw));
                    log.debug("Updating ECMPspg for dst:{} and target:{} for linkdown",
                              dstSw, targetSw);
                }
            } else {
                //linkup of seen before link
                boolean success = fixHashGroupsForRoute(route, false);
                if (success) {
                    currentEcmpSpgMap.put(targetSw, updatedEcmpSpgMap.get(targetSw));
                    currentEcmpSpgMap.put(dstSw, updatedEcmpSpgMap.get(dstSw));
                    log.debug("Updating ECMPspg for target:{} and dst:{} for linkup",
                              targetSw, dstSw);
                }
            }
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
        log.debug("{} hash-groups buckets For Route {} -> {} to next-hops {}",
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
            return srManager.routingRulePopulator.revokeIpRuleForSubnet(subnets);
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
        if (srManager.mastershipService.isLocalMaster(deviceId)) {
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
        if (srManager.mastershipService.isLocalMaster(deviceId)) {
            srManager.routingRulePopulator.revokeRoute(deviceId, prefix, hostMac, hostVlanId, outPort);
        }
    }

    /**
     * Remove ECMP graph entry for the given device. Typically called when
     * device is no longer available.
     *
     * @param deviceId the device for which graphs need to be purged
     */
    protected void purgeEcmpGraph(DeviceId deviceId) {
        currentEcmpSpgMap.remove(deviceId); // XXX reconsider
        if (updatedEcmpSpgMap != null) {
            updatedEcmpSpgMap.remove(deviceId);
        }
    }

    //////////////////////////////////////
    //  Routing helper methods and classes
    //////////////////////////////////////

    /**
     * Computes set of affected routes due to failed link. Assumes
     * previous ecmp shortest-path graph exists for a switch in order to compute
     * affected routes. If such a graph does not exist, the method returns null.
     *
     * @param linkFail the failed link
     * @return the set of affected routes which may be empty if no routes were
     *         affected, or null if no previous ecmp spg was found for comparison
     */
    private Set<ArrayList<DeviceId>> computeDamagedRoutes(Link linkFail) {
        Set<ArrayList<DeviceId>> routes = new HashSet<>();

        for (Device sw : srManager.deviceService.getDevices()) {
            log.debug("Computing the impacted routes for device {} due to link fail",
                      sw.id());
            DeviceId retId = shouldHandleRouting(sw.id());
            if (retId == null) {
                continue;
            }
            Set<DeviceId> devicesToProcess = Sets.newHashSet(retId, sw.id());
            for (DeviceId rootSw : devicesToProcess) {
                EcmpShortestPathGraph ecmpSpg = currentEcmpSpgMap.get(rootSw);
                if (ecmpSpg == null) {
                    log.warn("No existing ECMP graph for switch {}. Aborting optimized"
                            + " rerouting and opting for full-reroute", rootSw);
                    return null;
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
     * @return the set of affected routes which may be empty if no routes were
     *         affected
     */
    private Set<ArrayList<DeviceId>> computeRouteChange() {
        ImmutableSet.Builder<ArrayList<DeviceId>> changedRtBldr =
                ImmutableSet.builder();

        for (Device sw : srManager.deviceService.getDevices()) {
            log.debug("Computing the impacted routes for device {}", sw.id());
            DeviceId retId = shouldHandleRouting(sw.id());
            if (retId == null) {
                continue;
            }
            Set<DeviceId> devicesToProcess = Sets.newHashSet(retId, sw.id());
            for (DeviceId rootSw : devicesToProcess) {
                if (log.isTraceEnabled()) {
                    log.trace("Device links for dev: {}", rootSw);
                    for (Link link: srManager.linkService.getDeviceLinks(rootSw)) {
                        log.trace("{} -> {} ", link.src().deviceId(),
                                  link.dst().deviceId());
                    }
                }
                EcmpShortestPathGraph currEcmpSpg = currentEcmpSpgMap.get(rootSw);
                if (currEcmpSpg == null) {
                    log.debug("No existing ECMP graph for device {}.. adding self as "
                            + "changed route", rootSw);
                    changedRtBldr.add(Lists.newArrayList(rootSw));
                    continue;
                }
                EcmpShortestPathGraph newEcmpSpg = updatedEcmpSpgMap.get(rootSw);
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
     * Determines whether this controller instance should handle routing for the
     * given {@code deviceId}, based on mastership and pairDeviceId if one exists.
     * Returns null if this instance should not handle routing for given {@code deviceId}.
     * Otherwise the returned value could be the given deviceId itself, or the
     * deviceId for the paired edge device. In the latter case, this instance
     * should handle routing for both the given device and the paired device.
     *
     * @param deviceId device identifier to consider for routing
     * @return null or deviceId which could be the same as the given deviceId
     *          or the deviceId of a paired edge device
     */
    private DeviceId shouldHandleRouting(DeviceId deviceId) {
        if (!srManager.mastershipService.isLocalMaster(deviceId)) {
            log.debug("Not master for dev:{} .. skipping routing, may get handled "
                    + "elsewhere as part of paired devices", deviceId);
            return null;
        }
        NodeId myNode = srManager.mastershipService.getMasterFor(deviceId);
        DeviceId pairDev = getPairDev(deviceId);

        if (pairDev != null) {
            if (!srManager.deviceService.isAvailable(pairDev)) {
                log.warn("pairedDev {} not available .. routing this dev:{} "
                        + "without mastership check",
                          pairDev, deviceId);
                return pairDev; // handle both temporarily
            }
            NodeId pairMasterNode = srManager.mastershipService.getMasterFor(pairDev);
            if (myNode.compareTo(pairMasterNode) <= 0) {
                log.debug("Handling routing for both dev:{} pair-dev:{}; myNode: {}"
                        + " pairMaster:{} compare:{}", deviceId, pairDev,
                        myNode, pairMasterNode,
                        myNode.compareTo(pairMasterNode));
                return pairDev; // handle both
            } else {
                log.debug("PairDev node: {} should handle routing for dev:{} and "
                        + "pair-dev:{}", pairMasterNode, deviceId, pairDev);
                return null; // handle neither
            }
        }
        return deviceId; // not paired, just handle given device
    }

    /**
     * Returns the configured paired DeviceId for the given Device, or null
     * if no such paired device has been configured.
     *
     * @param deviceId
     * @return configured pair deviceId or null
     */
    private DeviceId getPairDev(DeviceId deviceId) {
        DeviceId pairDev;
        try {
            pairDev = srManager.deviceConfiguration.getPairDeviceId(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " .. cannot continue routing for dev: {}");
            return null;
        }
        return pairDev;
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
                if (!targetIsEdge && itrIdx > 1) {
                    // optimization for spines to not use other leaves to get
                    // to a leaf to avoid loops
                    log.debug("Avoiding {} hop path for non-edge targetSw:{}"
                            + " --> dstSw:{}", itrIdx, targetSw, dstSw);
                    break;
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
                return nextHops;
            }
        }
        return ImmutableSet.of(); //no next-hops found
    }

    /**
     * Represents two devices that are paired by configuration. An EdgePair for
     * (dev1, dev2) is the same as as EdgePair for (dev2, dev1)
     */
    protected final class EdgePair {
        DeviceId dev1;
        DeviceId dev2;

        EdgePair(DeviceId dev1, DeviceId dev2) {
            this.dev1 = dev1;
            this.dev2 = dev2;
        }

        boolean includes(DeviceId dev) {
            return dev1.equals(dev) || dev2.equals(dev);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EdgePair)) {
                return false;
            }
            EdgePair that = (EdgePair) o;
            return ((this.dev1.equals(that.dev1) && this.dev2.equals(that.dev2)) ||
                    (this.dev1.equals(that.dev2) && this.dev2.equals(that.dev1)));
        }

        @Override
        public int hashCode() {
            if (dev1.toString().compareTo(dev2.toString()) <= 0) {
                return Objects.hash(dev1, dev2);
            } else {
                return Objects.hash(dev2, dev1);
            }
        }

        @Override
        public String toString() {
            return toStringHelper(this)
                    .add("Dev1", dev1)
                    .add("Dev2", dev2)
                    .toString();
        }
    }

    /**
     * Updates the currentEcmpSpgGraph for all devices.
     */
    private void updateEcmpSpgMaps() {
        for (Device sw : srManager.deviceService.getDevices()) {
            EcmpShortestPathGraph ecmpSpgUpdated =
                    new EcmpShortestPathGraph(sw.id(), srManager);
            currentEcmpSpgMap.put(sw.id(), ecmpSpgUpdated);
        }
    }

    /**
     * Ensures routing is stable before updating all ECMP SPG graphs.
     *
     * TODO: CORD-1843 will ensure maps are updated faster, potentially while
     * topology/routing is still unstable
     */
    private final class UpdateMaps implements Runnable {
        @Override
        public void run() {
            if (isRoutingStable()) {
                updateEcmpSpgMaps();
            } else {
                executorService.schedule(new UpdateMaps(), UPDATE_INTERVAL,
                                         TimeUnit.SECONDS);
            }
        }
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
    public void populatePortAddressingRules(DeviceId deviceId) {
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
     * Utility class used to temporarily store information about the ports on a
     * device processed for filtering objectives.
     */
    public final class PortFilterInfo {
        int disabledPorts = 0, errorPorts = 0, filteredPorts = 0;

        public PortFilterInfo(int disabledPorts, int errorPorts,
                           int filteredPorts) {
            this.disabledPorts = disabledPorts;
            this.filteredPorts = filteredPorts;
            this.errorPorts = errorPorts;
        }

        @Override
        public int hashCode() {
            return Objects.hash(disabledPorts, filteredPorts, errorPorts);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if ((obj == null) || (!(obj instanceof PortFilterInfo))) {
                return false;
            }
            PortFilterInfo other = (PortFilterInfo) obj;
            return ((disabledPorts == other.disabledPorts) &&
                    (filteredPorts == other.filteredPorts) &&
                    (errorPorts == other.errorPorts));
        }

        @Override
        public String toString() {
            MoreObjects.ToStringHelper helper = toStringHelper(this)
                    .add("disabledPorts", disabledPorts)
                    .add("errorPorts", errorPorts)
                    .add("filteredPorts", filteredPorts);
            return helper.toString();
        }
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
            if (thisRun == null || !sameResult || (sameResult && --constantAttempts > 0)) {
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
