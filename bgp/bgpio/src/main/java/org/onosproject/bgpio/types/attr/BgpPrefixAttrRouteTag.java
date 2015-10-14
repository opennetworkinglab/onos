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

import java.util.Arrays;
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
 * Implements BGP prefix route tag attribute.
 */
public class BgpPrefixAttrRouteTag implements BGPValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpPrefixAttrRouteTag.class);

    public static final int ATTR_PREFIX_ROUTETAG = 1153;

    /* Prefix Route Tag */
    private int[] pfxRouteTag;

    /**
     * Constructor to initialize the values.
     *
     * @param pfxRouteTag prefix route tag
     */
    BgpPrefixAttrRouteTag(int[] pfxRouteTag) {
        this.pfxRouteTag = Arrays.copyOf(pfxRouteTag, pfxRouteTag.length);
    }

    /**
     * Reads the Route Tag.
     *
     * @param cb ChannelBuffer
     * @return object of BgpPrefixAttrRouteTag
     * @throws BGPParseException while parsing BgpPrefixAttrRouteTag
     */
    public static BgpPrefixAttrRouteTag read(ChannelBuffer cb)
            throws BGPParseException {
        int[] pfxRouteTag;

        short lsAttrLength = cb.readShort();
        int len = lsAttrLength / Integer.SIZE;

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BGPErrorType.UPDATE_MESSAGE_ERROR,
                                   BGPErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        pfxRouteTag = new int[lsAttrLength];

        for (int i = 0; i < len; i++) {
            pfxRouteTag[i] = cb.readInt();
        }

        return new BgpPrefixAttrRouteTag(pfxRouteTag);
    }

    /**
     * Returns the prefix route tag.
     *
     * @return route tag
     */
    int[] getPfxRouteTag() {
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
}
