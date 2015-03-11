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
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.Intent;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Connectivity intent codec.
 */
public final class ConnectivityIntentCodec extends JsonCodec<ConnectivityIntent> {

    @Override
    public ObjectNode encode(ConnectivityIntent intent, CodecContext context) {
        checkNotNull(intent, "Connectivity intent cannot be null");

        final JsonCodec<Intent> intentCodec = context.codec(Intent.class);
        final ObjectNode result = intentCodec.encode(intent, context);

        if (intent.selector() != null) {
            final JsonCodec<TrafficSelector> selectorCodec =
                    context.codec(TrafficSelector.class);
            result.set("selector", selectorCodec.encode(intent.selector(), context));
        }

        if (intent.treatment() != null) {
            final JsonCodec<TrafficTreatment> treatmentCodec =
                    context.codec(TrafficTreatment.class);
            result.set("treatment", treatmentCodec.encode(intent.treatment(), context));
        }

        result.put("priority", intent.priority());

        if (intent.constraints() != null) {
            final ArrayNode jsonConstraints = result.putArray("constraints");

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
}
