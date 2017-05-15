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

import com.google.common.collect.Lists;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
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
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.segmentrouting.DefaultRoutingHandler.PortFilterInfo;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.onosproject.segmentrouting.grouphandler.NeighborSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.packet.Ethernet.TYPE_ARP;
import static org.onlab.packet.Ethernet.TYPE_IPV6;
import static org.onlab.packet.ICMP6.NEIGHBOR_SOLICITATION;
import static org.onlab.packet.IPv6.PROTOCOL_ICMP6;
import static org.onosproject.segmentrouting.SegmentRoutingManager.INTERNAL_VLAN;

/**
 * Populator of segment routing flow rules.
 */
public class RoutingRulePopulator {
    private static final Logger log = LoggerFactory
            .getLogger(RoutingRulePopulator.class);

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
     *
     * @return number of rules
     */
    public long getCounter() {
        return rulePopulationCounter.get();
    }

    /**
     * Populates IP rules for a route that has direct connection to the
     * switch.
     *
     * @param deviceId device ID of the device that next hop attaches to
     * @param prefix IP prefix of the route
     * @param hostMac MAC address of the next hop
     * @param hostVlanId Vlan ID of the nexthop
     * @param outPort port where the next hop attaches to
     */
    public void populateRoute(DeviceId deviceId, IpPrefix prefix,
                              MacAddress hostMac, VlanId hostVlanId, PortNumber outPort) {
        log.debug("Populate routing entry for route {} at {}:{}",
                prefix, deviceId, outPort);
        ForwardingObjective.Builder fwdBuilder;
        try {
            fwdBuilder = routingFwdObjBuilder(deviceId, prefix, hostMac,
                                              hostVlanId, outPort, false);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateIpRuleForHost.");
            return;
        }
        if (fwdBuilder == null) {
            log.warn("Aborting host routing table entry due "
                    + "to error for dev:{} route:{}", deviceId, prefix);
            return;
        }
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Routing rule for route {} populated", prefix),
                (objective, error) ->
                        log.warn("Failed to populate routing rule for route {}: {}",
                                 prefix, error));
        srManager.flowObjectiveService.forward(deviceId, fwdBuilder.add(context));
        rulePopulationCounter.incrementAndGet();
    }

    /**
     * Removes IP rules for a route when the next hop is gone.
     *
     * @param deviceId device ID of the device that next hop attaches to
     * @param prefix IP prefix of the route
     * @param hostMac MAC address of the next hop
     * @param hostVlanId Vlan ID of the nexthop
     * @param outPort port that next hop attaches to
     */
    public void revokeRoute(DeviceId deviceId, IpPrefix prefix,
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
        VlanId untaggedVlan = srManager.getUntaggedVlanId(connectPoint);
        Set<VlanId> taggedVlans = srManager.getTaggedVlanId(connectPoint);
        VlanId nativeVlan = srManager.getNativeVlanId(connectPoint);

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
                // TODO: This check is turned off for now since vRouter still assumes that
                // hosts are internally tagged with INTERNAL_VLAN.
                // We should turn this back on when we move forward to the bridging CPR approach.
                //
                //log.warn("Untagged nexthop {}/{} is not allowed on {} without untagged or native vlan",
                //        hostMac, hostVlanId, connectPoint);
                //return null;
                mbuilder.matchVlanId(INTERNAL_VLAN);
            }
        } else {
            log.warn("Tagged nexthop {}/{} is not allowed on {} without VLAN listed"
                    + " in tagged vlan", hostMac, hostVlanId, connectPoint);
            return null;
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
     * Populates IP flow rules for the subnets of the destination router.
     *
     * @param deviceId switch ID to set the rules
     * @param subnets subnet being added
     * @param destSw destination switch ID
     * @param nextHops next hop switch ID list
     * @return true if all rules are set successfully, false otherwise
     */
    public boolean populateIpRuleForSubnet(DeviceId deviceId, Set<IpPrefix> subnets,
            DeviceId destSw, Set<DeviceId> nextHops) {
        for (IpPrefix subnet : subnets) {
            if (!populateIpRuleForRouter(deviceId, subnet, destSw, nextHops)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Revokes IP flow rules for the subnets.
     *
     * @param subnets subnet being removed
     * @return true if all rules are removed successfully, false otherwise
     */
    public boolean revokeIpRuleForSubnet(Set<IpPrefix> subnets) {
        for (IpPrefix subnet : subnets) {
            if (!revokeIpRuleForRouter(subnet)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Populates IP flow rules for an IP prefix in the target device. The prefix
     * is reachable via destination device.
     *
     * @param deviceId target device ID to set the rules
     * @param ipPrefix the destination IP prefix
     * @param destSw device ID of the destination router
     * @param nextHops next hop switch ID list
     * @return true if all rules are set successfully, false otherwise
     */
    public boolean populateIpRuleForRouter(DeviceId deviceId,
                                           IpPrefix ipPrefix, DeviceId destSw,
                                           Set<DeviceId> nextHops) {
        int segmentId;
        try {
            if (ipPrefix.isIp4()) {
                segmentId = config.getIPv4SegmentId(destSw);
            } else {
                segmentId = config.getIPv6SegmentId(destSw);
            }
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateIpRuleForRouter.");
            return false;
        }

        TrafficSelector.Builder sbuilder = buildIpSelectorFromIpPrefix(ipPrefix);
        TrafficSelector selector = sbuilder.build();

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        NeighborSet ns;
        TrafficTreatment treatment;

        // If the next hop is the same as the final destination, then MPLS label
        // is not set.
        if (nextHops.size() == 1 && nextHops.toArray()[0].equals(destSw)) {
            tbuilder.immediate().decNwTtl();
            ns = new NeighborSet(nextHops, false, destSw);
            treatment = tbuilder.build();
        } else {
            ns = new NeighborSet(nextHops, false, segmentId, destSw);
            treatment = null;
        }

        // setup metadata to pass to nextObjective - indicate the vlan on egress
        // if needed by the switch pipeline. Since neighbor sets are always to
        // other neighboring routers, there is no subnet assigned on those ports.
        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder(selector);
        metabuilder.matchVlanId(SegmentRoutingManager.INTERNAL_VLAN);
        DefaultGroupHandler grpHandler = srManager.getGroupHandler(deviceId);
        if (grpHandler == null) {
            log.warn("populateIPRuleForRouter: groupHandler for device {} "
                    + "not found", deviceId);
            return false;
        }

        int nextId = grpHandler.getNextObjectiveId(ns, metabuilder.build(), true);
        if (nextId <= 0) {
            log.warn("No next objective in {} for ns: {}", deviceId, ns);
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
                + "in switch {} with nextId: {}", ipPrefix, deviceId, nextId);
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("IP rule for router {} populated in dev:{}",
                                         ipPrefix, deviceId),
                (objective, error) ->
                        log.warn("Failed to populate IP rule for router {}: {} in dev:{}",
                                 ipPrefix, error, deviceId));
        srManager.flowObjectiveService.forward(deviceId, fwdBuilder.add(context));
        rulePopulationCounter.incrementAndGet();

        return true;
    }

    /**
     * Revokes IP flow rules for the router IP address.
     *
     * @param ipPrefix the IP address of the destination router
     * @return true if all rules are removed successfully, false otherwise
     */
    public boolean revokeIpRuleForRouter(IpPrefix ipPrefix) {
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
                (objective) -> log.debug("IP rule for router {} revoked", ipPrefix),
                (objective, error) ->
                        log.warn("Failed to revoke IP rule for router {}: {}", ipPrefix, error));

        srManager.deviceService.getAvailableDevices().forEach(device -> {
            srManager.flowObjectiveService.forward(device.id(), fwdBuilder.remove(context));
        });

        return true;
    }

    /**
     * Deals with !MPLS Bos use case.
     *
     * @param targetSwId the target sw
     * @param destSwId the destination sw
     * @param nextHops the set of next hops
     * @param segmentId the segmentId to match
     * @param routerIp the router ip
     * @return a collection of fwdobjective
     */
    private Collection<ForwardingObjective> handleMpls(DeviceId targetSwId,
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
                    + "label {} in switch {} with pop", segmentId, targetSwId);
            // Not-bos pop case (php for the current label). If MPLS-ECMP
            // has been configured, the application we will request the
            // installation for an MPLS-ECMP group.
            ForwardingObjective.Builder fwdObjNoBosBuilder =
                    getMplsForwardingObjective(targetSwId,
                                               nextHops,
                                               true,
                                               isMplsBos,
                                               metabuilder.build(),
                                               routerIp,
                                               destSwId);
            // Error case, we cannot handle, exit.
            if (fwdObjNoBosBuilder == null) {
                return Collections.emptyList();
            }
            fwdObjBuilders.add(fwdObjNoBosBuilder);

        } else {
            // next hop is not destination, SR CONTINUE case (swap with self)
            log.debug("Installing MPLS forwarding objective for label {} in "
                    + "switch {} without pop", segmentId, targetSwId);
            // Not-bos pop case. If MPLS-ECMP has been configured, the
            // application we will request the installation for an MPLS-ECMP
            // group.
            ForwardingObjective.Builder fwdObjNoBosBuilder =
                    getMplsForwardingObjective(targetSwId,
                                               nextHops,
                                               false,
                                               isMplsBos,
                                               metabuilder.build(),
                                               routerIp,
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
     * Populates MPLS flow rules in the target device to point towards the
     * destination device.
     *
     * @param targetSwId target device ID of the switch to set the rules
     * @param destSwId destination switch device ID
     * @param nextHops next hops switch ID list
     * @param routerIp the router ip
     * @return true if all rules are set successfully, false otherwise
     */
    public boolean populateMplsRule(DeviceId targetSwId, DeviceId destSwId,
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
        Collection<ForwardingObjective> fwdObjsMpls = Collections.emptyList();
        // Generates the transit rules used by the standard "routing".
        fwdObjsMpls = handleMpls(targetSwId, destSwId, nextHops, segmentId, routerIp, true);
        if (fwdObjsMpls.isEmpty()) {
            return false;
        }
        fwdObjs.addAll(fwdObjsMpls);
        // Generates the transit rules used by the MPLS Pwaas. For now it is
        // the only case !BoS supported.
        /*fwdObjsMpls = handleMpls(targetSwId, destSwId, nextHops, segmentId, routerIp, false);
        if (fwdObjsMpls.isEmpty()) {
            return false;
        }
        fwdObjs.addAll(fwdObjsMpls);*/

        for (ForwardingObjective fwdObj : fwdObjs) {
            log.debug("Sending MPLS fwd obj {} for SID {}-> next {} in sw: {}",
                      fwdObj.id(), segmentId, fwdObj.nextId(), targetSwId);
            srManager.flowObjectiveService.forward(targetSwId, fwdObj);
            rulePopulationCounter.incrementAndGet();
        }

        return true;
    }

    private ForwardingObjective.Builder getMplsForwardingObjective(
                                             DeviceId deviceId,
                                             Set<DeviceId> nextHops,
                                             boolean phpRequired,
                                             boolean isBos,
                                             TrafficSelector meta,
                                             IpAddress routerIp,
                                             DeviceId destSw) {

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                .builder().withFlag(ForwardingObjective.Flag.SPECIFIC);

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();

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
            } else {
                tbuilder.deferred().popMpls(EthType.EtherType.MPLS_UNICAST.ethType())
                    .decMplsTtl();
            }
        } else {
            // swap with self case - SR CONTINUE
            log.debug("getMplsForwardingObjective: php not required");
            tbuilder.deferred().decMplsTtl();
        }

        fwdBuilder.withTreatment(tbuilder.build());
        // if MPLS-ECMP == True we will build a standard NeighborSet.
        // Otherwise a RandomNeighborSet.
        NeighborSet ns = NeighborSet.neighborSet(false, nextHops, false, destSw);
        if (!isBos && this.srManager.getMplsEcmp()) {
            ns = NeighborSet.neighborSet(false, nextHops, true, destSw);
        } else if (!isBos && !this.srManager.getMplsEcmp()) {
            ns = NeighborSet.neighborSet(true, nextHops, true, destSw);
        }
        log.debug("Trying to get a nextObjId for mpls rule on device:{} to ns:{}",
                  deviceId, ns);
        // If BoS == True, all forwarding is via L3 ECMP group.
        // If Bos == False, the forwarding can be via MPLS-ECMP group or through
        // MPLS-Interface group. This depends on the configuration of the option
        // MPLS-ECMP.
        // The metadata informs the driver that the next-Objective will be used
        // by MPLS flows and if Bos == False the driver will use MPLS groups.
        DefaultGroupHandler grpHandler = srManager.getGroupHandler(deviceId);
        if (grpHandler == null) {
            log.warn("populateIPRuleForRouter: groupHandler for device {} "
                    + "not found", deviceId);
            return null;
        }
        int nextId = grpHandler.getNextObjectiveId(ns, meta, isBos);
        if (nextId <= 0) {
            log.warn("No next objective in {} for ns: {}", deviceId, ns);
            return null;
        } else {
            log.debug("nextObjId found:{} for mpls rule on device:{} to ns:{}",
                      nextId, deviceId, ns);
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
    public PortFilterInfo populateVlanMacFilters(DeviceId deviceId) {
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
        log.info("Filtering on dev:{}, disabledPorts:{}, errorPorts:{}, filteredPorts:{}",
                  deviceId, disabledPorts, errorPorts, filteredPorts);
        return srManager.defaultRoutingHandler.new PortFilterInfo(disabledPorts,
                                                       errorPorts, filteredPorts);
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
    public boolean processSinglePortFilters(DeviceId deviceId, PortNumber portnum, boolean install) {
        ConnectPoint connectPoint = new ConnectPoint(deviceId, portnum);
        VlanId untaggedVlan = srManager.getUntaggedVlanId(connectPoint);
        Set<VlanId> taggedVlans = srManager.getTaggedVlanId(connectPoint);
        VlanId nativeVlan = srManager.getNativeVlanId(connectPoint);

        if (taggedVlans.size() != 0) {
            // Filter for tagged vlans
            if (!srManager.getTaggedVlanId(connectPoint).stream().allMatch(taggedVlanId ->
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
        } else {
            // Unconfigure port, use INTERNAL_VLAN
            if (!processSinglePortFiltersInternal(deviceId, portnum, true, INTERNAL_VLAN, install)) {
                return false;
            }
        }
        return true;
    }

    private boolean processSinglePortFiltersInternal(DeviceId deviceId, PortNumber portnum,
                                                      boolean pushVlan, VlanId vlanId, boolean install) {
        FilteringObjective.Builder fob = buildFilteringObjective(deviceId, portnum, pushVlan, vlanId);
        if (fob == null) {
            // error encountered during build
            return false;
        }
        log.debug("{} filtering objectives for dev/port:{}/{}",
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
                                                               boolean pushVlan, VlanId vlanId) {
        MacAddress deviceMac;
        try {
            deviceMac = config.getDeviceMac(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Processing SinglePortFilters aborted");
            return null;
        }
        FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
        fob.withKey(Criteria.matchInPort(portnum))
            .addCondition(Criteria.matchEthDst(deviceMac))
            .withPriority(SegmentRoutingService.DEFAULT_PRIORITY);

        if (pushVlan) {
            fob.addCondition(Criteria.matchVlanId(VlanId.NONE));
            TrafficTreatment tt = DefaultTrafficTreatment.builder()
                    .pushVlan().setVlanId(vlanId).build();
            fob.withMeta(tt);
        } else {
            fob.addCondition(Criteria.matchVlanId(vlanId));
        }

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
    public void populateIpPunts(DeviceId deviceId) {
        Ip4Address routerIpv4;
        Ip6Address routerIpv6;
        try {
            routerIpv4 = config.getRouterIpv4(deviceId);
            routerIpv6 = config.getRouterIpv6(deviceId);
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
        if (routerIpv6 != null) {
            allIps.add(routerIpv6);
        }
        for (IpAddress ipaddr : allIps) {
            TrafficSelector.Builder sbuilder = buildIpSelectorFromIpAddress(ipaddr);
            Optional<DeviceId> optDeviceId = Optional.of(deviceId);

            srManager.packetService.requestPackets(sbuilder.build(),
                    PacketPriority.CONTROL, srManager.appId, optDeviceId);
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
    public void populateArpNdpPunts(DeviceId deviceId) {
        // We are not the master just skip.
        if (!srManager.mastershipService.isLocalMaster(deviceId)) {
            log.debug("Not installing ARP/NDP punts - not the master for dev:{} ",
                      deviceId);
            return;
        }

        // We punt all ARP packets towards the controller.
        ForwardingObjective puntFwd = puntArpFwdObjective()
                .add(new ObjectiveContext() {
                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        log.warn("Failed to install packet request for ARP to {}: {}",
                                 deviceId, error);
                    }
                });
        srManager.flowObjectiveService.forward(deviceId, puntFwd);

        // We punt all NDP packets towards the controller.
        puntFwd = puntNdpFwdObjective()
                .add(new ObjectiveContext() {
                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        log.warn("Failed to install packet request for NDP to {}: {}",
                                 deviceId, error);
                    }
                });
        srManager.flowObjectiveService.forward(deviceId, puntFwd);
    }

    private ForwardingObjective.Builder fwdObjBuilder(TrafficSelector selector) {

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.punt();

        return DefaultForwardingObjective.builder()
                .withPriority(PacketPriority.CONTROL.priorityValue())
                .withSelector(selector)
                .fromApp(srManager.appId)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withTreatment(tBuilder.build())
                .makePermanent();
    }

    private ForwardingObjective.Builder puntArpFwdObjective() {

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(TYPE_ARP);

        return fwdObjBuilder(sBuilder.build());
    }

    private ForwardingObjective.Builder puntNdpFwdObjective() {

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(TYPE_IPV6)
                .matchIPProtocol(PROTOCOL_ICMP6)
                .matchIcmpv6Type(NEIGHBOR_SOLICITATION)
                .build();

        return fwdObjBuilder(sBuilder.build());
    }

    /**
     * Populates a forwarding objective to send packets that miss other high
     * priority Bridging Table entries to a group that contains all ports of
     * its subnet.
     *
     * @param deviceId switch ID to set the rules
     */
    public void populateSubnetBroadcastRule(DeviceId deviceId) {
        srManager.getVlanPortMap(deviceId).asMap().forEach((vlanId, ports) -> {
            int nextId = srManager.getVlanNextObjectiveId(deviceId, vlanId);

            if (nextId < 0) {
                log.error("Cannot install vlan {} broadcast rule in dev:{} due"
                        + "to vlanId:{} or nextId:{}", vlanId, deviceId, vlanId, nextId);
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
            srManager.flowObjectiveService.forward(deviceId, fob.add(context));
        });
    }

    private int getPriorityFromPrefix(IpPrefix prefix) {
        return (prefix.isIp4()) ?
                2000 * prefix.prefixLength() + SegmentRoutingService.MIN_IP_PRIORITY :
                500 * prefix.prefixLength() + SegmentRoutingService.MIN_IP_PRIORITY;
    }
}
