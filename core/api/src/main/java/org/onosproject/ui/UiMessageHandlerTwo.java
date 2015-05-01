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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of an entity capable of processing a JSON message from the user
 * interface client.
 * <p>
 * The message is a JSON object with the following structure:
 * </p>
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
public abstract class UiMessageHandlerTwo {

    private final Map<String, RequestHandler> handlerMap = new HashMap<>();

    private UiConnection connection;
    private ServiceDirectory directory;

    /**
     * Mapper for creating ObjectNodes and ArrayNodes etc.
     */
    protected final ObjectMapper mapper = new ObjectMapper();

    /**
     * Binds the handlers returned from {@link #getHandlers()} to this
     * instance.
     */
    void bindHandlers() {
        Collection<RequestHandler> handlers = getHandlers();
        checkNotNull(handlers, "Handlers cannot be null");
        checkArgument(!handlers.isEmpty(), "Handlers cannot be empty");

        for (RequestHandler h : handlers) {
            h.setParent(this);
            handlerMap.put(h.eventType(), h);
        }
    }

    /**
     * Subclasses must return the collection of handlers for the
     * message types they handle.
     *
     * @return the message handler instances
     */
    protected abstract Collection<RequestHandler> getHandlers();

    /**
     * Returns the set of message types which this handler is capable of
     * processing.
     *
     * @return set of message types
     */
    public Set<String> messageTypes() {
        return Collections.unmodifiableSet(handlerMap.keySet());
    }

    /**
     * Processes a JSON message from the user interface client.
     *
     * @param message JSON message
     */
    public void process(ObjectNode message) {
        String type = JsonUtils.eventType(message);
        long sid = JsonUtils.sid(message);
        ObjectNode payload = JsonUtils.payload(message);
        exec(type, sid, payload);
    }

    /**
     * Finds the appropriate handler and executes the process method.
     *
     * @param eventType event type
     * @param sid       sequence identifier
     * @param payload   message payload
     */
    void exec(String eventType, long sid, ObjectNode payload) {
        RequestHandler handler = handlerMap.get(eventType);
        if (handler != null) {
            handler.process(sid, payload);
        }
    }

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
        bindHandlers();
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

}
