/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.routing.fpm.protocol;

import com.google.common.base.MoreObjects;
import org.onlab.packet.DeserializationException;

import java.nio.ByteBuffer;

/**
 * Priority route attribute.
 */
public final class RouteAttributePriority extends RouteAttribute {

    private static final int VALUE_LENGTH = 4;

    private final long priority;

    /**
     * Class constructor.
     *
     * @param length length
     * @param type type
     * @param priority priority
     */
    private RouteAttributePriority(int length, int type, long priority) {
        super(length, type);

        this.priority = priority;
    }

    /**
     * Returns the priority.
     *
     * @return priority
     */
    public long priority() {
        return priority;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("type", type())
                .add("length", length())
                .add("priority", priority)
                .toString();
    }

    /**
     * Returns a decoder for a priority route attribute.
     *
     * @return priority route attribute decoder
     */
    public static RouteAttributeDecoder<RouteAttributePriority> decoder() {
        return (int length, int type, byte[] value) -> {
            if (value.length != VALUE_LENGTH) {
                throw new DeserializationException("Wrong value length");
            }

            long priority = Integer.reverseBytes(ByteBuffer.wrap(value).getInt());

            return new RouteAttributePriority(length, type, priority);
        };
    }

}
