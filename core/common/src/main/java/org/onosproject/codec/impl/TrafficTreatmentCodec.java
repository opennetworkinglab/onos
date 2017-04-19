/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;

import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Traffic treatment codec.
 */
public final class TrafficTreatmentCodec extends JsonCodec<TrafficTreatment> {
    private static final String INSTRUCTIONS = "instructions";
    private static final String DEFERRED = "deferred";
    private static final String CLEAR_DEFERRED = "clearDeferred";

    @Override
    public ObjectNode encode(TrafficTreatment treatment, CodecContext context) {
        checkNotNull(treatment, "Traffic treatment cannot be null");

        final ObjectNode result = context.mapper().createObjectNode();
        final ArrayNode jsonInstructions = result.putArray(INSTRUCTIONS);

        final JsonCodec<Instruction> instructionCodec =
                context.codec(Instruction.class);

        for (final Instruction instruction : treatment.immediate()) {
            jsonInstructions.add(instructionCodec.encode(instruction, context));
        }

        if (treatment.metered() != null) {
            jsonInstructions.add(instructionCodec.encode(treatment.metered(), context));
        }
        if (treatment.tableTransition() != null) {
            jsonInstructions.add(instructionCodec.encode(treatment.tableTransition(), context));
        }
        if (treatment.clearedDeferred()) {
            result.put(CLEAR_DEFERRED, true);
        }

        final ArrayNode jsonDeferred = result.putArray(DEFERRED);

        for (final Instruction instruction : treatment.deferred()) {
            jsonDeferred.add(instructionCodec.encode(instruction, context));
        }

        return result;
    }

    @Override
    public TrafficTreatment decode(ObjectNode json, CodecContext context) {
        final JsonCodec<Instruction> instructionsCodec =
                context.codec(Instruction.class);

        JsonNode instructionsJson = json.get(INSTRUCTIONS);
        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        if (instructionsJson != null) {
            IntStream.range(0, instructionsJson.size())
                    .forEach(i -> builder.add(
                            instructionsCodec.decode(get(instructionsJson, i),
                                    context)));
        }

        JsonNode clearDeferred = json.get(CLEAR_DEFERRED);
        if (clearDeferred != null && clearDeferred.asBoolean(false)) {
            builder.wipeDeferred();
        }

        JsonNode deferredJson = json.get(DEFERRED);
        if (deferredJson != null) {
            IntStream.range(0, deferredJson.size())
            .forEach(i -> builder.deferred().add(
                    instructionsCodec.decode(get(deferredJson, i),
                            context)));
        }

        return builder.build();
    }
}
