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
package org.onosproject.soam.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;

import java.util.Iterator;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encode and decode to/from JSON to LossMeasurementEntry object.
 */
public class LmEntryCodec extends JsonCodec<LossMeasurementEntry> {

    @Override
    public ObjectNode encode(LossMeasurementEntry lm, CodecContext context) {
        checkNotNull(lm, "LM cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("lmId", lm.lmId().toString());

        if (lm.measuredForwardFlr() != null) {
            result.put("measuredForwardFlr", lm.measuredForwardFlr().percentValue());
        }
        if (lm.measuredBackwardFlr() != null) {
            result.put("measuredBackwardFlr", lm.measuredBackwardFlr().percentValue());
        }
        if (lm.measuredAvailabilityForwardStatus() != null) {
            result.put("measuredAvailabilityForwardStatus",
                    lm.measuredAvailabilityForwardStatus().name());
        }
        if (lm.measuredAvailabilityBackwardStatus() != null) {
            result.put("measuredAvailabilityBackwardStatus",
                    lm.measuredAvailabilityBackwardStatus().name());
        }
        if (lm.measuredForwardLastTransitionTime() != null) {
            result.put("measuredForwardLastTransitionTime",
                    lm.measuredForwardLastTransitionTime().toString());
        }
        if (lm.measuredBackwardLastTransitionTime() != null) {
            result.put("measuredBackwardLastTransitionTime",
                    lm.measuredBackwardLastTransitionTime().toString());
        }

        ObjectNode lmAttrs = new LmCreateCodec().encode(lm, context);
        Iterator<Entry<String, JsonNode>> elements = lmAttrs.fields();
        while (elements.hasNext()) {
            Entry<String, JsonNode> element = elements.next();
            result.set(element.getKey(), element.getValue());
        }

        if (lm.measurementCurrent() != null) {
            result.set("measurementCurrent", new LossMeasurementStatCurrentCodec()
                    .encode(lm.measurementCurrent(), context));
        }

        if (lm.measurementHistories() != null) {
            result.set("measurementHistories", new LossMeasurementStatHistoryCodec()
                    .encode(lm.measurementHistories(), context));
        }

        if (lm.availabilityCurrent() != null) {
            result.set("availabilityCurrent", new LossAvailabilityStatCurrentCodec()
                    .encode(lm.availabilityCurrent(), context));
        }

        if (lm.availabilityHistories() != null) {
            result.set("availabilityHistories", new LossAvailabilityStatHistoryCodec()
                    .encode(lm.availabilityHistories(), context));
        }

        return result;
    }

    @Override
    public ArrayNode encode(Iterable<LossMeasurementEntry> lmEntities, CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        lmEntities.forEach(dm -> an.add(encode(dm, context)));
        return an;
    }
}
