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

import java.util.ArrayList;
import java.util.List;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate.MeasurementOption;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Encode and decode to/from JSON to MeasurementOption object.
 */
public class DmMeasurementOptionCodec extends JsonCodec<MeasurementOption> {

    @Override
    public ArrayNode encode(Iterable<MeasurementOption> entities,
            CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        entities.forEach(node -> an.add(node.name()));

        return an;
    }

    @Override
    public List<MeasurementOption> decode(ArrayNode json,
            CodecContext context) {
        if (json == null) {
            return null;
        }
        List<MeasurementOption> moList = new ArrayList<>();
        json.forEach(node -> moList.add(MeasurementOption.valueOf(node.asText())));
        return moList;
    }

}
