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
 * Implements BGP MPLS protocol mask attribute.
 */
public class BgpLinkAttrMplsProtocolMask implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(BgpLinkAttrMplsProtocolMask.class);

    public static final int ATTRLINK_MPLSPROTOMASK = 1094;
    public static final int MASK_BYTE_LEN = 1;

    private final boolean bLdp;
    private final boolean bRsvpTe;

    public static final byte FIRST_BIT = (byte) 0x80;
    public static final byte SECOND_BIT = 0x40;

    /**
     * Constructor to initialize the values.
     *
     * @param bLdp boolean value true if LDP flag is available
     * @param bRsvpTe boolean value true if RSVP TE information is available
     */
    public BgpLinkAttrMplsProtocolMask(boolean bLdp, boolean bRsvpTe) {
        this.bLdp = bLdp;
        this.bRsvpTe = bRsvpTe;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param bLdp boolean value true if LDP flag is available
     * @param bRsvpTe boolean value true if RSVP TE information is available
     * @return object of BgpLinkAttrMplsProtocolMask
     */
    public static BgpLinkAttrMplsProtocolMask of(final boolean bLdp,
                                                 final boolean bRsvpTe) {
        return new BgpLinkAttrMplsProtocolMask(bLdp, bRsvpTe);
    }

    /**
     * Reads the BGP link attributes MPLS protocol mask.
     *
     * @param cb Channel buffer
     * @return object of type BgpLinkAttrMPLSProtocolMask
     * @throws BgpParseException while parsing BgpLinkAttrMplsProtocolMask
     */
    public static BgpLinkAttrMplsProtocolMask read(ChannelBuffer cb)
            throws BgpParseException {
        boolean bLdp = false;
        boolean bRsvpTe = false;

        short lsAttrLength = cb.readShort();

        if ((lsAttrLength != MASK_BYTE_LEN)
                || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        byte flags = cb.readByte();

        bLdp = ((flags & (byte) FIRST_BIT) == FIRST_BIT);
        bRsvpTe = ((flags & (byte) SECOND_BIT) == SECOND_BIT);

        return BgpLinkAttrMplsProtocolMask.of(bLdp, bRsvpTe);
    }

    /**
     * Returns true if LDP bit is set.
     *
     * @return True if LDP information is set else false.
     */
    public boolean ldpBit() {
        return bLdp;
    }

    /**
     * Returns RSVP TE information.
     *
     * @return True if RSVP TE information is set else false.
     */
    public boolean rsvpBit() {
        return bRsvpTe;
    }

    @Override
    public short getType() {
        return ATTRLINK_MPLSPROTOMASK;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bLdp, bRsvpTe);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrMplsProtocolMask) {
            BgpLinkAttrMplsProtocolMask other = (BgpLinkAttrMplsProtocolMask) obj;
            return Objects.equals(bLdp, other.bLdp)
                    && Objects.equals(bRsvpTe, other.bRsvpTe);
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
                .add("bLdp", bLdp).add("bRsvpTe", bRsvpTe).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
