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
package org.onosproject.pce.pceservice;

import org.onlab.util.Identifier;
import org.onosproject.net.resource.ResourceConsumer;

import com.google.common.annotations.Beta;
import org.onosproject.net.resource.ResourceConsumerId;

/**
 * Tunnel resource consumer identifier suitable to be used as a consumer id for
 * resource allocations.
 */
@Beta
public final class TunnelConsumerId extends Identifier<Long> implements ResourceConsumer {

    /**
     * Creates a tunnel resource consumer identifier from the specified long value.
     *
     * @param value long value to be used as tunnel resource consumer id
     * @return tunnel resource consumer identifier
     */
    public static TunnelConsumerId valueOf(long value) {
        return new TunnelConsumerId(value);
    }

    /**
     * Initializes object for serializer.
     */
    public TunnelConsumerId() {
        super(0L);
    }

    /**
     * Constructs the tunnel resource consumer id corresponding to a given long
     * value.
     *
     * @param value the underlying value in long representation of this tunnel
     *            resource consumer id
     */
    public TunnelConsumerId(long value) {
        super(value);
    }

    /**
     * Returns the backing identifier value.
     *
     * @return value backing identifier value
     */
    public long value() {
        return identifier;
    }

    @Override
    public String toString() {
        return "0x" + Long.toHexString(identifier);
    }

    @Override
    public ResourceConsumerId consumerId() {
        return ResourceConsumerId.of(this);
    }
}
