/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onlab.packet;

import java.nio.ByteBuffer;

/**
 * Implements IP packet format.
 */
public abstract class IP extends BasePacket {

    /**
     * Gets IP version number.
     *
     * @return IP version number
     */
    public abstract byte getVersion();

    /**
     * Sets IP version number.
     *
     * @param version the version to set
     * @return IP class
     */
    public abstract IP setVersion(final byte version);

    /**
     * Deserializer function for IP packets.
     *
     * @return deserializer function
     */
    public static Deserializer<? extends IP> deserializer() {
        return (data, offset, length) -> {
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            byte version = (byte) (bb.get() >> 4 & 0xf);

            switch (version) {
                case 4:
                    return IPv4.deserializer().deserialize(data, offset, length);
                case 6:
                    return IPv6.deserializer().deserialize(data, offset, length);
                default:
                    throw new DeserializationException("Invalid IP version");
            }
        };
    }
}
