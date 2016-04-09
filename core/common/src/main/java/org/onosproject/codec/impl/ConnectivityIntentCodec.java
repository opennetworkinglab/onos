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

import java.util.ArrayList;
import java.util.stream.IntStream;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Connectivity intent codec.
 */
public final class ConnectivityIntentCodec extends JsonCodec<ConnectivityIntent> {

    private static final String CONSTRAINTS = "constraints";
    private static final String SELECTOR = "selector";
    private static final String TREATMENT = "treatment";

    @Override
    public ObjectNode encode(ConnectivityIntent intent, CodecContext context) {
        checkNotNull(intent, "Connectivity intent cannot be null");

        final JsonCodec<Intent> intentCodec = context.codec(Intent.class);
        final ObjectNode result = intentCodec.encode(intent, context);

        if (intent.selector() != null) {
            final JsonCodec<TrafficSelector> selectorCodec =
                    context.codec(TrafficSelector.class);
            result.set(SELECTOR, selectorCodec.encode(intent.selector(), context));
        }

        if (intent.treatment() != null) {
            final JsonCodec<TrafficTreatment> treatmentCodec =
                    context.codec(TrafficTreatment.class);
            result.set(TREATMENT, treatmentCodec.encode(intent.treatment(), context));
        }

        result.put(IntentCodec.PRIORITY, intent.priority());

        if (intent.constraints() != null) {
            final ArrayNode jsonConstraints = result.putArray(CONSTRAINTS);

            if (intent.constraints() != null) {
                final JsonCodec<Constraint> constraintCodec =
                        context.codec(Constraint.class);
                for (final Constraint constraint : intent.constraints()) {
                    final ObjectNode constraintNode =
                            constraintCodec.encode(constraint, context);
                    jsonConstraints.add(constraintNode);
                }
            }
        }

        return result;
    }

    /**
     * Extracts connectivity intent specific attributes from a JSON object
     * and adds them to a builder.
     *
     * @param json root JSON object
     * @param context code context
     * @param builder builder to use for storing the attributes. Constraints,
     *                selector and treatment are modified by this call.
     */
    public static void intentAttributes(ObjectNode json, CodecContext context,
                                        ConnectivityIntent.Builder builder) {
        JsonNode constraintsJson = json.get(CONSTRAINTS);
        if (constraintsJson != null) {
            JsonCodec<Constraint> constraintsCodec = context.codec(Constraint.class);
            ArrayList<Constraint> constraints = new ArrayList<>(constraintsJson.size());
            IntStream.range(0, constraintsJson.size())
                    .forEach(i -> constraints.add(
                            constraintsCodec.decode(get(constraintsJson, i),
                                    context)));
            builder.constraints(constraints);
        }

        ObjectNode selectorJson = get(json, SELECTOR);
        if (selectorJson != null) {
            JsonCodec<TrafficSelector> selectorCodec = context.codec(TrafficSelector.class);
            TrafficSelector selector = selectorCodec.decode(selectorJson, context);
            builder.selector(selector);
        }

        ObjectNode treatmentJson = get(json, TREATMENT);
        if (treatmentJson != null) {
            JsonCodec<TrafficTreatment> treatmentCodec = context.codec(TrafficTreatment.class);
            TrafficTreatment treatment = treatmentCodec.decode(treatmentJson, context);
            builder.treatment(treatment);
        }
    }
}
