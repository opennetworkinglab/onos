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
 * Provides MPLS Protocol Mask.
 */
public class MPLSProtocolMaskTlv implements PcepValueType {

    /* Reference :[I-D.ietf-idr-ls-distribution]/3.3.2.2
     *  0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |              Type=TDB39       |             Length =1         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |L|R|  Reserved |
     +-+-+-+-+-+-+-+-+
     */
    protected static final Logger log = LoggerFactory.getLogger(MPLSProtocolMaskTlv.class);

    public static final short TYPE = 1094; //TDB39
    public static final short LENGTH = 1;
    public static final byte LFLAG_SET = (byte) 0x80;
    public static final byte RFLAG_SET = 0x40;

    private final byte rawValue;
    private final boolean bLFlag;
    private final boolean bRFlag;
    private final boolean isRawValueSet;

    /**
     * constructor to initialize rawValue.
     *
     * @param rawValue MPLS Protocol Mask Flag Bits
     */
    public MPLSProtocolMaskTlv(byte rawValue) {
        this.rawValue = rawValue;
        this.isRawValueSet = true;
        this.bLFlag = (rawValue & LFLAG_SET) == LFLAG_SET;
        this.bRFlag = (rawValue & RFLAG_SET) == RFLAG_SET;
    }

    /**
     * constructor to initialize different Flags.
     *
     * @param bLFlag L-flag
     * @param bRFlag R-flag
     */
    public MPLSProtocolMaskTlv(boolean bLFlag, boolean bRFlag) {
        this.bLFlag = bLFlag;
        this.bRFlag = bRFlag;
        this.rawValue = 0;
        isRawValueSet = false;
    }

    /**
     * Returns newly created MPLSProtocolMaskTlv object.
     *
     * @param raw MPLS Protocol Mask Tlv
     * @return new object of MPLS Protocol Mask Tlv
     */
    public static MPLSProtocolMaskTlv of(final byte raw) {
        return new MPLSProtocolMaskTlv(raw);
    }

    /**
     * Returns L-flag.
     *
     * @return bLFlag L-flag
     */
    public boolean getbLFlag() {
        return bLFlag;
    }

    /**
     * Returns R-flag.
     *
     * @return bRFlag R-flag
     */
    public boolean getbRFlag() {
        return bRFlag;
    }

    /**
     * Returns raw value.
     *
     * @return rawValue raw value
     */
    public byte getByte() {
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
        if (isRawValueSet) {
            return Objects.hash(rawValue);
        } else {
            return Objects.hash(bLFlag, bRFlag);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MPLSProtocolMaskTlv) {
            MPLSProtocolMaskTlv other = (MPLSProtocolMaskTlv) obj;
            if (isRawValueSet) {
                return Objects.equals(this.rawValue, other.rawValue);
            } else {
                return Objects.equals(this.bLFlag, other.bLFlag) && Objects.equals(this.bRFlag, other.bRFlag);
            }
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        if (isRawValueSet) {
            c.writeByte(rawValue);
        } else {
            byte temp = 0;
            if (bLFlag) {
                temp = (byte) (temp | LFLAG_SET);
            }
            if (bRFlag) {
                temp = (byte) (temp | RFLAG_SET);
            }
            c.writeByte(temp);
        }
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of MPLS Protocol Mask Tlv.
     *
     * @param c input channel buffer
     * @return object of MPLS Protocol Mask Tlv
     */
    public static PcepValueType read(ChannelBuffer c) {
        byte temp = c.readByte();
        boolean bLFlag;
        boolean bRFlag;

        bLFlag = (temp & LFLAG_SET) == LFLAG_SET;
        bRFlag = (temp & RFLAG_SET) == RFLAG_SET;

        return new MPLSProtocolMaskTlv(bLFlag, bRFlag);
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
