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

import java.util.Arrays;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Provides Opaque Link Attribute.
 */
public class OpaqueLinkAttributeSubTlv implements PcepValueType {

    /*
     * TLV format.
     * Reference :[I-D.ietf-idr-attributesls-distribution] /3.3.2.6
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |              Type=TBD42       |             Length            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     //                Opaque link attributes (variable)            //
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(OpaqueLinkAttributeSubTlv.class);

    public static final short TYPE = 31;
    private final short hLength;

    private final byte[] rawValue;

    /**
     * constructor to initialize rawValue.
     *
     * @param rawValue of Opaque Link Attribute
     * @param hLength length
     */
    public OpaqueLinkAttributeSubTlv(byte[] rawValue, short hLength) {
        log.debug("OpaqueLinkAttributeTlv");
        this.rawValue = rawValue;
        if (0 == hLength) {
            this.hLength = (short) rawValue.length;
        } else {
            this.hLength = hLength;
        }
    }

    /**
     * Returns newly created OpaqueLinkAttributeTlv object.
     *
     * @param raw of Opaque Link Attribute
     * @param hLength length
     * @return new object of OpaqueLinkAttributeTlv
     */
    public static OpaqueLinkAttributeSubTlv of(final byte[] raw, short hLength) {
        return new OpaqueLinkAttributeSubTlv(raw, hLength);
    }

    /**
     * Returns raw value of Opaque Link Attribute Tlv.
     * @return rawValue raw value
     */
    public byte[] getValue() {
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
        return hLength;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(rawValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OpaqueLinkAttributeSubTlv) {
            OpaqueLinkAttributeSubTlv other = (OpaqueLinkAttributeSubTlv) obj;
            return Arrays.equals(this.rawValue, other.rawValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(hLength);
        c.writeBytes(rawValue);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of OpaqueLinkAttributeTlv.
     *
     * @param c input channel buffer
     * @param hLength length
     * @return object of Opaque Link Attribute Tlv
     */
    public static PcepValueType read(ChannelBuffer c, short hLength) {
        byte[] iOpaqueValue = new byte[hLength];
        c.readBytes(iOpaqueValue, 0, hLength);
        return new OpaqueLinkAttributeSubTlv(iOpaqueValue, hLength);
    }

    @Override
    public String toString() {
        ToStringHelper toStrHelper = MoreObjects.toStringHelper(getClass());

        toStrHelper.add("Type", TYPE);
        toStrHelper.add("Length", hLength);

        StringBuffer result = new StringBuffer();
        for (byte b : rawValue) {
            result.append(String.format("%02X ", b));
        }
        toStrHelper.add("Value", result);

        return toStrHelper.toString();
    }
}
