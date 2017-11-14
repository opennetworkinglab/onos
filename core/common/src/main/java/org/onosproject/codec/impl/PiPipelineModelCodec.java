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

package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiTableModel;

/**
 * Codec for PiPipelineModel.
 */
public class PiPipelineModelCodec extends JsonCodec<PiPipelineModel> {
    private static final String ACTIONS = "actions";
    private static final String TABLES = "tables";

    @Override
    public ObjectNode encode(PiPipelineModel pipeline, CodecContext context) {
        ObjectNode result = context.mapper().createObjectNode();
        ArrayNode actions = result.putArray(ACTIONS);
        pipeline.tables()
                .stream()
                .flatMap(piTableModel -> piTableModel.actions().stream())
                .distinct()
                .map(action -> context.encode(action, PiActionModel.class))
                .forEach(actions::add);
        ArrayNode tables = result.putArray(TABLES);
        pipeline.tables().stream()
                .map(table -> context.encode(table, PiTableModel.class))
                .forEach(tables::add);
        return result;
    }
}
