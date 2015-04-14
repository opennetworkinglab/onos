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

import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onosproject.segmentrouting.grouphandler.NeighborSet;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

public class RoutingRulePopulator {

    private static final Logger log = LoggerFactory.getLogger(RoutingRulePopulator.class);

    private AtomicLong rulePopulationCounter;
    private SegmentRoutingManager srManager;
    private DeviceConfiguration config;
    /**
     * Creates a RoutingRulePopulator object.
     *
     * @param srManager segment routing manager reference
     */
    public RoutingRulePopulator(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.config = checkNotNull(srManager.deviceConfiguration);
        this.rulePopulationCounter = new AtomicLong(0);
    }

    /**
     * Resets the population counter.
     */
    public void resetCounter() {
        rulePopulationCounter.set(0);
    }

    /**
     * Returns the number of rules populated.
     */
    public long getCounter() {
        return rulePopulationCounter.get();
    }

    /**
     * Populates IP flow rules for specific hosts directly connected to the switch.
     *
     * @param deviceId switch ID to set the rules
     * @param hostIp host IP address
     * @param hostMac host MAC address
     * @param outPort port where the host is connected
     */
    public void populateIpRuleForHost(DeviceId deviceId, Ip4Address hostIp,
                                      MacAddress hostMac, PortNumber outPort) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();

        sbuilder.matchIPDst(IpPrefix.valueOf(hostIp, 32));
        sbuilder.matchEthType(Ethernet.TYPE_IPV4);

        tbuilder.setEthDst(hostMac)
                .setEthSrc(config.getDeviceMac(deviceId))
                .setOutput(outPort);

        TrafficTreatment treatment = tbuilder.build();
        TrafficSelector selector = sbuilder.build();

        FlowRule f = new DefaultFlowRule(deviceId, selector, treatment, 100,
                srManager.appId, 600, false, FlowRule.Type.IP);

        srManager.flowRuleService.applyFlowRules(f);
        rulePopulationCounter.incrementAndGet();
        log.debug("Flow rule {} is set to switch {}", f, deviceId);
    }

    /**
     * Populates IP flow rules for the subnets of the destination router.
     *
     * @param deviceId switch ID to set the rules
     * @param subnets subnet information
     * @param destSw destination switch ID
     * @param nextHops next hop switch ID list
     * @return true if all rules are set successfully, false otherwise
     */
    public boolean populateIpRuleForSubnet(DeviceId deviceId, List<Ip4Prefix> subnets,
                                           DeviceId destSw, Set<DeviceId> nextHops) {

        //List<IpPrefix> subnets = extractSubnet(subnetInfo);
        for (IpPrefix subnet: subnets) {
            if (!populateIpRuleForRouter(deviceId, subnet, destSw, nextHops)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Populates IP flow rules for the router IP address.
     *
     * @param deviceId device ID to set the rules
     * @param ipPrefix the IP address of the destination router
     * @param destSw device ID of the destination router
     * @param nextHops next hop switch ID list
     * @return true if all rules are set successfully, false otherwise
     */
    public boolean populateIpRuleForRouter(DeviceId deviceId, IpPrefix ipPrefix,
                                           DeviceId destSw, Set<DeviceId> nextHops) {

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();

        sbuilder.matchIPDst(ipPrefix);
        sbuilder.matchEthType(Ethernet.TYPE_IPV4);

        NeighborSet ns = null;

        //If the next hop is the same as the final destination, then MPLS label is not set.
        if (nextHops.size() == 1 && nextHops.toArray()[0].equals(destSw)) {
            tbuilder.decNwTtl();
            ns = new NeighborSet(nextHops);
        } else {
            tbuilder.copyTtlOut();
            ns = new NeighborSet(nextHops, config.getSegmentId(destSw));
        }

        DefaultGroupKey groupKey = (DefaultGroupKey) srManager.getGroupKey(ns);
        if (groupKey == null) {
            log.warn("Group key is not found for ns {}", ns);
            return false;
        }
        Group group = srManager.groupService.getGroup(deviceId, groupKey);
        if (group != null) {
            tbuilder.group(group.id());
        } else {
            log.warn("No group found for NeighborSet {} from {} to {}",
                    ns, deviceId, destSw);
            return false;
        }

        TrafficTreatment treatment = tbuilder.build();
        TrafficSelector selector = sbuilder.build();

        FlowRule f = new DefaultFlowRule(deviceId, selector, treatment, 100,
                srManager.appId, 600, false, FlowRule.Type.IP);

        srManager.flowRuleService.applyFlowRules(f);
        rulePopulationCounter.incrementAndGet();
        log.debug("IP flow rule {} is set to switch {}", f, deviceId);

        return true;
    }


    /**
     * Populates MPLS flow rules to all transit routers.
     *
     * @param deviceId device ID of the switch to set the rules
     * @param destSwId destination switch device ID
     * @param nextHops next hops switch ID list
     * @return true if all rules are set successfully, false otherwise
     */
    public boolean populateMplsRule(DeviceId deviceId, DeviceId destSwId, Set<DeviceId> nextHops) {

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        Collection<TrafficTreatment> treatments = new ArrayList<>();

        // TODO Handle the case of Bos == false
        sbuilder.matchMplsLabel(MplsLabel.mplsLabel(config.getSegmentId(destSwId)));
        sbuilder.matchEthType(Ethernet.MPLS_UNICAST);

        //If the next hop is the destination router, do PHP
        if (nextHops.size() == 1 && destSwId.equals(nextHops.toArray()[0])) {
            TrafficTreatment treatmentBos =
                    getMplsTreatment(deviceId, destSwId, nextHops, true, true);
            TrafficTreatment treatment =
                    getMplsTreatment(deviceId, destSwId, nextHops, true, false);
            if (treatmentBos != null) {
                treatments.add(treatmentBos);
            } else {
                log.warn("Failed to set MPLS rules.");
                return false;
            }
        } else {
            TrafficTreatment treatmentBos =
                    getMplsTreatment(deviceId, destSwId, nextHops, false, true);
            TrafficTreatment treatment =
                    getMplsTreatment(deviceId, destSwId, nextHops, false, false);

            if (treatmentBos != null) {
                treatments.add(treatmentBos);
            } else {
                log.warn("Failed to set MPLS rules.");
                return false;
            }
        }

        TrafficSelector selector = sbuilder.build();
        for (TrafficTreatment treatment: treatments) {
            FlowRule f = new DefaultFlowRule(deviceId, selector, treatment, 100,
                    srManager.appId, 600, false, FlowRule.Type.MPLS);
            srManager.flowRuleService.applyFlowRules(f);
            rulePopulationCounter.incrementAndGet();
            log.debug("MPLS rule {} is set to {}", f, deviceId);
        }

        return true;
    }


    private TrafficTreatment getMplsTreatment(DeviceId deviceId, DeviceId destSw,
                                             Set<DeviceId> nextHops,
                                             boolean phpRequired, boolean isBos) {

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();

        if (phpRequired) {
            tbuilder.copyTtlIn();
            if (isBos) {
                tbuilder.popMpls(Ethernet.TYPE_IPV4)
                        .decNwTtl();
            } else {
                tbuilder.popMpls(Ethernet.MPLS_UNICAST)
                .decMplsTtl();
            }
        } else {
            tbuilder.decMplsTtl();
        }

        if (!isECMPSupportedInTransitRouter() && !config.isEdgeDevice(deviceId)) {
            Link link = selectOneLink(deviceId, nextHops);
            DeviceId nextHop = (DeviceId) nextHops.toArray()[0];
            if (link == null) {
                log.warn("No link from {} to {}", deviceId, nextHops);
                return null;
            }
            tbuilder.setEthSrc(config.getDeviceMac(deviceId))
                    .setEthDst(config.getDeviceMac(nextHop))
                    .setOutput(link.src().port());
        } else {
            NeighborSet ns = new NeighborSet(nextHops);
            DefaultGroupKey groupKey = (DefaultGroupKey) srManager.getGroupKey(ns);
            if (groupKey == null) {
                log.warn("Group key is not found for ns {}", ns);
                return null;
            }
            Group group = srManager.groupService.getGroup(deviceId, groupKey);
            if (group != null) {
                tbuilder.group(group.id());
            } else {
                log.warn("No group found for ns {} key {} in {}", ns,
                        srManager.getGroupKey(ns), deviceId);
                return null;
            }
        }

        return tbuilder.build();
    }

    private boolean isECMPSupportedInTransitRouter() {

        // TODO: remove this function when objectives subsystem is supported.
        return false;
    }

    /**
     * Populates VLAN flows rules.
     * All packets are forwarded to TMAC table.
     *
     * @param deviceId switch ID to set the rules
     */
    public void populateTableVlan(DeviceId deviceId) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();

        tbuilder.transition(FlowRule.Type.ETHER);

        TrafficTreatment treatment = tbuilder.build();
        TrafficSelector selector = sbuilder.build();

        FlowRule f = new DefaultFlowRule(deviceId, selector, treatment, 100,
                srManager.appId, 600, false, FlowRule.Type.VLAN);

        srManager.flowRuleService.applyFlowRules(f);

        log.debug("Vlan flow rule {} is set to switch {}", f, deviceId);
    }

    /**
     * Populates TMAC table rules.
     * IP packets are forwarded to IP table.
     * MPLS packets are forwarded to MPLS table.
     *
     * @param deviceId switch ID to set the rules
     */
    public void populateTableTMac(DeviceId deviceId) {

        // flow rule for IP packets
        TrafficSelector selectorIp = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchEthDst(config.getDeviceMac(deviceId))
                .build();
        TrafficTreatment treatmentIp = DefaultTrafficTreatment.builder()
                .transition(FlowRule.Type.IP)
                .build();

        FlowRule flowIp = new DefaultFlowRule(deviceId, selectorIp, treatmentIp, 100,
                srManager.appId, 600, false, FlowRule.Type.ETHER);

        srManager.flowRuleService.applyFlowRules(flowIp);

        // flow rule for MPLS packets
        TrafficSelector selectorMpls = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.MPLS_UNICAST)
                .matchEthDst(config.getDeviceMac(deviceId))
                .build();
        TrafficTreatment treatmentMpls = DefaultTrafficTreatment.builder()
                .transition(FlowRule.Type.MPLS)
                .build();

        FlowRule flowMpls = new DefaultFlowRule(deviceId, selectorMpls, treatmentMpls, 100,
                srManager.appId, 600, false, FlowRule.Type.ETHER);

        srManager.flowRuleService.applyFlowRules(flowMpls);

    }

    /**
     * Populates a table miss entry.
     *
     * @param deviceId switch ID to set rules
     * @param tableToAdd table to set the rules
     * @param toControllerNow flag to send packets to controller immediately
     * @param toControllerWrite flag to send packets to controller at the end of pipeline
     * @param toTable flag to send packets to a specific table
     * @param tableToSend table type to send packets when the toTable flag is set
     */
    public void populateTableMissEntry(DeviceId deviceId, FlowRule.Type tableToAdd, boolean toControllerNow,
                                       boolean toControllerWrite,
                                       boolean toTable, FlowRule.Type tableToSend) {
        // TODO: Change arguments to EnumSet
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .build();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        if (toControllerNow) {
            tBuilder.setOutput(PortNumber.CONTROLLER);
        }

        if (toControllerWrite) {
            tBuilder.deferred().setOutput(PortNumber.CONTROLLER);
        }

        if (toTable) {
            tBuilder.transition(tableToSend);
        }

        FlowRule flow = new DefaultFlowRule(deviceId, selector, tBuilder.build(), 0,
                srManager.appId, 600, false, tableToAdd);

        srManager.flowRuleService.applyFlowRules(flow);

    }

    private Link selectOneLink(DeviceId srcId, Set<DeviceId> destIds) {

        Set<Link> links = srManager.linkService.getDeviceEgressLinks(srcId);
        DeviceId destId = (DeviceId) destIds.toArray()[0];
        for (Link link: links) {
            if (link.dst().deviceId().equals(destId)) {
                return link;
            }
        }

        return null;
    }

}
