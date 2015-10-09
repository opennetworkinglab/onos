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
package org.onosproject.bgpio.types.attr;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.BGPValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implements BGP attribute node flag.
 */
public class BgpAttrNodeFlagBitTlv implements BGPValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpAttrNodeFlagBitTlv.class);

    public static final int ATTRNODE_FLAGBIT = 1024;

    /* Node flag bit TLV */
    private boolean bOverloadBit;
    private boolean bAttachedBit;
    private boolean bExternalBit;
    private boolean bABRBit;

    public static final int BIT_SET = 1;
    public static final int FIRST_BIT = 0x80;
    public static final int SECOND_BIT = 0x40;
    public static final int THIRD_BIT = 0x20;
    public static final int FOURTH_BIT = 0x01;

    /**
     * Constructor to initialize parameters.
     *
     * @param bOverloadBit Overload bit
     * @param bAttachedBit Attached bit
     * @param bExternalBit External bit
     * @param bABRBit ABR Bit
     */
    BgpAttrNodeFlagBitTlv(boolean bOverloadBit, boolean bAttachedBit,
                          boolean bExternalBit, boolean bABRBit) {
        this.bOverloadBit = bOverloadBit;
        this.bAttachedBit = bAttachedBit;
        this.bExternalBit = bExternalBit;
        this.bABRBit = bABRBit;
    }

    /**
     * Reads the Node Flag Bits.
     *
     * @param cb ChannelBuffer
     * @return attribute node flag bit tlv
     * @throws BGPParseException while parsing BgpAttrNodeFlagBitTlv
     */
    public static BgpAttrNodeFlagBitTlv read(ChannelBuffer cb)
            throws BGPParseException {
        boolean bOverloadBit = false;
        boolean bAttachedBit = false;
        boolean bExternalBit = false;
        boolean bABRBit = false;

        short lsAttrLength = cb.readShort();

        if (lsAttrLength != 1) {
            Validation.validateLen(BGPErrorType.UPDATE_MESSAGE_ERROR,
                                   BGPErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        byte nodeFlagBits = cb.readByte();

        bOverloadBit = ((nodeFlagBits & (byte) FIRST_BIT) == FIRST_BIT);
        bAttachedBit = ((nodeFlagBits & (byte) SECOND_BIT) == SECOND_BIT);
        bExternalBit = ((nodeFlagBits & (byte) THIRD_BIT) == THIRD_BIT);
        bABRBit = ((nodeFlagBits & (byte) FOURTH_BIT) == FOURTH_BIT);

        return new BgpAttrNodeFlagBitTlv(bOverloadBit, bAttachedBit,
                                         bExternalBit, bABRBit);
    }

    /**
     * Returns Overload Bit.
     *
     * @return Overload Bit
     */
    boolean getOverLoadBit() {
        return bOverloadBit;
    }

    /**
     * Returns Attached Bit.
     *
     * @return Attached Bit
     */
    boolean getAttachedBit() {
        return bAttachedBit;
    }

    /**
     * Returns External Bit.
     *
     * @return External Bit
     */
    boolean getExternalBit() {
        return bExternalBit;
    }

    /**
     * Returns ABR Bit.
     *
     * @return ABR Bit
     */
    boolean getABRBit() {
        return bABRBit;
    }

    @Override
    public short getType() {
        return ATTRNODE_FLAGBIT;
    }

    @Override
    public int write(ChannelBuffer cb) {
        // TODO will be implementing it later
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bOverloadBit, bAttachedBit, bExternalBit, bABRBit);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpAttrNodeFlagBitTlv) {
            BgpAttrNodeFlagBitTlv other = (BgpAttrNodeFlagBitTlv) obj;
            return Objects.equals(bOverloadBit, other.bOverloadBit)
                    && Objects.equals(bAttachedBit, other.bAttachedBit)
                    && Objects.equals(bExternalBit, other.bExternalBit)
                    && Objects.equals(bABRBit, other.bABRBit);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("bOverloadBit", bOverloadBit)
                .add("bAttachedBit", bAttachedBit)
                .add("bExternalBit", bExternalBit).add("bABRBit", bABRBit)
                .toString();
    }
}
