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

package org.onosproject.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Provides convenience methods for dealing with JSON nodes, arrays etc.
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // non-instantiable
    private JsonUtils() {
    }

    /**
     * Composes a message structure for the given message type and payload.
     *
     * @param type    message type
     * @param payload message payload
     * @return the object node representation
     */
    public static ObjectNode envelope(String type, ObjectNode payload) {
        ObjectNode event = MAPPER.createObjectNode();
        event.put("event", type);
        event.set("payload", payload);
        return event;
    }

    /**
     * Returns the event type from the specified event.
     * If the node does not have an "event" property, "unknown" is returned.
     *
     * @param event message event
     * @return extracted event type
     */
    public static String eventType(ObjectNode event) {
        return string(event, "event", "unknown");
    }

    /**
     * Returns the payload from the specified event.
     *
     * @param event message event
     * @return extracted payload object
     */
    public static ObjectNode payload(ObjectNode event) {
        return (ObjectNode) event.path("payload");
    }

    /**
     * Returns the specified node property as a number.
     *
     * @param node object node
     * @param name property name
     * @return property as number
     */
    public static long number(ObjectNode node, String name) {
        return node.path(name).asLong();
    }

    /**
     * Returns the specified node property as a string.
     *
     * @param node object node
     * @param name property name
     * @return property as a string
     */
    public static String string(ObjectNode node, String name) {
        return node.path(name).asText();
    }

    /**
     * Returns the specified node property as a string, with a default fallback.
     *
     * @param node         object node
     * @param name         property name
     * @param defaultValue fallback value if property is absent
     * @return property as a string
     */
    public static String string(ObjectNode node, String name, String defaultValue) {
        return node.path(name).asText(defaultValue);
    }

    /**
     * Returns the specified node property as an object node.
     *
     * @param node object node
     * @param name property name
     * @return property as a node
     */
    public static ObjectNode node(ObjectNode node, String name) {
        return (ObjectNode) node.path(name);
    }

    /**
     * Returns the specified node property as a boolean.
     *
     * @param node object node
     * @param name property name
     * @return property as a boolean
     */
    public static boolean bool(ObjectNode node, String name) {
        return node.path(name).asBoolean();
    }
}
