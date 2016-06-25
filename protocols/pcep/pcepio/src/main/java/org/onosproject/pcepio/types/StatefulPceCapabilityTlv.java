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
 * Provides StatefulPceCapabilityTlv.
 */
public class StatefulPceCapabilityTlv implements PcepValueType {

    /*             STATEFUL-PCE-CAPABILITY TLV format
     *
     * Reference :PCEP Extensions for Stateful PCE draft-ietf-pce-stateful-pce-10

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |               Type=16         |            Length=4           |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                             Flags                   |D|T|I|S|U|
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */
    protected static final Logger log = LoggerFactory.getLogger(StatefulPceCapabilityTlv.class);

    public static final short TYPE = 16;
    public static final short LENGTH = 4;
    public static final byte UFLAG_SET = 0x01;
    public static final byte SFLAG_SET = 0x02;
    public static final byte IFLAG_SET = 0x04;
    public static final byte TFLAG_SET = 0x08;
    public static final byte DFLAG_SET = 0x10;
    public static final int SET = 1;

    private final int rawValue;
    private final boolean bDFlag;
    private final boolean bTFlag;
    private final boolean bIFlag;
    private final boolean bSFlag;
    private final boolean bUFlag;
    private final boolean isRawValueSet;

    /**
     * Constructor to initialize variables.
     *
     * @param rawValue Flags
     */
    public StatefulPceCapabilityTlv(int rawValue) {
        this.rawValue = rawValue;
        isRawValueSet = true;
        this.bUFlag = (rawValue & UFLAG_SET) == UFLAG_SET;
        this.bSFlag = (rawValue & SFLAG_SET) == SFLAG_SET;
        this.bIFlag = (rawValue & IFLAG_SET) == IFLAG_SET;
        this.bTFlag = (rawValue & TFLAG_SET) == TFLAG_SET;
        this.bDFlag = (rawValue & DFLAG_SET) == DFLAG_SET;
    }

    /**
     * Constructor to initialize variables.
     *
     * @param bDFlag D-flag
     * @param bTFlag T-flag
     * @param bIFlag I-flag
     * @param bSFlag S-flag
     * @param bUFlag U-flag
     */
    public StatefulPceCapabilityTlv(boolean bDFlag, boolean bTFlag, boolean bIFlag, boolean bSFlag, boolean bUFlag) {
        this.bDFlag = bDFlag;
        this.bTFlag = bTFlag;
        this.bIFlag = bIFlag;
        this.bSFlag = bSFlag;
        this.bUFlag = bUFlag;
        this.rawValue = 0;
        isRawValueSet = false;
    }

    /**
     * Returns object of StatefulPceCapabilityTlv.
     *
     * @param raw value Flags
     * @return object of StatefulPceCapabilityTlv
     */
    public static StatefulPceCapabilityTlv of(final int raw) {
        return new StatefulPceCapabilityTlv(raw);
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    /**
     * Returns D-flag.
     *
     * @return bDFlag D-flag
     */
    public boolean getDFlag() {
        return bDFlag;
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
     * Returns I-flag.
     *
     * @return bIFlag I-flag
     */
    public boolean getIFlag() {
        return bIFlag;
    }

    /**
     * Returns S-flag.
     *
     * @return bSFlag S-flag
     */
    public boolean getSFlag() {
        return bSFlag;
    }

    /**
     * Returns U-flag.
     *
     * @return bUFlag U-flag
     */
    public boolean getUFlag() {
        return bUFlag;
    }

    /**
     * Returns raw value Flags.
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
            return Objects.hash(bDFlag, bTFlag, bIFlag, bSFlag, bUFlag);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof StatefulPceCapabilityTlv) {
            StatefulPceCapabilityTlv other = (StatefulPceCapabilityTlv) obj;
            if (isRawValueSet) {
                return Objects.equals(this.rawValue, other.rawValue);
            } else {
                return Objects.equals(this.bDFlag, other.bDFlag) && Objects.equals(this.bTFlag, other.bTFlag)
                        && Objects.equals(this.bIFlag, other.bIFlag) && Objects.equals(this.bSFlag, other.bSFlag)
                        && Objects.equals(this.bUFlag, other.bUFlag);
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
            c.writeInt(rawValue);
        } else {
            int temp = 0;
            if (bUFlag) {
                temp = temp | UFLAG_SET;
            }
            if (bSFlag) {
                temp = temp | SFLAG_SET;
            }
            if (bIFlag) {
                temp = temp | IFLAG_SET;
            }
            if (bTFlag) {
                temp = temp | TFLAG_SET;
            }
            if (bDFlag) {
                temp = temp | DFLAG_SET;
            }
            c.writeInt(temp);
        }
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads from channel buffer and returns object of StatefulPceCapabilityTlv.
     *
     * @param c input channel buffer
     * @return object of StatefulPceCapabilityTlv
     */
    public static PcepValueType read(ChannelBuffer c) {
        int temp = c.readInt();
        boolean bDFlag;
        boolean bTFlag;
        boolean bIFlag;
        boolean bSFlag;
        boolean bUFlag;

        bUFlag = (temp & UFLAG_SET) == UFLAG_SET;
        bSFlag = (temp & SFLAG_SET) == SFLAG_SET;
        bIFlag = (temp & IFLAG_SET) == IFLAG_SET;
        bTFlag = (temp & TFLAG_SET) == TFLAG_SET;
        bDFlag = (temp & DFLAG_SET) == DFLAG_SET;

        return new StatefulPceCapabilityTlv(bDFlag, bTFlag, bIFlag, bSFlag, bUFlag);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("type", TYPE)
                .add("Length", LENGTH)
                .add("DFlag", bDFlag)
                .add("TFlag", bTFlag)
                .add("IFlag", bIFlag)
                .add("SFlag", bSFlag)
                .add("UFlag", bUFlag)
                .toString();
    }
}
