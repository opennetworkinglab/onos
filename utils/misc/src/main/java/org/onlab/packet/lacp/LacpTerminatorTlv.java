/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onlab.packet.lacp;

import org.onlab.packet.Deserializer;

import java.nio.ByteBuffer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Represents LACP terminator information.
 */
public class LacpTerminatorTlv extends LacpTlv {
    public static final byte LENGTH = 0;
    static final byte PADDING_LENGTH = 50;
    private static final byte[] PADDING = new byte[PADDING_LENGTH];

    /**
     * Deserializer function for LacpTerminatorTlv packets.
     *
     * @return deserializer function
     */
    public static Deserializer<LacpTerminatorTlv> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, LENGTH);

            return new LacpTerminatorTlv();
        };
    }

    @Override
    public byte[] serialize() {
        final byte[] data = new byte[LENGTH + PADDING_LENGTH];

        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(PADDING);

        return data;
    }

    @Override
    public String toString() {
        return toStringHelper(getClass()).toString();
    }
}