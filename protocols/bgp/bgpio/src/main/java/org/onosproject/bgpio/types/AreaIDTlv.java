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

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.base.MoreObjects;

/**
 * Provides AreaID Tlv which contains opaque value (32 Bit Area-ID).
 */
public class AreaIDTlv implements BgpValueType {

    /* Reference :draft-ietf-idr-ls-distribution-11
     *  0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Type= 514            |             Length=4         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                    opaque value (32 Bit Area-ID)              |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    public static final short TYPE = 514;
    public static final short LENGTH = 4;

    private final int areaID;

    /**
     * Constructor to initialize areaID.
     *
     * @param areaID of BGP AreaID Tlv
     */
    public AreaIDTlv(int areaID) {
        this.areaID = areaID;
    }

    /**
     * Returns object of this class with specified areaID.
     *
     * @param areaID opaque value of area id
     * @return object of AreaIDTlv
     */
    public static AreaIDTlv of(final int areaID) {
        return new AreaIDTlv(areaID);
    }

    /**
     * Returns opaque value of area id.
     *
     * @return opaque value of area id
     */
    public int getAreaID() {
        return areaID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(areaID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AreaIDTlv) {
            AreaIDTlv other = (AreaIDTlv) obj;
            return Objects.equals(areaID, other.areaID);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(areaID);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of AreaIDTlv.
     *
     * @param cb ChannelBuffer
     * @return object of AreaIDTlv
     */
    public static AreaIDTlv read(ChannelBuffer cb) {
        return AreaIDTlv.of(cb.readInt());
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
        return ((Integer) (this.areaID)).compareTo((Integer) (((AreaIDTlv) o).areaID));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("Value", areaID)
                .toString();
    }
}