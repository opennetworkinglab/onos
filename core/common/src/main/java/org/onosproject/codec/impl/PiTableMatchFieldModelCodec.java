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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.pi.model.PiHeaderFieldTypeModel;
import org.onosproject.net.pi.model.PiHeaderModel;
import org.onosproject.net.pi.model.PiTableMatchFieldModel;

/**
 * Codec for PiTableMatchFieldModel.
 */
public class PiTableMatchFieldModelCodec extends JsonCodec<PiTableMatchFieldModel> {
    private static final String MATCH_TYPE = "matchType";
    private static final String HEADER = "header";
    private static final String FIELD = "field";
    @Override
    public ObjectNode encode(PiTableMatchFieldModel tableMatchField, CodecContext context) {
        ObjectNode result = context.mapper().createObjectNode();
        result.put(MATCH_TYPE, tableMatchField.matchType().toString());
        PiHeaderModel header = tableMatchField.field().header();
        PiHeaderFieldTypeModel field = tableMatchField.field().type();
        ObjectNode headerData = context.encode(header, PiHeaderModel.class);
        ObjectNode headerFieldData = context.encode(field, PiHeaderFieldTypeModel.class);
        result.set(HEADER, headerData);
        result.set(FIELD, headerFieldData);
        return result;
    }
}
