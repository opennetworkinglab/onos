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

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Implements BGP prefix opaque data attribute.
 */
public final class BgpPrefixAttrOpaqueData implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(BgpPrefixAttrOpaqueData.class);

    public static final int ATTR_PREFIX_OPAQUEDATA = 1157;

    /* Opaque Node Attribute */
    private final byte[] opaquePrefixAttribute;

    /**
     * Constructor to initialize the values.
     *
     * @param opaquePrefixAttribute opaque prefix data
     */
    public BgpPrefixAttrOpaqueData(byte[] opaquePrefixAttribute) {
        this.opaquePrefixAttribute = Arrays
                .copyOf(opaquePrefixAttribute, opaquePrefixAttribute.length);
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param opaquePrefixAttribute opaque prefix data
     * @return object of BgpPrefixAttrOpaqueData
     */
    public static BgpPrefixAttrOpaqueData of(final byte[] opaquePrefixAttribute) {
        return new BgpPrefixAttrOpaqueData(opaquePrefixAttribute);
    }

    /**
     * Reads the Opaque Prefix Attribute.
     *
     * @param cb ChannelBuffer
     * @return object of BgpPrefixAttrOpaqueData
     * @throws BgpParseException while parsing BgpPrefixAttrOpaqueData
     */
    public static BgpPrefixAttrOpaqueData read(ChannelBuffer cb)
            throws BgpParseException {
        byte[] opaquePrefixAttribute;

        short lsAttrLength = cb.readShort();
        opaquePrefixAttribute = new byte[lsAttrLength];

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        cb.readBytes(opaquePrefixAttribute);

        return BgpPrefixAttrOpaqueData.of(opaquePrefixAttribute);
    }

    /**
     * Returns the Opaque prefix attribute name.
     *
     * @return opaque prefix name
     */
    public byte[] getOpaquePrefixAttribute() {
        return opaquePrefixAttribute;
    }

    @Override
    public short getType() {
        return ATTR_PREFIX_OPAQUEDATA;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(opaquePrefixAttribute);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpPrefixAttrOpaqueData) {
            BgpPrefixAttrOpaqueData other = (BgpPrefixAttrOpaqueData) obj;
            return Arrays.equals(opaquePrefixAttribute,
                                 other.opaquePrefixAttribute);
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
                .add("opaquePrefixAttribute", getOpaquePrefixAttribute())
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
