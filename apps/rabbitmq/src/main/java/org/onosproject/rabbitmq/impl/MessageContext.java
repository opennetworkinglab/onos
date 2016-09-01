/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.rabbitmq.impl;

import java.io.Serializable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents message context like data in byte stream and mq properties for
 * message delivery.
 */
public class MessageContext implements Serializable {
    private static final long serialVersionUID = -4174900539976805047L;
    private static final String NULL_ERR =
                                "The body and properties should be present";

    private final Map<String, Object> properties;
    private final byte[] body;

    /**
     * Initializes MessageContext class.
     *
     * @param body       Byte stream of the event's JSON data
     * @param properties Map of the Message Queue properties
     */
    public MessageContext(byte[] body, Map<String, Object> properties) {
        this.body = checkNotNull(body, NULL_ERR);
        this.properties = checkNotNull(properties, NULL_ERR);
    }

    /**
     * Returns the Message Properties Map.
     *
     * @return Map of the Message Queue properties
     */

    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Returns the Message Properties Map.
     *
     * @return Byte stream of the event's JSON data
     */
    public byte[] getBody() {
        return body;
    }
}
