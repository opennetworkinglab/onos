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
package org.onosproject.ovsdb.rfc.schema.type;

import org.onosproject.ovsdb.rfc.exception.AbnormalJsonNodeException;
import org.onosproject.ovsdb.rfc.utils.ObjectMapperUtil;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * ColumnType Factory class.
 */
public final class ColumnTypeFactory {

    /**
     * Constructs a ColumnTypeFactory object. This class should not be
     * instantiated.
     */
    private ColumnTypeFactory() {
    }

    /**
     * Those Json's key/value pairs.
     */
    public enum Type {
        KEY("key"), VALUE("value");

        private final String type;

        Type(String type) {
            this.type = type;
        }

        /**
         * Returns the type for Type.
         * @return the type
         */
        public String type() {
            return type;
        }
    }

    /**
     * JsonNode like
     * "flow_tables":{"type":{"key":{"maxInteger":254,"minInteger":0,"type":
     * "integer"},"min":0,"value":{"type":"uuid","refTable":"Flow_Table"},"max":
     * "unlimited"}}.
     * @param columnTypeJson the ColumnType JsonNode
     * @return ColumnType
     */
    public static ColumnType getColumnTypeFromJson(JsonNode columnTypeJson) {
        if (!columnTypeJson.isObject() || !columnTypeJson.has(Type.VALUE.type())) {
            return createAtomicColumnType(columnTypeJson);
        } else if (!columnTypeJson.isValueNode() && columnTypeJson.has(Type.VALUE.type())) {
            return createKeyValuedColumnType(columnTypeJson);
        }
        String message = "Abnormal ColumnType JsonNode, it should be AtomicColumnType or KeyValuedColumnType"
                + ObjectMapperUtil.convertToString(columnTypeJson);
        throw new AbnormalJsonNodeException(message);
    }

    /**
     * Create AtomicColumnType entity.
     * @param json JsonNode
     * @return AtomicColumnType entity
     */
    private static AtomicColumnType createAtomicColumnType(JsonNode json) {
        BaseType baseType = BaseTypeFactory.getBaseTypeFromJson(json, Type.KEY.type());
        int min = 1;
        int max = 1;
        JsonNode node = json.get("min");
        if (node != null && node.isNumber()) {
            min = node.asInt();
        }
        node = json.get("max");
        if (node != null) {
            if (node.isNumber()) {
                max = node.asInt();
            } else if (node.isTextual() && "unlimited".equals(node.asText())) {
                max = Integer.MAX_VALUE;
            }
        }
        return new AtomicColumnType(baseType, min, max);
    }

    /**
     * Create KeyValuedColumnType entity.
     * @param json JsonNode
     * @return KeyValuedColumnType entity
     */
    private static KeyValuedColumnType createKeyValuedColumnType(JsonNode json) {
        BaseType keyType = BaseTypeFactory.getBaseTypeFromJson(json, Type.KEY.type());
        BaseType valueType = BaseTypeFactory.getBaseTypeFromJson(json, Type.VALUE.type());
        int min = 1;
        int max = 1;
        JsonNode node = json.get("min");
        if (node != null && node.isNumber()) {
            min = node.asInt();
        }
        node = json.get("max");
        if (node != null) {
            if (node.isNumber()) {
                max = node.asInt();
            } else if (node.isTextual() && "unlimited".equals(node.asText())) {
                max = Integer.MAX_VALUE;
            }
        }
        return new KeyValuedColumnType(keyType, valueType, min, max);
    }
}
