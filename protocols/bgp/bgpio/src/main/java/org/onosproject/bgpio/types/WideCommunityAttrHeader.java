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
package org.onosproject.bgpio.types;

import com.google.common.base.MoreObjects;

import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Validation;
import org.jboss.netty.buffer.ChannelBuffer;

import java.util.Objects;

/**
 * Provides implementation of BGP wide community attribute header.
 */
public class WideCommunityAttrHeader implements BgpValueType {

 /* 0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |             Type              |     Flags     |   Hop Count   |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |            Length             |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+*/

    /*FLAG
      +------+-------+----------------------------------------------------+
      | Bit  | Value | Meaning                                            |
      +------+-------+----------------------------------------------------+
      |  0   |   0   | Local community value.                             |
      |      |   1   | Registered community value.                        |
      |  1   |   0   | Do not decrement Hop Count field across            |
      |      |       | confederation boundaries.                          |
      |      |   1   | Decrement Hop Count field across confederation     |
      |      |       | boundaries.                                        |
      | 2..7 |   -   | MUST be zero when sent and ignored upon receipt.   |
      +------+-------+----------------------------------------------------+*/

    public static final short TYPE = 1;
    public static final short HEADER_LENGTH = 6;
    private byte flag;
    private byte hopCount;
    private short length;

    /**
     * Wide community attribute header.
     *
     * @param flag to apply to all wide community container types
     * @param hopCount represents the forwarding radius, in units of AS hops, for the given Wide BGP Community
     * @param length field represents the total length of a given container
     */
    public WideCommunityAttrHeader(byte flag, byte hopCount, short length) {
        this.flag = flag;
        this.hopCount = hopCount;
        this.length = length;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param flag flag to apply to all wide community container types
     * @param hopCount represents the forwarding radius, in units of AS hops, for the given Wide BGP Community
     * @param length field represents the total length of a given container
     * @return wide community attribute header
     */
    public static WideCommunityAttrHeader of(byte flag, byte hopCount, short length) {
        return new WideCommunityAttrHeader(flag, hopCount, length);
    }

    /**
     * Returns wide community flag.
     *
     * @return wide community flag
     */
    public byte flag() {
        return flag;
    }

    /**
     * Sets wide community flag.
     *
     * @param flag to apply to all wide community container types
     */
    public void setFlag(byte flag) {
        this.flag = flag;
    }

    /**
     * Returns hop count for wide community attribute.
     *
     * @return hop count from wide community
     */
    public byte hopCount() {
        return hopCount;
    }

    /**
     * Sets wide community hop count.
     *
     * @param hopCount represents the forwarding radius, in units of AS hops, for the given Wide BGP Community
     */
    public void setHopCount(byte hopCount) {
        this.hopCount = hopCount;
    }

    /**
     * Returns length of wide community attribute.
     *
     * @return length of wide community attribute
     */
    public short length() {
        return length;
    }

    /**
     * Sets wide community length.
     *
     * @param length total length of a given container
     */
    public void setLength(short length) {
        this.length = length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(flag, hopCount, length);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof WideCommunityAttrHeader) {
            WideCommunityAttrHeader other = (WideCommunityAttrHeader) obj;
            return Objects.equals(flag, other.flag) && Objects.equals(hopCount, other.hopCount)
                    && Objects.equals(length, other.length);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeByte(flag);
        c.writeByte(hopCount);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of WideCommunityAttrHeader.
     *
     * @param c ChannelBuffer
     * @return object of WideCommunityAttrHeader
     * @throws BgpParseException if a parsing error occurs
     */
    public static WideCommunityAttrHeader read(ChannelBuffer c) throws BgpParseException {

        if (c.readableBytes() < HEADER_LENGTH) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR, c.readableBytes());
        }

        short type = c.readShort();
        byte flag = c.readByte();
        byte hopCount = c.readByte();
        short length = c.readShort();
        return WideCommunityAttrHeader.of(flag, hopCount, length);
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("flag", flag)
                .add("hopCount", hopCount)
                .add("length", length)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
