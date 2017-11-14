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
import org.onosproject.net.pi.model.PiMatchFieldModel;
import org.onosproject.net.pi.model.PiTableModel;

/**
 * Codec for PiTableModel.
 */
public class PiTableModelCodec extends JsonCodec<PiTableModel> {
    private static final String NAME = "name";
    private static final String MAX_SIZE = "maxSize";
    private static final String HAS_COUNTERS = "hasCounters";
    private static final String SUPPORT_AGING = "supportAging";
    private static final String ACTIONS = "actions";
    private static final String MATCH_FIELDS = "matchFields";


    @Override
    public ObjectNode encode(PiTableModel table, CodecContext context) {

        ObjectNode result = context.mapper().createObjectNode();

        result.put(NAME, table.id().toString());
        result.put(MAX_SIZE, table.maxSize());
        result.put(HAS_COUNTERS, table.counters().size() > 0);
        result.put(SUPPORT_AGING, table.supportsAging());

        ArrayNode matchFields = result.putArray(MATCH_FIELDS);
        table.matchFields().forEach(matchField -> {
            ObjectNode matchFieldData =
                    context.encode(matchField, PiMatchFieldModel.class);
            matchFields.add(matchFieldData);
        });

        ArrayNode actions = result.putArray(ACTIONS);
        table.actions().forEach(action -> {
            actions.add(action.id().toString());
        });

        return result;
    }
}
