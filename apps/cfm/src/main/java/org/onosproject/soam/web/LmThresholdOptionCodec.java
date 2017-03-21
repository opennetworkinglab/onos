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
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementThreshold.ThresholdOption;

import java.util.ArrayList;
import java.util.List;

/**
 * Encode and decode to/from JSON to ThresholdOption object.
 */
public class LmThresholdOptionCodec extends JsonCodec<ThresholdOption> {

    @Override
    public ArrayNode encode(Iterable<ThresholdOption> entities,
            CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        entities.forEach(node -> an.add(node.name()));

        return an;
    }

    @Override
    public List<ThresholdOption> decode(ArrayNode json,
            CodecContext context) {
        if (json == null) {
            return null;
        }
        List<ThresholdOption> moList = new ArrayList<>();
        json.forEach(node -> moList.add(ThresholdOption.valueOf(node.asText())));
        return moList;
    }

}
