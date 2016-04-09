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
 * Provide node Flags bits.
 */
public class NodeFlagBitsSubTlv implements PcepValueType {

    /* Reference :[I-D.ietf-idr- ls-distribution] /3.3.1.1
     * 0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |              Type=[TBD21]      |             Length=1         |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |O|T|E|B| Reserved|
     +-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(NodeFlagBitsSubTlv.class);

    public static final short TYPE = 13;
    public static final short LENGTH = 1;
    public static final int SET = 1;
    public static final byte OFLAG_SET = (byte) 0x80;
    public static final byte TFLAG_SET = 0x40;
    public static final byte EFLAG_SET = 0x20;
    public static final byte BFLAG_SET = 0x10;

    private final byte rawValue;
    private final boolean bOFlag;
    private final boolean bTFlag;
    private final boolean bEFlag;
    private final boolean bBFlag;
    private final boolean isRawValueSet;

    /**
     * constructor to initialize rawValue.
     *
     * @param rawValue of Node Flag Bits TLV
     */
    public NodeFlagBitsSubTlv(byte rawValue) {
        this.rawValue = rawValue;
        isRawValueSet = true;
        this.bOFlag = (rawValue & OFLAG_SET) == OFLAG_SET;
        this.bTFlag = (rawValue & TFLAG_SET) == TFLAG_SET;
        this.bEFlag = (rawValue & EFLAG_SET) == EFLAG_SET;
        this.bBFlag = (rawValue & BFLAG_SET) == BFLAG_SET;
    }

    /**
     * constructor to initialize different Flags.
     *
     * @param bOFlag O-flag
     * @param bTFlag T-flag
     * @param bEFlag E-flag
     * @param bBFlag B-flag
     */
    public NodeFlagBitsSubTlv(boolean bOFlag, boolean bTFlag, boolean bEFlag, boolean bBFlag) {
        this.bOFlag = bOFlag;
        this.bTFlag = bTFlag;
        this.bEFlag = bEFlag;
        this.bBFlag = bBFlag;
        this.rawValue = 0;
        this.isRawValueSet = false;
    }

    /**
     * Returns newly created NodeFlagBitsTlv object.
     *
     * @param raw of Node Flag Bits TLV
     * @return new object of NodeFlagBitsTlv
     */
    public static NodeFlagBitsSubTlv of(final byte raw) {
        return new NodeFlagBitsSubTlv(raw);
    }

    /**
     * Returns raw value of NodeFlagBitsTlv.
     *
     * @return rawValue raw value
     */
    public byte getbyte() {
        return rawValue;
    }

    /**
     * Returns O-flag.
     *
     * @return bOFlag O-flag
     */
    public boolean getOFlag() {
        return bOFlag;
    }

    /**
     * Returns T-flag.
     *
     * @return bTFlag T-flag
     */
    public boolean getTFlag() {
        return bTFlag;
    }

    /**
     * Returns E-flag.
     *
     * @return bEFlag E-flag
     */
    public boolean getEFlag() {
        return bEFlag;
    }

    /**
     * Returns B-flag.
     *
     * @return bBFlag B-flag
     */
    public boolean getBFlag() {
        return bBFlag;
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
            return Objects.hash(bOFlag, bTFlag, bEFlag, bBFlag);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeFlagBitsSubTlv) {
            NodeFlagBitsSubTlv other = (NodeFlagBitsSubTlv) obj;
            if (isRawValueSet) {
                return Objects.equals(this.rawValue, other.rawValue);
            } else {
                return Objects.equals(this.bOFlag, other.bOFlag) && Objects.equals(this.bTFlag, other.bTFlag)
                        && Objects.equals(this.bEFlag, other.bEFlag) && Objects.equals(this.bBFlag, other.bBFlag);
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
            if (bOFlag) {
                temp = (byte) (temp | OFLAG_SET);
            }
            if (bTFlag) {
                temp = (byte) (temp | TFLAG_SET);
            }
            if (bEFlag) {
                temp = (byte) (temp | EFLAG_SET);
            }
            if (bBFlag) {
                temp = (byte) (temp | BFLAG_SET);
            }
            c.writeByte(temp);
        }
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of NodeFlagBitsTlv.
     *
     * @param c input channel buffer
     * @return object of NodeFlagBitsTlv
     */
    public static PcepValueType read(ChannelBuffer c) {

        return NodeFlagBitsSubTlv.of(c.readByte());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("OFlag", (bOFlag) ? 1 : 0)
                .add("TFlag", (bTFlag) ? 1 : 0)
                .add("EFlag", (bEFlag) ? 1 : 0)
                .add("BFlag", (bBFlag) ? 1 : 0)
                .toString();
    }
}
