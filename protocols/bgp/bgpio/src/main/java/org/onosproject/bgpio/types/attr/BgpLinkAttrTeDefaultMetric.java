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
 * Implements BGP link state Default TE metric link attribute.
 */
public class BgpLinkAttrTeDefaultMetric implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(BgpLinkAttrTeDefaultMetric.class);

    public static final int ATTRLINK_TEDEFAULTMETRIC = 1092;
    public static final int TE_DATA_LEN = 4;

    /* TE Default Metric */
    private int linkTeMetric;

    /**
     * Constructor to initialize the value.
     *
     * @param linkTeMetric TE default metric
     *
     */
    public BgpLinkAttrTeDefaultMetric(int linkTeMetric) {
        this.linkTeMetric = linkTeMetric;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param linkTeMetric TE default metric
     * @return object of BgpLinkAttrTeDefaultMetric
     */
    public static BgpLinkAttrTeDefaultMetric of(final int linkTeMetric) {
        return new BgpLinkAttrTeDefaultMetric(linkTeMetric);
    }

    /**
     * Reads the BGP link attributes of TE default metric.
     *
     * @param cb Channel buffer
     * @return object of type BgpLinkAttrTeDefaultMetric
     * @throws BgpParseException while parsing BgpLinkAttrTeDefaultMetric
     */
    public static BgpLinkAttrTeDefaultMetric read(ChannelBuffer cb)
            throws BgpParseException {
        int linkTeMetric;

        short lsAttrLength = cb.readShort();

        if ((lsAttrLength != TE_DATA_LEN)
                || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        linkTeMetric = cb.readInt();

        return new BgpLinkAttrTeDefaultMetric(linkTeMetric);
    }

    /**
     * Returns the TE default metrics.
     *
     * @return link default metric
     */
    public int attrLinkDefTeMetric() {
        return linkTeMetric;
    }

    @Override
    public short getType() {
        return ATTRLINK_TEDEFAULTMETRIC;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkTeMetric);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrTeDefaultMetric) {
            BgpLinkAttrTeDefaultMetric other = (BgpLinkAttrTeDefaultMetric) obj;
            return Objects.equals(linkTeMetric, other.linkTeMetric);
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
                .add("linkTEMetric", linkTeMetric).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
