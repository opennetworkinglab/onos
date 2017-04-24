/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.driver.pipeline.ofdpa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.Icmpv6CodeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.packet.PacketPriority;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import static org.onlab.packet.IPv6.PROTOCOL_ICMP6;
import static org.onlab.packet.MacAddress.BROADCAST;
import static org.onlab.packet.MacAddress.NONE;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.*;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Driver for software switch emulation of the OFDPA pipeline.
 * The software switch is the CPqD OF 1.3 switch. Unfortunately the CPqD switch
 * does not handle vlan tags and mpls labels simultaneously, which requires us
 * to do some workarounds in the driver. This driver is meant for the use of
 * the cpqd switch when MPLS is required. As a result this driver works only
 * on incoming untagged packets.
 */
public class CpqdOfdpa2Pipeline extends Ofdpa2Pipeline {

    private final Logger log = getLogger(getClass());

    /**
     * Table that determines whether VLAN is popped before punting to controller.
     * <p>
     * This is a non-OFDPA table to emulate OFDPA packet in behavior.
     * VLAN will be popped before punting if the VLAN is internally assigned.
     * <p>
     * Also note that 63 is the max table number in CpqD.
     */
    private static final int PUNT_TABLE = 63;

    /**
     * A static indirect group that pop vlan and punt to controller.
     * <p>
     * The purpose of using a group instead of immediate action is that this
     * won't affect another copy on the data plane when write action exists.
     */
    private static final int POP_VLAN_PUNT_GROUP_ID = 0xc0000000;

    @Override
    protected boolean requireVlanExtensions() {
        return false;
    }

    /**
     * Determines whether this pipeline support copy ttl instructions or not.
     *
     * @return true if copy ttl instructions are supported
     */
    protected boolean supportCopyTtl() {
        return true;
    }

    /**
     * Determines whether this pipeline support push mpls to vlan-tagged packets or not.
     * <p>
     * If not support, pop vlan before push entering unicast and mpls table.
     * Side effect: HostService learns redundant hosts with same MAC but
     * different VLAN. No known side effect on the network reachability.
     *
     * @return true if push mpls to vlan-tagged packets is supported
     */
    protected boolean supportTaggedMpls() {
        return false;
    }

    /**
     * Determines whether this pipeline support punt action in group bucket.
     *
     * @return true if punt action in group bucket is supported
     */
    protected boolean supportPuntGroup() {
        return false;
    }

    @Override
    protected void initDriverId() {
        driverId = coreService.registerApplication(
                "org.onosproject.driver.CpqdOfdpa2Pipeline");
    }

    @Override
    protected void initGroupHander(PipelinerContext context) {
        groupHandler = new CpqdOfdpa2GroupHandler();
        groupHandler.init(deviceId, context);
    }

    /*
     * Cpqd emulation does not require the non OF-standard rules for
     * matching untagged packets that ofdpa uses.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#processVlanIdFilter
     */
    @Override
    protected List<FlowRule> processVlanIdFilter(PortCriterion portCriterion,
                                                 VlanIdCriterion vidCriterion,
                                                 VlanId assignedVlan,
                                                 ApplicationId applicationId) {
        List<FlowRule> rules = new ArrayList<>();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchVlanId(vidCriterion.vlanId());
        treatment.transition(TMAC_TABLE);

        if (vidCriterion.vlanId() == VlanId.NONE) {
            // untagged packets are assigned vlans
            treatment.pushVlan().setVlanId(assignedVlan);
        }

        // ofdpa cannot match on ALL portnumber, so we need to use separate
        // rules for each port.
        List<PortNumber> portnums = new ArrayList<PortNumber>();
        if (portCriterion.port() == PortNumber.ALL) {
            for (Port port : deviceService.getPorts(deviceId)) {
                if (port.number().toLong() > 0 && port.number().toLong() < OFPP_MAX) {
                    portnums.add(port.number());
                }
            }
        } else {
            portnums.add(portCriterion.port());
        }

        for (PortNumber pnum : portnums) {
            // NOTE: Emulating OFDPA behavior by popping off internal assigned
            //       VLAN before sending to controller
            if (supportPuntGroup() && vidCriterion.vlanId() == VlanId.NONE) {
                GroupKey groupKey = popVlanPuntGroupKey();
                Group group = groupService.getGroup(deviceId, groupKey);
                if (group != null) {
                    rules.add(buildPuntTableRule(pnum, assignedVlan));
                } else {
                    log.info("popVlanPuntGroup not found in dev:{}", deviceId);
                    return Collections.emptyList();
                }
            }

            // create rest of flowrule
            selector.matchInPort(pnum);
            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(VLAN_TABLE).build();
            rules.add(rule);
        }

        return rules;
    }

    /**
     * Creates punt table entry that matches IN_PORT and VLAN_VID and points to
     * a group that pop vlan and punt.
     *
     * @param portNumber port number
     * @param assignedVlan internally assigned vlan id
     * @return punt table flow rule
     */
    private FlowRule buildPuntTableRule(PortNumber portNumber, VlanId assignedVlan) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder()
                .matchInPort(portNumber)
                .matchVlanId(assignedVlan);
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder()
                .group(new GroupId(POP_VLAN_PUNT_GROUP_ID));

        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(sbuilder.build())
                .withTreatment(tbuilder.build())
                .withPriority(PacketPriority.CONTROL.priorityValue())
                .fromApp(driverId)
                .makePermanent()
                .forTable(PUNT_TABLE).build();
    }

    /**
     * Builds a punt to the controller rule for the arp protocol.
     * <p>
     * NOTE: CpqD cannot punt correctly in group bucket. The current impl will
     *       pop VLAN before sending to controller disregarding whether
     *       it's an internally assigned VLAN or a natural VLAN.
     *       Therefore, trunk port is not supported in CpqD.
     *
     * @param assignedVlan the internal assigned vlan id
     * @param applicationId the application id
     * @return the punt flow rule for the arp
     */
    private FlowRule buildArpPunt(VlanId assignedVlan, ApplicationId applicationId) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP)
                .matchVlanId(assignedVlan);
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder()
                .popVlan()
                .punt();

        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(sbuilder.build())
                .withTreatment(tbuilder.build())
                .withPriority(PacketPriority.CONTROL.priorityValue() + 1)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(ACL_TABLE).build();
    }

    /**
     * Builds a punt to the controller rule for the icmp v6 messages.
     * <p>
     * NOTE: CpqD cannot punt correctly in group bucket. The current impl will
     *       pop VLAN before sending to controller disregarding whether
     *       it's an internally assigned VLAN or a natural VLAN.
     *       Therefore, trunk port is not supported in CpqD.
     *
     * @param assignedVlan the internal assigned vlan id
     * @param applicationId the application id
     * @return the punt flow rule for the icmp v6 messages
     */
    private FlowRule buildIcmpV6Punt(VlanId assignedVlan, ApplicationId applicationId) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder()
                .matchVlanId(assignedVlan)
                .matchEthType(Ethernet.TYPE_IPV6)
                .matchIPProtocol(PROTOCOL_ICMP6);
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder()
                .popVlan()
                .punt();

        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(sbuilder.build())
                .withTreatment(tbuilder.build())
                .withPriority(PacketPriority.CONTROL.priorityValue() + 1)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(ACL_TABLE).build();
    }

    /*
     * Cpqd emulation does not handle vlan tags and mpls labels correctly.
     * Workaround requires popping off the VLAN tags in the TMAC table.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#processEthDstFilter
     */
    @Override
    protected List<FlowRule> processEthDstFilter(PortCriterion portCriterion,
                                                 EthCriterion ethCriterion,
                                                 VlanIdCriterion vidCriterion,
                                                 VlanId assignedVlan,
                                                 ApplicationId applicationId) {
        // Consider PortNumber.ANY as wildcard. Match ETH_DST only
        if (portCriterion != null && portCriterion.port() == PortNumber.ANY) {
            return processEthDstOnlyFilter(ethCriterion, applicationId);
        }

        // Multicast MAC
        if (ethCriterion.mask() != null) {
            return processMcastEthDstFilter(ethCriterion, applicationId);
        }

        //handling untagged packets via assigned VLAN
        if (vidCriterion.vlanId() == VlanId.NONE) {
            vidCriterion = (VlanIdCriterion) Criteria.matchVlanId(assignedVlan);
        }
        // ofdpa cannot match on ALL portnumber, so we need to use separate
        // rules for each port.
        List<PortNumber> portnums = new ArrayList<PortNumber>();
        if (portCriterion.port() == PortNumber.ALL) {
            for (Port port : deviceService.getPorts(deviceId)) {
                if (port.number().toLong() > 0 && port.number().toLong() < OFPP_MAX) {
                    portnums.add(port.number());
                }
            }
        } else {
            portnums.add(portCriterion.port());
        }

        List<FlowRule> rules = new ArrayList<FlowRule>();
        for (PortNumber pnum : portnums) {
            // TMAC rules for unicast IP packets
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(pnum);
            selector.matchVlanId(vidCriterion.vlanId());
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchEthDst(ethCriterion.mac());
            if (!supportTaggedMpls()) {
                treatment.popVlan();
            }
            treatment.transition(UNICAST_ROUTING_TABLE);
            FlowRule rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(TMAC_TABLE).build();
            rules.add(rule);

            // TMAC rules for MPLS packets
            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(pnum);
            selector.matchVlanId(vidCriterion.vlanId());
            selector.matchEthType(Ethernet.MPLS_UNICAST);
            selector.matchEthDst(ethCriterion.mac());
            if (!supportTaggedMpls()) {
                treatment.popVlan();
            }
            treatment.transition(MPLS_TABLE_0);
            rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(TMAC_TABLE).build();
            rules.add(rule);

            // TMAC rules for IPv6 packets
            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(pnum);
            selector.matchVlanId(vidCriterion.vlanId());
            selector.matchEthType(Ethernet.TYPE_IPV6);
            selector.matchEthDst(ethCriterion.mac());
            if (!supportTaggedMpls()) {
                treatment.popVlan();
            }
            treatment.transition(UNICAST_ROUTING_TABLE);
            rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(TMAC_TABLE).build();
            rules.add(rule);
        }
        return rules;
    }

    @Override
    protected List<FlowRule> processEthDstOnlyFilter(EthCriterion ethCriterion,
                                                     ApplicationId applicationId) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchEthDst(ethCriterion.mac());
        if (!supportTaggedMpls()) {
            treatment.popVlan();
        }
        treatment.transition(UNICAST_ROUTING_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        return ImmutableList.<FlowRule>builder().add(rule).build();
    }

    /*
     * Cpqd emulation allows MPLS ecmp.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#processEthTypeSpecific
     */
    @Override
    protected Collection<FlowRule> processEthTypeSpecific(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        if ((ethType == null) ||
                (ethType.ethType().toShort() != Ethernet.TYPE_IPV4) &&
                        (ethType.ethType().toShort() != Ethernet.MPLS_UNICAST) &&
                        (ethType.ethType().toShort() != Ethernet.TYPE_IPV6)) {
            log.warn("processSpecific: Unsupported forwarding objective criteria"
                    + "ethType:{} in dev:{}", ethType, deviceId);
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }
        boolean defaultRule = false;
        int forTableId = -1;
        TrafficSelector.Builder filteredSelector = DefaultTrafficSelector.builder();
        TrafficSelector.Builder complementarySelector = DefaultTrafficSelector.builder();

        if (ethType.ethType().toShort() == Ethernet.TYPE_IPV4) {
            IpPrefix ipv4Dst = ((IPCriterion) selector.getCriterion(Criterion.Type.IPV4_DST)).ip();
            if (ipv4Dst.isMulticast()) {
                if (ipv4Dst.prefixLength() != 32) {
                    log.warn("Multicast specific forwarding objective can only be /32");
                    fail(fwd, ObjectiveError.BADPARAMS);
                    return ImmutableSet.of();
                }
                VlanId assignedVlan = readVlanFromSelector(fwd.meta());
                if (assignedVlan == null) {
                    log.warn("VLAN ID required by multicast specific fwd obj is missing. Abort.");
                    fail(fwd, ObjectiveError.BADPARAMS);
                    return ImmutableSet.of();
                }
                filteredSelector.matchVlanId(assignedVlan);
                filteredSelector.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(ipv4Dst);
                forTableId = MULTICAST_ROUTING_TABLE;
                log.debug("processing IPv4 multicast specific forwarding objective {} -> next:{}"
                        + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
            } else {
                if (ipv4Dst.prefixLength() == 0) {
                    // The entire IPV4_DST field is wildcarded intentionally
                    filteredSelector.matchEthType(Ethernet.TYPE_IPV4);
                } else {
                    filteredSelector.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(ipv4Dst);
                }
                forTableId = UNICAST_ROUTING_TABLE;
                log.debug("processing IPv4 unicast specific forwarding objective {} -> next:{}"
                        + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
            }
        } else if (ethType.ethType().toShort() == Ethernet.TYPE_IPV6) {
            if (buildIpv6Selector(filteredSelector, fwd) < 0) {
                return Collections.emptyList();
            }
            forTableId = UNICAST_ROUTING_TABLE;
        } else {
            filteredSelector
                .matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(((MplsCriterion)
                        selector.getCriterion(Criterion.Type.MPLS_LABEL)).label());
            MplsBosCriterion bos = (MplsBosCriterion) selector
                                        .getCriterion(Criterion.Type.MPLS_BOS);
            if (bos != null) {
                filteredSelector.matchMplsBos(bos.mplsBos());
            }
            forTableId = MPLS_TABLE_1;
            log.debug("processing MPLS specific forwarding objective {} -> next:{}"
                    + " in dev {}", fwd.id(), fwd.nextId(), deviceId);
        }

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();
        if (fwd.treatment() != null) {
            for (Instruction i : fwd.treatment().allInstructions()) {
                if (!supportCopyTtl() && i instanceof L3ModificationInstruction) {
                    L3ModificationInstruction l3instr = (L3ModificationInstruction) i;
                    if (l3instr.subtype().equals(L3ModificationInstruction.L3SubType.TTL_IN) ||
                            l3instr.subtype().equals(L3ModificationInstruction.L3SubType.TTL_OUT)) {
                        continue;
                    }
                }
                /*
                 * NOTE: OF-DPA does not support immediate instruction in
                 * L3 unicast and MPLS table.
                 */
                tb.deferred().add(i);
            }
        }

        if (fwd.nextId() != null) {
            NextGroup next = getGroupForNextObjective(fwd.nextId());
            if (next != null) {
                List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
                // we only need the top level group's key to point the flow to it
                Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
                if (group == null) {
                    log.warn("Group with key:{} for next-id:{} not found in dev:{}",
                             gkeys.get(0).peekFirst(), fwd.nextId(), deviceId);
                    fail(fwd, ObjectiveError.GROUPMISSING);
                    return Collections.emptySet();
                }
                tb.deferred().group(group.id());
            }
        }
        tb.transition(ACL_TABLE);
        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector.build())
                .withTreatment(tb.build())
                .forTable(forTableId);

        if (fwd.permanent()) {
            ruleBuilder.makePermanent();
        } else {
            ruleBuilder.makeTemporary(fwd.timeout());
        }
        Collection<FlowRule> flowRuleCollection = new ArrayList<>();
        flowRuleCollection.add(ruleBuilder.build());
        if (defaultRule) {
            flowRuleCollection.add(
                    defaultRoute(fwd, complementarySelector, forTableId, tb)
            );
            log.debug("Default rule 0.0.0.0/0 is being installed two rules");
        }
        return flowRuleCollection;
    }

    @Override
    protected Collection<FlowRule> processEthDstSpecific(ForwardingObjective fwd) {
        List<FlowRule> rules = new ArrayList<>();

        // Build filtered selector
        TrafficSelector selector = fwd.selector();
        EthCriterion ethCriterion = (EthCriterion) selector
                .getCriterion(Criterion.Type.ETH_DST);
        VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) selector
                .getCriterion(Criterion.Type.VLAN_VID);

        if (vlanIdCriterion == null) {
            log.warn("Forwarding objective for bridging requires vlan. Not "
                    + "installing fwd:{} in dev:{}", fwd.id(), deviceId);
            fail(fwd, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }

        TrafficSelector.Builder filteredSelectorBuilder =
                DefaultTrafficSelector.builder();
        // Do not match MacAddress for subnet broadcast entry
        if (!ethCriterion.mac().equals(NONE) && !ethCriterion.mac().equals(BROADCAST)) {
            filteredSelectorBuilder.matchEthDst(ethCriterion.mac());
            log.debug("processing L2 forwarding objective:{} -> next:{} in dev:{}",
                    fwd.id(), fwd.nextId(), deviceId);
        } else {
            log.debug("processing L2 Broadcast forwarding objective:{} -> next:{} "
                            + "in dev:{} for vlan:{}",
                    fwd.id(), fwd.nextId(), deviceId, vlanIdCriterion.vlanId());
        }
        filteredSelectorBuilder.matchVlanId(vlanIdCriterion.vlanId());
        TrafficSelector filteredSelector = filteredSelectorBuilder.build();

        if (fwd.treatment() != null) {
            log.warn("Ignoring traffic treatment in fwd rule {} meant for L2 table"
                    + "for dev:{}. Expecting only nextId", fwd.id(), deviceId);
        }

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        if (fwd.nextId() != null) {
            NextGroup next = getGroupForNextObjective(fwd.nextId());
            if (next != null) {
                List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
                // we only need the top level group's key to point the flow to it
                Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
                if (group != null) {
                    treatmentBuilder.deferred().group(group.id());
                } else {
                    log.warn("Group with key:{} for next-id:{} not found in dev:{}",
                            gkeys.get(0).peekFirst(), fwd.nextId(), deviceId);
                    fail(fwd, ObjectiveError.GROUPMISSING);
                    return Collections.emptySet();
                }
            }
        }
        treatmentBuilder.immediate().transition(ACL_TABLE);
        TrafficTreatment filteredTreatment = treatmentBuilder.build();

        // Build bridging table entries
        FlowRule.Builder flowRuleBuilder = DefaultFlowRule.builder();
        flowRuleBuilder.fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(filteredSelector)
                .withTreatment(filteredTreatment)
                .forTable(BRIDGING_TABLE);
        if (fwd.permanent()) {
            flowRuleBuilder.makePermanent();
        } else {
            flowRuleBuilder.makeTemporary(fwd.timeout());
        }
        rules.add(flowRuleBuilder.build());
        return rules;
    }

    /*
     * In the OF-DPA 2.0 pipeline, versatile forwarding objectives go to the
     * ACL table. Because we pop off vlan tags in TMAC table,
     * we need to avoid matching on vlans in the ACL table.
     */
    @Override
    protected Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.info("Processing versatile forwarding objective");

        EthTypeCriterion ethType =
                (EthTypeCriterion) fwd.selector().getCriterion(Criterion.Type.ETH_TYPE);
        if (ethType == null) {
            log.error("Versatile forwarding objective must include ethType");
            fail(fwd, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        if (fwd.nextId() == null && fwd.treatment() == null) {
            log.error("Forwarding objective {} from {} must contain "
                    + "nextId or Treatment", fwd.selector(), fwd.appId());
            return Collections.emptySet();
        }

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        fwd.selector().criteria().forEach(criterion -> {
            if (criterion instanceof VlanIdCriterion) {
                // avoid matching on vlans
                return;
            } else if (criterion instanceof Icmpv6TypeCriterion ||
                    criterion instanceof Icmpv6CodeCriterion) {
                /*
                 * We silenty discard these criterions, our current
                 * OFDPA platform does not support these matches on
                 * the ACL table.
                 */
                log.warn("ICMPv6 Type and ICMPv6 Code are not supported");
            } else {
                sbuilder.add(criterion);
            }
        });

        // XXX driver does not currently do type checking as per Tables 65-67 in
        // OFDPA 2.0 spec. The only allowed treatment is a punt to the controller.
        TrafficTreatment.Builder ttBuilder = DefaultTrafficTreatment.builder();
        if (fwd.treatment() != null) {
            for (Instruction ins : fwd.treatment().allInstructions()) {
                if (ins instanceof OutputInstruction) {
                    OutputInstruction o = (OutputInstruction) ins;
                    if (o.port() == PortNumber.CONTROLLER) {
                        ttBuilder.transition(PUNT_TABLE);
                    } else {
                        log.warn("Only allowed treatments in versatile forwarding "
                                + "objectives are punts to the controller");
                    }
                } else {
                    log.warn("Cannot process instruction in versatile fwd {}", ins);
                }
            }
            if (fwd.treatment().clearedDeferred()) {
                ttBuilder.wipeDeferred();
            }
        }
        if (fwd.nextId() != null) {
            // overide case
            NextGroup next = getGroupForNextObjective(fwd.nextId());
            List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
            // we only need the top level group's key to point the flow to it
            Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
            if (group == null) {
                log.warn("Group with key:{} for next-id:{} not found in dev:{}",
                         gkeys.get(0).peekFirst(), fwd.nextId(), deviceId);
                fail(fwd, ObjectiveError.GROUPMISSING);
                return Collections.emptySet();
            }
            ttBuilder.deferred().group(group.id());
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(sbuilder.build())
                .withTreatment(ttBuilder.build())
                .makePermanent()
                .forTable(ACL_TABLE);
        return Collections.singletonList(ruleBuilder.build());
    }

    /*
     * Cpqd emulation requires table-miss-entries in forwarding tables.
     * Real OFDPA does not require these rules as they are put in by default.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#initializePipeline()
     */
    @Override
    protected void initializePipeline() {
        initTableMiss(PORT_TABLE, VLAN_TABLE, null);
        initTableMiss(VLAN_TABLE, ACL_TABLE, null);
        initTableMiss(TMAC_TABLE, BRIDGING_TABLE, null);
        initTableMiss(UNICAST_ROUTING_TABLE, ACL_TABLE, null);
        initTableMiss(MULTICAST_ROUTING_TABLE, ACL_TABLE, null);
        initTableMiss(MPLS_TABLE_0, MPLS_TABLE_1, null);
        initTableMiss(MPLS_TABLE_1, ACL_TABLE, null);
        initTableMiss(BRIDGING_TABLE, ACL_TABLE, null);
        initTableMiss(ACL_TABLE, -1, null);

        if (supportPuntGroup()) {
            initTableMiss(PUNT_TABLE, -1,
                    DefaultTrafficTreatment.builder().punt().build());
            initPopVlanPuntGroup();
        } else {
            initTableMiss(PUNT_TABLE, -1,
                    DefaultTrafficTreatment.builder().popVlan().punt().build());
        }
    }

    /**
     * Install table-miss flow entry.
     *
     * If treatment exists, use it directly.
     * Else if treatment does not exist but nextTable > 0, transit to next table.
     * Else apply empty treatment.
     *
     * @param thisTable this table ID
     * @param nextTable next table ID
     * @param treatment traffic treatment to apply.
     */
    private void initTableMiss(int thisTable, int nextTable, TrafficTreatment treatment) {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector selector = DefaultTrafficSelector.builder().build();

        if (treatment == null) {
            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            if (nextTable > 0) {
                tBuilder.transition(nextTable);
            }
            treatment = tBuilder.build();
        }

        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(thisTable).build();
        ops =  ops.add(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized table {} on {}", thisTable, deviceId);
            }
            @Override
            public void onError(FlowRuleOperations ops) {
                log.warn("Failed to initialize table {} on {}", thisTable, deviceId);
            }
        }));
    }

    /**
     * Builds a indirect group contains pop_vlan and punt actions.
     * <p>
     * Using group instead of immediate action to ensure that
     * the copy of packet on the data plane is not affected by the pop vlan action.
     */
    private void initPopVlanPuntGroup() {
        GroupKey groupKey = popVlanPuntGroupKey();
        TrafficTreatment bucketTreatment = DefaultTrafficTreatment.builder()
                .popVlan().punt().build();
        GroupBucket bucket =
                DefaultGroupBucket.createIndirectGroupBucket(bucketTreatment);
        GroupDescription groupDesc =
                new DefaultGroupDescription(
                        deviceId,
                        GroupDescription.Type.INDIRECT,
                        new GroupBuckets(Collections.singletonList(bucket)),
                        groupKey,
                        POP_VLAN_PUNT_GROUP_ID,
                        driverId);
        groupService.addGroup(groupDesc);

        log.info("Initialized pop vlan punt group on {}", deviceId);
    }

    /**
     * Generates group key for a static indirect group that pop vlan and punt to
     * controller.
     *
     * @return the group key of the indirect table
     */
    private GroupKey popVlanPuntGroupKey() {
        int hash = POP_VLAN_PUNT_GROUP_ID | (Objects.hash(deviceId) & FOUR_BIT_MASK);
        return new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(hash));
    }
}
