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
package org.onosproject.codec.impl;

import org.onosproject.codec.CodecContext;
import org.onosproject.net.OchSignal;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.onosproject.net.flow.instructions.L4ModificationInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * JSON encoding of Instructions.
 */
public final class EncodeInstructionCodecHelper {
    protected static final Logger log = LoggerFactory.getLogger(EncodeInstructionCodecHelper.class);
    private final Instruction instruction;
    private final CodecContext context;

    /**
     * Creates an instruction object encoder.
     *
     * @param instruction instruction to encode
     * @param context codec context for the encoding
     */
    public EncodeInstructionCodecHelper(Instruction instruction, CodecContext context) {
        this.instruction = instruction;
        this.context = context;
    }


    /**
     * Encode an L0 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     */
    private void encodeL0(ObjectNode result) {
        L0ModificationInstruction instruction =
                (L0ModificationInstruction) this.instruction;
        result.put(InstructionCodec.SUBTYPE, instruction.subtype().name());

        switch (instruction.subtype()) {
            case LAMBDA:
                final L0ModificationInstruction.ModLambdaInstruction modLambdaInstruction =
                        (L0ModificationInstruction.ModLambdaInstruction) instruction;
                result.put(InstructionCodec.LAMBDA, modLambdaInstruction.lambda());
                break;

            case OCH:
                L0ModificationInstruction.ModOchSignalInstruction ochSignalInstruction =
                        (L0ModificationInstruction.ModOchSignalInstruction) instruction;
                OchSignal ochSignal = ochSignalInstruction.lambda();
                result.put(InstructionCodec.GRID_TYPE, ochSignal.gridType().name());
                result.put(InstructionCodec.CHANNEL_SPACING, ochSignal.channelSpacing().name());
                result.put(InstructionCodec.SPACING_MULTIPLIER, ochSignal.spacingMultiplier());
                result.put(InstructionCodec.SLOT_GRANULARITY, ochSignal.slotGranularity());
                break;

            default:
                log.info("Cannot convert L0 subtype of {}", instruction.subtype());
        }
    }

    /**
     * Encode an L2 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     */
    private void encodeL2(ObjectNode result) {
        L2ModificationInstruction instruction =
                (L2ModificationInstruction) this.instruction;
        result.put(InstructionCodec.SUBTYPE, instruction.subtype().name());

        switch (instruction.subtype()) {
            case ETH_SRC:
            case ETH_DST:
                final L2ModificationInstruction.ModEtherInstruction modEtherInstruction =
                        (L2ModificationInstruction.ModEtherInstruction) instruction;
                result.put(InstructionCodec.MAC, modEtherInstruction.mac().toString());
                break;

            case VLAN_ID:
                final L2ModificationInstruction.ModVlanIdInstruction modVlanIdInstruction =
                        (L2ModificationInstruction.ModVlanIdInstruction) instruction;
                result.put(InstructionCodec.VLAN_ID, modVlanIdInstruction.vlanId().toShort());
                break;

            case VLAN_PCP:
                final L2ModificationInstruction.ModVlanPcpInstruction modVlanPcpInstruction =
                        (L2ModificationInstruction.ModVlanPcpInstruction) instruction;
                result.put(InstructionCodec.VLAN_PCP, modVlanPcpInstruction.vlanPcp());
                break;

            case MPLS_LABEL:
                final L2ModificationInstruction.ModMplsLabelInstruction modMplsLabelInstruction =
                        (L2ModificationInstruction.ModMplsLabelInstruction) instruction;
                result.put(InstructionCodec.MPLS_LABEL, modMplsLabelInstruction.mplsLabel().toInt());
                break;

            case MPLS_PUSH:
                final L2ModificationInstruction.PushHeaderInstructions pushHeaderInstructions =
                        (L2ModificationInstruction.PushHeaderInstructions) instruction;

                result.put(InstructionCodec.ETHERNET_TYPE,
                           pushHeaderInstructions.ethernetType().toShort());
                break;

            case TUNNEL_ID:
                final L2ModificationInstruction.ModTunnelIdInstruction modTunnelIdInstruction =
                        (L2ModificationInstruction.ModTunnelIdInstruction) instruction;
                result.put(InstructionCodec.TUNNEL_ID, modTunnelIdInstruction.tunnelId());
                break;

            default:
                log.info("Cannot convert L2 subtype of {}", instruction.subtype());
                break;
        }
    }

    /**
     * Encode an L3 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     */
    private void encodeL3(ObjectNode result) {
        L3ModificationInstruction instruction =
                (L3ModificationInstruction) this.instruction;
        result.put(InstructionCodec.SUBTYPE, instruction.subtype().name());
        switch (instruction.subtype()) {
            case IPV4_SRC:
            case IPV4_DST:
            case IPV6_SRC:
            case IPV6_DST:
                final L3ModificationInstruction.ModIPInstruction modIPInstruction =
                        (L3ModificationInstruction.ModIPInstruction) instruction;
                result.put(InstructionCodec.IP, modIPInstruction.ip().toString());
                break;

            case IPV6_FLABEL:
                final L3ModificationInstruction.ModIPv6FlowLabelInstruction
                        modFlowLabelInstruction =
                        (L3ModificationInstruction.ModIPv6FlowLabelInstruction) instruction;
                result.put(InstructionCodec.FLOW_LABEL, modFlowLabelInstruction.flowLabel());
                break;

            default:
                log.info("Cannot convert L3 subtype of {}", instruction.subtype());
                break;
        }
    }

    /**
     * Encode a L4 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     */
    private void encodeL4(ObjectNode result) {
        L4ModificationInstruction instruction =
                (L4ModificationInstruction) this.instruction;
        result.put(InstructionCodec.SUBTYPE, instruction.subtype().name());
        switch (instruction.subtype()) {
            case TCP_DST:
            case TCP_SRC:
                final L4ModificationInstruction.ModTransportPortInstruction modTcpPortInstruction =
                        (L4ModificationInstruction.ModTransportPortInstruction) instruction;
                result.put(InstructionCodec.TCP_PORT, modTcpPortInstruction.port().toInt());
                break;

            case UDP_DST:
            case UDP_SRC:
                final L4ModificationInstruction.ModTransportPortInstruction modUdpPortInstruction =
                        (L4ModificationInstruction.ModTransportPortInstruction) instruction;
                result.put(InstructionCodec.UDP_PORT, modUdpPortInstruction.port().toInt());
                break;

            default:
                log.info("Cannot convert L4 subtype of {}", instruction.subtype());
                break;
        }
    }

    /**
     * Encodes the given instruction into JSON.
     *
     * @return JSON object node representing the instruction
     */
    public ObjectNode encode() {
        final ObjectNode result = context.mapper().createObjectNode()
                .put(InstructionCodec.TYPE, instruction.type().toString());

        switch (instruction.type()) {
            case OUTPUT:
                final Instructions.OutputInstruction outputInstruction =
                        (Instructions.OutputInstruction) instruction;
                result.put(InstructionCodec.PORT, outputInstruction.port().toLong());
                break;

            case DROP:
            case NOACTION:
                break;

            case L0MODIFICATION:
                encodeL0(result);
                break;

            case L2MODIFICATION:
                encodeL2(result);
                break;

            case L3MODIFICATION:
                encodeL3(result);
                break;

            case L4MODIFICATION:
                encodeL4(result);
                break;

            default:
                log.info("Cannot convert instruction type of {}", instruction.type());
                break;
        }
        return result;
    }

}
