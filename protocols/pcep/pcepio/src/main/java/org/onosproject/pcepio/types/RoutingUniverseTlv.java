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
package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides RoutingUniverseTLV identifiers.
 */
public class RoutingUniverseTlv implements PcepValueType {

    /*
     * Reference : draft-dhodylee-pce-pcep-te-data-extn-02, section 9.2.1.
     *  0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Type=[TBD7]         |           Length=8            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                           Identifier                          |
     |                                                               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     *
     *
     *             +------------+---------------------+
                   | Identifier | Routing Universe    |
                   +------------+---------------------+
                   |     0      | L3 packet topology  |
                   |     1      | L1 optical topology |
                   +------------+---------------------+
     */

    protected static final Logger log = LoggerFactory.getLogger(RoutingUniverseTlv.class);

    public static final short TYPE = (short) 65281;
    public static final short LENGTH = 8;

    private final long rawValue;

    /**
     * Constructor to initialize raw value.
     *
     * @param rawValue raw value
     */
    public RoutingUniverseTlv(long rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Returns object of RoutingUniverseTLV.
     *
     * @param raw value
     * @return object of RoutingUniverseTLV
     */
    public static RoutingUniverseTlv of(final long raw) {
        return new RoutingUniverseTlv(raw);
    }

    /**
     * Returns raw value as Identifier.
     *
     * @return rawValue Identifier
     */
    public long getLong() {
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
        if (obj instanceof RoutingUniverseTlv) {
            RoutingUniverseTlv other = (RoutingUniverseTlv) obj;
            return Objects.equals(this.rawValue, other.rawValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeLong(rawValue);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads from channel buffer and returns object of RoutingUniverseTLV.
     *
     * @param c input channel buffer
     * @return object of RoutingUniverseTLV
     */
    public static RoutingUniverseTlv read(ChannelBuffer c) {
        return RoutingUniverseTlv.of(c.readLong());
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
