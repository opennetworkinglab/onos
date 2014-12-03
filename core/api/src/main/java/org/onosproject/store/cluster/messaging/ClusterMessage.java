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
package org.onosproject.store.cluster.messaging;

import java.io.IOException;

import org.onosproject.cluster.NodeId;
import org.onlab.util.ByteArraySizeHashPrinter;

import com.google.common.base.MoreObjects;

// TODO: Should payload type be ByteBuffer?
/**
 * Base message for cluster-wide communications.
 */
public class ClusterMessage {

    private final NodeId sender;
    private final MessageSubject subject;
    private final byte[] payload;

    /**
     * Creates a cluster message.
     *
     * @param sender  message sender
     * @param subject message subject
     * @param payload message payload
     */
    public ClusterMessage(NodeId sender, MessageSubject subject, byte[] payload) {
        this.sender = sender;
        this.subject = subject;
        this.payload = payload;
    }

    /**
     * Returns the id of the controller sending this message.
     *
     * @return message sender id.
     */
     public NodeId sender() {
         return sender;
     }

    /**
     * Returns the message subject indicator.
     *
     * @return message subject
     */
    public MessageSubject subject() {
        return subject;
    }

    /**
     * Returns the message payload.
     *
     * @return message payload.
     */
    public byte[] payload() {
        return payload;
    }

    /**
     * Sends a response to the sender.
     *
     * @param data payload response.
     * @throws IOException when I/O exception of some sort has occurred
     */
    public void respond(byte[] data) throws IOException {
        throw new IllegalStateException("One can only repond to message recived from others.");
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("sender", sender)
                .add("subject", subject)
                .add("payload", ByteArraySizeHashPrinter.of(payload))
                .toString();
    }
}
