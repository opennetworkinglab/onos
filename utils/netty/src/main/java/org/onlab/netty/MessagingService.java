/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.netty;

import java.io.IOException;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Interface for low level messaging primitives.
 */
public interface MessagingService {
    /**
     * Sends a message asynchronously to the specified communication end point.
     * The message is specified using the type and payload.
     * @param ep end point to send the message to.
     * @param type type of message.
     * @param payload message payload bytes.
     * @throws IOException when I/O exception of some sort has occurred
     */
    public void sendAsync(Endpoint ep, String type, byte[] payload) throws IOException;

    /**
     * Sends a message synchronously and waits for a response.
     * @param ep end point to send the message to.
     * @param type type of message.
     * @param payload message payload.
     * @return a response future
     * @throws IOException when I/O exception of some sort has occurred
     */
    public ListenableFuture<byte[]> sendAndReceive(Endpoint ep, String type, byte[] payload) throws IOException;

    /**
     * Registers a new message handler for message type.
     * @param type message type.
     * @param handler message handler
     */
    public void registerHandler(String type, MessageHandler handler);

    /**
     * Unregister current handler, if one exists for message type.
     * @param type message type
     */
    public void unregisterHandler(String type);
}
