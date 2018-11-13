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
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver for HP hybrid switches employing V1 hardware module.
 *
 * - HP2910
 * - HP3500, tested
 * - HP5400, depends on switch configuration if "allow-v1-modules" it operates as V1
 * - HP6200
 * - HP6600
 * - HP8200, depends on switch configuration if "allow-v1-modules" it operates as V1
 *
 * Refer to the device manual to check unsupported features and features supported in hardware
 */

public class HPPipelineV1 extends AbstractHPPipeline {

    private Logger log = getLogger(getClass());

    @Override
    protected FlowRule.Builder setDefaultTableIdForFlowObjective(FlowRule.Builder ruleBuilder) {
        log.debug("HP V1 Driver - Setting default table id to software table {}", HP_SOFTWARE_TABLE);

        return ruleBuilder.forTable(HP_SOFTWARE_TABLE);
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

        //Initialize unsupported L2MODIFICATION actions
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
        log.debug("HP V1 Driver - Initializing hardware supported criteria");

        hardwareCriteria.add(Criterion.Type.IN_PORT);
        hardwareCriteria.add(Criterion.Type.VLAN_VID);

        //Match in hardware is supported only for ETH_TYPE == IPv4 (0x0800)
        hardwareCriteria.add(Criterion.Type.ETH_TYPE);

        hardwareCriteria.add(Criterion.Type.IPV4_SRC);
        hardwareCriteria.add(Criterion.Type.IPV4_DST);
        hardwareCriteria.add(Criterion.Type.IP_PROTO);
        hardwareCriteria.add(Criterion.Type.IP_DSCP);
        hardwareCriteria.add(Criterion.Type.TCP_SRC);
        hardwareCriteria.add(Criterion.Type.TCP_DST);
    }

    @Override
    protected void initHardwareInstructions() {
        log.debug("HP V1 Driver - Initializing hardware supported instructions");

        //If the output is on CONTROLLER PORT the rule is processed in software
        hardwareInstructions.add(Instruction.Type.OUTPUT);

        //Only modification of VLAN priority (VLAN_PCP) is supported in hardware
        hardwareInstructions.add(Instruction.Type.L2MODIFICATION);
        hardwareInstructionsL2mod.add(L2ModificationInstruction.L2SubType.VLAN_PCP);

        //TODO also L3MODIFICATION of IP_DSCP is supported in hardware
    }

    //Return TRUE if ForwardingObjective fwd includes unsupported features
    @Override
    protected boolean checkUnSupportedFeatures(TrafficSelector selector, TrafficTreatment treatment) {
        boolean unsupportedFeatures = false;

        for (Criterion criterion : selector.criteria()) {
            if (this.unsupportedCriteria.contains(criterion.type())) {
                log.warn("HP V1 Driver - unsupported criteria {}", criterion.type());

                unsupportedFeatures = true;
            }
        }

        for (Instruction instruction : treatment.allInstructions()) {
            if (this.unsupportedInstructions.contains(instruction.type())) {
                log.warn("HP V1 Driver - unsupported instruction {}", instruction.type());

                unsupportedFeatures = true;
            }

            if (instruction.type() == Instruction.Type.L2MODIFICATION) {
                if (this.unsupportedL2mod.contains(((L2ModificationInstruction) instruction).subtype())) {
                    log.warn("HP V1 Driver - unsupported L2MODIFICATION instruction {}",
                            ((L2ModificationInstruction) instruction).subtype());

                    unsupportedFeatures = true;
                }
            }

            if (instruction.type() == Instruction.Type.L3MODIFICATION) {
                if (this.unsupportedL3mod.contains(((L3ModificationInstruction) instruction).subtype())) {
                    log.warn("HP V1 Driver - unsupported L3MODIFICATION instruction {}",
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

        log.debug("HP V1 Driver - Evaluating the ForwardingObjective for proper TableID");

        //Check criteria supported in hardware
        for (Criterion criterion : selector.criteria()) {

            if (!this.hardwareCriteria.contains(criterion.type())) {
                log.warn("HP V1 Driver - criterion {} only supported in SOFTWARE", criterion.type());

                hardwareProcess = false;
                break;
            }

            //HP3500 supports hardware match on ETH_TYPE only with value TYPE_IPV4
            if (criterion.type() == Criterion.Type.ETH_TYPE) {

                if (((EthTypeCriterion) criterion).ethType().toShort() != Ethernet.TYPE_IPV4) {
                    log.warn("HP V1 Driver - only ETH_TYPE == IPv4 (0x0800) is supported in hardware");

                    hardwareProcess = false;
                    break;
                }
            }

            //HP3500 supports IN_PORT criterion in hardware only if associated with ETH_TYPE criterion
            if (criterion.type() == Criterion.Type.IN_PORT) {
                hardwareProcess = false;

                for (Criterion requiredCriterion : selector.criteria()) {
                    if (requiredCriterion.type() == Criterion.Type.ETH_TYPE) {
                        hardwareProcess = true;
                    }
                }

                if (!hardwareProcess) {
                    log.warn("HP V1 Driver - IN_PORT criterion without ETH_TYPE is not supported in hardware");

                    break;
                }
            }

        }

        //Check if a CLEAR action is included
        if (treatment.clearedDeferred()) {
            log.warn("HP V1 Driver - CLEAR action only supported in SOFTWARE");

            hardwareProcess = false;
        }

        //If criteria can be processed in hardware, then check treatment
        if (hardwareProcess) {

            for (Instruction instruction : treatment.allInstructions()) {

                //Check if the instruction type is contained in the hardware instruction
                if (!this.hardwareInstructions.contains(instruction.type())) {
                    log.warn("HP V1 Driver - instruction {} only supported in SOFTWARE", instruction.type());

                    hardwareProcess = false;
                    break;
                }

                /** All GROUP types are supported in software by V2 switches
                 */

                /** If output is CONTROLLER_PORT the flow entry could be installed in hardware
                 * but is anyway processed in software because openflow header has to be added
                 */
                if (instruction.type() == Instruction.Type.OUTPUT) {
                    if (((Instructions.OutputInstruction) instruction).port() == PortNumber.CONTROLLER) {
                        log.warn("HP V1 Driver - Forwarding to CONTROLLER only supported in software");

                        hardwareProcess = false;
                        break;
                    }
                }

                /** Only L2MODIFICATION supported in hardware is MODIFY VLAN_PRIORITY.
                 * Check if the specific L2MODIFICATION.subtype is supported in hardware
                 */
                if (instruction.type() == Instruction.Type.L2MODIFICATION) {

                    if (!this.hardwareInstructionsL2mod.contains(((L2ModificationInstruction) instruction).subtype())) {
                        log.warn("HP V1 Driver - L2MODIFICATION.subtype {} only supported in SOFTWARE",
                                ((L2ModificationInstruction) instruction).subtype());

                        hardwareProcess = false;
                        break;
                    }

                }
            }
        }

        if (hardwareProcess) {
            log.warn("HP V1 Driver - This flow rule is supported in HARDWARE");
            return HP_HARDWARE_TABLE;
        } else {
            //TODO: create a specific flow in table 100 to redirect selected traffic on table 200

            log.warn("HP V1 Driver - This flow rule is only supported in SOFTWARE");
            return HP_SOFTWARE_TABLE;
        }

    }

    @Override
    public void filter(FilteringObjective filter) {
        log.error("HP V1 Driver - Unsupported FilteringObjective: filtering method send");
    }

    @Override
    protected FlowRule.Builder processEthFilter(FilteringObjective filt, EthCriterion eth, PortCriterion port) {
        log.error("HP V1 Driver - Unsupported FilteringObjective: processEthFilter invoked");
        return null;
    }

    @Override
    protected FlowRule.Builder processVlanFilter(FilteringObjective filt, VlanIdCriterion vlan, PortCriterion port) {
        log.error("HP V1 Driver - Unsupported FilteringObjective: processVlanFilter invoked");
        return null;
    }

    @Override
    protected FlowRule.Builder processIpFilter(FilteringObjective filt, IPCriterion ip, PortCriterion port) {
        log.error("HP V1 Driver - Unsupported FilteringObjective: processIpFilter invoked");
        return null;
    }
}

