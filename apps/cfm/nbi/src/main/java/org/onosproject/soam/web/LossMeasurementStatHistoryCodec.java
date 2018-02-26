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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementStatHistory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encode and decode to/from JSON to LossMeasurementStatHistory object.
 */
public class LossMeasurementStatHistoryCodec extends JsonCodec<LossMeasurementStatHistory> {

    @Override
    public ObjectNode encode(LossMeasurementStatHistory lmHistory, CodecContext context) {
        checkNotNull(lmHistory, "LM history cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("historyId", String.valueOf(lmHistory.historyStatsId()))
                .put("endTime", lmHistory.endTime().toString());
        ObjectNode resultAbstract = new LossMeasurementStatCodec().encode(lmHistory, context);
        result.setAll(resultAbstract);
        return result;
    }

    @Override
    public ArrayNode encode(Iterable<LossMeasurementStatHistory> historyEntities, CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        historyEntities.forEach(history -> an.add(encode(history, context)));
        return an;
    }
}
