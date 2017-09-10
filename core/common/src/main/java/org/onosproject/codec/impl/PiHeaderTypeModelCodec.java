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
import org.onosproject.net.pi.model.PiHeaderFieldTypeModel;
import org.onosproject.net.pi.model.PiHeaderTypeModel;

/**
 * Codec for PiHeaderTypeModel.
 */
public class PiHeaderTypeModelCodec extends JsonCodec<PiHeaderTypeModel> {
    private static final String NAME = "name";
    private static final String FIELDS = "fields";

    @Override
    public ObjectNode encode(PiHeaderTypeModel headerType, CodecContext context) {
        ObjectNode result = context.mapper().createObjectNode();
        result.put(NAME, headerType.name());
        ArrayNode fields = result.putArray(FIELDS);

        headerType.fields().forEach(field -> {
            fields.add(context.encode(field, PiHeaderFieldTypeModel.class));
        });

        return result;
    }
}
