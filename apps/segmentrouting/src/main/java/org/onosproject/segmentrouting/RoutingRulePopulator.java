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
import org.onlab.packet.VlanId;
import org.onosproject.segmentrouting.grouphandler.NeighborSet;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
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
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.flowobjective.ForwardingObjective.Builder;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

public class RoutingRulePopulator {

    private static final Logger log = LoggerFactory
            .getLogger(RoutingRulePopulator.class);

    private AtomicLong rulePopulationCounter;
    private SegmentRoutingManager srManager;
    private DeviceConfiguration config;

    private static final int HIGHEST_PRIORITY = 0xffff;
    private static final long OFPP_MAX = 0xffffff00L;


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
     * Populates IP flow rules for specific hosts directly connected to the
     * switch.
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

        sbuilder.matchIPDst(IpPrefix.valueOf(hostIp, IpPrefix.MAX_INET_MASK_LENGTH));
        sbuilder.matchEthType(Ethernet.TYPE_IPV4);

        tbuilder.deferred()
                .setEthDst(hostMac)
                .setEthSrc(config.getDeviceMac(deviceId))
                .setOutput(outPort);

        TrafficTreatment treatment = tbuilder.build();
        TrafficSelector selector = sbuilder.build();

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                .builder().fromApp(srManager.appId).makePermanent()
                .withSelector(selector).withTreatment(treatment)
                .withPriority(100).withFlag(ForwardingObjective.Flag.SPECIFIC);

        log.debug("Installing IPv4 forwarding objective "
                + "for host {} in switch {}", hostIp, deviceId);
        srManager.flowObjectiveService.
            forward(deviceId,
                    fwdBuilder.
                    add(new SRObjectiveContext(deviceId,
                                           SRObjectiveContext.ObjectiveType.FORWARDING)));
        rulePopulationCounter.incrementAndGet();
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
    public boolean populateIpRuleForSubnet(DeviceId deviceId,
                                           Set<Ip4Prefix> subnets,
                                           DeviceId destSw,
                                           Set<DeviceId> nextHops) {

        for (IpPrefix subnet : subnets) {
            if (!populateIpRuleForRouter(deviceId, subnet, destSw, nextHops)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Populates IP flow rules for the router IP address.
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

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();

        sbuilder.matchIPDst(ipPrefix);
        sbuilder.matchEthType(Ethernet.TYPE_IPV4);

        NeighborSet ns = null;

        // If the next hop is the same as the final destination, then MPLS label
        // is not set.
        if (nextHops.size() == 1 && nextHops.toArray()[0].equals(destSw)) {
            tbuilder.deferred().decNwTtl();
            ns = new NeighborSet(nextHops);
        } else {
            tbuilder.deferred().copyTtlOut();
            ns = new NeighborSet(nextHops, config.getSegmentId(destSw));
        }

        TrafficTreatment treatment = tbuilder.build();
        TrafficSelector selector = sbuilder.build();

        if (srManager.getNextObjectiveId(deviceId, ns) <= 0) {
            log.warn("No next objective in {} for ns: {}", deviceId, ns);
            return false;
        }

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                .builder()
                .fromApp(srManager.appId)
                .makePermanent()
                .nextStep(srManager.getNextObjectiveId(deviceId, ns))
                .withTreatment(treatment)
                .withSelector(selector)
                .withPriority(100)
                .withFlag(ForwardingObjective.Flag.SPECIFIC);
        log.debug("Installing IPv4 forwarding objective "
                        + "for router IP/subnet {} in switch {}",
                ipPrefix,
                deviceId);
        srManager.flowObjectiveService.
            forward(deviceId,
                    fwdBuilder.
                    add(new SRObjectiveContext(deviceId,
                                               SRObjectiveContext.ObjectiveType.FORWARDING)));
        rulePopulationCounter.incrementAndGet();

        return true;
    }

    /**
     * Populates MPLS flow rules to all routers.
     *
     * @param deviceId target device ID of the switch to set the rules
     * @param destSwId destination switch device ID
     * @param nextHops next hops switch ID list
     * @return true if all rules are set successfully, false otherwise
     */
    public boolean populateMplsRule(DeviceId deviceId, DeviceId destSwId,
                                    Set<DeviceId> nextHops) {

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        List<ForwardingObjective.Builder> fwdObjBuilders = new ArrayList<>();

        // TODO Handle the case of Bos == false
        sbuilder.matchMplsLabel(MplsLabel.mplsLabel(config.getSegmentId(destSwId)));
        sbuilder.matchEthType(Ethernet.MPLS_UNICAST);

        // If the next hop is the destination router, do PHP
        if (nextHops.size() == 1 && destSwId.equals(nextHops.toArray()[0])) {
            log.debug("populateMplsRule: Installing MPLS forwarding objective for "
                    + "label {} in switch {} with PHP",
                    config.getSegmentId(destSwId),
                    deviceId);

            ForwardingObjective.Builder fwdObjBosBuilder =
                    getMplsForwardingObjective(deviceId,
                                               destSwId,
                                               nextHops,
                                               true,
                                               true);
            // TODO: Check with Sangho on why we need this
            ForwardingObjective.Builder fwdObjNoBosBuilder =
                    getMplsForwardingObjective(deviceId,
                                               destSwId,
                                               nextHops,
                                               true,
                                               false);
            if (fwdObjBosBuilder != null) {
                fwdObjBuilders.add(fwdObjBosBuilder);
            } else {
                log.warn("Failed to set MPLS rules.");
                return false;
            }
        } else {
            log.debug("Installing MPLS forwarding objective for "
                    + "label {} in switch {} without PHP",
                    config.getSegmentId(destSwId),
                    deviceId);

            ForwardingObjective.Builder fwdObjBosBuilder =
                    getMplsForwardingObjective(deviceId,
                                               destSwId,
                                               nextHops,
                                               false,
                                               true);
            // TODO: Check with Sangho on why we need this
            ForwardingObjective.Builder fwdObjNoBosBuilder =
                    getMplsForwardingObjective(deviceId,
                                               destSwId,
                                               nextHops,
                                               false,
                                               false);
            if (fwdObjBosBuilder != null) {
                fwdObjBuilders.add(fwdObjBosBuilder);
            } else {
                log.warn("Failed to set MPLS rules.");
                return false;
            }
        }

        TrafficSelector selector = sbuilder.build();
        for (ForwardingObjective.Builder fwdObjBuilder : fwdObjBuilders) {
            ((Builder) ((Builder) fwdObjBuilder.fromApp(srManager.appId)
                    .makePermanent()).withSelector(selector)
                    .withPriority(100))
                    .withFlag(ForwardingObjective.Flag.SPECIFIC);
            srManager.flowObjectiveService.
                forward(deviceId,
                        fwdObjBuilder.
                        add(new SRObjectiveContext(deviceId,
                                                   SRObjectiveContext.ObjectiveType.FORWARDING)));
            rulePopulationCounter.incrementAndGet();
        }

        return true;
    }

    private ForwardingObjective.Builder getMplsForwardingObjective(DeviceId deviceId,
                                                                   DeviceId destSw,
                                                                   Set<DeviceId> nextHops,
                                                                   boolean phpRequired,
                                                                   boolean isBos) {

        ForwardingObjective.Builder fwdBuilder = DefaultForwardingObjective
                .builder().withFlag(ForwardingObjective.Flag.SPECIFIC);

        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();

        if (phpRequired) {
            log.debug("getMplsForwardingObjective: php required");
            tbuilder.deferred().copyTtlIn();
            if (isBos) {
                tbuilder.deferred().popMpls(Ethernet.TYPE_IPV4).decNwTtl();
            } else {
                tbuilder.deferred().popMpls(Ethernet.MPLS_UNICAST).decMplsTtl();
            }
        } else {
            log.debug("getMplsForwardingObjective: php not required");
            tbuilder.deferred().decMplsTtl();
        }

        if (!isECMPSupportedInTransitRouter() && !config.isEdgeDevice(deviceId)) {
            PortNumber port = selectOnePort(deviceId, nextHops);
            DeviceId nextHop = (DeviceId) nextHops.toArray()[0];
            if (port == null) {
                log.warn("No link from {} to {}", deviceId, nextHops);
                return null;
            }
            tbuilder.deferred()
                    .setEthSrc(config.getDeviceMac(deviceId))
                    .setEthDst(config.getDeviceMac(nextHop))
                    .setOutput(port);
            fwdBuilder.withTreatment(tbuilder.build());
        } else {
            NeighborSet ns = new NeighborSet(nextHops);
            fwdBuilder.withTreatment(tbuilder.build());
            fwdBuilder.nextStep(srManager
                    .getNextObjectiveId(deviceId, ns));
        }

        return fwdBuilder;
    }

    private boolean isECMPSupportedInTransitRouter() {

        // TODO: remove this function when objectives subsystem is supported.
        return false;
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
     */
    public void populateRouterMacVlanFilters(DeviceId deviceId) {
        log.debug("Installing per-port filtering objective for untagged "
                + "packets in device {}", deviceId);
        for (Port port : srManager.deviceService.getPorts(deviceId)) {
            if (port.number().toLong() > 0 && port.number().toLong() < OFPP_MAX) {
                Ip4Prefix portSubnet = config.getPortSubnet(deviceId, port.number());
                VlanId assignedVlan = (portSubnet == null)
                        ? VlanId.vlanId(SegmentRoutingManager.ASSIGNED_VLAN_NO_SUBNET)
                        : srManager.getSubnetAssignedVlanId(deviceId, portSubnet);
                FilteringObjective.Builder fob = DefaultFilteringObjective.builder();
                fob.withKey(Criteria.matchInPort(port.number()))
                .addCondition(Criteria.matchEthDst(config.getDeviceMac(deviceId)))
                .addCondition(Criteria.matchVlanId(VlanId.NONE));
                // vlan assignment is valid only if this instance is master
                if (srManager.mastershipService.isLocalMaster(deviceId)) {
                    TrafficTreatment tt = DefaultTrafficTreatment.builder()
                            .pushVlan().setVlanId(assignedVlan).build();
                    fob.setMeta(tt);
                }
                fob.permit().fromApp(srManager.appId);
                srManager.flowObjectiveService.
                filter(deviceId, fob.add(new SRObjectiveContext(deviceId,
                                      SRObjectiveContext.ObjectiveType.FILTER)));
            }
        }
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
        if (!srManager.mastershipService.isLocalMaster(deviceId)) {
            log.debug("Not installing port-IP punts - not the master for dev:{} ",
                      deviceId);
            return;
        }
        ForwardingObjective.Builder puntIp = DefaultForwardingObjective.builder();
        Set<Ip4Address> allIps = new HashSet<Ip4Address>(config.getPortIPs(deviceId));
        allIps.add(config.getRouterIp(deviceId));
        for (Ip4Address ipaddr : allIps) {
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchIPDst(IpPrefix.valueOf(ipaddr,
                                                 IpPrefix.MAX_INET_MASK_LENGTH));
            treatment.setOutput(PortNumber.CONTROLLER);
            puntIp.withSelector(selector.build());
            puntIp.withTreatment(treatment.build());
            puntIp.withFlag(Flag.VERSATILE)
                .withPriority(HIGHEST_PRIORITY)
                .makePermanent()
                .fromApp(srManager.appId);
            log.debug("Installing forwarding objective to punt port IP addresses");
            srManager.flowObjectiveService.
                forward(deviceId,
                        puntIp.add(new SRObjectiveContext(deviceId,
                                           SRObjectiveContext.ObjectiveType.FORWARDING)));
        }
    }

    private PortNumber selectOnePort(DeviceId srcId, Set<DeviceId> destIds) {

        Set<Link> links = srManager.linkService.getDeviceLinks(srcId);
        for (DeviceId destId: destIds) {
            for (Link link : links) {
                if (link.dst().deviceId().equals(destId)) {
                    return link.src().port();
                } else if (link.src().deviceId().equals(destId)) {
                    return link.dst().port();
                }
            }
        }

        return null;
    }

    private static class SRObjectiveContext implements ObjectiveContext {
        enum ObjectiveType {
            FILTER,
            FORWARDING
        }
        final DeviceId deviceId;
        final ObjectiveType type;

        SRObjectiveContext(DeviceId deviceId, ObjectiveType type) {
            this.deviceId = deviceId;
            this.type = type;
        }
        @Override
        public void onSuccess(Objective objective) {
            log.debug("{} objective operation successful in device {}",
                      type.name(), deviceId);
        }

        @Override
        public void onError(Objective objective, ObjectiveError error) {
            log.warn("{} objective {} operation failed with error: {} in device {}",
                     type.name(), objective, error, deviceId);
        }
    }

}
