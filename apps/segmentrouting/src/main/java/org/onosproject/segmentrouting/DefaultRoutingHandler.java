/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private static final String ECMPSPG_MISSING = "ECMP shortest path graph not found";
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
     * @return the current ECMP graph
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
     * Populates all routing rules to all connected routers, including default
     * routing rules, adjacency rules, and policy rules if any.
     */
    public void populateAllRoutingRules() {

        statusLock.lock();
        try {
            populationStatus = Status.STARTED;
            rulePopulator.resetCounter();
            log.info("Starting to populate segment-routing rules");
            log.debug("populateAllRoutingRules: populationStatus is STARTED");

            for (Device sw : srManager.deviceService.getDevices()) {
                if (!srManager.mastershipService.isLocalMaster(sw.id())) {
                    log.debug("populateAllRoutingRules: skipping device {}..."
                            + "we are not master", sw.id());
                    continue;
                }

                EcmpShortestPathGraph ecmpSpg = new EcmpShortestPathGraph(sw.id(), srManager);
                if (!populateEcmpRoutingRules(sw.id(), ecmpSpg, ImmutableSet.of())) {
                    log.debug("populateAllRoutingRules: populationStatus is ABORTED");
                    populationStatus = Status.ABORTED;
                    log.debug("Abort routing rule population");
                    return;
                }
                currentEcmpSpgMap.put(sw.id(), ecmpSpg);
                log.debug("Updating ECMPspg for sw:{}", sw.id());

                // TODO: Set adjacency routing rule for all switches
            }

            log.debug("populateAllRoutingRules: populationStatus is SUCCEEDED");
            populationStatus = Status.SUCCEEDED;
            log.info("Completed routing rule population. Total # of rules pushed : {}",
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
     */
    public void populateRoutingRulesForLinkStatusChange(Link linkDown,
                                                           Link linkUp,
                                                           DeviceId switchDown) {
        if ((linkDown != null && (linkUp != null || switchDown != null)) ||
                (linkUp != null && (linkDown != null || switchDown != null)) ||
                (switchDown != null && (linkUp != null || linkDown != null))) {
            log.warn("Only one event can be handled for link status change .. aborting");
            return;
        }

        statusLock.lock();
        try {

            if (populationStatus == Status.STARTED) {
                log.warn("Previous rule population is not finished.");
                return;
            }

            // Take the snapshots of the links
            updatedEcmpSpgMap = new HashMap<>();
            for (Device sw : srManager.deviceService.getDevices()) {
                if (!srManager.mastershipService.isLocalMaster(sw.id())) {
                    continue;
                }
                EcmpShortestPathGraph ecmpSpgUpdated =
                        new EcmpShortestPathGraph(sw.id(), srManager);
                updatedEcmpSpgMap.put(sw.id(), ecmpSpgUpdated);
            }

            log.info("Starts rule population from link change");

            Set<ArrayList<DeviceId>> routeChanges;
            log.debug("populateRoutingRulesForLinkStatusChange: "
                    + "populationStatus is STARTED");
            populationStatus = Status.STARTED;
            // try optimized re-routing
            if (linkDown == null) {
                // either a linkUp or a switchDown - compute all route changes by
                // comparing all routes of existing ECMP SPG to new ECMP SPG
                routeChanges = computeRouteChange();

                if (routeChanges != null) {
                    // deal with linkUp of a seen-before link
                    if (linkUp != null && srManager.isSeenLink(linkUp)) {
                        if (!isBidirectional(linkUp)) {
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

                    //deal with switchDown
                    if (switchDown != null) {
                        processHashGroupChange(routeChanges, true, switchDown);
                        // clear out routesChanges so a re-route is not attempted
                        routeChanges = ImmutableSet.of();
                    }

                    // for a linkUp of a never-seen-before link
                    // let it fall through to a reroute of the routeChanges

                }

                // now that we are past the check for a previously seen link
                // it is safe to update the store for the linkUp
                if (linkUp != null) {
                    srManager.updateSeenLink(linkUp, true);
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
                log.info("Optimized routing failed... doing full re-route");
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
            if (repopulateRoutingRulesForRoutes(routeChanges)) {
                log.debug("populateRoutingRulesForLinkStatusChange: populationStatus is SUCCEEDED");
                populationStatus = Status.SUCCEEDED;
                log.info("Completed repopulation of rules. # of rules populated : {}",
                        rulePopulator.getCounter());
                return;
            } else {
                log.debug("populateRoutingRulesForLinkStatusChange: populationStatus is ABORTED");
                populationStatus = Status.ABORTED;
                log.warn("Failed to repopulate the rules.");
                return;
            }
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Returns true if the link being queried is a bidirectional link. A bidi
     * link is defined as a link, whose reverse link - ie. the link in the reverse
     * direction - has been seen-before and is up.
     *
     * @param link the infrastructure link being queried
     * @return true if another unidirectional link exists in the reverse direction,
     *              has been seen-before and is up
     */
    private boolean isBidirectional(Link link) {
        Link reverseLink = srManager.linkService.getLink(link.dst(), link.src());
        if (reverseLink == null) {
            return false;
        }
        Boolean result = srManager.isSeenLinkUp(reverseLink);
        if (result == null) {
            return false;
        }
        return result.booleanValue();
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
        for (ArrayList<DeviceId> route : routeChanges) {
            DeviceId targetSw = route.get(0);
            boolean success;
            DeviceId dstSw = null;
            if (route.size() > 1) {
                dstSw = route.get(1);
            }

            if (linkOrSwitchFailed) {
                success = fixHashGroupsForRoute(route, true);
                // it's possible that we cannot fix hash groups for a route
                // if the target switch has failed. Nevertheless the ecmp graph
                // for the impacted switch must still be updated.
                if (failedSwitch != null && targetSw.equals(failedSwitch)
                        && dstSw != null) {
                    currentEcmpSpgMap.put(dstSw, updatedEcmpSpgMap.get(dstSw));
                    currentEcmpSpgMap.remove(targetSw);
                    log.debug("Updating ECMPspg for dst:{} removing failed "
                            + "target:{}", dstSw, targetSw);
                    return;
                }
                //linkfailed - update both sides
                currentEcmpSpgMap.put(targetSw, updatedEcmpSpgMap.get(targetSw));
                dstSw = route.get(1);
                currentEcmpSpgMap.put(dstSw, updatedEcmpSpgMap.get(dstSw));
                log.debug("Updating ECMPspg for dst:{} and target:{}", dstSw, targetSw);
            } else {
                success = fixHashGroupsForRoute(route, false);
                if (success) {
                    currentEcmpSpgMap.put(targetSw, updatedEcmpSpgMap.get(targetSw));
                    if (dstSw != null) {
                        currentEcmpSpgMap.put(dstSw, updatedEcmpSpgMap.get(dstSw));
                    }
                    log.debug("Updating ECMPspg for target:{} and dst:{}",
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
        log.debug("Processing fixHashGroupsForRoute: Target {} -> Dest {}",
                  targetSw, destSw);
        boolean targetIsEdge = false;
        try {
            targetIsEdge = srManager.deviceConfiguration.isEdgeDevice(targetSw);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + "Cannot determine if targetIsEdge {}.. "
                    + "continuing fixHash", targetSw);
        }

        // figure out the new next hops at the targetSw towards the destSw
        Set<DeviceId> nextHops = new HashSet<>();
        EcmpShortestPathGraph ecmpSpg = updatedEcmpSpgMap.get(destSw);
        HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchVia =
                ecmpSpg.getAllLearnedSwitchesAndVia();
        for (Integer itrIdx : switchVia.keySet()) {
            HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap =
                    switchVia.get(itrIdx);
            for (DeviceId target : swViaMap.keySet()) {
                if (target.equals(targetSw)) {
                    // found the iteration where targetSw is reached- get nextHops
                    if (!targetIsEdge && itrIdx > 1) {
                        // optimization for spines to not use other leaves to get
                        // to a leaf to avoid loops
                        log.debug("Avoiding {} hop path for non-edge targetSw:{}"
                                + " --> dstSw:{}", itrIdx, targetSw, destSw);
                        break;
                    }
                    for (ArrayList<DeviceId> via : swViaMap.get(target)) {
                        if (via.isEmpty()) {
                            nextHops.add(destSw);
                        } else {
                            // first elem is next-hop in each ECMP path
                            nextHops.add(via.get(0));
                        }
                    }
                    break;
                }
            }
        }

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
     * Processes a set a route-path changes by reprogramming routing rules and
     * creating new hash-groups if necessary.
     *
     * @param routeChanges a set of route-path changes, where each route-path is
     *                     a list with its first element the src-switch of the path
     *                     and the second element the dst-switch of the path.
     * @return true if successful in repopulating routes
     */
    private boolean repopulateRoutingRulesForRoutes(Set<ArrayList<DeviceId>> routeChanges) {
        rulePopulator.resetCounter();
        HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> routesBydevice =
                new HashMap<>();
        for (ArrayList<DeviceId> link: routeChanges) {
            // When only the source device is defined, reinstall routes to all other devices
            if (link.size() == 1) {
                log.debug("-- repopulateRoutingRulesForRoutes: running ECMP graph for device {}", link.get(0));
                EcmpShortestPathGraph ecmpSpg = new EcmpShortestPathGraph(link.get(0), srManager);
                if (populateEcmpRoutingRules(link.get(0), ecmpSpg, ImmutableSet.of())) {
                    log.debug("Populating flow rules from all to dest:{} is successful",
                              link.get(0));
                    currentEcmpSpgMap.put(link.get(0), ecmpSpg);
                    log.debug("Updating ECMPspg for dest:{}", link.get(0));
                } else {
                    log.warn("Failed to populate the flow rules from all to dest:{}", link.get(0));
                    return false;
                }
            } else {
                ArrayList<ArrayList<DeviceId>> deviceRoutes =
                        routesBydevice.get(link.get(1));
                if (deviceRoutes == null) {
                    deviceRoutes = new ArrayList<>();
                    routesBydevice.put(link.get(1), deviceRoutes);
                }
                deviceRoutes.add(link);
            }
        }

        for (DeviceId impactedDevice : routesBydevice.keySet()) {
            ArrayList<ArrayList<DeviceId>> deviceRoutes =
                    routesBydevice.get(impactedDevice);
            for (ArrayList<DeviceId> link: deviceRoutes) {
                log.debug("-- repopulateRoutingRulesForRoutes {} -> {}",
                          link.get(0), link.get(1));
                DeviceId src = link.get(0);
                DeviceId dst = link.get(1);
                EcmpShortestPathGraph ecmpSpg = updatedEcmpSpgMap.get(dst);
                HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchVia =
                        ecmpSpg.getAllLearnedSwitchesAndVia();
                for (Integer itrIdx : switchVia.keySet()) {
                    HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap =
                            switchVia.get(itrIdx);
                    for (DeviceId targetSw : swViaMap.keySet()) {
                        if (!targetSw.equals(src)) {
                            continue;
                        }
                        Set<DeviceId> nextHops = new HashSet<>();
                        for (ArrayList<DeviceId> via : swViaMap.get(targetSw)) {
                            // in this ECMP path to the targetSw, get the next hop
                            if (via.isEmpty()) {
                                nextHops.add(dst);
                            } else {
                                nextHops.add(via.get(0));
                            }
                        }
                        if (!populateEcmpRoutingRulePartial(targetSw, dst,
                                nextHops, ImmutableSet.of())) {
                            return false;
                        }
                        log.debug("Populating flow rules from {} to {} is successful",
                                  targetSw, dst);
                    }
                }
            }
            //Only if all the flows for all impacted routes to a
            //specific target are pushed successfully, update the
            //ECMP graph for that target. Or else the next event
            //would not see any changes in the ECMP graphs.
            //In another case, the target switch has gone away, so
            //routes can't be installed. In that case, the current map
            //is updated here, without any flows being pushed.
            currentEcmpSpgMap.put(impactedDevice,
                                  updatedEcmpSpgMap.get(impactedDevice));
            log.debug("Updating ECMPspg for impacted dev:{}", impactedDevice);
        }

        processHashGroupChange(routeChanges, false, null);

        return true;
    }

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
            if (!srManager.mastershipService.isLocalMaster(sw.id())) {
                log.debug("No mastership for {} .. skipping route optimization",
                          sw.id());
                continue;
            }
            EcmpShortestPathGraph ecmpSpg = currentEcmpSpgMap.get(sw.id());
            if (ecmpSpg == null) {
                log.warn("No existing ECMP graph for switch {}. Aborting optimized"
                        + " rerouting and opting for full-reroute", sw.id());
                return null;
            }
            if (log.isDebugEnabled()) {
                log.debug("Root switch: {}", sw.id());
                log.debug("  Current/Existing SPG: {}", ecmpSpg);
                log.debug("       New/Updated SPG: {}", updatedEcmpSpgMap.get(sw.id()));
            }
            HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchVia =
                    ecmpSpg.getAllLearnedSwitchesAndVia();
            for (Integer itrIdx : switchVia.keySet()) {
                log.trace("Current/Exiting SPG Iterindex# {}", itrIdx);
                HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap =
                        switchVia.get(itrIdx);
                for (DeviceId targetSw : swViaMap.keySet()) {
                    DeviceId rootSw = sw.id();
                    if (log.isTraceEnabled()) {
                        log.trace("TargetSwitch {} --> RootSwitch {}", targetSw, rootSw);
                        for (ArrayList<DeviceId> via : swViaMap.get(targetSw)) {
                            log.trace(" Via:");
                            via.forEach(e -> log.trace("  {}", e));
                        }
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

        return routes;
    }

    /**
     * Computes set of affected routes due to new links or failed switches.
     *
     * @return the set of affected routes which may be empty if no routes were
     *         affected
     */
    private Set<ArrayList<DeviceId>> computeRouteChange() {

        ImmutableSet.Builder<ArrayList<DeviceId>> changedRoutesBuilder =
                ImmutableSet.builder();

        for (Device sw : srManager.deviceService.getDevices()) {
            DeviceId rootSw = sw.id();
            log.debug("Computing the impacted routes for device {}", rootSw);
            if (!srManager.mastershipService.isLocalMaster(rootSw)) {
                log.debug("No mastership for {} ... skipping route optimization",
                          rootSw);
                continue;
            }
            if (log.isTraceEnabled()) {
                log.trace("Device links for dev: {}", rootSw);
                for (Link link: srManager.linkService.getDeviceLinks(rootSw)) {
                    log.trace("{} -> {} ", link.src().deviceId(), link.dst().deviceId());
                }
            }
            EcmpShortestPathGraph currEcmpSpg = currentEcmpSpgMap.get(rootSw);
            if (currEcmpSpg == null) {
                log.debug("No existing ECMP graph for device {}.. adding self as "
                        + "changed route", rootSw);
                changedRoutesBuilder.add(Lists.newArrayList(rootSw));
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
            changedRoutesBuilder.addAll(compareGraphs(newEcmpSpg, currEcmpSpg, rootSw));
            // then use the current/existing map to compare to updated/new map
            // as switch may have been removed
            changedRoutesBuilder.addAll(compareGraphs(currEcmpSpg, newEcmpSpg, rootSw));
        }

        Set<ArrayList<DeviceId>> changedRoutes = changedRoutesBuilder.build();
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
                    route.add(targetSw);
                    route.add(rootSw);
                    changedRoutesBuilder.add(route);
                }
            }
        }
        return changedRoutesBuilder.build();
    }

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
     * Populate ECMP rules for subnets from all switches to destination.
     *
     * @param destSw Device ID of destination switch
     * @param ecmpSPG ECMP shortest path graph
     * @param subnets Subnets to be populated. If empty, populate all configured subnets.
     * @return true if it succeeds in populating rules
     */
    private boolean populateEcmpRoutingRules(DeviceId destSw,
                                             EcmpShortestPathGraph ecmpSPG,
                                             Set<IpPrefix> subnets) {

        HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchVia = ecmpSPG
                .getAllLearnedSwitchesAndVia();
        for (Integer itrIdx : switchVia.keySet()) {
            HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap = switchVia
                    .get(itrIdx);
            for (DeviceId targetSw : swViaMap.keySet()) {
                Set<DeviceId> nextHops = new HashSet<>();
                log.debug("** Iter: {} root: {} target: {}", itrIdx, destSw, targetSw);
                for (ArrayList<DeviceId> via : swViaMap.get(targetSw)) {
                    if (via.isEmpty()) {
                        nextHops.add(destSw);
                    } else {
                        nextHops.add(via.get(0));
                    }
                }
                if (!populateEcmpRoutingRulePartial(targetSw, destSw, nextHops, subnets)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Populate ECMP rules for subnets from target to destination via nexthops.
     *
     * @param targetSw Device ID of target switch in which rules will be programmed
     * @param destSw Device ID of final destination switch to which the rules will forward
     * @param nextHops List of next hops via which destSw will be reached
     * @param subnets Subnets to be populated. If empty, populate all configured subnets.
     * @return true if it succees in populating rules
     */
    private boolean populateEcmpRoutingRulePartial(DeviceId targetSw,
                                                   DeviceId destSw,
                                                   Set<DeviceId> nextHops,
                                                   Set<IpPrefix> subnets) {
        boolean result;

        if (nextHops.isEmpty()) {
            nextHops.add(destSw);
        }
        // If both target switch and dest switch are edge routers, then set IP
        // rule for both subnet and router IP.
        boolean targetIsEdge;
        boolean destIsEdge;
        Ip4Address destRouterIpv4;
        Ip6Address destRouterIpv6;

        try {
            targetIsEdge = config.isEdgeDevice(targetSw);
            destIsEdge = config.isEdgeDevice(destSw);
            destRouterIpv4 = config.getRouterIpv4(destSw);
            destRouterIpv6 = config.getRouterIpv6(destSw);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateEcmpRoutingRulePartial.");
            return false;
        }

        if (targetIsEdge && destIsEdge) {
            subnets = (subnets != null && !subnets.isEmpty()) ? subnets
                                                              : config.getSubnets(destSw);
            log.debug("* populateEcmpRoutingRulePartial in device {} towards {} "
                    + "for subnets {}", targetSw, destSw, subnets);
            result = rulePopulator.populateIpRuleForSubnet(targetSw, subnets,
                                                           destSw, nextHops);
            if (!result) {
                return false;
            }
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
            }
        }

        if (!targetIsEdge && destIsEdge) {
            // MPLS rules in all non-edge target devices
            log.debug("* populateEcmpRoutingRulePartial in device{} towards {} for "
                    + "all MPLS rules", targetSw, destSw);
            result = rulePopulator.populateMplsRule(targetSw, destSw, nextHops,
                                                    destRouterIpv4);
            if (!result) {
                return false;
            }
            if (destRouterIpv6 != null) {
                result = rulePopulator.populateMplsRule(targetSw, destSw, nextHops,
                                                        destRouterIpv6);
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
     * Populates filtering rules for port, and punting rules
     * for gateway IPs, loopback IPs and arp/ndp traffic.
     * Should only be called by the master instance for this device/port.
     *
     * @param deviceId Switch ID to set the rules
     */
    public void populatePortAddressingRules(DeviceId deviceId) {
        rulePopulator.populateIpPunts(deviceId);
        rulePopulator.populateArpNdpPunts(deviceId);

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
                populationStatus = Status.STARTED;
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
     * Resume the flow rule population process if it was aborted for any reason.
     * Mostly the process is aborted when the groups required are not set yet.
     *  XXX is this called?
     *
     */
    public void resumePopulationProcess() {
        statusLock.lock();
        try {
            if (populationStatus == Status.ABORTED) {
                populationStatus = Status.STARTED;
                // TODO: we need to restart from the point aborted instead of
                // restarting.
                populateAllRoutingRules();
            }
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Populate rules of given subnet at given location.
     *
     * @param cp connect point of the subnet being added
     * @param subnets subnet being added
     * @return true if succeed
     */
    protected boolean populateSubnet(ConnectPoint cp, Set<IpPrefix> subnets) {
        statusLock.lock();
        try {
            EcmpShortestPathGraph ecmpSpg = currentEcmpSpgMap.get(cp.deviceId());
            if (ecmpSpg == null) {
                log.warn("Fail to populating subnet {}: {}", subnets, ECMPSPG_MISSING);
                return false;
            }
            return populateEcmpRoutingRules(cp.deviceId(), ecmpSpg, subnets);
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Revoke rules of given subnet at given location.
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

    protected void purgeEcmpGraph(DeviceId deviceId) {
        currentEcmpSpgMap.remove(deviceId);
        if (updatedEcmpSpgMap != null) {
            updatedEcmpSpgMap.remove(deviceId);
        }
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
            log.info("RETRY FILTER ATTEMPT {} ** dev:{}", ++counter, devId);
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
