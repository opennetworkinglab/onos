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
import org.onosproject.codec.JsonCodec;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mapping instruction codec.
 */
public final class MappingInstructionCodec extends JsonCodec<MappingInstruction> {

    protected static final Logger log = LoggerFactory.getLogger(MappingInstructionCodec.class);

    protected static final String TYPE = "type";
    protected static final String SUBTYPE = "subtype";
    protected static final String UNICAST_WEIGHT = "unicastWeight";
    protected static final String UNICAST_PRIORITY = "unicastPriority";
    protected static final String MULTICAST_WEIGHT = "multicastWeight";
    protected static final String MULTICAST_PRIORITY = "multicastPriority";

    protected static final String MISSING_MEMBER_MESSAGE =
                                        " member is required in Instruction";
    protected static final String ERROR_MESSAGE =
                                        " not specified in Instruction";

    @Override
    public ObjectNode encode(MappingInstruction instruction, CodecContext context) {
        checkNotNull(instruction, "Mapping instruction cannot be null");

        return new EncodeMappingInstructionCodecHelper(instruction, context).encode();
    }

    @Override
    public MappingInstruction decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        return new DecodeMappingInstructionCodecHelper(json, context).decode();
    }
}
