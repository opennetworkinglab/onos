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
 * Provides PceccCapabilityTlv.
 */
public class PceccCapabilityTlv implements PcepValueType {

    /*          PCECC CAPABILITY TLV
     * Reference : draft-zhao-pce-pcep-extension-for-pce-controller-01, section-7.1.1

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |               Type=32         |            Length=4           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                             Flags                         |G|L|
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */
    protected static final Logger log = LoggerFactory.getLogger(PceccCapabilityTlv.class);

    public static final short TYPE = 32;
    public static final short LENGTH = 4;
    public static final int SET = 1;
    public static final byte LFLAG_CHECK = 0x01;
    public static final byte GFLAG_CHECK = 0x02;

    private final boolean bGFlag;
    private final boolean bLFlag;

    private final int rawValue;
    private final boolean isRawValueSet;

    /**
     * Constructor to initialize raw Value.
     *
     * @param rawValue raw value
     */
    public PceccCapabilityTlv(final int rawValue) {
        this.rawValue = rawValue;
        this.isRawValueSet = true;

        bLFlag = (rawValue & LFLAG_CHECK) == LFLAG_CHECK;
        bGFlag = (rawValue & GFLAG_CHECK) == GFLAG_CHECK;
    }

    /**
     * Constructor to initialize G-flag L-flag.
     * @param bGFlag G-flag
     * @param bLFlag L-flag
     */
    public PceccCapabilityTlv(boolean bGFlag, boolean bLFlag) {
        this.bGFlag = bGFlag;
        this.bLFlag = bLFlag;
        this.rawValue = 0;
        this.isRawValueSet = false;
    }

    /**
     * Returns newly created PceccCapabilityTlv object.
     *
     * @param raw value
     * @return object of Pcecc Capability Tlv
     */
    public static PceccCapabilityTlv of(final int raw) {
        return new PceccCapabilityTlv(raw);
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    /**
     * Returns G-flag.
     * @return bGFlag G-flag
     */
    public boolean getGFlag() {
        return bGFlag;
    }

    /**
     * Returns L-flag.
     * @return bLFlag L-flag
     */
    public boolean getLFlag() {
        return bLFlag;
    }

    /**
     * Returns the raw value.
     * @return rawValue Flags
     */
    public int getInt() {
        return rawValue;
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
            return Objects.hash(bLFlag, bGFlag);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PceccCapabilityTlv) {
            PceccCapabilityTlv other = (PceccCapabilityTlv) obj;
            if (isRawValueSet) {
                return Objects.equals(this.rawValue, other.rawValue);
            } else {
                return Objects.equals(this.bGFlag, other.bGFlag) && Objects.equals(this.bLFlag, other.bLFlag);
            }
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        int temp = 0;
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        if (isRawValueSet) {
            c.writeInt(rawValue);
        } else {
            if (bGFlag) {
                temp = temp | GFLAG_CHECK;
            }
            if (bLFlag) {
                temp = temp | LFLAG_CHECK;
            }
            c.writeInt(temp);
        }
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads channel buffer and returns object of PceccCapabilityTlv.
     *
     * @param c input channel buffer
     * @return object of PceccCapabilityTlv
     */
    public static PceccCapabilityTlv read(ChannelBuffer c) {
        return PceccCapabilityTlv.of(c.readInt());
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
