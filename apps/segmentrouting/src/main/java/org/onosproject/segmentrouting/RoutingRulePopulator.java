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

import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.segmentrouting.DefaultRoutingHandler.PortFilterInfo;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.config.SegmentRoutingAppConfig;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

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
     * @param outPort port where the next hop attaches to
     */
    public void populateRoute(DeviceId deviceId, IpPrefix prefix,
                                      MacAddress hostMac, PortNumber outPort) {
        log.debug("Populate IP table entry for route {} at {}:{}",
                prefix, deviceId, outPort);
        ForwardingObjective.Builder fwdBuilder;
        try {
            fwdBuilder = getForwardingObjectiveBuilder(
                    deviceId, prefix, hostMac, outPort);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateIpRuleForHost.");
            return;
        }
        if (fwdBuilder == null) {
            log.warn("Aborting host routing table entries due "
                    + "to error for dev:{} route:{}", deviceId, prefix);
            return;
        }
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("IP rule for route {} populated", prefix),
                (objective, error) ->
                        log.warn("Failed to populate IP rule for route {}: {}", prefix, error));
        srManager.flowObjectiveService.forward(deviceId, fwdBuilder.add(context));
        rulePopulationCounter.incrementAndGet();
    }

    /**
     * Removes IP rules for a route when the next hop is gone.
     *
     * @param deviceId device ID of the device that next hop attaches to
     * @param prefix IP prefix of the route
     * @param hostMac MAC address of the next hop
     * @param outPort port that next hop attaches to
     */
    public void revokeRoute(DeviceId deviceId, IpPrefix prefix,
            MacAddress hostMac, PortNumber outPort) {
        log.debug("Revoke IP table entry for route {} at {}:{}",
                prefix, deviceId, outPort);
        ForwardingObjective.Builder fwdBuilder;
        try {
            fwdBuilder = getForwardingObjectiveBuilder(
                    deviceId, prefix, hostMac, outPort);
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
     * Returns a forwarding objective that points packets destined to a
     * given prefix to given port on given device with given destination MAC.
     *
     * @param deviceId device ID
     * @param prefix prefix that need to be routed
     * @param hostMac MAC address of the nexthop
     * @param outPort port where the nexthop attaches to
     * @return forwarding objective builder
     * @throws DeviceConfigNotFoundException if given device is not configured
     */
    private ForwardingObjective.Builder getForwardingObjectiveBuilder(
            DeviceId deviceId, IpPrefix prefix,
            MacAddress hostMac, PortNumber outPort)
            throws DeviceConfigNotFoundException {
        MacAddress deviceMac;
        deviceMac = config.getDeviceMac(deviceId);

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchEthType(Ethernet.TYPE_IPV4);
        sbuilder.matchIPDst(prefix);
        TrafficSelector selector = sbuilder.build();

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        tbuilder.deferred()
                .setEthDst(hostMac)
                .setEthSrc(deviceMac)
                .setOutput(outPort);
        TrafficTreatment treatment = tbuilder.build();

        // All forwarding is via Groups. Drivers can re-purpose to flow-actions if needed.
        // for switch pipelines that need it, provide outgoing vlan as metadata
        VlanId outvlan = null;
        Ip4Prefix subnet = srManager.deviceConfiguration.getPortSubnet(deviceId, outPort);
        if (subnet == null) {
            outvlan = VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET);
        } else {
            outvlan = srManager.getSubnetAssignedVlanId(deviceId, subnet);
        }
        TrafficSelector meta = DefaultTrafficSelector.builder()
                                    .matchVlanId(outvlan).build();
        int portNextObjId = srManager.getPortNextObjectiveId(deviceId, outPort,
                                                             treatment, meta);
        if (portNextObjId == -1) {
            // warning log will come from getPortNextObjective method
            return null;
        }
        return DefaultForwardingObjective.builder()
                .withSelector(selector)
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
    public boolean populateIpRuleForSubnet(DeviceId deviceId, Set<Ip4Prefix> subnets,
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
    public boolean revokeIpRuleForSubnet(Set<Ip4Prefix> subnets) {
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
     * @param ipPrefix the IP address of the destination router
     * @param destSw device ID of the destination router
     * @param nextHops next hop switch ID list
     * @return true if all rules are set successfully, false otherwise
     */
    public boolean populateIpRuleForRouter(DeviceId deviceId,
                                           IpPrefix ipPrefix, DeviceId destSw,
                                           Set<DeviceId> nextHops) {
        int segmentId;
        try {
            segmentId = config.getSegmentId(destSw);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateIpRuleForRouter.");
            return false;
        }

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchIPDst(ipPrefix);
        sbuilder.matchEthType(Ethernet.TYPE_IPV4);
        TrafficSelector selector = sbuilder.build();

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
        NeighborSet ns;
        TrafficTreatment treatment;

        // If the next hop is the same as the final destination, then MPLS label
        // is not set.
        if (nextHops.size() == 1 && nextHops.toArray()[0].equals(destSw)) {
            tbuilder.immediate().decNwTtl();
            ns = new NeighborSet(nextHops);
            treatment = tbuilder.build();
        } else {
            ns = new NeighborSet(nextHops, segmentId);
            treatment = null;
        }

        // setup metadata to pass to nextObjective - indicate the vlan on egress
        // if needed by the switch pipeline. Since neighbor sets are always to
        // other neighboring routers, there is no subnet assigned on those ports.
        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder(selector);
        metabuilder.matchVlanId(
            VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET));

        int nextId = srManager.getNextObjectiveId(deviceId, ns, metabuilder.build());
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
        log.debug("Installing IPv4 forwarding objective "
                        + "for router IP/subnet {} in switch {}",
                ipPrefix,
                deviceId);
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
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        sbuilder.matchIPDst(ipPrefix);
        sbuilder.matchEthType(Ethernet.TYPE_IPV4);
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
     * Populates MPLS flow rules in the target device to point towards the
     * destination device.
     *
     * @param targetSwId target device ID of the switch to set the rules
     * @param destSwId destination switch device ID
     * @param nextHops next hops switch ID list
     * @return true if all rules are set successfully, false otherwise
     */
    public boolean populateMplsRule(DeviceId targetSwId, DeviceId destSwId,
                                    Set<DeviceId> nextHops) {
        int segmentId;
        try {
            segmentId = config.getSegmentId(destSwId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateMplsRule.");
            return false;
        }

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        List<ForwardingObjective.Builder> fwdObjBuilders = new ArrayList<>();

        // TODO Handle the case of Bos == false
        sbuilder.matchEthType(Ethernet.MPLS_UNICAST);
        sbuilder.matchMplsLabel(MplsLabel.mplsLabel(segmentId));
        sbuilder.matchMplsBos(true);
        TrafficSelector selector = sbuilder.build();

        // setup metadata to pass to nextObjective - indicate the vlan on egress
        // if needed by the switch pipeline. Since mpls next-hops are always to
        // other neighboring routers, there is no subnet assigned on those ports.
        TrafficSelector.Builder metabuilder = DefaultTrafficSelector.builder(selector);
        metabuilder.matchVlanId(
            VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET));

        // If the next hop is the destination router for the segment, do pop
        if (nextHops.size() == 1 && destSwId.equals(nextHops.toArray()[0])) {
            log.debug("populateMplsRule: Installing MPLS forwarding objective for "
                    + "label {} in switch {} with pop", segmentId, targetSwId);

            // bos pop case (php)
            ForwardingObjective.Builder fwdObjBosBuilder =
                    getMplsForwardingObjective(targetSwId,
                                               nextHops,
                                               true,
                                               true,
                                               metabuilder.build());
            if (fwdObjBosBuilder == null) {
                return false;
            }
            fwdObjBuilders.add(fwdObjBosBuilder);

            // XXX not-bos pop case,  SR app multi-label not implemented yet
            /*ForwardingObjective.Builder fwdObjNoBosBuilder =
                    getMplsForwardingObjective(deviceId,
                                               nextHops,
                                               true,
                                               false);*/

        } else {
            // next hop is not destination, SR CONTINUE case (swap with self)
            log.debug("Installing MPLS forwarding objective for "
                    + "label {} in switch {} without pop", segmentId, targetSwId);

            // continue case with bos - this does get triggered in edge routers
            // and in core routers - driver can handle depending on availability
            // of MPLS ECMP or not
            ForwardingObjective.Builder fwdObjBosBuilder =
                    getMplsForwardingObjective(targetSwId,
                                               nextHops,
                                               false,
                                               true,
                                               metabuilder.build());
            if (fwdObjBosBuilder == null) {
                return false;
            }
            fwdObjBuilders.add(fwdObjBosBuilder);

            // XXX continue case with not-bos - SR app multi label not implemented yet
            // also requires MPLS ECMP
            /*ForwardingObjective.Builder fwdObjNoBosBuilder =
                    getMplsForwardingObjective(deviceId,
                                               nextHops,
                                               false,
                                               false); */

        }
        // XXX when other cases above are implemented check for validity of
        // debug messages below
        for (ForwardingObjective.Builder fwdObjBuilder : fwdObjBuilders) {
            ((Builder) ((Builder) fwdObjBuilder.fromApp(srManager.appId)
                    .makePermanent()).withSelector(selector)
                    .withPriority(SegmentRoutingService.DEFAULT_PRIORITY))
                    .withFlag(ForwardingObjective.Flag.SPECIFIC);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("MPLS rule {} for SID {} populated in dev:{} ",
                                             objective.id(), segmentId, targetSwId),
                    (objective, error) ->
                            log.warn("Failed to populate MPLS rule {} for SID {}: {} in dev:{}",
                                     objective.id(), segmentId, error, targetSwId));
            ForwardingObjective fob = fwdObjBuilder.add(context);
            log.debug("Sending MPLS fwd obj {} for SID {}-> next {} in sw: {}",
                      fob.id(), segmentId, fob.nextId(), targetSwId);
            srManager.flowObjectiveService.forward(targetSwId, fob);
            rulePopulationCounter.incrementAndGet();
        }

        return true;
    }

    private ForwardingObjective.Builder getMplsForwardingObjective(
                                             DeviceId deviceId,
                                             Set<DeviceId> nextHops,
                                             boolean phpRequired,
                                             boolean isBos,
                                             TrafficSelector meta) {

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                .builder().withFlag(ForwardingObjective.Flag.SPECIFIC);

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();

        if (phpRequired) {
            // php case - pop should always be flow-action
            log.debug("getMplsForwardingObjective: php required");
            tbuilder.deferred().copyTtlIn();
            if (isBos) {
                tbuilder.deferred().popMpls(EthType.EtherType.IPV4.ethType())
                    .decNwTtl();
            } else {
                tbuilder.deferred().popMpls(EthType.EtherType.MPLS_UNICAST.ethType())
                    .decMplsTtl();
            }
        } else {
            // swap with self case - SR CONTINUE
            log.debug("getMplsForwardingObjective: php not required");
            tbuilder.deferred().decMplsTtl();
        }

        // All forwarding is via ECMP group, the metadata informs the driver
        // that the next-Objective will be used by MPLS flows. In other words,
        // MPLS ECMP is requested. It is up to the driver to decide if these
        // packets will be hashed or not.
        fwdBuilder.withTreatment(tbuilder.build());
        NeighborSet ns = new NeighborSet(nextHops);
        log.debug("Trying to get a nextObjId for mpls rule on device:{} to ns:{}",
                 deviceId, ns);

        int nextId = srManager.getNextObjectiveId(deviceId, ns, meta);
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
     * Note that the vlan assignment is only done by the master-instance for a switch.
     * However we send the filtering objective from slave-instances as well, so
     * that drivers can obtain other information (like Router MAC and IP).
     *
     * @param deviceId  the switch dpid for the router
     * @return PortFilterInfo information about the processed ports
     */
    public PortFilterInfo populateRouterMacVlanFilters(DeviceId deviceId) {
        log.debug("Installing per-port filtering objective for untagged "
                + "packets in device {}", deviceId);

        MacAddress deviceMac;
        try {
            deviceMac = config.getDeviceMac(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateRouterMacVlanFilters.");
            return null;
        }

        List<Port> devPorts = srManager.deviceService.getPorts(deviceId);
        if (devPorts != null && devPorts.size() == 0) {
            log.warn("Device {} ports not available. Unable to add MacVlan filters",
                     deviceId);
            return null;
        }
        int disabledPorts = 0, suppressedPorts = 0, filteredPorts = 0;
        for (Port port : devPorts) {
            ConnectPoint connectPoint = new ConnectPoint(deviceId, port.number());
            // TODO: Handles dynamic port events when we are ready for dynamic config
            SegmentRoutingAppConfig appConfig = srManager.cfgService
                    .getConfig(srManager.appId, SegmentRoutingAppConfig.class);
            if (!port.isEnabled()) {
                disabledPorts++;
                continue;
            }
            if (appConfig != null && appConfig.suppressSubnet().contains(connectPoint)) {
                suppressedPorts++;
                continue;
            }
            Ip4Prefix portSubnet = config.getPortSubnet(deviceId, port.number());
            VlanId assignedVlan = (portSubnet == null)
                    ? VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET)
                    : srManager.getSubnetAssignedVlanId(deviceId, portSubnet);

            FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
            fob.withKey(Criteria.matchInPort(port.number()))
                .addCondition(Criteria.matchEthDst(deviceMac))
                .addCondition(Criteria.matchVlanId(VlanId.NONE))
                .withPriority(SegmentRoutingService.DEFAULT_PRIORITY);
            // vlan assignment is valid only if this instance is master
            if (srManager.mastershipService.isLocalMaster(deviceId)) {
                TrafficTreatment tt = DefaultTrafficTreatment.builder()
                        .pushVlan().setVlanId(assignedVlan).build();
                fob.withMeta(tt);
            }
            fob.permit().fromApp(srManager.appId);
            log.debug("Sending filtering objective for dev/port:{}/{}", deviceId, port);
            filteredPorts++;
            ObjectiveContext context = new DefaultObjectiveContext(
                (objective) -> log.debug("Filter for {} populated", connectPoint),
                (objective, error) ->
                log.warn("Failed to populate filter for {}: {}", connectPoint, error));
            srManager.flowObjectiveService.filter(deviceId, fob.add(context));
        }
        log.info("Filtering on dev:{}, disabledPorts:{}, suppressedPorts:{}, filteredPorts:{}",
                  deviceId, disabledPorts, suppressedPorts, filteredPorts);
        return srManager.defaultRoutingHandler.new PortFilterInfo(disabledPorts,
                                                       suppressedPorts, filteredPorts);
    }

    /**
     * Creates a forwarding objective to punt all IP packets, destined to the
     * router's port IP addresses, to the controller. Note that the input
     * port should not be matched on, as these packets can come from any input.
     * Furthermore, these are applied only by the master instance.
     *
     * @param deviceId the switch dpid for the router
     */
    public void populateRouterIpPunts(DeviceId deviceId) {
        Ip4Address routerIp;
        try {
            routerIp = config.getRouterIp(deviceId);
        } catch (DeviceConfigNotFoundException e) {
            log.warn(e.getMessage() + " Aborting populateRouterIpPunts.");
            return;
        }

        if (!srManager.mastershipService.isLocalMaster(deviceId)) {
            log.debug("Not installing port-IP punts - not the master for dev:{} ",
                      deviceId);
            return;
        }
        ForwardingObjective.Builder puntIp = DefaultForwardingObjective.builder();
        Set<Ip4Address> allIps = new HashSet<>(config.getPortIPs(deviceId));
        allIps.add(routerIp);
        for (Ip4Address ipaddr : allIps) {
            TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
            sbuilder.matchEthType(Ethernet.TYPE_IPV4);
            sbuilder.matchIPDst(IpPrefix.valueOf(ipaddr,
                                                 IpPrefix.MAX_INET_MASK_LENGTH));
            tbuilder.setOutput(PortNumber.CONTROLLER);
            puntIp.withSelector(sbuilder.build());
            puntIp.withTreatment(tbuilder.build());
            puntIp.withFlag(Flag.VERSATILE)
                .withPriority(SegmentRoutingService.HIGHEST_PRIORITY)
                .makePermanent()
                .fromApp(srManager.appId);
            ObjectiveContext context = new DefaultObjectiveContext(
                    (objective) -> log.debug("IP punt rule for {} populated", ipaddr),
                    (objective, error) ->
                            log.warn("Failed to populate IP punt rule for {}: {}", ipaddr, error));
            srManager.flowObjectiveService.forward(deviceId, puntIp.add(context));
        }
    }

    /**
     * Populates a forwarding objective to send packets that miss other high
     * priority Bridging Table entries to a group that contains all ports of
     * its subnet.
     *
     * Note: We assume that packets sending from the edge switches to the hosts
     * have untagged VLAN.
     * The VLAN tag will be popped later in the flooding group.
     *
     * @param deviceId switch ID to set the rules
     */
    public void populateSubnetBroadcastRule(DeviceId deviceId) {
        config.getSubnets(deviceId).forEach(subnet -> {
            if (subnet.prefixLength() == 0 ||
                    subnet.prefixLength() == IpPrefix.MAX_INET_MASK_LENGTH) {
                return;
            }
            int nextId = srManager.getSubnetNextObjectiveId(deviceId, subnet);
            VlanId vlanId = srManager.getSubnetAssignedVlanId(deviceId, subnet);

            if (nextId < 0 || vlanId == null) {
                log.error("Cannot install subnet {} broadcast rule in dev:{} due"
                        + "to vlanId:{} or nextId:{}", subnet, deviceId, vlanId, nextId);
                return;
            }

            /* Driver should treat objective with MacAddress.NONE as the
             * subnet broadcast rule
             */
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
                    (objective) -> log.debug("Subnet broadcast rule for {} populated", subnet),
                    (objective, error) ->
                            log.warn("Failed to populate subnet broadcast rule for {}: {}", subnet, error));
            srManager.flowObjectiveService.forward(deviceId, fob.add(context));
        });
    }

    private int getPriorityFromPrefix(IpPrefix prefix) {
        return (prefix.isIp4()) ?
                2000 * prefix.prefixLength() + SegmentRoutingService.MIN_IP_PRIORITY :
                500 * prefix.prefixLength() + SegmentRoutingService.MIN_IP_PRIORITY;
    }
}
