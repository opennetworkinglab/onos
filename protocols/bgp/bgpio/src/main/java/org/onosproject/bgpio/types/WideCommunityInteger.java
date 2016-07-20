/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Validation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Provides implementation of BGP wide community integer subtlv.
 */
public class WideCommunityInteger implements BgpValueType {
    public static final short TYPE = 4;
    private List<Integer> integer;

    /**
     * Creates an instance of wide community integer subtlv.
     *
     * @param integer integer
     */
    public WideCommunityInteger(List<Integer> integer) {
        this.integer = integer;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param integer wide community subtlv integer
     * @return object of WideCommunityInteger
     */
    public static WideCommunityInteger of(List<Integer> integer) {
        return new WideCommunityInteger(integer);
    }

    /**
     * Returns wide community subtlv integer.
     *
     * @return wide community subtlv integer
     */
    public List<Integer> integer() {
        return integer;
    }

    /**
     * Sets wide community subtlv integer.
     *
     * @param integer wide community subtlv integer
     */
    public void setInteger(List<Integer> integer) {
        this.integer = integer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(integer);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof WideCommunityInteger) {
            WideCommunityInteger other = (WideCommunityInteger) obj;
            return Objects.equals(integer, other.integer);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();

        Iterator<Integer> listIterator = integer.iterator();
        c.writeByte(TYPE);

        int iLengthIndex = c.writerIndex();
        c.writeShort(0);

        while (listIterator.hasNext()) {
            Integer integer = listIterator.next();
            if (integer instanceof Integer) {
                c.writeInt(integer);
            }
        }

        int length = c.writerIndex() - iLengthIndex;
        c.setShort(iLengthIndex, (short) (length - 2));

        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of WideCommunityInteger.
     *
     * @param c ChannelBuffer
     * @return object of WideCommunityInteger
     * @throws BgpParseException on read error
     */
    public static WideCommunityInteger read(ChannelBuffer c) throws BgpParseException {

        List<Integer> integer = new ArrayList<>();
        short length;

        if (c.readableBytes() < 2) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   c.readableBytes());
        }

        length = c.readShort();
        if (length == 0) {
            return new WideCommunityInteger(integer);
        }

        if (c.readableBytes() < length) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   c.readableBytes());
        }

        while (c.readableBytes() > 0) {
            if (c.readableBytes() < 4) {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                       c.readableBytes());
            }
            integer.add(c.readInt());
        }

        return new WideCommunityInteger(integer);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("integer", integer)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public short getType() {
        // TODO Auto-generated method stub
        return 0;
    }
}
