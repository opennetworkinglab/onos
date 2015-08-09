/*
 * Copyright 2015 Open Networking Laboratory
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

import org.onosproject.ovsdb.rfc.error.TypedSchemaException;
import org.onosproject.ovsdb.rfc.utils.ObjectMapperUtil;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * ColumnType Factory class.
 */
public final class ColumnTypeFactory {

    /**
     * Constructs a ColumnTypeFactory object.
     * This class should not be instantiated.
     */
    private ColumnTypeFactory() {
    }

    /**
     * Those Json's key/value pairs.
     */
    public enum Type {
        KEY("key"), VALUE("value");

        private final String type;

        private Type(String type) {
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
     * JsonNode like "flow_tables":{"type":{"key":{"maxInteger":254,"minInteger":0,"type":
     * "integer"},"min":0,"value":{"type":"uuid","refTable":"Flow_Table"},"max":
     * "unlimited"}}.
     * @param json the ColumnType JsonNode
     * @return ColumnType
     */
    public static ColumnType getColumnTypeFromJson(JsonNode json) {
        if (!json.isObject() || !json.has(Type.VALUE.type())) {
            return createAtomicColumnType(json);
        } else if (!json.isValueNode() && json.has(Type.VALUE.type())) {
            return createKeyValuedColumnType(json);
        }
        throw new TypedSchemaException("could not find the right column type :"
                + ObjectMapperUtil.convertToString(json));
    }

    /**
     * Create AtomicColumnType entity.
     * @param json JsonNode
     * @return AtomicColumnType entity
     */
    private static AtomicColumnType createAtomicColumnType(JsonNode json) {
        BaseType baseType = BaseTypeFactory
                .getBaseTypeFromJson(json, Type.KEY.type());
        int min = 1;
        int max = 1;
        JsonNode node = json.get("min");
        if (node != null) {
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
        BaseType keyType = BaseTypeFactory.getBaseTypeFromJson(json,
                                                               Type.KEY.type());
        BaseType valueType = BaseTypeFactory
                .getBaseTypeFromJson(json, Type.VALUE.type());
        int min = 1;
        int max = 1;
        JsonNode node = json.get("min");
        if (node != null) {
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
