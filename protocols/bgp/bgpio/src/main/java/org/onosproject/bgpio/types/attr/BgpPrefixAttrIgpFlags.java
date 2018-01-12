/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Implements BGP prefix IGP Flag attribute.
 */
public final class BgpPrefixAttrIgpFlags implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(BgpPrefixAttrIgpFlags.class);

    public static final int ATTR_PREFIX_FLAGBIT = 1152;
    public static final int ATTR_PREFIX_FLAG_LEN = 1;

    public static final byte FIRST_BIT = (byte) 0x80;
    public static final byte SECOND_BIT = 0x40;
    public static final byte THIRD_BIT = 0x20;
    public static final byte FOURTH_BIT = 0x01;

    /* Prefix IGP flag bit TLV */
    private final boolean bisisUpDownBit;
    private final boolean bOspfNoUnicastBit;
    private final boolean bOspfLclAddrBit;
    private final boolean bOspfNSSABit;

    /**
     * Constructor to initialize the value.
     *
     * @param bisisUpDownBit IS-IS Up/Down Bit
     * @param bOspfNoUnicastBit OSPF no unicast Bit
     * @param bOspfLclAddrBit OSPF local address Bit
     * @param bOspfNssaBit OSPF propagate NSSA Bit
     */
    public BgpPrefixAttrIgpFlags(boolean bisisUpDownBit,
                          boolean bOspfNoUnicastBit,
                          boolean bOspfLclAddrBit, boolean bOspfNssaBit) {
        this.bisisUpDownBit = bisisUpDownBit;
        this.bOspfNoUnicastBit = bOspfNoUnicastBit;
        this.bOspfLclAddrBit = bOspfLclAddrBit;
        this.bOspfNSSABit = bOspfNssaBit;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param bisisUpDownBit IS-IS Up/Down Bit
     * @param bOspfNoUnicastBit OSPF no unicast Bit
     * @param bOspfLclAddrBit OSPF local address Bit
     * @param bOspfNssaBit OSPF propagate NSSA Bit
     * @return object of BgpPrefixAttrIGPFlags
     */
    public static BgpPrefixAttrIgpFlags of(final boolean bisisUpDownBit,
                                           final boolean bOspfNoUnicastBit,
                                           final boolean bOspfLclAddrBit,
                                           final boolean bOspfNssaBit) {
        return new BgpPrefixAttrIgpFlags(bisisUpDownBit, bOspfNoUnicastBit,
                                         bOspfLclAddrBit, bOspfNssaBit);
    }

    /**
     * Reads the IGP Flags.
     *
     * @param cb ChannelBuffer
     * @return object of BgpPrefixAttrIGPFlags
     * @throws BgpParseException while parsing BgpPrefixAttrIGPFlags
     */
    public static BgpPrefixAttrIgpFlags read(ChannelBuffer cb)
            throws BgpParseException {
        boolean bisisUpDownBit = false;
        boolean bOspfNoUnicastBit = false;
        boolean bOspfLclAddrBit = false;
        boolean bOspfNssaBit = false;

        short lsAttrLength = cb.readShort();

        if ((lsAttrLength != ATTR_PREFIX_FLAG_LEN)
                || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        byte nodeFlagBits = cb.readByte();

        bisisUpDownBit = ((nodeFlagBits & FIRST_BIT) == FIRST_BIT);
        bOspfNoUnicastBit = ((nodeFlagBits & SECOND_BIT) == SECOND_BIT);
        bOspfLclAddrBit = ((nodeFlagBits & THIRD_BIT) == THIRD_BIT);
        bOspfNssaBit = ((nodeFlagBits & FOURTH_BIT) == FOURTH_BIT);

        return BgpPrefixAttrIgpFlags.of(bisisUpDownBit, bOspfNoUnicastBit,
                                        bOspfLclAddrBit, bOspfNssaBit);
    }

    /**
     * Returns the IS-IS Up/Down Bit set or not.
     *
     * @return IS-IS Up/Down Bit set or not
     */
    public boolean isisUpDownBit() {
        return bisisUpDownBit;
    }

    /**
     * Returns the OSPF no unicast Bit set or not.
     *
     * @return OSPF no unicast Bit set or not
     */
    public boolean ospfNoUnicastBit() {
        return bOspfNoUnicastBit;
    }

    /**
     * Returns the OSPF local address Bit set or not.
     *
     * @return OSPF local address Bit set or not
     */
    public boolean ospfLclAddrBit() {
        return bOspfLclAddrBit;
    }

    /**
     * Returns the OSPF propagate NSSA Bit set or not.
     *
     * @return OSPF propagate NSSA Bit set or not
     */
    public boolean ospfNssaBit() {
        return bOspfNSSABit;
    }

    @Override
    public short getType() {
        return ATTR_PREFIX_FLAGBIT;
    }

    @Override
    public int write(ChannelBuffer cb) {
        // TODO This will be implemented in the next version
        return 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bisisUpDownBit, bOspfNoUnicastBit, bOspfLclAddrBit,
                            bOspfNSSABit);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpPrefixAttrIgpFlags) {
            BgpPrefixAttrIgpFlags other = (BgpPrefixAttrIgpFlags) obj;
            return Objects.equals(bisisUpDownBit, other.bisisUpDownBit)
                    && Objects.equals(bOspfNoUnicastBit,
                                      other.bOspfNoUnicastBit)
                                      && Objects.equals(bOspfLclAddrBit, other.bOspfLclAddrBit)
                                      && Objects.equals(bOspfNSSABit, other.bOspfNSSABit);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("bisisUpDownBit", bisisUpDownBit)
                .add("bOspfNoUnicastBit", bOspfNoUnicastBit)
                .add("bOspfLclAddrBit", bOspfLclAddrBit)
                .add("bOspfNSSABit", bOspfNSSABit).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
