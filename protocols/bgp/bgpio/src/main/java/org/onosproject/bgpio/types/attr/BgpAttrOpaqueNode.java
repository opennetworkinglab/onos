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
 * Implements BGP attribute opaque node.
 */
public class BgpAttrOpaqueNode implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpAttrOpaqueNode.class);

    public static final int ATTRNODE_OPAQUEDATA = 1025;

    /* Opaque Node Attribute */
    private byte[] opaqueNodeAttribute;

    /**
     * Constructor to initialize parameter.
     *
     * @param opaqueNodeAttribute opaque node attribute
     */
    public BgpAttrOpaqueNode(byte[] opaqueNodeAttribute) {
        this.opaqueNodeAttribute = Arrays.copyOf(opaqueNodeAttribute, opaqueNodeAttribute.length);
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param opaqueNodeAttribute Prefix Metric
     * @return object of BgpAttrOpaqueNode
     */
    public static BgpAttrOpaqueNode of(byte[] opaqueNodeAttribute) {
        return new BgpAttrOpaqueNode(opaqueNodeAttribute);
    }

    /**
     * Reads the Opaque Node Properties.
     *
     * @param cb ChannelBuffer
     * @return object of BgpAttrOpaqueNode
     * @throws BgpParseException while parsing BgpAttrOpaqueNode
     */
    public static BgpAttrOpaqueNode read(ChannelBuffer cb)
            throws BgpParseException {

        byte[] opaqueNodeAttribute;

        short lsAttrLength = cb.readShort();

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        opaqueNodeAttribute = new byte[lsAttrLength];
        cb.readBytes(opaqueNodeAttribute);

        return BgpAttrOpaqueNode.of(opaqueNodeAttribute);
    }

    /**
     * Returns opaque node attribute.
     *
     * @return LS node attribute value
     */
    public byte[] attrOpaqueNode() {
        return opaqueNodeAttribute;
    }

    @Override
    public short getType() {
        return ATTRNODE_OPAQUEDATA;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(opaqueNodeAttribute);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpAttrOpaqueNode) {
            BgpAttrOpaqueNode other = (BgpAttrOpaqueNode) obj;
            return Arrays
                    .equals(opaqueNodeAttribute, other.opaqueNodeAttribute);
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
                .add("opaqueNodeAttribute", opaqueNodeAttribute).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
