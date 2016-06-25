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
 * Implements BGP link opaque attribute.
 */
public final class BgpLinkAttrOpaqLnkAttrib implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpLinkAttrOpaqLnkAttrib.class);

    public static final int ATTRNODE_OPAQUELNKATTRIB = 1097;

    /* Opaque Node Attribute */
    private final byte[] opaqueLinkAttribute;

    /**
     * Constructor to initialize the data.
     *
     * @param opaqueLinkAttribute opaque link attribute
     */
    private BgpLinkAttrOpaqLnkAttrib(byte[] opaqueLinkAttribute) {
        this.opaqueLinkAttribute = Arrays.copyOf(opaqueLinkAttribute,
                                                 opaqueLinkAttribute.length);
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param opaqueLinkAttribute opaque link attribute
     * @return object of BgpLinkAttrOpaqLnkAttrib
     */
    public static BgpLinkAttrOpaqLnkAttrib of(final byte[] opaqueLinkAttribute) {
        return new BgpLinkAttrOpaqLnkAttrib(opaqueLinkAttribute);
    }

    /**
     * Reads the BGP link attributes Opaque link attribute.
     *
     * @param cb Channel buffer
     * @return object of type BgpLinkAttrOpaqLnkAttrib
     * @throws BgpParseException while parsing BgpLinkAttrOpaqLnkAttrib
     */
    public static BgpLinkAttrOpaqLnkAttrib read(ChannelBuffer cb)
            throws BgpParseException {

        byte[] opaqueLinkAttribute;

        short lsAttrLength = cb.readShort();

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        opaqueLinkAttribute = new byte[lsAttrLength];
        cb.readBytes(opaqueLinkAttribute);

        return BgpLinkAttrOpaqLnkAttrib.of(opaqueLinkAttribute);
    }

    /**
     * Returns the Opaque link attribute.
     *
     * @return byte array of opaque link attribute.
     */
    public byte[] attrOpaqueLnk() {
        return opaqueLinkAttribute;
    }

    @Override
    public short getType() {
        return ATTRNODE_OPAQUELNKATTRIB;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(opaqueLinkAttribute);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrOpaqLnkAttrib) {
            BgpLinkAttrOpaqLnkAttrib other = (BgpLinkAttrOpaqLnkAttrib) obj;
            return Arrays
                    .equals(opaqueLinkAttribute, other.opaqueLinkAttribute);
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
                .add("opaqueLinkAttribute", opaqueLinkAttribute).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
