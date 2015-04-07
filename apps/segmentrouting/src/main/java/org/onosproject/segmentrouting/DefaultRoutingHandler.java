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

import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.flow.FlowRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultRoutingHandler {

    private static Logger log = LoggerFactory.getLogger(DefaultRoutingHandler.class);

    private SegmentRoutingManager srManager;
    private RoutingRulePopulator rulePopulator;
    private NetworkConfigHandler config;
    private Status populationStatus;

    /**
     * Represents the default routing population status.
     */
    public enum Status {
        // population process is not started yet.
        IDLE,

        // population process started.
        STARTED,

        // population process was aborted due to errors, mostly for groups not found.
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
        this.config = checkNotNull(srManager.networkConfigHandler);
        this.populationStatus = Status.IDLE;
    }

    /**
     * Populates all routing rules to all connected routers, including default
     * routing rules, adjacency rules, and policy rules if any.
     *
     * @return true if it succeeds in populating all rules, otherwise false
     */
    public boolean populateAllRoutingRules() {

        populationStatus = Status.STARTED;
        log.info("Starts to populate routing rules");

        for (Device sw : srManager.deviceService.getDevices()) {
            if (srManager.mastershipService.
                    getLocalRole(sw.id()) != MastershipRole.MASTER) {
                continue;
            }

            ECMPShortestPathGraph ecmpSPG = new ECMPShortestPathGraph(sw.id(), srManager);
            if (!populateEcmpRoutingRules(sw, ecmpSPG)) {
                populationStatus = Status.ABORTED;
                log.debug("Abort routing rule population");
                return false;
            }

            // TODO: Set adjacency routing rule for all switches
        }

        populationStatus = Status.SUCCEEDED;
        log.info("Completes routing rule population");
        return true;
    }

    private boolean populateEcmpRoutingRules(Device sw,
                                             ECMPShortestPathGraph ecmpSPG) {

        HashMap<Integer, HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>>> switchVia =
                ecmpSPG.getAllLearnedSwitchesAndVia();
        for (Integer itrIdx : switchVia.keySet()) {
            HashMap<DeviceId, ArrayList<ArrayList<DeviceId>>> swViaMap =
                    switchVia.get(itrIdx);
            for (DeviceId targetSw : swViaMap.keySet()) {
                DeviceId destSw = sw.id();
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

    private boolean populateEcmpRoutingRulePartial(DeviceId targetSw, DeviceId destSw,
                                Set<DeviceId> nextHops) {
        boolean result;

        if (nextHops.isEmpty()) {
            nextHops.add(destSw);
        }

        // If both target switch and dest switch are edge routers, then set IP rule
        // for both subnet and router IP.
        if (config.isEdgeRouter(targetSw) && config.isEdgeRouter(destSw)) {
            List<Ip4Prefix> subnets = config.getSubnetInfo(destSw);
            result = rulePopulator.populateIpRuleForSubnet(targetSw,
                                                           subnets,
                                                           destSw,
                                                           nextHops);
            if (!result) {
                return false;
            }

            IpPrefix routerIp = config.getRouterIpAddress(destSw);
            result = rulePopulator.populateIpRuleForRouter(targetSw, routerIp, destSw, nextHops);
            if (!result) {
                return false;
            }

        // If the target switch is an edge router, then set IP rules for the router IP.
        } else if (config.isEdgeRouter(targetSw)) {
            IpPrefix routerIp = config.getRouterIpAddress(destSw);
            result = rulePopulator.populateIpRuleForRouter(targetSw, routerIp, destSw, nextHops);
            if (!result) {
                return false;
            }

        // If the target switch is an transit router, then set MPLS rules only.
        } else if (config.isTransitRouter(targetSw)) {
            result = rulePopulator.populateMplsRule(targetSw, destSw, nextHops);
            if (!result) {
                return false;
            }
        } else {
            log.warn("The switch {} is neither an edge router nor a transit router.", targetSw);
            return false;
        }

        return true;
    }

    /**
     * Populates table miss entries for all tables, and pipeline rules for
     * VLAN and TACM tables.
     *
     * @param deviceId Switch ID to set the rules
     */
    public void populateTtpRules(DeviceId deviceId) {

        rulePopulator.populateTableMissEntry(deviceId, FlowRule.Type.VLAN,
                true, false, false, FlowRule.Type.DEFAULT);
        rulePopulator.populateTableMissEntry(deviceId, FlowRule.Type.ETHER,
                true, false, false, FlowRule.Type.DEFAULT);
        rulePopulator.populateTableMissEntry(deviceId, FlowRule.Type.IP,
                false, true, true, FlowRule.Type.ACL);
        rulePopulator.populateTableMissEntry(deviceId, FlowRule.Type.MPLS,
                false, true, true, FlowRule.Type.ACL);
        rulePopulator.populateTableMissEntry(deviceId, FlowRule.Type.ACL,
                false, false, false, FlowRule.Type.DEFAULT);

        rulePopulator.populateTableVlan(deviceId);
        rulePopulator.populateTableTMac(deviceId);
    }

    /**
     * Start the flow rule population process if it was never started.
     * The process finishes successfully when all flow rules are set and
     * stops with ABORTED status when any groups required for flows is not
     * set yet.
     */
    public void startPopulationProcess() {
        synchronized (populationStatus) {
            if (populationStatus == Status.IDLE ||
                    populationStatus == Status.SUCCEEDED) {
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
                // TODO: we need to restart from the point aborted instead of restarting.
                populateAllRoutingRules();
            }
        }
    }
}
