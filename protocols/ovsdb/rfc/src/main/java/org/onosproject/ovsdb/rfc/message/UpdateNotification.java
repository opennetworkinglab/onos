/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.ovsdb.rfc.message;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.ovsdb.rfc.notation.json.UpdateNotificationConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * The "update" notification is sent by the server to the client to report
 * changes in tables that are being monitored following a "monitor" request. The
 * "params" of the result JsonNode.
 */
@JsonDeserialize(converter = UpdateNotificationConverter.class)
public final class UpdateNotification {
    private final Object jsonValue;
    private final JsonNode tbUpdatesJsonNode;

    /**
     * Constructs a UpdateNotification object.
     * @param jsonValue the "json-value" in "params" of the result JsonNode
     * @param tbUpdatesJsonNode the "table-updates" in "params" of the result JsonNode
     */
    public UpdateNotification(Object jsonValue, JsonNode tbUpdatesJsonNode) {
        checkNotNull(jsonValue, "jsonValue cannot be null");
        checkNotNull(tbUpdatesJsonNode, "tablebUpdates JsonNode cannot be null");
        this.jsonValue = jsonValue;
        this.tbUpdatesJsonNode = tbUpdatesJsonNode;
    }

    /**
     * Return context.
     * @return context
     */
    public Object jsonValue() {
        return jsonValue;
    }

    /**
     * Return tbUpdatesJsonNode.
     * @return tbUpdatesJsonNode
     */
    public JsonNode tbUpdatesJsonNode() {
        return tbUpdatesJsonNode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonValue, tbUpdatesJsonNode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UpdateNotification) {
            final UpdateNotification other = (UpdateNotification) obj;
            return Objects.equals(this.jsonValue, other.jsonValue)
                    && Objects.equals(this.tbUpdatesJsonNode,
                                      other.tbUpdatesJsonNode);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("jsonValue", jsonValue)
                .add("tbUpdatesJsonNode", tbUpdatesJsonNode).toString();
    }
}
