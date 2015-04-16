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
package org.onlab.nio.service;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onlab.nio.AbstractMessage;
import org.onlab.packet.IpAddress;
import org.onlab.util.ByteArraySizeHashPrinter;
import org.onosproject.store.cluster.messaging.Endpoint;

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;

/**
 * Default message.
 */
public class DefaultMessage extends AbstractMessage {

    private long id;
    private Endpoint sender;
    private String type;
    private byte[] payload;

    /**
     * Creates a new message with the specified data.
     *
     * @param id message id
     * @param type message type
     * @param sender sender endpoint
     * @param payload message payload
     */
    DefaultMessage(long id, Endpoint sender, String type, byte[] payload) {
        this.id = id;
        this.type = checkNotNull(type, "Type cannot be null");
        this.sender = checkNotNull(sender, "Sender cannot be null");
        this.payload = checkNotNull(payload, "Payload cannot be null");

        byte[] messageTypeBytes = type.getBytes(Charsets.UTF_8);
        IpAddress senderIp = sender.host();
        byte[] ipOctets = senderIp.toOctets();

        length = 25 + ipOctets.length + messageTypeBytes.length + payload.length;
    }

    /**
     * Returns message id.
     *
     * @return message id
     */
    public long id() {
        return id;
    }

    /**
     * Returns message sender.
     *
     * @return message sender
     */
    public Endpoint sender() {
        return sender;
    }

    /**
     * Returns message type.
     *
     * @return message type
     */
    public String type() {
        return type;
    }

    /**
     * Returns message payload.
     *
     * @return payload
     */
    public byte[] payload() {
        return payload;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("sender", sender)
                .add("payload", ByteArraySizeHashPrinter.of(payload))
                .toString();
    }
}