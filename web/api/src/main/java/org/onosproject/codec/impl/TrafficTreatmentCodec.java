/*
 * Copyright 2014 Open Networking Laboratory
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
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Traffic treatment codec.
 */
public class TrafficTreatmentCodec extends JsonCodec<TrafficTreatment> {
    @Override
    public ObjectNode encode(TrafficTreatment treatment, CodecContext context) {
        checkNotNull(treatment, "Traffic treatment cannot be null");

        final ObjectNode result = context.mapper().createObjectNode();
        final ArrayNode jsonInstructions = result.putArray("instructions");

        if (treatment.instructions() != null) {
            final JsonCodec<Instruction> instructionCodec =
                    context.codec(Instruction.class);
            for (final Instruction instruction : treatment.instructions()) {
                jsonInstructions.add(instructionCodec.encode(instruction, context));
            }
        }

        return result;
    }
}
