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
 * Implements BGP attribute Max Link bandwidth.
 */
public final class BgpLinkAttrMaxLinkBandwidth implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpLinkAttrMaxLinkBandwidth.class);

    public static final int MAX_BANDWIDTH_LEN = 4;
    public static final int NO_OF_BITS = 8;

    private short type;

    /* ISIS administrative group */
    private final float maxBandwidth;

    /**
     * Constructor to initialize the values.
     *
     * @param maxBandwidth Maximum link bandwidth.
     * @param type TLV type
     */
    private BgpLinkAttrMaxLinkBandwidth(float maxBandwidth, short type) {
        this.maxBandwidth = maxBandwidth;
        this.type = type;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param maxBandwidth Maximum link bandwidth.
     * @param type TLV type
     * @return object of BgpLinkAttrMaxLinkBandwidth
     */
    public static BgpLinkAttrMaxLinkBandwidth of(final float maxBandwidth,
                                                 final short type) {
        return new BgpLinkAttrMaxLinkBandwidth(maxBandwidth, type);
    }

    /**
     * Reads the BGP link attributes of Maximum link bandwidth.
     *
     * @param cb Channel buffer
     * @param type type of this tlv
     * @return object of type BgpLinkAttrMaxLinkBandwidth
     * @throws BgpParseException while parsing BgpLinkAttrMaxLinkBandwidth
     */
    public static BgpLinkAttrMaxLinkBandwidth read(ChannelBuffer cb, short type)
            throws BgpParseException {
        float maxBandwidth;
        short lsAttrLength = cb.readShort();

        if ((lsAttrLength != MAX_BANDWIDTH_LEN)
                || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        maxBandwidth = ieeeToFloatRead(cb.readInt()) * NO_OF_BITS;

        return BgpLinkAttrMaxLinkBandwidth.of(maxBandwidth, type);
    }

    /**
     * Returns Maximum link bandwidth.
     *
     * @return Maximum link bandwidth
     */
    public float linkAttrMaxLinkBandwidth() {
        return maxBandwidth;
    }

    /**
     * Parse the IEEE floating point notation and returns it in normal float.
     *
     * @param iVal IEEE floating point number
     * @return normal float
     */
    static float ieeeToFloatRead(int iVal) {

        return Float.intBitsToFloat(iVal);
    }

    @Override
    public short getType() {
        return this.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxBandwidth);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrMaxLinkBandwidth) {
            BgpLinkAttrMaxLinkBandwidth other = (BgpLinkAttrMaxLinkBandwidth) obj;
            return Objects.equals(maxBandwidth, other.maxBandwidth);
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
                .add("maxBandwidth", maxBandwidth).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
