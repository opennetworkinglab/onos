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

package org.onosproject.pipelines.fabric;

import com.google.common.collect.ImmutableList;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiPipelineInterpreter.PiInterpreterException;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;

import java.util.List;

import static java.lang.String.format;
import static org.onosproject.net.flow.instructions.Instruction.Type.L2MODIFICATION;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.ETH_DST;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.ETH_SRC;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_ID;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_PUSH;


final class FabricTreatmentInterpreter {
    private static final String INVALID_TREATMENT = "Invalid treatment for %s block: %s";

    // Hide default constructor
    protected FabricTreatmentInterpreter() {
    }

    /*
     * In Filtering block, we need to implement these actions:
     *
     * push_internal_vlan
     * set_vlan
     * nop
     *
     * Unsupported, using PiAction directly:
     * set_forwarding_type
     */

    static PiAction mapFilteringTreatment(TrafficTreatment treatment)
            throws PiInterpreterException {
        List<Instruction> instructions = treatment.allInstructions();
        Instruction noActInst = Instructions.createNoAction();
        if (instructions.isEmpty() || instructions.contains(noActInst)) {
            // nop
            return PiAction.builder()
                    .withId(FabricConstants.ACT_NOP_ID)
                    .build();
        }

        L2ModificationInstruction.ModVlanHeaderInstruction pushVlanInst = null;
        ModVlanIdInstruction setVlanInst = null;

        for (Instruction inst : instructions) {
            if (inst.type() == L2MODIFICATION) {
                L2ModificationInstruction l2Inst = (L2ModificationInstruction) inst;

                if (l2Inst.subtype() == VLAN_PUSH) {
                    pushVlanInst = (L2ModificationInstruction.ModVlanHeaderInstruction) l2Inst;

                } else if (l2Inst.subtype() == VLAN_ID) {
                    setVlanInst = (ModVlanIdInstruction) l2Inst;
                }
            }
        }

        if (setVlanInst == null) {
            throw new PiInterpreterException(format(INVALID_TREATMENT, "filtering", treatment));
        }

        VlanId vlanId = setVlanInst.vlanId();
        PiActionParam param = new PiActionParam(FabricConstants.ACT_PRM_NEW_VLAN_ID_ID,
                                                ImmutableByteSequence.copyFrom(vlanId.toShort()));
        PiActionId actionId;
        if (pushVlanInst != null) {
            // push_internal_vlan
            actionId = FabricConstants.ACT_PUSH_INTERNAL_VLAN_ID;
        } else {
            actionId = FabricConstants.ACT_SET_VLAN_ID;
        }

        // set_vlan
        return PiAction.builder()
                .withId(actionId)
                .withParameter(param)
                .build();
    }

    /*
     * In forwarding block, we need to implement these actions:
     * duplicate_to_controller
     *
     * Unsupported, using PiAction directly:
     * set_next_id
     * push_mpls_and_next_v4
     * push_mpls_and_next_v6
     */

    public static PiAction mapForwardingTreatment(TrafficTreatment treatment)
            throws PiInterpreterException {
        List<Instruction> insts = treatment.allInstructions();
        OutputInstruction outInst = insts.stream()
                .filter(inst -> inst.type() == OUTPUT)
                .map(inst -> (OutputInstruction) inst)
                .findFirst()
                .orElse(null);

        if (outInst == null) {
            throw new PiInterpreterException(format(INVALID_TREATMENT, "forwarding", treatment));
        }

        PortNumber portNumber = outInst.port();
        if (!portNumber.equals(PortNumber.CONTROLLER)) {
            throw new PiInterpreterException(format("Unsupported port number %s," +
                                                            "supports punt action only",
                                                    portNumber));
        }

        return PiAction.builder()
                .withId(FabricConstants.ACT_DUPLICATE_TO_CONTROLLER_ID)
                .build();
    }

    /*
     * In Next block, we need to implement these actions:
     * output
     * output_ecmp
     * set_vlan_output
     *
     * Unsupported, using PiAction directly:
     * set_mcast_group
     */

    public static PiAction mapNextTreatment(TrafficTreatment treatment)
            throws PiInterpreterException {
        List<Instruction> insts = treatment.allInstructions();
        OutputInstruction outInst = null;
        ModEtherInstruction modEthDstInst = null;
        ModEtherInstruction modEthSrcInst = null;
        ModVlanIdInstruction modVlanIdInst = null;

        // TODO: use matrix to store the combination of instruction
        for (Instruction inst : insts) {
            switch (inst.type()) {
                case L2MODIFICATION:
                    L2ModificationInstruction l2Inst = (L2ModificationInstruction) inst;

                    if (l2Inst.subtype() == ETH_SRC) {
                        modEthSrcInst = (ModEtherInstruction) l2Inst;
                    }

                    if (l2Inst.subtype() == ETH_DST) {
                        modEthDstInst = (ModEtherInstruction) l2Inst;
                    }

                    if (l2Inst.subtype() == VLAN_ID) {
                        modVlanIdInst = (ModVlanIdInstruction) l2Inst;
                    }
                    break;
                case OUTPUT:
                    outInst = (OutputInstruction) inst;
                    break;
                default:
                    break;
            }
        }

        if (outInst == null) {
            throw new PiInterpreterException(format(INVALID_TREATMENT, "next", treatment));
        }
        short portNum = (short) outInst.port().toLong();
        PiActionParam portNumParam = new PiActionParam(FabricConstants.ACT_PRM_PORT_NUM_ID,
                                                       ImmutableByteSequence.copyFrom(portNum));
        if (modEthDstInst == null && modEthSrcInst == null) {
            if (modVlanIdInst != null) {
                VlanId vlanId = modVlanIdInst.vlanId();
                PiActionParam vlanParam =
                        new PiActionParam(FabricConstants.ACT_PRM_NEW_VLAN_ID_ID,
                                          ImmutableByteSequence.copyFrom(vlanId.toShort()));
                // set_vlan_output
                return PiAction.builder()
                        .withId(FabricConstants.ACT_SET_VLAN_OUTPUT_ID)
                        .withParameters(ImmutableList.of(portNumParam, vlanParam))
                        .build();
            } else {
                // output
                return PiAction.builder()
                        .withId(FabricConstants.ACT_OUTPUT_ID)
                        .withParameter(portNumParam)
                        .build();
            }
        }

        if (modEthDstInst != null && modEthSrcInst != null) {
            // output and rewrite src/dst mac
            MacAddress srcMac = modEthSrcInst.mac();
            MacAddress dstMac = modEthDstInst.mac();
            PiActionParam srcMacParam = new PiActionParam(FabricConstants.ACT_PRM_SMAC_ID,
                                                          ImmutableByteSequence.copyFrom(srcMac.toBytes()));
            PiActionParam dstMacParam = new PiActionParam(FabricConstants.ACT_PRM_DMAC_ID,
                                                          ImmutableByteSequence.copyFrom(dstMac.toBytes()));
            return PiAction.builder()
                    .withId(FabricConstants.ACT_L3_ROUTING_ID)
                    .withParameters(ImmutableList.of(portNumParam,
                                                     srcMacParam,
                                                     dstMacParam))
                    .build();
        }

        throw new PiInterpreterException(format(INVALID_TREATMENT, "next", treatment));
    }
}
