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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.EthType;
import org.onlab.packet.Ethernet;
import org.onlab.packet.EthType.EtherType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.AbstractAccumulator;
import org.onlab.util.Accumulator;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.driver.extensions.Ofdpa3CopyField;
import org.onosproject.driver.extensions.Ofdpa3MplsType;
import org.onosproject.driver.extensions.Ofdpa3SetMplsType;
import org.onosproject.driver.extensions.OfdpaMatchActsetOutput;
import org.onosproject.driver.extensions.OfdpaMatchAllowVlanTranslation;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
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
import org.onosproject.net.flow.criteria.Icmpv6CodeCriterion;
import org.onosproject.net.flow.criteria.Icmpv6TypeCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.TcpPortCriterion;
import org.onosproject.net.flow.criteria.UdpPortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.Instructions.NoActionInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsHeaderInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction.L3SubType;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveStore;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.packet.MacAddress.*;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.driver.extensions.Ofdpa3CopyField.OXM_ID_PACKET_REG_1;
import static org.onosproject.driver.extensions.Ofdpa3CopyField.OXM_ID_VLAN_VID;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaGroupHandlerUtility.*;
import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.*;
import static org.onosproject.net.flowobjective.ForwardingObjective.Flag.SPECIFIC;
import static org.onosproject.net.group.GroupDescription.Type.SELECT;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.net.flow.criteria.Criterion.Type.MPLS_BOS;

/**
 * Driver for Broadcom's OF-DPA v2.0 TTP.
 */
public class Ofdpa2Pipeline extends AbstractHandlerBehaviour implements Pipeliner {
    // Timer for the accumulator
    private static final Timer TIMER = new Timer("fwdobj-batching");
    private Accumulator<Pair<ForwardingObjective, Collection<FlowRule>>> accumulator;
    // Internal objects
    private final Logger log = getLogger(getClass());
    protected ServiceDirectory serviceDirectory;
    protected FlowRuleService flowRuleService;
    protected CoreService coreService;
    protected GroupService groupService;
    protected FlowObjectiveStore flowObjectiveStore;
    protected DeviceId deviceId;
    protected ApplicationId driverId;
    protected DeviceService deviceService;
    protected static KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(KryoNamespaces.API)
            .register(GroupKey.class)
            .register(DefaultGroupKey.class)
            .register(OfdpaNextGroup.class)
            .register(ArrayDeque.class)
            .build("Ofdpa2Pipeline");

    protected Ofdpa2GroupHandler groupHandler;

    // flows installations to be retried
    private ScheduledExecutorService retryExecutorService
        = newScheduledThreadPool(5, groupedThreads("OfdpaPipeliner", "retry-%d", log));

    // accumulator executor service
    private ScheduledExecutorService accumulatorExecutorService
        = newSingleThreadScheduledExecutor(groupedThreads("OfdpaPipeliner", "acc-%d", log));

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        this.deviceId = deviceId;

        serviceDirectory = context.directory();
        coreService = serviceDirectory.get(CoreService.class);
        flowRuleService = serviceDirectory.get(FlowRuleService.class);
        groupService = serviceDirectory.get(GroupService.class);
        flowObjectiveStore = context.store();
        deviceService = serviceDirectory.get(DeviceService.class);
        // Init the accumulator, if enabled
        if (isAccumulatorEnabled(this)) {
            accumulator = new ForwardingObjectiveAccumulator(context.accumulatorMaxObjectives(),
                    context.accumulatorMaxBatchMillis(),
                    context.accumulatorMaxIdleMillis());
        }

        initDriverId();
        initGroupHander(context);

        initializePipeline();
    }

    void setupAccumulatorForTests(int maxFwd, int maxBatchMS, int maxIdleMS) {
        if (accumulator == null) {
            accumulator = new ForwardingObjectiveAccumulator(maxFwd,
                                                             maxBatchMS,
                                                             maxIdleMS);
        }
    }

    protected void initDriverId() {
        driverId = coreService.registerApplication(
                "org.onosproject.driver.Ofdpa2Pipeline");
    }

    protected void initGroupHander(PipelinerContext context) {
        // Terminate internal references
        // We are terminating the references here
        // because when the device is offline the apps
        // are still sending flowobjectives
        if (groupHandler != null) {
            groupHandler.terminate();
        }
        groupHandler = new Ofdpa2GroupHandler();
        groupHandler.init(deviceId, context);
    }

    protected void initializePipeline() {
        // OF-DPA does not require initializing the pipeline as it puts default
        // rules automatically in the hardware. However emulation of OFDPA in
        // software switches does require table-miss-entries.
    }

    /**
     * Determines whether this pipeline requires MPLS POP instruction.
     *
     * @return true to use MPLS POP instruction
     */
    public boolean requireMplsPop() {
        return true;
    }

    /**
     * Determines whether this pipeline requires one additional flow matching on ethType 0x86dd in ACL table.
     *
     * @return true to create one additional flow matching on ethType 0x86dd in ACL table
     */
    protected boolean requireEthType() {
        return true;
    }

    /**
     * Determines whether this pipeline requires MPLS BOS match.
     *
     * @return true to use MPLS BOS match
     */
    public boolean requireMplsBosMatch() {
        return true;
    }

    /**
     * Determines whether this pipeline requires MPLS TTL decrement and copy.
     *
     * @return true to use MPLS TTL decrement and copy
     */
    public boolean requireMplsTtlModification() {
        return true;
    }

    /**
     * Determines whether this pipeline requires OFDPA match and set VLAN extensions.
     *
     * @return true to use the extensions
     */
    protected boolean requireVlanExtensions() {
        return true;
    }

    /**
     * Determines whether this pipeline requires second VLAN entry in VLAN table.
     * OF-DPA hardware requires one VLAN filtering rule and one VLAN assignment
     * flow in the VLAN table in the case of untagged packets. Software emulations
     * just use one flow.
     *
     * @return true if required
     */
    public boolean requireSecondVlanTableEntry() {
        return true;
    }

    /**
     * Determines whether in-port should be matched on in TMAC table rules.
     *
     * @return true if match on in-port should be programmed
     */
    protected boolean matchInPortTmacTable() {
        return true;
    }

    /**
     * Determines whether matching L4 destination port on IPv6 packets is supported in ACL table.
     *
     * @return true if matching L4 destination port on IPv6 packets is supported in ACL table.
     */
    protected boolean supportIpv6L4Dst() {
        return true;
    }

    /**
     * Determines whether this driver should continue to retry flows that point
     * to empty groups. See CORD-554.
     *
     * @return true if the driver should retry flows
     */
    protected boolean shouldRetry() {
        return true;
    }

    /**
     * Determines whether this driver requires unicast flow to be installed before multicast flow
     * in TMAC table.
     *
     * @return true if required
     */
    protected boolean requireUnicastBeforeMulticast() {
        return false;
    }

    /**
     * Determines whether this driver supports installing a clearDeferred action on table 30.
     *
     * @return true if required
     */
    protected boolean supportsUnicastBlackHole() {
        return true;
    }

    protected boolean requirePuntTable() {
        return false;
    }

    //////////////////////////////////////
    //  Flow Objectives
    //////////////////////////////////////

    @Override
    public void filter(FilteringObjective filteringObjective) {
        if (filteringObjective.type() == FilteringObjective.Type.PERMIT) {
            processFilter(filteringObjective,
                          filteringObjective.op() == Objective.Operation.ADD,
                          filteringObjective.appId());
        } else {
            // Note that packets that don't match the PERMIT filter are
            // automatically denied. The DENY filter is used to deny packets
            // that are otherwise permitted by the PERMIT filter.
            // Use ACL table flow rules here for DENY filtering objectives
            log.warn("filter objective other than PERMIT currently not supported");
            fail(filteringObjective, ObjectiveError.UNSUPPORTED);
        }
    }

    @Override
    public void forward(ForwardingObjective fwd) {
        Collection<FlowRule> rules = processForward(fwd);
        if (rules == null || rules.isEmpty()) {
            // Assumes fail message has already been generated to the objective
            // context. Returning here prevents spurious pass message to be
            // generated by FlowRule service for empty flowOps.
            return;
        }
        // Let's accumulate flow rules if accumulator is active and fwd objective is not versatile.
        // Otherwise send directly, without adding futher delay
        if (accumulator != null && Objects.equals(fwd.flag(), SPECIFIC)) {
            accumulator.add(Pair.of(fwd, rules));
        } else {
            sendForwards(Collections.singletonList(Pair.of(fwd, rules)));
        }
    }

    // Builds the batch using the accumulated flow rules
    private void sendForwards(List<Pair<ForwardingObjective, Collection<FlowRule>>> pairs) {
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();
        log.debug("Sending {} fwd-objs", pairs.size());
        List<Objective> fwdObjs = Lists.newArrayList();
        // Iterates over all accumulated flow rules and then build an unique batch
        pairs.forEach(pair -> {
            ForwardingObjective fwd = pair.getLeft();
            Collection<FlowRule> rules = pair.getRight();
            switch (fwd.op()) {
                case ADD:
                    rules.stream()
                            .filter(Objects::nonNull)
                            .forEach(flowOpsBuilder::add);
                    log.debug("Applying a add fwd-obj {} to sw:{}", fwd.id(), deviceId);
                    fwdObjs.add(fwd);
                    break;
                case REMOVE:
                    rules.stream()
                            .filter(Objects::nonNull)
                            .forEach(flowOpsBuilder::remove);
                    log.debug("Deleting a flow rule to sw:{}", deviceId);
                    fwdObjs.add(fwd);
                    break;
                default:
                    fail(fwd, ObjectiveError.UNKNOWN);
                    log.warn("Unknown forwarding type {}", fwd.op());
            }
        });
        // Finally applies the operations
        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {

            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.trace("Flow rule operations onSuccess {}", ops);
                fwdObjs.forEach(OfdpaPipelineUtility::pass);
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                ObjectiveError error = ObjectiveError.FLOWINSTALLATIONFAILED;
                log.warn("Flow rule operations onError {}. Reason = {}", ops, error);
                fwdObjs.forEach(fwdObj -> fail(fwdObj, error));
            }
        }));
    }

    @Override
    public void next(NextObjective nextObjective) {
        NextGroup nextGroup = flowObjectiveStore.getNextGroup(nextObjective.id());
        switch (nextObjective.op()) {
        case ADD:
            if (nextGroup != null) {
                log.warn("Cannot add next {} that already exists in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id {} in dev {} - add group",
                      nextObjective.id(), deviceId);
            groupHandler.addGroup(nextObjective);
            break;
        case ADD_TO_EXISTING:
            if (nextGroup != null) {
                log.debug("Processing NextObjective id {} in dev {} - add bucket",
                          nextObjective.id(), deviceId);
                groupHandler.addBucketToGroup(nextObjective, nextGroup);
            } else {
                // it is possible that group-chain has not been fully created yet
                log.debug("Waiting to add bucket to group for next-id:{} in dev:{}",
                          nextObjective.id(), deviceId);

                // by design multiple pending bucket is allowed for the group
                groupHandler.pendingBuckets.compute(nextObjective.id(), (nextId, pendBkts) -> {
                    if (pendBkts == null) {
                        pendBkts = Sets.newHashSet();
                    }
                    pendBkts.add(nextObjective);
                    return pendBkts;
                });
            }
            break;
        case REMOVE:
            if (nextGroup == null) {
                log.warn("Cannot remove next {} that does not exist in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id {}  in dev {} - remove group",
                      nextObjective.id(), deviceId);
            groupHandler.removeGroup(nextObjective, nextGroup);
            break;
        case REMOVE_FROM_EXISTING:
            if (nextGroup == null) {
                log.warn("Cannot remove from next {} that does not exist in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id {} in dev {} - remove bucket",
                      nextObjective.id(), deviceId);
            groupHandler.removeBucketFromGroup(nextObjective, nextGroup);
            break;
        case MODIFY:
            if (nextGroup == null) {
                log.warn("Cannot modify next {} that does not exist in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id {} in dev {} group {} - modify bucket",
                      nextObjective.id(), deviceId, nextGroup);
            groupHandler.modifyBucketFromGroup(nextObjective, nextGroup);
            break;
        case VERIFY:
            if (nextGroup == null) {
                log.warn("Cannot verify next {} that does not exist in device {}",
                         nextObjective.id(), deviceId);
                return;
            }
            log.debug("Processing NextObjective id {} in dev {} - verify",
                      nextObjective.id(), deviceId);
            groupHandler.verifyGroup(nextObjective, nextGroup);
            break;
        default:
            log.warn("Unsupported operation {}", nextObjective.op());
        }
    }

    @Override
    public void purgeAll(ApplicationId appId) {
        flowRuleService.purgeFlowRules(deviceId, appId);
        groupService.purgeGroupEntries(deviceId, appId);
    }

    //////////////////////////////////////
    //  Flow handling
    //////////////////////////////////////

    /**
     * As per OFDPA 2.0 TTP, filtering of VLAN ids and MAC addresses (for routing)
     * configured on switch ports happen in different tables.
     *
     * @param filt      the filtering objective
     * @param install   indicates whether to add or remove the objective
     * @param applicationId     the application that sent this objective
     */
    protected void processFilter(FilteringObjective filt,
                                 boolean install, ApplicationId applicationId) {
        // This driver only processes filtering criteria defined with switch
        // ports as the key
        PortCriterion portCriterion = null;
        EthCriterion ethCriterion = null;
        VlanIdCriterion vidCriterion = null;
        if (!filt.key().equals(Criteria.dummy()) &&
                filt.key().type() == Criterion.Type.IN_PORT) {
            portCriterion = (PortCriterion) filt.key();
        }
        if (portCriterion == null) {
            log.debug("No IN_PORT defined in filtering objective from app: {}",
                    applicationId);
        } else {
            log.debug("Received filtering objective for dev/port: {}/{}", deviceId,
                    portCriterion.port());
        }
        // convert filtering conditions for switch-intfs into flowrules
        FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
        for (Criterion criterion : filt.conditions()) {
            switch (criterion.type()) {
                case ETH_DST:
                case ETH_DST_MASKED:
                    ethCriterion = (EthCriterion) criterion;
                    break;
                case VLAN_VID:
                    vidCriterion = (VlanIdCriterion) criterion;
                    break;
                default:
                    log.warn("Unsupported filter {}", criterion);
                    fail(filt, ObjectiveError.UNSUPPORTED);
                    return;
            }
        }

        VlanId assignedVlan = null;
        if (vidCriterion != null) {
            // Use the VLAN in criterion if metadata is not present and the traffic is tagged
            if (!vidCriterion.vlanId().equals(VlanId.NONE)) {
                assignedVlan = vidCriterion.vlanId();
            }
            // If the meta VLAN is present let's update the assigned vlan
            if (filt.meta() != null) {
                VlanId metaVlan = readVlanFromTreatment(filt.meta());
                if (metaVlan != null) {
                    assignedVlan = metaVlan;
                }
            }

            if (assignedVlan == null) {
                log.error("Driver fails to extract VLAN information. "
                        + "Not processing VLAN filters on device {}.", deviceId);
                log.debug("VLAN ID in criterion={}, metadata={}",
                        readVlanFromTreatment(filt.meta()), vidCriterion.vlanId());
                fail(filt, ObjectiveError.BADPARAMS);
                return;
            }
        }

        if (ethCriterion == null || ethCriterion.mac().equals(NONE)) {
            // NOTE: it is possible that a filtering objective only has vidCriterion
            log.debug("filtering objective missing dstMac, won't program TMAC table");
        } else {
            MacAddress unicastMac = readEthDstFromTreatment(filt.meta());
            List<List<FlowRule>> allStages = processEthDstFilter(portCriterion, ethCriterion,
                    vidCriterion, assignedVlan, unicastMac, applicationId);
            for (List<FlowRule> flowRules : allStages) {
                log.trace("Starting a new flow rule stage for TMAC table flow");
                ops.newStage();

                for (FlowRule flowRule : flowRules) {
                    log.trace("{} flow rule in TMAC table: {} for dev: {}",
                            (install) ? "adding" : "removing", flowRule, deviceId);
                    if (install) {
                        ops = ops.add(flowRule);
                    } else {
                        // NOTE: Only remove TMAC flow when there is no more enabled port within the
                        // same VLAN on this device if TMAC doesn't support matching on in_port.
                        if (filt.meta() != null && filt.meta().clearedDeferred()) {
                            // TMac mcast does not match on the input port - we have to remove it
                            // only if this is the last port
                            FlowRule rule = buildTmacRuleForMcastFromUnicast(flowRule, applicationId);
                            // IPv6 or IPv4 tmac rule
                            if (rule != null) {
                                // Add first the mcast rule and then open a new stage for the unicast
                                ops = ops.remove(rule);
                                ops.newStage();
                            }
                            ops = ops.remove(flowRule);
                        } else if (matchInPortTmacTable()) {
                            ops = ops.remove(flowRule);
                        } else {
                            log.debug("Abort TMAC flow removal on {}. " +
                                              "Some other ports still share this TMAC flow", deviceId);
                        }
                    }
                }
            }
        }

        if (vidCriterion == null) {
            // NOTE: it is possible that a filtering objective only has ethCriterion
            log.info("filtering objective missing VLAN, cannot program VLAN Table");
        } else {
            List<List<FlowRule>> allStages = processVlanIdFilter(
                    portCriterion, vidCriterion, assignedVlan, applicationId, install);
            for (List<FlowRule> flowRules : allStages) {
                log.trace("Starting a new flow rule stage for VLAN table flow");
                ops.newStage();

                for (FlowRule flowRule : flowRules) {
                    log.trace("{} flow rules in VLAN table: {} for dev: {}",
                            (install) ? "adding" : "removing", flowRule, deviceId);
                    ops = install ? ops.add(flowRule) : ops.remove(flowRule);
                }
            }
        }

        // apply filtering flow rules
        flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Applied {} filtering rules in device {}",
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

    /**
     * Internal implementation of processVlanIdFilter.
     * <p>
     * The is_present bit in set_vlan_vid action is required to be 0 in OFDPA i12.
     * Since it is non-OF spec, we need an extension treatment for that.
     * The useVlanExtension must be set to false for OFDPA i12.
     * </p>
     * <p>
     * NOTE: Separate VLAN filtering rules and assignment rules
     * into different stages in order to guarantee that filtering rules
     * always go first, as required by OFDPA.
     * </p>
     *
     * @param portCriterion       port on device for which this filter is programmed
     * @param vidCriterion        vlan assigned to port, or NONE for untagged
     * @param assignedVlan        assigned vlan-id for untagged packets
     * @param applicationId       for application programming this filter
     * @param install   indicates whether to add or remove the objective
     * @return stages of flow rules for port-vlan filters
     */
    protected List<List<FlowRule>> processVlanIdFilter(PortCriterion portCriterion,
                                                       VlanIdCriterion vidCriterion,
                                                       VlanId assignedVlan,
                                                       ApplicationId applicationId,
                                                       boolean install) {
        List<FlowRule> filteringRules = new ArrayList<>();
        List<FlowRule> assignmentRules = new ArrayList<>();
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        TrafficSelector.Builder preSelector = null;
        TrafficTreatment.Builder preTreatment = null;

        treatment.transition(TMAC_TABLE);

        if (vidCriterion.vlanId() == VlanId.NONE) {
            // untagged packets are assigned vlans
            preSelector = DefaultTrafficSelector.builder();
            if (requireVlanExtensions()) {
                OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(VlanId.NONE);
                selector.extension(ofdpaMatchVlanVid, deviceId);
                OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(assignedVlan);
                treatment.extension(ofdpaSetVlanVid, deviceId);

                OfdpaMatchVlanVid preOfdpaMatchVlanVid = new OfdpaMatchVlanVid(assignedVlan);
                preSelector.extension(preOfdpaMatchVlanVid, deviceId);
            } else {
                selector.matchVlanId(VlanId.NONE);
                treatment.setVlanId(assignedVlan);

                preSelector.matchVlanId(assignedVlan);
            }
            preTreatment = DefaultTrafficTreatment.builder().transition(TMAC_TABLE);
        } else {
            if (requireVlanExtensions()) {
                OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(vidCriterion.vlanId());
                selector.extension(ofdpaMatchVlanVid, deviceId);
            } else {
                selector.matchVlanId(vidCriterion.vlanId());
            }

            if (!assignedVlan.equals(vidCriterion.vlanId())) {
                if (requireVlanExtensions()) {
                    OfdpaSetVlanVid ofdpaSetVlanVid = new OfdpaSetVlanVid(assignedVlan);
                    treatment.extension(ofdpaSetVlanVid, deviceId);
                } else {
                    treatment.setVlanId(assignedVlan);
                }
            }
        }

        // ofdpa cannot match on ALL portnumber, so we need to use separate
        // rules for each port.
        List<PortNumber> portnums = new ArrayList<>();
        if (portCriterion != null) {
            if (PortNumber.ALL.equals(portCriterion.port())) {
                for (Port port : deviceService.getPorts(deviceId)) {
                    if (port.number().toLong() > 0 && port.number().toLong() < OFPP_MAX) {
                        portnums.add(port.number());
                    }
                }
            } else {
                portnums.add(portCriterion.port());
            }
        } else {
            log.warn("Filtering Objective missing Port Criterion . " +
                    "VLAN Table cannot be programmed for {}", deviceId);
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
            assignmentRules.add(rule);

            if (preSelector != null) {
                preSelector.matchInPort(pnum);
                FlowRule preRule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(preSelector.build())
                        .withTreatment(preTreatment.build())
                        .withPriority(DEFAULT_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(VLAN_TABLE).build();
                filteringRules.add(preRule);
            }
        }
        return install ? ImmutableList.of(filteringRules, assignmentRules) :
                ImmutableList.of(assignmentRules, filteringRules);
    }

    /**
     * Allows routed packets with correct destination MAC to be directed
     * to unicast routing table, multicast routing table or MPLS forwarding table.
     *
     * @param portCriterion  port on device for which this filter is programmed
     * @param ethCriterion   dstMac of device for which is filter is programmed
     * @param vidCriterion   vlan assigned to port, or NONE for untagged
     * @param assignedVlan   assigned vlan-id for untagged packets
     * @param unicastMac     some switches require a unicast TMAC flow to be programmed before multicast
     *                       TMAC flow. This MAC address will be used for the unicast TMAC flow.
     *                       This is unused if the filtering objective is a unicast.
     * @param applicationId  for application programming this filter
     * @return stages of flow rules for port-vlan filters

     */
    protected List<List<FlowRule>> processEthDstFilter(PortCriterion portCriterion,
                                                 EthCriterion ethCriterion,
                                                 VlanIdCriterion vidCriterion,
                                                 VlanId assignedVlan,
                                                 MacAddress unicastMac,
                                                 ApplicationId applicationId) {
        // Consider PortNumber.ANY as wildcard. Match ETH_DST only
        if (portCriterion != null && PortNumber.ANY.equals(portCriterion.port())) {
            return processEthDstOnlyFilter(ethCriterion, applicationId);
        }

        // Multicast MAC
        if (ethCriterion.mask() != null) {
            return processMcastEthDstFilter(ethCriterion, assignedVlan, unicastMac, applicationId);
        }

        //handling untagged packets via assigned VLAN
        if (vidCriterion != null && vidCriterion.vlanId() == VlanId.NONE) {
            vidCriterion = (VlanIdCriterion) Criteria.matchVlanId(assignedVlan);
        }
        List<FlowRule> rules = new ArrayList<>();
        OfdpaMatchVlanVid ofdpaMatchVlanVid = null;
        if (vidCriterion != null && requireVlanExtensions()) {
            ofdpaMatchVlanVid = new OfdpaMatchVlanVid(vidCriterion.vlanId());
        }
        // ofdpa cannot match on ALL portnumber, so we need to use separate
        // rules for each port.
        List<PortNumber> portnums = new ArrayList<>();
        if (portCriterion != null) {
            if (PortNumber.ALL.equals(portCriterion.port())) {
                for (Port port : deviceService.getPorts(deviceId)) {
                    if (port.number().toLong() > 0 && port.number().toLong() < OFPP_MAX) {
                        portnums.add(port.number());
                    }
                }
            } else {
                portnums.add(portCriterion.port());
            }
            for (PortNumber pnum : portnums) {
                rules.add(buildTmacRuleForIpv4(ethCriterion,
                        vidCriterion,
                        ofdpaMatchVlanVid,
                        applicationId,
                        pnum));
                rules.add(buildTmacRuleForMpls(ethCriterion,
                        vidCriterion,
                        ofdpaMatchVlanVid,
                        applicationId,
                        pnum));
                rules.add(buildTmacRuleForIpv6(ethCriterion,
                        vidCriterion,
                        ofdpaMatchVlanVid,
                        applicationId,
                        pnum));
            }
        } else {
            rules.add(buildTmacRuleForIpv4(ethCriterion,
                    vidCriterion,
                    ofdpaMatchVlanVid,
                    applicationId,
                    null));
            rules.add(buildTmacRuleForMpls(ethCriterion,
                    vidCriterion,
                    ofdpaMatchVlanVid,
                    applicationId,
                    null));
            rules.add(buildTmacRuleForIpv6(ethCriterion,
                    vidCriterion,
                    ofdpaMatchVlanVid,
                    applicationId,
                    null));
        }
        return ImmutableList.of(rules);
    }

    /**
     * Builds TMAC rules for IPv4 packets.
     *
     * @param ethCriterion dst mac matching
     * @param vidCriterion vlan id assigned to the port
     * @param ofdpaMatchVlanVid OFDPA vlan id matching
     * @param applicationId application id
     * @param pnum port number
     * @return TMAC rule for IPV4 packets
     */
    private FlowRule buildTmacRuleForIpv4(EthCriterion ethCriterion,
                                          VlanIdCriterion vidCriterion,
                                          OfdpaMatchVlanVid ofdpaMatchVlanVid,
                                          ApplicationId applicationId,
                                          PortNumber pnum) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        if (pnum != null) {
            if (matchInPortTmacTable()) {
                selector.matchInPort(pnum);
            } else {
                log.debug("Pipeline does not support IN_PORT matching in TMAC table, " +
                        "ignoring the IN_PORT criteria");
            }
        }
        if (vidCriterion != null) {
            if (requireVlanExtensions()) {
                selector.extension(ofdpaMatchVlanVid, deviceId);
            } else {
                selector.matchVlanId(vidCriterion.vlanId());
            }
        }
        selector.matchEthType(Ethernet.TYPE_IPV4);
        selector.matchEthDst(ethCriterion.mac());
        treatment.transition(UNICAST_ROUTING_TABLE);
        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
    }

    /**
     * Builds TMAC rule for MPLS packets.
     *
     * @param ethCriterion dst mac matching
     * @param vidCriterion vlan id assigned to the port
     * @param ofdpaMatchVlanVid OFDPA vlan id matching
     * @param applicationId application id
     * @param pnum port number
     * @return TMAC rule for MPLS packets
     */
    private FlowRule buildTmacRuleForMpls(EthCriterion ethCriterion,
                                          VlanIdCriterion vidCriterion,
                                          OfdpaMatchVlanVid ofdpaMatchVlanVid,
                                          ApplicationId applicationId,
                                          PortNumber pnum) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        if (pnum != null) {
            if (matchInPortTmacTable()) {
                selector.matchInPort(pnum);
            } else {
                log.debug("Pipeline does not support IN_PORT matching in TMAC table, " +
                        "ignoring the IN_PORT criteria");
            }
        }
        if (vidCriterion != null) {
            if (requireVlanExtensions()) {
                selector.extension(ofdpaMatchVlanVid, deviceId);
            } else {
                selector.matchVlanId(vidCriterion.vlanId());
            }
        }
        selector.matchEthType(Ethernet.MPLS_UNICAST);
        selector.matchEthDst(ethCriterion.mac());
        treatment.transition(MPLS_TABLE_0);
        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
    }

    /**
     * Builds TMAC rules for IPv6 packets.
     *
     * @param ethCriterion dst mac matching
     * @param vidCriterion vlan id assigned to the port
     * @param ofdpaMatchVlanVid OFDPA vlan id matching
     * @param applicationId application id
     * @param pnum port number
     * @return TMAC rule for IPV6 packets
     */
     private FlowRule buildTmacRuleForIpv6(EthCriterion ethCriterion,
                                          VlanIdCriterion vidCriterion,
                                          OfdpaMatchVlanVid ofdpaMatchVlanVid,
                                          ApplicationId applicationId,
                                          PortNumber pnum) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        if (pnum != null) {
            if (matchInPortTmacTable()) {
                selector.matchInPort(pnum);
            } else {
                log.debug("Pipeline does not support IN_PORT matching in TMAC table, " +
                        "ignoring the IN_PORT criteria");
            }
        }
         if (vidCriterion != null) {
            if (requireVlanExtensions()) {
                selector.extension(ofdpaMatchVlanVid, deviceId);
            } else {
                selector.matchVlanId(vidCriterion.vlanId());
            }
        }
        selector.matchEthType(Ethernet.TYPE_IPV6);
        selector.matchEthDst(ethCriterion.mac());
        treatment.transition(UNICAST_ROUTING_TABLE);
        return DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
    }

    protected List<List<FlowRule>> processEthDstOnlyFilter(EthCriterion ethCriterion,
                                                     ApplicationId applicationId) {
        ImmutableList.Builder<FlowRule> builder = ImmutableList.builder();

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
        builder.add(rule);

        selector = DefaultTrafficSelector.builder();
        treatment = DefaultTrafficTreatment.builder();
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
        return ImmutableList.of(builder.add(rule).build());
    }

    private FlowRule buildTmacRuleForMcastFromUnicast(FlowRule tmacRuleForUnicast, ApplicationId applicationId) {
        final TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        final TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        FlowRule rule;
        // Build the selector
        for (Criterion criterion : tmacRuleForUnicast.selector().criteria()) {
            if (criterion instanceof VlanIdCriterion) {
                if (requireVlanExtensions()) {
                    OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(((VlanIdCriterion) criterion).vlanId());
                    selector.extension(ofdpaMatchVlanVid, deviceId);
                } else {
                    selector.add(criterion);
                }
            } else if (criterion instanceof EthTypeCriterion) {
                selector.add(criterion);
                if (Objects.equals(((EthTypeCriterion) criterion).ethType(),
                                   EtherType.IPV4.ethType())) {
                    selector.matchEthDstMasked(IPV4_MULTICAST, IPV4_MULTICAST_MASK);
                } else if (Objects.equals(((EthTypeCriterion) criterion).ethType(),
                                          EtherType.IPV6.ethType())) {
                    selector.matchEthDstMasked(IPV6_MULTICAST, IPV6_MULTICAST_MASK);
                } else {
                    // We don't need for mpls rules
                    return null;
                }
            }
        }
        // Build the treatment
        treatment.transition(MULTICAST_ROUTING_TABLE);
        // Build the flowrule
        rule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(TMAC_TABLE).build();
        log.debug("Building TMac Mcast flowRule {}", rule);
        return rule;
    }

    List<List<FlowRule>> processMcastEthDstFilter(EthCriterion ethCriterion,
                                                      VlanId assignedVlan,
                                                      MacAddress unicastMac,
                                                      ApplicationId applicationId) {
        ImmutableList.Builder<FlowRule> unicastFlows = ImmutableList.builder();
        ImmutableList.Builder<FlowRule> multicastFlows = ImmutableList.builder();
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;
        FlowRule rule;

        if (IPV4_MULTICAST.equals(ethCriterion.mac())) {
            if (requireUnicastBeforeMulticast()) {
                selector = DefaultTrafficSelector.builder();
                treatment = DefaultTrafficTreatment.builder();
                selector.matchEthType(Ethernet.TYPE_IPV4);
                selector.matchEthDst(unicastMac);
                selector.matchVlanId(assignedVlan);
                treatment.transition(UNICAST_ROUTING_TABLE);
                rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(DEFAULT_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(TMAC_TABLE).build();
                unicastFlows.add(rule);
            }

            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();
            selector.matchEthType(Ethernet.TYPE_IPV4);
            selector.matchEthDstMasked(ethCriterion.mac(), ethCriterion.mask());
            selector.matchVlanId(assignedVlan);
            treatment.transition(MULTICAST_ROUTING_TABLE);
            rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(TMAC_TABLE).build();
            multicastFlows.add(rule);
        }

        if (IPV6_MULTICAST.equals(ethCriterion.mac())) {
            if (requireUnicastBeforeMulticast()) {
                selector = DefaultTrafficSelector.builder();
                treatment = DefaultTrafficTreatment.builder();
                selector.matchEthType(Ethernet.TYPE_IPV6);
                selector.matchEthDst(unicastMac);
                selector.matchVlanId(assignedVlan);
                treatment.transition(UNICAST_ROUTING_TABLE);
                rule = DefaultFlowRule.builder()
                        .forDevice(deviceId)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withPriority(DEFAULT_PRIORITY)
                        .fromApp(applicationId)
                        .makePermanent()
                        .forTable(TMAC_TABLE).build();
                unicastFlows.add(rule);
            }

            selector = DefaultTrafficSelector.builder();
            treatment = DefaultTrafficTreatment.builder();
            selector.matchEthType(Ethernet.TYPE_IPV6);
            selector.matchEthDstMasked(ethCriterion.mac(), ethCriterion.mask());
            selector.matchVlanId(assignedVlan);
            treatment.transition(MULTICAST_ROUTING_TABLE);
            rule = DefaultFlowRule.builder()
                    .forDevice(deviceId)
                    .withSelector(selector.build())
                    .withTreatment(treatment.build())
                    .withPriority(DEFAULT_PRIORITY)
                    .fromApp(applicationId)
                    .makePermanent()
                    .forTable(TMAC_TABLE).build();
            multicastFlows.add(rule);
        }
        return ImmutableList.of(unicastFlows.build(), multicastFlows.build());
    }

    private Collection<FlowRule> processForward(ForwardingObjective fwd) {
        switch (fwd.flag()) {
            case SPECIFIC:
                return processSpecific(fwd);
            case VERSATILE:
                return processVersatile(fwd);
            case EGRESS:
                return processEgress(fwd);
            default:
                fail(fwd, ObjectiveError.UNKNOWN);
                log.warn("Unknown forwarding flag {}", fwd.flag());
        }
        return Collections.emptySet();
    }

    /**
     * In the OF-DPA 2.0 pipeline, egress forwarding objectives go to the
     * egress tables.
     * @param fwd  the forwarding objective of type 'egress'
     * @return     a collection of flow rules to be sent to the switch. An empty
     *             collection may be returned if there is a problem in processing
     *             the flow rule
     */
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
                           .filter(instruction -> instruction instanceof OutputInstruction).findFirst();
        if (!outInstr.isPresent()) {
            log.error("Egress forwarding objective:{} must include output port", fwd.id());
            fail(fwd, ObjectiveError.BADPARAMS);
            return rules;
        }

        PortNumber portNumber = ((OutputInstruction) outInstr.get()).port();

        sb.matchVlanId(vlanIdCriterion.vlanId());
        OfdpaMatchActsetOutput actsetOutput = new OfdpaMatchActsetOutput(portNumber);
        sb.extension(actsetOutput, deviceId);

        sb.extension(new OfdpaMatchAllowVlanTranslation(ALLOW_VLAN_TRANSLATION), deviceId);

        // Build a flow rule for Egress VLAN Flow table
        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();
        tb.transition(EGRESS_DSCP_PCP_REMARK_FLOW_TABLE);
        if (fwd.treatment() != null) {
            for (Instruction instr : fwd.treatment().allInstructions()) {
                if (instr instanceof L2ModificationInstruction &&
                        ((L2ModificationInstruction) instr).subtype() == L2SubType.VLAN_ID) {
                    tb.immediate().add(instr);
                }
                if (instr instanceof L2ModificationInstruction &&
                        ((L2ModificationInstruction) instr).subtype() == L2SubType.VLAN_PUSH) {
                    tb.immediate().pushVlan();
                    EthType ethType = ((L2ModificationInstruction.ModVlanHeaderInstruction) instr).ethernetType();
                    if (ethType.equals(EtherType.QINQ.ethType())) {
                        // Build a flow rule for Egress TPID Flow table
                        TrafficSelector tpidSelector = DefaultTrafficSelector.builder()
                                .extension(actsetOutput, deviceId)
                                .matchVlanId(VlanId.ANY).build();

                        TrafficTreatment tpidTreatment = DefaultTrafficTreatment.builder()
                                .extension(new Ofdpa3CopyField(COPY_FIELD_NBITS, COPY_FIELD_OFFSET,
                                                               COPY_FIELD_OFFSET, OXM_ID_VLAN_VID,
                                                               OXM_ID_PACKET_REG_1),
                                           deviceId)
                                .popVlan()
                                .pushVlan(EtherType.QINQ.ethType())
                                .extension(new Ofdpa3CopyField(COPY_FIELD_NBITS, COPY_FIELD_OFFSET,
                                                               COPY_FIELD_OFFSET, OXM_ID_PACKET_REG_1,
                                                               OXM_ID_VLAN_VID),
                                           deviceId)
                                .build();

                        FlowRule.Builder tpidRuleBuilder = DefaultFlowRule.builder()
                                .fromApp(fwd.appId())
                                .withPriority(fwd.priority())
                                .forDevice(deviceId)
                                .withSelector(tpidSelector)
                                .withTreatment(tpidTreatment)
                                .makePermanent()
                                .forTable(EGRESS_TPID_FLOW_TABLE);
                        rules.add(tpidRuleBuilder.build());
                    }
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
                .forTable(EGRESS_VLAN_FLOW_TABLE);
        rules.add(ruleBuilder.build());
        return rules;
    }

    /**
     * In the OF-DPA 2.0 pipeline, versatile forwarding objectives go to the
     * ACL table.
     * @param fwd  the forwarding objective of type 'versatile'
     * @return     a collection of flow rules to be sent to the switch. An empty
     *             collection may be returned if there is a problem in processing
     *             the flow rule
     */
    protected Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        log.debug("Processing versatile forwarding objective:{} in dev:{}",
                 fwd.id(), deviceId);
        List<FlowRule> flowRules = new ArrayList<>();
        final AtomicBoolean ethTypeUsed  = new AtomicBoolean(false);

        if (fwd.nextId() == null && fwd.treatment() == null) {
            log.error("Forwarding objective {} from {} must contain "
                    + "nextId or Treatment", fwd.selector(), fwd.appId());
            fail(fwd, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }

        TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
        fwd.selector().criteria().forEach(criterion -> {
            if (criterion instanceof VlanIdCriterion) {
                VlanId vlanId = ((VlanIdCriterion) criterion).vlanId();
                // ensure that match does not include vlan = NONE as OF-DPA does not
                // match untagged packets this way in the ACL table.
                if (vlanId.equals(VlanId.NONE)) {
                    return;
                }
                if (requireVlanExtensions()) {
                    OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(vlanId);
                    sbuilder.extension(ofdpaMatchVlanVid, deviceId);
                } else {
                    sbuilder.matchVlanId(vlanId);
                }
            } else if (criterion instanceof Icmpv6TypeCriterion) {
                byte icmpv6Type = (byte) ((Icmpv6TypeCriterion) criterion).icmpv6Type();
                sbuilder.matchIcmpv6Type(icmpv6Type);
            } else if (criterion instanceof Icmpv6CodeCriterion) {
                byte icmpv6Code = (byte) ((Icmpv6CodeCriterion) criterion).icmpv6Code();
                sbuilder.matchIcmpv6Type(icmpv6Code);
            } else if (criterion instanceof TcpPortCriterion || criterion instanceof UdpPortCriterion) {
                // FIXME: QMX switches do not support L4 dst port matching in ACL table.
                // Currently L4 dst port matching is only used by DHCP relay feature
                // and therefore is safe to be replaced with L4 src port matching.
                // We need to revisit this if L4 dst port is used for other purpose in the future.
                if (!supportIpv6L4Dst() && isIpv6(fwd.selector())) {
                    switch (criterion.type()) {
                        case UDP_DST:
                        case UDP_DST_MASKED:
                        case TCP_DST:
                        case TCP_DST_MASKED:
                            break;
                        default:
                            sbuilder.add(criterion);
                    }
                } else {
                    sbuilder.add(criterion);
                }
            } else if (criterion instanceof EthTypeCriterion) {
                sbuilder.add(criterion);
                ethTypeUsed.set(true);
            } else {
                sbuilder.add(criterion);
            }
        });

        TrafficTreatment.Builder ttBuilder = versatileTreatmentBuilder(fwd);
        if (ttBuilder == null) {
            return Collections.emptySet();
        }

        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(fwd.appId())
                .withPriority(fwd.priority())
                .forDevice(deviceId)
                .withSelector(sbuilder.build())
                .withTreatment(ttBuilder.build())
                .makePermanent()
                .forTable(ACL_TABLE);

        flowRules.add(ruleBuilder.build());

        if (!ethTypeUsed.get() && requireEthType()) {
            log.debug("{} doesn't match on ethType but requireEthType is true, adding complementary ACL flow.",
                      sbuilder.toString());
            sbuilder.matchEthType(Ethernet.TYPE_IPV6);
            FlowRule.Builder ethTypeRuleBuilder = DefaultFlowRule.builder()
                    .fromApp(fwd.appId())
                    .withPriority(fwd.priority())
                    .forDevice(deviceId)
                    .withSelector(sbuilder.build())
                    .withTreatment(ttBuilder.build())
                    .makePermanent()
                    .forTable(ACL_TABLE);
            flowRules.add(ethTypeRuleBuilder.build());
        }
        return flowRules;
    }

    /**
     * Helper function to create traffic treatment builder for versatile forwarding objectives.
     *
     * @param fwd original forwarding objective
     * @return treatment builder for the flow rule, or null if there is an error.
     */
    protected TrafficTreatment.Builder versatileTreatmentBuilder(ForwardingObjective fwd) {
        // XXX driver does not currently do type checking as per Tables 65-67 in
        // OFDPA 2.0 spec. The only allowed treatment is a punt to the controller.
        TrafficTreatment.Builder ttBuilder = DefaultTrafficTreatment.builder();
        if (fwd.treatment() != null) {
            for (Instruction ins : fwd.treatment().allInstructions()) {
                if (ins instanceof OutputInstruction) {
                    OutputInstruction o = (OutputInstruction) ins;
                    if (PortNumber.CONTROLLER.equals(o.port())) {
                        ttBuilder.add(o);
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

    /**
     * In the OF-DPA 2.0 pipeline, specific forwarding refers to the IP table
     * (unicast or multicast) or the L2 table (mac + vlan) or the MPLS table.
     *
     * @param fwd the forwarding objective of type 'specific'
     * @return    a collection of flow rules. Typically there will be only one
     *            for this type of forwarding objective. An empty set may be
     *            returned if there is an issue in processing the objective.
     */
    private Collection<FlowRule> processSpecific(ForwardingObjective fwd) {
        log.debug("Processing specific fwd objective:{} in dev:{} with next:{}",
                  fwd.id(), deviceId, fwd.nextId());
        boolean isEthTypeObj = isSupportedEthTypeObjective(fwd);
        boolean isEthDstObj = isSupportedEthDstObjective(fwd);

        if (isEthTypeObj) {
            return processEthTypeSpecific(fwd);
        } else if (isEthDstObj) {
            return processEthDstSpecific(fwd);
        } else {
            log.warn("processSpecific: Unsupported forwarding objective "
                    + "criteria fwd:{} in dev:{}", fwd.nextId(), deviceId);
            fail(fwd, ObjectiveError.UNSUPPORTED);
            return Collections.emptySet();
        }
    }

    /**
     * Handles forwarding rules to the IP and MPLS tables.
     *
     * @param fwd the forwarding objective
     * @return A collection of flow rules, or an empty set
     */
    protected Collection<FlowRule> processEthTypeSpecific(ForwardingObjective fwd) {
        return processEthTypeSpecificInternal(fwd, false, ACL_TABLE);
    }

    /**
     * Internal implementation of processEthTypeSpecific.
     * <p>
     * Wildcarded IPv4_DST is not supported in OFDPA i12. Therefore, we break
     * the rule into 0.0.0.0/1 and 128.0.0.0/1.
     * The allowDefaultRoute must be set to false for OFDPA i12.
     * </p>
     *
     * @param fwd the forwarding objective
     * @param allowDefaultRoute allow wildcarded IPv4_DST or not
     * @param mplsNextTable next MPLS table
     * @return A collection of flow rules, or an empty set
     */
    protected Collection<FlowRule> processEthTypeSpecificInternal(ForwardingObjective fwd,
                                                                  boolean allowDefaultRoute,
                                                                  int mplsNextTable) {
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType =
                (EthTypeCriterion) selector.getCriterion(Criterion.Type.ETH_TYPE);
        boolean emptyGroup = false;
        int forTableId;
        TrafficSelector.Builder filteredSelector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();
        TrafficSelector.Builder complementarySelector = DefaultTrafficSelector.builder();

        if (ethType.ethType().toShort() == Ethernet.TYPE_IPV4) {
            if (buildIpv4Selector(filteredSelector, complementarySelector, fwd, allowDefaultRoute) < 0) {
                return Collections.emptyList();
            }
            // We need to set properly the next table
            IpPrefix ipv4Dst = ((IPCriterion) selector.getCriterion(Criterion.Type.IPV4_DST)).ip();
            if (ipv4Dst.isMulticast()) {
                forTableId = MULTICAST_ROUTING_TABLE;
            } else {
                forTableId = UNICAST_ROUTING_TABLE;
            }

            // TODO decrementing IP ttl is done automatically for routing, this
            // action is ignored or rejected in ofdpa as it is not fully implemented
            // if (fwd.treatment() != null) {
            //     for (Instruction instr : fwd.treatment().allInstructions()) {
            //         if (instr instanceof L3ModificationInstruction &&
            //                 ((L3ModificationInstruction) instr).subtype() == L3SubType.DEC_TTL) {
            //             tb.deferred().add(instr);
            //         }
            //     }
            // }

        } else if (ethType.ethType().toShort() == Ethernet.TYPE_IPV6) {
            if (buildIpv6Selector(filteredSelector, fwd) < 0) {
                return Collections.emptyList();
            }
            //We need to set the proper next table
            IpPrefix ipv6Dst = ((IPCriterion) selector.getCriterion(Criterion.Type.IPV6_DST)).ip();
            if (ipv6Dst.isMulticast()) {
                forTableId = MULTICAST_ROUTING_TABLE;
            } else {
                forTableId = UNICAST_ROUTING_TABLE;
            }

            // TODO decrementing IP ttl is done automatically for routing, this
            // action is ignored or rejected in ofdpa as it is not fully implemented
            // if (fwd.treatment() != null) {
            //     for (Instruction instr : fwd.treatment().allInstructions()) {
            //         if (instr instanceof L3ModificationInstruction &&
            //                 ((L3ModificationInstruction) instr).subtype() == L3SubType.DEC_TTL) {
            //             tb.deferred().add(instr);
            //         }
            //     }
            // }
        } else {
            filteredSelector
                .matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(((MplsCriterion)
                        selector.getCriterion(Criterion.Type.MPLS_LABEL)).label());
            MplsBosCriterion bos = (MplsBosCriterion) selector
                                        .getCriterion(MPLS_BOS);
            if (bos != null && requireMplsBosMatch()) {
                filteredSelector.matchMplsBos(bos.mplsBos());
            }
            forTableId = MPLS_TABLE_1;
            log.debug("processing MPLS specific forwarding objective {} -> next:{}"
                    + " in dev {}", fwd.id(), fwd.nextId(), deviceId);

            if (fwd.treatment() != null) {
                for (Instruction instr : fwd.treatment().allInstructions()) {
                    if (instr instanceof L2ModificationInstruction &&
                            ((L2ModificationInstruction) instr).subtype() == L2SubType.MPLS_POP) {
                        // OF-DPA does not pop in MPLS table in some cases. For the L3 VPN, it requires
                        // setting the MPLS_TYPE so pop can happen down the pipeline
                        if (requireMplsPop()) {
                            if (mplsNextTable == MPLS_TYPE_TABLE && isNotMplsBos(selector)) {
                                tb.immediate().popMpls();
                            }
                        } else {
                            // Skip mpls pop action for mpls_unicast label
                            if (instr instanceof ModMplsHeaderInstruction &&
                                    !((ModMplsHeaderInstruction) instr).ethernetType()
                                            .equals(EtherType.MPLS_UNICAST.ethType())) {
                                tb.immediate().add(instr);
                            }
                        }
                    }

                    if (requireMplsTtlModification()) {
                        if (instr instanceof L3ModificationInstruction &&
                                ((L3ModificationInstruction) instr).subtype() == L3SubType.DEC_TTL) {
                            // FIXME Should modify the app to send the correct DEC_MPLS_TTL instruction
                            tb.immediate().decMplsTtl();
                        }
                        if (instr instanceof L3ModificationInstruction &&
                                ((L3ModificationInstruction) instr).subtype() == L3SubType.TTL_IN) {
                            tb.immediate().add(instr);
                        }
                    }
                }
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
                if (isNotMplsBos(selector) && group.type().equals(SELECT)) {
                    log.warn("SR CONTINUE case cannot be handled as MPLS ECMP "
                            + "is not implemented in OF-DPA yet. Aborting flow {} -> next:{} "
                            + "in this device {}", fwd.id(), fwd.nextId(), deviceId);
                    fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
                    return Collections.emptySet();
                }
                tb.deferred().group(group.id());
                // retrying flows may be necessary due to bug CORD-554
                if (gkeys.size() == 1 && gkeys.get(0).size() == 1) {
                    if (shouldRetry()) {
                        log.warn("Found empty group 0x{} in dev:{} .. will retry fwd:{}",
                                 Integer.toHexString(group.id().id()), deviceId,
                                 fwd.id());
                        emptyGroup = true;
                    }
                }
            } else {
                log.warn("Cannot find group for nextId:{} in dev:{}. Aborting fwd:{}",
                         fwd.nextId(), deviceId, fwd.id());
                fail(fwd, ObjectiveError.FLOWINSTALLATIONFAILED);
                return Collections.emptySet();
            }
        }

        if (forTableId == MPLS_TABLE_1) {
            if (mplsNextTable == MPLS_L3_TYPE_TABLE) {
                Ofdpa3SetMplsType setMplsType = new Ofdpa3SetMplsType(Ofdpa3MplsType.L3_PHP);
                // set mpls type as apply_action
                tb.immediate().extension(setMplsType, deviceId);
            }
            tb.transition(mplsNextTable);
        } else {
            tb.transition(ACL_TABLE);
        }

        if (fwd.treatment() != null && fwd.treatment().clearedDeferred()) {
            if (supportsUnicastBlackHole()) {
                tb.wipeDeferred();
            } else {
                log.warn("Clear Deferred is not supported Unicast Routing Table on device {}", deviceId);
                return Collections.emptySet();
            }
        }

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
        if (!allowDefaultRoute) {
            flowRuleCollection.add(
                    defaultRoute(fwd, complementarySelector, forTableId, tb)
            );
            log.debug("Default rule 0.0.0.0/0 is being installed two rules");
        }

        if (emptyGroup) {
            retryExecutorService.schedule(new RetryFlows(fwd, flowRuleCollection),
                                     RETRY_MS, TimeUnit.MILLISECONDS);
        }
        return flowRuleCollection;
    }

    private int buildIpv4Selector(TrafficSelector.Builder builderToUpdate,
                                    TrafficSelector.Builder extBuilder,
                                    ForwardingObjective fwd,
                                    boolean allowDefaultRoute) {
        TrafficSelector selector = fwd.selector();

        IpPrefix ipv4Dst = ((IPCriterion) selector.getCriterion(Criterion.Type.IPV4_DST)).ip();
        if (ipv4Dst.isMulticast()) {
            if (ipv4Dst.prefixLength() != 32) {
                log.warn("Multicast specific forwarding objective can only be /32");
                fail(fwd, ObjectiveError.BADPARAMS);
                return -1;
            }
            VlanId assignedVlan = readVlanFromSelector(fwd.meta());
            if (assignedVlan == null) {
                log.warn("VLAN ID required by multicast specific fwd obj is missing. Abort.");
                fail(fwd, ObjectiveError.BADPARAMS);
                return -1;
            }
            if (requireVlanExtensions()) {
                OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(assignedVlan);
                builderToUpdate.extension(ofdpaMatchVlanVid, deviceId);
            } else {
                builderToUpdate.matchVlanId(assignedVlan);
            }
            builderToUpdate.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(ipv4Dst);
            log.debug("processing IPv4 multicast specific forwarding objective {} -> next:{}"
                              + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
        } else {
            if (ipv4Dst.prefixLength() == 0) {
                if (allowDefaultRoute) {
                    // The entire IPV4_DST field is wildcarded intentionally
                    builderToUpdate.matchEthType(Ethernet.TYPE_IPV4);
                } else {
                    // NOTE: The switch does not support matching 0.0.0.0/0
                    // Split it into 0.0.0.0/1 and 128.0.0.0/1
                    builderToUpdate.matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPDst(IpPrefix.valueOf("0.0.0.0/1"));
                    extBuilder.matchEthType(Ethernet.TYPE_IPV4)
                            .matchIPDst(IpPrefix.valueOf("128.0.0.0/1"));
                }
            } else {
                builderToUpdate.matchEthType(Ethernet.TYPE_IPV4).matchIPDst(ipv4Dst);
            }
            log.debug("processing IPv4 unicast specific forwarding objective {} -> next:{}"
                              + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
        }
        return 0;
    }

    /**
     * Helper method to build Ipv6 selector using the selector provided by
     * a forwarding objective.
     *
     * @param builderToUpdate the builder to update
     * @param fwd the selector to read
     * @return 0 if the update ends correctly. -1 if the matches
     * are not yet supported
     */
    int buildIpv6Selector(TrafficSelector.Builder builderToUpdate,
                                    ForwardingObjective fwd) {

        TrafficSelector selector = fwd.selector();

        IpPrefix ipv6Dst = ((IPCriterion) selector.getCriterion(Criterion.Type.IPV6_DST)).ip();
        if (ipv6Dst.isMulticast()) {
            if (ipv6Dst.prefixLength() != IpAddress.INET6_BIT_LENGTH) {
                log.warn("Multicast specific forwarding objective can only be /128");
                fail(fwd, ObjectiveError.BADPARAMS);
                return -1;
            }
            VlanId assignedVlan = readVlanFromSelector(fwd.meta());
            if (assignedVlan == null) {
                log.warn("VLAN ID required by multicast specific fwd obj is missing. Abort.");
                fail(fwd, ObjectiveError.BADPARAMS);
                return -1;
            }
            if (requireVlanExtensions()) {
                OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(assignedVlan);
                builderToUpdate.extension(ofdpaMatchVlanVid, deviceId);
            } else {
                builderToUpdate.matchVlanId(assignedVlan);
            }
            builderToUpdate.matchEthType(Ethernet.TYPE_IPV6).matchIPv6Dst(ipv6Dst);
            log.debug("processing IPv6 multicast specific forwarding objective {} -> next:{}"
                              + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
        } else {
           if (ipv6Dst.prefixLength() != 0) {
               builderToUpdate.matchIPv6Dst(ipv6Dst);
           }
        builderToUpdate.matchEthType(Ethernet.TYPE_IPV6);
        log.debug("processing IPv6 unicast specific forwarding objective {} -> next:{}"
                              + " in dev:{}", fwd.id(), fwd.nextId(), deviceId);
        }
        return 0;
    }

    FlowRule defaultRoute(ForwardingObjective fwd,
                                    TrafficSelector.Builder complementarySelector,
                                    int forTableId,
                                    TrafficTreatment.Builder tb) {
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
        return rule.build();
    }

    /**
     * Handles forwarding rules to the L2 bridging table. Flow actions are not
     * allowed in the bridging table - instead we use L2 Interface group or
     * L2 flood group
     *
     * @param fwd the forwarding objective
     * @return A collection of flow rules, or an empty set
     */
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

        if (!ethCriterion.mac().equals(NONE) &&
                !ethCriterion.mac().equals(BROADCAST)) {
            filteredSelectorBuilder.matchEthDst(ethCriterion.mac());
            log.debug("processing L2 forwarding objective:{} -> next:{} in dev:{}",
                      fwd.id(), fwd.nextId(), deviceId);
        } else {
            // Use wildcard DST_MAC if the MacAddress is None or Broadcast
            log.debug("processing L2 Broadcast forwarding objective:{} -> next:{} "
                    + "in dev:{} for vlan:{}",
                      fwd.id(), fwd.nextId(), deviceId, vlanIdCriterion.vlanId());
        }
        if (requireVlanExtensions()) {
            OfdpaMatchVlanVid ofdpaMatchVlanVid = new OfdpaMatchVlanVid(vlanIdCriterion.vlanId());
            filteredSelectorBuilder.extension(ofdpaMatchVlanVid, deviceId);
        } else {
            filteredSelectorBuilder.matchVlanId(vlanIdCriterion.vlanId());
        }
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

    //////////////////////////////////////
    //  Helper Methods and Classes
    //////////////////////////////////////

    private boolean isSupportedEthTypeObjective(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthTypeCriterion ethType = (EthTypeCriterion) selector
                .getCriterion(Criterion.Type.ETH_TYPE);
        return !((ethType == null) ||
                ((ethType.ethType().toShort() != Ethernet.TYPE_IPV4) &&
                        (ethType.ethType().toShort() != Ethernet.MPLS_UNICAST)) &&
                        (ethType.ethType().toShort() != Ethernet.TYPE_IPV6));
    }

    private boolean isSupportedEthDstObjective(ForwardingObjective fwd) {
        TrafficSelector selector = fwd.selector();
        EthCriterion ethDst = (EthCriterion) selector
                .getCriterion(Criterion.Type.ETH_DST);
        VlanIdCriterion vlanId = (VlanIdCriterion) selector
                .getCriterion(Criterion.Type.VLAN_VID);
        return !(ethDst == null && vlanId == null);
    }

    NextGroup getGroupForNextObjective(Integer nextId) {
        NextGroup next = flowObjectiveStore.getNextGroup(nextId);
        if (next != null) {
            List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
            if (gkeys != null && !gkeys.isEmpty()) {
                return next;
            } else {
               log.warn("Empty next group found in FlowObjective store for "
                       + "next-id:{} in dev:{}", nextId, deviceId);
            }
        } else {
            log.warn("next-id {} not found in Flow objective store for dev:{}",
                     nextId, deviceId);
        }
        return null;
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        List<String> mappings = new ArrayList<>();
        List<Deque<GroupKey>> gkeys = appKryo.deserialize(nextGroup.data());
        for (Deque<GroupKey> gkd : gkeys) {
            Group lastGroup = null;
            StringBuilder gchain = new StringBuilder();
            for (GroupKey gk : gkd) {
                Group g = groupService.getGroup(deviceId, gk);
                if (g == null) {
                    gchain.append("  NoGrp").append(" -->");
                    continue;
                }
                gchain.append("  0x").append(Integer.toHexString(g.id().id()))
                    .append(" -->");
                lastGroup = g;
            }
            // add port information for last group in group-chain
            List<Instruction> lastGroupIns = new ArrayList<>();
            if (lastGroup != null && !lastGroup.buckets().buckets().isEmpty()) {
                lastGroupIns = lastGroup.buckets().buckets().get(0)
                                    .treatment().allInstructions();
            }
            for (Instruction i: lastGroupIns) {
                if (i instanceof OutputInstruction) {
                    gchain.append(" port:").append(((OutputInstruction) i).port());
                }
            }
            mappings.add(gchain.toString());
        }
        return mappings;
    }

    /**
     *  Utility class that retries sending flows a fixed number of times, even if
     *  some of the attempts are successful. Used only for forwarding objectives.
     */
    public final class RetryFlows implements Runnable {
        int attempts = MAX_RETRY_ATTEMPTS;
        private Collection<FlowRule> retryFlows;
        private ForwardingObjective fwd;

        RetryFlows(ForwardingObjective fwd, Collection<FlowRule> retryFlows) {
            this.fwd = fwd;
            this.retryFlows = retryFlows;
        }

        @Override
        public void run() {
            log.info("RETRY FLOWS ATTEMPT# {} for fwd:{} rules:{}",
                     MAX_RETRY_ATTEMPTS - attempts, fwd.id(), retryFlows.size());
            sendForwards(Collections.singletonList(Pair.of(fwd, retryFlows)));
            if (--attempts > 0) {
                retryExecutorService.schedule(this, RETRY_MS, TimeUnit.MILLISECONDS);
            }
        }
    }

    // Flow rules accumulator for reducing the number of transactions required to the devices.
    private final class ForwardingObjectiveAccumulator
            extends AbstractAccumulator<Pair<ForwardingObjective, Collection<FlowRule>>> {

        ForwardingObjectiveAccumulator(int maxFwd, int maxBatchMS, int maxIdleMS) {
            super(TIMER, maxFwd, maxBatchMS, maxIdleMS);
        }

        @Override
        public void processItems(List<Pair<ForwardingObjective, Collection<FlowRule>>> pairs) {
            // Triggers creation of a batch using the list of flowrules generated from fwdobjs.
            accumulatorExecutorService.execute(new FlowRulesBuilderTask(pairs));
        }
    }

    // Task for building batch of flow rules in a separate thread.
    private final class FlowRulesBuilderTask implements Runnable {
        private final List<Pair<ForwardingObjective, Collection<FlowRule>>> pairs;

        FlowRulesBuilderTask(List<Pair<ForwardingObjective, Collection<FlowRule>>> pairs) {
            this.pairs = pairs;
        }

        @Override
        public void run() {
            try {
                sendForwards(pairs);
            } catch (Exception e) {
                log.warn("Unable to send forwards", e);
            }
        }
    }

}
