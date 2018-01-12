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
 * Implements BGP link IGP metric attribute.
 */
public class BgpLinkAttrIgpMetric implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(BgpLinkAttrIgpMetric.class);

    public static final int ATTRLINK_IGPMETRIC = 1095;
    public static final int ATTRLINK_MAX_LEN = 3;

    /* Variable metric length based on protocol */
    public static final int ISIS_SMALL_METRIC = 1;
    public static final int OSPF_LINK_METRIC = 2;
    public static final int ISIS_WIDE_METRIC = 3;

    /* IGP Metric */
    private final int igpMetric;
    private final int igpMetricLen;

    /**
     * Constructor to initialize the value.
     *
     * @param igpMetric 3 byte IGP metric data.
     * @param igpMetricLen length of IGP metric data.
     */
    public BgpLinkAttrIgpMetric(final int igpMetric, final int igpMetricLen) {
        this.igpMetric = igpMetric;
        this.igpMetricLen = igpMetricLen;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param igpMetric 3 byte IGP metric data.
     * @param igpMetricLen length of IGP metric data.
     * @return object of BgpLinkAttrIgpMetric
     */
    public static BgpLinkAttrIgpMetric of(final int igpMetric,
                                          final int igpMetricLen) {
        return new BgpLinkAttrIgpMetric(igpMetric, igpMetricLen);
    }

    /**
     * Reads the BGP link attributes IGP Metric.
     *
     * @param cb Channel buffer
     * @return object of type BgpLinkAttrIgpMetric
     * @throws BgpParseException while parsing BgpLinkAttrIgpMetric
     */
    public static BgpLinkAttrIgpMetric read(ChannelBuffer cb)
            throws BgpParseException {

        int linkigp;
        int igpMetric = 0;
        int igpMetricLen = 0;

        short lsAttrLength = cb.readShort();

        if (cb.readableBytes() < lsAttrLength
                || lsAttrLength > ATTRLINK_MAX_LEN) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        switch (lsAttrLength) {
        case ISIS_SMALL_METRIC:
            igpMetric = cb.readByte();
            igpMetricLen = ISIS_SMALL_METRIC;
            break;
        case OSPF_LINK_METRIC:
            igpMetric = cb.readShort();
            igpMetricLen = OSPF_LINK_METRIC;
            break;
        case ISIS_WIDE_METRIC:
            linkigp = cb.readShort();
            igpMetric = cb.readByte();
            igpMetric = (linkigp << 8) | igpMetric;
            igpMetricLen = ISIS_WIDE_METRIC;
            break;
        default: // validation is already in place
            break;
        }

        return BgpLinkAttrIgpMetric.of(igpMetric, igpMetricLen);
    }

    /**
     * Returns the variable length IGP metric data.
     *
     * @return IGP metric data
     */
    public int attrLinkIgpMetric() {
        return igpMetric;
    }

    /**
     * Returns IGP metric data length.
     *
     * @return IGP metric length
     */
    public int attrLinkIgpMetricLength() {
        return igpMetricLen;
    }

    @Override
    public short getType() {
        return ATTRLINK_IGPMETRIC;
    }

    @Override
    public int hashCode() {
        return Objects.hash(igpMetric, igpMetricLen);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrIgpMetric) {
            BgpLinkAttrIgpMetric other = (BgpLinkAttrIgpMetric) obj;
            return Objects.equals(igpMetric, other.igpMetric)
                    && Objects.equals(igpMetricLen, other.igpMetricLen);
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
                .add("igpMetric", igpMetric).add("igpMetricLen", igpMetricLen)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
