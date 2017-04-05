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

import java.util.ArrayList;
import java.util.List;
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
 * Implements BGP unreserved bandwidth attribute.
 */
public class BgpLinkAttrUnRsrvdLinkBandwidth implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpLinkAttrUnRsrvdLinkBandwidth.class);

    public static final int MAX_BANDWIDTH_LEN = 4;
    public static final int NO_OF_BITS = 8;
    public static final int NO_OF_PRIORITY = 8;

    private short sType;

    /* ISIS administrative group */
    private List<Float> maxUnResBandwidth = new ArrayList<Float>();

    /**
     * Constructor to initialize the values.
     *
     * @param maxUnResBandwidth Maximum Unreserved bandwidth
     * @param sType returns the tag value
     */
    public BgpLinkAttrUnRsrvdLinkBandwidth(List<Float> maxUnResBandwidth,
                                           short sType) {
        this.maxUnResBandwidth = maxUnResBandwidth;
        this.sType = sType;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param linkPfxMetric Prefix Metric
     * @param sType returns the tag value
     * @return object of BgpLinkAttrUnRsrvdLinkBandwidth
     */
    public static BgpLinkAttrUnRsrvdLinkBandwidth of(List<Float> linkPfxMetric, short sType) {
        return new BgpLinkAttrUnRsrvdLinkBandwidth(linkPfxMetric, sType);
    }

    /**
     * Reads the BGP link attributes of Maximum link bandwidth.
     *
     * @param cb Channel buffer
     * @param sType returns the tag value
     * @return object of type BgpLinkAttrMaxLinkBandwidth
     * @throws BgpParseException while parsing BgpLinkAttrMaxLinkBandwidth
     */
    public static BgpLinkAttrUnRsrvdLinkBandwidth read(ChannelBuffer cb,
                                                       short sType)
                                                               throws BgpParseException {
        ArrayList<Float> maxUnResBandwidth = new ArrayList<Float>();
        float tmp;
        short lsAttrLength = cb.readShort();

        if ((lsAttrLength != MAX_BANDWIDTH_LEN * NO_OF_PRIORITY)
                || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        for (int i = 0; i < NO_OF_PRIORITY; i++) {
            tmp = ieeeToFloatRead(cb.readInt()) * NO_OF_BITS;
            maxUnResBandwidth.add(new Float(tmp));
        }

        return BgpLinkAttrUnRsrvdLinkBandwidth.of(maxUnResBandwidth, sType);
    }

    /**
     * Returns maximum unreserved bandwidth.
     *
     * @return unreserved bandwidth.
     */
    public List<Float> getLinkAttrUnRsrvdLinkBandwidth() {
        return maxUnResBandwidth;
    }

    /**
     * Parse the IEEE floating point notation and returns it in normal float.
     *
     * @param iVal IEEE floating point number
     * @return normal float
     */
    static float ieeeToFloatRead(int  iVal) {

        return Float.intBitsToFloat(iVal);
    }

    @Override
    public short getType() {
        return this.sType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxUnResBandwidth);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrUnRsrvdLinkBandwidth) {
            BgpLinkAttrUnRsrvdLinkBandwidth other = (BgpLinkAttrUnRsrvdLinkBandwidth) obj;
            return Objects.equals(maxUnResBandwidth, other.maxUnResBandwidth);
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
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("maxUnResBandwidth", maxUnResBandwidth).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
