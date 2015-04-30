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

package org.onosproject.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Provides convenience methods for dealing with JSON nodes, arrays etc.
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // non-instantiable
    private JsonUtils() { }

    /**
     * Wraps a message payload into an event structure for the given event
     * type and sequence ID. Generally, the sequence ID should be a copy of
     * the ID from the client request event.
     *
     * @param type    event type
     * @param sid     sequence ID
     * @param payload event payload
     * @return the object node representation
     */
    public static ObjectNode envelope(String type, long sid, ObjectNode payload) {
        ObjectNode event = MAPPER.createObjectNode();
        event.put("event", type);
        if (sid > 0) {
            event.put("sid", sid);
        }
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
     * @param node message event
     * @param name property name
     * @return property as number
     */
    public static long number(ObjectNode node, String name) {
        return node.path(name).asLong();
    }

    /**
     * Returns the specified node property as a string.
     *
     * @param node message event
     * @param name property name
     * @return property as a string
     */
    public static String string(ObjectNode node, String name) {
        return node.path(name).asText();
    }

    /**
     * Returns the specified node property as a string, with a default fallback.
     *
     * @param node         message event
     * @param name         property name
     * @param defaultValue fallback value if property is absent
     * @return property as a string
     */
    public static String string(ObjectNode node, String name, String defaultValue) {
        return node.path(name).asText(defaultValue);
    }

}
