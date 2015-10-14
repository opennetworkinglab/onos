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
 * Implements BGP prefix IGP Flag attribute.
 */
public class BgpPrefixAttrIGPFlags implements BGPValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpPrefixAttrIGPFlags.class);

    public static final int ATTR_PREFIX_FLAGBIT = 1152;
    public static final int ATTR_PREFIX_FLAG_LEN = 1;

    public static final int FIRST_BIT = 0x80;
    public static final int SECOND_BIT = 0x40;
    public static final int THIRD_BIT = 0x20;
    public static final int FOURTH_BIT = 0x01;

    /* Prefix IGP flag bit TLV */
    private boolean bisisUpDownBit = false;
    private boolean bOspfNoUnicastBit = false;
    private boolean bOspfLclAddrBit = false;
    private boolean bOspfNSSABit = false;

    /**
     * Constructor to initialize the value.
     *
     * @param bisisUpDownBit IS-IS Up/Down Bit
     * @param bOspfNoUnicastBit OSPF no unicast Bit
     * @param bOspfLclAddrBit OSPF local address Bit
     * @param bOspfNSSABit OSPF propagate NSSA Bit
     */
    BgpPrefixAttrIGPFlags(boolean bisisUpDownBit, boolean bOspfNoUnicastBit,
                          boolean bOspfLclAddrBit, boolean bOspfNSSABit) {
        this.bisisUpDownBit = bisisUpDownBit;
        this.bOspfNoUnicastBit = bOspfNoUnicastBit;
        this.bOspfLclAddrBit = bOspfLclAddrBit;
        this.bOspfNSSABit = bOspfNSSABit;
    }

    /**
     * Reads the IGP Flags.
     *
     * @param cb ChannelBuffer
     * @return object of BgpPrefixAttrIGPFlags
     * @throws BGPParseException while parsing BgpPrefixAttrIGPFlags
     */
    public static BgpPrefixAttrIGPFlags read(ChannelBuffer cb)
            throws BGPParseException {
        boolean bisisUpDownBit = false;
        boolean bOspfNoUnicastBit = false;
        boolean bOspfLclAddrBit = false;
        boolean bOspfNSSABit = false;

        short lsAttrLength = cb.readShort();

        if ((lsAttrLength != ATTR_PREFIX_FLAG_LEN)
                || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BGPErrorType.UPDATE_MESSAGE_ERROR,
                                   BGPErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        byte nodeFlagBits = cb.readByte();

        bisisUpDownBit = ((nodeFlagBits & (byte) FIRST_BIT) == FIRST_BIT);
        bOspfNoUnicastBit = ((nodeFlagBits & (byte) SECOND_BIT) == SECOND_BIT);
        bOspfLclAddrBit = ((nodeFlagBits & (byte) THIRD_BIT) == THIRD_BIT);
        bOspfNSSABit = ((nodeFlagBits & (byte) FOURTH_BIT) == FOURTH_BIT);

        return new BgpPrefixAttrIGPFlags(bisisUpDownBit, bOspfNoUnicastBit,
                                         bOspfLclAddrBit, bOspfNSSABit);
    }

    /**
     * Returns the IS-IS Up/Down Bit set or not.
     *
     * @return IS-IS Up/Down Bit set or not
     */
    boolean getisisUpDownBit() {
        return bisisUpDownBit;
    }

    /**
     * Returns the OSPF no unicast Bit set or not.
     *
     * @return OSPF no unicast Bit set or not
     */
    boolean getOspfNoUnicastBit() {
        return bOspfNoUnicastBit;
    }

    /**
     * Returns the OSPF local address Bit set or not.
     *
     * @return OSPF local address Bit set or not
     */
    boolean getOspfLclAddrBit() {
        return bOspfLclAddrBit;
    }

    /**
     * Returns the OSPF propagate NSSA Bit set or not.
     *
     * @return OSPF propagate NSSA Bit set or not
     */
    boolean getOspfNSSABit() {
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

        if (obj instanceof BgpPrefixAttrIGPFlags) {
            BgpPrefixAttrIGPFlags other = (BgpPrefixAttrIGPFlags) obj;
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
}
