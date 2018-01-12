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
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.mapping.DefaultMappingValue;
import org.onosproject.mapping.MappingTreatment;
import org.onosproject.mapping.MappingValue;
import org.onosproject.mapping.actions.MappingAction;

import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mapping value codec.
 */
public final class MappingValueCodec extends JsonCodec<MappingValue> {

    static final String ACTION = "action";
    static final String TREATMENTS = "treatments";

    @Override
    public ObjectNode encode(MappingValue value, CodecContext context) {
        checkNotNull(value, "Mapping value cannot be null");

        final ObjectNode result = context.mapper().createObjectNode();
        final ArrayNode jsonTreatments = result.putArray(TREATMENTS);

        final JsonCodec<MappingTreatment> treatmentCodec =
                context.codec(MappingTreatment.class);

        final JsonCodec<MappingAction> actionCodec =
                context.codec(MappingAction.class);

        for (final MappingTreatment treatment : value.treatments()) {
            jsonTreatments.add(treatmentCodec.encode(treatment, context));
        }

        result.set(ACTION, actionCodec.encode(value.action(), context));

        return result;
    }

    @Override
    public MappingValue decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        final JsonCodec<MappingTreatment> treatmentCodec =
                context.codec(MappingTreatment.class);

        JsonNode treatmentJson = json.get(TREATMENTS);
        MappingValue.Builder builder = DefaultMappingValue.builder();

        if (treatmentJson != null) {
            IntStream.range(0, treatmentJson.size())
                    .forEach(i -> builder.add(
                            treatmentCodec.decode(get(treatmentJson, i),
                                    context)));
        }

        ObjectNode actionJson = get(json, ACTION);
        if (actionJson != null) {
            final JsonCodec<MappingAction> actionCodec =
                    context.codec(MappingAction.class);
            builder.withAction(actionCodec.decode(actionJson, context));
        }

        return builder.build();
    }
}
