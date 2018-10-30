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

package org.onlab.packet;

import com.google.common.collect.ImmutableMap;
import org.onlab.packet.lacp.Lacp;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Implements ethernet slow protocols.
 */
public class Slow extends BasePacket {
    public static final int HEADER_LENGTH = 1;

    public static final byte SUBTYPE_LACP = 0x1;
    // Subtypes below has not been implemented yet
    // public static final byte SUBTYPE_LAMP = 0x2;
    // public static final byte SUBTYPE_OAM = 0x3;
    // public static final byte SUBTYPE_OSSP = 0xa;

    public static final Map<Byte, Deserializer<? extends IPacket>> PROTOCOL_DESERIALIZER_MAP =
            ImmutableMap.<Byte, Deserializer<? extends IPacket>>builder()
                    .put(Slow.SUBTYPE_LACP, Lacp.deserializer())
                    .build();

    private byte subtype;

    /**
     * Gets subtype.
     *
     * @return subtype
     */
    public byte getSubtype() {
        return subtype;
    }

    /**
     * Sets subtype.
     *
     * @param subtype the subtype to set
     * @return this
     */
    public Slow setSubtype(byte subtype) {
        this.subtype = subtype;
        return this;
    }

    /**
     * Deserializer function for Slow packets.
     *
     * @return deserializer function
     */
    public static Deserializer<Slow> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            Slow slow = new Slow();
            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            slow.setSubtype(bb.get());

            Deserializer<? extends IPacket> deserializer;
            if (Slow.PROTOCOL_DESERIALIZER_MAP.containsKey(slow.subtype)) {
                deserializer = Slow.PROTOCOL_DESERIALIZER_MAP.get(slow.subtype);
            } else {
                throw new DeserializationException("Unsupported slow protocol subtype " + Byte.toString(slow.subtype));
            }

            int remainingLength = bb.limit() - bb.position();
            slow.payload = deserializer.deserialize(data, bb.position(), remainingLength);
            slow.payload.setParent(slow);

            return slow;
        };
    }

    @Override
    public byte[] serialize() {
        int length = HEADER_LENGTH;
        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
            length += payloadData.length;
        }

        final byte[] data = new byte[length];

        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.subtype);
        if (payloadData != null) {
            bb.put(payloadData);
        }

        return data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subtype);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Slow)) {
            return false;
        }
        final Slow other = (Slow) obj;

        return this.subtype == other.subtype;
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("subtype", Byte.toString(subtype))
                .toString();
    }
}
