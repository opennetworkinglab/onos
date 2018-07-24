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

import com.google.common.base.Objects;
import org.onlab.packet.Deserializer;

import java.nio.ByteBuffer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Represents LACP collector information.
 */
public class LacpCollectorTlv extends LacpTlv {
    public static final byte LENGTH = 16;
    private static final byte[] RESERVED = new byte[12];

    private short collectorMaxDelay;

    /**
     * Gets collector max delay.
     *
     * @return collector max delay
     */
    public short getCollectorMaxDelay() {
        return collectorMaxDelay;
    }

    /**
     * Sets collector max delay.
     *
     * @param collectorMaxDelay collector max delay
     * @return this
     */
    public LacpCollectorTlv setCollectorMaxDelay(short collectorMaxDelay) {
        this.collectorMaxDelay = collectorMaxDelay;
        return this;
    }

    /**
     * Deserializer function for LacpCollectorTlv packets.
     *
     * @return deserializer function
     */
    public static Deserializer<LacpCollectorTlv> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, LENGTH - HEADER_LENGTH);

            LacpCollectorTlv lacpCollectorTlv = new LacpCollectorTlv();
            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            lacpCollectorTlv.setCollectorMaxDelay(bb.getShort());

            return lacpCollectorTlv;
        };
    }

    @Override
    public byte[] serialize() {
        final byte[] data = new byte[LENGTH - HEADER_LENGTH];

        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.putShort(this.collectorMaxDelay);
        bb.put(RESERVED);

        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof LacpCollectorTlv)) {
            return false;
        }
        final LacpCollectorTlv other = (LacpCollectorTlv) obj;
        return this.collectorMaxDelay == other.collectorMaxDelay;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), collectorMaxDelay);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("collectorMaxDelay", Short.toString(collectorMaxDelay))
                .toString();
    }
}
