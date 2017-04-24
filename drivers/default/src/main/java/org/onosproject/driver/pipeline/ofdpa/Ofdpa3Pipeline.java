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
import org.onosproject.core.ApplicationId;
import org.onosproject.driver.extensions.Ofdpa3MatchMplsL2Port;
import org.onosproject.driver.extensions.Ofdpa3MatchOvid;
import org.onosproject.driver.extensions.Ofdpa3PopCw;
import org.onosproject.driver.extensions.Ofdpa3PopL2Header;
import org.onosproject.driver.extensions.Ofdpa3SetMplsL2Port;
import org.onosproject.driver.extensions.Ofdpa3SetMplsType;
import org.onosproject.driver.extensions.Ofdpa3SetOvid;
import org.onosproject.driver.extensions.Ofdpa3SetQosIndex;
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
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupKey;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static org.onosproject.driver.extensions.Ofdpa3MplsType.VPWS;
import static org.onosproject.net.flow.criteria.Criterion.Type.*;
import static org.onosproject.net.flow.instructions.Instruction.Type.L2MODIFICATION;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.ModTunnelIdInstruction;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pipeliner for Broadcom OF-DPA 3.0 TTP.
 */
public class Ofdpa3Pipeline extends Ofdpa2Pipeline {

    private final Logger log = getLogger(getClass());

    @Override
    protected void initDriverId() {
        driverId = coreService.registerApplication(
                "org.onosproject.driver.Ofdpa3Pipeline");
    }

    @Override
    protected void initGroupHander(PipelinerContext context) {
        groupHandler = new Ofdpa3GroupHandler();
        groupHandler.init(deviceId, context);
    }

    @Override
    protected boolean requireVlanExtensions() {
        return false;
    }

    @Override
    protected void processFilter(FilteringObjective filteringObjective,
                                 boolean install,
                                 ApplicationId applicationId) {
        // We are looking for inner vlan id criterion. We use this
        // to identify the pseudo wire flows. In future we can enforce
        // using also the tunnel id in the meta.
        VlanIdCriterion innerVlanIdCriterion = null;
        for (Criterion criterion : filteringObjective.conditions()) {
            if (criterion.type() == INNER_VLAN_VID) {
                innerVlanIdCriterion = (VlanIdCriterion) criterion;
                break;
            }
        }
        if (innerVlanIdCriterion != null) {
            FlowRuleOperations.Builder ops = FlowRuleOperations.builder();
            PortCriterion portCriterion;
            VlanIdCriterion outerVlanIdCriterion = null;
            // We extract the expected port criterion in the key.
            portCriterion = (PortCriterion) filteringObjective.key();
            // We extract the outer vlan id criterion.
            for (Criterion criterion : filteringObjective.conditions()) {
                if (criterion.type() == VLAN_VID) {
                    outerVlanIdCriterion = (VlanIdCriterion) criterion;
                    break;
                }
            }
            // We extract the tunnel id.
            long tunnelId;
            if (filteringObjective.meta() != null &&
                    filteringObjective.meta().allInstructions().size() != 1) {
                log.warn("Bad filtering objective from app: {}. Not"
                                 + "processing filtering objective", applicationId);
                fail(filteringObjective, ObjectiveError.BADPARAMS);
                return;
            } else if (filteringObjective.meta() != null &&
                    filteringObjective.meta().allInstructions().size() == 1 &&
                    filteringObjective.meta().allInstructions().get(0).type() == L2MODIFICATION) {
                L2ModificationInstruction l2instruction = (L2ModificationInstruction)
                        filteringObjective.meta().allInstructions().get(0);
                if (l2instruction.subtype() != L2SubType.TUNNEL_ID) {
                    log.warn("Bad filtering objective from app: {}. Not"
                                     + "processing filtering objective", applicationId);
                    fail(filteringObjective, ObjectiveError.BADPARAMS);
                    return;
                } else {
                    tunnelId = ((ModTunnelIdInstruction) l2instruction).tunnelId();
                }
            } else {
                log.warn("Bad filtering objective from app: {}. Not"
                                 + "processing filtering objective", applicationId);
                fail(filteringObjective, ObjectiveError.BADPARAMS);
                return;
            }
            // Mpls tunnel ids according to the OFDPA manual have to be
            // in the range [2^17-1, 2^16].
            tunnelId = MPLS_TUNNEL_ID_BASE | tunnelId;
            // Sanity check for the filtering objective.
            if (portCriterion == null ||
                    outerVlanIdCriterion == null ||
                    tunnelId > MPLS_TUNNEL_ID_MAX) {
                log.warn("Bad filtering objective from app: {}. Not"
                                 + "processing filtering objective", applicationId);
                fail(filteringObjective, ObjectiveError.BADPARAMS);
                return;
            }
            // 0x0000XXXX is UNI interface.
            if (portCriterion.port().toLong() > MPLS_UNI_PORT_MAX) {
                log.error("Filering Objective invalid logical port {}",
                          portCriterion.port().toLong());
                fail(filteringObjective, ObjectiveError.BADPARAMS);
                return;
            }
            // We create the flows.
            List<FlowRule> pwRules = processPwFilter(portCriterion,
                                                     innerVlanIdCriterion,
                                                     outerVlanIdCriterion,
                                                     tunnelId,
                                                     applicationId
            );
            // We tag the flow for adding or for removing.
            for (FlowRule pwRule : pwRules) {
                log.debug("adding filtering rule in VLAN tables: {} for dev: {}",
                          pwRule, deviceId);
                ops = install ? ops.add(pwRule) : ops.remove(pwRule);
            }
            // We push the filtering rules for the pw.
            flowRuleService.apply(ops.build(new FlowRuleOperationsContext() {
                @Override
                public void onSuccess(FlowRuleOperations ops) {
                    log.info("Applied {} filtering rules in device {}",
                             ops.stages().get(0).size(), deviceId);
                    pass(filteringObjective);
                }

                @Override
                public void onError(FlowRuleOperations ops) {
                    log.info("Failed to apply all filtering rules in dev {}", deviceId);
                    fail(filteringObjective, ObjectiveError.FLOWINSTALLATIONFAILED);
                }
            }));

            return;
        }
        // If it is not a pseudo wire flow we fall back
        // to the OFDPA 2.0 pipeline.
        super.processFilter(filteringObjective, install, applicationId);
    }

    /**
     * Method to process the pw related filtering objectives.
     *
     * @param portCriterion the in port match
     * @param innerVlanIdCriterion the inner vlan match
     * @param outerVlanIdCriterion the outer vlan match
     * @param tunnelId the tunnel id
     * @param applicationId the application id
     * @return a list of flow rules to install
     */
    private List<FlowRule> processPwFilter(PortCriterion portCriterion,
                                           VlanIdCriterion innerVlanIdCriterion,
                                           VlanIdCriterion outerVlanIdCriterion,
                                           long tunnelId,
                                           ApplicationId applicationId) {
        // As first we create the flow rule for the vlan 1 table.
        FlowRule vlan1FlowRule;
        int mplsLogicalPort = ((int) portCriterion.port().toLong());
        // We have to match on the inner vlan and outer vlan at the same time.
        // Ofdpa supports this through the OVID meta-data type.
        TrafficSelector.Builder vlan1Selector = DefaultTrafficSelector.builder()
                .matchInPort(portCriterion.port())
                .matchVlanId(innerVlanIdCriterion.vlanId())
                .extension(new Ofdpa3MatchOvid(outerVlanIdCriterion.vlanId()), deviceId);
        // TODO understand for the future how to manage the vlan rewrite.
        TrafficTreatment.Builder vlan1Treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(outerVlanIdCriterion.vlanId())
                .extension(new Ofdpa3SetMplsType(VPWS), deviceId)
                .extension(new Ofdpa3SetMplsL2Port(mplsLogicalPort), deviceId)
                .setTunnelId(tunnelId)
                .transition(MPLS_L2_PORT_FLOW_TABLE);
        vlan1FlowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(vlan1Selector.build())
                .withTreatment(vlan1Treatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(VLAN_1_TABLE)
                .build();
        // Finally we create the flow rule for the vlan table.
        FlowRule vlanFlowRule;
        // We have to match on the outer vlan.
        TrafficSelector.Builder vlanSelector = DefaultTrafficSelector.builder()
                .matchInPort(portCriterion.port())
                .matchVlanId(outerVlanIdCriterion.vlanId());
        // TODO understand for the future how to manage the vlan rewrite.
        TrafficTreatment.Builder vlanTreatment = DefaultTrafficTreatment.builder()
                .popVlan()
                .extension(new Ofdpa3SetOvid(outerVlanIdCriterion.vlanId()), deviceId)
                .transition(VLAN_1_TABLE);
        vlanFlowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(vlanSelector.build())
                .withTreatment(vlanTreatment.build())
                .withPriority(DEFAULT_PRIORITY)
                .fromApp(applicationId)
                .makePermanent()
                .forTable(VLAN_TABLE)
                .build();

        return ImmutableList.of(vlan1FlowRule, vlanFlowRule);
    }

    @Override
    protected Collection<FlowRule> processEthTypeSpecific(ForwardingObjective fwd) {
        if (isNotMplsBos(fwd.selector())) {
            return processEthTypeSpecificInternal(fwd, true, MPLS_TYPE_TABLE);
        }
        return processEthTypeSpecificInternal(fwd, true, MPLS_L3_TYPE_TABLE);
    }

    @Override
    protected Collection<FlowRule> processVersatile(ForwardingObjective fwd) {
        // We use the tunnel id to identify pw related flows.
        // Looking for the fwd objective of the initiation.
        TunnelIdCriterion tunnelIdCriterion = (TunnelIdCriterion) fwd.selector()
                .getCriterion(TUNNEL_ID);
        if (tunnelIdCriterion != null) {
            return processInitPwVersatile(fwd);
        }
        // Looking for the fwd objective of the termination.
        ModTunnelIdInstruction modTunnelIdInstruction = getModTunnelIdInstruction(fwd.treatment());
        OutputInstruction outputInstruction = getOutputInstruction(fwd.treatment());
        if (modTunnelIdInstruction != null && outputInstruction != null) {
            return processTermPwVersatile(fwd, modTunnelIdInstruction, outputInstruction);
        }
        // If it is not a pseudo wire flow we fall back
        // to the OFDPA 2.0 pipeline.
        return super.processVersatile(fwd);
    }

    private Collection<FlowRule> processTermPwVersatile(ForwardingObjective forwardingObjective,
                                                        ModTunnelIdInstruction modTunnelIdInstruction,
                                                        OutputInstruction outputInstruction) {
        TrafficTreatment.Builder flowTreatment;
        TrafficSelector.Builder flowSelector;
        // We divide the mpls actions from the tunnel actions. We need
        // this to order the actions in the final treatment.
        TrafficTreatment.Builder mplsTreatment = DefaultTrafficTreatment.builder();
        createMplsTreatment(forwardingObjective.treatment(), mplsTreatment);
        // The match of the forwarding objective is ready to go.
        flowSelector = DefaultTrafficSelector.builder(forwardingObjective.selector());
        // We verify the tunnel id and mpls port are correct.
        long tunnelId = MPLS_TUNNEL_ID_BASE | modTunnelIdInstruction.tunnelId();
        if (tunnelId > MPLS_TUNNEL_ID_MAX) {
            log.error("Pw Versatile Forwarding Objective must include tunnel id < {}",
                      MPLS_TUNNEL_ID_MAX);
            fail(forwardingObjective, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        // 0x0002XXXX is NNI interface.
        int mplsLogicalPort = ((int) outputInstruction.port().toLong()) | MPLS_NNI_PORT_BASE;
        if (mplsLogicalPort > MPLS_NNI_PORT_MAX) {
            log.error("Pw Versatile Forwarding Objective invalid logical port {}",
                      mplsLogicalPort);
            fail(forwardingObjective, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        // Next id cannot be null.
        if (forwardingObjective.nextId() == null) {
            log.error("Pw Versatile Forwarding Objective must contain nextId ",
                      forwardingObjective.nextId());
            fail(forwardingObjective, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        // We retrieve the l2 interface group and point the mpls
        // flow to this.
        NextGroup next = getGroupForNextObjective(forwardingObjective.nextId());
        if (next == null) {
            log.warn("next-id:{} not found in dev:{}", forwardingObjective.nextId(), deviceId);
            fail(forwardingObjective, ObjectiveError.GROUPMISSING);
            return Collections.emptySet();
        }
        List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
        Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
        if (group == null) {
            log.warn("Group with key:{} for next-id:{} not found in dev:{}",
                     gkeys.get(0).peekFirst(), forwardingObjective.nextId(), deviceId);
            fail(forwardingObjective, ObjectiveError.GROUPMISSING);
            return Collections.emptySet();
        }
        // We prepare the treatment for the mpls flow table.
        // The order of the actions has to be strictly this
        // according to the OFDPA 2.0 specification.
        flowTreatment = DefaultTrafficTreatment.builder(mplsTreatment.build());
        flowTreatment.extension(new Ofdpa3PopCw(), deviceId);
        // Even though the specification and the xml/json files
        // specify is allowed, the switch rejects the flow. In the
        // OFDPA 3.0 EA0 version was necessary
        //flowTreatment.popVlan();
        flowTreatment.extension(new Ofdpa3PopL2Header(), deviceId);
        flowTreatment.setTunnelId(tunnelId);
        flowTreatment.extension(new Ofdpa3SetMplsL2Port(mplsLogicalPort), deviceId);
        flowTreatment.extension(new Ofdpa3SetMplsType(VPWS), deviceId);
        flowTreatment.transition(MPLS_TYPE_TABLE);
        flowTreatment.deferred().group(group.id());
        // We prepare the flow rule for the mpls table.
        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(forwardingObjective.appId())
                .withPriority(forwardingObjective.priority())
                .forDevice(deviceId)
                .withSelector(flowSelector.build())
                .withTreatment(flowTreatment.build())
                .makePermanent()
                .forTable(MPLS_TABLE_1);
        return Collections.singletonList(ruleBuilder.build());
    }

    /**
     * Helper method to process the pw forwarding objectives.
     *
     * @param forwardingObjective the fw objective to process
     * @return a singleton list of flow rule
     */
    private Collection<FlowRule> processInitPwVersatile(ForwardingObjective forwardingObjective) {
        // We retrieve the matching criteria for mpls l2 port.
        TunnelIdCriterion tunnelIdCriterion = (TunnelIdCriterion) forwardingObjective.selector()
                .getCriterion(TUNNEL_ID);
        PortCriterion portCriterion = (PortCriterion) forwardingObjective.selector()
                .getCriterion(IN_PORT);
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        int mplsLogicalPort;
        long tunnelId;
        // Mpls tunnel ids according to the OFDPA manual have to be
        // in the range [2^17-1, 2^16].
        tunnelId = MPLS_TUNNEL_ID_BASE | tunnelIdCriterion.tunnelId();
        if (tunnelId > MPLS_TUNNEL_ID_MAX) {
            log.error("Pw Versatile Forwarding Objective must include tunnel id < {}",
                      MPLS_TUNNEL_ID_MAX);
            fail(forwardingObjective, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        // Port has not been null.
        if (portCriterion == null) {
            log.error("Pw Versatile Forwarding Objective must include port");
            fail(forwardingObjective, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        // 0x0000XXXX is UNI interface.
        if (portCriterion.port().toLong() > MPLS_UNI_PORT_MAX) {
            log.error("Pw Versatile Forwarding Objective invalid logical port {}",
                      portCriterion.port().toLong());
            fail(forwardingObjective, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        mplsLogicalPort = ((int) portCriterion.port().toLong());
        if (forwardingObjective.nextId() == null) {
            log.error("Pw Versatile Forwarding Objective must contain nextId ",
                      forwardingObjective.nextId());
            fail(forwardingObjective, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        // We don't expect a treatment.
        if (forwardingObjective.treatment() != null &&
                !forwardingObjective.treatment().equals(DefaultTrafficTreatment.emptyTreatment())) {
            log.error("Pw Versatile Forwarding Objective cannot contain a treatment ",
                      forwardingObjective.nextId());
            fail(forwardingObjective, ObjectiveError.BADPARAMS);
            return Collections.emptySet();
        }
        // We retrieve the l2 vpn group and point the mpls
        // l2 port to this.
        NextGroup next = getGroupForNextObjective(forwardingObjective.nextId());
        if (next == null) {
            log.warn("next-id:{} not found in dev:{}", forwardingObjective.nextId(), deviceId);
            fail(forwardingObjective, ObjectiveError.GROUPMISSING);
            return Collections.emptySet();
        }
        List<Deque<GroupKey>> gkeys = appKryo.deserialize(next.data());
        Group group = groupService.getGroup(deviceId, gkeys.get(0).peekFirst());
        if (group == null) {
            log.warn("Group with key:{} for next-id:{} not found in dev:{}",
                     gkeys.get(0).peekFirst(), forwardingObjective.nextId(), deviceId);
            fail(forwardingObjective, ObjectiveError.GROUPMISSING);
            return Collections.emptySet();
        }
        // We prepare the flow rule for the mpls l2 port table.
        selector.matchTunnelId(tunnelId);
        selector.extension(new Ofdpa3MatchMplsL2Port(mplsLogicalPort), deviceId);
        // This should not be necessary but without we receive an error
        treatment.extension(new Ofdpa3SetQosIndex(0), deviceId);
        treatment.transition(MPLS_L2_PORT_PCP_TRUST_FLOW_TABLE);
        treatment.deferred().group(group.id());
        FlowRule.Builder ruleBuilder = DefaultFlowRule.builder()
                .fromApp(forwardingObjective.appId())
                .withPriority(MPLS_L2_PORT_PRIORITY)
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .makePermanent()
                .forTable(MPLS_L2_PORT_FLOW_TABLE);
        return Collections.singletonList(ruleBuilder.build());
    }

    /**
     * Utility function to get the mod tunnel id instruction
     * if present.
     *
     * @param treatment the treatment to analyze
     * @return the mod tunnel id instruction if present,
     * otherwise null
     */
    private ModTunnelIdInstruction getModTunnelIdInstruction(TrafficTreatment treatment) {
        if (treatment == null) {
            return null;
        }

        L2ModificationInstruction l2ModificationInstruction;
        for (Instruction instruction : treatment.allInstructions()) {
            if (instruction.type() == L2MODIFICATION) {
                l2ModificationInstruction = (L2ModificationInstruction) instruction;
                if (l2ModificationInstruction.subtype() == L2SubType.TUNNEL_ID) {
                    return (ModTunnelIdInstruction) l2ModificationInstruction;
                }
            }
        }
        return null;
    }

    /**
     * Utility function to get the output instruction
     * if present.
     *
     * @param treatment the treatment to analyze
     * @return the output instruction if present,
     * otherwise null
     */
    private OutputInstruction getOutputInstruction(TrafficTreatment treatment) {
        if (treatment == null) {
            return null;
        }

        for (Instruction instruction : treatment.allInstructions()) {
            if (instruction.type() == Instruction.Type.OUTPUT) {
                return (OutputInstruction) instruction;
            }
        }
        return null;
    }

    /**
     * Helper method for dividing the tunnel instructions from the mpls
     * instructions.
     *
     * @param treatment the treatment to analyze
     * @param mplsTreatment the mpls treatment builder
     */
    private void createMplsTreatment(TrafficTreatment treatment,
                                     TrafficTreatment.Builder mplsTreatment) {

        for (Instruction ins : treatment.allInstructions()) {

            if (ins.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2ins = (L2ModificationInstruction) ins;
                switch (l2ins.subtype()) {
                    // These instructions have to go in the mpls
                    // treatment.
                    case TUNNEL_ID:
                        break;
                    case DEC_MPLS_TTL:
                    case MPLS_POP:
                        mplsTreatment.add(ins);
                        break;
                    default:
                        log.warn("Driver does not handle this type of TrafficTreatment"
                                         + " instruction in nextObjectives: {} - {}",
                                 ins.type(), ins);
                        break;
                }
            } else if (ins.type() == Instruction.Type.OUTPUT) {
                break;
            } else if (ins.type() == Instruction.Type.L3MODIFICATION) {
                // We support partially the l3 instructions.
                L3ModificationInstruction l3ins = (L3ModificationInstruction) ins;
                switch (l3ins.subtype()) {
                    case TTL_IN:
                        mplsTreatment.add(ins);
                        break;
                    default:
                        log.warn("Driver does not handle this type of TrafficTreatment"
                                         + " instruction in nextObjectives: {} - {}",
                                 ins.type(), ins);
                }

            } else {
                log.warn("Driver does not handle this type of TrafficTreatment"
                                 + " instruction in nextObjectives: {} - {}",
                         ins.type(), ins);
            }
        }
    }
}
