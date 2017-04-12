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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.mapping.DefaultMappingTreatment;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.instructions.MappingInstruction;

import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mapping treatment codec.
 */
public final class MappingTreatmentCodec extends JsonCodec<MappingTreatment> {

    private static final String INSTRUCTIONS = "instructions";
    private static final String ADDRESS = "address";

    @Override
    public ObjectNode encode(MappingTreatment treatment, CodecContext context) {
        checkNotNull(treatment, "Mapping treatment cannot be null");

        final ObjectNode result = context.mapper().createObjectNode();
        final ArrayNode jsonInstructions = result.putArray(INSTRUCTIONS);

        final JsonCodec<MappingInstruction> instructionCodec =
                context.codec(MappingInstruction.class);

        final JsonCodec<MappingAddress> addressCodec =
                context.codec(MappingAddress.class);

        for (final MappingInstruction instruction : treatment.instructions()) {
            jsonInstructions.add(instructionCodec.encode(instruction, context));
        }

        result.set(ADDRESS, addressCodec.encode(treatment.address(), context));

        return result;
    }

    @Override
    public MappingTreatment decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        final JsonCodec<MappingInstruction> instructionCodec =
                context.codec(MappingInstruction.class);

        JsonNode instructionJson = json.get(INSTRUCTIONS);
        MappingTreatment.Builder builder = DefaultMappingTreatment.builder();

        if (instructionJson != null) {
            IntStream.range(0, instructionJson.size())
                    .forEach(i -> builder.add(
                            instructionCodec.decode(get(instructionJson, i),
                                    context)));
        }

        ObjectNode addressJson = get(json, ADDRESS);
        if (addressJson != null) {
            final JsonCodec<MappingAddress> addressCodec =
                    context.codec(MappingAddress.class);
            builder.withAddress(addressCodec.decode(addressJson, context));
        }

        return builder.build();
    }
}
