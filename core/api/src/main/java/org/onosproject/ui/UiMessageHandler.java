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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of an entity capable of processing JSON messages from the user
 * interface client.
 * <p>
 * The message structure is:
 * </p>
 * <pre>
 * {
 *     "event": "<em>event-type</em>",
 *     "payload": {
 *         <em>arbitrary JSON object structure</em>
 *     }
 * }
 * </pre>
 * On {@link #init initialization} the handler will create and cache
 * {@link RequestHandler} instances, each of which are bound to a particular
 * <em>event-type</em>. On {@link #process arrival} of a new message,
 * the <em>event-type</em> is determined, and the message dispatched to the
 * corresponding <em>RequestHandler</em>'s
 * {@link RequestHandler#process process} method.
 * <p>
 * For convenience the implementation includes methods to obtain JSON
 * generating objects (mapper, objectNode, arrayNode) as well as a
 * JsonCodecContext for preparing and digesting messages to the UI
 * client.
 */
public abstract class UiMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<String, RequestHandler> handlerMap = new HashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    private UiConnection connection;
    private ServiceDirectory directory;

    private MessageCodecContext codecContext;

    /**
     * Subclasses must create and return the collection of request handlers
     * for the message types they handle.
     * <p>
     * Note that request handlers should be stateless. When we are
     * {@link #destroy destroyed}, we will simply drop our references to them
     * and allow them to be garbage collected.
     *
     * @return the message handler instances
     */
    protected abstract Collection<RequestHandler> createRequestHandlers();

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
        ObjectNode payload = JsonUtils.payload(message);
        exec(type, payload);
    }

    /**
     * Finds the appropriate handler and executes the process method.
     *
     * @param eventType event type
     * @param payload   message payload
     */
    void exec(String eventType, ObjectNode payload) {
        RequestHandler requestHandler = handlerMap.get(eventType);
        if (requestHandler != null) {
            requestHandler.process(payload);
        } else {
            log.warn("no request handler for event type {}", eventType);
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

        Collection<RequestHandler> handlers = createRequestHandlers();
        checkNotNull(handlers, "Handlers cannot be null");
        checkArgument(!handlers.isEmpty(), "Handlers cannot be empty");

        for (RequestHandler h : handlers) {
            h.setParent(this);
            handlerMap.put(h.eventType(), h);
        }
    }

    /**
     * Destroys the message handler context.
     */
    public void destroy() {
        this.connection = null;
        this.directory = null;
        handlerMap.clear();
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
     * Returns the service directory with which this handler was primed.
     *
     * @return service directory
     */
    public ServiceDirectory directory() {
        return directory;
    }

    /**
     * Returns an implementation of the specified service class.
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
     * Returns a freshly minted object node.
     *
     * @return new object node
     */
    protected ObjectNode objectNode() {
        return mapper.createObjectNode();
    }

    /**
     * Returns a freshly minted array node.
     *
     * @return new array node
     */
    protected ArrayNode arrayNode() {
        return mapper.createArrayNode();
    }

    /**
     * Sends the specified data to the client.
     * It is expected that the data is in the prescribed JSON format for
     * events to the client.
     *
     * @param data data to be sent
     */
    protected synchronized void sendMessage(ObjectNode data) {
        UiConnection connection = connection();
        if (connection != null) {
            connection.sendMessage(data);
        }
    }

    /**
     * Obtain a CodecContext to be used in encoding and decoding objects
     * that have a registered JsonCodec for their class.  This method
     * instantiates a private inner class which is returned on
     * subsequent calls.
     *
     * @return a CodecContext.
     */
    protected CodecContext getJsonCodecContext() {
        if (codecContext != null) {
            return codecContext;
        }
        codecContext = new MessageCodecContext();
        return codecContext;
    }

    private class MessageCodecContext implements CodecContext {

        CodecService cs = get(CodecService.class);

        @Override
        public ObjectMapper mapper() {
            return mapper;
        }

        @Override
        public <T> JsonCodec<T> codec(Class<T> entityClass) {
            return cs.getCodec(entityClass);
        }

        @Override
        public <T> T getService(Class<T> serviceClass) {
            return get(serviceClass);
        }
    }
}
