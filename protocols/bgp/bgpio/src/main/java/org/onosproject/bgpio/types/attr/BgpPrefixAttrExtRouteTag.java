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

import static com.google.common.base.Preconditions.checkNotNull;

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
 * Implements BGP prefix route Extended tag attribute.
 */
public class BgpPrefixAttrExtRouteTag implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpPrefixAttrExtRouteTag.class);

    public static final int ATTR_PREFIX_EXTROUTETAG = 1154;
    public static final int ATTR_PREFIX_EXT_LEN = 8;

    /* Prefix Route Tag */
    private List<Long> pfxExtRouteTag = new ArrayList<Long>();

    /**
     * Constructor to initialize the values.
     *
     * @param pfxExtRouteTag Extended route tag
     */
    public BgpPrefixAttrExtRouteTag(List<Long> pfxExtRouteTag) {
        this.pfxExtRouteTag = checkNotNull(pfxExtRouteTag);
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param pfxExtRouteTag Prefix Metric
     * @return object of BgpPrefixAttrMetric
     */
    public static BgpPrefixAttrExtRouteTag of(ArrayList<Long> pfxExtRouteTag) {
        return new BgpPrefixAttrExtRouteTag(pfxExtRouteTag);
    }

    /**
     * Reads the Extended Tag.
     *
     * @param cb ChannelBuffer
     * @return object of BgpPrefixAttrExtRouteTag
     * @throws BgpParseException while parsing BgpPrefixAttrExtRouteTag
     */
    public static BgpPrefixAttrExtRouteTag read(ChannelBuffer cb)
            throws BgpParseException {
        ArrayList<Long> pfxExtRouteTag = new ArrayList<Long>();
        long temp;

        short lsAttrLength = cb.readShort();
        int len = lsAttrLength / ATTR_PREFIX_EXT_LEN;

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        for (int i = 0; i < len; i++) {
            temp = cb.readLong();
            pfxExtRouteTag.add(new Long(temp));
        }

        return new BgpPrefixAttrExtRouteTag(pfxExtRouteTag);
    }

    /**
     * Returns Extended route tag.
     *
     * @return route tag
     */
    public List<Long> pfxExtRouteTag() {
        return pfxExtRouteTag;
    }

    @Override
    public short getType() {
        return ATTR_PREFIX_EXTROUTETAG;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pfxExtRouteTag);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpPrefixAttrExtRouteTag) {
            BgpPrefixAttrExtRouteTag other = (BgpPrefixAttrExtRouteTag) obj;
            return Objects.equals(pfxExtRouteTag, other.pfxExtRouteTag);
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
                .add("pfxExtRouteTag", pfxExtRouteTag).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
