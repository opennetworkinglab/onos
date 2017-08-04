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
package org.onosproject.ovsdb.rfc.utils;

import java.util.Map;
import java.util.Set;

import org.onosproject.ovsdb.rfc.notation.OvsdbMap;
import org.onosproject.ovsdb.rfc.notation.OvsdbSet;
import org.onosproject.ovsdb.rfc.notation.RefTableRow;
import org.onosproject.ovsdb.rfc.notation.Uuid;
import org.onosproject.ovsdb.rfc.schema.type.AtomicColumnType;
import org.onosproject.ovsdb.rfc.schema.type.BaseType;
import org.onosproject.ovsdb.rfc.schema.type.BooleanBaseType;
import org.onosproject.ovsdb.rfc.schema.type.ColumnType;
import org.onosproject.ovsdb.rfc.schema.type.IntegerBaseType;
import org.onosproject.ovsdb.rfc.schema.type.KeyValuedColumnType;
import org.onosproject.ovsdb.rfc.schema.type.RealBaseType;
import org.onosproject.ovsdb.rfc.schema.type.StringBaseType;
import org.onosproject.ovsdb.rfc.schema.type.UuidBaseType;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Object value utility class.
 */
public final class TransValueUtil {

    /**
     * Constructs a TransValueUtil object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully.
     * This class should not be instantiated.
     */
    private TransValueUtil() {
    }

    /**
     * if the type is Set, convert into OvsdbSet, if Map, convert into OvsdbMap.
     * @param value Object
     * @return Object
     */
    public static Object getFormatData(Object value) {
        if (value instanceof Map) {
            return OvsdbMap.ovsdbMap((Map) value);
        } else if (value instanceof Set) {
            return OvsdbSet.ovsdbSet((Set) value);
        } else {
            return value;
        }
    }

    /**
     * Transform JsonNode to corresponding value.
     * @param json the ColumnType JsonNode
     * @param columnType AtomicColumnType or KeyValuedColumnType
     * @return Object OvsdbMap or OvsdbSet
     */
    public static Object getValueFromJson(JsonNode json, ColumnType columnType) {
        if (columnType instanceof AtomicColumnType) {
            AtomicColumnType atoType = (AtomicColumnType) columnType;
            return getValueFromAtoType(json, atoType);
        } else if (columnType instanceof KeyValuedColumnType) {
            KeyValuedColumnType kvType = (KeyValuedColumnType) columnType;
            return getValueFromKvType(json, kvType);
        }
        return null;
    }

    /**
     * Convert AtomicColumnType JsonNode into OvsdbSet value.
     * @param json AtomicColumnType JsonNode
     * @param atoType AtomicColumnType entity
     * @return Object OvsdbSet or the value of JsonNode
     */
    private static Object getValueFromAtoType(JsonNode json, AtomicColumnType atoType) {
        BaseType baseType = atoType.baseType();
        // If "min" or "max" is not specified, If "min" is not 1 or "max" is not
        // 1, or both, and "value" is not specified, the type is a set of scalar
        // type "key". Refer to RFC 7047, Section 3.2 <type>.
        if (atoType.min() != atoType.max()) {
            Set set = Sets.newHashSet();
            if (json.isArray()) {
                if (json.size() == 2) {
                    if (json.get(0).isTextual() && "set".equals(json.get(0).asText())) {
                        for (JsonNode node : json.get(1)) {
                            set.add(transToValue(node, baseType));
                        }
                    } else {
                        set.add(transToValue(json, baseType));
                    }
                }
            } else {
                set.add(transToValue(json, baseType));
            }
            return OvsdbSet.ovsdbSet(set);
        } else {
            return transToValue(json, baseType);
        }
    }

    /**
     * Convert KeyValuedColumnType JsonNode into OvsdbMap value.
     * @param json KeyValuedColumnType JsonNode
     * @param kvType KeyValuedColumnType entity
     * @return Object OvsdbMap
     */
    private static Object getValueFromKvType(JsonNode json, KeyValuedColumnType kvType) {
        if (json.isArray()) {
            if (json.size() == 2) {
                if (json.get(0).isTextual() && "map".equals(json.get(0).asText())) {
                    Map map = Maps.newHashMap();
                    for (JsonNode pairNode : json.get(1)) {
                        if (pairNode.isArray() && json.size() == 2) {
                            Object key = transToValue(pairNode.get(0), kvType.keyType());
                            Object value = transToValue(pairNode.get(1), kvType.valueType());
                            map.put(key, value);
                        }
                    }
                    return OvsdbMap.ovsdbMap(map);
                }
            }
        }
        return null;
    }

    /**
     * convert into value.
     * @param valueNode the BaseType JsonNode
     * @param baseType BooleanBaseType or IntegerBaseType or RealBaseType or
     *            StringBaseType or UuidBaseType
     * @return Object the value of JsonNode
     */
    public static Object transToValue(JsonNode valueNode, BaseType baseType) {
        if (baseType instanceof BooleanBaseType) {
            return valueNode.asBoolean();
        } else if (baseType instanceof IntegerBaseType) {
            return valueNode.asInt();
        } else if (baseType instanceof RealBaseType) {
            return valueNode.asDouble();
        } else if (baseType instanceof StringBaseType) {
            return valueNode.asText();
        } else if (baseType instanceof UuidBaseType) {
            if (valueNode.isArray()) {
                if (valueNode.size() == 2) {
                    if (valueNode.get(0).isTextual()
                            && ("uuid".equals(valueNode.get(0).asText()) || "named-uuid"
                                    .equals(valueNode.get(0).asText()))) {
                        return Uuid.uuid(valueNode.get(1).asText());
                    }
                }
            } else {
                return new RefTableRow(((UuidBaseType) baseType).getRefTable(), valueNode);
            }
        }
        return null;
    }
}
