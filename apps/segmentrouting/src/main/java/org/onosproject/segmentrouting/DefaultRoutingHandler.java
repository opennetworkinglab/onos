/*
 * Copyright 2015 Open Networking Laboratory
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultRoutingHandler {

    private static Logger log = LoggerFactory
            .getLogger(DefaultRoutingHandler.class);

    private SegmentRoutingManager srManager;
    private RoutingRulePopulator rulePopulator;
    private HashMap<DeviceId, ECMPShortestPathGraph> currentEcmpSpgMap;
    private HashMap<DeviceId, ECMPShortestPathGraph> updatedEcmpSpgMap;
    private DeviceConfiguration config;
    // private Status populationStatus;
    private boolean linkHasWeight;
    private final Lock statusLock = new ReentrantLock();
    private volatile Status populationStatus;

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
    public DefaultRoutingHandler(SegmentRoutingManager srManager, boolean linkHasWeight) {
        this.srManager = srManager;
        this.rulePopulator = checkNotNull(srManager.routingRulePopulator);
        this.config = checkNotNull(srManager.deviceConfiguration);
        this.populationStatus = Status.IDLE;
        this.currentEcmpSpgMap = Maps.newHashMap();
        this.linkHasWeight = linkHasWeight;
    }

    /**
     * Populates all routing rules to all connected routers, including default
     * routing rules, adjacency rules, and policy rules if any.
     *
     * @return true if it succeeds in populating all rules, otherwise false
     */
    public boolean populateAllRoutingRules() {

        statusLock.lock();
        try {
            populationStatus = Status.STARTED;
            rulePopulator.resetCounter();
            log.info("Starts to populate routing rules");
            log.debug("populateAllRoutingRules: populationStatus is STARTED");

            for (Device sw : srManager.deviceService.getDevices()) {
                if (srManager.mastershipService.getLocalRole(sw.id()) != MastershipRole.MASTER) {
                    log.debug("populateAllRoutingRules: skipping device {}...we are not master",
                              sw.id());
                    continue;
                }

                ECMPShortestPathGraph ecmpSpg = new ECMPShortestPathGraph(sw.id(), srManager, linkHasWeight);
                if (!populateEcmpRoutingRules(sw.id(), ecmpSpg)) {
                    log.debug("populateAllRoutingRules: populationStatus is ABORTED");
                    populationStatus = Status.ABORTED;
                    log.debug("Abort routing rule population");
                    return false;
                    currentEcmpSpgMap.put(sw.id(), ecmpSpg);
                }

                log.debug("populateAllRoutingRules: populationStatus is SUCCEEDED");
                populationStatus = Status.SUCCEEDED;
                log.info("Completes routing rule population. Total # of rules pushed : {}",
                        rulePopulator.getCounter());
                return true;
            }
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Populates the routing rules according to the route changes due to the link
     * failure or link add. It computes the routes changed due to the link changes and
     * repopulates the rules only for the routes.
     *
     * @param linkFail link failed, null for link added
     * @return true if it succeeds to populate all rules, false otherwise
     */
    public boolean populateRoutingRulesForLinkStatusChange(Link linkFail) {

        statusLock.lock();
        try {

            if (populationStatus == Status.STARTED) {
                log.warn("Previous rule population is not finished.");
                return true;
            }

            // Take the snapshots of the links
            updatedEcmpSpgMap = new HashMap<>();
            for (Device sw : srManager.deviceService.getDevices()) {
                if (srManager.mastershipService.
                        getLocalRole(sw.id()) != MastershipRole.MASTER) {
                    continue;
                }
                ECMPShortestPathGraph ecmpSpgUpdated =
                        new ECMPShortestPathGraph(sw.id(), srManager, linkHasWeight);
                updatedEcmpSpgMap.put(sw.id(), ecmpSpgUpdated);
            }

            log.info("Starts rule population from link change");

            Set<ArrayList<DeviceId>> routeChanges;
            log.trace("populateRoutingRulesForLinkStatusChange: "
                    + "populationStatus is STARTED");
            populationStatus = Status.STARTED;
            if (linkFail == null) {
                // Compare all routes of existing ECMP SPG with the new ones
                routeChanges = computeRouteChange();
            } else {
                // Compare existing ECMP SPG only with the link removed
                routeChanges = computeDamagedRoutes(linkFail);
            }

            if (routeChanges.isEmpty()) {
                log.info("No route changes for the link status change");
                log.debug("populateRoutingRulesForLinkStatusChange: populationStatus is SUCCEEDED");
                populationStatus = Status.SUCCEEDED;
                return true;
            }

            if (repopulateRoutingRulesForRoutes(routeChanges)) {
                log.debug("populateRoutingRulesForLinkStatusChange: populationStatus is SUCCEEDED");
                populationStatus = Status.SUCCEEDED;
                log.info("Complete to repopulate the rules. # of rules populated : {}",
                        rulePopulator.getCounter());
                return true;
            } else {
                log.debug("populateRoutingRulesForLinkStatusChange: populationStatus is ABORTED");
                populationStatus = Status.ABORTED;
                log.warn("Failed to repopulate the rules.");
                return false;
            }
        } finally {
            statusLock.unlock();
        }
    }

    private boolean repopulateRoutingRulesForRoutes(Set<ArrayList<DeviceId>> routes) {
        rulePopulator.resetCounter();
        HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> routesBydevice =
                new HashMap<>();
        for (ArrayList<DeviceId> link: routes) {
            // When only the source device is defined, reinstall routes to all other devices
            if (link.size() == 1) {
                log.trace("repopulateRoutingRulesForRoutes: running ECMP graph for device {}", link.get(0));
                ECMPShortestPathGraph ecmpSpg = new ECMPShortestPathGraph(link.get(0), srManager, linkHasWeight);
                if (populateEcmpRoutingRules(link.get(0), ecmpSpg)) {
                    log.debug("Populating flow rules from {} to all is successful",
                              link.get(0));
                    currentEcmpSpgMap.put(link.get(0), ecmpSpg);
                } else {
                    log.warn("Failed to populate the flow rules from {} to all", link.get(0));
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
                log.debug("repopulate RoutingRules For Routes {} -> {}",
                          link.get(0), link.get(1));
                DeviceId src = link.get(0);
                DeviceId dst = link.get(1);
                ECMPShortestPathGraph ecmpSpg = updatedEcmpSpgMap.get(dst);
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
                            if (via.isEmpty()) {
                                nextHops.add(dst);
                            } else {
                                nextHops.add(via.get(0));
                            }
                        }
                        if (!populateEcmpRoutingRulePartial(targetSw, dst, nextHops)) {
                            return false;
                        }
                        log.debug("Populating flow rules from {} to {} is successful",
                                  targetSw, dst);
                    }
                }
                //currentEcmpSpgMap.put(dst, ecmpSpg);
            }
            //Only if all the flows for all impacted routes to a
            //specific target are pushed successfully, update the
            //ECMP graph for that target. (Or else the next event
            //would not see any changes in the ECMP graphs)
            currentEcmpSpgMap.put(impactedDevice,
                                  updatedEcmpSpgMap.get(impactedDevice));
        }
        return true;
    }

    private Set<ArrayList<DeviceId>> computeDamagedRoutes(Link linkFail) {

        Set<ArrayList<DeviceId>> routes = new HashSet<>();

        for (Device sw : srManager.deviceService.getDevices()) {
            log.debug("Computing the impacted routes for device {} due to link fail",
                      sw.id());
            if (srManager.mastershipService.
                    getLocalRole(sw.id()) != MastershipRole.MASTER) {
                continue;
            }
            ECMPShortestPathGraph ecmpSpg = currentEcmpSpgMap.get(sw.id());
            if (ecmpSpg == null) {
                log.error("No existing ECMP graph for switch {}", sw.id());
                continue;
            }
            HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchVia =
                    ecmpSpg.getAllLearnedSwitchesAndVia();
            for (Integer itrIdx : switchVia.keySet()) {
                HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap =
                        switchVia.get(itrIdx);
                for (DeviceId targetSw : swViaMap.keySet()) {
                    DeviceId destSw = sw.id();
                    Set<ArrayList<DeviceId>> subLinks =
                            computeLinks(targetSw, destSw, swViaMap);
                    for (ArrayList<DeviceId> alink: subLinks) {
                        if ((alink.get(0).equals(linkFail.src().deviceId()) &&
                                alink.get(1).equals(linkFail.dst().deviceId()))
                                ||
                             (alink.get(0).equals(linkFail.dst().deviceId()) &&
                                     alink.get(1).equals(linkFail.src().deviceId()))) {
                            log.debug("Impacted route:{}->{}", targetSw, destSw);
                            ArrayList<DeviceId> aRoute = new ArrayList<>();
                            aRoute.add(targetSw);
                            aRoute.add(destSw);
                            routes.add(aRoute);
                            break;
                        }
                    }
                }
            }

        }

        return routes;
    }

    private Set<ArrayList<DeviceId>> computeRouteChange() {

        Set<ArrayList<DeviceId>> routes = new HashSet<>();

        for (Device sw : srManager.deviceService.getDevices()) {
            log.debug("Computing the impacted routes for device {}",
                      sw.id());
            if (srManager.mastershipService.
                    getLocalRole(sw.id()) != MastershipRole.MASTER) {
                log.debug("No mastership for {} and skip route optimization",
                          sw.id());
                continue;
            }

            log.trace("link of {} - ", sw.id());
            for (Link link: srManager.linkService.getDeviceLinks(sw.id())) {
                log.trace("{} -> {} ", link.src().deviceId(), link.dst().deviceId());
            }

            log.debug("Checking route change for switch {}", sw.id());
            ECMPShortestPathGraph ecmpSpg = currentEcmpSpgMap.get(sw.id());
            if (ecmpSpg == null) {
                log.debug("No existing ECMP graph for device {}", sw.id());
                ArrayList<DeviceId> route = new ArrayList<>();
                route.add(sw.id());
                routes.add(route);
                continue;
            }
            ECMPShortestPathGraph newEcmpSpg = updatedEcmpSpgMap.get(sw.id());
            //currentEcmpSpgMap.put(sw.id(), newEcmpSpg);
            HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchVia =
                    ecmpSpg.getAllLearnedSwitchesAndVia();
            HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchViaUpdated =
                    newEcmpSpg.getAllLearnedSwitchesAndVia();

            for (Integer itrIdx : switchViaUpdated.keySet()) {
                HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMapUpdated =
                        switchViaUpdated.get(itrIdx);
                for (DeviceId srcSw : swViaMapUpdated.keySet()) {
                    ArrayList<ArrayList<DeviceId>> viaUpdated = swViaMapUpdated.get(srcSw);
                    ArrayList<ArrayList<DeviceId>> via = getVia(switchVia, srcSw);
                    if ((via == null) || !viaUpdated.equals(via)) {
                        log.debug("Impacted route:{}->{}", srcSw, sw.id());
                        ArrayList<DeviceId> route = new ArrayList<>();
                        route.add(srcSw);
                        route.add(sw.id());
                        routes.add(route);
                    }
                }
            }
        }

        for (ArrayList<DeviceId> link: routes) {
            log.trace("Route changes - ");
            if (link.size() == 1) {
                log.trace(" : {} - all", link.get(0));
            } else {
                log.trace(" : {} - {}", link.get(0), link.get(1));
            }
        }

        return routes;
    }

    private ArrayList<ArrayList<DeviceId>> getVia(HashMap<Integer, HashMap<DeviceId,
            ArrayList<ArrayList<DeviceId>>>> switchVia, DeviceId srcSw) {
        for (Integer itrIdx : switchVia.keySet()) {
            HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap =
                    switchVia.get(itrIdx);
            if (swViaMap.get(srcSw) == null) {
                continue;
            } else {
                return swViaMap.get(srcSw);
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

    private boolean populateEcmpRoutingRules(DeviceId destSw,
                                             ECMPShortestPathGraph ecmpSPG) {

        HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchVia = ecmpSPG
                .getAllLearnedSwitchesAndVia();
        for (Integer itrIdx : switchVia.keySet()) {
            HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap = switchVia
                    .get(itrIdx);
            for (DeviceId targetSw : swViaMap.keySet()) {
                Set<DeviceId> nextHops = new HashSet<>();

                for (ArrayList<DeviceId> via : swViaMap.get(targetSw)) {
                    if (via.isEmpty()) {
                        nextHops.add(destSw);
                    } else {
                        nextHops.add(via.get(0));
                    }
                }
                if (!populateEcmpRoutingRulePartial(targetSw, destSw, nextHops)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean populateEcmpRoutingRulePartial(DeviceId targetSw,
                                                   DeviceId destSw,
                                                   Set<DeviceId> nextHops) {
        boolean result;

        if (nextHops.isEmpty()) {
            nextHops.add(destSw);
        }

        // If both target switch and dest switch are edge routers, then set IP
        // rule for both subnet and router IP.
        if (config.isEdgeDevice(targetSw) && config.isEdgeDevice(destSw)) {
            List<Ip4Prefix> subnets = config.getSubnets(destSw);
            log.debug("populateEcmpRoutingRulePartial in device {} towards {} for subnets {}",
                    targetSw, destSw, subnets);
            result = rulePopulator.populateIpRuleForSubnet(targetSw,
                                                           subnets,
                                                           destSw,
                                                           nextHops);
            if (!result) {
                return false;
            }

            Ip4Address routerIp = config.getRouterIp(destSw);
            IpPrefix routerIpPrefix = IpPrefix.valueOf(routerIp, IpPrefix.MAX_INET_MASK_LENGTH);
            log.debug("populateEcmpRoutingRulePartial in device {} towards {} for router IP {}",
                    targetSw, destSw, routerIpPrefix);
            result = rulePopulator.populateIpRuleForRouter(targetSw, routerIpPrefix, destSw, nextHops);
            if (!result) {
                return false;
            }

        // If the target switch is an edge router, then set IP rules for the router IP.
        } else if (config.isEdgeDevice(targetSw)) {
            Ip4Address routerIp = config.getRouterIp(destSw);
            IpPrefix routerIpPrefix = IpPrefix.valueOf(routerIp, IpPrefix.MAX_INET_MASK_LENGTH);
            log.debug("populateEcmpRoutingRulePartial in device {} towards {} for router IP {}",
                    targetSw, destSw, routerIpPrefix);
            result = rulePopulator.populateIpRuleForRouter(targetSw, routerIpPrefix, destSw, nextHops);
            if (!result) {
                return false;
            }
        }

        // Populates MPLS rules to all routers
        log.debug("populateEcmpRoutingRulePartial in device{} towards {} for all MPLS rules",
                targetSw, destSw);
        result = rulePopulator.populateMplsRule(targetSw, destSw, nextHops);
        if (!result) {
            return false;
        }

        return true;
    }

    /**
     * Populates table miss entries for all tables, and pipeline rules for VLAN
     * and TACM tables.
     *
     * @param deviceId Switch ID to set the rules
     */
    public void populateTtpRules(DeviceId deviceId) {
        rulePopulator.populateTableVlan(deviceId);
        rulePopulator.populateTableTMac(deviceId);
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
}
