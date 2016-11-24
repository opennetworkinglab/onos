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
 * Implements BGP attribute node name.
 */
public class BgpAttrNodeName implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpAttrNodeName.class);

    public static final int ATTRNODE_NAME = 1026;

    /* Node Name */
    private byte[] nodeName;

    /**
     * Constructor to initialize value.
     *
     * @param nodeName node name
     */
    public BgpAttrNodeName(byte[] nodeName) {
        this.nodeName = Arrays.copyOf(nodeName, nodeName.length);
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param nodeName node name
     * @return object of BgpAttrNodeName
     */
    public static BgpAttrNodeName of(final byte[] nodeName) {
        return new BgpAttrNodeName(nodeName);
    }

    /**
     * Reads the LS attribute node name.
     *
     * @param cb ChannelBuffer
     * @return object of BgpAttrNodeName
     * @throws BgpParseException while parsing BgpAttrNodeName
     */
    public static BgpAttrNodeName read(ChannelBuffer cb)
            throws BgpParseException {
        byte[] nodeName;

        short lsAttrLength = cb.readShort();

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        nodeName = new byte[lsAttrLength];
        cb.readBytes(nodeName);
        log.debug("LS attribute node name read");
        return BgpAttrNodeName.of(nodeName);
    }

    /**
     * Returns LS attribute node name.
     *
     * @return node name
     */
    public byte[] attrNodeName() {
        return nodeName;
    }

    @Override
    public short getType() {
        return ATTRNODE_NAME;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(nodeName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpAttrNodeName) {
            BgpAttrNodeName other = (BgpAttrNodeName) obj;
            return Arrays.equals(nodeName, other.nodeName);
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
                .add("nodeName", nodeName).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
