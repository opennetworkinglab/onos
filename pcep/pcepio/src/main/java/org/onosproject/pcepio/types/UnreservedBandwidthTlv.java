/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Unreserved Bandwidth Tlv.
 */
public class UnreservedBandwidthTlv implements PcepValueType {

    /* Reference :[RFC5305]/3.6
     0                   1                   2                   3
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |           Type=[TDB36]        |             Length=4          |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                 Unreserved Bandwidth                          |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(UnreservedBandwidthTlv.class);

    public static final short TYPE = 11; //TDB36
    public static final short LENGTH = 4;

    private final int rawValue;

    /**
     * Constructor to initialize rawValue.
     *
     * @param rawValue Unreserved Bandwidth
     */
    public UnreservedBandwidthTlv(int rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Returns newly created UnreservedBandwidthTlv object.
     *
     * @param raw as Unreserved Bandwidth
     * @return object of UnreservedBandwidthTlv
     */
    public static UnreservedBandwidthTlv of(final int raw) {
        return new UnreservedBandwidthTlv(raw);
    }

    /**
     * Returns Unreserved Bandwidth.
     *
     * @return rawValue Unreserved Bandwidth
     */
    public int getInt() {
        return rawValue;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public short getLength() {
        return LENGTH;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UnreservedBandwidthTlv) {
            UnreservedBandwidthTlv other = (UnreservedBandwidthTlv) obj;
            return Objects.equals(this.rawValue, other.rawValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(rawValue);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads byte stream from channel buffer and returns object of UnreservedBandwidthTlv.
     *
     * @param c input channel buffer
     * @return object of UnreservedBandwidthTlv
     */
    public static UnreservedBandwidthTlv read(ChannelBuffer c) {
        return UnreservedBandwidthTlv.of(c.readInt());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("Value", rawValue)
                .toString();
    }
}
