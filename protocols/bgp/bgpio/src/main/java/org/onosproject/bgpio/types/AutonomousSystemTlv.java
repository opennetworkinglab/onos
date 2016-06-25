/*
 * Copyright 2015-present Open Networking Laboratory
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
 * Provides Autonomous System Tlv which contains opaque value (32 Bit AS Number).
 */
public class AutonomousSystemTlv implements BgpValueType {

    /* Reference :draft-ietf-idr-ls-distribution-11
     *  0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Type= 512            |             Length=4         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                    opaque value (32 Bit AS Number)            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    public static final short TYPE = 512;
    public static final short LENGTH = 4;

    private final int asNum;

    /**
     * Constructor to initialize asNum.
     *
     * @param asNum 32 Bit AS Number
     */
    public AutonomousSystemTlv(int asNum) {
        this.asNum = asNum;
    }

    /**
     * Returns object of this class with specified asNum.
     *
     * @param asNum 32 Bit AS Number
     * @return object of AutonomousSystemTlv
     */
    public static AutonomousSystemTlv of(final int asNum) {
        return new AutonomousSystemTlv(asNum);
    }

    /**
     * Returns opaque value of AS Number.
     *
     * @return opaque value of AS Number
     */
    public int getAsNum() {
        return asNum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(asNum);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AutonomousSystemTlv) {
            AutonomousSystemTlv other = (AutonomousSystemTlv) obj;
            return Objects.equals(asNum, other.asNum);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(asNum);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of AutonomousSystemTlv.
     *
     * @param c ChannelBuffer
     * @return object of AutonomousSystemTlv
     */
    public static AutonomousSystemTlv read(ChannelBuffer c) {
        return AutonomousSystemTlv.of(c.readInt());
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
        return ((Integer) (this.asNum)).compareTo((Integer) (((AutonomousSystemTlv) o).asNum));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("asNum", asNum)
                .toString();
    }
}