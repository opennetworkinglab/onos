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
import org.onosproject.mapping.instructions.MappingInstructions;
import org.onosproject.mapping.instructions.MulticastMappingInstruction;
import org.onosproject.mapping.instructions.UnicastMappingInstruction;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Decoding portion of the mapping instruction codec.
 */
public final class DecodeMappingInstructionCodecHelper {
    private final ObjectNode json;
    private final CodecContext context;

    /**
     * Creates a decode mapping instruction codec object.
     *
     * @param json    JSON object to decode
     * @param context codec context
     */
    public DecodeMappingInstructionCodecHelper(ObjectNode json, CodecContext context) {
        this.json = json;
        this.context = context;
    }

    /**
     * Decodes an unicast mapping instruction.
     *
     * @return mapping instruction object decoded from the JSON
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private MappingInstruction decodeUnicast() {
        String subType = nullIsIllegal(json.get(MappingInstructionCodec.SUBTYPE),
                MappingInstructionCodec.SUBTYPE + MappingInstructionCodec.ERROR_MESSAGE).asText();

        if (subType.equals(UnicastMappingInstruction.UnicastType.WEIGHT.name())) {
            int weight = nullIsIllegal(json.get(MappingInstructionCodec.UNICAST_WEIGHT),
                    MappingInstructionCodec.UNICAST_WEIGHT +
                            MappingInstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            return MappingInstructions.unicastWeight(weight);
        } else if (subType.equals(UnicastMappingInstruction.UnicastType.PRIORITY.name())) {
            int priority = nullIsIllegal(json.get(MappingInstructionCodec.UNICAST_PRIORITY),
                    MappingInstructionCodec.UNICAST_PRIORITY +
                            MappingInstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            return MappingInstructions.unicastPriority(priority);
        }
        throw new IllegalArgumentException("Unicast MappingInstruction subtype "
                + subType + " is not supported");
    }

    /**
     * Decodes a multicast mapping instruction.
     *
     * @return mapping instruction object decoded from the JSON
     * @throws IllegalArgumentException if the JSON is invalid
     */
    private MappingInstruction decodeMulticast() {
        String subType = nullIsIllegal(json.get(MappingInstructionCodec.SUBTYPE),
                MappingInstructionCodec.SUBTYPE + MappingInstructionCodec.ERROR_MESSAGE).asText();

        if (subType.equals(MulticastMappingInstruction.MulticastType.WEIGHT.name())) {
            int weight = nullIsIllegal(json.get(MappingInstructionCodec.MULTICAST_WEIGHT),
                    MappingInstructionCodec.MULTICAST_WEIGHT +
                            MappingInstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            return MappingInstructions.multicastWeight(weight);
        } else if (subType.equals(MulticastMappingInstruction.MulticastType.PRIORITY.name())) {
            int priority = nullIsIllegal(json.get(MappingInstructionCodec.MULTICAST_PRIORITY),
                    MappingInstructionCodec.MULTICAST_PRIORITY +
                            MappingInstructionCodec.MISSING_MEMBER_MESSAGE).asInt();
            return MappingInstructions.multicastPriority(priority);
        }
        throw new IllegalArgumentException("Multicast MappingInstruction subtype "
                + subType + " is not supported");
    }

    /**
     * Decodes the JSON into a mapping instruction object.
     *
     * @return MappingInstruction object
     * @throws IllegalArgumentException if the JSON is invalid
     */
    public MappingInstruction decode() {
        String type = nullIsIllegal(json.get(MappingInstructionCodec.TYPE),
                MappingInstructionCodec.TYPE + MappingInstructionCodec.ERROR_MESSAGE).asText();

        if (type.equals(MappingInstruction.Type.UNICAST.name())) {
            return decodeUnicast();
        } else if (type.equals(MappingInstruction.Type.MULTICAST.name())) {
            return decodeMulticast();
        }
        throw new IllegalArgumentException("MappingInstruction type "
                + type + " is not supported");
    }
}
