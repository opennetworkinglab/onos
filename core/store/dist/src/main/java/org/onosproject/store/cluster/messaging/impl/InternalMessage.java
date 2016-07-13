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
package org.onosproject.store.cluster.messaging.impl;

import com.google.common.base.MoreObjects;

import org.onlab.util.ByteArraySizeHashPrinter;
import org.onosproject.core.HybridLogicalTime;
import org.onosproject.store.cluster.messaging.Endpoint;

/**
 * Internal message representation with additional attributes
 * for supporting, synchronous request/reply behavior.
 */
public final class InternalMessage {

    /**
     * Message status.
     */
    public enum Status {
        /**
         * All ok.
         */
        OK,

        /**
         * Response status signifying no registered handler.
         */
        ERROR_NO_HANDLER,

        /**
         * Response status signifying an exception handling the message.
         */
        ERROR_HANDLER_EXCEPTION,

        /**
         * Reponse status signifying invalid message structure.
         */
        PROTOCOL_EXCEPTION

        // NOTE: For backwards compatibility it important that new enum constants
        // be appended.
        // FIXME: We should remove this restriction in the future.
    }

    private final int preamble;
    private final HybridLogicalTime time;
    private final long id;
    private final Endpoint sender;
    private final String type;
    private final byte[] payload;
    private final Status status;

    public InternalMessage(int preamble,
                           HybridLogicalTime time,
                           long id,
                           Endpoint sender,
                           String type,
                           byte[] payload) {
        this(preamble, time, id, sender, type, payload, Status.OK);
    }

    public InternalMessage(int preamble,
                           HybridLogicalTime time,
                           long id,
                           Endpoint sender,
                           String type,
                           byte[] payload,
                           Status status) {
        this.preamble = preamble;
        this.time = time;
        this.id = id;
        this.sender = sender;
        this.type = type;
        this.payload = payload;
        this.status = status;
    }

    public HybridLogicalTime time() {
        return time;
    }

    public int preamble() {
        return preamble;
    }

    public long id() {
        return id;
    }

    public String type() {
        return type;
    }

    public Endpoint sender() {
        return sender;
    }

    public byte[] payload() {
        return payload;
    }

    public Status status() {
        return status;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("time", time)
                .add("id", id)
                .add("type", type)
                .add("sender", sender)
                .add("status", status)
                .add("payload", ByteArraySizeHashPrinter.of(payload))
                .toString();
    }
}
