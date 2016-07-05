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
package org.onosproject.driver.pipeline;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.packet.PacketPriority;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Driver for software switch emulation of the OFDPA 2.0 pipeline.
 * The software switch is the CPqD OF 1.3 switch. Unfortunately the CPqD switch
 * does not handle vlan tags and mpls labels simultaneously, which requires us
 * to do some workarounds in the driver. This driver is meant for the use of
 * the cpqd switch when MPLS is required. As a result this driver works only
 * on incoming untagged packets.
 */
public class CpqdOfdpa2Pipeline extends Ofdpa2Pipeline {

    private final Logger log = getLogger(getClass());

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.deviceId = deviceId;

        // Initialize OFDPA group handler
        groupHandler = new CpqdOfdpa2GroupHandler();
        groupHandler.init(deviceId, context);

        serviceDirectory = context.directory();
        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();
        deviceService = serviceDirectory.get(DeviceService.class);

        driverId = coreService.registerApplication(
                "org.onosproject.driver.CpqdOfdpa2Pipeline");

        initializePipeline();
    }

    /*
     * CPQD emulation does not require special untagged packet handling, unlike
     * the real ofdpa.
     */
    @Override
    protected void processFilter(FilteringObjective filt,
                                 boolean install, ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
        PortCriterion portCriterion = null;
        EthCriterion ethCriterion = null;
        VlanIdCriterion vidCriterion = null;
        Collection<IPCriterion> ips = new ArrayList<IPCriterion>();
        if (!filt.key().equals(Criteria.dummy()) &&
                filt.key().type() == Criterion.Type.IN_PORT) {
            portCriterion = (PortCriterion) filt.key();
        } else {
            log.warn("No key defined in filtering objective from app: {}. Not"
                    + "processing filtering objective", applicationId);
            fail(filt, ObjectiveError.UNKNOWN);
            return;
        }
        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion criterion : filt.conditions()) {
            if (criterion.type() == Criterion.Type.ETH_DST ||
                    criterion.type() == Criterion.Type.ETH_DST_MASKED) {
                ethCriterion = (EthCriterion) criterion;
            } else if (criterion.type() == Criterion.Type.VLAN_VID) {
                vidCriterion = (VlanIdCriterion) criterion;
            } else if (criterion.type() == Criterion.Type.IPV4_DST) {
                ips.add((IPCriterion) criterion);
            } else {
                log.error("Unsupported filter {}", criterion);
                fail(filt, ObjectiveError.UNSUPPORTED);
                return;
            }
        }

        VlanId assignedVlan = null;
        // For VLAN cross-connect packets, use the configured VLAN
        if (vidCriterion != null) {
            if (vidCriterion.vlanId() != VlanId.NONE) {
                assignedVlan = vidCriterion.vlanId();

            // For untagged packets, assign a VLAN ID
            } else {
                if (filt.meta() == null) {
                    log.error("Missing metadata in filtering objective required " +
                            "for vlan assignment in dev {}", deviceId);
                    fail(filt, ObjectiveError.BADPARAMS);
                    return;
                }
                for (Instruction i : filt.meta().allInstructions()) {
                    if (i instanceof ModVlanIdInstruction) {
                        assignedVlan = ((ModVlanIdInstruction) i).vlanId();
                    }
                }
                if (assignedVlan == null) {
                    log.error("Driver requires an assigned vlan-id to tag incoming "
                            + "untagged packets. Not processing vlan filters on "
                            + "device {}", deviceId);
                    fail(filt, ObjectiveError.BADPARAMS);
                    return;
                }
            }
        }

        if (ethCriterion == null || ethCriterion.mac().equals(MacAddress.NONE)) {
            log.debug("filtering objective missing dstMac, cannot program TMAC table");
        } else {
            for (FlowRule tmacRule : processEthDstFilter(portCriterion, ethCriterion,
                                                         vidCriterion, assignedVlan,
                                                         applicationId)) {
                log.debug("adding MAC filtering rules in TMAC table: {} for dev: {}",
                          tmacRule, deviceId);
                ops = install ? ops.add(tmacRule) : ops.remove(tmacRule);
            }
        }

        if (ethCriterion == null || vidCriterion == null) {
            log.debug("filtering objective missing dstMac or VLAN, "
                    + "cannot program VLAN Table");
        } else {
            List<FlowRule> allRules = processVlanIdFilter(
                    portCriterion, vidCriterion, assignedVlan, applicationId);
            for (FlowRule rule : allRules) {
                log.debug("adding VLAN filtering rule in VLAN table: {} for dev: {}",
                        rule, deviceId);
                ops = install ? ops.add(rule) : ops.remove(rule);
            }
        }

        for (IPCriterion ipaddr : ips) {
            // since we ignore port information for IP rules, and the same (gateway) IP
            // can be configured on multiple ports, we make sure that we send
            // only a single rule to the switch.
            if (!sentIpFilters.contains(ipaddr)) {
                sentIpFilters.add(ipaddr);
                log.debug("adding IP filtering rules in ACL table {} for dev: {}",
                          ipaddr, deviceId);
                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
                selector.matchEthType(Ethernet.TYPE_IPV4);
                selector.matchIPDst(ipaddr.ip());
                treatment.setOutput(PortNumber.CONTROLLER);
                FlowRule rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(HIGHEST_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(ACL_TABLE).build();
                ops = install ? ops.add(rule) : ops.remove(rule);
            }
        }

        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Applied {} filtering rules in device {}",
                         ops.stages().get(0).size(), deviceId);
                pass(filt);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to apply all filtering rules in dev {}", deviceId);
                fail(filt, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));

    }

    /*
     * Cpqd emulation does not require the non-OF standard rules for
     * matching untagged packets.
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

            // Emulating OFDPA behavior by popping off internal assigned VLAN
            // before sending to controller
            TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_ARP)
                    .matchVlanId(assignedVlan);
            TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder()
                    .popVlan()
                    .punt();
            FlowRule internalVlan = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(sbuilder.build())
                    .withTreatment(tbuilder.build())
                    .withPriority(PacketPriority.CONTROL.priorityValue() + 1)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(ACL_TABLE).build();
            rules.add(internalVlan);
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
            // for unicast IP packets
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(pnum);
            selector.matchVlanId(vidCriterion.vlanId());
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchEthDst(ethCriterion.mac());
            /*
             * Note: CpqD switches do not handle MPLS-related operation properly
             * for a packet with VLAN tag. We pop VLAN here as a workaround.
             * Side effect: HostService learns redundant hosts with same MAC but
             * different VLAN. No known side effect on the network reachability.
             */
            treatment.popVlan();
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
            //for MPLS packets
            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(pnum);
            selector.matchVlanId(vidCriterion.vlanId());
            selector.matchEthType(Ethernet.MPLS_UNICAST);
            selector.matchEthDst(ethCriterion.mac());
            // workaround here again
            treatment.popVlan();
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
        /*
         * Note: CpqD switches do not handle MPLS-related operation properly
         * for a packet with VLAN tag. We pop VLAN here as a workaround.
         * Side effect: HostService learns redundant hosts with same MAC but
         * different VLAN. No known side effect on the network reachability.
         */
        treatment.popVlan();
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
                (ethType.ethType().toShort() != Ethernet.MPLS_UNICAST)) {
            log.warn("processSpecific: Unsupported forwarding objective criteria"
                    + "ethType:{} in dev:{}", ethType, deviceId);
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }
        boolean defaultRule = false;
        int forTableId = -1;
        TrafficSelector.Builder filteredSelector = DefaultTrafficSelector.builder();
        TrafficSelector.Builder complementarySelector = DefaultTrafficSelector.builder();

        /*
         * NOTE: The switch does not support matching 0.0.0.0/0.
         * Split it into 0.0.0.0/1 and 128.0.0.0/1
         */
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
                if (ipv4Dst.prefixLength() > 0) {
                    filteredSelector.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(ipv4Dst);
                } else {
                    filteredSelector.matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPDst(IpPrefix.valueOf("0.0.0.0/1"));
                    complementarySelector.matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPDst(IpPrefix.valueOf("128.0.0.0/1"));
                    defaultRule = true;
                }
                forTableId = UNICAST_ROUTING_TABLE;
                log.debug("processing IPv4 unicast specific forwarding objective {} -> next:{}"
                        + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
            }
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
                    log.warn("The group left!");
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
            FlowRule.Builder rule = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(complementarySelector.build())
                .withTreatment(tb.build())
                .forTable(forTableId);
            if (fwd.permanent()) {
                rule.makePermanent();
            } else {
                rule.makeTemporary(fwd.timeout());
            }
            flowRuleCollection.add(rule.build());
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
        if (!ethCriterion.mac().equals(MacAddress.NONE)) {
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
                        ttBuilder.add(o);
                    } else {
                        log.warn("Only allowed treatments in versatile forwarding "
                                + "objectives are punts to the controller");
                    }
                } else {
                    log.warn("Cannot process instruction in versatile fwd {}", ins);
                }
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
        processPortTable();
        // vlan table processing not required, as default is to drop packets
        // which can be accomplished without a table-miss-entry.
        processTmacTable();
        processIpTable();
        processMulticastIpTable();
        processMplsTable();
        processBridgingTable();
        processAclTable();
    }

    protected void processPortTable() {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(VLAN_TABLE);
        FlowRule tmisse = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(PORT_TABLE).build();
        ops = ops.add(tmisse);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized port table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize port table");
            }
        }));
    }

    protected void processTmacTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        treatment.transition(BRIDGING_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        ops =  ops.add(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized tmac table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize tmac table");
            }
        }));
    }

    protected void processIpTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(ACL_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(UNICAST_ROUTING_TABLE).build();
        ops =  ops.add(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized IP table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize unicast IP table");
            }
        }));
    }

    protected void processMulticastIpTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(ACL_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(MULTICAST_ROUTING_TABLE).build();
        ops =  ops.add(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized multicast IP table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize multicast IP table");
            }
        }));
    }

    protected void processMplsTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        treatment.transition(MPLS_TABLE_1);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(MPLS_TABLE_0).build();
        ops =  ops.add(rule);

        treatment.transition(ACL_TABLE);
        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(MPLS_TABLE_1).build();
        ops = ops.add(rule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized MPLS tables");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize MPLS tables");
            }
        }));
    }

    private void processBridgingTable() {
        //table miss entry
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        treatment.transition(ACL_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(BRIDGING_TABLE).build();
        ops =  ops.add(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized Bridging table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize Bridging table");
            }
        }));
    }

    protected void processAclTable() {
        //table miss entry - catch all to executed action-set
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(LOWEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(ACL_TABLE).build();
        ops =  ops.add(rule);
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized Acl table");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to initialize Acl table");
            }
        }));
    }

}
