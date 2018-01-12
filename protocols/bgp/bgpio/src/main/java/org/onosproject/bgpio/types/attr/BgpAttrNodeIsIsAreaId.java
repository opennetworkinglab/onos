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
 * Implements BGP attribute ISIS Area Identifier.
 */
public class BgpAttrNodeIsIsAreaId implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(BgpAttrNodeIsIsAreaId.class);

    public static final int ATTRNODE_ISISAREAID = 1027;

    /* IS-IS Area Identifier TLV */
    private byte[] isisAreaId;

    /**
     * Constructor to initialize value.
     *
     * @param isisAreaId ISIS area Identifier
     */
    public BgpAttrNodeIsIsAreaId(byte[] isisAreaId) {
        this.isisAreaId = Arrays.copyOf(isisAreaId, isisAreaId.length);
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param isisAreaId ISIS area Identifier
     * @return object of BgpAttrNodeIsIsAreaId
     */
    public static BgpAttrNodeIsIsAreaId of(final byte[] isisAreaId) {
        return new BgpAttrNodeIsIsAreaId(isisAreaId);
    }

    /**
     * Reads the IS-IS Area Identifier.
     *
     * @param cb ChannelBuffer
     * @return object of BgpAttrNodeIsIsAreaId
     * @throws BgpParseException while parsing BgpAttrNodeIsIsAreaId
     */
    public static BgpAttrNodeIsIsAreaId read(ChannelBuffer cb)
            throws BgpParseException {
        byte[] isisAreaId;

        short lsAttrLength = cb.readShort();

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        isisAreaId = new byte[lsAttrLength];
        cb.readBytes(isisAreaId);

        return BgpAttrNodeIsIsAreaId.of(isisAreaId);
    }

    /**
     * Returns ISIS area Identifier.
     *
     * @return Area ID
     */
    public byte[] attrNodeIsIsAreaId() {
        return isisAreaId;
    }

    @Override
    public short getType() {
        return ATTRNODE_ISISAREAID;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(isisAreaId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpAttrNodeIsIsAreaId) {
            BgpAttrNodeIsIsAreaId other = (BgpAttrNodeIsIsAreaId) obj;
            return Arrays.equals(isisAreaId, other.isisAreaId);
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
                .add("isisAreaId", isisAreaId).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
