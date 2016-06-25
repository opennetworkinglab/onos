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
package org.onosproject.bgpio.types.attr;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implements BGP attribute node flag.
 */
public final class BgpAttrNodeFlagBitTlv implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpAttrNodeFlagBitTlv.class);

    public static final int ATTRNODE_FLAGBIT = 1024;

    /* Node flag bit TLV */
    private final boolean bOverloadBit;
    private final boolean bAttachedBit;
    private final boolean bExternalBit;
    private final boolean bAbrBit;

    public static final byte FIRST_BIT = (byte) 0x80;
    public static final byte SECOND_BIT = 0x40;
    public static final byte THIRD_BIT = 0x20;
    public static final byte FOURTH_BIT = 0x01;

    /**
     * Constructor to initialize parameters.
     *
     * @param bOverloadBit Overload bit
     * @param bAttachedBit Attached bit
     * @param bExternalBit External bit
     * @param bAbrBit ABR Bit
     */
    public BgpAttrNodeFlagBitTlv(boolean bOverloadBit, boolean bAttachedBit,
                                  boolean bExternalBit, boolean bAbrBit) {
        this.bOverloadBit = bOverloadBit;
        this.bAttachedBit = bAttachedBit;
        this.bExternalBit = bExternalBit;
        this.bAbrBit = bAbrBit;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param bOverloadBit Overload bit
     * @param bAttachedBit Attached bit
     * @param bExternalBit External bit
     * @param bAbrBit ABR Bit
     * @return object of BgpAttrNodeFlagBitTlv
     */
    public static BgpAttrNodeFlagBitTlv of(final boolean bOverloadBit,
                                           final boolean bAttachedBit,
                                           final boolean bExternalBit,
                                           final boolean bAbrBit) {
        return new BgpAttrNodeFlagBitTlv(bOverloadBit, bAttachedBit,
                                         bExternalBit, bAbrBit);
    }

    /**
     * Reads the Node Flag Bits.
     *
     * @param cb ChannelBuffer
     * @return attribute node flag bit tlv
     * @throws BgpParseException while parsing BgpAttrNodeFlagBitTlv
     */
    public static BgpAttrNodeFlagBitTlv read(ChannelBuffer cb)
            throws BgpParseException {
        boolean bOverloadBit = false;
        boolean bAttachedBit = false;
        boolean bExternalBit = false;
        boolean bAbrBit = false;

        short lsAttrLength = cb.readShort();

        if ((lsAttrLength != 1) || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        byte nodeFlagBits = cb.readByte();

        bOverloadBit = ((nodeFlagBits & FIRST_BIT) == FIRST_BIT);
        bAttachedBit = ((nodeFlagBits & SECOND_BIT) == SECOND_BIT);
        bExternalBit = ((nodeFlagBits & THIRD_BIT) == THIRD_BIT);
        bAbrBit = ((nodeFlagBits & FOURTH_BIT) == FOURTH_BIT);

        return BgpAttrNodeFlagBitTlv.of(bOverloadBit, bAttachedBit,
                                        bExternalBit, bAbrBit);
    }

    /**
     * Returns Overload Bit.
     *
     * @return Overload Bit
     */
    public boolean overLoadBit() {
        return bOverloadBit;
    }

    /**
     * Returns Attached Bit.
     *
     * @return Attached Bit
     */
    public boolean attachedBit() {
        return bAttachedBit;
    }

    /**
     * Returns External Bit.
     *
     * @return External Bit
     */
    public boolean externalBit() {
        return bExternalBit;
    }

    /**
     * Returns ABR Bit.
     *
     * @return ABR Bit
     */
    public boolean abrBit() {
        return bAbrBit;
    }

    @Override
    public short getType() {
        return ATTRNODE_FLAGBIT;
    }

    @Override
    public int write(ChannelBuffer cb) {
        // TODO This will be implemented in the next version
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bOverloadBit, bAttachedBit, bExternalBit, bAbrBit);
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
                    && Objects.equals(bAbrBit, other.bAbrBit);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("bOverloadBit", bOverloadBit)
                .add("bAttachedBit", bAttachedBit)
                .add("bExternalBit", bExternalBit).add("bAbrBit", bAbrBit)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
