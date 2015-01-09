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

import org.onlab.packet.Ethernet;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L3ModificationInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instruction codec.
 */
public class InstructionCodec extends JsonCodec<Instruction> {

    protected static final Logger log = LoggerFactory.getLogger(InstructionCodec.class);

    /**
     * Encode an L0 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     * @param instruction The L0 instruction
     */
    private void encodeL0(ObjectNode result, L0ModificationInstruction instruction) {
        result.put("subtype", instruction.subtype().name());

        switch (instruction.subtype()) {
            case LAMBDA:
                final L0ModificationInstruction.ModLambdaInstruction modLambdaInstruction =
                        (L0ModificationInstruction.ModLambdaInstruction) instruction;
                result.put("lambda", modLambdaInstruction.lambda());
                break;

            default:
                log.info("Cannot convert L0 subtype of {}", instruction.subtype());
        }
    }

    /**
     * Encode an L2 modification instruction.
     *
     * @param result json node that the instruction attributes are added to
     * @param instruction The L2 instruction
     * @param context context of the request
     */
    private void encodeL2(ObjectNode result,
                          L2ModificationInstruction instruction,
                          CodecContext context) {
        result.put("subtype", instruction.subtype().name());

        switch (instruction.subtype()) {
            case ETH_SRC:
            case ETH_DST:
                final L2ModificationInstruction.ModEtherInstruction modEtherInstruction =
                        (L2ModificationInstruction.ModEtherInstruction) instruction;
                result.put("mac", modEtherInstruction.mac().toString());
                break;

            case VLAN_ID:
                final L2ModificationInstruction.ModVlanIdInstruction modVlanIdInstruction =
                        (L2ModificationInstruction.ModVlanIdInstruction) instruction;
                result.put("vlanId", modVlanIdInstruction.vlanId().toShort());
                break;

            case VLAN_PCP:
                final L2ModificationInstruction.ModVlanPcpInstruction modVlanPcpInstruction =
                        (L2ModificationInstruction.ModVlanPcpInstruction) instruction;
                result.put("vlanPcp", modVlanPcpInstruction.vlanPcp());
                break;

            case MPLS_LABEL:
                final L2ModificationInstruction.ModMplsLabelInstruction modMplsLabelInstruction =
                        (L2ModificationInstruction.ModMplsLabelInstruction) instruction;
                result.put("label", modMplsLabelInstruction.label());
                break;

            case MPLS_PUSH:
                final L2ModificationInstruction.PushHeaderInstructions pushHeaderInstructions =
                        (L2ModificationInstruction.PushHeaderInstructions) instruction;

                final JsonCodec<Ethernet> ethernetCodec =
                        context.codec(Ethernet.class);
                result.set("ethernetType",
                        ethernetCodec.encode(pushHeaderInstructions.ethernetType(),
                                context));
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
     * @param instruction The L3 instruction
     */
    private void encodeL3(ObjectNode result, L3ModificationInstruction instruction) {
        result.put("subtype", instruction.subtype().name());
        switch (instruction.subtype()) {
            case IP_SRC:
            case IP_DST:
                final L3ModificationInstruction.ModIPInstruction modIPInstruction =
                        (L3ModificationInstruction.ModIPInstruction) instruction;
                result.put("ip", modIPInstruction.ip().toString());
                break;

            default:
                log.info("Cannot convert L3 subtype of {}", instruction.subtype());
                break;
        }
    }

    @Override
    public ObjectNode encode(Instruction instruction, CodecContext context) {
        checkNotNull(instruction, "Instruction cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put("type", instruction.type().toString());


        switch (instruction.type()) {
            case OUTPUT:
                final Instructions.OutputInstruction outputInstruction =
                        (Instructions.OutputInstruction) instruction;
                result.put("port", outputInstruction.port().toLong());
                break;

            case DROP:
                break;

            case L0MODIFICATION:
                final L0ModificationInstruction l0ModificationInstruction =
                        (L0ModificationInstruction) instruction;
                encodeL0(result, l0ModificationInstruction);
                break;

            case L2MODIFICATION:
                final L2ModificationInstruction l2ModificationInstruction =
                        (L2ModificationInstruction) instruction;
                encodeL2(result, l2ModificationInstruction, context);
                break;

            case L3MODIFICATION:
                final L3ModificationInstruction l3ModificationInstruction =
                        (L3ModificationInstruction) instruction;
                encodeL3(result, l3ModificationInstruction);

            default:
                log.info("Cannot convert instruction type of {}", instruction.type());
                break;
        }
        return result;
    }
}
