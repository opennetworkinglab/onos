/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.onosproject.ovsdb.lib.notation.json;

import org.onosproject.ovsdb.lib.message.TableUpdates;
import org.onosproject.ovsdb.lib.message.UpdateNotification;
import org.onosproject.ovsdb.lib.notation.OvsdbMap;
import org.onosproject.ovsdb.lib.notation.OvsdbSet;
import org.onosproject.ovsdb.lib.notation.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdConverter;

public final class Converter {

    private Converter() {
        super();
    }

    static AtomDeser atomDeser = new AtomDeser();
    static MapDeser mapDeser = new MapDeser();
    static SetDeser setDeser = new SetDeser();
    static UpdateNotificationDeser unDeser = new UpdateNotificationDeser();

    public static class MapConverter
            extends StdConverter<JsonNode, OvsdbMap<Object, Object>> {
        @Override
        public OvsdbMap<Object, Object> convert(JsonNode value) {
            return mapDeser.deserialize(value);
        }
    }

    public static class SetConverter
            extends StdConverter<JsonNode, OvsdbSet<Object>> {
        @Override
        public OvsdbSet<Object> convert(JsonNode value) {
            return setDeser.deserialize(value);
        }
    }

    public static class UpdateNotificationConverter
            extends StdConverter<JsonNode, UpdateNotification> {
        @Override
        public UpdateNotification convert(JsonNode value) {
            return unDeser.deserialize(value);
        }
    }

    static class MapDeser {
        public OvsdbMap<Object, Object> deserialize(JsonNode node) {
            if (node.isArray()) {
                if (node.size() == 2) {
                    if (node.get(0).isTextual()
                            && "map".equals(node.get(0).asText())) {
                        OvsdbMap<Object, Object> map = new OvsdbMap<Object, Object>();
                        for (JsonNode pairNode : node.get(1)) {
                            if (pairNode.isArray() && node.size() == 2) {
                                Object key = atomDeser.deserialize(pairNode
                                        .get(0));
                                Object value = atomDeser.deserialize(pairNode
                                        .get(1));
                                map.put(key, value);
                            }
                        }
                        return map;
                    } else if (node.size() == 0) {
                        return null;
                    }
                }
            }
            throw new RuntimeException("not a map type");
        }
    }

    static class SetDeser {
        public OvsdbSet<Object> deserialize(JsonNode node) {
            OvsdbSet<Object> set = new OvsdbSet<Object>();
            if (node.isArray()) {
                if (node.size() == 2) {
                    if (node.get(0).isTextual()
                            && "set".equals(node.get(0).asText())) {
                        for (JsonNode atomNode : node.get(1)) {
                            set.add(atomDeser.deserialize(atomNode));
                        }
                        return set;
                    }
                } else if (node.size() == 0) {
                    return null;
                }
            }
            // treat the whole thing as a single Atom
            Object atom = atomDeser.deserialize(node);
            if (null != atom) {
                set.add(atom);
            }
            return set;
        }
    }

    static class UpdateNotificationDeser {
        public UpdateNotification deserialize(JsonNode node) {
            UpdateNotification un = new UpdateNotification();
            if (node.isArray()) {
                if (node.size() == 2) {
                    un.setContext(node.get(0).asText());
                    un.setUpdates(node.get(1));
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper
                            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                                       false);
                    TableUpdates updates = objectMapper.convertValue(node
                            .get(1), TableUpdates.class);
                    un.setUpdate(updates);
                    return un;
                }
            }
            return null;
        }
    }

    static class AtomDeser {

        public Object deserialize(JsonNode node) {
            if (!node.isArray()) {
                switch (node.getNodeType()) {
                case BOOLEAN:
                    return node.asBoolean();
                case NUMBER:
                    if (node.isFloatingPointNumber()) {
                        return node.decimalValue();
                    } else {
                        return node.bigIntegerValue();
                    }
                case STRING:
                    return node.asText();
                default:
                    return null;
                }
            }

            if (node.isArray() && node.get(0).isTextual()) {
                if ("uuid".equals(node.get(0).asText())
                        || "named-uuid".equals(node.get(0).asText())) {
                    return new UUID(node.get(1).asText());
                }
            }

            throw new RuntimeException("not an atom node");
        }
    }
}
