/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.hp;

import org.onlab.packet.Ethernet;
import org.onosproject.core.GroupId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.EthCriterion;
import org.onosproject.net.flow.criteria.EthTypeCriterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.group.Group;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver for HP3800 hybrid switches.
 *
 * Refer to the device manual to check unsupported features and features supported in hardware
 *
 */

public class HPPipelineV3800 extends AbstractHPPipeline {

    private final Logger log = getLogger(getClass());

    @Override
    protected FlowRule.Builder setDefaultTableIdForFlowObjective(FlowRule.Builder ruleBuilder) {
        log.debug("HP V3800 Driver - Setting default table id to hardware table {}", HP_HARDWARE_TABLE);
        return ruleBuilder.forTable(HP_HARDWARE_TABLE);
    }

    @Override
    protected void initUnSupportedFeatures() {
        //Initialize unsupported criteria
        unsupportedCriteria.add(Criterion.Type.METADATA);
        unsupportedCriteria.add(Criterion.Type.IP_ECN);
        unsupportedCriteria.add(Criterion.Type.SCTP_SRC);
        unsupportedCriteria.add(Criterion.Type.SCTP_SRC_MASKED);
        unsupportedCriteria.add(Criterion.Type.SCTP_DST);
        unsupportedCriteria.add(Criterion.Type.SCTP_DST_MASKED);
        unsupportedCriteria.add(Criterion.Type.IPV6_ND_SLL);
        unsupportedCriteria.add(Criterion.Type.IPV6_ND_TLL);
        unsupportedCriteria.add(Criterion.Type.MPLS_LABEL);
        unsupportedCriteria.add(Criterion.Type.MPLS_TC);
        unsupportedCriteria.add(Criterion.Type.MPLS_BOS);
        unsupportedCriteria.add(Criterion.Type.PBB_ISID);
        unsupportedCriteria.add(Criterion.Type.TUNNEL_ID);
        unsupportedCriteria.add(Criterion.Type.IPV6_EXTHDR);

        //Initialize unsupported instructions
        unsupportedInstructions.add(Instruction.Type.QUEUE);
        unsupportedInstructions.add(Instruction.Type.METADATA);
        unsupportedInstructions.add(Instruction.Type.L0MODIFICATION);
        unsupportedInstructions.add(Instruction.Type.L1MODIFICATION);
        unsupportedInstructions.add(Instruction.Type.PROTOCOL_INDEPENDENT);
        unsupportedInstructions.add(Instruction.Type.EXTENSION);
        unsupportedInstructions.add(Instruction.Type.STAT_TRIGGER);

        //Initialize unsupportet L2MODIFICATION actions
        unsupportedL2mod.add(L2ModificationInstruction.L2SubType.MPLS_PUSH);
        unsupportedL2mod.add(L2ModificationInstruction.L2SubType.MPLS_POP);
        unsupportedL2mod.add(L2ModificationInstruction.L2SubType.MPLS_LABEL);
        unsupportedL2mod.add(L2ModificationInstruction.L2SubType.MPLS_BOS);
        unsupportedL2mod.add(L2ModificationInstruction.L2SubType.DEC_MPLS_TTL);

        //Initialize unsupported L3MODIFICATION actions
        unsupportedL3mod.add(L3ModificationInstruction.L3SubType.TTL_IN);
        unsupportedL3mod.add(L3ModificationInstruction.L3SubType.TTL_OUT);
        unsupportedL3mod.add(L3ModificationInstruction.L3SubType.DEC_TTL);

        //All L4MODIFICATION actions are supported
    }

    @Override
    protected void initHardwareCriteria() {
        log.debug("HP V3800 Driver - Initializing hardware supported criteria");

        hardwareCriteria.add(Criterion.Type.IN_PORT);
        hardwareCriteria.add(Criterion.Type.VLAN_VID);
        hardwareCriteria.add(Criterion.Type.VLAN_PCP);

        //Match in hardware is not supported ETH_TYPE == VLAN (0x8100)
        hardwareCriteria.add(Criterion.Type.ETH_TYPE);

        hardwareCriteria.add(Criterion.Type.ETH_SRC);
        hardwareCriteria.add(Criterion.Type.ETH_DST);
        hardwareCriteria.add(Criterion.Type.IPV4_SRC);
        hardwareCriteria.add(Criterion.Type.IPV4_DST);
        hardwareCriteria.add(Criterion.Type.IP_PROTO);
        hardwareCriteria.add(Criterion.Type.IP_DSCP);
        hardwareCriteria.add(Criterion.Type.TCP_SRC);
        hardwareCriteria.add(Criterion.Type.TCP_DST);
    }

    @Override
    protected void initHardwareInstructions() {
        log.debug("HP V3800 Driver - Initializing hardware supported instructions");

        hardwareInstructions.add(Instruction.Type.OUTPUT);

        //Only modification of VLAN priority (VLAN_PCP) is supported in hardware
        hardwareInstructions.add(Instruction.Type.L2MODIFICATION);

        hardwareInstructionsL2mod.add(L2ModificationInstruction.L2SubType.ETH_SRC);
        hardwareInstructionsL2mod.add(L2ModificationInstruction.L2SubType.ETH_DST);
        hardwareInstructionsL2mod.add(L2ModificationInstruction.L2SubType.VLAN_ID);
        hardwareInstructionsL2mod.add(L2ModificationInstruction.L2SubType.VLAN_PCP);

        //Only GROUP of type ALL is supported in hardware
        //Moreover, for hardware support, each bucket must contain one and only one instruction of type OUTPUT
        hardwareInstructions.add(Instruction.Type.GROUP);

        hardwareGroups.add(Group.Type.ALL);

        //TODO also L3MODIFICATION of IP_DSCP is supported in hardware
    }

    //Return TRUE if ForwardingObjective fwd includes UNSUPPORTED features
    @Override
    protected boolean checkUnSupportedFeatures(TrafficSelector selector, TrafficTreatment treatment) {
        boolean unsupportedFeatures = false;

        for (Criterion criterion : selector.criteria()) {
            if (this.unsupportedCriteria.contains(criterion.type())) {
                log.warn("HP V3800 Driver - unsupported criteria {}", criterion.type());

                unsupportedFeatures = true;
            }
        }

        for (Instruction instruction : treatment.allInstructions()) {
            if (this.unsupportedInstructions.contains(instruction.type())) {
                log.warn("HP V3800 Driver - unsupported instruction {}", instruction.type());

                unsupportedFeatures = true;
            }

            if (instruction.type() == Instruction.Type.L2MODIFICATION) {
                if (this.unsupportedL2mod.contains(((L2ModificationInstruction) instruction).subtype())) {
                    log.warn("HP V3800 Driver - unsupported L2MODIFICATION instruction {}",
                            ((L2ModificationInstruction) instruction).subtype());

                    unsupportedFeatures = true;
                }
            }

            if (instruction.type() == Instruction.Type.L3MODIFICATION) {
                if (this.unsupportedL3mod.contains(((L3ModificationInstruction) instruction).subtype())) {
                    log.warn("HP V3800 Driver - unsupported L3MODIFICATION instruction {}",
                            ((L3ModificationInstruction) instruction).subtype());

                    unsupportedFeatures = true;
                }
            }
        }

        return unsupportedFeatures;
    }

    @Override
    protected int tableIdForForwardingObjective(TrafficSelector selector, TrafficTreatment treatment) {
        boolean hardwareProcess = true;

        log.debug("HP V3800 Driver - Evaluating the ForwardingObjective for proper TableID");

        //Check criteria supported in hardware
        for (Criterion criterion : selector.criteria()) {

            if (!this.hardwareCriteria.contains(criterion.type())) {
                log.warn("HP V3800 Driver - criterion {} only supported in SOFTWARE", criterion.type());

                hardwareProcess = false;
                break;
            }

            //HP3800 does not support hardware match on ETH_TYPE of value TYPE_VLAN
            if (criterion.type() == Criterion.Type.ETH_TYPE) {

                if (((EthTypeCriterion) criterion).ethType().toShort() == Ethernet.TYPE_VLAN) {
                    log.warn("HP V3800 Driver -  ETH_TYPE == VLAN (0x8100) is only supported in software");

                    hardwareProcess = false;
                    break;
                }
            }

        }

        //Check if a CLEAR action is included
        if (treatment.clearedDeferred()) {
            log.warn("HP V3800 Driver - CLEAR action only supported in SOFTWARE");

            hardwareProcess = false;
        }

        //If criteria can be processed in hardware, then check treatment
        if (hardwareProcess) {
            for (Instruction instruction : treatment.allInstructions()) {

                //Check if the instruction type is contained in the hardware instruction
                if (!this.hardwareInstructions.contains(instruction.type())) {
                    log.warn("HP V3800 Driver - instruction {} only supported in SOFTWARE", instruction.type());

                    hardwareProcess = false;
                    break;
                }

                /** If output is CONTROLLER_PORT the flow entry could be installed in hardware
                 * but is anyway processed in software because OPENFLOW header has to be added
                 */
                if (instruction.type() == Instruction.Type.OUTPUT) {
                    if (((Instructions.OutputInstruction) instruction).port() == PortNumber.CONTROLLER) {
                        log.warn("HP V3800 Driver - Forwarding to CONTROLLER only supported in software");

                        hardwareProcess = false;
                        break;
                    }
                }

                //Check if the specific L2MODIFICATION.subtype is supported in hardware
                if (instruction.type() == Instruction.Type.L2MODIFICATION) {

                    if (!this.hardwareInstructionsL2mod.contains(((L2ModificationInstruction) instruction).subtype())) {
                        log.warn("HP V3800 Driver - L2MODIFICATION.subtype {} only supported in SOFTWARE",
                                ((L2ModificationInstruction) instruction).subtype());

                        hardwareProcess = false;
                        break;
                    }
                }

                //Check if the specific GROUP addressed in the instruction is:
                // --- installed in the device
                // --- type ALL
                // TODO --- check if all the buckets contains one and only one output action
                if (instruction.type() == Instruction.Type.GROUP) {
                    boolean groupInstalled = false;

                    GroupId groupId = ((Instructions.GroupInstruction) instruction).groupId();

                    Iterable<Group> groupsOnDevice = groupService.getGroups(deviceId);

                    for (Group group : groupsOnDevice) {

                        if ((group.state() == Group.GroupState.ADDED) && (group.id().equals(groupId))) {
                            groupInstalled = true;

                            if (group.type() != Group.Type.ALL) {
                                log.warn("HP V3800 Driver - group type {} only supported in SOFTWARE",
                                        group.type().toString());
                                hardwareProcess = false;
                            }

                            break;
                        }
                    }

                    if (!groupInstalled) {
                        log.warn("HP V3800 Driver - referenced group is not installed on the device.");
                        hardwareProcess = false;
                    }
                }
            }
        }

        if (hardwareProcess) {
            log.warn("HP V3800 Driver - This flow rule is supported in HARDWARE");
            return HP_HARDWARE_TABLE;
        } else {
            //TODO: create a specific flow in table 100 to redirect selected traffic on table 200

            log.warn("HP V3800 Driver - This flow rule is only supported in SOFTWARE");
            return HP_SOFTWARE_TABLE;
        }
    }

    @Override
    public void filter(FilteringObjective filter) {
        log.error("Unsupported FilteringObjective: : filtering method send");
    }

    @Override
    protected FlowRule.Builder processEthFilter(FilteringObjective filt, EthCriterion eth, PortCriterion port) {
        log.error("Unsupported FilteringObjective: processEthFilter invoked");
        return null;
    }

    @Override
    protected FlowRule.Builder processVlanFilter(FilteringObjective filt, VlanIdCriterion vlan, PortCriterion port) {
        log.error("Unsupported FilteringObjective: processVlanFilter invoked");
        return null;
    }

    @Override
    protected FlowRule.Builder processIpFilter(FilteringObjective filt, IPCriterion ip, PortCriterion port) {
        log.error("Unsupported FilteringObjective: processIpFilter invoked");
        return null;
    }
}
