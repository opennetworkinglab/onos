/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgpio.types;

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.protocol.IgpRouterId;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Provides Implementation of IsIsNonPseudonode Tlv.
 */
public class IsIsNonPseudonode implements IgpRouterId, BgpValueType {
    public static final short TYPE = 515;
    public static final short LENGTH = 6;

    private final byte[] isoNodeID;

    /**
     * Constructor to initialize isoNodeID.
     *
     * @param isoNodeID ISO system-ID
     */
    public IsIsNonPseudonode(byte[] isoNodeID) {
        this.isoNodeID = Arrays.copyOf(isoNodeID, isoNodeID.length);
    }

    /**
     * Returns object of this class with specified isoNodeID.
     *
     * @param isoNodeID ISO system-ID
     * @return object of IsIsNonPseudonode
     */
    public static IsIsNonPseudonode of(final byte[] isoNodeID) {
        return new IsIsNonPseudonode(isoNodeID);
    }

    /**
     * Returns ISO NodeID.
     *
     * @return ISO NodeID
     */
    public byte[] getIsoNodeId() {
        return isoNodeID;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(isoNodeID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IsIsNonPseudonode) {
            IsIsNonPseudonode other = (IsIsNonPseudonode) obj;
            return Arrays.equals(isoNodeID, other.isoNodeID);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeBytes(isoNodeID);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of IsIsNonPseudonode.
     *
     * @param cb ChannelBuffer
     * @return object of IsIsNonPseudonode
     */
    public static IsIsNonPseudonode read(ChannelBuffer cb) {
        byte[] isoNodeID = new byte[LENGTH];
        cb.readBytes(isoNodeID);
        return IsIsNonPseudonode.of(isoNodeID);
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }
        ByteBuffer value1 = ByteBuffer.wrap(this.isoNodeID);
        ByteBuffer value2 = ByteBuffer.wrap(((IsIsNonPseudonode) o).isoNodeID);
        return value1.compareTo(value2);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("ISONodeID", isoNodeID)
                .toString();
    }
}
