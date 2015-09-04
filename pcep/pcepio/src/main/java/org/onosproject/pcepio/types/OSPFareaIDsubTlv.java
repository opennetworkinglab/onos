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
 * Provides area ID for OSPF area.
 */
public class OSPFareaIDsubTlv implements PcepValueType {

    /* Reference :draft-ietf-idr-ls-distribution-10.
     *  0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Type=[TBD12]         |             Length=4         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                    opaque value (32 Bit AS Number)            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(OSPFareaIDsubTlv.class);

    public static final short TYPE = 600; //TODD:change this TBD12
    public static final short LENGTH = 4;

    private final int rawValue;

    /**
     * constructor to initialize rawValue.
     *
     * @param rawValue area ID for OSPF area.
     */
    public OSPFareaIDsubTlv(int rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * Returns newly created OSPFareaIDsubTlv object.
     *
     * @param raw opaque value of AreaID
     * @return new object of OSPF area ID sub TLV
     */
    public static OSPFareaIDsubTlv of(final int raw) {
        return new OSPFareaIDsubTlv(raw);
    }

    /**
     * Returns RawValue opaque value of AreaID.
     *
     * @return rawValue Area ID
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
        if (obj instanceof OSPFareaIDsubTlv) {
            OSPFareaIDsubTlv other = (OSPFareaIDsubTlv) obj;
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
     * Reads the channel buffer and returns object of OSPFAreaIdSubTlv.
     *
     * @param c input channel buffer
     * @return object of OSPFAreaIdSubTlv
     */
    public static OSPFareaIDsubTlv read(ChannelBuffer c) {
        return OSPFareaIDsubTlv.of(c.readInt());
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
