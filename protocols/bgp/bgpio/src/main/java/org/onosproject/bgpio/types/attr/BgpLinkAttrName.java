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
 * Implements BGP link name attribute.
 */
public class BgpLinkAttrName implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(BgpLinkAttrName.class);

    public static final int ATTRLINK_NAME = 1098;

    /* Link Name */
    private byte[] linkName;

    /**
     * Constructor to initialize the values.
     *
     * @param linkName link name
     */
    public BgpLinkAttrName(byte[] linkName) {
        this.linkName = Arrays.copyOf(linkName, linkName.length);
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param linkName Prefix Metric
     * @return object of BgpLinkAttrName
     */
    public static BgpLinkAttrName of(byte[] linkName) {
        return new BgpLinkAttrName(linkName);
    }

    /**
     * Reads the BGP link attributes Name.
     *
     * @param cb Channel buffer
     * @return object of type BgpLinkAttrName
     * @throws BgpParseException while parsing BgpLinkAttrName
     */
    public static BgpLinkAttrName read(ChannelBuffer cb)
            throws BgpParseException {
        byte[] linkName;
        short lsAttrLength = cb.readShort();

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        linkName = new byte[lsAttrLength];
        cb.readBytes(linkName);
        return BgpLinkAttrName.of(linkName);
    }

    /**
     * Returns the link name.
     *
     * @return link name
     */
    public byte[] attrLinkName() {
        return linkName;
    }

    @Override
    public short getType() {
        return ATTRLINK_NAME;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(linkName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrName) {
            BgpLinkAttrName other = (BgpLinkAttrName) obj;
            return Arrays.equals(linkName, other.linkName);
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
                .add("linkName", linkName).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
