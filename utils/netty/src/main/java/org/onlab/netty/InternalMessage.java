/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import org.onlab.util.ByteArraySizeHashPrinter;
import org.onosproject.store.cluster.messaging.Endpoint;

import com.google.common.base.MoreObjects;

/**
 * Internal message representation with additional attributes
 * for supporting, synchronous request/reply behavior.
 */
public final class InternalMessage {

    private final long id;
    private final Endpoint sender;
    private final String type;
    private final byte[] payload;

    public InternalMessage(long id, Endpoint sender, String type, byte[] payload) {
        this.id = id;
        this.sender = sender;
        this.type = type;
        this.payload = payload;
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
