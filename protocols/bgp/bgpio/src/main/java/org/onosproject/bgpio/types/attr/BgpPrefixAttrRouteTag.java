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
 * Implements BGP prefix route tag attribute.
 */
public class BgpPrefixAttrRouteTag implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpPrefixAttrRouteTag.class);

    public static final short ATTR_PREFIX_ROUTETAG = 1153;
    public static final short SIZE = 4;

    /* Prefix Route Tag */
    private List<Integer> pfxRouteTag = new ArrayList<Integer>();

    /**
     * Constructor to initialize the values.
     *
     * @param pfxRouteTag prefix route tag
     */
    public BgpPrefixAttrRouteTag(List<Integer> pfxRouteTag) {
        this.pfxRouteTag = pfxRouteTag;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param pfxRouteTag Prefix Metric
     * @return object of BgpPrefixAttrRouteTag
     */
    public static BgpPrefixAttrRouteTag of(ArrayList<Integer> pfxRouteTag) {
        return new BgpPrefixAttrRouteTag(pfxRouteTag);
    }

    /**
     * Reads the Route Tag.
     *
     * @param cb ChannelBuffer
     * @return object of BgpPrefixAttrRouteTag
     * @throws BgpParseException while parsing BgpPrefixAttrRouteTag
     */
    public static BgpPrefixAttrRouteTag read(ChannelBuffer cb)
            throws BgpParseException {
        int tmp;
        ArrayList<Integer> pfxRouteTag = new ArrayList<Integer>();

        short lsAttrLength = cb.readShort();
        int len = lsAttrLength / SIZE;

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        for (int i = 0; i < len; i++) {
            tmp = cb.readInt();
            pfxRouteTag.add(new Integer(tmp));
        }

        return BgpPrefixAttrRouteTag.of(pfxRouteTag);
    }

    /**
     * Returns the prefix route tag.
     *
     * @return route tag
     */
    public List<Integer> getPfxRouteTag() {
        return pfxRouteTag;
    }

    @Override
    public short getType() {
        return ATTR_PREFIX_ROUTETAG;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pfxRouteTag);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpPrefixAttrRouteTag) {
            BgpPrefixAttrRouteTag other = (BgpPrefixAttrRouteTag) obj;
            return Objects.equals(pfxRouteTag, other.pfxRouteTag);
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
                .add("pfxRouteTag", pfxRouteTag).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
