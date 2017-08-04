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

import java.net.InetAddress;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * Implementation of NextHop BGP Path Attribute.
 */
public class NextHop implements BgpValueType {
    public static final byte NEXTHOP_TYPE = 3;
    public static final byte FLAGS = (byte) 0x40;
    private boolean isNextHop = false;
    private Ip4Address nextHop;

    /**
     * Constructor to initialize parameters.
     *
     * @param nextHop nextHop address
     */
    public NextHop(Ip4Address nextHop) {
        this.nextHop = Preconditions.checkNotNull(nextHop);
        this.isNextHop = true;
    }

    /**
     * Constructor to initialize default parameters.
     *
     */
    public NextHop() {
        this.nextHop = null;
    }

    /**
     * Returns whether next hop is present.
     *
     * @return whether next hop is present
     */
    public boolean isNextHopSet() {
        return this.isNextHop;
    }

    /**
     * Reads from ChannelBuffer and parses NextHop.
     *
     * @param cb ChannelBuffer
     * @return object of NextHop
     * @throws BgpParseException while parsing nexthop attribute
     */
    public static NextHop read(ChannelBuffer cb) throws BgpParseException {
        Ip4Address nextHop;
        ChannelBuffer tempCb = cb.copy();
        Validation parseFlags = Validation.parseAttributeHeader(cb);

        if (cb.readableBytes() < parseFlags.getLength()) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                    parseFlags.getLength());
        }
        int len = parseFlags.isShort() ? parseFlags.getLength() + Constants.TYPE_AND_LEN_AS_SHORT : parseFlags
                .getLength() + Constants.TYPE_AND_LEN_AS_BYTE;
        ChannelBuffer data = tempCb.readBytes(len);
        if (parseFlags.getFirstBit() && !parseFlags.getSecondBit() && parseFlags.getThirdBit()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_FLAGS_ERROR, data);
        }

         InetAddress ipAddress = Validation.toInetAddress(parseFlags.getLength(), cb);
        if (ipAddress.isMulticastAddress()) {
            throw new BgpParseException("Multicast address is not supported");
        }

        nextHop = Ip4Address.valueOf(ipAddress);
        return new NextHop(nextHop);
    }

    /**
     * Return nexthop address.
     *
     * @return nexthop address
     */
    public Ip4Address nextHop() {
        return nextHop;
    }

    @Override
    public short getType() {
        return NEXTHOP_TYPE;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeByte(FLAGS);
        cb.writeByte(getType());
        if (!isNextHopSet()) {
            cb.writeByte(0);
        } else {
            cb.writeInt(nextHop.toInt());
        }

        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nextHop);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NextHop) {
            NextHop other = (NextHop) obj;
            return Objects.equals(nextHop, other.nextHop);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("nextHop", nextHop)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}