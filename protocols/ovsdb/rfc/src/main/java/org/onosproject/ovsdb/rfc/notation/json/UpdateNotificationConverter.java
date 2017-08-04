/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.rfc.notation.json;

import org.onosproject.ovsdb.rfc.message.UpdateNotification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.StdConverter;

/**
 * UpdateNotificationDeser Converter.
 */
public class UpdateNotificationConverter
        extends StdConverter<JsonNode, UpdateNotification> {

    @Override
    public UpdateNotification convert(JsonNode value) {
        return deserialize(value);
    }

    /**
     * JsonNode convert into UpdateNotification.
     * @param node the "params" node of UpdateNotification JsonNode
     */
    private UpdateNotification deserialize(JsonNode node) {
        if (node.isArray()) {
            if (node.size() == 2) {
                return new UpdateNotification(node.get(0).asText(), node.get(1));
            }
        }
        return null;
    }
}
