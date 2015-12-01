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
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implements BGP link protection type attribute.
 */
public final class BgpLinkAttrProtectionType implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpLinkAttrProtectionType.class);

    public static final int ATTRLINK_PROTECTIONTYPE = 1093;
    public static final int LINK_PROTECTION_LEN = 2;

    public static final int EXTRA_TRAFFIC = 0x01;
    public static final int UNPROTECTED = 0x02;
    public static final int SHARED = 0x04;
    public static final int DEDICATED_ONE_ISTO_ONE = 0x08;
    public static final int DEDICATED_ONE_PLUS_ONE = 0x10;
    public static final int ENHANCED = 0x20;

    /* Link Protection type flags */
    private final boolean bExtraTraffic;
    private final boolean bUnprotected;
    private final boolean bShared;
    private final boolean bDedOneIstoOne;
    private final boolean bDedOnePlusOne;
    private final boolean bEnhanced;

    /**
     * Constructor to initialize the value.
     *
     * @param bExtraTraffic Extra Traffic
     * @param bUnprotected Unprotected
     * @param bShared Shared
     * @param bDedOneIstoOne Dedicated 1:1
     * @param bDedOnePlusOne Dedicated 1+1
     * @param bEnhanced Enhanced
     */
    private BgpLinkAttrProtectionType(boolean bExtraTraffic,
                                      boolean bUnprotected,
                                      boolean bShared, boolean bDedOneIstoOne,
                                      boolean bDedOnePlusOne, boolean bEnhanced) {
        this.bExtraTraffic = bExtraTraffic;
        this.bUnprotected = bUnprotected;
        this.bShared = bShared;
        this.bDedOneIstoOne = bDedOneIstoOne;
        this.bDedOnePlusOne = bDedOnePlusOne;
        this.bEnhanced = bEnhanced;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param bExtraTraffic Extra Traffic
     * @param bUnprotected Unprotected
     * @param bShared Shared
     * @param bDedOneIstoOne Dedicated 1:1
     * @param bDedOnePlusOne Dedicated 1+1
     * @param bEnhanced Enhanced
     * @return object of BgpLinkAttrProtectionType
     */
    public static BgpLinkAttrProtectionType of(boolean bExtraTraffic,
                                               boolean bUnprotected,
                                               boolean bShared,
                                               boolean bDedOneIstoOne,
                                               boolean bDedOnePlusOne,
                                               boolean bEnhanced) {
        return new BgpLinkAttrProtectionType(bExtraTraffic, bUnprotected,
                                             bShared, bDedOneIstoOne,
                                             bDedOnePlusOne, bEnhanced);
    }

    /**
     * Reads the BGP link attributes protection type.
     *
     * @param cb Channel buffer
     * @return object of type BgpLinkAttrProtectionType
     * @throws BgpParseException while parsing BgpLinkAttrProtectionType
     */
    public static BgpLinkAttrProtectionType read(ChannelBuffer cb)
            throws BgpParseException {
        short linkProtectionType;
        byte higherByte;
        short lsAttrLength = cb.readShort();

        boolean bExtraTraffic;
        boolean bUnprotected;
        boolean bShared;
        boolean bDedOneIstoOne;
        boolean bDedOnePlusOne;
        boolean bEnhanced;

        if ((lsAttrLength != LINK_PROTECTION_LEN)
                || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        linkProtectionType = cb.readShort();
        higherByte = (byte) (linkProtectionType >> 8);

        bExtraTraffic = ((higherByte & (byte) EXTRA_TRAFFIC) == EXTRA_TRAFFIC);
        bUnprotected = ((higherByte & (byte) UNPROTECTED) == UNPROTECTED);
        bShared = ((higherByte & (byte) SHARED) == SHARED);
        bDedOneIstoOne = ((higherByte & (byte) DEDICATED_ONE_ISTO_ONE) == DEDICATED_ONE_ISTO_ONE);
        bDedOnePlusOne = ((higherByte & (byte) DEDICATED_ONE_PLUS_ONE) == DEDICATED_ONE_PLUS_ONE);
        bEnhanced = ((higherByte & (byte) ENHANCED) == ENHANCED);

        return BgpLinkAttrProtectionType.of(bExtraTraffic, bUnprotected,
                                            bShared, bDedOneIstoOne,
                                            bDedOnePlusOne, bEnhanced);
    }

    /**
     * Returns ExtraTraffic Bit.
     *
     * @return ExtraTraffic Bit
     */
    public boolean extraTraffic() {
        return bExtraTraffic;
    }

    /**
     * Returns Unprotected Bit.
     *
     * @return Unprotected Bit
     */
    public boolean unprotected() {
        return bUnprotected;
    }

    /**
     * Returns Shared Bit.
     *
     * @return Shared Bit
     */
    public boolean shared() {
        return bShared;
    }

    /**
     * Returns DedOneIstoOne Bit.
     *
     * @return DedOneIstoOne Bit
     */
    public boolean dedOneIstoOne() {
        return bDedOneIstoOne;
    }

    /**
     * Returns DedOnePlusOne Bit.
     *
     * @return DedOnePlusOne Bit
     */
    public boolean dedOnePlusOne() {
        return bDedOnePlusOne;
    }

    /**
     * Returns Enhanced Bit.
     *
     * @return Enhanced Bit
     */
    public boolean enhanced() {
        return bEnhanced;
    }

    @Override
    public short getType() {
        return ATTRLINK_PROTECTIONTYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bExtraTraffic, bUnprotected, bShared,
                            bDedOneIstoOne, bDedOnePlusOne, bEnhanced);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrProtectionType) {
            BgpLinkAttrProtectionType other = (BgpLinkAttrProtectionType) obj;
            return Objects.equals(bExtraTraffic, other.bExtraTraffic)
                    && Objects.equals(bUnprotected, other.bUnprotected)
                    && Objects.equals(bShared, other.bShared)
                    && Objects.equals(bDedOneIstoOne, other.bDedOneIstoOne)
                    && Objects.equals(bDedOnePlusOne, other.bDedOnePlusOne)
                    && Objects.equals(bEnhanced, other.bEnhanced);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        // TODO This will be implemented in the next version
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("bExtraTraffic", bExtraTraffic)
                .add("bUnprotected", bUnprotected).add("bShared", bShared)
                .add("bDedOneIstoOne", bDedOneIstoOne)
                .add("bDedOnePlusOne", bDedOnePlusOne)
                .add("bEnhanced", bEnhanced).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
