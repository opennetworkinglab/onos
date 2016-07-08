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
 * Abstraction of an entity that handles a specific request from the
 * user interface client.
 *
 * @see UiMessageHandler
 */
public abstract class RequestHandler {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private final String eventType;
    private UiMessageHandler parent;


    public RequestHandler(String eventType) {
        this.eventType = eventType;
    }

    // package private
    void setParent(UiMessageHandler parent) {
        this.parent = parent;
    }

    /**
     * Returns the event type that this handler handles.
     *
     * @return event type
     */
    public String eventType() {
        return eventType;
    }

    /**
     * Processes the incoming message payload from the client.
     *
     * @param sid     message sequence identifier
     * @param payload request message payload
     */
    // TODO: remove sid from signature
    public abstract void process(long sid, ObjectNode payload);


    // ===================================================================
    // === Convenience methods...

    /**
     * Returns implementation of the specified service class.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return implementation class
     * @throws org.onlab.osgi.ServiceNotFoundException if no implementation found
     */
    protected <T> T get(Class<T> serviceClass) {
        return parent.directory().get(serviceClass);
    }

    /**
     * Sends a message back to the client.
     *
     * @param eventType message event type
     * @param sid       message sequence identifier
     * @param payload   message payload
     */
    // TODO: remove sid from signature
    protected void sendMessage(String eventType, long sid, ObjectNode payload) {
        parent.connection().sendMessage(eventType, sid, payload);
    }

    /**
     * Sends a message back to the client.
     *
     * @param eventType message event type
     * @param payload   message payload
     */
    protected void sendMessage(String eventType, ObjectNode payload) {
        // TODO: remove sid
        parent.connection().sendMessage(eventType, 0, payload);
    }

    /**
     * Sends a message back to the client.
     * Here, the message is preformatted; the assumption is it has its
     * eventType, sid and payload attributes already filled in.
     *
     * @param message the message to send
     */
    protected void sendMessage(ObjectNode message) {
        parent.connection().sendMessage(message);
    }

    /**
     * Allows one request handler to pass the event on to another for
     * further processing.
     * Note that the message handlers must be defined in the same parent.
     *
     * @param eventType event type
     * @param sid       sequence identifier
     * @param payload   message payload
     */
    // TODO: remove sid from signature
    protected void chain(String eventType, long sid, ObjectNode payload) {
        parent.exec(eventType, sid, payload);
    }

    // ===================================================================


    /**
     * Returns the specified node property as a string.
     *
     * @param node message event
     * @param key  property name
     * @return property as a string
     */
    protected String string(ObjectNode node, String key) {
        return JsonUtils.string(node, key);
    }

    /**
     * Returns the specified node property as a string, with a default fallback.
     *
     * @param node     object node
     * @param key      property name
     * @param defValue fallback value if property is absent
     * @return property as a string
     */
    protected String string(ObjectNode node, String key, String defValue) {
        return JsonUtils.string(node, key, defValue);
    }

    /**
     * Returns the specified node property as a boolean. More precisely, if
     * the value for the given key is the string "true" then this returns true,
     * false otherwise.
     *
     * @param node object node
     * @param key  property name
     * @return property as a boolean
     */
    protected boolean bool(ObjectNode node, String key) {
        return JsonUtils.bool(node, key);
    }

}
