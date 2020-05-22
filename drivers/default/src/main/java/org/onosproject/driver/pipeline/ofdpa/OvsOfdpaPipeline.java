/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.driver.extensions.OfdpaMatchActsetOutput;
import org.onosproject.net.Host;
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
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.Instructions.NoActionInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
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
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketPriority;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Optional;

import static org.onlab.packet.MacAddress.BROADCAST;
import static org.onlab.packet.MacAddress.NONE;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.*;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.*;
import static org.onosproject.net.flow.criteria.Criterion.Type.VLAN_VID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver for Open vSwitch emulation of the OFDPA pipeline.
 */
public class OvsOfdpaPipeline extends Ofdpa2Pipeline {

    private final Logger log = getLogger(getClass());

    private static final int EGRESS_VLAN_FLOW_TABLE_IN_INGRESS = 31;
    private static final int UNICAST_ROUTING_TABLE_1 = 32;
    /**
     * Table that determines whether VLAN is popped before punting to controller.
     * <p>
     * This is a non-OFDPA table to emulate OFDPA packet in behavior.
     * VLAN will be popped before punting if the VLAN is internally assigned.
     */
    public static final int PUNT_TABLE = 63;

    /**
     * A static indirect group that pop vlan and punt to controller.
     * <p>
     * The purpose of using a group instead of immediate action is that this
     * won't affect another copy on the data plane when write action exists.
     */
    public static final int POP_VLAN_PUNT_GROUP_ID = 0xd0000000;

    /**
     * Executor for group checker thread that checks pop vlan punt group.
     */
    private ScheduledExecutorService groupChecker;

    /**
     * Queue for passing pop vlan punt group flow rules to the GroupChecker thread.
     */
    private Queue<FlowRule> flowRuleQueue;

    /**
     * Lock used in synchronizing driver thread with groupCheckerThread.
     */
    private ReentrantLock groupCheckerLock;

    @Override
    protected boolean requireVlanExtensions() {
        return false;
    }

    @Override
    protected boolean requireEthType() {
        return false;
    }

    @Override
    public boolean requireSecondVlanTableEntry() {
        return false;
    }

    @Override
    protected void initDriverId() {
        driverId = coreService.registerApplication(
                "org.onosproject.driver.OvsOfdpaPipeline");
    }

    @Override
    protected void initGroupHander(PipelinerContext context) {
        // Terminate internal references
        // We are terminating the references here
        // because when the device is offline the apps
        // are still sending flowobjectives
        if (groupHandler != null) {
            groupHandler.terminate();
        }
        groupHandler = new OvsOfdpaGroupHandler();
        groupHandler.init(deviceId, context);
    }

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        // Terminate internal references
        // We are terminating the references here
        // because when the device is offline the apps
        // are still sending flowobjectives
        if (groupChecker != null) {
            groupChecker.shutdown();
        }
        // create a new executor at each init and a new empty queue
        groupChecker = Executors.newSingleThreadScheduledExecutor(groupedThreads("onos/driver",
                "ovs-ofdpa-%d", log));
        if (flowRuleQueue != null) {
            flowRuleQueue.clear();
        }
        flowRuleQueue = new ConcurrentLinkedQueue<>();
        groupCheckerLock = new ReentrantLock();
        groupChecker.scheduleAtFixedRate(new PopVlanPuntGroupChecker(), 20, 50, TimeUnit.MILLISECONDS);
        super.init(deviceId, context);
    }

    protected void processFilter(FilteringObjective filteringObjective,
                                 boolean install,
                                 ApplicationId applicationId) {
        if (OfdpaPipelineUtility.isDoubleTagged(filteringObjective)) {
            processDoubleTaggedFilter(filteringObjective, install, applicationId);
        } else {
            // If it is not a double-tagged filter, we fall back
            // to the OFDPA 2.0 pipeline.
            super.processFilter(filteringObjective, install, applicationId);
        }
    }

    /**
     * Determines if the forwarding objective will be used for double-tagged packets.
     *
     * @param fwd Forwarding objective
     * @return True if the objective was created for double-tagged packets, false otherwise.
     */
    private boolean isDoubleTagged(ForwardingObjective fwd) {
        if (fwd.nextId() != null) {
            NextGroup next = getGroupForNextObjective(fwd.nextId());
            if (next != null) {
                List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
                // we only need the top level group's key
                Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
                if (group != null) {
                    int groupId = group.id().id();
                    if (((groupId & ~TYPE_MASK) == L3_UNICAST_TYPE) &&
                            ((groupId & TYPE_L3UG_DOUBLE_VLAN_MASK) == TYPE_L3UG_DOUBLE_VLAN_MASK)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Configure filtering rules of outer and inner VLAN IDs, and a MAC address.
     * Filtering happens in three tables (VLAN_TABLE, VLAN_1_TABLE, TMAC_TABLE).
     *
     * @param filteringObjective the filtering objective
     * @param install            true to add, false to remove
     * @param applicationId      for application programming this filter
     */
    private void processDoubleTaggedFilter(FilteringObjective filteringObjective,
                                           boolean install,
                                           ApplicationId applicationId) {
        PortCriterion portCriterion = null;
        EthCriterion ethCriterion = null;
        VlanIdCriterion innervidCriterion = null;
        VlanIdCriterion outerVidCriterion = null;
        boolean popVlan = false;
        TrafficTreatment meta = filteringObjective.meta();
        if (!filteringObjective.key().equals(Criteria.dummy()) &&
                filteringObjective.key().type() == Criterion.Type.IN_PORT) {
            portCriterion = (PortCriterion) filteringObjective.key();
        }
        if (portCriterion == null) {
            log.warn("No IN_PORT defined in filtering objective from app: {}" +
                             "Failed to program VLAN tables.", applicationId);
            return;
        } else {
            log.debug("Received filtering objective for dev/port: {}/{}", deviceId,
                      portCriterion.port());
        }

        // meta should have only one instruction, popVlan.
        if (meta != null && meta.allInstructions().size() == 1) {
            L2ModificationInstruction l2Inst = (L2ModificationInstruction) meta.allInstructions().get(0);
            if (l2Inst.subtype().equals(L2ModificationInstruction.L2SubType.VLAN_POP)) {
                popVlan = true;
            } else {
                log.warn("Filtering objective can have only VLAN_POP instruction.");
                return;
            }
        } else {
            log.warn("Filtering objective should have one instruction.");
            return;
        }

        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion criterion : filteringObjective.conditions()) {
            switch (criterion.type()) {
                case ETH_DST:
                case ETH_DST_MASKED:
                    ethCriterion = (EthCriterion) criterion;
                    break;
                case VLAN_VID:
                    outerVidCriterion = (VlanIdCriterion) criterion;
                    break;
                case INNER_VLAN_VID:
                    innervidCriterion = (VlanIdCriterion) criterion;
                    break;
                default:
                    log.warn("Unsupported filter {}", criterion);
                    fail(filteringObjective, ObjectiveError.UNSUPPORTED);
                    return;
            }
        }

        if (innervidCriterion == null || outerVidCriterion == null) {
            log.warn("filtering objective should have two vidCriterion.");
            return;
        }

        if (ethCriterion == null || ethCriterion.mac().equals(NONE)) {
            // NOTE: it is possible that a filtering objective only has vidCriterion
            log.warn("filtering objective missing dstMac, cannot program TMAC table");
            return;
        } else {
            MacAddress unicastMac = readEthDstFromTreatment(filteringObjective.meta());
            List<List<FlowRule>> allStages = processEthDstFilter(portCriterion, ethCriterion, innervidCriterion,
                                                                 innervidCriterion.vlanId(), unicastMac,
                                                                 applicationId);
            for (List<FlowRule> flowRules : allStages) {
                log.trace("Starting a new flow rule stage for TMAC table flow");
                ops.newStage();

                for (FlowRule flowRule : flowRules) {
                    log.trace("{} flow rules in TMAC table: {} for dev: {}",
                              (install) ? "adding" : "removing", flowRules, deviceId);
                    if (install) {
                        ops = ops.add(flowRule);
                    } else {
                        // NOTE: Only remove TMAC flow when there is no more enabled port within the
                        // same VLAN on this device if TMAC doesn't support matching on in_port.
                        if (matchInPortTmacTable()
                                || (filteringObjective.meta() != null
                                && filteringObjective.meta().clearedDeferred())) {
                            ops = ops.remove(flowRule);
                        } else {
                            log.debug("Abort TMAC flow removal on {}. Some other ports still share this TMAC flow");
                        }
                    }
                }
            }
        }

        List<FlowRule> rules;
        rules = processDoubleVlanIdFilter(portCriterion, innervidCriterion,
                                          outerVidCriterion, popVlan, applicationId);
        for (FlowRule flowRule : rules) {
            log.trace("{} flow rule in VLAN table: {} for dev: {}",
                      (install) ? "adding" : "removing", flowRule, deviceId);
            ops = install ? ops.add(flowRule) : ops.remove(flowRule);
        }

        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Applied {} filtering rules in device {}",
                          ops.stages().get(0).size(), deviceId);
                pass(filteringObjective);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.info("Failed to apply all filtering rules in dev {}", deviceId);
                fail(filteringObjective, ObjectiveError.FLOWINSTALLATIONFAILED);
            }
        }));

    }
    /**
     * Internal implementation of processDoubleVlanIdFilter.
     *
     * @param portCriterion       port on device for which this filter is programmed
     * @param innerVidCriterion   inner vlan
     * @param outerVidCriterion   outer vlan
     * @param popVlan             true if outer vlan header needs to be removed
     * @param applicationId       for application programming this filter
     * @return flow rules for port-vlan filters
     */
    private List<FlowRule> processDoubleVlanIdFilter(PortCriterion portCriterion,
                                                     VlanIdCriterion innerVidCriterion,
                                                     VlanIdCriterion outerVidCriterion,
                                                     boolean popVlan,
                                                     ApplicationId applicationId) {
        List<FlowRule> rules = new ArrayList<>();
        TrafficSelector.Builder outerSelector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder outerTreatment = DefaultTrafficTreatment.builder();
        TrafficSelector.Builder innerSelector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder innerTreatment = DefaultTrafficTreatment.builder();

        VlanId outerVlanId = outerVidCriterion.vlanId();
        VlanId innerVlanId = innerVidCriterion.vlanId();
        PortNumber portNumber = portCriterion.port();
        // Check arguments
        if (PortNumber.ALL.equals(portNumber)
                || outerVlanId.equals(VlanId.NONE)
                || innerVlanId.equals(VlanId.NONE)) {
            log.warn("Incomplete Filtering Objective. " +
                             "VLAN Table cannot be programmed for {}", deviceId);
            return ImmutableList.of();
        } else {
            outerSelector.matchInPort(portNumber);
            innerSelector.matchInPort(portNumber);
            outerTreatment.transition(VLAN_1_TABLE);
            innerTreatment.transition(TMAC_TABLE);
            outerTreatment.writeMetadata(outerVlanId.toShort(), 0xFFF);

            outerSelector.matchVlanId(outerVlanId);
            innerSelector.matchVlanId(innerVlanId);
            //force recompilation
            //FIXME might be issue due tu /fff mask
            innerSelector.matchMetadata(outerVlanId.toShort());

            if (popVlan) {
                outerTreatment.popVlan();
            }
        }

        // NOTE: for double-tagged packets, restore original outer vlan
        // before sending it to the controller.
        GroupKey groupKey = popVlanPuntGroupKey();
        Group group = groupService.getGroup(deviceId, groupKey);
        if (group != null) {
            // push outer vlan and send to controller
            TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder()
                    .matchInPort(portNumber)
                    .matchVlanId(innerVlanId);
            Host host = handler().get(HostService.class).getConnectedHosts(ConnectPoint.
                    deviceConnectPoint(deviceId + "/" + portNumber.toLong())).stream().filter(h ->
                    h.vlan().equals(outerVlanId)).findFirst().orElse(null);
            EthType vlanType = EthType.EtherType.VLAN.ethType();
            if (host != null) {
                vlanType = host.tpid();
            }
            TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder()
                    .pushVlan(vlanType).setVlanId(outerVlanId).punt();

            rules.add(DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(sbuilder.build())
                    .withTreatment(tbuilder.build())
                    .withPriority(PacketPriority.CONTROL.priorityValue())
                    .fromApp(driverId)
                    .makePermanent()
                    .forTable(PUNT_TABLE).build());
        } else {
            log.info("popVlanPuntGroup not found in dev:{}", deviceId);
            return Collections.emptyList();
        }

        FlowRule outerRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(outerSelector.build())
                .withTreatment(outerTreatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(VLAN_TABLE)
                .build();
        FlowRule innerRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(innerSelector.build())
                .withTreatment(innerTreatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(VLAN_1_TABLE)
                .build();
        rules.add(outerRule);
        rules.add(innerRule);

        return rules;
    }

    /**
     * In the OF-DPA 2.0 pipeline, egress forwarding objectives go to the
     * egress tables.
     * @param fwd  the forwarding objective of type 'egress'
     * @return     a collection of flow rules to be sent to the switch. An empty
     *             collection may be returned if there is a problem in processing
     *             the flow rule
     */
    @Override
    protected Collection<FlowRule> processEgress(ForwardingObjective fwd) {
        log.debug("Processing egress forwarding objective:{} in dev:{}",
                  fwd, deviceId);

        List<FlowRule> rules = new ArrayList<>();

        // Build selector
        TrafficSelector.Builder sb = DefaultTrafficSelector.builder();
        VlanIdCriterion vlanIdCriterion = (VlanIdCriterion) fwd.selector().getCriterion(Criterion.Type.VLAN_VID);
        if (vlanIdCriterion == null) {
            log.error("Egress forwarding objective:{} must include vlanId", fwd.id());
            fail(fwd, ObjectiveError.BADPARAMS);
            return rules;
        }

        Optional<Instruction> outInstr = fwd.treatment().allInstructions().stream()
                .filter(instruction -> instruction instanceof Instructions.OutputInstruction).findFirst();
        if (!outInstr.isPresent()) {
            log.error("Egress forwarding objective:{} must include output port", fwd.id());
            fail(fwd, ObjectiveError.BADPARAMS);
            return rules;
        }

        PortNumber portNumber = ((Instructions.OutputInstruction) outInstr.get()).port();

        sb.matchVlanId(vlanIdCriterion.vlanId());
        OfdpaMatchActsetOutput actsetOutput = new OfdpaMatchActsetOutput(portNumber);
        sb.extension(actsetOutput, deviceId);

        // Build a flow rule for Egress VLAN Flow table
        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();
        tb.transition(UNICAST_ROUTING_TABLE_1);
        if (fwd.treatment() != null) {
            for (Instruction instr : fwd.treatment().allInstructions()) {
                if (instr instanceof L2ModificationInstruction &&
                        ((L2ModificationInstruction) instr).subtype() ==
                                L2ModificationInstruction.L2SubType.VLAN_ID) {
                    tb.immediate().add(instr);
                }
                if (instr instanceof L2ModificationInstruction &&
                        ((L2ModificationInstruction) instr).subtype() ==
                                L2ModificationInstruction.L2SubType.VLAN_PUSH) {
                    EthType ethType = ((L2ModificationInstruction.ModVlanHeaderInstruction) instr).ethernetType();
                    tb.immediate().pushVlan(ethType);
                }
            }
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(sb.build())
                .withTreatment(tb.build())
                .makePermanent()
                .forTable(EGRESS_VLAN_FLOW_TABLE_IN_INGRESS);
        rules.add(ruleBuilder.build());
        return rules;
    }

    /**
     * Handles forwarding rules to the IP Unicast Routing.
     *
     * @param fwd the forwarding objective
     * @return A collection of flow rules, or an empty set
     */
    protected Collection<FlowRule> processDoubleTaggedFwd(ForwardingObjective fwd) {
        // inner for UNICAST_ROUTING_TABLE_1, outer for UNICAST_ROUTING_TABLE
        TrafficSelector selector = fwd.selector();
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder innerTtb = DefaultTrafficTreatment.builder();
        TrafficTreatment.Builder outerTtb = DefaultTrafficTreatment.builder();

        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);

        if (ethType.ethType().toShort() == Ethernet.TYPE_IPV4) {
            sBuilder.matchEthType(Ethernet.TYPE_IPV4);
            sBuilder.matchVlanId(VlanId.ANY);
            IpPrefix ipv4Dst = ((IPCriterion) selector.getCriterion(Criterion.Type.IPV4_DST)).ip();
            if (!ipv4Dst.isMulticast() && ipv4Dst.prefixLength() == 32) {
                sBuilder.matchIPDst(ipv4Dst);
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
                        outerTtb.immediate().setVlanId(extractDummyVlanIdFromGroupId(group.id().id()));
                        //ACTSET_OUTPUT in OVS will match output action in write_action() set.
                        outerTtb.deferred().setOutput(extractOutputPortFromGroupId(group.id().id()));
                        outerTtb.transition(EGRESS_VLAN_FLOW_TABLE_IN_INGRESS);
                        innerTtb.deferred().group(group.id());
                        innerTtb.transition(ACL_TABLE);

                        FlowRule.Builder innerRuleBuilder = DefaultFlowRule.builder()
                                .fromApp(fwd.appId())
                                .withPriority(fwd.priority())
                                .forDevice(deviceId)
                                .withSelector(sBuilder.build())
                                .withTreatment(innerTtb.build())
                                .forTable(UNICAST_ROUTING_TABLE_1);
                        if (fwd.permanent()) {
                            innerRuleBuilder.makePermanent();
                        } else {
                            innerRuleBuilder.makeTemporary(fwd.timeout());
                        }
                        Collection<FlowRule> flowRuleCollection = new HashSet<>();
                        flowRuleCollection.add(innerRuleBuilder.build());

                        FlowRule.Builder outerRuleBuilder = DefaultFlowRule.builder()
                                .fromApp(fwd.appId())
                                .withPriority(fwd.priority())
                                .forDevice(deviceId)
                                .withSelector(sBuilder.build())
                                .withTreatment(outerTtb.build())
                                .forTable(UNICAST_ROUTING_TABLE);
                        if (fwd.permanent()) {
                            outerRuleBuilder.makePermanent();
                        } else {
                            outerRuleBuilder.makeTemporary(fwd.timeout());
                        }
                        flowRuleCollection.add(innerRuleBuilder.build());
                        flowRuleCollection.add(outerRuleBuilder.build());
                        return flowRuleCollection;
                    } else {
                        log.warn("Cannot find group for nextId:{} in dev:{}. Aborting fwd:{}",
                                 fwd.nextId(), deviceId, fwd.id());
                        fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
                        return Collections.emptySet();
                    }
                } else {
                    log.warn("NextId is not specified in fwd:{}", fwd.id());
                    fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
                    return Collections.emptySet();
                }
            }
        }
        return Collections.emptySet();
    }

    private static VlanId extractDummyVlanIdFromGroupId(int groupId) {
        short vlanId = (short) ((groupId & 0x7FF8000) >> 15);
        return VlanId.vlanId(vlanId);
    }

    private static PortNumber extractOutputPortFromGroupId(int groupId) {
        return PortNumber.portNumber(groupId & 0x7FFF);
    }

    /*
     * Open vSwitch emulation does not require the non OF-standard rules for
     * matching untagged packets that ofdpa uses.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#processVlanIdFilter
     */
    @Override
    protected List<List<FlowRule>> processVlanIdFilter(PortCriterion portCriterion,
                                                       VlanIdCriterion vidCriterion,
                                                       VlanId assignedVlan,
                                                       ApplicationId applicationId,
                                                       boolean install) {
        List<FlowRule> rules = new ArrayList<>();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchVlanId(vidCriterion.vlanId());
        treatment.transition(TMAC_TABLE);

        if (vidCriterion.vlanId() == VlanId.NONE) {
            // untagged packets are assigned vlans
            treatment.pushVlan().setVlanId(assignedVlan);
        } else if (!vidCriterion.vlanId().equals(assignedVlan)) {
            // Rewrite with assigned vlans
            treatment.setVlanId(assignedVlan);
        }

        // ofdpa cannot match on ALL portnumber, so we need to use separate
        // rules for each port.
        List<PortNumber> portnums = new ArrayList<>();
        if (portCriterion != null) {
            if (portCriterion.port() == PortNumber.ALL) {
                for (Port port : deviceService.getPorts(deviceId)) {
                    if (port.number().toLong() > 0 && port.number().toLong() < OFPP_MAX) {
                        portnums.add(port.number());
                    }
                }
            } else {
                portnums.add(portCriterion.port());
            }
        }

        for (PortNumber pnum : portnums) {
            // NOTE: Emulating OFDPA behavior by popping off internal assigned
            //       VLAN before sending to controller
            if (vidCriterion.vlanId() == VlanId.NONE) {
                try {
                    groupCheckerLock.lock();
                    if (flowRuleQueue == null) {
                        // this means that the group has been created
                        // and that groupChecker has destroyed the queue
                        log.debug("Installing punt table rule for untagged port {} and vlan {}.",
                                  pnum, assignedVlan);
                        rules.add(buildPuntTableRule(pnum, assignedVlan));
                    } else {
                        // The VLAN punt group may be held back due to device initial audit.
                        // In that case, we queue all punt table flow until the group has been created.
                        log.debug("popVlanPuntGroup not found in dev:{}, queueing this flow rule.", deviceId);
                        flowRuleQueue.add(buildPuntTableRule(pnum, assignedVlan));
                    }
                } finally {
                    groupCheckerLock.unlock();
                }
            } else if (vidCriterion.vlanId() != VlanId.NONE) {
                // for tagged ports just forward to the controller
                log.debug("Installing punt rule for tagged port {} and vlan {}.", pnum, vidCriterion.vlanId());
                rules.add(buildPuntTableRuleTagged(pnum, vidCriterion.vlanId()));
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

        return ImmutableList.of(rules);
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
     * Creates punt table entry that matches IN_PORT and VLAN_VID and forwards
     * packet to controller tagged.
     *
     * @param portNumber port number
     * @param packetVlan vlan tag of the packet
     * @return punt table flow rule
     */
    private FlowRule buildPuntTableRuleTagged(PortNumber portNumber, VlanId packetVlan) {
        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder()
                .matchInPort(portNumber)
                .matchVlanId(packetVlan);
        TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder().punt();

        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(sbuilder.build())
                .withTreatment(tbuilder.build())
                .withPriority(PacketPriority.CONTROL.priorityValue())
                .fromApp(driverId)
                .makePermanent()
                .forTable(PUNT_TABLE).build();
    }

    @Override
    protected List<List<FlowRule>> processEthDstFilter(PortCriterion portCriterion,
                                                 EthCriterion ethCriterion,
                                                 VlanIdCriterion vidCriterion,
                                                 VlanId assignedVlan,
                                                 MacAddress unicastMac,
                                                 ApplicationId applicationId) {
        // Consider PortNumber.ANY as wildcard. Match ETH_DST only
        if (portCriterion != null && portCriterion.port() == PortNumber.ANY) {
            return processEthDstOnlyFilter(ethCriterion, applicationId);
        }

        // Multicast MAC
        if (ethCriterion.mask() != null) {
            return processMcastEthDstFilter(ethCriterion, assignedVlan, unicastMac, applicationId);
        }

        //handling untagged packets via assigned VLAN
        if (vidCriterion.vlanId() == VlanId.NONE) {
            vidCriterion = (VlanIdCriterion) Criteria.matchVlanId(assignedVlan);
        }
        // ofdpa cannot match on ALL portnumber, so we need to use separate
        // rules for each port.
        List<PortNumber> portnums = new ArrayList<>();
        if (portCriterion != null) {
            if (portCriterion.port() == PortNumber.ALL) {
                for (Port port : deviceService.getPorts(deviceId)) {
                    if (port.number().toLong() > 0 && port.number().toLong() < OFPP_MAX) {
                        portnums.add(port.number());
                    }
                }
            } else {
                portnums.add(portCriterion.port());
            }
        }

        List<FlowRule> rules = new ArrayList<>();
        for (PortNumber pnum : portnums) {
            // TMAC rules for unicast IP packets
            TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            selector.matchInPort(pnum);
            selector.matchVlanId(vidCriterion.vlanId());
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchEthDst(ethCriterion.mac());
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
        return ImmutableList.of(rules);
    }

    @Override
    protected List<List<FlowRule>> processEthDstOnlyFilter(EthCriterion ethCriterion,
                                                     ApplicationId applicationId) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchEthDst(ethCriterion.mac());
        treatment.transition(UNICAST_ROUTING_TABLE);
        FlowRule rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        return ImmutableList.of(ImmutableList.of(rule));
    }

    /*
     * Open vSwitch emulation allows MPLS ECMP.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#processEthTypeSpecific
     */
    @Override
    protected Collection<FlowRule> processEthTypeSpecific(ForwardingObjective fwd) {
        if (isDoubleTagged(fwd)) {
            return processDoubleTaggedFwd(fwd);
        }
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
        int forTableId;
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
            IpPrefix ipv6Dst = ((IPCriterion) selector.getCriterion(Criterion.Type.IPV6_DST)).ip();
            if (ipv6Dst.isMulticast()) {
                if (ipv6Dst.prefixLength() != IpAddress.INET6_BIT_LENGTH) {
                    log.debug("Multicast specific IPv6 forwarding objective can only be /128");
                    fail(fwd, ObjectiveError.BADPARAMS);
                    return ImmutableSet.of();
                }
                VlanId assignedVlan = readVlanFromSelector(fwd.meta());
                if (assignedVlan == null) {
                    log.debug("VLAN ID required by multicast specific fwd obj is missing. Abort.");
                    fail(fwd, ObjectiveError.BADPARAMS);
                    return ImmutableSet.of();
                }
                filteredSelector.matchVlanId(assignedVlan);
                filteredSelector.matchEthType(Ethernet.TYPE_IPV6).matchIPv6Dst(ipv6Dst);
                forTableId = MULTICAST_ROUTING_TABLE;
                log.debug("processing IPv6 multicast specific forwarding objective {} -> next:{}"
                        + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
            } else {
                if (buildIpv6Selector(filteredSelector, fwd) < 0) {
                    return Collections.emptyList();
                }
                forTableId = UNICAST_ROUTING_TABLE;
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
                if (i instanceof L3ModificationInstruction) {
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
                .getCriterion(VLAN_VID);

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

    @Override
    protected TrafficTreatment.Builder versatileTreatmentBuilder(ForwardingObjective fwd) {
        // XXX driver does not currently do type checking as per Tables 65-67 in
        // OFDPA 2.0 spec. The only allowed treatment is a punt to the controller.
        TrafficTreatment.Builder ttBuilder = DefaultTrafficTreatment.builder();
        if (fwd.treatment() != null) {
            for (Instruction ins : fwd.treatment().allInstructions()) {
                if (ins instanceof OutputInstruction) {
                    OutputInstruction o = (OutputInstruction) ins;
                    if (PortNumber.CONTROLLER.equals(o.port())) {
                        ttBuilder.transition(PUNT_TABLE);
                    } else {
                        log.warn("Only allowed treatments in versatile forwarding "
                                + "objectives are punts to the controller");
                    }
                } else if (ins instanceof NoActionInstruction) {
                    // No action is allowed and nothing needs to be done
                } else {
                    log.warn("Cannot process instruction in versatile fwd {}", ins);
                }
            }
            if (fwd.treatment().clearedDeferred()) {
                ttBuilder.wipeDeferred();
            }
        }
        if (fwd.nextId() != null) {
            // Override case
            NextGroup next = getGroupForNextObjective(fwd.nextId());
            if (next == null) {
                fail(fwd, ObjectiveError.BADPARAMS);
                return null;
            }
            List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
            // we only need the top level group's key to point the flow to it
            Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
            if (group == null) {
                log.warn("Group with key:{} for next-id:{} not found in dev:{}",
                        gkeys.get(0).peekFirst(), fwd.nextId(), deviceId);
                fail(fwd, ObjectiveError.GROUPMISSING);
                return null;
            }
            ttBuilder.deferred().group(group.id());
        }
        return ttBuilder;
    }

    /*
     * Open vSwitch emulation requires table-miss-entries in forwarding tables.
     * Real OFDPA does not require these rules as they are put in by default.
     *
     * (non-Javadoc)
     * @see org.onosproject.driver.pipeline.OFDPA2Pipeline#initializePipeline()
     */
    @Override
    protected void initializePipeline() {
        initTableMiss(PORT_TABLE, VLAN_TABLE, null);
        initTableMiss(VLAN_TABLE, ACL_TABLE, null);
        initTableMiss(VLAN_1_TABLE, ACL_TABLE, null);
        initTableMiss(TMAC_TABLE, BRIDGING_TABLE, null);
        initTableMiss(UNICAST_ROUTING_TABLE, ACL_TABLE, null);
        initTableMiss(MULTICAST_ROUTING_TABLE, ACL_TABLE, null);
        initTableMiss(EGRESS_VLAN_FLOW_TABLE_IN_INGRESS, ACL_TABLE, null);
        initTableMiss(UNICAST_ROUTING_TABLE_1, ACL_TABLE, null);
        initTableMiss(MPLS_TABLE_0, MPLS_TABLE_1, null);
        initTableMiss(MPLS_TABLE_1, ACL_TABLE, null);
        initTableMiss(BRIDGING_TABLE, ACL_TABLE, null);
        initTableMiss(ACL_TABLE, -1, null);
        initPuntTable();

        initPopVlanPuntGroup();
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
     * Install lldp/bbdp matching rules at table PUNT_TABLE
     * that forward traffic to controller.
     *
     */
    private void initPuntTable() {
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        TrafficTreatment treatment =  DefaultTrafficTreatment.builder().punt().build();

        // Add punt rule for LLDP and BDDP
        TrafficSelector.Builder lldpSelector = DefaultTrafficSelector.builder();
        lldpSelector.matchEthType(EthType.EtherType.LLDP.ethType().toShort());
        FlowRule lldpRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(lldpSelector.build())
                .withTreatment(treatment)
                .withPriority(HIGHEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(PUNT_TABLE).build();
        ops =  ops.add(lldpRule);

        TrafficSelector.Builder bbdpSelector = DefaultTrafficSelector.builder();
        bbdpSelector.matchEthType(EthType.EtherType.BDDP.ethType().toShort());
        FlowRule bbdpRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(bbdpSelector.build())
                .withTreatment(treatment)
                .withPriority(HIGHEST_PRIORITY)
                .fromApp(driverId)
                .makePermanent()
                .forTable(PUNT_TABLE).build();
        ops.add(bbdpRule);

        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.info("Initialized table {} on {}", PUNT_TABLE, deviceId);
            }
            @Override
            public void onError(FlowRuleOperations ops) {
                log.warn("Failed to initialize table {} on {}", PUNT_TABLE, deviceId);
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
        int hash = POP_VLAN_PUNT_GROUP_ID | (Objects.hash(deviceId) & FOUR_NIBBLE_MASK);
        return new DefaultGroupKey(Ofdpa2Pipeline.appKryo.serialize(hash));
    }

    private class PopVlanPuntGroupChecker implements Runnable {
        @Override
        public void run() {
            try {
                groupCheckerLock.lock();
                // this can happen outside of the lock but I think it is safer
                // to include it here.
                Group group = groupService.getGroup(deviceId, popVlanPuntGroupKey());
                if (group != null) {
                    log.debug("PopVlanPuntGroupChecker: Installing {} missing rules at punt table.",
                              flowRuleQueue.size());

                    // if we have pending flow rules install them
                    if (flowRuleQueue.size() > 0) {
                        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
                        // we should not care about the context here, it can only be add
                        // since when removing the rules the group should be there already.
                        flowRuleQueue.forEach(ops::add);
                        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
                            @Override
                            public void onSuccess(FlowRuleOperations ops) {
                                log.debug("Applied {} pop vlan punt rules in device {}",
                                          ops.stages().get(0).size(), deviceId);
                            }

                            @Override
                            public void onError(FlowRuleOperations ops) {
                                log.error("Failed to apply all pop vlan punt rules in dev {}", deviceId);
                            }
                        }));
                    }
                    // this signifies that the group is created and now
                    // flow rules can be installed directly
                    flowRuleQueue = null;
                    // Schedule with an initial delay the miss table flow rule installation
                    // the delay is to make sure the queued flows are all installed before
                    // pushing the table miss flow rule
                    // TODO it can be further optimized by using context and completable future
                    groupChecker.schedule(new TableMissFlowInstaller(), 5000, TimeUnit.MILLISECONDS);
                }
            } finally {
                groupCheckerLock.unlock();
            }
        }
    }

    private class TableMissFlowInstaller implements Runnable {
        @Override
        public void run() {
            // Add table miss flow rule
            FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
            TrafficSelector.Builder defaultSelector = DefaultTrafficSelector.builder();
            TrafficTreatment treatment =  DefaultTrafficTreatment.builder().punt().build();
            FlowRule defaultRule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(defaultSelector.build())
                    .withTreatment(treatment)
                    .withPriority(LOWEST_PRIORITY)
                    .fromApp(driverId)
                    .makePermanent()
                    .forTable(PUNT_TABLE).build();
            ops.add(defaultRule);
            flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    log.info("Initialized table miss flow rule {} on {}", PUNT_TABLE, deviceId);
                }
                @Override
                public void onError(FlowRuleOperations ops) {
                    log.warn("Failed to initialize table miss flow rule {} on {}", PUNT_TABLE, deviceId);
                }
            }));
            // shutdown the group checker gracefully
            groupChecker.shutdown();
        }
    }
}
