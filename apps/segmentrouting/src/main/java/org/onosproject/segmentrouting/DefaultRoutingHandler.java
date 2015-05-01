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

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultRoutingHandler {

    private static Logger log = LoggerFactory
            .getLogger(DefaultRoutingHandler.class);

    private SegmentRoutingManager srManager;
    private RoutingRulePopulator rulePopulator;
    private HashMap<DeviceId, ECMPShortestPathGraph> currentEcmpSpgMap;
    private DeviceConfiguration config;
    private Status populationStatus;

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
     * Populates all routing rules to all connected routers, including default
     * routing rules, adjacency rules, and policy rules if any.
     *
     * @return true if it succeeds in populating all rules, otherwise false
     */
    public boolean populateAllRoutingRules() {

        populationStatus = Status.STARTED;
        rulePopulator.resetCounter();
        log.info("Starts to populate routing rules");

        for (Device sw : srManager.deviceService.getDevices()) {
            if (srManager.mastershipService.getLocalRole(sw.id()) != MastershipRole.MASTER) {
                continue;
            }

            ECMPShortestPathGraph ecmpSpg = new ECMPShortestPathGraph(sw.id(), srManager);
            if (!populateEcmpRoutingRules(sw.id(), ecmpSpg)) {
                populationStatus = Status.ABORTED;
                log.debug("Abort routing rule population");
                return false;
            }
            currentEcmpSpgMap.put(sw.id(), ecmpSpg);

            // TODO: Set adjacency routing rule for all switches
        }

        populationStatus = Status.SUCCEEDED;
        log.info("Completes routing rule population. Total # of rules pushed : {}",
                rulePopulator.getCounter());
        return true;
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

        synchronized (populationStatus) {

            if (populationStatus == Status.STARTED) {
                return true;
            }

            Set<ArrayList<DeviceId>> routeChanges;
            populationStatus = Status.STARTED;
            if (linkFail == null) {
                // Compare all routes of existing ECMP SPG with the new ones
                routeChanges = computeRouteChange();
            } else {
                // Compare existing ECMP SPG only with the link removed
                routeChanges = computeDamagedRoutes(linkFail);
            }

            if (routeChanges.isEmpty()) {
                log.debug("No route changes for the link status change");
                populationStatus = Status.SUCCEEDED;
                return true;
            }

            if (repopulateRoutingRulesForRoutes(routeChanges)) {
                populationStatus = Status.SUCCEEDED;
                log.info("Complete to repopulate the rules. # of rules populated : {}",
                        rulePopulator.getCounter());
                return true;
            } else {
                populationStatus = Status.ABORTED;
                log.warn("Failed to repopulate the rules.");
                return false;
            }
        }
    }

    private boolean repopulateRoutingRulesForRoutes(Set<ArrayList<DeviceId>> routes) {
        rulePopulator.resetCounter();
        for (ArrayList<DeviceId> link: routes) {
            // When only the source device is defined, reinstall routes to all other devices
            if (link.size() == 1) {
                ECMPShortestPathGraph ecmpSpg = new ECMPShortestPathGraph(link.get(0), srManager);
                if (populateEcmpRoutingRules(link.get(0), ecmpSpg)) {
                    currentEcmpSpgMap.put(link.get(0), ecmpSpg);
                }
                continue;
            }
            DeviceId src = link.get(0);
            DeviceId dst = link.get(1);
            ECMPShortestPathGraph ecmpSpg = new ECMPShortestPathGraph(dst, srManager);

            currentEcmpSpgMap.put(dst, ecmpSpg);
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
                }
            }
        }
        return true;
    }

    private Set<ArrayList<DeviceId>> computeDamagedRoutes(Link linkFail) {

        Set<ArrayList<DeviceId>> routes = new HashSet<>();

        for (Device sw : srManager.deviceService.getDevices()) {
            if (srManager.mastershipService.
                    getLocalRole(sw.id()) != MastershipRole.MASTER) {
                continue;
            }
            ECMPShortestPathGraph ecmpSpg = currentEcmpSpgMap.get(sw.id());
            if (ecmpSpg == null) {
                log.error("No existing ECMP path for switch {}", sw.id());
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
                        if (alink.get(0).equals(linkFail.src().deviceId()) &&
                                alink.get(1).equals(linkFail.dst().deviceId())) {
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
            if (srManager.mastershipService.
                    getLocalRole(sw.id()) != MastershipRole.MASTER) {
                continue;
            }
            ECMPShortestPathGraph ecmpSpg = currentEcmpSpgMap.get(sw.id());
            if (ecmpSpg == null) {
                log.debug("No existing ECMP path for Switch {}", sw.id());
                ArrayList<DeviceId> route = new ArrayList<>();
                route.add(sw.id());
                routes.add(route);
                continue;
            }
            ECMPShortestPathGraph newEcmpSpg =
                    new ECMPShortestPathGraph(sw.id(), srManager);
            HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchVia =
                    ecmpSpg.getAllLearnedSwitchesAndVia();
            HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchViaUpdated =
                    newEcmpSpg.getAllLearnedSwitchesAndVia();

            for (Integer itrIdx : switchVia.keySet()) {
                HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap =
                        switchVia.get(itrIdx);
                for (DeviceId srcSw : swViaMap.keySet()) {
                    ArrayList<ArrayList<DeviceId>> via1 = swViaMap.get(srcSw);
                    ArrayList<ArrayList<DeviceId>> via2 = getVia(switchViaUpdated, srcSw);
                    if (!via1.equals(via2)) {
                        ArrayList<DeviceId> route = new ArrayList<>();
                        route.add(srcSw);
                        route.add(sw.id());
                        routes.add(route);
                    }
                }
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

        return new ArrayList<>();
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
        // rule
        // for both subnet and router IP.
        if (config.isEdgeDevice(targetSw) && config.isEdgeDevice(destSw)) {
            List<Ip4Prefix> subnets = config.getSubnets(destSw);
            result = rulePopulator.populateIpRuleForSubnet(targetSw,
                                                           subnets,
                                                           destSw,
                                                           nextHops);
            if (!result) {
                return false;
            }

            Ip4Address routerIp = config.getRouterIp(destSw);
            IpPrefix routerIpPrefix = IpPrefix.valueOf(routerIp, IpPrefix.MAX_INET_MASK_LENGTH);
            result = rulePopulator.populateIpRuleForRouter(targetSw, routerIpPrefix, destSw, nextHops);
            if (!result) {
                return false;
            }

        // TODO: If the target switch is an edge router, then set IP rules for the router IP.
        } else if (config.isEdgeDevice(targetSw)) {
            Ip4Address routerIp = config.getRouterIp(destSw);
            IpPrefix routerIpPrefix = IpPrefix.valueOf(routerIp, IpPrefix.MAX_INET_MASK_LENGTH);
            result = rulePopulator.populateIpRuleForRouter(targetSw, routerIpPrefix, destSw, nextHops);
            if (!result) {
                return false;
            }

        // TODO: If the target switch is an transit router, then set MPLS rules only.
        } else if (!config.isEdgeDevice(targetSw)) {
            result = rulePopulator.populateMplsRule(targetSw, destSw, nextHops);
            if (!result) {
                return false;
            }
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
        synchronized (populationStatus) {
            if (populationStatus == Status.IDLE
                    || populationStatus == Status.SUCCEEDED) {
                populationStatus = Status.STARTED;
                populateAllRoutingRules();
            }
        }
    }

    /**
     * Resume the flow rule population process if it was aborted for any reason.
     * Mostly the process is aborted when the groups required are not set yet.
     */
    public void resumePopulationProcess() {
        synchronized (populationStatus) {
            if (populationStatus == Status.ABORTED) {
                populationStatus = Status.STARTED;
                // TODO: we need to restart from the point aborted instead of
                // restarting.
                populateAllRoutingRules();
            }
        }
    }
}
