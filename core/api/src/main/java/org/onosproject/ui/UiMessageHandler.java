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
import org.onlab.osgi.ServiceDirectory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of an entity capable of processing a JSON message from the user
 * interface client.
 * <p>
 * The message is a JSON object with the following structure:
 * <pre>
 * {
 *     "type": "<em>event-type</em>",
 *     "sid": "<em>sequence-number</em>",
 *     "payload": {
 *         <em>arbitrary JSON object structure</em>
 *     }
 * }
 * </pre>
 */
public abstract class UiMessageHandler {

    private final Set<String> messageTypes;
    private UiConnection connection;
    private ServiceDirectory directory;

    /** Mapper for creating ObjectNodes and ArrayNodes etc. */
    protected final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a new message handler for the specified set of message types.
     *
     * @param messageTypes set of message types
     */
    protected UiMessageHandler(Set<String> messageTypes) {
        this.messageTypes = checkNotNull(messageTypes, "Message types cannot be null");
        checkArgument(!messageTypes.isEmpty(), "Message types cannot be empty");
    }

    /**
     * Returns the set of message types which this handler is capable of
     * processing.
     *
     * @return set of message types
     */
    public Set<String> messageTypes() {
        return messageTypes;
    }

    /**
     * Processes a JSON message from the user interface client.
     *
     * @param message JSON message
     */
    public abstract void process(ObjectNode message);

    /**
     * Initializes the handler with the user interface connection and
     * service directory context.
     *
     * @param connection user interface connection
     * @param directory  service directory
     */
    public void init(UiConnection connection, ServiceDirectory directory) {
        this.connection = connection;
        this.directory = directory;
    }

    /**
     * Destroys the message handler context.
     */
    public void destroy() {
        this.connection = null;
        this.directory = null;
    }

    /**
     * Returns the user interface connection with which this handler was primed.
     *
     * @return user interface connection
     */
    public UiConnection connection() {
        return connection;
    }

    /**
     * Returns the user interface connection with which this handler was primed.
     *
     * @return user interface connection
     */
    public ServiceDirectory directory() {
        return directory;
    }

    /**
     * Returns implementation of the specified service class.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return implementation class
     * @throws org.onlab.osgi.ServiceNotFoundException if no implementation found
     */
    protected <T> T get(Class<T> serviceClass) {
        return directory.get(serviceClass);
    }

    /**
     * Wraps a message payload into an event structure for the given event
     * type and sequence ID. Generally the
     *
     * @param type event type
     * @param sid sequence ID
     * @param payload event payload
     * @return the object node representation
     */
    protected ObjectNode envelope(String type, long sid, ObjectNode payload) {
        ObjectNode event = mapper.createObjectNode();
        event.put("event", type);
        if (sid > 0) {
            event.put("sid", sid);
        }
        event.set("payload", payload);
        return event;
    }

}
