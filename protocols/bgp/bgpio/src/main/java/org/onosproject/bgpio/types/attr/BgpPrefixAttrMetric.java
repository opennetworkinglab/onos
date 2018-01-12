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
 * Implements BGP prefix metric attribute.
 */
public class BgpPrefixAttrMetric implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(BgpPrefixAttrMetric.class);

    public static final int ATTR_PREFIX_METRIC = 1155;
    public static final int ATTR_PREFIX_LEN = 4;

    /* TE Default Metric */
    private final int linkPfxMetric;

    /**
     * Constructor to initialize value.
     *
     * @param linkPfxMetric Prefix Metric
     */
    public BgpPrefixAttrMetric(int linkPfxMetric) {
        this.linkPfxMetric = linkPfxMetric;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param linkPfxMetric Prefix Metric
     * @return object of BgpPrefixAttrMetric
     */
    public static BgpPrefixAttrMetric of(final int linkPfxMetric) {
        return new BgpPrefixAttrMetric(linkPfxMetric);
    }

    /**
     * Reads the Prefix Metric.
     *
     * @param cb ChannelBuffer
     * @return object of BgpPrefixAttrMetric
     * @throws BgpParseException while parsing BgpPrefixAttrMetric
     */
    public static BgpPrefixAttrMetric read(ChannelBuffer cb)
            throws BgpParseException {
        int linkPfxMetric;

        short lsAttrLength = cb.readShort(); // 4 Bytes

        if ((lsAttrLength != ATTR_PREFIX_LEN)
                || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        linkPfxMetric = cb.readInt();

        return BgpPrefixAttrMetric.of(linkPfxMetric);
    }

    /**
     * Returns the Prefix Metric.
     *
     * @return Prefix Metric
     */
    public int attrPfxMetric() {
        return linkPfxMetric;
    }

    @Override
    public short getType() {
        return ATTR_PREFIX_METRIC;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkPfxMetric);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpPrefixAttrMetric) {
            BgpPrefixAttrMetric other = (BgpPrefixAttrMetric) obj;
            return Objects.equals(linkPfxMetric, other.linkPfxMetric);
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
                .add("linkPfxMetric", linkPfxMetric).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
