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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv6;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.onosproject.segmentrouting.grouphandler.DestinationSet;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Builder;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.segmentrouting.storekey.DummyVlanIdStoreKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.Ethernet.TYPE_ARP;
import static org.onlab.packet.Ethernet.TYPE_IPV6;
import static org.onlab.packet.ICMP6.NEIGHBOR_ADVERTISEMENT;
import static org.onlab.packet.ICMP6.NEIGHBOR_SOLICITATION;
import static org.onlab.packet.ICMP6.ROUTER_ADVERTISEMENT;
import static org.onlab.packet.ICMP6.ROUTER_SOLICITATION;
import static org.onlab.packet.IPv6.PROTOCOL_ICMP6;
import static org.onosproject.segmentrouting.SegmentRoutingManager.INTERNAL_VLAN;
import static org.onosproject.segmentrouting.SegmentRoutingService.DEFAULT_PRIORITY;

import static org.onosproject.segmentrouting.SegmentRoutingManager.PSEUDOWIRE_VLAN;

/**
 * Populator of segment routing flow rules.
 */
public class RoutingRulePopulator {
    private static final Logger log = LoggerFactory.getLogger(RoutingRulePopulator.class);

    private static final int ARP_NDP_PRIORITY = 30000;

    private AtomicLong rulePopulationCounter;
    private SegmentRoutingManager srManager;
    private DeviceConfiguration config;

    /**
     * Creates a RoutingRulePopulator object.
     *
     * @param srManager segment routing manager reference
     */
    RoutingRulePopulator(SegmentRoutingManager srManager) {
        this.srManager = srManager;
        this.config = checkNotNull(srManager.deviceConfiguration);
        this.rulePopulationCounter = new AtomicLong(0);
    }

    /**
     * Resets the population counter.
     */
    void resetCounter() {
        rulePopulationCounter.set(0);
    }

    /**
     * Returns the number of rules populated.
     *
     * @return number of rules
     */
    long getCounter() {
        return rulePopulationCounter.get();
    }

    /**
     * Populate a bridging rule on given deviceId that matches given mac, given vlan and
     * output to given port.
     *
     * @param deviceId device ID
     * @param port port
     * @param mac mac address
     * @param vlanId VLAN ID
     */
    void populateBridging(DeviceId deviceId, PortNumber port, MacAddress mac, VlanId vlanId) {
        ForwardingObjective.Builder fob = bridgingFwdObjBuilder(deviceId, mac, vlanId, port, false);
        if (fob == null) {
            log.warn("Fail to build fwd obj for host {}/{}. Abort.", mac, vlanId);
            return;
        }

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Brigding rule for {}/{} populated", mac, vlanId),
                (objective, error) -> log.warn("Failed to populate bridging rule for {}/{}: {}", mac, vlanId, error));
        srManager.flowObjectiveService.forward(deviceId, fob.add(context));
    }

    /**
     * Revoke a bridging rule on given deviceId that matches given mac, given vlan and
     * output to given port.
     *
     * @param deviceId device ID
     * @param port port
     * @param mac mac address
     * @param vlanId VLAN ID
     */
    void revokeBridging(DeviceId deviceId, PortNumber port, MacAddress mac, VlanId vlanId) {
        ForwardingObjective.Builder fob = bridgingFwdObjBuilder(deviceId, mac, vlanId, port, true);
        if (fob == null) {
            log.warn("Fail to build fwd obj for host {}/{}. Abort.", mac, vlanId);
            return;
        }

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Brigding rule for {}/{} revoked", mac, vlanId),
                (objective, error) -> log.warn("Failed to revoke bridging rule for {}/{}: {}", mac, vlanId, error));
        srManager.flowObjectiveService.forward(deviceId, fob.remove(context));
    }

    /**
     * Generates a forwarding objective builder for bridging rules.
     * <p>
     * The forwarding objective bridges packets destined to a given MAC to
     * given port on given device.
     *
     * @param deviceId Device that host attaches to
     * @param mac MAC address of the host
     * @param hostVlanId VLAN ID of the host
     * @param outport Port that host attaches to
     * @param revoke true if forwarding objective is meant to revoke forwarding rule
     * @return Forwarding objective builder
     */
    private ForwardingObjective.Builder bridgingFwdObjBuilder(
            DeviceId deviceId, MacAddress mac, VlanId hostVlanId, PortNumber outport, boolean revoke) {
        ConnectPoint connectPoint = new ConnectPoint(deviceId, outport);
        VlanId untaggedVlan = srManager.interfaceService.getUntaggedVlanId(connectPoint);
        Set<VlanId> taggedVlans = srManager.interfaceService.getTaggedVlanId(connectPoint);
        VlanId nativeVlan = srManager.interfaceService.getNativeVlanId(connectPoint);

        // Create host selector
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchEthDst(mac);

        // Create host treatment
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.immediate().setOutput(outport);

        // Create host meta
        TrafficSelector.Builder mbuilder = DefaultTrafficSelector.builder();

        // Adjust the selector, treatment and meta according to VLAN configuration
        if (taggedVlans.contains(hostVlanId)) {
            sbuilder.matchVlanId(hostVlanId);
            mbuilder.matchVlanId(hostVlanId);
        } else if (hostVlanId.equals(VlanId.NONE)) {
            if (untaggedVlan != null) {
                sbuilder.matchVlanId(untaggedVlan);
                mbuilder.matchVlanId(untaggedVlan);
                tbuilder.immediate().popVlan();
            } else if (nativeVlan != null) {
                sbuilder.matchVlanId(nativeVlan);
                mbuilder.matchVlanId(nativeVlan);
                tbuilder.immediate().popVlan();
            } else {
                log.warn("Untagged host {}/{} is not allowed on {} without untagged or native" +
                        "vlan config", mac, hostVlanId, connectPoint);
                return null;
            }
        } else {
            log.warn("Tagged host {}/{} is not allowed on {} without VLAN listed in tagged vlan",
                    mac, hostVlanId, connectPoint);
            return null;
        }

        // All forwarding is via Groups. Drivers can re-purpose to flow-actions if needed.
        // If the objective is to revoke an existing rule, and for some reason
        // the next-objective does not exist, then a new one should not be created
        int portNextObjId = srManager.getPortNextObjectiveId(deviceId, outport,
                tbuilder.build(), mbuilder.build(), !revoke);
        if (portNextObjId == -1) {
            // Warning log will come from getPortNextObjective method
            return null;
        }

        return DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withSelector(sbuilder.build())
                .nextStep(portNextObjId)
                .withPriority(100)
                .fromApp(srManager.appId)
                .makePermanent();
    }

    /**
     * Populate or revoke a bridging rule on given deviceId that matches given vlanId,
     * and hostMAC connected to given port, and output to given port only when
     * vlan information is valid.
     *
     * @param deviceId device ID that host attaches to
     * @param portNum port number that host attaches to
     * @param hostMac mac address of the host connected to the switch port
     * @param vlanId Vlan ID configured on the switch port
     * @param popVlan true to pop Vlan tag at TrafficTreatment, false otherwise
     * @param install true to populate the objective, false to revoke
     */
    // TODO Refactor. There are a lot of duplications between this method, populateBridging,
    //      revokeBridging and bridgingFwdObjBuilder.
    void updateBridging(DeviceId deviceId, PortNumber portNum, MacAddress hostMac,
                        VlanId vlanId, boolean popVlan, boolean install) {
        // Create host selector
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchEthDst(hostMac);

        // Create host meta
        TrafficSelector.Builder mbuilder = DefaultTrafficSelector.builder();

        sbuilder.matchVlanId(vlanId);
        mbuilder.matchVlanId(vlanId);

        // Create host treatment
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.immediate().setOutput(portNum);

        if (popVlan) {
            tbuilder.immediate().popVlan();
        }

        int portNextObjId = srManager.getPortNextObjectiveId(deviceId, portNum,
                tbuilder.build(), mbuilder.build(), install);
        if (portNextObjId != -1) {
            ForwardingObjective.Builder fob = DefaultForwardingObjective.builder()
                    .withFlag(ForwardingObjective.Flag.SPECIFIC)
                    .withSelector(sbuilder.build())
                    .nextStep(portNextObjId)
                    .withPriority(100)
                    .fromApp(srManager.appId)
                    .makePermanent();

            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("Brigding rule for {}/{} {}", hostMac, vlanId,
                            install ? "populated" : "revoked"),
                    (objective, error) -> log.warn("Failed to {} bridging rule for {}/{}: {}",
                            install ? "populate" : "revoke", hostMac, vlanId, error));
            srManager.flowObjectiveService.forward(deviceId, install ? fob.add(context) : fob.remove(context));
        } else {
            log.warn("Failed to retrieve next objective for {}/{}", hostMac, vlanId);
        }
    }

    /**
     * Populates IP rules for a route that has direct connection to the switch.
     * This method should not be invoked directly without going through DefaultRoutingHandler.
     *
     * @param deviceId device ID of the device that next hop attaches to
     * @param prefix IP prefix of the route
     * @param hostMac MAC address of the next hop
     * @param hostVlanId Vlan ID of the nexthop
     * @param outPort port where the next hop attaches to
     */
    void populateRoute(DeviceId deviceId, IpPrefix prefix,
                              MacAddress hostMac, VlanId hostVlanId, PortNumber outPort) {
        log.debug("Populate direct routing entry for route {} at {}:{}",
                prefix, deviceId, outPort);
        ForwardingObjective.Builder fwdBuilder;
        try {
            fwdBuilder = routingFwdObjBuilder(deviceId, prefix, hostMac,
                                              hostVlanId, outPort, false);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting direct populateRoute");
            return;
        }
        if (fwdBuilder == null) {
            log.warn("Aborting host routing table entry due "
                    + "to error for dev:{} route:{}", deviceId, prefix);
            return;
        }

        int nextId = fwdBuilder.add().nextId();
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Direct routing rule for route {} populated. nextId={}",
                                         prefix, nextId),
                (objective, error) ->
                        log.warn("Failed to populate direct routing rule for route {}: {}",
                                 prefix, error));
        srManager.flowObjectiveService.forward(deviceId, fwdBuilder.add(context));
        rulePopulationCounter.incrementAndGet();
    }

    /**
     * Removes IP rules for a route when the next hop is gone.
     * This method should not be invoked directly without going through DefaultRoutingHandler.
     *
     * @param deviceId device ID of the device that next hop attaches to
     * @param prefix IP prefix of the route
     * @param hostMac MAC address of the next hop
     * @param hostVlanId Vlan ID of the nexthop
     * @param outPort port that next hop attaches to
     */
    void revokeRoute(DeviceId deviceId, IpPrefix prefix,
            MacAddress hostMac, VlanId hostVlanId, PortNumber outPort) {
        log.debug("Revoke IP table entry for route {} at {}:{}",
                prefix, deviceId, outPort);
        ForwardingObjective.Builder fwdBuilder;
        try {
            fwdBuilder = routingFwdObjBuilder(deviceId, prefix, hostMac,
                                              hostVlanId, outPort, true);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting revokeIpRuleForHost.");
            return;
        }
        if (fwdBuilder == null) {
            log.warn("Aborting host routing table entries due "
                    + "to error for dev:{} route:{}", deviceId, prefix);
            return;
        }
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("IP rule for route {} revoked", prefix),
                (objective, error) ->
                        log.warn("Failed to revoke IP rule for route {}: {}", prefix, error));
        srManager.flowObjectiveService.forward(deviceId, fwdBuilder.remove(context));
    }

    /**
     * Returns a forwarding objective builder for routing rules.
     * <p>
     * The forwarding objective routes packets destined to a given prefix to
     * given port on given device with given destination MAC.
     *
     * @param deviceId device ID
     * @param prefix prefix that need to be routed
     * @param hostMac MAC address of the nexthop
     * @param hostVlanId Vlan ID of the nexthop
     * @param outPort port where the nexthop attaches to
     * @param revoke true if forwarding objective is meant to revoke forwarding rule
     * @return forwarding objective builder
     * @throws DeviceConfigNotFoundException if given device is not configured
     */
    private ForwardingObjective.Builder routingFwdObjBuilder(
            DeviceId deviceId, IpPrefix prefix,
            MacAddress hostMac, VlanId hostVlanId, PortNumber outPort,
            boolean revoke)
            throws DeviceConfigNotFoundException {
        MacAddress deviceMac;
        deviceMac = config.getDeviceMac(deviceId);

        ConnectPoint connectPoint = new ConnectPoint(deviceId, outPort);
        VlanId untaggedVlan = srManager.interfaceService.getUntaggedVlanId(connectPoint);
        Set<VlanId> taggedVlans = srManager.interfaceService.getTaggedVlanId(connectPoint);
        VlanId nativeVlan = srManager.interfaceService.getNativeVlanId(connectPoint);

        // Create route selector
        TrafficSelector.Builder sbuilder = buildIpSelectorFromIpPrefix(prefix);

        // Create route treatment
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.deferred()
                .setEthDst(hostMac)
                .setEthSrc(deviceMac)
                .setOutput(outPort);

        // Create route meta
        TrafficSelector.Builder mbuilder = DefaultTrafficSelector.builder();

        // Adjust the meta according to VLAN configuration
        if (taggedVlans.contains(hostVlanId)) {
            tbuilder.setVlanId(hostVlanId);
        } else if (hostVlanId.equals(VlanId.NONE)) {
            if (untaggedVlan != null) {
                mbuilder.matchVlanId(untaggedVlan);
            } else if (nativeVlan != null) {
                mbuilder.matchVlanId(nativeVlan);
            } else {
                log.warn("Untagged nexthop {}/{} is not allowed on {} without untagged or native vlan",
                        hostMac, hostVlanId, connectPoint);
                return null;
            }
        } else {
            // Internally-assigned dummy VLAN id will be given as hostVlanId
            // when destination is double-tagged.
            VlanId vlanId = srManager.dummyVlanIdStore().get(
                    new DummyVlanIdStoreKey(connectPoint, prefix.address()));
            if (vlanId != null && vlanId.equals(hostVlanId)) {
                tbuilder.setVlanId(hostVlanId);
                mbuilder.matchVlanId(VlanId.ANY);
            } else {
                log.warn("Tagged nexthop {}/{} is not allowed on {} without VLAN listed"
                                 + " in tagged vlan", hostMac, hostVlanId, connectPoint);
                return null;
            }
        }
        // if the objective is to revoke an existing rule, and for some reason
        // the next-objective does not exist, then a new one should not be created
        int portNextObjId = srManager.getPortNextObjectiveId(deviceId, outPort,
                                          tbuilder.build(), mbuilder.build(), !revoke);
        if (portNextObjId == -1) {
            // Warning log will come from getPortNextObjective method
            return null;
        }

        return DefaultForwardingObjective.builder()
                .withSelector(sbuilder.build())
                .nextStep(portNextObjId)
                .fromApp(srManager.appId).makePermanent()
                .withPriority(getPriorityFromPrefix(prefix))
                .withFlag(ForwardingObjective.Flag.SPECIFIC);
    }

    /**
     * Populates IP flow rules for all the given prefixes reachable from the
     * destination switch(es).
     *
     * @param targetSw switch where rules are to be programmed
     * @param subnets subnets/prefixes being added
     * @param destSw1 destination switch where the prefixes are reachable
     * @param destSw2 paired destination switch if one exists for the subnets/prefixes.
     *                Should be null if there is no paired destination switch (by config)
     *                or if the given prefixes are reachable only via destSw1
     * @param nextHops a map containing a set of next-hops for each destination switch.
     *                 If destSw2 is not null, then this map must contain an
     *                 entry for destSw2 with its next-hops from the targetSw
     *                 (although the next-hop set may be empty in certain scenarios).
     *                 If destSw2 is null, there should not be an entry in this
     *                 map for destSw2.
     * @return true if all rules are set successfully, false otherwise
     */
    boolean populateIpRuleForSubnet(DeviceId targetSw, Set<IpPrefix> subnets,
            DeviceId destSw1, DeviceId destSw2, Map<DeviceId, Set<DeviceId>> nextHops) {
        for (IpPrefix subnet : subnets) {
            if (!populateIpRuleForRouter(targetSw, subnet, destSw1, destSw2, nextHops)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Revokes IP flow rules for the subnets from given device.
     *
     * @param targetSw target switch from which the subnets need to be removed
     * @param subnets subnet being removed
     * @return true if all rules are removed successfully, false otherwise
     */
    boolean revokeIpRuleForSubnet(DeviceId targetSw, Set<IpPrefix> subnets) {
        for (IpPrefix subnet : subnets) {
            if (!revokeIpRuleForRouter(targetSw, subnet)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Populates IP flow rules for an IP prefix in the target device. The prefix
     * is reachable via destination device(s).
     *
     * @param targetSw target device ID to set the rules
     * @param ipPrefix the IP prefix
     * @param destSw1 destination switch where the prefixes are reachable
     * @param destSw2 paired destination switch if one exists for the subnets/prefixes.
     *                Should be null if there is no paired destination switch (by config)
     *                or if the given prefixes are reachable only via destSw1
     * @param nextHops map of destination switches and their next-hops.
     *                  Should only contain destination switches that are
     *                  actually meant to be routed to. If destSw2 is null, there
     *                  should not be an entry for destSw2 in this map.
     * @return true if all rules are set successfully, false otherwise
     */
    private boolean populateIpRuleForRouter(DeviceId targetSw,
                                           IpPrefix ipPrefix, DeviceId destSw1,
                                           DeviceId destSw2,
                                           Map<DeviceId, Set<DeviceId>> nextHops) {
        int segmentId1, segmentId2 = -1;
        try {
            if (ipPrefix.isIp4()) {
                segmentId1 = config.getIPv4SegmentId(destSw1);
                if (destSw2 != null) {
                    segmentId2 = config.getIPv4SegmentId(destSw2);
                }
            } else {
                segmentId1 = config.getIPv6SegmentId(destSw1);
                if (destSw2 != null) {
                    segmentId2 = config.getIPv6SegmentId(destSw2);
                }
            }
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateIpRuleForRouter.");
            return false;
        }

        TrafficSelector.Builder sbuilder = buildIpSelectorFromIpPrefix(ipPrefix);
        TrafficSelector selector = sbuilder.build();

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        DestinationSet ds;
        TrafficTreatment treatment;
        DestinationSet.DestinationSetType dsType;

        if (destSw2 == null) {
            // single dst - create destination set based on next-hop
            // If the next hop is the same as the final destination, then MPLS
            // label is not set.
            Set<DeviceId> nhd1 = nextHops.get(destSw1);
            if (nhd1.size() == 1 && nhd1.iterator().next().equals(destSw1)) {
                tbuilder.immediate().decNwTtl();
                ds = DestinationSet.createTypePushNone(destSw1);
                treatment = tbuilder.build();
            } else {
                ds = DestinationSet.createTypePushBos(segmentId1, destSw1);
                treatment = null;
            }
        } else {
            // dst pair - IP rules for dst-pairs are always from other edge nodes
            // the destination set needs to have both destinations, even if there
            // are no next hops to one of them
            ds = DestinationSet.createTypePushBos(segmentId1, destSw1, segmentId2, destSw2);
            treatment = null;
        }

        // setup metadata to pass to nextObjective - indicate the vlan on egress
        // if needed by the switch pipeline. Since neighbor sets are always to
        // other neighboring routers, there is no subnet assigned on those ports.
        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder(selector);
        metabuilder.matchVlanId(SegmentRoutingManager.INTERNAL_VLAN);
        DefaultGroupHandler grpHandler = srManager.getGroupHandler(targetSw);
        if (grpHandler == null) {
            log.warn("populateIPRuleForRouter: groupHandler for device {} "
                    + "not found", targetSw);
            return false;
        }

        int nextId = grpHandler.getNextObjectiveId(ds, nextHops,
                                                   metabuilder.build(), false);
        if (nextId <= 0) {
            log.warn("No next objective in {} for ds: {}", targetSw, ds);
            return false;
        }

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                .builder()
                .fromApp(srManager.appId)
                .makePermanent()
                .nextStep(nextId)
                .withSelector(selector)
                .withPriority(getPriorityFromPrefix(ipPrefix))
                .withFlag(ForwardingObjective.Flag.SPECIFIC);
        if (treatment != null) {
            fwdBuilder.withTreatment(treatment);
        }
        log.debug("Installing IPv4 forwarding objective for router IP/subnet {} "
                + "in switch {} with nextId: {}", ipPrefix, targetSw, nextId);
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("IP rule for router {} populated in dev:{}",
                                         ipPrefix, targetSw),
                (objective, error) ->
                        log.warn("Failed to populate IP rule for router {}: {} in dev:{}",
                                 ipPrefix, error, targetSw));
        srManager.flowObjectiveService.forward(targetSw, fwdBuilder.add(context));
        rulePopulationCounter.incrementAndGet();

        return true;
    }

    /**
     * Revokes IP flow rules for the router IP address from given device.
     *
     * @param targetSw target switch from which the ipPrefix need to be removed
     * @param ipPrefix the IP address of the destination router
     * @return true if all rules are removed successfully, false otherwise
     */
    private boolean revokeIpRuleForRouter(DeviceId targetSw, IpPrefix ipPrefix) {
        TrafficSelector.Builder sbuilder = buildIpSelectorFromIpPrefix(ipPrefix);
        TrafficSelector selector = sbuilder.build();
        TrafficTreatment dummyTreatment = DefaultTrafficTreatment.builder().build();

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                .builder()
                .fromApp(srManager.appId)
                .makePermanent()
                .withSelector(selector)
                .withTreatment(dummyTreatment)
                .withPriority(getPriorityFromPrefix(ipPrefix))
                .withFlag(ForwardingObjective.Flag.SPECIFIC);

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("IP rule for router {} revoked from {}", ipPrefix, targetSw),
                (objective, error) -> log.warn("Failed to revoke IP rule for router {} from {}: {}",
                        ipPrefix, targetSw, error));
        srManager.flowObjectiveService.forward(targetSw, fwdBuilder.remove(context));

        return true;
    }

    /**
     * Populates MPLS flow rules in the target device to point towards the
     * destination device.
     *
     * @param targetSwId target device ID of the switch to set the rules
     * @param destSwId destination switch device ID
     * @param nextHops next hops switch ID list
     * @param routerIp the router ip of the destination switch
     * @return true if all rules are set successfully, false otherwise
     */
    boolean populateMplsRule(DeviceId targetSwId, DeviceId destSwId,
                             Set<DeviceId> nextHops, IpAddress routerIp) {
        int segmentId;
        try {
            if (routerIp.isIp4()) {
                segmentId = config.getIPv4SegmentId(destSwId);
            } else {
                segmentId = config.getIPv6SegmentId(destSwId);
            }
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateMplsRule.");
            return false;
        }

        List<ForwardingObjective> fwdObjs = new ArrayList<>();
        Collection<ForwardingObjective> fwdObjsMpls;
        // Generates the transit rules used by the standard "routing".
        fwdObjsMpls = handleMpls(targetSwId, destSwId, nextHops, segmentId,
                                 routerIp, true);
        if (fwdObjsMpls.isEmpty()) {
            return false;
        }
        fwdObjs.addAll(fwdObjsMpls);

        // Generates the transit rules used by the MPLS Pwaas.
        int pwSrLabel;
        try {
            pwSrLabel = config.getPWRoutingLabel(destSwId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage()
                    + " Aborting populateMplsRule. No label for PseudoWire traffic.");
            return false;
        }
        fwdObjsMpls = handleMpls(targetSwId, destSwId, nextHops, pwSrLabel,
                                 routerIp, false);
        if (fwdObjsMpls.isEmpty()) {
            return false;
        }
        fwdObjs.addAll(fwdObjsMpls);

        for (ForwardingObjective fwdObj : fwdObjs) {
            log.debug("Sending MPLS fwd obj {} for SID {}-> next {} in sw: {}",
                      fwdObj.id(), segmentId, fwdObj.nextId(), targetSwId);
            srManager.flowObjectiveService.forward(targetSwId, fwdObj);
            rulePopulationCounter.incrementAndGet();
        }

        return true;
    }

    /**
     * Differentiates between popping and swapping labels when building an MPLS
     * forwarding objective.
     *
     * @param targetSwId the target sw
     * @param destSwId the destination sw
     * @param nextHops the set of next hops
     * @param segmentId the segmentId to match representing the destination
     *            switch
     * @param routerIp the router ip representing the destination switch
     * @return a collection of fwdobjective
     */
    private Collection<ForwardingObjective> handleMpls(
                                        DeviceId targetSwId,
                                        DeviceId destSwId,
                                        Set<DeviceId> nextHops,
                                        int segmentId,
                                        IpAddress routerIp,
                                        boolean isMplsBos) {

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        List<ForwardingObjective.Builder> fwdObjBuilders = Lists.newArrayList();
        // For the transport of Pwaas we can use two or three MPLS label
        sbuilder.matchEthType(Ethernet.MPLS_UNICAST);
        sbuilder.matchMplsLabel(MplsLabel.mplsLabel(segmentId));
        sbuilder.matchMplsBos(isMplsBos);
        TrafficSelector selector = sbuilder.build();

        // setup metadata to pass to nextObjective - indicate the vlan on egress
        // if needed by the switch pipeline. Since mpls next-hops are always to
        // other neighboring routers, there is no subnet assigned on those ports.
        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder(selector);
        metabuilder.matchVlanId(SegmentRoutingManager.INTERNAL_VLAN);

        if (nextHops.size() == 1 && destSwId.equals(nextHops.toArray()[0])) {
            // If the next hop is the destination router for the segment, do pop
            log.debug("populateMplsRule: Installing MPLS forwarding objective for "
                    + "label {} in switch {} with pop to next-hops {}",
                    segmentId, targetSwId, nextHops);
            ForwardingObjective.Builder fwdObjNoBosBuilder =
                    getMplsForwardingObjective(targetSwId,
                                               nextHops,
                                               true,
                                               isMplsBos,
                                               metabuilder.build(),
                                               routerIp,
                                               segmentId,
                                               destSwId);
            // Error case, we cannot handle, exit.
            if (fwdObjNoBosBuilder == null) {
                return Collections.emptyList();
            }
            fwdObjBuilders.add(fwdObjNoBosBuilder);

        } else {
            // next hop is not destination, irrespective of the number of next
            // hops (1 or more) -- SR CONTINUE case (swap with self)
            log.debug("Installing MPLS forwarding objective for "
                    + "label {} in switch {} without pop to next-hops {}",
                    segmentId, targetSwId, nextHops);
            ForwardingObjective.Builder fwdObjNoBosBuilder =
                    getMplsForwardingObjective(targetSwId,
                                               nextHops,
                                               false,
                                               isMplsBos,
                                               metabuilder.build(),
                                               routerIp,
                                               segmentId,
                                               destSwId);
            // Error case, we cannot handle, exit.
            if (fwdObjNoBosBuilder == null) {
                return Collections.emptyList();
            }
            fwdObjBuilders.add(fwdObjNoBosBuilder);

        }

        List<ForwardingObjective> fwdObjs = Lists.newArrayList();
        // We add the final property to the fwdObjs.
        for (ForwardingObjective.Builder fwdObjBuilder : fwdObjBuilders) {
            ((Builder) ((Builder) fwdObjBuilder
                    .fromApp(srManager.appId)
                    .makePermanent())
                    .withSelector(selector)
                    .withPriority(SegmentRoutingService.DEFAULT_PRIORITY))
                    .withFlag(ForwardingObjective.Flag.SPECIFIC);

            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) ->
                            log.debug("MPLS rule {} for SID {} populated in dev:{} ",
                                      objective.id(), segmentId, targetSwId),
                    (objective, error) ->
                            log.warn("Failed to populate MPLS rule {} for SID {}: {} in dev:{}",
                                     objective.id(), segmentId, error, targetSwId));

            ForwardingObjective fob = fwdObjBuilder.add(context);
            fwdObjs.add(fob);
        }

        return fwdObjs;
    }

    /**
     * Returns a Forwarding Objective builder for the MPLS rule that references
     * the desired Next Objective. Creates a DestinationSet that allows the
     * groupHandler to create or find the required next objective.
     *
     * @param targetSw the target sw
     * @param nextHops the set of next hops
     * @param phpRequired true if penultimate-hop-popping is required
     * @param isBos true if matched label is bottom-of-stack
     * @param meta metadata for creating next objective
     * @param routerIp the router ip representing the destination switch
     * @param destSw the destination sw
     * @return the mpls forwarding objective builder
     */
    private ForwardingObjective.Builder getMplsForwardingObjective(
                                             DeviceId targetSw,
                                             Set<DeviceId> nextHops,
                                             boolean phpRequired,
                                             boolean isBos,
                                             TrafficSelector meta,
                                             IpAddress routerIp,
                                             int segmentId,
                                             DeviceId destSw) {

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                .builder().withFlag(ForwardingObjective.Flag.SPECIFIC);

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        DestinationSet ds = null;
        DestinationSet.DestinationSetType dstType = null;
        boolean simple = false;
        if (phpRequired) {
            // php case - pop should always be flow-action
            log.debug("getMplsForwardingObjective: php required");
            tbuilder.deferred().copyTtlIn();
            if (isBos) {
                if (routerIp.isIp4()) {
                    tbuilder.deferred().popMpls(EthType.EtherType.IPV4.ethType());
                } else {
                    tbuilder.deferred().popMpls(EthType.EtherType.IPV6.ethType());
                }
                tbuilder.decNwTtl();
                // standard case -> BoS == True; pop results in IP packet and forwarding
                // is via an ECMP group
                ds = DestinationSet.createTypePopBos(destSw);
            } else {
                tbuilder.deferred().popMpls(EthType.EtherType.MPLS_UNICAST.ethType())
                    .decMplsTtl();
                // double-label case -> BoS == False, pop results in MPLS packet
                // depending on configuration we can ECMP this packet or choose one output
                ds = DestinationSet.createTypePopNotBos(destSw);
                if (!srManager.getMplsEcmp()) {
                   simple = true;
                }
            }
        } else {
            // swap with self case - SR CONTINUE
            log.debug("getMplsForwardingObjective: swap with self");
            tbuilder.deferred().decMplsTtl();
            // swap results in MPLS packet with same BoS bit regardless of bit value
            // depending on configuration we can ECMP this packet or choose one output
            // differentiate here between swap with not bos or swap with bos
            ds = isBos ? DestinationSet.createTypeSwapBos(segmentId, destSw) :
                    DestinationSet.createTypeSwapNotBos(segmentId, destSw);
            if (!srManager.getMplsEcmp()) {
                simple = true;
            }
        }

        fwdBuilder.withTreatment(tbuilder.build());
        log.debug("Trying to get a nextObjId for mpls rule on device:{} to ds:{}",
                  targetSw, ds);
        DefaultGroupHandler gh = srManager.getGroupHandler(targetSw);
        if (gh == null) {
            log.warn("getNextObjectiveId query - groupHandler for device {} "
                    + "not found", targetSw);
            return null;
        }

        Map<DeviceId, Set<DeviceId>> dstNextHops = new HashMap<>();
        dstNextHops.put(destSw, nextHops);
        int nextId = gh.getNextObjectiveId(ds, dstNextHops, meta, simple);
        if (nextId <= 0) {
            log.warn("No next objective in {} for ds: {}", targetSw, ds);
            return null;
        } else {
            log.debug("nextObjId found:{} for mpls rule on device:{} to ds:{}",
                      nextId, targetSw, ds);
        }

        fwdBuilder.nextStep(nextId);
        return fwdBuilder;
    }

    /**
     * Creates a filtering objective to permit all untagged packets with a
     * dstMac corresponding to the router's MAC address. For those pipelines
     * that need to internally assign vlans to untagged packets, this method
     * provides per-subnet vlan-ids as metadata.
     * <p>
     * Note that the vlan assignment and filter programming should only be done by
     * the master for a switch. This method is typically called at deviceAdd and
     * programs filters only for the enabled ports of the device. For port-updates,
     * that enable/disable ports after device add, singlePortFilter methods should
     * be called.
     *
     * @param deviceId  the switch dpid for the router
     * @return PortFilterInfo information about the processed ports
     */
    PortFilterInfo populateVlanMacFilters(DeviceId deviceId) {
        log.debug("Installing per-port filtering objective for untagged "
                + "packets in device {}", deviceId);

        List<Port> devPorts = srManager.deviceService.getPorts(deviceId);
        if (devPorts == null || devPorts.isEmpty()) {
            log.warn("Device {} ports not available. Unable to add MacVlan filters",
                     deviceId);
            return null;
        }
        int disabledPorts = 0, errorPorts = 0, filteredPorts = 0;
        for (Port port : devPorts) {
            if (!port.isEnabled()) {
                disabledPorts++;
                continue;
            }
            if (processSinglePortFilters(deviceId, port.number(), true)) {
                filteredPorts++;
            } else {
                errorPorts++;
            }
        }
        log.debug("Filtering on dev:{}, disabledPorts:{}, errorPorts:{}, filteredPorts:{}",
                  deviceId, disabledPorts, errorPorts, filteredPorts);
        return new PortFilterInfo(disabledPorts, errorPorts, filteredPorts);
    }

    /**
     * Creates or removes filtering objectives for a single port. Should only be
     * called by the master for a switch.
     *
     * @param deviceId device identifier
     * @param portnum  port identifier for port to be filtered
     * @param install true to install the filtering objective, false to remove
     * @return true if no errors occurred during the build of the filtering objective
     */
    boolean processSinglePortFilters(DeviceId deviceId, PortNumber portnum, boolean install) {
        ConnectPoint connectPoint = new ConnectPoint(deviceId, portnum);
        VlanId untaggedVlan = srManager.interfaceService.getUntaggedVlanId(connectPoint);
        Set<VlanId> taggedVlans = srManager.interfaceService.getTaggedVlanId(connectPoint);
        VlanId nativeVlan = srManager.interfaceService.getNativeVlanId(connectPoint);

        // Do not configure filter for edge ports where double-tagged hosts are connected.
        if (taggedVlans.size() != 0) {
            // Filter for tagged vlans
            if (!srManager.interfaceService.getTaggedVlanId(connectPoint).stream().allMatch(taggedVlanId ->
                    processSinglePortFiltersInternal(deviceId, portnum, false, taggedVlanId, install))) {
                return false;
            }
            if (nativeVlan != null) {
                // Filter for native vlan
                if (!processSinglePortFiltersInternal(deviceId, portnum, true, nativeVlan, install)) {
                    return false;
                }
            }
        } else if (untaggedVlan != null) {
            // Filter for untagged vlan
            if (!processSinglePortFiltersInternal(deviceId, portnum, true, untaggedVlan, install)) {
                return false;
            }
        } else if (!hasIPConfiguration(connectPoint)) {
            // Filter for unconfigured upstream port, using INTERNAL_VLAN
            if (!processSinglePortFiltersInternal(deviceId, portnum, true, INTERNAL_VLAN, install)) {
                return false;
            }
            // Filter for receiveing pseudowire traffic
            if (!processSinglePortFiltersInternal(deviceId, portnum, false, PSEUDOWIRE_VLAN, install)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Updates filtering objectives for a single port. Should only be called by
     * the master for a switch
     * @param deviceId device identifier
     * @param portNum  port identifier for port to be filtered
     * @param pushVlan true to push vlan, false otherwise
     * @param vlanId vlan identifier
     * @param install true to install the filtering objective, false to remove
     */
    void updateSinglePortFilters(DeviceId deviceId, PortNumber portNum,
                                 boolean pushVlan, VlanId vlanId, boolean install) {
        if (!processSinglePortFiltersInternal(deviceId, portNum, pushVlan, vlanId, install)) {
            log.warn("Failed to update FilteringObjective for {}/{} with vlan {}",
                     deviceId, portNum, vlanId);
        }
    }

    private boolean processSinglePortFiltersInternal(DeviceId deviceId, PortNumber portnum,
                                                      boolean pushVlan, VlanId vlanId, boolean install) {
        boolean doTMAC = true;

        if (!pushVlan) {
            // Skip the tagged vlans belonging to an interface without an IP address
            Set<Interface> ifaces = srManager.interfaceService
                    .getInterfacesByPort(new ConnectPoint(deviceId, portnum))
                    .stream()
                    .filter(intf -> intf.vlanTagged().contains(vlanId) && intf.ipAddressesList().isEmpty())
                    .collect(Collectors.toSet());
            if (!ifaces.isEmpty()) {
                log.debug("processSinglePortFiltersInternal: skipping TMAC for vlan {} at {}/{} - no IP",
                          vlanId, deviceId, portnum);
                doTMAC = false;
            }
        }

        FilteringObjective.Builder fob = buildFilteringObjective(deviceId, portnum, pushVlan, vlanId, doTMAC);
        if (fob == null) {
            // error encountered during build
            return false;
        }
        log.debug("{} filtering objectives for dev/port: {}/{}",
                 install ? "Installing" : "Removing", deviceId, portnum);
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Filter for {}/{} {}", deviceId, portnum,
                        install ? "installed" : "removed"),
                (objective, error) -> log.warn("Failed to {} filter for {}/{}: {}",
                        install ? "install" : "remove", deviceId, portnum, error));
        if (install) {
            srManager.flowObjectiveService.filter(deviceId, fob.add(context));
        } else {
            srManager.flowObjectiveService.filter(deviceId, fob.remove(context));
        }
        return true;
    }

    private FilteringObjective.Builder buildFilteringObjective(DeviceId deviceId, PortNumber portnum,
                                                               boolean pushVlan, VlanId vlanId, boolean doTMAC) {
        MacAddress deviceMac;
        try {
            deviceMac = config.getDeviceMac(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Processing SinglePortFilters aborted");
            return null;
        }
        FilteringObjective.Builder fob = DefaultFilteringObjective.builder();

        if (doTMAC) {
            fob.withKey(Criteria.matchInPort(portnum))
                    .addCondition(Criteria.matchEthDst(deviceMac))
                    .withPriority(SegmentRoutingService.DEFAULT_PRIORITY);
        } else {
            fob.withKey(Criteria.matchInPort(portnum))
                    .withPriority(SegmentRoutingService.DEFAULT_PRIORITY);
        }

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        if (pushVlan) {
            fob.addCondition(Criteria.matchVlanId(VlanId.NONE));
            tBuilder.pushVlan().setVlanId(vlanId);
        } else {
            fob.addCondition(Criteria.matchVlanId(vlanId));
        }

        // NOTE: Some switch hardware share the same filtering flow among different ports.
        //       We use this metadata to let the driver know that there is no more enabled port
        //       within the same VLAN on this device.
        if (noMoreEnabledPort(deviceId, vlanId)) {
            tBuilder.wipeDeferred();
        }

        fob.withMeta(tBuilder.build());

        fob.permit().fromApp(srManager.appId);
        return fob;
    }

    /**
     * Creates or removes filtering objectives for a double-tagged host on a port.
     *
     * @param deviceId device identifier
     * @param portNum  port identifier for port to be filtered
     * @param outerVlan outer VLAN ID
     * @param innerVlan inner VLAN ID
     * @param install true to install the filtering objective, false to remove
     */
    void processDoubleTaggedFilter(DeviceId deviceId, PortNumber portNum, VlanId outerVlan,
                                   VlanId innerVlan, boolean install) {
        FilteringObjective.Builder fob = buildDoubleTaggedFilteringObj(deviceId, portNum, outerVlan, innerVlan);
        if (fob == null) {
            // error encountered during build
            return;
        }
        log.debug("{} double-tagged filtering objectives for dev/port: {}/{}",
                  install ? "Installing" : "Removing", deviceId, portNum);
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Filter for {}/{} {}", deviceId, portNum,
                                         install ? "installed" : "removed"),
                (objective, error) -> log.warn("Failed to {} filter for {}/{}: {}",
                                               install ? "install" : "remove", deviceId, portNum, error));
        if (install) {
            srManager.flowObjectiveService.filter(deviceId, fob.add(context));
        } else {
            srManager.flowObjectiveService.filter(deviceId, fob.remove(context));
        }
    }

    private FilteringObjective.Builder buildDoubleTaggedFilteringObj(DeviceId deviceId, PortNumber portNum,
                                                                     VlanId outerVlan, VlanId innerVlan) {
        MacAddress deviceMac;
        try {
            deviceMac = config.getDeviceMac(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Processing DoubleTaggedFilters aborted");
            return null;
        }
        FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
        // Outer vlan id match should be appeared before inner vlan id match.
        fob.withKey(Criteria.matchInPort(portNum))
                .addCondition(Criteria.matchEthDst(deviceMac))
                .addCondition(Criteria.matchVlanId(outerVlan))
                .addCondition(Criteria.matchVlanId(innerVlan))
                .withPriority(SegmentRoutingService.DEFAULT_PRIORITY);

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        // Pop outer vlan
        tBuilder.popVlan();

        // NOTE: Some switch hardware share the same filtering flow among different ports.
        //       We use this metadata to let the driver know that there is no more enabled port
        //       within the same VLAN on this device.
        if (noMoreEnabledPort(deviceId, outerVlan)) {
            tBuilder.wipeDeferred();
        }

        fob.withMeta(tBuilder.build());

        fob.permit().fromApp(srManager.appId);
        return fob;
    }

    /**
     * Creates a forwarding objective to punt all IP packets, destined to the
     * router's port IP addresses, to the controller. Note that the input
     * port should not be matched on, as these packets can come from any input.
     * Furthermore, these are applied only by the master instance.
     *
     * @param deviceId the switch dpid for the router
     */
    void populateIpPunts(DeviceId deviceId) {
        Ip4Address routerIpv4, pairRouterIpv4 = null;
        Ip6Address routerIpv6, routerLinkLocalIpv6, pairRouterIpv6 = null;
        try {
            routerIpv4 = config.getRouterIpv4(deviceId);
            routerIpv6 = config.getRouterIpv6(deviceId);
            routerLinkLocalIpv6 = Ip6Address.valueOf(
                    IPv6.getLinkLocalAddress(config.getDeviceMac(deviceId).toBytes()));

            if (config.isPairedEdge(deviceId)) {
                pairRouterIpv4 = config.getRouterIpv4(config.getPairDeviceId(deviceId));
                pairRouterIpv6 = config.getRouterIpv6(config.getPairDeviceId(deviceId));
            }
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateIpPunts.");
            return;
        }

        if (!srManager.mastershipService.isLocalMaster(deviceId)) {
            log.debug("Not installing port-IP punts - not the master for dev:{} ",
                      deviceId);
            return;
        }
        Set<IpAddress> allIps = new HashSet<>(config.getPortIPs(deviceId));
        allIps.add(routerIpv4);
        allIps.add(routerLinkLocalIpv6);
        if (routerIpv6 != null) {
            allIps.add(routerIpv6);
        }
        if (pairRouterIpv4 != null) {
            allIps.add(pairRouterIpv4);
        }
        if (pairRouterIpv6 != null) {
            allIps.add(pairRouterIpv6);
        }
        for (IpAddress ipaddr : allIps) {
            populateSingleIpPunts(deviceId, ipaddr);
        }
    }

    /**
     * Creates a forwarding objective to punt all IP packets, destined to the
     * specified IP address, which should be router's port IP address.
     *
     * @param deviceId the switch dpid for the router
     * @param ipAddress the IP address of the router's port
     */
    void populateSingleIpPunts(DeviceId deviceId, IpAddress ipAddress) {
        TrafficSelector.Builder sbuilder = buildIpSelectorFromIpAddress(ipAddress);
        Optional<DeviceId> optDeviceId = Optional.of(deviceId);

        srManager.packetService.requestPackets(sbuilder.build(),
                                               PacketPriority.CONTROL, srManager.appId, optDeviceId);
    }

    /**
     * Removes a forwarding objective to punt all IP packets, destined to the
     * specified IP address, which should be router's port IP address.
     *
     * @param deviceId the switch dpid for the router
     * @param ipAddress the IP address of the router's port
     */
    void revokeSingleIpPunts(DeviceId deviceId, IpAddress ipAddress) {
        TrafficSelector.Builder sbuilder = buildIpSelectorFromIpAddress(ipAddress);
        Optional<DeviceId> optDeviceId = Optional.of(deviceId);

        try {
            if (!ipAddress.equals(config.getRouterIpv4(deviceId)) &&
                    !ipAddress.equals(config.getRouterIpv6(deviceId))) {
                srManager.packetService.cancelPackets(sbuilder.build(),
                                                      PacketPriority.CONTROL, srManager.appId, optDeviceId);
            }
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting revokeSingleIpPunts");
        }
    }

    /**
     * Method to build IPv4 or IPv6 selector.
     *
     * @param addressToMatch the address to match
     */
    private TrafficSelector.Builder buildIpSelectorFromIpAddress(IpAddress addressToMatch) {
        return buildIpSelectorFromIpPrefix(addressToMatch.toIpPrefix());
    }

    /**
     * Method to build IPv4 or IPv6 selector.
     *
     * @param prefixToMatch the prefix to match
     */
    private TrafficSelector.Builder buildIpSelectorFromIpPrefix(IpPrefix prefixToMatch) {
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        // If the prefix is IPv4
        if (prefixToMatch.isIp4()) {
            selectorBuilder.matchEthType(Ethernet.TYPE_IPV4);
            selectorBuilder.matchIPDst(prefixToMatch.getIp4Prefix());
            return selectorBuilder;
        }
        // If the prefix is IPv6
        selectorBuilder.matchEthType(Ethernet.TYPE_IPV6);
        selectorBuilder.matchIPv6Dst(prefixToMatch.getIp6Prefix());
        return selectorBuilder;
    }

    /**
     * Creates forwarding objectives to punt ARP and NDP packets, to the controller.
     * Furthermore, these are applied only by the master instance. Deferred actions
     * are not cleared such that packets can be flooded in the cross connect use case
     *
     * @param deviceId the switch dpid for the router
     */
    void populateArpNdpPunts(DeviceId deviceId) {
        // We are not the master just skip.
        if (!srManager.mastershipService.isLocalMaster(deviceId)) {
            log.debug("Not installing ARP/NDP punts - not the master for dev:{} ",
                      deviceId);
            return;
        }

        ForwardingObjective fwdObj;
        // We punt all ARP packets towards the controller.
        fwdObj = arpFwdObjective(null, true, ARP_NDP_PRIORITY)
                .add(new ObjectiveContext() {
                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        log.warn("Failed to install forwarding objective to punt ARP to {}: {}",
                                 deviceId, error);
                    }
                });
        srManager.flowObjectiveService.forward(deviceId, fwdObj);

        // We punt all NDP packets towards the controller.
        ndpFwdObjective(null, true, ARP_NDP_PRIORITY).forEach(builder -> {
             ForwardingObjective obj = builder.add(new ObjectiveContext() {
                @Override
                public void onError(Objective objective, ObjectiveError error) {
                    log.warn("Failed to install forwarding objective to punt NDP to {}: {}",
                            deviceId, error);
                }
            });
            srManager.flowObjectiveService.forward(deviceId, obj);
        });

        srManager.getPairLocalPort(deviceId).ifPresent(port -> {
            ForwardingObjective pairFwdObj;
            // Do not punt ARP packets from pair port
            pairFwdObj = arpFwdObjective(port, false, PacketPriority.CONTROL.priorityValue() + 1)
                    .add(new ObjectiveContext() {
                        @Override
                        public void onError(Objective objective, ObjectiveError error) {
                            log.warn("Failed to install forwarding objective to ignore ARP to {}: {}",
                                    deviceId, error);
                        }
                    });
            srManager.flowObjectiveService.forward(deviceId, pairFwdObj);

            // Do not punt NDP packets from pair port
            ndpFwdObjective(port, false, PacketPriority.CONTROL.priorityValue() + 1).forEach(builder -> {
                ForwardingObjective obj = builder.add(new ObjectiveContext() {
                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        log.warn("Failed to install forwarding objective to ignore ARP to {}: {}",
                                deviceId, error);
                    }
                });
                srManager.flowObjectiveService.forward(deviceId, obj);
            });

            // Do not forward DAD packets from pair port
            pairFwdObj = dad6FwdObjective(port, PacketPriority.CONTROL.priorityValue() + 2)
                    .add(new ObjectiveContext() {
                        @Override
                        public void onError(Objective objective, ObjectiveError error) {
                            log.warn("Failed to install forwarding objective to drop DAD to {}: {}",
                                    deviceId, error);
                        }
                    });
            srManager.flowObjectiveService.forward(deviceId, pairFwdObj);
        });
    }

    private ForwardingObjective.Builder fwdObjBuilder(TrafficSelector selector,
                                                      TrafficTreatment treatment, int priority) {
        return DefaultForwardingObjective.builder()
                .withPriority(priority)
                .withSelector(selector)
                .fromApp(srManager.appId)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withTreatment(treatment)
                .makePermanent();
    }

    private ForwardingObjective.Builder arpFwdObjective(PortNumber port, boolean punt, int priority) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(TYPE_ARP);
        if (port != null) {
            sBuilder.matchInPort(port);
        }

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        if (punt) {
            tBuilder.punt();
        }
        return fwdObjBuilder(sBuilder.build(), tBuilder.build(), priority);
    }

    private Set<ForwardingObjective.Builder> ndpFwdObjective(PortNumber port, boolean punt, int priority) {
        Set<ForwardingObjective.Builder> result = Sets.newHashSet();

        Lists.newArrayList(NEIGHBOR_SOLICITATION, NEIGHBOR_ADVERTISEMENT, ROUTER_SOLICITATION, ROUTER_ADVERTISEMENT)
                .forEach(type -> {
                    TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
                    sBuilder.matchEthType(TYPE_IPV6)
                            .matchIPProtocol(PROTOCOL_ICMP6)
                            .matchIcmpv6Type(type);
                    if (port != null) {
                        sBuilder.matchInPort(port);
                    }

                    TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
                    if (punt) {
                        tBuilder.punt();
                    }

                    result.add(fwdObjBuilder(sBuilder.build(), tBuilder.build(), priority));
                });

        return result;
    }

    private ForwardingObjective.Builder dad6FwdObjective(PortNumber port, int priority) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(TYPE_IPV6)
                .matchIPv6Src(Ip6Address.ZERO.toIpPrefix());
                // TODO CORD-1672 Fix this when OFDPA can distinguish ::/0 and ::/128 correctly
                // .matchIPProtocol(PROTOCOL_ICMP6)
                // .matchIcmpv6Type(NEIGHBOR_SOLICITATION);
        if (port != null) {
            sBuilder.matchInPort(port);
        }

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.wipeDeferred();
        return fwdObjBuilder(sBuilder.build(), tBuilder.build(), priority);
    }

    /**
     * Populates a forwarding objective to send packets that miss other high
     * priority Bridging Table entries to a group that contains all ports of
     * its subnet.
     *
     * @param address the address to block
     * @param deviceId switch ID to set the rules
     */
    void populateDefaultRouteBlackhole(DeviceId deviceId, IpPrefix address) {
        updateDefaultRouteBlackhole(deviceId, address, true);
    }

    /**
     * Populates a forwarding objective to send packets that miss other high
     * priority Bridging Table entries to a group that contains all ports of
     * its subnet.
     *
     * @param address the address to block
     * @param deviceId switch ID to set the rules
     */
    void removeDefaultRouteBlackhole(DeviceId deviceId, IpPrefix address) {
        updateDefaultRouteBlackhole(deviceId, address, false);
    }

    private void updateDefaultRouteBlackhole(DeviceId deviceId, IpPrefix address, boolean install) {
        try {
            if (srManager.deviceConfiguration.isEdgeDevice(deviceId)) {

                TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
                if (address.isIp4()) {
                    sbuilder.matchIPDst(address);
                    sbuilder.matchEthType(EthType.EtherType.IPV4.ethType().toShort());
                } else {
                    sbuilder.matchIPv6Dst(address);
                    sbuilder.matchEthType(EthType.EtherType.IPV6.ethType().toShort());
                }

                TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

                tBuilder.transition(60);
                tBuilder.wipeDeferred();

                ForwardingObjective.Builder fob = DefaultForwardingObjective.builder();
                fob.withFlag(Flag.SPECIFIC)
                        .withSelector(sbuilder.build())
                        .withTreatment(tBuilder.build())
                        .withPriority(getPriorityFromPrefix(address))
                        .fromApp(srManager.appId)
                        .makePermanent();

                log.debug("{} blackhole forwarding objectives for dev: {}",
                        install ? "Installing" : "Removing", deviceId);
                ObjectiveContext context = new DefaultObjectiveContext(
                        (objective) -> log.debug("Forward for {} {}", deviceId,
                                install ? "installed" : "removed"),
                        (objective, error) -> log.warn("Failed to {} forward for {}: {}",
                                install ? "install" : "remove", deviceId, error));
                if (install) {
                    srManager.flowObjectiveService.forward(deviceId, fob.add(context));
                } else {
                    srManager.flowObjectiveService.forward(deviceId, fob.remove(context));
                }
            }
        } catch (DeviceConfigNotFoundException e) {
            log.info("Not populating blackhole for un-configured device {}", deviceId);
        }

    }

    /**
     * Populates a forwarding objective to send packets that miss other high
     * priority Bridging Table entries to a group that contains all ports of
     * its subnet.
     *
     * @param deviceId switch ID to set the rules
     */
    void populateSubnetBroadcastRule(DeviceId deviceId) {
        srManager.getVlanPortMap(deviceId).asMap().forEach((vlanId, ports) -> {
            updateSubnetBroadcastRule(deviceId, vlanId, true);
        });
    }

    /**
     * Creates or removes a forwarding objective to broadcast packets to its subnet.
     * @param deviceId switch ID to set the rule
     * @param vlanId vlan ID to specify the subnet
     * @param install true to install the rule, false to revoke the rule
     */
    void updateSubnetBroadcastRule(DeviceId deviceId, VlanId vlanId, boolean install) {
        int nextId = srManager.getVlanNextObjectiveId(deviceId, vlanId);

        if (nextId < 0) {
            log.error("Cannot install vlan {} broadcast rule in dev:{} due"
                              + " to vlanId:{} or nextId:{}", vlanId, deviceId, vlanId, nextId);
            return;
        }

        // Driver should treat objective with MacAddress.NONE as the
        // subnet broadcast rule
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchVlanId(vlanId);
        sbuilder.matchEthDst(MacAddress.NONE);

        ForwardingObjective.Builder fob = DefaultForwardingObjective.builder();
        fob.withFlag(Flag.SPECIFIC)
                .withSelector(sbuilder.build())
                .nextStep(nextId)
                .withPriority(SegmentRoutingService.FLOOD_PRIORITY)
                .fromApp(srManager.appId)
                .makePermanent();
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Vlan broadcast rule for {} populated", vlanId),
                (objective, error) ->
                        log.warn("Failed to populate vlan broadcast rule for {}: {}", vlanId, error));

        if (install) {
            srManager.flowObjectiveService.forward(deviceId, fob.add(context));
        } else {
            srManager.flowObjectiveService.forward(deviceId, fob.remove(context));
        }
    }

    private int getPriorityFromPrefix(IpPrefix prefix) {
        return (prefix.isIp4()) ?
                2000 * prefix.prefixLength() + SegmentRoutingService.MIN_IP_PRIORITY :
                500 * prefix.prefixLength() + SegmentRoutingService.MIN_IP_PRIORITY;
    }

    /**
     * Update Forwarding objective for each host and IP address connected to given port.
     * And create corresponding Simple Next objective if it does not exist.
     * Applied only when populating Forwarding objective
     * @param deviceId switch ID to set the rule
     * @param portNumber port number
     * @param prefix IP prefix of the route
     * @param hostMac MAC address of the next hop
     * @param vlanId Vlan ID of the port
     * @param popVlan true to pop vlan tag in TrafficTreatment
     * @param install true to populate the forwarding objective, false to revoke
     */
    void updateFwdObj(DeviceId deviceId, PortNumber portNumber, IpPrefix prefix, MacAddress hostMac,
                      VlanId vlanId, boolean popVlan, boolean install) {
        ForwardingObjective.Builder fob;
        TrafficSelector.Builder sbuilder = buildIpSelectorFromIpPrefix(prefix);
        MacAddress deviceMac;
        try {
            deviceMac = config.getDeviceMac(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting updateFwdObj.");
            return;
        }

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.deferred()
                .setEthDst(hostMac)
                .setEthSrc(deviceMac)
                .setOutput(portNumber);

        TrafficSelector.Builder mbuilder = DefaultTrafficSelector.builder();

        if (!popVlan) {
            tbuilder.setVlanId(vlanId);
        } else {
            mbuilder.matchVlanId(vlanId);
        }

        // if the objective is to revoke an existing rule, and for some reason
        // the next-objective does not exist, then a new one should not be created
        int portNextObjId = srManager.getPortNextObjectiveId(deviceId, portNumber,
                                                             tbuilder.build(), mbuilder.build(), install);
        if (portNextObjId == -1) {
            // Warning log will come from getPortNextObjective method
            return;
        }

        fob = DefaultForwardingObjective.builder().withSelector(sbuilder.build())
                .nextStep(portNextObjId).fromApp(srManager.appId).makePermanent()
                .withPriority(getPriorityFromPrefix(prefix)).withFlag(ForwardingObjective.Flag.SPECIFIC);

        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("IP rule for route {} {}", prefix, install ? "installed" : "revoked"),
                (objective, error) ->
                        log.warn("Failed to {} IP rule for route {}: {}",
                                 install ? "install" : "revoke", prefix, error));
        srManager.flowObjectiveService.forward(deviceId, install ? fob.add(context) : fob.remove(context));

        if (!install) {
            DefaultGroupHandler grpHandler = srManager.getGroupHandler(deviceId);
            if (grpHandler == null) {
                log.warn("updateFwdObj: groupHandler for device {} not found", deviceId);
            } else {
                // Remove L3UG for the given port and host
                grpHandler.removeGroupFromPort(portNumber, tbuilder.build(), mbuilder.build());
            }
        }
    }

    /**
     * Checks if there is other enabled port within the given VLAN on the given device.
     *
     * @param deviceId device ID
     * @param vlanId VLAN ID
     * @return true if there is no more port enabled within the given VLAN on the given device
     */
    boolean noMoreEnabledPort(DeviceId deviceId, VlanId vlanId) {
        Set<ConnectPoint> enabledPorts = srManager.deviceService.getPorts(deviceId).stream()
                .filter(Port::isEnabled)
                .map(port -> new ConnectPoint(port.element().id(), port.number()))
                .collect(Collectors.toSet());

        return enabledPorts.stream().noneMatch(cp ->
            // Given vlanId is included in the vlan-tagged configuration
            srManager.interfaceService.getTaggedVlanId(cp).contains(vlanId) ||
            // Given vlanId is INTERNAL_VLAN and the interface is not configured
            (srManager.interfaceService.getTaggedVlanId(cp).isEmpty() && srManager.getInternalVlanId(cp) == null &&
                    vlanId.equals(INTERNAL_VLAN)) ||
            // interface is configured and either vlan-untagged or vlan-native matches given vlanId
            (srManager.getInternalVlanId(cp) != null && srManager.getInternalVlanId(cp).equals(vlanId))
        );
    }

    /**
     * Returns a forwarding objective builder for egress forwarding rules.
     * <p>
     * The forwarding objective installs flow rules to egress pipeline to push
     * two vlan headers with given inner, outer vlan ids and outer tpid.
     *
     * @param portNumber port where the next hop attaches to
     * @param dummyVlanId vlan ID of the packet to match
     * @param innerVlan inner vlan ID of the next hop
     * @param outerVlan outer vlan ID of the next hop
     * @param outerTpid outer TPID of the next hop
     * @return forwarding objective builder
     */
    private ForwardingObjective.Builder egressFwdObjBuilder(PortNumber portNumber, VlanId dummyVlanId,
                                                            VlanId innerVlan, VlanId outerVlan, EthType outerTpid) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchVlanId(dummyVlanId);
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.setOutput(portNumber).setVlanId(innerVlan);

        if (outerTpid.equals(EthType.EtherType.QINQ.ethType())) {
            tbuilder.pushVlan(outerTpid);
        } else {
            tbuilder.pushVlan();
        }

        tbuilder.setVlanId(outerVlan);
        return DefaultForwardingObjective.builder()
                .withSelector(sbuilder.build())
                .withTreatment(tbuilder.build())
                .fromApp(srManager.appId)
                .makePermanent()
                .withPriority(DEFAULT_PRIORITY)
                .withFlag(ForwardingObjective.Flag.EGRESS);
    }

    /**
     * Populates IP rules for a route that has double-tagged next hop.
     *
     * @param deviceId device ID of the device that next hop attaches to
     * @param prefix IP prefix of the route
     * @param hostMac MAC address of the next hop
     * @param dummyVlan Dummy Vlan ID allocated for this route
     * @param innerVlan inner Vlan ID of the next hop
     * @param outerVlan outer Vlan ID of the next hop
     * @param outerTpid outer TPID of the next hop
     * @param outPort port where the next hop attaches to
     */
    void populateDoubleTaggedRoute(DeviceId deviceId, IpPrefix prefix, MacAddress hostMac, VlanId dummyVlan,
                                   VlanId innerVlan, VlanId outerVlan, EthType outerTpid, PortNumber outPort) {
        ForwardingObjective.Builder fwdBuilder;
        log.debug("Populate direct routing entry for double-tagged host route {} at {}:{}",
                  prefix, deviceId, outPort);

        ForwardingObjective.Builder egressFwdBuilder = egressFwdObjBuilder(
                outPort, dummyVlan, innerVlan, outerVlan, outerTpid);
        DefaultObjectiveContext egressFwdContext = new DefaultObjectiveContext(
                objective -> log.debug("Egress rule for IP {} is populated", prefix.address()),
                (objective, error) -> {
                    log.warn("Failed to populate egress rule for IP {}: {}", prefix.address(), error);
                    srManager.dummyVlanIdStore().remove(new DummyVlanIdStoreKey(
                            new ConnectPoint(deviceId, outPort), prefix.address()
                    ));
                });
        try {
            fwdBuilder = routingFwdObjBuilder(deviceId, prefix, hostMac, dummyVlan, outPort, false);
        } catch (DeviceConfigNotFoundException e) {
            log.error(e.getMessage() + " Aborting populateDoubleTaggedRoute");
            return;
        }
        if (fwdBuilder == null) {
            log.error("Aborting double-tagged host routing table entry due to error for dev:{} route:{}",
                     deviceId, prefix);
            return;
        }

        // Egress forwarding objective should be installed after the nextObjective for the output port is installed.
        // Installation of routingFwdObj will ensure the installation of the nextObjective.
        int nextId = fwdBuilder.add().nextId();
        DefaultObjectiveContext context = new DefaultObjectiveContext(objective -> {
            log.debug("Direct routing rule for double-tagged host route {} populated. nextId={}", prefix, nextId);
            srManager.flowObjectiveService.forward(deviceId, egressFwdBuilder.add(egressFwdContext));
        }, (objective, error) ->
            log.warn("Failed to populate direct routing rule for double-tagged host route {}: {}", prefix, error)
        );
        srManager.flowObjectiveService.forward(deviceId, fwdBuilder.add(context));
        rulePopulationCounter.incrementAndGet();
    }

    /**
     * Revokes IP rules for a route that has double-tagged next hop.
     *
     * @param deviceId device ID of the device that next hop attaches to
     * @param prefix IP prefix of the route
     * @param hostMac MAC address of the next hop
     * @param hostVlan Vlan ID of the next hop
     * @param innerVlan inner Vlan ID of the next hop
     * @param outerVlan outer Vlan ID of the next hop
     * @param outerTpid outer TPID of the next hop
     * @param outPort port where the next hop attaches to
     */
    void revokeDoubleTaggedRoute(DeviceId deviceId, IpPrefix prefix, MacAddress hostMac,
                                 VlanId hostVlan, VlanId innerVlan, VlanId outerVlan,
                                 EthType outerTpid, PortNumber outPort) {
        revokeRoute(deviceId, prefix, hostMac, hostVlan, outPort);

        DummyVlanIdStoreKey key = new DummyVlanIdStoreKey(
                new ConnectPoint(deviceId, outPort), prefix.address());
        VlanId dummyVlanId = srManager.dummyVlanIdStore().get(key);
        if (dummyVlanId == null) {
            log.warn("Failed to retrieve dummy VLAN ID for {}/{} and {}",
                     deviceId, outPort, prefix.address());
            return;
        }
        ForwardingObjective.Builder fob = egressFwdObjBuilder(
                outPort, dummyVlanId, innerVlan, outerVlan, outerTpid);
        DefaultObjectiveContext context = new DefaultObjectiveContext(objective -> {
            log.debug("Egress rule for IP {} revoked", prefix.address());
            srManager.dummyVlanIdStore().remove(key);
        }, (objective, error) -> {
            log.warn("Failed to revoke egress rule for IP {}: {}", prefix.address(), error);
        });
        srManager.flowObjectiveService.forward(deviceId, fob.remove(context));
    }

    /**
     * Checks whether the specified port has IP configuration or not.
     *
     * @param cp ConnectPoint to check the existance of IP configuration
     * @return true if the port has IP configuration; false otherwise.
     */
    private boolean hasIPConfiguration(ConnectPoint cp) {
        Set<Interface> interfaces = srManager.interfaceService.getInterfacesByPort(cp);
        return interfaces.stream().anyMatch(intf -> intf.ipAddressesList().size() > 0);
    }
}
