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

import com.google.common.collect.ImmutableMap;
import org.onlab.packet.DeserializationException;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Route attribute header.
 */
public abstract class RouteAttribute {

    public static final int ROUTE_ATTRIBUTE_HEADER_LENGTH = 4;

    public static final int RTA_DST = 1;
    public static final int RTA_OIF = 4;
    public static final int RTA_GATEWAY = 5;
    public static final int RTA_PRIORITY = 6;

    private final int length;
    private final int type;

    private static final Map<Integer, RouteAttributeDecoder<?>> TYPE_DECODER_MAP
            = ImmutableMap.<Integer, RouteAttributeDecoder<?>>builder()
            .put(RTA_DST, RouteAttributeDst.decoder())
            .put(RTA_OIF, RouteAttributeOif.decoder())
            .put(RTA_GATEWAY, RouteAttributeGateway.decoder())
            .put(RTA_PRIORITY, RouteAttributePriority.decoder())
            .build();

    /**
     * Class constructor.
     *
     * @param length attribute length
     * @param type attribute type
     */
    protected RouteAttribute(int length, int type) {
        this.length = length;
        this.type = type;
    }

    /**
     * Returns the attribute length.
     *
     * @return length
     */
    public int length() {
        return length;
    }

    /**
     * Returns the attribute type.
     *
     * @return type
     */
    public int type() {
        return type;
    }

    @Override
    public abstract String toString();

    /**
     * Decodes a route attribute from an input buffer.
     *
     * @param buffer input buffer
     * @param start starting position the route attribute message
     * @param length length of the message
     * @return route attribute message
     * @throws DeserializationException if a route attribute could not be
     * decoded from the input buffer
     */
    public static RouteAttribute decode(byte[] buffer, int start, int length)
            throws DeserializationException {
        checkInput(buffer, start, length, ROUTE_ATTRIBUTE_HEADER_LENGTH);

        ByteBuffer bb = ByteBuffer.wrap(buffer, start, length);

        int tlvLength = Short.reverseBytes(bb.getShort());
        int type = Short.reverseBytes(bb.getShort());

        if (bb.remaining() < tlvLength - ROUTE_ATTRIBUTE_HEADER_LENGTH) {
            throw new DeserializationException(
                    "Incorrect buffer size when decoding route attribute");
        }

        byte[] value = new byte[tlvLength - ROUTE_ATTRIBUTE_HEADER_LENGTH];
        bb.get(value);

        RouteAttributeDecoder<?> decoder = TYPE_DECODER_MAP.get(type);
        if (decoder == null) {
            throw new DeserializationException(
                    "No decoder found for route attribute type " + type);
        }

        return decoder.decodeAttribute(tlvLength, type, value);
    }
}
