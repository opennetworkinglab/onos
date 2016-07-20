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
 * Provides PceccCapabilityTlv.
 */
public class PceccCapabilityTlv implements PcepValueType {

    /*          PCECC CAPABILITY TLV
     * Reference : draft-zhao-pce-pcep-extension-for-pce-controller-03, section-7.1.1

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |               Type=[TBD]      |            Length=4           |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                             Flags                           |S|
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */
    protected static final Logger log = LoggerFactory.getLogger(PceccCapabilityTlv.class);

    public static final short TYPE = (short) 65287;
    public static final short LENGTH = 4;
    public static final int SET = 1;
    public static final byte SBIT_CHECK = 0x01;

    private final boolean sBit;

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

        sBit = (rawValue & SBIT_CHECK) == SBIT_CHECK;
    }

    /**
     * Constructor to initialize G-flag L-flag.
     *
     * @param sBit pcecc sr capbaility bit
     */
    public PceccCapabilityTlv(boolean sBit) {
        this.sBit = sBit;
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
     * Returns sBit.
     *
     * @return sBit S bit
     */
    public boolean sBit() {
        return sBit;
    }

    /**
     * Returns the raw value.
     *
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
            return Objects.hash(sBit);
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
                return Objects.equals(this.sBit, other.sBit);
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
            if (sBit) {
                temp = temp | SBIT_CHECK;
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
                .add("rawValue", rawValue)
                .toString();
    }
}
