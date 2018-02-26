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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.Map.Entry;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to DelayMeasurementEntry object.
 */
public class DmEntryCodec extends JsonCodec<DelayMeasurementEntry> {

    private static final String DM_ID = "dmId";
    private static final String SESSION_STATUS = "sessionStatus";
    private static final String FRAME_DELAY_TWO_WAY = "frameDelayTwoWay";
    private static final String FRAME_DELAY_FORWARD = "frameDelayForward";
    private static final String FRAME_DELAY_BACKWARD = "frameDelayBackward";
    private static final String INTER_FRAME_DELAY_VARIATION_TWO_WAY = "interFrameDelayVariationTwoWay";
    private static final String INTER_FRAME_DELAY_VARIATION_FORWARD = "interFrameDelayVariationForward";
    private static final String INTER_FRAME_DELAY_VARIATION_BACKWARD = "interFrameDelayVariationBackward";
    private static final String CURRENT = "current";
    private static final String HISTORIC = "historic";

    @Override
    public ObjectNode encode(DelayMeasurementEntry dm, CodecContext context) {
        checkNotNull(dm, "DM cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(DM_ID, dm.dmId().toString());

        if (dm.sessionStatus() != null) {
            result.put(SESSION_STATUS, dm.sessionStatus().name());
        }
        if (dm.frameDelayTwoWay() != null) {
            result.put(FRAME_DELAY_TWO_WAY, dm.frameDelayTwoWay().toString());
        }
        if (dm.frameDelayForward() != null) {
            result.put(FRAME_DELAY_FORWARD, dm.frameDelayForward().toString());
        }
        if (dm.frameDelayBackward() != null) {
            result.put(FRAME_DELAY_BACKWARD, dm.frameDelayBackward().toString());
        }
        if (dm.interFrameDelayVariationTwoWay() != null) {
            result.put(INTER_FRAME_DELAY_VARIATION_TWO_WAY,
                    dm.interFrameDelayVariationTwoWay().toString());
        }
        if (dm.interFrameDelayVariationForward() != null) {
            result.put(INTER_FRAME_DELAY_VARIATION_FORWARD,
                    dm.interFrameDelayVariationForward().toString());
        }
        if (dm.interFrameDelayVariationBackward() != null) {
            result.put(INTER_FRAME_DELAY_VARIATION_BACKWARD,
                    dm.interFrameDelayVariationBackward().toString());
        }

        ObjectNode dmAttrs = new DmCreateCodec().encode(dm, context);
        Iterator<Entry<String, JsonNode>> elements = dmAttrs.fields();
        while (elements.hasNext()) {
            Entry<String, JsonNode> element = elements.next();
            result.set(element.getKey(), element.getValue());
        }

        if (dm.currentResult() != null) {
            result.set(CURRENT, new DelayMeasurementStatCurrentCodec()
                    .encode(dm.currentResult(), context));
        }

        if (dm.historicalResults() != null) {
            result.set(HISTORIC, new DelayMeasurementStatHistoryCodec()
                    .encode(dm.historicalResults(), context));
        }

        return result;
    }

    @Override
    public ArrayNode encode(Iterable<DelayMeasurementEntry> dmEntities, CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        dmEntities.forEach(dm -> an.add(encode(dm, context)));
        return an;
    }
}
