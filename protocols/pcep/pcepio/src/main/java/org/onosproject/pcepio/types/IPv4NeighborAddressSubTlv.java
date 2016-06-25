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
package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides IPv4 Neighbor Address .
 */
public class IPv4NeighborAddressSubTlv implements PcepValueType {

    /* Reference :[RFC5305]/3.3
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Type=8              |             Length=4          |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                   IPv4 Neighbor Address                       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(IPv4NeighborAddressSubTlv.class);

    public static final short TYPE = 8;
    public static final short LENGTH = 4;

    private final int rawValue;

    /**
     * Constructor to initialize rawValue.
     *
     * @param rawValue IPv4-Neighbor-Address-Tlv
     */
    public IPv4NeighborAddressSubTlv(int rawValue) {
        log.debug("IPv4NeighborAddressTlv");
        this.rawValue = rawValue;
    }

    /**
     * Returns newly created IPv4NeighborAddressTlv object.
     *
     * @param raw value of IPv4-Neighbor-Address
     * @return object of IPv4NeighborAddressTlv
     */
    public static IPv4NeighborAddressSubTlv of(final int raw) {
        return new IPv4NeighborAddressSubTlv(raw);
    }

    /**
     * Returns value of IPv4 Neighbor Address.
     *
     * @return rawValue IPv4 Neighbor Address
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
        if (obj instanceof IPv4NeighborAddressSubTlv) {
            IPv4NeighborAddressSubTlv other = (IPv4NeighborAddressSubTlv) obj;
            return Objects.equals(rawValue, other.rawValue);
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
     * Reads the channel buffer and returns object of IPv4-Neighbor-Address-Tlv.
     *
     * @param c input channel buffer
     * @return object of IPv4-Neighbor-Address-Tlv
     */
    public static IPv4NeighborAddressSubTlv read(ChannelBuffer c) {
        return IPv4NeighborAddressSubTlv.of(c.readInt());
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
