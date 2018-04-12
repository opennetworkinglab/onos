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
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModMplsLabelInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiPipelineInterpreter.PiInterpreterException;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.slf4j.Logger;

import java.util.List;

import static java.lang.String.format;
import static org.onosproject.net.flow.instructions.Instruction.Type.L2MODIFICATION;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_ID;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.L2SubType.VLAN_PUSH;
import static org.slf4j.LoggerFactory.getLogger;


final class FabricTreatmentInterpreter {
    private static final Logger log = getLogger(FabricTreatmentInterpreter.class);
    private static final String INVALID_TREATMENT = "Invalid treatment for %s block: %s";
    private static final PiAction NOP = PiAction.builder()
            .withId(FabricConstants.NOP)
            .build();

    private static final PiAction POP_VLAN = PiAction.builder()
            .withId(FabricConstants.FABRIC_EGRESS_EGRESS_NEXT_POP_VLAN)
            .build();

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
            return NOP;
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
        PiActionParam param = new PiActionParam(FabricConstants.NEW_VLAN_ID,
                                                ImmutableByteSequence.copyFrom(vlanId.toShort()));
        PiActionId actionId;
        if (pushVlanInst != null) {
            // push_internal_vlan
            actionId = FabricConstants.FABRIC_INGRESS_FILTERING_PUSH_INTERNAL_VLAN;
        } else {
            actionId = FabricConstants.FABRIC_INGRESS_FILTERING_SET_VLAN;
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
        // Empty treatment, generate table entry with no action
        if (treatment.equals(DefaultTrafficTreatment.emptyTreatment())) {
            return null;
        }
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
                .withId(FabricConstants.FABRIC_INGRESS_FORWARDING_DUPLICATE_TO_CONTROLLER)
                .build();
    }

    /*
     * In Next block, we need to implement these actions:
     * output
     * set_vlan_output
     * l3_routing
     * l3_routing_vlan
     * mpls_routing_v4
     *
     * Unsupported, need to find a way to implement it
     * mpls_routing_v6
     */

    public static PiAction mapNextTreatment(TrafficTreatment treatment)
            throws PiInterpreterException {
        // TODO: refactor this method
        List<Instruction> insts = treatment.allInstructions();
        OutputInstruction outInst = null;
        ModEtherInstruction modEthDstInst = null;
        ModEtherInstruction modEthSrcInst = null;
        ModVlanIdInstruction modVlanIdInst = null;
        ModMplsLabelInstruction modMplsInst = null;

        // TODO: add NextFunctionType (like ForwardingFunctionType)
        for (Instruction inst : insts) {
            switch (inst.type()) {
                case L2MODIFICATION:
                    L2ModificationInstruction l2Inst = (L2ModificationInstruction) inst;
                    switch (l2Inst.subtype()) {
                        case ETH_SRC:
                            modEthSrcInst = (ModEtherInstruction) l2Inst;
                            break;
                        case ETH_DST:
                            modEthDstInst = (ModEtherInstruction) l2Inst;
                            break;
                        case VLAN_ID:
                            modVlanIdInst = (ModVlanIdInstruction) l2Inst;
                            break;
                        case MPLS_LABEL:
                            modMplsInst = (ModMplsLabelInstruction) l2Inst;
                            break;
                        default:
                            log.warn("Unsupported l2 instruction sub type: {}", l2Inst.subtype());
                            break;
                    }
                    break;
                case OUTPUT:
                    outInst = (OutputInstruction) inst;
                    break;
                default:
                    log.warn("Unsupported instruction sub type: {}", inst.type());
                    break;
            }
        }

        if (outInst == null) {
            // for vlan_meta table only
            if (modVlanIdInst != null) {
                // set_vlan
                VlanId vlanId = modVlanIdInst.vlanId();
                PiActionParam newVlanParam =
                        new PiActionParam(FabricConstants.NEW_VLAN_ID,
                                          ImmutableByteSequence.copyFrom(vlanId.toShort()));
                return PiAction.builder()
                        .withId(FabricConstants.FABRIC_INGRESS_NEXT_SET_VLAN)
                        .withParameter(newVlanParam)
                        .build();
            } else {
                throw new PiInterpreterException(format(INVALID_TREATMENT, "next", treatment));
            }
        }

        short portNum = (short) outInst.port().toLong();
        PiActionParam portNumParam = new PiActionParam(FabricConstants.PORT_NUM,
                                                       ImmutableByteSequence.copyFrom(portNum));
        if (modEthDstInst == null && modEthSrcInst == null) {
            if (modVlanIdInst != null) {
                VlanId vlanId = modVlanIdInst.vlanId();
                PiActionParam vlanParam =
                        new PiActionParam(FabricConstants.NEW_VLAN_ID,
                                          ImmutableByteSequence.copyFrom(vlanId.toShort()));
                // set_vlan_output
                return PiAction.builder()
                        .withId(FabricConstants.FABRIC_INGRESS_NEXT_SET_VLAN_OUTPUT)
                        .withParameters(ImmutableList.of(portNumParam, vlanParam))
                        .build();
            } else {
                // output
                return PiAction.builder()
                        .withId(FabricConstants.FABRIC_INGRESS_NEXT_OUTPUT)
                        .withParameter(portNumParam)
                        .build();
            }
        }

        if (modEthDstInst != null && modEthSrcInst != null) {
            MacAddress srcMac = modEthSrcInst.mac();
            MacAddress dstMac = modEthDstInst.mac();
            PiActionParam srcMacParam = new PiActionParam(FabricConstants.SMAC,
                                                          ImmutableByteSequence.copyFrom(srcMac.toBytes()));
            PiActionParam dstMacParam = new PiActionParam(FabricConstants.DMAC,
                                                          ImmutableByteSequence.copyFrom(dstMac.toBytes()));

            if (modMplsInst != null) {
                // MPLS routing
                MplsLabel mplsLabel = modMplsInst.label();
                try {
                    ImmutableByteSequence mplsValue =
                            ImmutableByteSequence.copyFrom(mplsLabel.toInt()).fit(20);
                    PiActionParam mplsParam = new PiActionParam(FabricConstants.LABEL, mplsValue);
                    return PiAction.builder()
                            // FIXME: fins a way to determine v4 or v6
                            .withId(FabricConstants.FABRIC_INGRESS_NEXT_MPLS_ROUTING_V4)
                            .withParameters(ImmutableList.of(portNumParam,
                                                             srcMacParam,
                                                             dstMacParam,
                                                             mplsParam))
                            .build();
                } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
                    // Basically this won't happened because we already limited
                    // size of mpls value to 20 bits (0xFFFFF)
                    throw new PiInterpreterException(format(INVALID_TREATMENT, "next", treatment));
                }
            }

            if (modVlanIdInst != null) {
                VlanId vlanId = modVlanIdInst.vlanId();
                PiActionParam vlanParam =
                        new PiActionParam(FabricConstants.NEW_VLAN_ID,
                                          ImmutableByteSequence.copyFrom(vlanId.toShort()));
                // L3 routing and set VLAN
                return PiAction.builder()
                        .withId(FabricConstants.FABRIC_INGRESS_NEXT_L3_ROUTING_VLAN)
                        .withParameters(ImmutableList.of(srcMacParam, dstMacParam, portNumParam, vlanParam))
                        .build();
            } else {
                // L3 routing
                return PiAction.builder()
                        .withId(FabricConstants.FABRIC_INGRESS_NEXT_L3_ROUTING)
                        .withParameters(ImmutableList.of(portNumParam,
                                                         srcMacParam,
                                                         dstMacParam))
                        .build();
            }
        }

        throw new PiInterpreterException(format(INVALID_TREATMENT, "next", treatment));
    }

    public static PiAction mapEgressNextTreatment(TrafficTreatment treatment) {
        // Pop VLAN action for now, may add new action to this control block in the future.
        return treatment.allInstructions()
                .stream()
                .filter(inst -> inst.type() == Instruction.Type.L2MODIFICATION)
                .map(inst -> (L2ModificationInstruction) inst)
                .filter(inst -> inst.subtype() == L2ModificationInstruction.L2SubType.VLAN_POP)
                .findFirst()
                .map(inst -> POP_VLAN)
                .orElse(NOP);
    }
}
