/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.mapping.instructions.MulticastMappingInstruction;
import org.onosproject.mapping.instructions.UnicastMappingInstruction;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * JSON encoding of MappingInstructions.
 */
public final class EncodeMappingInstructionCodecHelper {
    protected static final Logger log = getLogger(EncodeMappingInstructionCodecHelper.class);
    private final MappingInstruction instruction;
    private final CodecContext context;

    /**
     * Creates a mapping instruction object encoder.
     *
     * @param instruction mapping instruction to encode
     * @param context     codec context for the encoding
     */
    public EncodeMappingInstructionCodecHelper(MappingInstruction instruction,
                                               CodecContext context) {
        this.instruction = instruction;
        this.context = context;
    }

    /**
     * Encodes an unicast mapping instruction.
     *
     * @param result json node that the mapping instruction
     *               attributes are added to
     */
    private void encodeUnicast(ObjectNode result) {
        UnicastMappingInstruction unicastInstruction =
                (UnicastMappingInstruction) instruction;
        result.put(MappingInstructionCodec.SUBTYPE, unicastInstruction.subtype().name());

        switch (unicastInstruction.subtype()) {
            case WEIGHT:
                UnicastMappingInstruction.WeightMappingInstruction wmi =
                        (UnicastMappingInstruction.WeightMappingInstruction)
                                                            unicastInstruction;
                result.put(MappingInstructionCodec.UNICAST_WEIGHT, wmi.weight());
                break;
            case PRIORITY:
                UnicastMappingInstruction.PriorityMappingInstruction pmi =
                        (UnicastMappingInstruction.PriorityMappingInstruction)
                                                            unicastInstruction;
                result.put(MappingInstructionCodec.UNICAST_PRIORITY, pmi.priority());
                break;
            default:
                log.info("Cannot convert unicast subtype of {}",
                                                unicastInstruction.subtype());
        }
    }

    /**
     * Encodes a multicast mapping instruction.
     *
     * @param result json node that the mapping instruction
     *               attributes are added to
     */
    private void encodeMulticast(ObjectNode result) {
        MulticastMappingInstruction multicastMappingInstruction =
                (MulticastMappingInstruction) instruction;
        result.put(MappingInstructionCodec.SUBTYPE, multicastMappingInstruction.subtype().name());

        switch (multicastMappingInstruction.subtype()) {
            case WEIGHT:
                MulticastMappingInstruction.WeightMappingInstruction wmi =
                        (MulticastMappingInstruction.WeightMappingInstruction)
                                                    multicastMappingInstruction;
                result.put(MappingInstructionCodec.MULTICAST_WEIGHT, wmi.weight());
                break;
            case PRIORITY:
                MulticastMappingInstruction.PriorityMappingInstruction pmi =
                        (MulticastMappingInstruction.PriorityMappingInstruction)
                                                    multicastMappingInstruction;
                result.put(MappingInstructionCodec.MULTICAST_PRIORITY, pmi.priority());
                break;
            default:
                log.info("Cannot convert multicast subtype of {}",
                                            multicastMappingInstruction.subtype());
        }
    }

    /**
     * Encodes the given mapping instruction into JSON.
     *
     * @return JSON object node representing the mapping instruction.
     */
    public ObjectNode encode() {
        final ObjectNode result = context.mapper().createObjectNode()
                .put(MappingInstructionCodec.TYPE, instruction.type().toString());

        switch (instruction.type()) {
            case UNICAST:
                encodeUnicast(result);
                break;
            case MULTICAST:
                encodeMulticast(result);
                break;
            default:
                log.info("Cannot convert mapping instruction type of {}", instruction.type());
                break;
        }
        return result;
    }
}
